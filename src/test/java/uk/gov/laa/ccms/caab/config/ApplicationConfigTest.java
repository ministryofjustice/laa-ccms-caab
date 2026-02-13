package uk.gov.laa.ccms.caab.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import fi.solita.clamav.ClamAVClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.reactive.function.client.WebClient;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;

@ExtendWith(MockitoExtension.class)
@SpringBootTest(classes = {ApplicationConfig.class, TestConfig.class})
@TestPropertySource(
    properties = {
      "laa.ccms.ebs-api.url=http://mockUrl",
      "laa.ccms.soa-api.url=http://mockUrl",
      "laa.ccms.caab-api.url=http://mockUrl",
      "os.api.url=http://mockUrl",
      "av.api.hostname=http://mockUrl",
      "av.api.port=3000",
      "av.api.timeout=100"
    })
class ApplicationConfigTest {

  @Qualifier("ebsApiWebClient")
  @Autowired
  private WebClient ebsApiWebClient;

  @Qualifier("soaApiWebClient")
  @Autowired
  private WebClient soaApiWebClient;

  @Qualifier("caabApiWebClient")
  @Autowired
  private WebClient caabApiWebClient;

  @Qualifier("osApiWebClient")
  @Autowired
  private WebClient osApiWebClient;

  @Qualifier("clamAvClient")
  @Autowired
  private ClamAVClient clamAvClient;

  @Autowired private ThymeleafViewResolver thymeleafViewResolver;

  @MockitoBean private SpringTemplateEngine templateEngine;

  @MockitoBean private LoggingInterceptor loggingInterceptor;

  @Test
  void ebsApiWebClientBeanExists() {
    assertNotNull(ebsApiWebClient, "ebsApiWebClient bean should not be null");
  }

  @Test
  void soaApiWebClientBeanExists() {
    assertNotNull(soaApiWebClient, "soaApiWebClient bean should not be null");
  }

  @Test
  void caabApiWebClientBeanExists() {
    assertNotNull(caabApiWebClient, "caabApiWebClient bean should not be null");
  }

  @Test
  void osApiWebClientBeanExists() {
    assertNotNull(osApiWebClient, "osApiWebClient bean should not be null");
  }

  @Test
  void avApiClientBeanExists() {
    assertNotNull(clamAvClient, "clamAvClient bean should not be null");
  }

  @Test
  void thymeleafViewResolverBeanExists() {
    assertNotNull(thymeleafViewResolver, "thymeleafViewResolver bean should not be null");
  }
}
