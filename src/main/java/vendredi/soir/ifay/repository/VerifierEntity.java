package vendredi.soir.ifay.repository;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "verifier")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VerifierEntity {
  @Id private UUID id;

  private String appId;
  private String version;
  private String revision;
}
