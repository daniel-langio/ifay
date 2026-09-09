package vendredi.soir.ifay.mvola;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Response from polling a transaction's status. The field actually carrying the status is
 * uncertain - "status" or "transactionStatus" (a second, independent MVola implementation checks
 * both, preferring transactionStatus) - so this does too, until a real sandbox response confirms
 * which one MVola actually sends. Expected values: "pending", "completed", "failed".
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MvolaTransactionStatusResponse(
    String status, String transactionStatus, String transactionReference, String amount) {

  private String effectiveStatus() {
    return transactionStatus != null ? transactionStatus : status;
  }

  public boolean isCompleted() {
    return "completed".equalsIgnoreCase(effectiveStatus());
  }

  public boolean isFailed() {
    return "failed".equalsIgnoreCase(effectiveStatus());
  }
}
