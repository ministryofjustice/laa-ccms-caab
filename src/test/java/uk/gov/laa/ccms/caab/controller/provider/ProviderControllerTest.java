package uk.gov.laa.ccms.caab.controller.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

import jakarta.servlet.http.HttpSession;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.saml2.provider.service.authentication.Saml2AssertionAuthentication;
import org.springframework.security.saml2.provider.service.authentication.Saml2ResponseAssertionAccessor;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.Errors;
import org.springframework.web.context.WebApplicationContext;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.bean.provider.ProviderFirmFormData;
import uk.gov.laa.ccms.caab.bean.validators.provider.ProviderFirmValidator;
import uk.gov.laa.ccms.caab.service.NotificationSearchOptionsCache;
import uk.gov.laa.ccms.caab.service.UserService;
import uk.gov.laa.ccms.data.model.BaseProvider;
import uk.gov.laa.ccms.data.model.UserDetail;
import uk.gov.laa.ccms.soa.gateway.model.ClientTransactionResponse;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration
@WebAppConfiguration
public class ProviderControllerTest {

  @Mock ProviderFirmValidator validator;

  @Mock UserService userService;

  @Mock NotificationSearchOptionsCache notificationSearchOptionsCache;

  @InjectMocks ProviderController providerController;

  private MockMvc mockMvc;

  @Autowired private WebApplicationContext webApplicationContext;

  @BeforeEach
  public void setup() {
    mockMvc = standaloneSetup(providerController).build();
  }

  @Test
  public void testGetProviderSwitchScreen() throws Exception {

    BaseProvider providerFirm = new BaseProvider().id(12345).name("providerFirm");

    UserDetail userDetails =
        new UserDetail()
            .userId(1)
            .userType("testUserType")
            .loginId("testLoginId")
            .firms(List.of(providerFirm))
            .provider(providerFirm);

    this.mockMvc
        .perform(
            get("/provider-switch").flashAttr("user", userDetails).sessionAttr("user", userDetails))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("provider/provider-switch"))
        .andExpect(model().attribute("providerFirms", List.of(providerFirm)));
  }

  @Test
  public void testUpdateProviderOptions() throws Exception {

    String loginId = "testLoginId";
    String userType = "testUserType";

    BaseProvider providerFirm1 = new BaseProvider().id(12345).name("providerFirm1");
    BaseProvider providerFirm2 = new BaseProvider().id(67890).name("providerFirm2");

    UserDetail userDetails =
        new UserDetail()
            .userId(1)
            .userType(userType)
            .loginId(loginId)
            .firms(List.of(providerFirm1, providerFirm2))
            .provider(providerFirm1);

    ProviderFirmFormData providerFirmFormData = new ProviderFirmFormData();
    providerFirmFormData.setProviderFirmId(67890);

    when(userService.updateUserOptions(67890, loginId, userType))
        .thenReturn(Mono.just(new ClientTransactionResponse()));
    when(userService.getUserByLoginId(loginId))
        .thenReturn(Mono.just(new UserDetail().loginId(loginId).functions(List.of("CA", "VC"))));

    HttpSession session =
        this.mockMvc
            .perform(
                post("/provider-switch")
                    .sessionAttr("user", userDetails)
                    .flashAttr("providerFirmFormData", providerFirmFormData))
            .andDo(print())
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/home"))
            .andReturn()
            .getRequest()
            .getSession();

    // Check session has been updated with new provider
    assertEquals(providerFirm2, ((UserDetail) session.getAttribute("user")).getProvider());
    assertEquals(List.of("CA", "VC"), ((UserDetail) session.getAttribute("user")).getFunctions());
    verify(notificationSearchOptionsCache).clear();
  }

  @Test
  public void switchingProviderReloadsFunctionsInTheLoggedInAuthentication() throws Exception {
    String loginId = "testLoginId";
    BaseProvider providerFirm1 = new BaseProvider().id(12345).name("providerFirm1");
    BaseProvider providerFirm2 = new BaseProvider().id(67890).name("providerFirm2");
    UserDetail userDetails =
        new UserDetail()
            .userId(1)
            .userType("testUserType")
            .loginId(loginId)
            .firms(List.of(providerFirm1, providerFirm2))
            .provider(providerFirm1)
            .functions(List.of("CA", "NOT"));
    ProviderFirmFormData providerFirmFormData = new ProviderFirmFormData();
    providerFirmFormData.setProviderFirmId(67890);
    Saml2AssertionAuthentication authentication =
        new Saml2AssertionAuthentication(
            loginId,
            mock(Saml2ResponseAssertionAccessor.class),
            List.of(
                new SimpleGrantedAuthority("group1"),
                new SimpleGrantedAuthority("CA"),
                new SimpleGrantedAuthority("NOT")),
            "idp");

    when(userService.updateUserOptions(67890, loginId, "testUserType"))
        .thenReturn(Mono.just(new ClientTransactionResponse()));
    when(userService.getUserByLoginId(loginId))
        .thenReturn(Mono.just(new UserDetail().loginId(loginId).functions(List.of("NOT", "VC"))));

    try {
      this.mockMvc
          .perform(
              post("/provider-switch")
                  .principal(authentication)
                  .sessionAttr("user", userDetails)
                  .flashAttr("providerFirmFormData", providerFirmFormData))
          .andExpect(redirectedUrl("/home"));

      assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
          .extracting(GrantedAuthority::getAuthority)
          .containsExactlyInAnyOrder("group1", "NOT", "VC");
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

  @Test
  public void testUpdateProviderOptions_returnsToSwitchProviderScreen_whenValidationFails()
      throws Exception {

    String loginId = "testLoginId";
    String userType = "testUserType";

    BaseProvider providerFirm = new BaseProvider().id(12345).name("providerFirm");

    UserDetail userDetails =
        new UserDetail()
            .userId(1)
            .userType(userType)
            .loginId(loginId)
            .firms(List.of(providerFirm))
            .provider(providerFirm);

    doAnswer(
            invocation -> {
              Errors errors = (Errors) invocation.getArguments()[1];
              errors.rejectValue(
                  "providerFirmId", "required.providerFirmId", "Please complete 'Provider firm'.");
              return null;
            })
        .when(validator)
        .validate(any(), any());

    this.mockMvc
        .perform(
            post("/provider-switch")
                .sessionAttr("user", userDetails)
                .flashAttr("providerFirmFormData", new ProviderFirmFormData()))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("provider/provider-switch"))
        .andExpect(model().attribute("providerFirms", List.of(providerFirm)));
  }
}
