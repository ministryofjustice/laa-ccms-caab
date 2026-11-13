package uk.gov.laa.ccms.caab.controller.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.ACTIVE_CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.BindingResult;
import uk.gov.laa.ccms.caab.advice.ActiveCaseModelAdvice;
import uk.gov.laa.ccms.caab.advice.GlobalBinderAdvice;
import uk.gov.laa.ccms.caab.bean.ActiveCase;
import uk.gov.laa.ccms.caab.bean.award.RecoveryFormData;
import uk.gov.laa.ccms.caab.bean.validators.BaselineTextValidator;
import uk.gov.laa.ccms.caab.bean.validators.awards.RecoveryValidator;
import uk.gov.laa.ccms.caab.constants.FunctionConstants;
import uk.gov.laa.ccms.caab.mapper.LiablePartiesDisplayMapper;
import uk.gov.laa.ccms.caab.mapper.RecoveryMapper;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.CostAwardDetail;
import uk.gov.laa.ccms.caab.model.FinancialAwardDetail;
import uk.gov.laa.ccms.caab.model.RecoveryDetail;
import uk.gov.laa.ccms.caab.model.RecoveryRequest;
import uk.gov.laa.ccms.caab.service.CaseOutcomeService;
import uk.gov.laa.ccms.caab.util.DateUtils;
import uk.gov.laa.ccms.data.model.UserDetail;

@ExtendWith(MockitoExtension.class)
@DisplayName("Recovery for cost and financial awards")
class RecoveryControllerTest {
  @Mock private CaseOutcomeService caseOutcomeService;
  @Spy private RecoveryValidator recoveryValidator = new RecoveryValidator();
  @Spy private RecoveryMapper recoveryMapper = Mappers.getMapper(RecoveryMapper.class);

  @Spy
  private LiablePartiesDisplayMapper awardDisplayMapper =
      Mappers.getMapper(LiablePartiesDisplayMapper.class);

  @InjectMocks private RecoveryController controller;

  private MockMvc mvc;
  private ApplicationDetail ebsCase;
  private UserDetail user;

  @BeforeEach
  void setUp() {
    ebsCase =
        new ApplicationDetail()
            .caseReferenceNumber("300000001")
            .availableFunctions(List.of(FunctionConstants.OUTCOME_WITH_DISCHARGE));
    user = ApplicationTestUtils.buildUser();
    mvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(
                new ActiveCaseModelAdvice(), new GlobalBinderAdvice(new BaselineTextValidator()))
            .build();
  }

  @ParameterizedTest
  @CsvSource({
    "cost-award,display",
    "financial-award,display",
    "cost-award,calculate",
    "financial-award,calculate",
    "cost-award,next",
    "financial-award,next"
  })
  void includesActiveCaseBannerOnDisplayAndRedisplay(String path, String action) throws Exception {
    stubAward(path, null);
    final var activeCase =
        ActiveCase.builder()
            .caseReferenceNumber("300000001")
            .client("Test Client")
            .providerCaseReferenceNumber("PROVIDER-123")
            .build();
    final var request =
        "display".equals(action)
            ? get(url(path))
            : post(url(path)).param("action", action).param("solicitorRecoveryAmount", "-1");
    mvc.perform(
            request
                .sessionAttr(CASE, ebsCase)
                .sessionAttr(ACTIVE_CASE, activeCase)
                .sessionAttr(USER_DETAILS, user))
        .andExpect(view().name("application/recovery"))
        .andExpect(model().attribute(ACTIVE_CASE, activeCase));
  }

