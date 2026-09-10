package vendredi.soir.ifay.model;

import java.time.Instant;
import lombok.Builder;

/**
 * A payment is created the moment *either* side reports it - a sender's claim ("I paid this psp
 * reference") or a verifier's report ("I observed this psp reference") - whichever arrives first.
 * Since either can arrive first, every claim-side and report-side field is nullable until that
 * side actually shows up. {@code verifiedAt} is set the moment both agree on the same
 * {@code (type, pspRef)}: that's the one thing that must match between them - once verified,
 * {@code confirmedAmount} (the verifier's report) is authoritative, never {@code claimedAmount}.
 *
 * This is a materialized read-model, kept in sync with the append-only, hash-chained
 * {@code PaymentEvent} log (see {@link PaymentEventType}) - the event log is the actual source of
 * truth for tamper-evidence, this record exists purely so reads stay simple and fast.
 *
 * {@code verificationType} records *how* verification happened - an opaque string a verifier app
 * supplies on report (e.g. "SMS_AUTO"), or "MANUAL" when a receiver verifies without a report at
 * all. ifay doesn't validate or enumerate values here; the verifier/receiver side owns the
 * vocabulary.
 */
@Builder(toBuilder = true)
public record Payment(
    String id,
    String pspRef,
    Provider type,
    Party sender,
    Party receiver,
    Long claimedAmount,
    Long confirmedAmount,
    Verifier verifier,
    Instant sentAt,
    Instant receivedAt,
    Instant verifiedAt,
    String verificationType) {

  public boolean isVerified() {
    return verifiedAt != null;
  }

  public Long effectiveAmount() {
    return confirmedAmount != null ? confirmedAmount : claimedAmount;
  }
}
