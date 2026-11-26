#!/usr/bin/env bash
# Starts the website + REST API with the SQLite database.  Usage: bash scripts/run-web.sh [port]
set -e
PORT="${1:-8080}"
mvn -q compile exec:java -Dexec.args="--web --port=${PORT} --storage=sqlite --data=data"
