package vendredi.soir.ifay.mvola;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Response from initiating a merchant payment request - the transaction is still pending. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MvolaInitiateResponse(String status, String serverCorrelationId) {}
