package vendredi.soir.ifay.service;

import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import vendredi.soir.ifay.repository.VerifierEntity;
import vendredi.soir.ifay.repository.VerifierRepository;

/**
 * Lookup-or-create by (appId, version, revision). Unlike {@link PartyService}, no unique
 * constraint backs this - new verifier app/version combos are created rarely enough that an
 * occasional duplicate row from a rare race is an acceptable tradeoff against the extra
 * machinery a strict guarantee would need.
 */
@Component
@AllArgsConstructor
class VerifierService {
  private final VerifierRepository verifierRepository;

  UUID findOrCreate(String appId, String version, String revision) {
    return verifierRepository
        .findByAppIdAndVersionAndRevision(appId, version, revision)
        .orElseGet(
            () ->
                verifierRepository.save(
                    VerifierEntity.builder()
                        .id(UUID.randomUUID())
                        .appId(appId)
                        .version(version)
                        .revision(revision)
                        .build()))
        .getId();
  }
}
