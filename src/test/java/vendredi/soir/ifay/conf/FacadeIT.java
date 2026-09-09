package vendredi.soir.ifay.conf;

import static java.lang.Runtime.getRuntime;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = RANDOM_PORT)
public class FacadeIT {

  private static final PostgresConf POSTGRES_CONF = new PostgresConf();

  @Autowired protected TestRestTemplate rest;

  @BeforeAll
  static void beforeAll() {
    POSTGRES_CONF.start();
    // Not stopped in an @AfterAll: it's shared across every FacadeIT subclass in the same JVM run.
    getRuntime().addShutdownHook(new Thread(POSTGRES_CONF::stop));
  }

  // TestRestTemplate's default JDK HttpURLConnection-backed request factory throws
  // HttpRetryException ("cannot retry due to server authentication, in streaming mode") for a
  // POST with a body that gets back a 401 - swap in Apache HttpClient, which doesn't have that
  // limitation, for every FacadeIT subclass.
  @BeforeEach
  void useHttpComponentsRequestFactory() {
    rest.getRestTemplate()
        .setRequestFactory(new HttpComponentsClientHttpRequestFactory(HttpClients.createDefault()));
  }

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    POSTGRES_CONF.configureProperties(registry);
  }
}