  private void stubAward(String path, RecoveryDetail recovery) {
    if ("cost-award".equals(path)) {
      when(caseOutcomeService.getCostAward("300000001", 123, 7))
          .thenReturn(
              Optional.of(
                  new CostAwardDetail()
                      .id(7)
                      .awardType("COST")
                      .description("Cost award")
                      .preCertificateLscCost(new BigDecimal("100.00"))
                      .preCertificateOtherCost(new BigDecimal("200.00"))
                      .certificateCostLsc(new BigDecimal("300.00"))
                      .certificateCostMarket(new BigDecimal("400.00"))
                      .recovery(recovery)));
    } else {
      when(caseOutcomeService.getFinancialAward("300000001", 123, 7))
          .thenReturn(
              Optional.of(
                  new FinancialAwardDetail()
                      .id(7)
                      .awardType("DAMAGE")
                      .description("Financial award")
                      .awardAmount(new BigDecimal("1000.00"))
                      .recovery(recovery)));
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"cost-award", "financial-award"})
  void loadsDefaultsAndAuthoritativeAwardValue(String path) throws Exception {
    stubAward(path, null);
    final var result =
        mvc.perform(get(url(path)).sessionAttr(CASE, ebsCase).sessionAttr(USER_DETAILS, user))
            .andExpect(view().name("application/recovery"))
            .andExpect(model().attribute("awardAmount", new BigDecimal("1000.00")))
            .andExpect(model().attribute("recoveredAmount", new BigDecimal("0.00")))
            .andExpect(model().attribute("unrecoveredAmount", new BigDecimal("1000.00")))
            .andReturn();
    final var form = (RecoveryFormData) result.getModelAndView().getModel().get("recovery");
    assertThat(form.getLeaveOfCourtRequiredInd()).isNull();
    assertThat(form.getSolicitorRecoveryAmount()).isEqualTo("0.00");
  }

  @Test
  void loadsExistingRecoveryAndIgnoresStaleStoredTotals() throws Exception {
    stubAward(
        "financial-award",
        new RecoveryDetail()
            .solicitorRecoveryAmount(new BigDecimal("10.00"))
            .courtRecoveryAmount(new BigDecimal("20.00"))
            .clientRecoveryAmount(new BigDecimal("30.00"))
            .recoveredAmount(new BigDecimal("999"))
            .awardAmount(new BigDecimal("1"))
            .solicitorRecoveryDate(DateUtils.convertToDate("01/01/1900"))
            .leaveOfCourtRequiredInd(false)
            .offeredAmount(new BigDecimal("12.50"))
            .offerDetails("Terms"));
    final var result =
        mvc.perform(
                get(url("financial-award"))
                    .sessionAttr(CASE, ebsCase)
                    .sessionAttr(USER_DETAILS, user))
            .andExpect(model().attribute("recoveredAmount", new BigDecimal("60.00")))
            .andExpect(model().attribute("unrecoveredAmount", new BigDecimal("940.00")))
            .andReturn();
    final var form = (RecoveryFormData) result.getModelAndView().getModel().get("recovery");
    assertThat(form.getSolicitorRecoveryDate()).isEqualTo("01/01/1900");
    assertThat(form.getLeaveOfCourtRequiredInd()).isFalse();
    assertThat(form.getOfferDetails()).isEqualTo("Terms");
  }

  @ParameterizedTest
  @ValueSource(strings = {"cost-award", "financial-award"})
  void calculatesNegativeBalanceWithoutSaving(String path) throws Exception {
    stubAward(path, null);
    mvc.perform(
            post(url(path))
                .sessionAttr(CASE, ebsCase)
                .sessionAttr(USER_DETAILS, user)
                .param("action", "calculate")
                .param("solicitorRecoveryAmount", "1000.01")
                .param("courtRecoveryAmount", ".50")
                .param("clientRecoveryAmount", "2")
                .param("solicitorAmountPaidToLsc", "900"))
        .andExpect(view().name("application/recovery"))
        .andExpect(model().attribute("recoveredAmount", new BigDecimal("1002.51")))
        .andExpect(model().attribute("unrecoveredAmount", new BigDecimal("-2.51")));
    verify(caseOutcomeService, never()).upsertRecovery(any(), any(), any(), any(), any(), any());
  }

  @ParameterizedTest
  @ValueSource(strings = {"cost-award", "financial-award"})
  void preservesLegacyCalculationAboveThePersistenceLimit(String path) throws Exception {
    stubAward(path, null);
    mvc.perform(
            post(url(path))
                .sessionAttr(CASE, ebsCase)
                .sessionAttr(USER_DETAILS, user)
                .param("action", "calculate")
                .param("solicitorRecoveryAmount", "99999999.99")
                .param("courtRecoveryAmount", "1000.02"))
        .andExpect(view().name("application/recovery"))
        .andExpect(model().hasNoErrors())
        .andExpect(model().attribute("recoveredAmount", new BigDecimal("100001000.01")))
        .andExpect(model().attribute("unrecoveredAmount", new BigDecimal("-100000000.01")));
    verify(caseOutcomeService, never()).upsertRecovery(any(), any(), any(), any(), any(), any());
  }

