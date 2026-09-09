package vendredi.soir.ifay.model;

import java.time.Instant;
import lombok.Builder;

/**
 * A single MVola merchant-initiated payment request: ifay asks MVola to prompt {@code
 * payerMsisdn} to approve paying {@code amountRequested}, then polls MVola's status endpoint
 * (see MvolaApiClient) using {@code serverCorrelationId} until it resolves. Status is always
 * derived, never stored directly - {@code confirmedAmount} is null until MVola reports the
 * transaction as completed, matching what it actually confirms was paid (which should equal
 * amountRequested, but the confirmed figure is the authoritative one for crediting anything).
 */
@Builder(toBuilder = true)
public record Payment(
    String id,
    String payerReference,
    String payerMsisdn,
    long amountRequested,
    Long confirmedAmount,
    boolean pspReportedFailure,
    String serverCorrelationId,
    String scope,
    Instant creationInstant,
    Instant lastVerificationInstant,
    int verificationAttemptNb) {

  public static final int MAX_VERIFICATION_ATTEMPT_NB = 20;

  public boolean hasNoMoreVerificationAttempts() {
    return verificationAttemptNb > MAX_VERIFICATION_ATTEMPT_NB;
  }

  public VerificationStatus getVerificationStatus() {
    if (confirmedAmount != null) {
      return VerificationStatus.SUCCEEDED;
    }
    if (pspReportedFailure || hasNoMoreVerificationAttempts()) {
      return VerificationStatus.FAILED;
    }
    return VerificationStatus.VERIFYING;
  }
}
