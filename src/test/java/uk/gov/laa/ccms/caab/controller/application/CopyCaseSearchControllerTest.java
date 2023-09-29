package uk.gov.laa.ccms.caab.controller.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.COPY_CASE_SEARCH_CRITERIA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;

import jakarta.servlet.ServletException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.Errors;
import org.springframework.web.context.WebApplicationContext;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.bean.CopyCaseSearchCriteria;
import uk.gov.laa.ccms.caab.bean.CopyCaseSearchCriteriaValidator;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.service.ProviderService;
import uk.gov.laa.ccms.data.model.BaseOffice;
import uk.gov.laa.ccms.data.model.BaseProvider;
import uk.gov.laa.ccms.data.model.ContactDetail;
import uk.gov.laa.ccms.data.model.ProviderDetail;
import uk.gov.laa.ccms.data.model.UserDetail;

@ExtendWith(SpringExtension.class)
@ContextConfiguration
@WebAppConfiguration
public class CopyCaseSearchControllerTest {
  @Mock
  private CopyCaseSearchCriteriaValidator validator;

  @Mock
  private ProviderService providerService;

  @InjectMocks
  private CopyCaseSearchController copyCaseSearchController;

  private MockMvc mockMvc;

  @Autowired
  private WebApplicationContext webApplicationContext;

  @BeforeEach
  public void setup() {
    mockMvc = standaloneSetup(copyCaseSearchController).build();
  }

  @Test
  public void testGetCopyCaseSearchAddsFeeEarnersToModel() throws Exception {
    final UserDetail user = buildUser();

    ProviderDetail providerDetail = new ProviderDetail();
    List<ContactDetail> feeEarners = buildFeeEarners();

    when(providerService.getProvider(user.getProvider().getId()))
        .thenReturn(Mono.just(providerDetail));
    when(providerService.getAllFeeEarners(providerDetail)).thenReturn(feeEarners);

    this.mockMvc.perform(get("/application/copy-case/search")
            .sessionAttr("user", user))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("application/application-copy-case-search"))
        .andExpect(model().attribute("feeEarners", feeEarners))
        .andExpect(model().attribute("offices", user.getProvider().getOffices()));
  }

  @Test
  public void testGetCopyCaseSearchNoFeeEarners() throws Exception {
    final UserDetail user = buildUser();

    when(providerService.getProvider(user.getProvider().getId()))
        .thenReturn(Mono.empty());

    Exception exception = assertThrows(Exception.class, () ->
        this.mockMvc.perform(get("/application/copy-case/search")
            .sessionAttr(USER_DETAILS, user)));

    assertTrue(exception.getCause() instanceof CaabApplicationException);
    assertEquals(String.format("Failed to retrieve Provider with id: %s", user.getProvider().getId()), exception.getCause().getMessage());
  }

  @Test
  public void testPostCopyCaseSearchHandlesValidationFailure() throws Exception {
    final UserDetail user = buildUser();

    ProviderDetail providerDetail = new ProviderDetail();
    List<ContactDetail> feeEarners = buildFeeEarners();

    when(providerService.getProvider(user.getProvider().getId()))
        .thenReturn(Mono.just(providerDetail));
    when(providerService.getAllFeeEarners(providerDetail)).thenReturn(feeEarners);

    doAnswer(invocation -> {
      Errors errors = (Errors) invocation.getArguments()[1];
      errors.rejectValue(null, "required.atLeastOneSearchCriteria",
          "You must provide at least one search criteria below. Please amend your entry.");
      return null;
    }).when(validator).validate(any(), any());

    this.mockMvc.perform(post("/application/copy-case/search")
            .sessionAttr("user", user))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("application/application-copy-case-search"))
        .andExpect(model().attribute("feeEarners", feeEarners))
        .andExpect(model().attribute("offices", user.getProvider().getOffices()));
  }

  @Test
  public void testPostCopyCaseSearch_RedirectsToResults() throws Exception {
    final UserDetail user = buildUser();

    this.mockMvc.perform(post("/application/copy-case/search")
            .sessionAttr("user", user)
            .flashAttr(COPY_CASE_SEARCH_CRITERIA, new CopyCaseSearchCriteria()))
        .andDo(print())
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/application/copy-case/results"));
  }

  private UserDetail buildUser() {
    return new UserDetail()
        .userId(1)
        .userType("testUserType")
        .loginId("testLoginId")
        .provider(buildBaseProvider());
  }

  private BaseProvider buildBaseProvider() {
    return new BaseProvider()
        .id(123)
        .name("provider1")
        .addOfficesItem(new BaseOffice()
            .id(10)
            .name("Office 1"))
        .addOfficesItem(new BaseOffice()
            .id(11)
            .name("Office 2"));
  }

  private List<ContactDetail> buildFeeEarners() {
    List<ContactDetail> feeEarners = new ArrayList<>();
    feeEarners.add(new ContactDetail()
            .id(1)
            .name("FeeEarner1"));
    feeEarners.add(new ContactDetail()
            .id(2)
            .name("FeeEarner2"));
    return feeEarners;
  }
}
