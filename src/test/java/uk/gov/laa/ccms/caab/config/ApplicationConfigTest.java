package uk.gov.laa.ccms.caab.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import fi.solita.clamav.ClamAVClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.client.WebClient;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = ApplicationConfig.class)
@TestPropertySource(properties = {
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

  @MockBean
  private LoggingInterceptor loggingInterceptor;

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
}
