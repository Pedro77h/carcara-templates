#!/usr/bin/env bash
set -euo pipefail

# Smoke do fluxo crítico de conta contra uma instância de pé: cadastro, confirmação de e-mail,
# login, leitura do próprio perfil, troca de senha (que precisa matar o token antigo), login com
# a senha nova e exclusão da conta.
#
# É a única validação que inclui Flyway real, perfil ativo, variável de ambiente e proxy. Nenhum
# *FlowTest cobre isso: eles sobem H2 em processo. Ver docs/VALIDACAO.md.
#
# Uso:
#   scripts/smoke.sh                                    # local, http://localhost:8080
#   API_URL=https://api.exemplo.com scripts/smoke.sh    # outro ambiente
#
# A confirmação de e-mail precisa do token, que só sai por e-mail. Em ambiente de demonstração,
# suba com app.auth.email-verification-required=false e o smoke segue direto do cadastro ao login.
# Caso contrário, informe VERIFY_TOKEN=<token recebido>.

API_URL="${API_URL:-http://localhost:8080}"
VERIFY_TOKEN="${VERIFY_TOKEN:-}"
EMAIL="${SMOKE_EMAIL:-smoke-$(date +%s)@example.com}"
PASSWORD="${SMOKE_PASSWORD:-senha-forte-smoke}"
NEW_PASSWORD="${SMOKE_NEW_PASSWORD:-senha-nova-smoke}"

passos=0
falhas=0

ok()   { passos=$((passos + 1)); printf '  \033[32mok\033[0m   %s\n' "$1"; }
falha() { falhas=$((falhas + 1)); printf '  \033[31mX\033[0m    %s\n' "$1"; }

# Imprime "<status>\n<corpo>" para a requisicao.
req() {
  local method="$1" path="$2" body="${3:-}" token="${4:-}"
  local args=(-s -o /tmp/smoke-body.$$ -w '%{http_code}' -X "$method" "$API_URL$path")

  [ -n "$body" ] && args+=(-H 'Content-Type: application/json' -d "$body")
  [ -n "$token" ] && args+=(-H "Authorization: Bearer $token")

  local status
  status=$(curl "${args[@]}")
  printf '%s\n' "$status"
  cat /tmp/smoke-body.$$
  rm -f /tmp/smoke-body.$$
}

status_de()  { printf '%s' "$1" | head -1; }
corpo_de()   { printf '%s' "$1" | tail -n +2; }
campo()      { printf '%s' "$1" | sed -n "s/.*\"$2\" *: *\"\([^\"]*\)\".*/\1/p"; }

espera() {
  local descricao="$1" esperado="$2" obtido="$3"
  if [ "$obtido" = "$esperado" ]; then ok "$descricao ($obtido)"; else falha "$descricao: esperado $esperado, obtido $obtido"; fi
}

echo "==> smoke contra $API_URL (conta: $EMAIL)"

r=$(req GET /api/ping)
espera "ping responde" 200 "$(status_de "$r")"
if [ "$(status_de "$r")" != "200" ]; then
  echo "  aplicacao nao respondeu, abortando"
  exit 1
fi

CADASTRO="{\"fullName\":\"Smoke Teste\",\"phone\":\"85999990000\",\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}"

r=$(req POST /api/auth/register "$CADASTRO")
espera "cadastro responde 202 sem corpo" 202 "$(status_de "$r")"
[ -z "$(corpo_de "$r")" ] && ok "cadastro nao devolve corpo" || falha "cadastro devolveu corpo: $(corpo_de "$r")"

r=$(req POST /api/auth/register "$CADASTRO")
espera "cadastro repetido responde igual, sem revelar conta existente" 202 "$(status_de "$r")"

# O modo de verificacao de e-mail e descoberto pelo login, nao pelo status do cadastro --
# e justamente por isso que o cadastro nao revela nada.
r=$(req POST /api/auth/login "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}")
if [ "$(status_de "$r")" = "401" ]; then
  ok "login barrado antes de confirmar o e-mail (401)"

  if [ -z "$VERIFY_TOKEN" ]; then
    echo "  -- VERIFY_TOKEN ausente: o resto do fluxo exige e-mail confirmado."
    echo "     Informe VERIFY_TOKEN=<token do e-mail>, ou suba com"
    echo "     app.auth.email-verification-required=false."
    echo "==> $passos passo(s) ok, $falhas falha(s), fluxo incompleto"
    [ "$falhas" -eq 0 ] && exit 0 || exit 1
  fi

  r=$(req POST /api/auth/verify-email "{\"token\":\"$VERIFY_TOKEN\"}")
  espera "confirmacao de e-mail" 200 "$(status_de "$r")"

  r=$(req POST /api/auth/login "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}")
fi

espera "login" 200 "$(status_de "$r")"
TOKEN=$(campo "$(corpo_de "$r")" accessToken)
[ -n "$TOKEN" ] && ok "login devolveu accessToken" || falha "login nao devolveu accessToken"

r=$(req GET /api/me "" "$TOKEN")
espera "perfil com token" 200 "$(status_de "$r")"
printf '%s' "$(corpo_de "$r")" | grep -q '"password"' && falha "perfil expos o campo password" || ok "perfil nao expoe password"

r=$(req GET /api/me)
espera "perfil sem token" 401 "$(status_de "$r")"

r=$(req POST /api/me/password "{\"currentPassword\":\"senha-errada\",\"newPassword\":\"$NEW_PASSWORD\"}" "$TOKEN")
espera "troca de senha com senha atual errada" 401 "$(status_de "$r")"

r=$(req POST /api/me/password "{\"currentPassword\":\"$PASSWORD\",\"newPassword\":\"$NEW_PASSWORD\"}" "$TOKEN")
espera "troca de senha" 200 "$(status_de "$r")"
NOVO_TOKEN=$(campo "$(corpo_de "$r")" accessToken)

r=$(req GET /api/me "" "$TOKEN")
espera "token antigo morre depois da troca de senha" 401 "$(status_de "$r")"

r=$(req GET /api/me "" "$NOVO_TOKEN")
espera "token novo funciona" 200 "$(status_de "$r")"

r=$(req POST /api/auth/login "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}")
espera "login com a senha antiga falha" 401 "$(status_de "$r")"

r=$(req POST /api/auth/login "{\"email\":\"$EMAIL\",\"password\":\"$NEW_PASSWORD\"}")
espera "login com a senha nova" 200 "$(status_de "$r")"
TOKEN=$(campo "$(corpo_de "$r")" accessToken)

r=$(req POST /api/auth/logout "" "$TOKEN")
espera "logout" 204 "$(status_de "$r")"

r=$(req GET /api/me "" "$TOKEN")
espera "token morre depois do logout" 401 "$(status_de "$r")"

r=$(req POST /api/auth/login "{\"email\":\"$EMAIL\",\"password\":\"$NEW_PASSWORD\"}")
TOKEN=$(campo "$(corpo_de "$r")" accessToken)

r=$(req DELETE /api/me "{\"password\":\"$NEW_PASSWORD\"}" "$TOKEN")
espera "exclusao da conta" 204 "$(status_de "$r")"

r=$(req POST /api/auth/login "{\"email\":\"$EMAIL\",\"password\":\"$NEW_PASSWORD\"}")
espera "conta excluida nao loga mais" 401 "$(status_de "$r")"

echo "==> $passos passo(s) ok, $falhas falha(s)"
[ "$falhas" -eq 0 ] || exit 1
