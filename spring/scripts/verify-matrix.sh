#!/usr/bin/env bash
set -euo pipefail

# Gera cada combinação de features e roda nela o próprio .githooks/pre-push do projeto gerado
# (migrations, verify, cobertura >= 80%, registro de achados). É o teste do template: uma feature
# só está pronta quando passa aqui sozinha e combinada.
#
# Uso: scripts/verify-matrix.sh [diretório de trabalho]

TEMPLATE_DIR="$(cd "$(dirname "$0")/.." && pwd)"
WORK="${1:-$(mktemp -d)}"

COMBOS=(
  ""
  "rest"
  "rest,kafka"
  "grpc,kafka"
  "rest,storage,multitenant"
  "rest,openspec"
  "rest,docs-changes"
  "rest,grpc,kafka,storage,multitenant,deploy"
)

falhas=()

for combo in "${COMBOS[@]}"; do
  label="${combo:-base}"
  dir="$WORK/${label//,/-}"
  log="$dir.log"
  printf '==> %-45s ' "$label"

  if "$TEMPLATE_DIR/scripts/generate.sh" --name app-teste --group br.com.exemplo \
       --package br.com.exemplo.app --features "$combo" --out "$dir" > "$log" 2>&1 \
     && (cd "$dir" && ./.githooks/pre-push >> "$log" 2>&1); then
    echo "ok   $(grep -o 'cobertura: [0-9.]*%' "$log")"
  else
    echo "FALHOU (log: $log)"
    falhas+=("$label")
  fi
done

if [ ${#falhas[@]} -gt 0 ]; then
  echo "==> ${#falhas[@]} combinação(ões) falharam: ${falhas[*]}"
  exit 1
fi

echo "==> todas as combinações passaram"
