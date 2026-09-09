package vendredi.soir.ifay.model;

import java.time.Instant;
import lombok.Builder;

/**
 * A payment is created the moment *either* side reports it - a sender's claim ("I paid this psp
 * reference") or a receiver's report ("I received this psp reference") - whichever arrives first.
 * Since either can arrive first, every claim-side and report-side field is nullable until that
 * side actually shows up. {@code verifiedAt} is set the moment both agree on the same
 * {@code (type, pspRef)}: that's the one thing that must match between them - once verified,
 * {@code confirmedAmount} (the receiver's report) is authoritative, never {@code claimedAmount}.
 *
 * {@code verifier}/{@code verifierRevision} record which verification mechanism (and which
 * version of its code) produced the match - this core is deliberately not tied to any one
 * verifier (e.g. porofo, the SMS-based one) so more can exist side by side later, each traceable
 * on every payment it confirmed.
 */
@Builder(toBuilder = true)
public record Payment(
    String id,
    String pspRef,
    Provider type,
    String sender,
    String receiver,
    Long claimedAmount,
    Long confirmedAmount,
    String verifier,
    String verifierRevision,
    Instant sentAt,
    Instant receivedAt,
    Instant verifiedAt) {

  public boolean isVerified() {
    return verifiedAt != null;
  }

  public Long effectiveAmount() {
    return confirmedAmount != null ? confirmedAmount : claimedAmount;
  }
}
