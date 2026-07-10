#!/usr/bin/env bash
# Quick liveness check of the regtest stack. Exits non-zero if any service is unreachable.
# NOTE: not executed in the authoring environment (no Docker); review before first real run.
set -euo pipefail

RPC_USER=satschat
RPC_PASS=satschat-regtest-TEST-ONLY
ECLAIR_PASS=satschat-regtest-TEST-ONLY

ok=0
check() { if "$@" >/dev/null 2>&1; then echo "OK   $*"; else echo "FAIL $*"; ok=1; fi; }

check docker compose exec -T bitcoind bitcoin-cli -regtest -rpcuser="$RPC_USER" -rpcpassword="$RPC_PASS" getblockchaininfo
check docker compose exec -T eclair eclair-cli -p "$ECLAIR_PASS" getinfo
# electrs: expect the Electrum port to be listening.
check docker compose exec -T electrs sh -c "nc -z 127.0.0.1 50001"

if [ "$ok" -eq 0 ]; then echo "healthcheck: all services up"; else echo "healthcheck: one or more services down" >&2; fi
exit "$ok"
