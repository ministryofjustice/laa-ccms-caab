package uk.gov.laa.ccms.caab.dialect;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = ThymeleafTestConfig.class)
class ButtonElementTagProcessorTest {

    @Autowired
    private SpringTemplateEngine templateEngine;

    @Test
    void shouldRenderGovukButton() {

        Context context = new Context();
        String renderedHtml = templateEngine.process("test-button", context);
        assertThat(renderedHtml)
                .contains("<a href=\"/test\" role=\"button\" draggable=\"false\" class=\"govuk-button custom-class\" data-module=\"govuk-button\" id=\"button-id\">Click Me!</a>")
                .contains("<button type=\"submit\"  disabled aria-disabled=\"true\" class=\"govuk-button\" data-module=\"govuk-button\" id=\"button-id\">Click Me!</button>");

    }


}