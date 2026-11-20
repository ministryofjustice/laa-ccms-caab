package uk.gov.laa.ccms.caab.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.json.JsonParserFactory;
import org.yaml.snakeyaml.Yaml;

/**
 * Guards the JSON carried by environment variables in docker-compose.
 *
 * <p>A malformed value stops the container it configures from starting, and nothing else in the
 * build reads this file.
 */
@DisplayName("Docker compose environment test")
class DockerComposeEnvironmentTest {

  private static final Path COMPOSE_FILE = Path.of("docker-compose.yml");

  @Test
  @DisplayName("Every SPRING_APPLICATION_JSON value is parseable JSON")
  void springApplicationJsonValuesParse() throws IOException {
    List<String> values = springApplicationJsonValues();

    assertThat(values).isNotEmpty();
    values.forEach(
        value ->
            assertThatCode(() -> JsonParserFactory.getJsonParser().parseMap(value))
                .as("SPRING_APPLICATION_JSON: %s", value)
                .doesNotThrowAnyException());
  }

  @SuppressWarnings("unchecked")
  private List<String> springApplicationJsonValues() throws IOException {
    final Map<String, Object> compose = new Yaml().load(Files.readString(COMPOSE_FILE));
    final Map<String, Object> services = (Map<String, Object>) compose.get("services");

    final List<String> values = new ArrayList<>();
    for (Object service : services.values()) {
      final Object environment = ((Map<String, Object>) service).get("environment");
      if (environment instanceof Map<?, ?> entries) {
        final Object value = entries.get("SPRING_APPLICATION_JSON");
        if (value != null) {
          values.add(String.valueOf(value));
        }
      }
    }
    return values;
  }
}
