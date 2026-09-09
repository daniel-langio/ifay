package vendredi.soir.ifay.mvola;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Response from polling a transaction's status. {@code status} is expected to be one of
 * "pending", "completed", "failed" - verify the exact values against MVola's real API docs once
 * sandbox access is available, this is built from general knowledge of the MVola API shape, not
 * a confirmed spec.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MvolaTransactionStatusResponse(
    String status, String transactionReference, String amount) {

  public boolean isCompleted() {
    return "completed".equalsIgnoreCase(status);
  }

  public boolean isFailed() {
    return "failed".equalsIgnoreCase(status);
  }
}
