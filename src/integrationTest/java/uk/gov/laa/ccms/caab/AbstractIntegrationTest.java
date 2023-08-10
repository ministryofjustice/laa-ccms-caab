package uk.gov.laa.ccms.caab;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.saml2.Saml2RelyingPartyAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.laa.ccms.caab.advice.SamlPrincipalControllerAdvice;
import uk.gov.laa.ccms.caab.config.SecurityConfiguration;

@SpringBootTest
@EnableAutoConfiguration(exclude = {Saml2RelyingPartyAutoConfiguration.class})
public abstract class AbstractIntegrationTest {

    @MockBean
    private SecurityConfiguration securityConfiguration;

}
