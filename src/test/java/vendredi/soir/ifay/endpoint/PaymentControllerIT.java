package vendredi.soir.ifay.endpoint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import vendredi.soir.ifay.conf.FacadeIT;
import vendredi.soir.ifay.endpoint.PaymentController.CreatePaymentRequest;
import vendredi.soir.ifay.endpoint.PaymentController.PaymentResponse;
import vendredi.soir.ifay.model.VerificationStatus;
import vendredi.soir.ifay.mvola.MvolaApiClient;
import vendredi.soir.ifay.mvola.MvolaInitiateResponse;
import vendredi.soir.ifay.mvola.MvolaTransactionStatusResponse;

class PaymentControllerIT extends FacadeIT {

  @Autowired private TestRestTemplate rest;
  @MockBean private MvolaApiClient mvolaApiClient;

  private static final String API_KEY = "dev-only-insecure-default-change-me";

  @Test
  void a_payment_stays_verifying_until_mvola_reports_it_completed() {
    when(mvolaApiClient.initiatePayment(anyString(), anyLong(), anyString()))
        .thenReturn(new MvolaInitiateResponse("pending", "server-correlation-1"));

    var created =
        rest.postForEntity(
            "/payments",
            authorized(new CreatePaymentRequest("user1", "0341234567", 1000L, "karata-deposit")),
            PaymentResponse.class);
    assertEquals(HttpStatus.CREATED, created.getStatusCode());
    assertEquals(VerificationStatus.VERIFYING, created.getBody().status());
    var id = created.getBody().id();

    when(mvolaApiClient.statusOf("server-correlation-1"))
        .thenReturn(new MvolaTransactionStatusResponse("pending", null, null, null));
    var stillVerifying =
        rest.exchange(
            "/payments/" + id,
            org.springframework.http.HttpMethod.GET,
            authorized(null),
            PaymentResponse.class);
    assertEquals(VerificationStatus.VERIFYING, stillVerifying.getBody().status());

    when(mvolaApiClient.statusOf("server-correlation-1"))
        .thenReturn(new MvolaTransactionStatusResponse("completed", null, "server-correlation-1", "1000"));
    var succeeded =
        rest.exchange(
            "/payments/" + id,
            org.springframework.http.HttpMethod.GET,
            authorized(null),
            PaymentResponse.class);
    assertEquals(VerificationStatus.SUCCEEDED, succeeded.getBody().status());
    assertEquals(1000L, succeeded.getBody().confirmedAmount());

    // Once resolved, re-checking must not call MVola again.
    rest.exchange(
        "/payments/" + id, org.springframework.http.HttpMethod.GET, authorized(null), PaymentResponse.class);
    verify(mvolaApiClient, org.mockito.Mockito.times(2)).statusOf("server-correlation-1");
  }

  @Test
  void rejects_requests_with_the_wrong_api_key() {
    var headers = new HttpHeaders();
    headers.set("X-Api-Key", "wrong-key");
    var req =
        new HttpEntity<>(new CreatePaymentRequest("user1", "0341234567", 1000L, null), headers);
    var resp = rest.postForEntity("/payments", req, String.class);
    assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
  }

  @Test
  void rejects_a_non_positive_amount() {
    when(mvolaApiClient.initiatePayment(anyString(), anyLong(), anyString()))
        .thenReturn(new MvolaInitiateResponse("pending", "unused"));
    var resp =
        rest.postForEntity(
            "/payments", authorized(new CreatePaymentRequest("user1", "0341234567", 0L, null)), String.class);
    assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
  }

  private <T> HttpEntity<T> authorized(T body) {
    var headers = new HttpHeaders();
    headers.set("X-Api-Key", API_KEY);
    return new HttpEntity<>(body, headers);
  }
}
