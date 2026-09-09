package vendredi.soir.ifay.endpoint;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import vendredi.soir.ifay.endpoint.exception.UnauthorizedException;

/** A single shared-secret API key, checked on every request - one client (karata) for now. */
@Component
public class ApiKeyAuthorizer {
  private final String expectedApiKey;

  public ApiKeyAuthorizer(@Value("${ifay.api.key:dev-only-insecure-default-change-me}") String expectedApiKey) {
    this.expectedApiKey = expectedApiKey;
  }

  public void accept(String apiKey) {
    if (apiKey == null || !apiKey.equals(expectedApiKey)) {
      throw new UnauthorizedException("Invalid API key");
    }
  }
}
