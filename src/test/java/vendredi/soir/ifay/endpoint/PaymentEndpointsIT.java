package vendredi.soir.ifay.endpoint;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
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
            HttpMethod.POST,
            new HttpEntity<>(
                new PaymentController.CreateClaimRequest(
                    "+261340000001", "+261340000002", 1000L, Provider.MVOLA, pspRef),
                clientHeaders()),
            PaymentController.PaymentResponse.class);

    assertThat(claimResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(claimResponse.getBody().status()).isEqualTo("PENDING");
    String paymentId = claimResponse.getBody().id();

    ResponseEntity<PaymentController.PaymentResponse> reportResponse =
        rest.exchange(
            "/payments/reports",
            HttpMethod.POST,
            new HttpEntity<>(
                new PaymentReportController.CreateReportRequest(
                    Provider.MVOLA,
                    pspRef,
                    1000L,
                    new PaymentReportController.VerifierInfo("mg.langio.porofo", "1.0.0", null)),
                verifierHeaders()),
            PaymentController.PaymentResponse.class);

    assertThat(reportResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(reportResponse.getBody().status()).isEqualTo("VERIFIED");
    assertThat(reportResponse.getBody().id()).isEqualTo(paymentId);
    assertThat(reportResponse.getBody().amount()).isEqualTo(1000L);

    ResponseEntity<LedgerController.EventResponse[]> events =
        rest.exchange(
            "/ledger/events?type=MVOLA&pspRef=" + pspRef,
            HttpMethod.GET,
            new HttpEntity<>(clientHeaders()),
            LedgerController.EventResponse[].class);
    assertThat(events.getBody()).hasSize(2);
    assertThat(events.getBody()[0].eventType()).isEqualTo("CLAIM_RECORDED");
    assertThat(events.getBody()[0].senderPhone()).isEqualTo("+261340000001");
    assertThat(events.getBody()[1].eventType()).isEqualTo("REPORT_RECORDED");
    assertThat(events.getBody()[1].previousEventHash()).isEqualTo(events.getBody()[0].eventHash());
  }

  @Test
  void report_then_claim_verifies_the_payment() {
    String pspRef = "report-then-claim-ref";

    ResponseEntity<PaymentController.PaymentResponse> reportResponse =
        rest.exchange(
            "/payments/reports",
            HttpMethod.POST,
            new HttpEntity<>(
                new PaymentReportController.CreateReportRequest(
                    Provider.ORANGE_MONEY,
                    pspRef,
                    2500L,
                    new PaymentReportController.VerifierInfo("mg.langio.porofo", "1.0.0", null)),
                verifierHeaders()),
            PaymentController.PaymentResponse.class);

    assertThat(reportResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(reportResponse.getBody().status()).isEqualTo("PENDING");

    ResponseEntity<PaymentController.PaymentResponse> claimResponse =
        rest.exchange(
            "/payments/claims",
            HttpMethod.POST,
            new HttpEntity<>(
                new PaymentController.CreateClaimRequest(
                    "+261340000001", "+261340000002", 2500L, Provider.ORANGE_MONEY, pspRef.toUpperCase()),
                clientHeaders()),
            PaymentController.PaymentResponse.class);

    assertThat(claimResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(claimResponse.getBody().status()).isEqualTo("VERIFIED");
  }

  @Test
  void reusing_a_phone_number_resolves_to_the_same_party() {
    String pspRefA = "same-party-ref-a";
    String pspRefB = "same-party-ref-b";
    String phone = "+261340000099";

    rest.exchange(
        "/payments/claims",
        HttpMethod.POST,
        new HttpEntity<>(
            new PaymentController.CreateClaimRequest(
                phone, "+261340000002", 1000L, Provider.MVOLA, pspRefA),
            clientHeaders()),
        PaymentController.PaymentResponse.class);
    rest.exchange(
        "/payments/claims",
        HttpMethod.POST,
        new HttpEntity<>(
            new PaymentController.CreateClaimRequest(
                phone, "+261340000003", 1000L, Provider.MVOLA, pspRefB),
            clientHeaders()),
        PaymentController.PaymentResponse.class);

    ResponseEntity<LedgerController.EventResponse[]> eventsA =
        rest.exchange(
            "/ledger/events?type=MVOLA&pspRef=" + pspRefA,
            HttpMethod.GET,
            new HttpEntity<>(clientHeaders()),
            LedgerController.EventResponse[].class);
    ResponseEntity<LedgerController.EventResponse[]> eventsB =
        rest.exchange(
            "/ledger/events?type=MVOLA&pspRef=" + pspRefB,
            HttpMethod.GET,
            new HttpEntity<>(clientHeaders()),
            LedgerController.EventResponse[].class);

    assertThat(eventsA.getBody()[0].senderPhone()).isEqualTo(phone);
    assertThat(eventsB.getBody()[0].senderPhone()).isEqualTo(phone);
  }

  @Test
  void rejects_malformed_phone_number() {
    ResponseEntity<String> response =
        rest.exchange(
            "/payments/claims",
            HttpMethod.POST,
            new HttpEntity<>(
                new PaymentController.CreateClaimRequest(
                    "0340000001", "+261340000002", 1000L, Provider.MVOLA, "malformed-phone-ref"),
                clientHeaders()),
            String.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void claim_rejects_verifier_key() {
    ResponseEntity<String> response =
        rest.exchange(
            "/payments/claims",
            HttpMethod.POST,
            new HttpEntity<>(
                new PaymentController.CreateClaimRequest(
                    "+261340000001", "+261340000002", 1000L, Provider.MVOLA, "some-ref"),
                clientHeaders()),
            String.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

    ResponseEntity<String> reportWithClientKey =
        rest.exchange(
            "/payments/reports",
            HttpMethod.POST,
            new HttpEntity<>(
                new PaymentReportController.CreateReportRequest(
                    Provider.MVOLA,
                    "some-other-ref",
                    1000L,
                    new PaymentReportController.VerifierInfo("mg.langio.porofo", "1.0.0", null)),
                clientHeaders()),
            String.class);
    assertThat(reportWithClientKey.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void anchor_is_public_and_advances_with_the_chain() {
    ResponseEntity<LedgerController.AnchorResponse> before =
        rest.getForEntity("/ledger/anchor", LedgerController.AnchorResponse.class);
    assertThat(before.getStatusCode()).isEqualTo(HttpStatus.OK);

    rest.exchange(
        "/payments/claims",
        HttpMethod.POST,
        new HttpEntity<>(
            new PaymentController.CreateClaimRequest(
                "+261340000001", "+261340000002", 1000L, Provider.MVOLA, "anchor-advance-ref"),
            clientHeaders()),
        PaymentController.PaymentResponse.class);

    ResponseEntity<LedgerController.AnchorResponse> after =
        rest.getForEntity("/ledger/anchor", LedgerController.AnchorResponse.class);
    assertThat(after.getBody().sequence()).isGreaterThan(before.getBody().sequence());
    assertThat(after.getBody().tipHash()).isNotEqualTo(before.getBody().tipHash());
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
