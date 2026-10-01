package uk.gov.laa.ccms.multipart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import uk.gov.laa.ccms.caab.bean.evidence.EvidenceUploadFormData;
import uk.gov.laa.ccms.caab.bean.validators.request.ProviderRequestDocumentUploadValidator;
import uk.gov.laa.ccms.caab.config.UploadSizeErrorFilter;
import uk.gov.laa.ccms.caab.config.UploadSizeErrorInterceptor;

@SpringBootTest(
    classes = MultipartCsrfRecoveryIntegrationTest.TestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MultipartCsrfRecoveryIntegrationTest {

  private static final String BOUNDARY = "caab-csrf-boundary";
  private static final String UPLOAD_PATH = "/case-provider-requests/documents";
  private static final int MIB = 1024 * 1024;

  @Value("${local.server.port}")
  private int port;

  @Value("${server.servlet.context-path}")
  private String contextPath;

  @Autowired private UploadController uploadController;

  @Test
  void csrfMultipartSizeFailureRedirectsToOneTimeFileError() throws Exception {
    String baseUrl = "http://localhost:" + port + contextPath;
    try (HttpClient client = HttpClient.newBuilder().cookieHandler(new CookieManager()).build()) {
      String csrfToken =
          client
              .send(
                  HttpRequest.newBuilder(URI.create(baseUrl + "/csrf-token")).GET().build(),
                  HttpResponse.BodyHandlers.ofString())
              .body();

      HttpResponse<String> invalidCsrf =
          client.send(
              multipartRequest(baseUrl + UPLOAD_PATH, "invalid", 1),
              HttpResponse.BodyHandlers.ofString());
      assertEquals(403, invalidCsrf.statusCode());

      HttpResponse<String> validUpload =
          client.send(
              multipartRequest(baseUrl + UPLOAD_PATH, csrfToken, 1),
              HttpResponse.BodyHandlers.ofString());
      assertEquals(200, validUpload.statusCode());
      assertEquals("uploaded", validUpload.body());
      assertEquals(1, uploadController.uploadCalls.get());

      HttpResponse<String> oversized =
          client.send(
              multipartRequest(baseUrl + UPLOAD_PATH, csrfToken, 8 * MIB + 1),
              HttpResponse.BodyHandlers.ofString());
      assertEquals(302, oversized.statusCode());
      URI redirect = URI.create(oversized.headers().firstValue("Location").orElseThrow());
      assertEquals("localhost", redirect.getHost());
      assertEquals(contextPath + UPLOAD_PATH, redirect.getPath());
      assertTrue(redirect.getRawQuery().startsWith("uploadSizeErrorToken="));
      assertEquals(1, uploadController.uploadCalls.get());

      HttpRequest redirected = HttpRequest.newBuilder(redirect).GET().build();
      HttpResponse<String> form = client.send(redirected, HttpResponse.BodyHandlers.ofString());
      assertEquals(200, form.statusCode());
      assertEquals("validation.error.maxFileSize", form.body());
      assertEquals(
          "no-error", client.send(redirected, HttpResponse.BodyHandlers.ofString()).body());
    }
  }

  private static HttpRequest multipartRequest(String url, String csrfToken, int fileSize)
      throws IOException {
    ByteArrayOutputStream body = new ByteArrayOutputStream(fileSize + 512);
    body.write(
        ("--"
                + BOUNDARY
                + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"file.pdf\"\r\n"
                + "Content-Type: application/pdf\r\n\r\n")
            .getBytes(StandardCharsets.US_ASCII));
    body.write(new byte[fileSize]);
    body.write("\r\n".getBytes(StandardCharsets.US_ASCII));
    body.write(
        ("--"
                + BOUNDARY
                + "\r\nContent-Disposition: form-data; name=\"_csrf\"\r\n\r\n"
                + csrfToken
                + "\r\n--"
                + BOUNDARY
                + "--\r\n")
            .getBytes(StandardCharsets.US_ASCII));
    return HttpRequest.newBuilder(URI.create(url))
        .header("Content-Type", "multipart/form-data; boundary=" + BOUNDARY)
        .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()))
        .build();
  }

  @SpringBootConfiguration
  @EnableAutoConfiguration(exclude = OAuth2ClientAutoConfiguration.class)
  @EnableWebSecurity
  @Import({UploadSizeErrorFilter.class, UploadController.class})
  static class TestApplication {
    @Bean
    SecurityFilterChain security(HttpSecurity http) throws Exception {
      return http.authorizeHttpRequests(requests -> requests.anyRequest().permitAll())
          .csrf(Customizer.withDefaults())
          .build();
    }

    @Bean
    WebMvcConfigurer uploadSizeInterceptor() {
      UploadSizeErrorInterceptor interceptor =
          new UploadSizeErrorInterceptor(
              new ProviderRequestDocumentUploadValidator(
                  List.of("pdf"), "8MB", List.of("application/pdf")));
      return new WebMvcConfigurer() {
        @Override
        public void addInterceptors(InterceptorRegistry registry) {
          registry.addInterceptor(interceptor);
        }
      };
    }
  }

  @RestController
  static class UploadController {
    private final AtomicInteger uploadCalls = new AtomicInteger();

    @GetMapping("/csrf-token")
    String csrfToken(HttpServletRequest request) {
      return ((CsrfToken) request.getAttribute(CsrfToken.class.getName())).getToken();
    }

    @PostMapping(UPLOAD_PATH)
    String upload() {
      uploadCalls.incrementAndGet();
      return "uploaded";
    }

    @GetMapping(UPLOAD_PATH)
    ModelAndView uploadForm() {
      ModelAndView view =
          new ModelAndView(
              (model, request, response) -> {
                BindingResult errors =
                    (BindingResult)
                        model.get(
                            BindingResult.MODEL_KEY_PREFIX
                                + "caseProviderRequestEvidenceUploadForm");
                response
                    .getWriter()
                    .write(
                        errors != null && errors.hasFieldErrors("file")
                            ? errors.getFieldError("file").getCode()
                            : "no-error");
              });
      view.addObject("caseProviderRequestEvidenceUploadForm", new EvidenceUploadFormData());
      return view;
    }
  }
}
