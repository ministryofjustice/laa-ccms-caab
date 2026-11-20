package uk.gov.laa.ccms.multipart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@SpringBootTest(
    classes = MultipartRequestLimitIntegrationTest.TestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MultipartRequestLimitIntegrationTest {

  private static final int MIB = 1024 * 1024;
  private static final String BOUNDARY = "caab-multipart-boundary";

  @Value("${local.server.port}")
  private int port;

  @Value("${server.servlet.context-path}")
  private String contextPath;

  @Test
  void acceptsRequestBetweenTenAndTwelveMibWithFilesWithinEightMib() throws Exception {
    int firstFileSize = 8 * MIB;
    int secondFileSize = 3 * MIB;
    ByteArrayOutputStream body = new ByteArrayOutputStream(11 * MIB + 1024);
    addFile(body, "first", new byte[firstFileSize]);
    addFile(body, "second", new byte[secondFileSize]);
    body.write(("--" + BOUNDARY + "--\r\n").getBytes(StandardCharsets.US_ASCII));
    assertTrue(body.size() > 10 * MIB && body.size() < 12 * MIB);

    HttpRequest request =
        HttpRequest.newBuilder(
                URI.create("http://localhost:" + port + contextPath + "/multipart-boundary"))
            .header("Content-Type", "multipart/form-data; boundary=" + BOUNDARY)
            .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()))
            .build();

    try (HttpClient client = HttpClient.newHttpClient()) {
      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      assertEquals(200, response.statusCode());
      assertEquals(firstFileSize + ":" + secondFileSize, response.body());
    }
  }

  private static void addFile(ByteArrayOutputStream body, String name, byte[] content)
      throws IOException {
    body.write(
        ("--"
                + BOUNDARY
                + "\r\nContent-Disposition: form-data; name=\""
                + name
                + "\"; filename=\"file.pdf\"\r\n"
                + "Content-Type: application/pdf\r\n\r\n")
            .getBytes(StandardCharsets.US_ASCII));
    body.write(content);
    body.write("\r\n".getBytes(StandardCharsets.US_ASCII));
  }

  @SpringBootConfiguration
  @EnableAutoConfiguration(
      exclude = {
        SecurityAutoConfiguration.class,
        ManagementWebSecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class,
        ServletWebSecurityAutoConfiguration.class,
        OAuth2ClientAutoConfiguration.class
      })
  @Import(MultipartBoundaryController.class)
  static class TestApplication {}

  @RestController
  static class MultipartBoundaryController {
    @PostMapping("/multipart-boundary")
    String accept(
        @RequestPart("first") MultipartFile first, @RequestPart("second") MultipartFile second) {
      return first.getSize() + ":" + second.getSize();
    }
  }
}
