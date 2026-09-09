package vendredi.soir.ifay.repository;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "party", uniqueConstraints = @UniqueConstraint(columnNames = {"phone_number"}))
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PartyEntity {
  @Id private UUID id;

  private String phoneNumber;
}
