package uk.gov.laa.ccms.caab.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.client.WebClient;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = ApplicationConfig.class)
@TestPropertySource(properties = {
        "laa.ccms.data-api.url=http://mockUrl"
})
class ApplicationConfigTest {

    @Autowired
    private WebClient webClient;

    @Test
    void dataWebClientBeanExists() {
        assertNotNull(webClient, "WebClient bean should not be null");
    }
}
