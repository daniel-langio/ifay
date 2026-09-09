package vendredi.soir.ifay.mvola;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** OAuth2 client-credentials token response from MVola's {@code /token} endpoint. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MvolaTokenResponse(String access_token, String token_type, long expires_in) {}
