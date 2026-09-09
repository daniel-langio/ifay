package vendredi.soir.ifay.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import vendredi.soir.ifay.repository.ChainTipEntity;
import vendredi.soir.ifay.repository.ChainTipRepository;

/**
 * Ensures the singleton chain-tip row exists before any request is served, so the write path
 * (see {@link PaymentTransactions}) never has to handle a "genesis" case - it can always assume
 * the row is there to lock. Multiple instances starting concurrently (e.g. a Cloud Run scale-up)
 * could race to create it; harmless if a second one loses, hence the plain catch-and-ignore.
 */
@Slf4j
@Component
@AllArgsConstructor
class ChainTipInitializer implements ApplicationRunner {
  private final ChainTipRepository chainTipRepository;

  @Override
  public void run(ApplicationArguments args) {
    if (chainTipRepository.existsById(1L)) {
      return;
    }
    try {
      chainTipRepository.save(ChainTipEntity.builder().id(1L).sequence(0).tipHash(null).build());
    } catch (DataIntegrityViolationException e) {
      log.info("Chain tip row already created by another instance");
    }
  }
}
