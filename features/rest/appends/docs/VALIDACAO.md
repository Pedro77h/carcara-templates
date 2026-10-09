
## Smoke do fluxo crítico (`scripts/smoke.sh`)

Percorre o fluxo de conta por HTTP contra uma instância de pé: ping, cadastro, cadastro repetido
(que não pode revelar conta existente), login barrado antes da confirmação, login, perfil com e sem
token, troca de senha com senha errada e com a correta, **morte do token antigo**, login com a
senha antiga falhando, logout, e exclusão da conta.

```bash
docker compose up -d
./mvnw spring-boot:run                      # outro terminal
scripts/smoke.sh                            # local
API_URL=https://api.exemplo.com scripts/smoke.sh
```

Com `app.auth.email-verification-required=true` (o default) o smoke para na confirmação de e-mail,
porque o token só sai por e-mail — ele reporta os passos que deu e pede `VERIFY_TOKEN=<token>`.
Para rodar o fluxo inteiro sem caixa de entrada, suba com a verificação desligada:

```bash
AUTH_EMAIL_VERIFICATION_REQUIRED=false ./mvnw spring-boot:run
```

Saída esperada: `==> N passo(s) ok, 0 falha(s)`, com código de saída 0. Qualquer falha devolve 1,
então ele serve direto em pipeline de homologação.
