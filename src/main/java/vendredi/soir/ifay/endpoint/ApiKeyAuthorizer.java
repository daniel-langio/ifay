package vendredi.soir.ifay.endpoint;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import vendredi.soir.ifay.endpoint.exception.UnauthorizedException;

/**
 * Two distinct shared secrets - client apps (e.g. karata) submitting claims, and verifier apps
 * (e.g. porofo's SMS reader) submitting reports, are different trust levels and shouldn't share
 * a key: a verifier's key can make ifay believe real money arrived, a client's key can only ever
 * assert a claim that still needs a matching report to mean anything.
 */
@Component
public class ApiKeyAuthorizer {
  private final String clientApiKey;
  private final String verifierApiKey;

  public ApiKeyAuthorizer(
      @Value("${ifay.client-api-key:dev-only-insecure-client-key-change-me}") String clientApiKey,
      @Value("${ifay.verifier-api-key:dev-only-insecure-verifier-key-change-me}") String verifierApiKey) {
    this.clientApiKey = clientApiKey;
    this.verifierApiKey = verifierApiKey;
  }

  public void acceptClient(String apiKey) {
    if (apiKey == null || !apiKey.equals(clientApiKey)) {
      throw new UnauthorizedException("Invalid client API key");
    }
  }

  public void acceptVerifier(String apiKey) {
    if (apiKey == null || !apiKey.equals(verifierApiKey)) {
      throw new UnauthorizedException("Invalid verifier API key");
    }
  }
}
