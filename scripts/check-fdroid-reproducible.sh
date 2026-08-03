#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

fdroiddata=${FDROIDDATA_DIR:-../fdroiddata}
metadata=metadata/com.zaelio.app.yml

[[ -d $fdroiddata/.git ]] || { echo "Set FDROIDDATA_DIR to your fdroiddata checkout" >&2; exit 1; }
command -v fdroid >/dev/null || { echo "fdroid not found; activate your fdroidserver venv" >&2; exit 1; }

cp docs/fdroiddata/com.zaelio.app.yml "$fdroiddata/$metadata"
(
    cd "$fdroiddata"
    fdroid readmeta
    fdroid lint com.zaelio.app
    fdroid build -v -l com.zaelio.app
)
