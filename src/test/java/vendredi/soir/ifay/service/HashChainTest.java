package vendredi.soir.ifay.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import vendredi.soir.ifay.model.PaymentEventType;
import vendredi.soir.ifay.model.Provider;
import vendredi.soir.ifay.repository.PaymentEventEntity;

class HashChainTest {

  @Test
  void same_content_and_previous_hash_produce_the_same_hash() {
    PaymentEventEntity event = sampleEvent();

    assertThat(HashChain.hash(event, "prev-hash")).isEqualTo(HashChain.hash(event, "prev-hash"));
  }

  @Test
  void different_previous_hash_changes_the_hash() {
    PaymentEventEntity event = sampleEvent();

    assertThat(HashChain.hash(event, "prev-hash-a")).isNotEqualTo(HashChain.hash(event, "prev-hash-b"));
  }

  @Test
  void changing_any_field_changes_the_hash() {
    PaymentEventEntity event = sampleEvent();
    String originalHash = HashChain.hash(event, "prev-hash");

    event.setClaimedAmount(event.getClaimedAmount() + 1);

    assertThat(HashChain.hash(event, "prev-hash")).isNotEqualTo(originalHash);
  }

  @Test
  void null_previous_hash_is_valid_for_the_genesis_event() {
    PaymentEventEntity event = sampleEvent();

    assertThat(HashChain.hash(event, null)).isNotBlank();
  }

  private static PaymentEventEntity sampleEvent() {
    return PaymentEventEntity.builder()
        .id(UUID.randomUUID())
        .sequence(1)
        .pspRef("SOME-REF")
        .type(Provider.MVOLA)
        .eventType(PaymentEventType.CLAIM_RECORDED)
        .senderId(UUID.randomUUID())
        .receiverId(UUID.randomUUID())
        .claimedAmount(1000L)
        .occurredAt(Instant.parse("2026-01-01T00:00:00Z"))
        .build();
  }
}
