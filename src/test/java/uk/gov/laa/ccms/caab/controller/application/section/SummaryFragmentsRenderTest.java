package uk.gov.laa.ccms.caab.controller.application.section;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.xpath;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockServletContext;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templatemode.TemplateMode;

class SummaryFragmentsRenderTest {

  private MockMvc mockMvc;

  @BeforeEach
  void setup() {
    final GenericWebApplicationContext applicationContext =
        new GenericWebApplicationContext(new MockServletContext());
    applicationContext.refresh();

    final SpringResourceTemplateResolver templateResolver = new SpringResourceTemplateResolver();
    templateResolver.setApplicationContext(applicationContext);
    templateResolver.setPrefix("classpath:/templates/");
    templateResolver.setSuffix(".html");
    templateResolver.setTemplateMode(TemplateMode.HTML);
    templateResolver.setCharacterEncoding("UTF-8");

    final SpringTemplateEngine templateEngine = new SpringTemplateEngine();
    templateEngine.setTemplateResolver(templateResolver);

    final ThymeleafViewResolver viewResolver = new ThymeleafViewResolver();
    viewResolver.setTemplateEngine(templateEngine);
    viewResolver.setCharacterEncoding("UTF-8");

    mockMvc = standaloneSetup(new HarnessController()).setViewResolvers(viewResolver).build();
  }

  @Test
  @DisplayName("Reusable summary list rows preserve IDs, actions and linked values")
  void rendersReusableSummaryListRows() throws Exception {
    mockMvc
        .perform(get("/test/summary-fragments"))
        .andExpect(status().isOk())
        .andExpect(xpath("//*[@id='status-value']").string("Complete"))
        .andExpect(xpath("//*[@id='client-value']").string("Jane Example"))
        .andExpect(xpath("//*[@id='change-client']/@href").string("/client"))
        .andExpect(xpath("//*[@id='change-client']//span").string("client"))
        .andExpect(xpath("//*[@id='costs-row']").nodeCount(1))
        .andExpect(xpath("//*[@id='costs-key']").nodeCount(1))
        .andExpect(xpath("//*[@id='costs-link']/@href").string("/costs"))
        .andExpect(xpath("//*[@id='costs-link']").string("Case cost limitation"));
  }

  @Controller
  static class HarnessController {

    @GetMapping("/test/summary-fragments")
    public String summaryFragments() {
      return "test/summary-fragments-harness";
    }
  }
}
