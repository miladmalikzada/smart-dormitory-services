#!/usr/bin/env bash
# Starts the website + REST API with the SQLite database.  Usage: bash scripts/run-web.sh [port]
set -e
cd "$(dirname "$0")/.."
PORT="${1:-8080}"
[ -f .env ] || python3 scripts/setup_env.py
mvn -q compile exec:java -Dexec.args="--web --port=${PORT} --storage=sqlite --data=data"
