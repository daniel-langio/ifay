package vendredi.soir.ifay.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class PaymentTest {

  private Payment.PaymentBuilder base() {
    return Payment.builder()
        .id("p1")
        .payerReference("user1")
        .payerMsisdn("0341234567")
        .amountRequested(1000L)
        .serverCorrelationId("corr1")
        .creationInstant(Instant.now())
        .verificationAttemptNb(0);
  }

  @Test
  void verifying_while_not_yet_confirmed_and_attempts_remain() {
    var payment = base().build();
    assertEquals(VerificationStatus.VERIFYING, payment.getVerificationStatus());
  }

  @Test
  void succeeded_once_a_confirmed_amount_is_present() {
    var payment = base().confirmedAmount(1000L).build();
    assertEquals(VerificationStatus.SUCCEEDED, payment.getVerificationStatus());
  }

  @Test
  void failed_once_the_psp_explicitly_reports_failure() {
    var payment = base().pspReportedFailure(true).build();
    assertEquals(VerificationStatus.FAILED, payment.getVerificationStatus());
  }

  @Test
  void failed_once_verification_attempts_are_exhausted() {
    var payment = base().verificationAttemptNb(Payment.MAX_VERIFICATION_ATTEMPT_NB + 1).build();
    assertEquals(VerificationStatus.FAILED, payment.getVerificationStatus());
  }

  @Test
  void a_confirmed_amount_wins_even_if_attempts_were_exhausted() {
    // Shouldn't normally happen (confirmation stops further polling), but confirmation is the
    // stronger signal if it's ever present alongside exhausted attempts.
    var payment =
        base()
            .confirmedAmount(1000L)
            .verificationAttemptNb(Payment.MAX_VERIFICATION_ATTEMPT_NB + 1)
            .build();
    assertEquals(VerificationStatus.SUCCEEDED, payment.getVerificationStatus());
  }
}
