package uk.gov.laa.ccms.caab.controller.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.Errors;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.bean.award.FinancialAwardFormData;
import uk.gov.laa.ccms.caab.bean.validators.awards.FinancialAwardValidator;
import uk.gov.laa.ccms.caab.client.CaabApiClientException;
import uk.gov.laa.ccms.caab.mapper.FinancialAwardMapper;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.FinancialAwardDetail;
import uk.gov.laa.ccms.caab.model.FinancialAwardRequest;
import uk.gov.laa.ccms.caab.service.CaseOutcomeService;
import uk.gov.laa.ccms.caab.service.LookupService;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.UserDetail;

@ExtendWith(MockitoExtension.class)
class AwardControllerTest {

  @Mock private LookupService lookupService;
  @Mock private CaseOutcomeService caseOutcomeService;
  @Mock private FinancialAwardValidator financialAwardValidator;
  @Mock private FinancialAwardMapper financialAwardMapper;

  @InjectMocks private AwardController controller;

  private MockMvcTester mockMvc;
  private ApplicationDetail ebsCase;
  private UserDetail user;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcTester.create(MockMvcBuilders.standaloneSetup(controller).build());
    ebsCase = new ApplicationDetail().caseReferenceNumber("300000001");
    user = ApplicationTestUtils.buildUser();
    lenient()
        .when(financialAwardMapper.toFinancialAwardRequest(any()))
        .thenReturn(new FinancialAwardRequest());
    lenient()
        .when(lookupService.getCommonValues(any()))
        .thenReturn(Mono.just(new CommonLookupDetail().content(Collections.emptyList())));
  }

  @Test
  void getNewFinancialAwardLoadsDefaultMetadata() {
    assertThat(
            mockMvc.perform(
                get("/case/outcome-and-awards/financial-award")
                    .sessionAttr(CASE, ebsCase)
                    .sessionAttr(USER_DETAILS, user)))
        .hasViewName("application/financial-award")
        .model()
        .hasEntrySatisfying(
            "financialAward",
            value ->
                assertThat(value)
                    .extracting("awardCode", "awardType", "description")
                    .containsExactly("DAMAGE_ARG", "DAMAGE", "Damage"));
  }

  @Test
  void getExistingFinancialAwardLoadsPersistedValues() {
    final FinancialAwardDetail award =
        new FinancialAwardDetail()
            .id(7)
            .awardCode("DAMAGE")
            .awardType("DAMAGE")
            .description("Damage")
            .dateOfOrder(new Date())
            .awardAmount(new BigDecimal("123.45"))
            .interimAward("0")
            .awardedBy("COURT");
    when(caseOutcomeService.getFinancialAward("300000001", 123, 7)).thenReturn(Optional.of(award));
    final FinancialAwardFormData formData = new FinancialAwardFormData();
    formData.setId(7);
    formData.setAwardAmount("123.45");
    when(financialAwardMapper.toFinancialAwardFormData(award)).thenReturn(formData);

    assertThat(
            mockMvc.perform(
                get("/case/outcome-and-awards/financial-award/7")
                    .sessionAttr(CASE, ebsCase)
                    .sessionAttr(USER_DETAILS, user)))
        .hasViewName("application/financial-award")
        .model()
        .hasEntrySatisfying(
            "financialAward",
            value ->
                assertThat(value).extracting("id", "awardAmount").containsExactly(7, "123.45"));
  }

  @Test
  void getNonUpdateableFinancialAwardReturnsToOverview() {
    final FinancialAwardDetail award = new FinancialAwardDetail().id(7).updateAllowed(false);
    when(caseOutcomeService.getFinancialAward("300000001", 123, 7)).thenReturn(Optional.of(award));

    assertThat(
            mockMvc.perform(
                get("/case/outcome-and-awards/financial-award/7")
                    .sessionAttr(CASE, ebsCase)
                    .sessionAttr(USER_DETAILS, user)))
        .hasRedirectedUrl("/case/outcome-and-awards");
  }

  @Test
  void postWithoutIdCreatesFinancialAward() {
    assertThat(mockMvc.perform(validPost())).hasRedirectedUrl("/case/outcome-and-awards");

    verify(caseOutcomeService)
        .createFinancialAward(
            eq("300000001"),
            eq(user.getProvider().getId().intValue()),
            any(FinancialAwardRequest.class),
            eq(user.getLoginId()));
  }

  @Test
  void postWithIdUpdatesFinancialAward() {
    assertThat(mockMvc.perform(validPost().param("id", "7")))
        .hasRedirectedUrl("/case/outcome-and-awards");

    verify(caseOutcomeService)
        .updateFinancialAward(
            eq("300000001"),
            eq(user.getProvider().getId().intValue()),
            eq(7),
            any(FinancialAwardRequest.class),
            eq(user.getLoginId()));
  }

  @Test
  void invalidPostRedisplaysFinancialAwardPage() {
    doAnswer(
            invocation -> {
              invocation
                  .getArgument(1, Errors.class)
                  .rejectValue("awardAmount", "invalid.currency", "Invalid amount");
              return null;
            })
        .when(financialAwardValidator)
        .validate(any(), any());

    assertThat(mockMvc.perform(validPost()))
        .hasViewName("application/financial-award")
        .model()
        .hasErrors();
  }

  @Test
  void apiFailureRedisplaysFinancialAwardPageWithError() {
    doThrow(new CaabApiClientException("API failure"))
        .when(caseOutcomeService)
        .createFinancialAward(any(), any(), any(), any());

    assertThat(mockMvc.perform(validPost()))
        .hasViewName("application/financial-award")
        .model()
        .hasErrors();
  }

  private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder validPost() {
    return post("/case/outcome-and-awards/financial-award")
        .param("awardCode", "DAMAGE_AGR")
        .param("awardType", "DAMAGE")
        .param("description", "Financial Settlement")
        .param("dateOfOrder", "01/01/2025")
        .param("awardAmount", "123.45")
        .param("interimAward", "0")
        .param("awardedBy", "COURT")
        .sessionAttr(CASE, ebsCase)
        .sessionAttr(USER_DETAILS, user);
  }
}
