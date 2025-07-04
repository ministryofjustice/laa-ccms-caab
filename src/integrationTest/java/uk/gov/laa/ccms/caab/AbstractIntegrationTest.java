package uk.gov.laa.ccms.caab;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.saml2.Saml2RelyingPartyAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.laa.ccms.caab.bean.metric.PuiMetricService;
import uk.gov.laa.ccms.caab.config.SecurityConfiguration;

@SpringBootTest
@EnableAutoConfiguration(exclude = {Saml2RelyingPartyAutoConfiguration.class})
public abstract class AbstractIntegrationTest {

  @MockitoBean private SecurityConfiguration securityConfiguration;

  @MockitoBean private PuiMetricService puiMetricService;
}
