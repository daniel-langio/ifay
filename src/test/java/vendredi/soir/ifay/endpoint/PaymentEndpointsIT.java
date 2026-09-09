package vendredi.soir.ifay.endpoint;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import vendredi.soir.ifay.conf.FacadeIT;
import vendredi.soir.ifay.model.Provider;

class PaymentEndpointsIT extends FacadeIT {
  private static final String CLIENT_API_KEY = "dev-only-insecure-client-key-change-me";
  private static final String VERIFIER_API_KEY = "dev-only-insecure-verifier-key-change-me";

  @Test
  void claim_then_report_verifies_the_payment() {
    String pspRef = "claim-then-report-ref";

    ResponseEntity<PaymentController.PaymentResponse> claimResponse =
        rest.exchange(
            "/payments/claims",
            org.springframework.http.HttpMethod.POST,
            new HttpEntity<>(
                new PaymentController.CreateClaimRequest(
                    "alice", "bob", 1000L, Provider.MVOLA, pspRef),
                clientHeaders()),
            PaymentController.PaymentResponse.class);

    assertThat(claimResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(claimResponse.getBody().status()).isEqualTo("PENDING");
    String paymentId = claimResponse.getBody().id();

    ResponseEntity<PaymentController.PaymentResponse> reportResponse =
        rest.exchange(
            "/payments/reports",
            org.springframework.http.HttpMethod.POST,
            new HttpEntity<>(
                new PaymentReportController.CreateReportRequest(
                    Provider.MVOLA, pspRef, 1000L, "porofo", "porofo-v1"),
                verifierHeaders()),
            PaymentController.PaymentResponse.class);

    assertThat(reportResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(reportResponse.getBody().status()).isEqualTo("VERIFIED");
    assertThat(reportResponse.getBody().id()).isEqualTo(paymentId);
    assertThat(reportResponse.getBody().amount()).isEqualTo(1000L);
  }

  @Test
  void report_then_claim_verifies_the_payment() {
    String pspRef = "report-then-claim-ref";

    ResponseEntity<PaymentController.PaymentResponse> reportResponse =
        rest.exchange(
            "/payments/reports",
            org.springframework.http.HttpMethod.POST,
            new HttpEntity<>(
                new PaymentReportController.CreateReportRequest(
                    Provider.ORANGE_MONEY, pspRef, 2500L, "porofo", "porofo-v1"),
                verifierHeaders()),
            PaymentController.PaymentResponse.class);

    assertThat(reportResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(reportResponse.getBody().status()).isEqualTo("PENDING");

    ResponseEntity<PaymentController.PaymentResponse> claimResponse =
        rest.exchange(
            "/payments/claims",
            org.springframework.http.HttpMethod.POST,
            new HttpEntity<>(
                new PaymentController.CreateClaimRequest(
                    "alice", "bob", 2500L, Provider.ORANGE_MONEY, pspRef.toUpperCase()),
                clientHeaders()),
            PaymentController.PaymentResponse.class);

    assertThat(claimResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(claimResponse.getBody().status()).isEqualTo("VERIFIED");
  }

  @Test
  void claim_rejects_verifier_key() {
    ResponseEntity<String> response =
        rest.exchange(
            "/payments/claims",
            org.springframework.http.HttpMethod.POST,
            new HttpEntity<>(
                new PaymentController.CreateClaimRequest(
                    "alice", "bob", 1000L, Provider.MVOLA, "some-ref"),
                clientHeaders()),
            String.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

    ResponseEntity<String> reportWithClientKey =
        rest.exchange(
            "/payments/reports",
            org.springframework.http.HttpMethod.POST,
            new HttpEntity<>(
                new PaymentReportController.CreateReportRequest(
                    Provider.MVOLA, "some-other-ref", 1000L, "porofo", "porofo-v1"),
                clientHeaders()),
            String.class);
    assertThat(reportWithClientKey.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  private HttpHeaders clientHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.set("X-Api-Key", CLIENT_API_KEY);
    return headers;
  }

  private HttpHeaders verifierHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.set("X-Api-Key", VERIFIER_API_KEY);
    return headers;
  }
}
