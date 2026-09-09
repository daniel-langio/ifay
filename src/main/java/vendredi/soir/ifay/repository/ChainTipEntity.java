package vendredi.soir.ifay.repository;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Singleton row (always {@code id = 1}) holding the ledger's current chain tip. Every ledger
 * write locks this row first ({@code SELECT ... FOR UPDATE}), which serializes all appends to the
 * {@code PaymentEvent} log - and, as a side effect, everything else a write touches (Party
 * lookup-or-create, Payment row upsert) - to one at a time, so the chain's "previous hash" link
 * is always computed against a consistent view with no concurrent-insert race to guard against
 * separately.
 */
@Entity
@Table(name = "chain_tip")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChainTipEntity {
  @Id private Long id;

  private long sequence;
  private String tipHash;
}
