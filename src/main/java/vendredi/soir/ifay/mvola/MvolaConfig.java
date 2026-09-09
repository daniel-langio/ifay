package vendredi.soir.ifay.mvola;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * All MVola credentials/identifiers, read from env vars with sandbox-safe defaults so the app
 * still starts locally/in CI without real credentials configured - MvolaApiClient calls will just
 * fail loudly at request time in that case, which is fine since nothing calls MVola in tests.
 */
@Getter
@Component
public class MvolaConfig {
  private final String apiUrl;
  private final String consumerKey;
  private final String consumerSecret;
  private final String partnerName;
  private final String merchantMsisdn;

  public MvolaConfig(
      @Value("${mvola.api.url:https://devapi.mvola.mg}") String apiUrl,
      @Value("${mvola.consumer.key:}") String consumerKey,
      @Value("${mvola.consumer.secret:}") String consumerSecret,
      @Value("${mvola.partner.name:ifay}") String partnerName,
      @Value("${mvola.merchant.msisdn:}") String merchantMsisdn) {
    this.apiUrl = apiUrl;
    this.consumerKey = consumerKey;
    this.consumerSecret = consumerSecret;
    this.partnerName = partnerName;
    this.merchantMsisdn = merchantMsisdn;
  }
}
