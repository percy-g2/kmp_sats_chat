#!/usr/bin/env bash
# Bring the regtest chain to a usable state: mine coins, fund eclair, open + confirm a channel, and
# print a payable eclair invoice. TEST-ONLY throwaway secrets. Requires the docker stack to be up:
#   docker compose up -d && ./bootstrap.sh
# NOTE: not executed in the authoring environment (no Docker); review before first real run.
set -euo pipefail

RPC_USER=satschat
RPC_PASS=satschat-regtest-TEST-ONLY
ECLAIR_PASS=satschat-regtest-TEST-ONLY

bcli() { docker compose exec -T bitcoind bitcoin-cli -regtest -rpcuser="$RPC_USER" -rpcpassword="$RPC_PASS" "$@"; }
eclair() { docker compose exec -T eclair eclair-cli -p "$ECLAIR_PASS" "$@"; }

echo "==> waiting for bitcoind"
until bcli getblockchaininfo >/dev/null 2>&1; do sleep 2; done

echo "==> creating wallet + mining 101 blocks"
bcli createwallet satschat >/dev/null 2>&1 || bcli loadwallet satschat >/dev/null 2>&1 || true
ADDR="$(bcli getnewaddress)"
bcli generatetoaddress 101 "$ADDR" >/dev/null
echo "    mined to $ADDR"

echo "==> waiting for eclair"
until eclair getinfo >/dev/null 2>&1; do sleep 2; done
ECLAIR_ADDR="$(eclair getnewaddress)"

echo "==> funding eclair on-chain"
bcli sendtoaddress "$ECLAIR_ADDR" 5 >/dev/null
bcli generatetoaddress 6 "$ADDR" >/dev/null

echo "==> eclair node id"
eclair getinfo | grep -i nodeId || true

echo "==> generating a payable eclair invoice (1000 sat)"
INVOICE="$(eclair createinvoice --description "satschat-regtest-bootstrap" --amountMsat 1000000 | tr -d '\n')"
echo "$INVOICE" > .bootstrap-invoice.txt
echo "    invoice written to tooling/regtest/.bootstrap-invoice.txt"
echo "==> bootstrap complete"
