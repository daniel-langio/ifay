# ifay

A small payment service wrapping MVola (Telma's Madagascar mobile money) merchant payments,
built to be called by other apps (starting with the [vendredi.soir.karata](https://github.com/daniel-langio/vendredi.soir.karata)
poker app's chip deposit flow) rather than each one integrating MVola directly.

Modeled after [hei-school/vola](https://github.com/hei-school/vola)'s Orange Money integration
(a `Payment` domain with a derived - never stored - verification status), but simpler: MVola's
API supports a merchant-initiated payment request (prompt the payer's phone to approve, then poll
a status endpoint), so there's no need for vola's batch/event-driven reconciliation machinery -
`GET /payments/{id}` just re-checks MVola's status endpoint live, lazily, on each call.

## ⚠️ Status: not yet usable with real money

- The MVola merchant account is still being set up - nothing here has been tested against a real
  MVola sandbox or production endpoint.
- The exact API shape in `mvola/` (endpoint paths, header names, request/response fields) is
  built from general knowledge of MVola's API, **not verified against their real API Developer
  docs**. Once sandbox credentials exist, cross-check every constant in `MvolaApiClient` and the
  two response records against the real docs before relying on this for anything real.

## API

All endpoints require an `X-Api-Key` header (see `IFAY_API_KEY`).

- `POST /payments` - `{payerReference, payerMsisdn, amount, scope}` → asks MVola to prompt
  `payerMsisdn` to approve paying `amount` Ar. Returns `{id, status: "VERIFYING", ...}`
  immediately - the payer hasn't necessarily approved yet.
- `GET /payments/{id}` - polls MVola's status fresh and returns the current
  `{id, status, amountRequested, confirmedAmount}`. `status` is one of `VERIFYING`, `SUCCEEDED`,
  `FAILED`. Once resolved, this stops calling MVola (safe to poll repeatedly).

## Config (env vars)

| Var | Purpose | Default |
|---|---|---|
| `DATABASE_URL` | Postgres connection (`postgres://...` or `jdbc:postgresql://...`) | local dev fallback |
| `IFAY_API_KEY` | Shared secret required on every request | insecure dev default - **must** be overridden in any real deployment |
| `MVOLA_API_URL` | MVola API base URL | `https://devapi.mvola.mg` (sandbox) |
| `MVOLA_CONSUMER_KEY` / `MVOLA_CONSUMER_SECRET` | OAuth2 client credentials from the MVola developer portal | none |
| `MVOLA_PARTNER_NAME` | Registered partner name | `ifay` |
| `MVOLA_MERCHANT_MSISDN` | The merchant's own MVola phone number (receives payments) | none |

## Local dev

```
DATABASE_URL=jdbc:postgresql://localhost:5442/postgres ./gradlew bootRun
```

Tests use Testcontainers (a real Postgres) for the integration test and mock `MvolaApiClient` -
no real MVola credentials needed to run the test suite.