  @ParameterizedTest
  @ValueSource(strings = {"cost-award", "financial-award"})
  void rejectsUnsaveableTotalsWithSpecificErrorsAndPreservesInputs(String path) throws Exception {
    stubAward(path, null);
    final var result =
        mvc.perform(
                post(url(path))
                    .sessionAttr(CASE, ebsCase)
                    .sessionAttr(USER_DETAILS, user)
                    .param("solicitorRecoveryAmount", "99999999.99")
                    .param("courtRecoveryAmount", "1000.02")
                    .param("offerDetails", "Keep these terms"))
            .andExpect(view().name("application/recovery"))
            .andExpect(
                model()
                    .attributeHasFieldErrorCode(
                        "recovery",
                        "solicitorRecoveryAmount",
                        "recovery.recoveredAmount.exceedsMax"))
            .andExpect(
                model()
                    .attributeHasFieldErrorCode(
                        "recovery",
                        "clientRecoveryAmount",
                        "recovery.unrecoveredAmount.exceedsMax"))
            .andExpect(model().attribute("recoveredAmount", new BigDecimal("100001000.01")))
            .andReturn();
    final var form = (RecoveryFormData) result.getModelAndView().getModel().get("recovery");
    assertThat(form.getSolicitorRecoveryAmount()).isEqualTo("99999999.99");
    assertThat(form.getCourtRecoveryAmount()).isEqualTo("1000.02");
    assertThat(form.getOfferDetails()).isEqualTo("Keep these terms");
    verify(caseOutcomeService, never()).upsertRecovery(any(), any(), any(), any(), any(), any());
  }

  @ParameterizedTest
  @ValueSource(strings = {"cost-award", "financial-award"})
  void savesMaximumTotalAndNegativeBalanceAsInLegacy(String path) throws Exception {
    stubAward(path, null);
    mvc.perform(
            post(url(path))
                .sessionAttr(CASE, ebsCase)
                .sessionAttr(USER_DETAILS, user)
                .param("solicitorRecoveryAmount", "99999999.99"))
        .andExpect(redirectedUrl("/case/outcome-and-awards"));
    verify(caseOutcomeService).upsertRecovery(any(), any(), any(), any(), any(), any());
  }

  @Test
  void rejectsAwardAmountOutsideThePersistenceLimitBeforeSaving() throws Exception {
    when(caseOutcomeService.getCostAward("300000001", 123, 7))
        .thenReturn(
            Optional.of(
                new CostAwardDetail()
                    .id(7)
                    .awardType("COST")
                    .preCertificateLscCost(new BigDecimal("99999999.99"))
                    .preCertificateOtherCost(new BigDecimal("0.01"))));
    final var result =
        mvc.perform(
                post(url("cost-award")).sessionAttr(CASE, ebsCase).sessionAttr(USER_DETAILS, user))
            .andExpect(view().name("application/recovery"))
            .andExpect(model().attributeHasErrors("recovery"))
            .andReturn();
    final var errors =
        (BindingResult)
            result.getModelAndView().getModel().get(BindingResult.MODEL_KEY_PREFIX + "recovery");
    assertThat(errors.getGlobalError().getCode()).isEqualTo("recovery.awardAmount.exceedsMax");
    verify(caseOutcomeService, never()).upsertRecovery(any(), any(), any(), any(), any(), any());
  }

  @ParameterizedTest
  @ValueSource(strings = {"cost-award", "financial-award"})
  void rejectsReceiptDatesAtOrBeforeTheSharedLowerBound(String path) throws Exception {
    stubAward(path, null);
    mvc.perform(
            post(url(path))
                .sessionAttr(CASE, ebsCase)
                .sessionAttr(USER_DETAILS, user)
                .param("solicitorRecoveryDate", "01/01/1900")
                .param("courtRecoveryDate", "12/12/1901")
                .param("clientRecoveryDate", "13/12/1901"))
        .andExpect(view().name("application/recovery"))
        .andExpect(
            model()
                .attributeHasFieldErrors(
                    "recovery",
                    "solicitorRecoveryDate",
                    "courtRecoveryDate",
                    "clientRecoveryDate"));
    verify(caseOutcomeService, never()).upsertRecovery(any(), any(), any(), any(), any(), any());
  }

