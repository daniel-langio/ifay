package vendredi.soir.ifay;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Neon (and most managed Postgres hosts) inject connection details as a single env var in the
 * "postgres://user:password@host:port/dbname" form, but Spring's JDBC driver needs a bare
 * "jdbc:postgresql://host:port/dbname" URL plus separate username/password. This splits that
 * apart at startup, before any bean is created, by adding a property source ahead of
 * application.properties for spring.datasource.url/username/password. A DATABASE_URL that's
 * already a jdbc: URL (or unset entirely, e.g. local dev) is left untouched.
 */
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

  @Override
  public void postProcessEnvironment(
      ConfigurableEnvironment environment, SpringApplication application) {
    String databaseUrl = environment.getProperty("DATABASE_URL");
    if (databaseUrl == null
        || !(databaseUrl.startsWith("postgres://") || databaseUrl.startsWith("postgresql://"))) {
      return;
    }

    URI uri;
    try {
      uri = new URI(databaseUrl);
    } catch (URISyntaxException e) {
      return; // Malformed - let Spring's own datasource startup fail with a clearer error.
    }

    String username = null;
    String password = null;
    String userInfo = uri.getUserInfo();
    if (userInfo != null) {
      int colon = userInfo.indexOf(':');
      username = colon >= 0 ? userInfo.substring(0, colon) : userInfo;
      password = colon >= 0 ? userInfo.substring(colon + 1) : null;
    }

    String jdbcUrl =
        "jdbc:postgresql://"
            + uri.getHost()
            + (uri.getPort() >= 0 ? ":" + uri.getPort() : "")
            + uri.getPath()
            + (uri.getQuery() != null ? "?" + uri.getQuery() : "");

    Map<String, Object> overrides = new LinkedHashMap<>();
    overrides.put("spring.datasource.url", jdbcUrl);
    if (username != null) overrides.put("spring.datasource.username", username);
    if (password != null) overrides.put("spring.datasource.password", password);

    environment.getPropertySources().addFirst(new MapPropertySource("databaseUrl", overrides));
  }
}
