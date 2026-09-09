package vendredi.soir.ifay.conf;

import static java.lang.Runtime.getRuntime;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = RANDOM_PORT)
public class FacadeIT {

  private static final PostgresConf POSTGRES_CONF = new PostgresConf();

  @BeforeAll
  static void beforeAll() {
    POSTGRES_CONF.start();
    // Not stopped in an @AfterAll: it's shared across every FacadeIT subclass in the same JVM run.
    getRuntime().addShutdownHook(new Thread(POSTGRES_CONF::stop));
  }

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    POSTGRES_CONF.configureProperties(registry);
  }
}
