#!/usr/bin/env bash
set -euo pipefail

# Gera um projeto novo a partir de base/ + features/<nome>/, seguindo as convenções de
# features/README.md, e renomeia pacote, groupId e artifactId.
#
# Uso:
#   scripts/generate.sh --name loja --group br.com.acme --package br.com.acme.loja \
#                       --features rest,kafka --out ../loja [--no-git]
#
# Sem dependência além de bash, awk, perl, tar e git.

TEMPLATE_DIR="$(cd "$(dirname "$0")/.." && pwd)"

NAME="" GROUP="" PACKAGE="" FEATURES="" OUT="" GIT=1

uso() {
  sed -n '4,9p' "$0" | sed 's/^# \{0,1\}//'
  echo
  echo "Features disponíveis: $(ls "$TEMPLATE_DIR/features" | grep -v README | tr '\n' ' ')"
  exit "${1:-1}"
}

erro() { echo "erro: $*" >&2; exit 1; }

while [ $# -gt 0 ]; do
  case "$1" in
    --name) NAME="$2"; shift 2 ;;
    --group) GROUP="$2"; shift 2 ;;
    --package) PACKAGE="$2"; shift 2 ;;
    --features) FEATURES="$2"; shift 2 ;;
    --out) OUT="$2"; shift 2 ;;
    --no-git) GIT=0; shift ;;
    -h|--help) uso 0 ;;
    *) erro "argumento desconhecido: $1" ;;
  esac
done

[ -n "$NAME" ] && [ -n "$GROUP" ] && [ -n "$PACKAGE" ] && [ -n "$OUT" ] || uso

JAVA_ID='[a-z][a-z0-9_]*'
[[ "$NAME" =~ ^[a-z][a-z0-9-]*$ ]] || erro "--name precisa casar com [a-z][a-z0-9-]* (vira artifactId, banco e serviço): $NAME"
[[ "$GROUP" =~ ^$JAVA_ID(\.$JAVA_ID)*$ ]] || erro "--group inválido: $GROUP"
[[ "$PACKAGE" =~ ^$JAVA_ID(\.$JAVA_ID)+$ ]] || erro "--package inválido (minúsculas, ao menos dois segmentos): $PACKAGE"
[ ! -e "$OUT" ] || [ -z "$(ls -A "$OUT")" ] || erro "$OUT já existe e não está vazio"

IFS=',' read -r -a SELECTED <<< "${FEATURES// /}"
for feature in ${SELECTED[@]+"${SELECTED[@]}"}; do
  [ -d "$TEMPLATE_DIR/features/$feature" ] || erro "feature desconhecida: $feature"
done

# "minha-loja" -> "MinhaLoja", prefixo das classes CoreApplication/CoreTestServer.
PASCAL="$(echo "$NAME" | perl -pe 's/(^|-)([a-z])/\U$2/g; s/-//g')"
EXCLUDES=(--exclude=target --exclude=.idea --exclude='*.iml' --exclude=.DS_Store)

copy_tree() { # origem destino [excludes extras...]
  local src="$1" dst="$2"; shift 2
  mkdir -p "$dst"
  tar -C "$src" "${EXCLUDES[@]}" "$@" -cf - . | tar -C "$dst" -xf -
}

echo "==> base"
copy_tree "$TEMPLATE_DIR/base" "$OUT"
OUT="$(cd "$OUT" && pwd)"

# Duas features fornecendo o mesmo arquivo seria uma substituindo a outra em silêncio.
# (Arquivo de controle em vez de array associativo: o bash do macOS é 3.2.)
OWNERS="$(mktemp)"
trap 'rm -f "$OWNERS"' EXIT

