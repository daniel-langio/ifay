package vendredi.soir.ifay.mvola;

import static java.net.http.HttpClient.newHttpClient;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * MVola's merchant payment API - built from general knowledge of the MVola API v1 shape (OAuth2
 * client-credentials token, then a "merchantpay" transaction type: initiate a payment request
 * that prompts the payer's phone for approval, then poll a status endpoint until it resolves),
 * cross-checked against a second, independent implementation (mandaniainarandriambinintsoa/
 * paidmada-mobile-money on GitHub, MIT-licensed, references the real MVola devportal docs) which
 * agreed on the token/initiate flow shape but corrected two real mistakes this had: the status
 * endpoint has no "/status/" path segment, and the initiate payload needs
 * originalTransactionReference + a metadata array. Still NOT verified against a real MVola
 * sandbox - treat every constant/path below as a well-corroborated starting point, not a
 * guarantee.
 */
@Slf4j
@Component
public class MvolaApiClient {
  private static final ObjectMapper OM = new ObjectMapper();

  static {
    OM.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    OM.setSerializationInclusion(JsonInclude.Include.NON_NULL);
  }

  private final MvolaConfig config;

  public MvolaApiClient(MvolaConfig config) {
    this.config = config;
  }

  /** Fetches a fresh OAuth2 access token - not cached, verification happens infrequently. */
  public String fetchAccessToken() {
    var basicAuth =
        Base64.getEncoder()
            .encodeToString(
                (config.getConsumerKey() + ":" + config.getConsumerSecret()).getBytes(UTF_8));
    var httpRequest =
        HttpRequest.newBuilder()
            .uri(URI.create(config.getApiUrl() + "/token"))
            .header("Authorization", "Basic " + basicAuth)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString("grant_type=client_credentials&scope=EXT_INT_MVOLA_SCOPE"))
            .build();

    var response = send(httpRequest);
    return readBody(response, MvolaTokenResponse.class).access_token();
  }

  /**
   * Asks MVola to prompt {@code payerMsisdn} to approve paying {@code amount} Ar. Returns
   * immediately with a {@code serverCorrelationId} - the transaction is still pending, poll
   * {@link #statusOf} to find out how it resolves.
   */
  public MvolaInitiateResponse initiatePayment(String payerMsisdn, long amount, String ourReference) {
    var token = fetchAccessToken();
    var correlationId = UUID.randomUUID().toString();
    var description = "ifay payment";
    var body =
        Map.of(
            "amount", String.valueOf(amount),
            "currency", "Ar",
            "descriptionText", description.substring(0, Math.min(description.length(), 40)),
            "requestingOrganisationTransactionReference", ourReference,
            "requestDate", Instant.now().toString(),
            "originalTransactionReference", ourReference,
            "debitParty", List.of(Map.of("key", "msisdn", "value", payerMsisdn)),
            "creditParty", List.of(Map.of("key", "msisdn", "value", config.getMerchantMsisdn())),
            "metadata", List.of(Map.of("key", "partnerName", "value", config.getPartnerName())));

    HttpRequest httpRequest;
    try {
      httpRequest =
          HttpRequest.newBuilder()
              .uri(URI.create(config.getApiUrl() + "/mvola/mm/transactions/type/merchantpay/1.0.0"))
              .header("Authorization", "Bearer " + token)
              .header("Version", "1.0")
              .header("X-CorrelationID", correlationId)
              .header("UserLanguage", "FR")
              .header("UserAccountIdentifier", "msisdn;" + config.getMerchantMsisdn())
              .header("partnerName", config.getPartnerName())
              .header("Content-Type", "application/json")
              .header("Cache-Control", "no-cache")
              .POST(HttpRequest.BodyPublishers.ofString(OM.writeValueAsString(body)))
              .build();
    } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
      throw new MvolaApiException("Could not serialize MVola request: " + e.getMessage());
    }

    var response = send(httpRequest);
    return readBody(response, MvolaInitiateResponse.class);
  }

  public MvolaTransactionStatusResponse statusOf(String serverCorrelationId) {
    var token = fetchAccessToken();
    var httpRequest =
        HttpRequest.newBuilder()
            .uri(
                URI.create(
                    config.getApiUrl()
                        + "/mvola/mm/transactions/type/merchantpay/1.0.0/"
                        + serverCorrelationId))
            .header("Authorization", "Bearer " + token)
            .header("Version", "1.0")
            .header("X-CorrelationID", UUID.randomUUID().toString())
            .header("UserLanguage", "FR")
            .header("UserAccountIdentifier", "msisdn;" + config.getMerchantMsisdn())
            .header("partnerName", config.getPartnerName())
            .header("Cache-Control", "no-cache")
            .GET()
            .build();

    var response = send(httpRequest);
    return readBody(response, MvolaTransactionStatusResponse.class);
  }

  private HttpResponse<String> send(HttpRequest httpRequest) {
    try (var httpClient = newHttpClient()) {
      log.info("Calling MVola: {}", httpRequest.uri());
      var response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
      log.info("HTTP status from MVola was: {}", response.statusCode());
      if (response.statusCode() / 100 != 2) {
        throw new MvolaApiException("Response from MVola was not 2xx: " + response);
      }
      return response;
    } catch (IOException | InterruptedException e) {
      throw new MvolaApiException("Could not reach MVola: " + e.getMessage());
    }
  }

  private <T> T readBody(HttpResponse<String> response, Class<T> type) {
    try {
      return OM.readValue(response.body(), type);
    } catch (IOException e) {
      throw new MvolaApiException("Could not parse MVola response: " + e.getMessage());
    }
  }
}
