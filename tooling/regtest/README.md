# SatsChat regtest stack

Local Bitcoin/Lightning backend for dev + integration tests.

```bash
cd tooling/regtest
docker compose up -d
./bootstrap.sh      # mine 101 blocks, fund eclair, print a payable invoice
./healthcheck.sh    # verify all services are up
```

Services (see `docker-compose.yml`):

| Service   | Purpose                          | Port(s)              |
|-----------|----------------------------------|----------------------|
| bitcoind  | regtest chain                    | 18443 (RPC), 28332/3 |
| electrs   | Electrum server (chain access)   | 50001 (plaintext)    |
| eclair    | LSP / channel peer               | 8080 (API), 9735     |
| smp-server| our SMP relay (Phase 2, TODO)    | 5223 (TLS)           |

The app reaches these via `:core:config` when built with `-Penv=regtest`. On the Android emulator,
`127.0.0.1` is rewritten to `10.0.2.2` (see `AppConfig` / `localhostAlias`).

## Caveats

- **TEST-ONLY secrets.** Every credential/seed here is a throwaway. Never reuse them.
- **Image tags are indicative.** Pin/verify them before relying on this stack.
- **Not yet run.** This stack was authored without a Docker host; expect to shake out image tags and
  wiring on first `docker compose up`.
- **SMP relay is our own component.** Wire-compat with SimpleX is not a goal, so an upstream
  `smp-server` won't interoperate. The relay is uncommented once the reference implementation exists
  (see the plan's "DECISION NEEDED — SMP relay server").
