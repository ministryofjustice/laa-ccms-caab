package uk.gov.laa.ccms.caab.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CLIENT_FLOW_FORM_DATA;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import uk.gov.laa.ccms.caab.bean.ClientFlowFormData;

class CreateClientRegistrationMvcConfigTest {

  @Test
  void productionRegistrationIsScopedToCreateClientRoutes() throws Exception {
    try (var context = new AnnotationConfigWebApplicationContext()) {
      context.setServletContext(new MockServletContext());
      context.register(CreateClientRegistrationMvcConfig.class, TestMvcConfig.class);
      context.refresh();
      MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).build();
      var client = new ClientFlowFormData("create");
      mvc.perform(
              get("/application/client/details/basic").sessionAttr(CLIENT_FLOW_FORM_DATA, client))
          .andExpect(status().isConflict());
      mvc.perform(
              get("/application/client/details/basic")
                  .sessionAttr(CLIENT_FLOW_FORM_DATA, client)
                  .param("registrationId", client.getRegistrationId()))
          .andExpect(status().isOk());
      mvc.perform(get("/amendments/sections/correspondence-address")).andExpect(status().isOk());
      mvc.perform(get("/application/sections/client/details/address")).andExpect(status().isOk());
    }
  }

  @Configuration
  @EnableWebMvc
  static class TestMvcConfig {
    @Bean
    TestController testController() {
      return new TestController();
    }
  }

  @RestController
  static class TestController {
    @GetMapping({
      "/application/client/details/basic",
      "/amendments/sections/correspondence-address",
      "/application/sections/client/details/address"
    })
    String view() {
      return "ok";
    }
  }
}
