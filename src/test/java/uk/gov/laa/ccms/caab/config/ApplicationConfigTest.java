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
    "laa.ccms.data-api.url=http://mockUrl",
    "laa.ccms.soa-gateway-api.url=http://mockUrl"
})
class ApplicationConfigTest {

  @Qualifier("dataWebClient")
  @Autowired
  private WebClient dataWebClient;

  @Qualifier("soaGatewayWebClient")
  @Autowired
  private WebClient soaGatewayWebClient;

  @Test
  void dataWebClientBeanExists() {
    assertNotNull(dataWebClient, "dataWebClient bean should not be null");
    assertNotNull(soaGatewayWebClient, "soaGatewayWebClient bean should not be null");
  }
}
