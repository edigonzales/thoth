#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
THOTH_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
INTERLIS_LAB_REPO="${INTERLIS_LAB_REPO:-$(cd "$THOTH_ROOT/.." && pwd)/interlis-lab-web-component}"

if [[ ! -d "$INTERLIS_LAB_REPO" ]]; then
  echo "interlis-lab-web-component repo not found: $INTERLIS_LAB_REPO" >&2
  exit 1
fi

(cd "$INTERLIS_LAB_REPO" && npm run build)

if [[ ! -f "$INTERLIS_LAB_REPO/dist/interlis-lab.js" ]]; then
  echo "Missing built component: $INTERLIS_LAB_REPO/dist/interlis-lab.js" >&2
  exit 1
fi

if [[ ! -f "$INTERLIS_LAB_REPO/public/ili2c.jar" ]]; then
  echo "Missing ili2c.jar. Run npm run build:ili2c in $INTERLIS_LAB_REPO first." >&2
  exit 1
fi

for target in \
  "$THOTH_ROOT/thoth-blog/src/main/resources/site-assets/interlis-lab" \
  "$THOTH_ROOT/thoth-biblios/src/main/resources/site-assets/interlis-lab"
do
  mkdir -p "$target"
  cp "$INTERLIS_LAB_REPO/dist/interlis-lab.js" "$target/interlis-lab.js"
  cp "$INTERLIS_LAB_REPO/public/ili2c.jar" "$target/ili2c.jar"
done

echo "Updated bundled Interlis Lab assets from $INTERLIS_LAB_REPO"