  @ParameterizedTest
  @CsvSource({"cost-award,COST", "financial-award,DAMAGE"})
  void savesOnlyEditableRecoveryInputsOnNext(String path, String type) throws Exception {
    stubAward(path, null);
    mvc.perform(
            post(url(path))
                .sessionAttr(CASE, ebsCase)
                .sessionAttr(USER_DETAILS, user)
                .param("solicitorRecoveryAmount", "10.5")
                .param("courtRecoveryAmount", "")
                .param("clientRecoveryAmount", "20")
                .param("solicitorRecoveryDate", "14/12/1901")
                .param("solicitorAmountPaidToLsc", "1")
                .param("courtAmountPaidToLsc", "2")
                .param("clientAmountPaidToLsc", "3")
                .param("leaveOfCourtRequiredInd", "true")
                .param("offeredAmount", "4")
                .param("offerDetails", "Terms")
                .param("awardAmount", "999999")
                .param("awardType", "LAND")
                .param("id", "500")
                .param("recoveredAmount", "9999"))
        .andExpect(redirectedUrl("/case/outcome-and-awards"));
    final var captor = ArgumentCaptor.forClass(RecoveryRequest.class);
    verify(caseOutcomeService)
        .upsertRecovery(eq("300000001"), eq(123), eq(type), eq(7), captor.capture(), any());
    final var request = captor.getValue();
    assertThat(request.getSolicitorRecoveryAmount()).isEqualTo(new BigDecimal("10.50"));
    assertThat(request.getCourtRecoveryAmount()).isEqualTo(new BigDecimal("0.00"));
    assertThat(request.getClientRecoveryAmount()).isEqualTo(new BigDecimal("20.00"));
    assertThat(request.getCourtAmountPaidToLsc()).isEqualTo(new BigDecimal("2.00"));
    assertThat(request.getClientAmountPaidToLsc()).isEqualTo(new BigDecimal("3.00"));
    assertThat(request.getSolicitorRecoveryDate()).isEqualTo(DateUtils.convertToDate("14/12/1901"));
    assertThat(request.getLeaveOfCourtRequiredInd()).isTrue();
    assertThat(request.getOfferDetails()).isEqualTo("Terms");
  }

  @ParameterizedTest
  @ValueSource(strings = {"calculate", "next"})
  void retainsInvalidInputAndNeverSaves(String action) throws Exception {
    stubAward("cost-award", null);
    mvc.perform(
            post(url("cost-award"))
                .sessionAttr(CASE, ebsCase)
                .sessionAttr(USER_DETAILS, user)
                .param("action", action)
                .param("solicitorRecoveryAmount", "-1")
                .param("courtRecoveryDate", "31/02/2026")
                .param("leaveOfCourtRequiredInd", "invalid"))
        .andExpect(view().name("application/recovery"))
        .andExpect(
            model()
                .attributeHasFieldErrors(
                    "recovery",
                    "solicitorRecoveryAmount",
                    "courtRecoveryDate",
                    "leaveOfCourtRequiredInd"));
    verify(caseOutcomeService, never()).upsertRecovery(any(), any(), any(), any(), any(), any());
  }

  @ParameterizedTest
  @CsvSource({"cost-award,COST", "financial-award,DAMAGE"})
  void savesLegacyCleanedDatesAndOfferTextWithoutTrimmingTheOffer(String path, String type)
      throws Exception {
    stubAward(path, null);
    mvc.perform(
            post(url(path))
                .sessionAttr(CASE, ebsCase)
                .sessionAttr(USER_DETAILS, user)
                .param("solicitorRecoveryAmount", " ")
                .param("solicitorRecoveryDate", " \t14/12/1901 ")
                .param("courtRecoveryDate", " 01/01/2000 ")
                .param("clientRecoveryDate", " \t ")
                .param("offerDetails", " Term\u0000s\r\n  Second\rThird\n "))
        .andExpect(redirectedUrl("/case/outcome-and-awards"));
    final var captor = ArgumentCaptor.forClass(RecoveryRequest.class);
    verify(caseOutcomeService)
        .upsertRecovery(eq("300000001"), eq(123), eq(type), eq(7), captor.capture(), any());
    final var request = captor.getValue();
    assertThat(request.getSolicitorRecoveryDate()).isEqualTo(DateUtils.convertToDate("14/12/1901"));
    assertThat(request.getCourtRecoveryDate()).isEqualTo(DateUtils.convertToDate("01/01/2000"));
    assertThat(request.getClientRecoveryDate()).isNull();
    assertThat(request.getSolicitorRecoveryAmount()).isEqualTo(new BigDecimal("0.00"));
    assertThat(request.getLeaveOfCourtRequiredInd()).isNull();
    assertThat(request.getOfferDetails()).isEqualTo(" Terms\n  Second\rThird\n ");
  }