for feature in ${SELECTED[@]+"${SELECTED[@]}"}; do
  echo "==> feature $feature"
  dir="$TEMPLATE_DIR/features/$feature"

  while IFS= read -r file; do
    rel="${file#./}"
    owner="$(awk -F'\t' -v f="$rel" '$1 == f { print $2 }' "$OWNERS")"
    [ -z "$owner" ] || erro "$rel vem de duas features: $owner e $feature"
    printf '%s\t%s\n' "$rel" "$feature" >> "$OWNERS"
  done < <(cd "$dir" && find . -type f ! -path './appends/*' ! -name .DS_Store \
             ! -path ./FEATURE.md ! -path ./pom-snippet.xml ! -path ./properties-snippet.properties)

  copy_tree "$dir" "$OUT" --exclude=./appends --exclude=./FEATURE.md \
    --exclude=./pom-snippet.xml --exclude=./properties-snippet.properties

  if [ -d "$dir/appends" ]; then
    while IFS= read -r file; do
      rel="${file#./}"
      [ -f "$OUT/$rel" ] || erro "appends/$rel ($feature) não tem arquivo de destino no projeto"
      cat "$dir/appends/$rel" >> "$OUT/$rel"
    done < <(cd "$dir/appends" && find . -type f ! -name .DS_Store)
  fi

  if [ -f "$dir/properties-snippet.properties" ]; then
    { echo; cat "$dir/properties-snippet.properties"; } >> "$OUT/src/main/resources/application.properties"
  fi

  if [ -f "$dir/pom-snippet.xml" ]; then
    # Cada bloco do snippet entra imediatamente antes do marcador de mesmo nome no pom.
    awk -v snippet="$dir/pom-snippet.xml" '
      BEGIN {
        while ((getline line < snippet) > 0) {
          trimmed = line; gsub(/^[ \t]+|[ \t]+$/, "", trimmed)
          if (trimmed ~ /^<!-- features:[a-z-]+ -->$/) { current = trimmed; continue }
          if (current != "") blocks[current] = blocks[current] line "\n"
        }
      }
      {
        trimmed = $0; gsub(/^[ \t]+|[ \t]+$/, "", trimmed)
        if (trimmed in blocks) { printf "%s", blocks[trimmed]; used[trimmed] = 1 }
        print
      }
      END { for (m in blocks) if (!(m in used)) { print "marcador ausente no pom: " m > "/dev/stderr"; exit 1 } }
    ' "$OUT/pom.xml" > "$OUT/pom.xml.tmp"
    mv "$OUT/pom.xml.tmp" "$OUT/pom.xml"
  fi
done

echo "==> renomeando para $PACKAGE ($GROUP:$NAME)"

PACKAGE_PATH="${PACKAGE//.//}"
for root in src/main/java src/test/java; do
  [ -d "$OUT/$root/br/com/template/core" ] || continue
  mv "$OUT/$root/br/com/template/core" "$OUT/.pkg-tmp"
  rmdir -p "$OUT/$root/br/com/template" 2>/dev/null || true
  mkdir -p "$(dirname "$OUT/$root/$PACKAGE_PATH")"
  mv "$OUT/.pkg-tmp" "$OUT/$root/$PACKAGE_PATH"
done

for kind in main test; do
  dir="$OUT/src/$kind/java/$PACKAGE_PATH"
  [ -f "$dir/CoreApplication.java" ] && mv "$dir/CoreApplication.java" "$dir/${PASCAL}Application.java"
  [ -f "$dir/CoreApplicationTests.java" ] && mv "$dir/CoreApplicationTests.java" "$dir/${PASCAL}ApplicationTests.java"
  [ -f "$dir/support/CoreTestServer.java" ] && mv "$dir/support/CoreTestServer.java" "$dir/support/${PASCAL}TestServer.java"
done

if [ -d "$OUT/deploy" ]; then
  for file in "$OUT"/deploy/*core-app*; do
    [ -e "$file" ] && mv "$file" "${file//core-app/$NAME-app}"
  done
fi

# Substituições pontuais: "core" sozinho também é core/utils, core.hooksPath, flyway-core e o
# objeto core do github-script, que não podem ser tocados.
export NAME GROUP PACKAGE PASCAL
grep -rlI --exclude-dir=.git -e 'br.com.template' -e 'br/com/template' -e 'core' -e 'Core' "$OUT" | while IFS= read -r file; do
  perl -pi -e '
    s/br\.com\.template\.core/$ENV{PACKAGE}/g;
    s{br/com/template/core}{join("/", split(/\./, $ENV{PACKAGE}))}ge;
    s{<groupId>br\.com\.template</groupId>}{<groupId>$ENV{GROUP}</groupId>}g;
    s{<artifactId>core</artifactId>}{<artifactId>$ENV{NAME}</artifactId>}g;
    s{<name>core</name>}{<name>$ENV{NAME}</name>}g;
    s/\bCore(Application|ApplicationTests|TestServer)\b/$ENV{PASCAL}$1/g;
    s/\bcore-(app|front|postgres|0\.0\.1)/$ENV{NAME}-$1/g;
    s/chown core:core/chown $ENV{NAME}:$ENV{NAME}/g;
    s/id -u core /id -u $ENV{NAME} /;
    s/(=|:|:-|5432\/)core\b(?![\/.-])/$1$ENV{NAME}/g;
    s/"service": "core"/"service": "$ENV{NAME}"/g;
    s/^# core$/# $ENV{NAME}/;
    s/(nologin|docker) core$/$1 $ENV{NAME}/;
    s/(serviço|dono) `core`/$1 `$ENV{NAME}`/g;
  ' "$file"
done

chmod +x "$OUT/mvnw" "$OUT"/.githooks/* "$OUT"/scripts/*.sh "$OUT"/deploy/*.sh 2>/dev/null || true

if [ "$GIT" -eq 1 ]; then
  echo "==> git init"
  git -C "$OUT" init -q
  git -C "$OUT" config core.hooksPath .githooks
  git -C "$OUT" add -A
  git -C "$OUT" -c commit.gpgsign=false commit -q -m "Projeto gerado do template (features: ${FEATURES:-nenhuma})"
fi

echo "==> pronto: $OUT"
