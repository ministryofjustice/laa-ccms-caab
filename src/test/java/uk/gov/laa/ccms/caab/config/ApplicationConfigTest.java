package uk.gov.laa.ccms.caab.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.client.WebClient;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = ApplicationConfig.class)
@TestPropertySource(properties = {
    "laa.ccms.ebs-api.url=http://mockUrl",
    "laa.ccms.soa-api.url=http://mockUrl",
    "laa.ccms.caab-api.url=http://mockUrl"
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

  @Test
  void dataWebClientBeanExists() {
    assertNotNull(ebsApiWebClient, "ebsApiWebClient bean should not be null");
    assertNotNull(soaApiWebClient, "soaApiWebClient bean should not be null");
    assertNotNull(caabApiWebClient, "caabApiWebClient bean should not be null");
  }
}