  @ParameterizedTest
  @ValueSource(strings = {"cost-award", "financial-award"})
  void calculatesUsingLegacyCleanedDatesAndRedisplaysNormalisedOfferText(String path)
      throws Exception {
    stubAward(path, null);
    final var result =
        mvc.perform(
                post(url(path))
                    .sessionAttr(CASE, ebsCase)
                    .sessionAttr(USER_DETAILS, user)
                    .param("action", "calculate")
                    .param("solicitorRecoveryDate", " 14/12/1901 ")
                    .param("courtRecoveryDate", "\t01/01/2000\t")
                    .param("clientRecoveryDate", " 02/01/2000 ")
                    .param("offerDetails", " Terms\r\n  More terms "))
            .andExpect(view().name("application/recovery"))
            .andExpect(model().hasNoErrors())
            .andReturn();
    final var form = (RecoveryFormData) result.getModelAndView().getModel().get("recovery");
    assertThat(form.getSolicitorRecoveryDate()).isEqualTo("14/12/1901");
    assertThat(form.getCourtRecoveryDate()).isEqualTo("01/01/2000");
    assertThat(form.getClientRecoveryDate()).isEqualTo("02/01/2000");
    assertThat(form.getOfferDetails()).isEqualTo(" Terms\n  More terms ");
    verify(caseOutcomeService, never()).upsertRecovery(any(), any(), any(), any(), any(), any());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "solicitorRecoveryAmount",
        "solicitorAmountPaidToLsc",
        "courtRecoveryAmount",
        "courtAmountPaidToLsc",
        "clientRecoveryAmount",
        "clientAmountPaidToLsc",
        "offeredAmount"
      })
  void rejectsWhitespaceAroundAmountsRatherThanSilentlyChangingThem(String field) throws Exception {
    stubAward("cost-award", null);
    mvc.perform(
            post(url("cost-award"))
                .sessionAttr(CASE, ebsCase)
                .sessionAttr(USER_DETAILS, user)
                .param(field, " 1.00 "))
        .andExpect(view().name("application/recovery"))
        .andExpect(model().attributeHasFieldErrors("recovery", field));
    verify(caseOutcomeService, never()).upsertRecovery(any(), any(), any(), any(), any(), any());
  }

  @ParameterizedTest
  @ValueSource(strings = {"\tTerms", "Terms\t", " Terms <script> "})
  void retainsAndRejectsInvalidOfferCharactersRatherThanTrimmingThemAway(String offer)
      throws Exception {
    stubAward("cost-award", null);
    final var result =
        mvc.perform(
                post(url("cost-award"))
                    .sessionAttr(CASE, ebsCase)
                    .sessionAttr(USER_DETAILS, user)
                    .param("offerDetails", offer))
            .andExpect(view().name("application/recovery"))
            .andExpect(model().attributeHasFieldErrors("recovery", "offerDetails"))
            .andReturn();
    final var form = (RecoveryFormData) result.getModelAndView().getModel().get("recovery");
    assertThat(form.getOfferDetails()).isEqualTo(offer);
    verify(caseOutcomeService, never()).upsertRecovery(any(), any(), any(), any(), any(), any());
  }

  @ParameterizedTest
  @CsvSource({
    "cost-award,calculate", "cost-award,next",
    "financial-award,calculate", "financial-award,next"
  })
  void rejectsOverlengthWhitespaceOnlyOfferWithoutSaving(String path, String action)
      throws Exception {
    stubAward(path, null);
    final String offer = " ".repeat(951);
    final var result =
        mvc.perform(
                post(url(path))
                    .sessionAttr(CASE, ebsCase)
                    .sessionAttr(USER_DETAILS, user)
                    .param("action", action)
                    .param("offerDetails", offer))
            .andExpect(view().name("application/recovery"))
            .andExpect(model().attributeHasFieldErrors("recovery", "offerDetails"))
            .andReturn();
    final var form = (RecoveryFormData) result.getModelAndView().getModel().get("recovery");
    assertThat(form.getOfferDetails()).isEqualTo(offer);
    verify(caseOutcomeService, never()).upsertRecovery(any(), any(), any(), any(), any(), any());
  }

  @ParameterizedTest
  @CsvSource({
    "cost-award,COST,space", "cost-award,COST,tab",
    "financial-award,DAMAGE,space", "financial-award,DAMAGE,tab"
  })
  void savesWhitespaceOnlyOfferUnchangedAsInLegacy(String path, String type, String whitespace)
      throws Exception {
    stubAward(path, null);
    final String offer = "space".equals(whitespace) ? " ".repeat(950) : "\t";
    mvc.perform(
            post(url(path))
                .sessionAttr(CASE, ebsCase)
                .sessionAttr(USER_DETAILS, user)
                .param("offerDetails", offer))
        .andExpect(redirectedUrl("/case/outcome-and-awards"));
    final var captor = ArgumentCaptor.forClass(RecoveryRequest.class);
    verify(caseOutcomeService)
        .upsertRecovery(eq("300000001"), eq(123), eq(type), eq(7), captor.capture(), any());
    assertThat(captor.getValue().getOfferDetails()).isEqualTo(offer);
  }

  @ParameterizedTest
  @CsvSource({"948,false", "949,true"})
  void validatesOfferLengthAfterLegacyLineEndingNormalisation(int letters, boolean invalid)
      throws Exception {
    stubAward("cost-award", null);
    mvc.perform(
            post(url("cost-award"))
                .sessionAttr(CASE, ebsCase)
                .sessionAttr(USER_DETAILS, user)
                .param("action", "calculate")
                .param("offerDetails", "a".repeat(letters) + "\r\nb"))
        .andExpect(view().name("application/recovery"))
        .andExpect(
            invalid
                ? model().attributeHasFieldErrors("recovery", "offerDetails")
                : model().hasNoErrors());
    verify(caseOutcomeService, never()).upsertRecovery(any(), any(), any(), any(), any(), any());
  }

  @ParameterizedTest
  @CsvSource({
    "cost-award,back",
    "financial-award,back",
    "cost-award,return",
    "financial-award,return",
    "cost-award,unknown",
    "financial-award,unknown"
  })
  void rejectsUnsupportedActionsWithoutSaving(String path, String action) throws Exception {
    stubAward(path, null);
    final var result =
        mvc.perform(
                post(url(path))
                    .sessionAttr(CASE, ebsCase)
                    .sessionAttr(USER_DETAILS, user)
                    .param("action", action)
                    .param("offerDetails", "Terms"))
            .andExpect(view().name("application/recovery"))
            .andExpect(model().attributeHasErrors("recovery"))
            .andReturn();
    final var errors =
        (BindingResult)
            result.getModelAndView().getModel().get(BindingResult.MODEL_KEY_PREFIX + "recovery");
    assertThat(errors.getGlobalError()).isNotNull();
    assertThat(errors.getGlobalError().getCode()).isEqualTo("recovery.action.invalid");
    final var form = (RecoveryFormData) errors.getTarget();
    assertThat(form.getOfferDetails()).isEqualTo("Terms");
    verify(caseOutcomeService, never()).upsertRecovery(any(), any(), any(), any(), any(), any());
  }

  @Test
  void surfacesSaveFailureInsteadOfRedirecting() throws Exception {
    stubAward("cost-award", null);
    doThrow(new IllegalStateException("API unavailable"))
        .when(caseOutcomeService)
        .upsertRecovery(any(), any(), any(), any(), any(), any());
    mvc.perform(post(url("cost-award")).sessionAttr(CASE, ebsCase).sessionAttr(USER_DETAILS, user))
        .andExpect(view().name("application/recovery"))
        .andExpect(model().attributeHasErrors("recovery"));
  }

  @Test
  void blocksReadOnlyRequestsBeforeLoadingOrSaving() throws Exception {
    final var readOnly = new ApplicationDetail().availableFunctions(List.of());
    mvc.perform(get(url("cost-award")).sessionAttr(CASE, readOnly).sessionAttr(USER_DETAILS, user))
        .andExpect(redirectedUrl("/case/outcome-and-awards"));
    mvc.perform(post(url("cost-award")).sessionAttr(CASE, readOnly).sessionAttr(USER_DETAILS, user))
        .andExpect(redirectedUrl("/case/outcome-and-awards"));
    verifyNoInteractions(caseOutcomeService);
  }

  private String url(String path) {
    return "/case/outcome-and-awards/" + path + "/7/recovery";
  }
}
