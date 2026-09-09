# ifay

A generalized payment recording/validation API: something claims a payment happened (a sender,
e.g. [vendredi.soir.karata](https://github.com/daniel-langio/vendredi.soir.karata)'s chip deposit
flow), something else independently reports that it actually observed the money move (a
verifier), and ifay matches the two by `(type, pspRef)` and marks the payment verified once both
sides agree. Either side can arrive first - a claim with no report yet, or a report with no claim
yet, both just sit `PENDING` until the other side shows up. There's no timeout: a payment stays
pending indefinitely until a matching report confirms it.

Verifiers are trusted to have actually observed the payment themselves (e.g. reading their own
device's SMS) before calling ifay - ifay has no way to independently confirm that, so a report is
only ever as trustworthy as the verifier holding the verifier API key. The first verifier is
[porofo](https://github.com/daniel-langio/porofo) (Malagasy for "proof"), a separate app doing
on-device SMS parsing for MVola/Orange Money confirmation messages - deliberately **not** part of
this repo, since trusting a raw SMS string sent over the network would mean trusting whoever holds
the API key to not have fabricated it; the actual verification (matching the SMS to a real
on-device message) has to happen on the verifier's own device.

## API

Every endpoint (except `/ledger/anchor`, see below) requires an `X-Api-Key` header - client apps
(submitting claims) and verifier apps (submitting reports) use separate keys, since they
represent different trust levels.

- `POST /payments/claims` - `{senderPhone, receiverPhone, amount, type, pspRef}` (client API key,
  phone numbers in E.164, e.g. `+261341234567`) → records a payer's claim that they paid
  `pspRef`. Returns `{id, status, amount}`, `status` one of `PENDING`/`VERIFIED`. `senderPhone`/
  `receiverPhone` resolve to a `Party` entity looked up (or created) by phone number - the same
  number reused across payments always resolves to the same `Party`.
- `GET /payments/claims/{id}` - (client API key) current status of a previously created claim.
- `POST /payments/reports` - `{type, pspRef, amount, verifier: {appId, version, revision}}`
  (verifier API key, `revision` optional) → records that the named verifier app directly observed
  a payment matching `pspRef`. Returns the same `{id, status, amount}` shape; `amount` here is
  always the verifier-confirmed amount once verified, never the claimed one.

`type` is one of `MVOLA`, `ORANGE_MONEY`, `AIRTEL_MONEY`. `pspRef` is normalized
(`trim().toUpperCase()`) on both the claim and report side before matching, so case differences
between how a payer types their own reference and how a verifier extracts it from a raw message
don't cause a false miss.

## Tamper-evidence

Every claim/report is also appended as an immutable event to a hash-chained, append-only ledger
(`eventHash = hash(event content + previous event's hash)`) - this, not the `Payment` row above
(which is just a materialized read-model kept in sync for fast/simple reads), is the actual
source of truth for "this record wasn't altered". All ledger writes are serialized through a
single locked `chain_tip` row, so there's no concurrent-insert race to reason about separately.

- `GET /ledger/anchor` - **public, no API key** - the current chain tip `{sequence, tipHash}`.
  Public on purpose: proving a payment wasn't altered only means something if someone other than
  ifay itself can independently fetch and check this.
- `GET /ledger/events?type=...&pspRef=...` - (client API key) the ordered event history for one
  payment, each with its own hash and its predecessor's hash, resolved sender/receiver phone
  numbers and verifier info included - what you'd hand a third party as evidence.

A hash chain alone only proves internal consistency - anyone with DB write access could
regenerate a fake one from scratch. The `Anchor Ledger` GitHub Actions workflow
(`.github/workflows/anchor-ledger.yml`) closes that gap by committing the chain tip to
`ledger/anchors.log` in this repo every day: a tip that was already pushed to git history
yesterday can't be quietly rewound today without visibly disagreeing with that commit.

## Config (env vars)

| Var | Purpose | Default |
|---|---|---|
| `DATABASE_URL` | Postgres connection (`postgres://...` or `jdbc:postgresql://...`) | local dev fallback |
| `IFAY_CLIENT_API_KEY` | Shared secret for client apps submitting claims | insecure dev default - **must** be overridden in any real deployment |
| `IFAY_VERIFIER_API_KEY` | Shared secret for verifier apps submitting reports | insecure dev default - **must** be overridden in any real deployment |

## Local dev

```
DATABASE_URL=jdbc:postgresql://localhost:5442/postgres ./gradlew bootRun
```

Tests use Testcontainers (a real Postgres) for the integration tests - no external credentials
needed to run the test suite.

## License

Copyright Daniel Langio. Licensed under the [PolyForm Noncommercial License 1.0.0](LICENSE),
plus additional terms in the same file (no use to train/fine-tune AI models). Free for
noncommercial use; contact langio.tehiniavo@gmail.com for a commercial license.
