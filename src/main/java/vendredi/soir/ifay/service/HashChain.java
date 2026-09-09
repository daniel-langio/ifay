package vendredi.soir.ifay.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import vendredi.soir.ifay.repository.PaymentEventEntity;

/**
 * Computes each event's hash as SHA-256 over its own content plus the previous event's hash -
 * the actual tamper-evidence mechanism: changing any past event, or its position in the chain,
 * changes every hash after it, which a later comparison against a published anchor (see the
 * `/ledger/anchor` endpoint and the daily anchoring job) will catch.
 */
final class HashChain {
  private HashChain() {}

  static String hash(PaymentEventEntity event, String previousEventHash) {
    String canonical =
        String.join(
            "|",
            nullToEmpty(previousEventHash),
            String.valueOf(event.getSequence()),
            nullToEmpty(event.getPspRef()),
            String.valueOf(event.getType()),
            String.valueOf(event.getEventType()),
            nullToEmpty(event.getSenderId()),
            nullToEmpty(event.getReceiverId()),
            nullToEmpty(event.getClaimedAmount()),
            nullToEmpty(event.getVerifierId()),
            nullToEmpty(event.getConfirmedAmount()),
            nullToEmpty(event.getOccurredAt()));
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 is not available", e);
    }
  }

  private static String nullToEmpty(Object value) {
    return value == null ? "" : value.toString();
  }
}
