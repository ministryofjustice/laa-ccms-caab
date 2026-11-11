package uk.gov.laa.ccms.caab.controller.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_AWARD_ASSESSMENT_STATUS;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_VALUATION_BASIS;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.ACTIVE_CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.AWARD_TYPE_FORM;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.advice.ActiveCaseModelAdvice;
import uk.gov.laa.ccms.caab.advice.GlobalBinderAdvice;
import uk.gov.laa.ccms.caab.bean.ActiveCase;
import uk.gov.laa.ccms.caab.bean.AwardTypeForm;
import uk.gov.laa.ccms.caab.bean.award.CostAwardFormData;
import uk.gov.laa.ccms.caab.bean.award.FinancialAwardFormData;
import uk.gov.laa.ccms.caab.bean.award.LandAwardFormData;
import uk.gov.laa.ccms.caab.bean.award.OtherAssetAwardFormData;
import uk.gov.laa.ccms.caab.bean.award.RecoveryFormData;
import uk.gov.laa.ccms.caab.bean.award.RecoveryRequestData;
import uk.gov.laa.ccms.caab.bean.validators.BaselineTextValidator;
import uk.gov.laa.ccms.caab.bean.validators.application.AwardTypeValidator;
import uk.gov.laa.ccms.caab.bean.validators.awards.CostAwardValidator;
import uk.gov.laa.ccms.caab.bean.validators.awards.FinancialAwardValidator;
import uk.gov.laa.ccms.caab.bean.validators.awards.LandAwardValidator;
import uk.gov.laa.ccms.caab.bean.validators.awards.OtherAssetAwardValidator;
import uk.gov.laa.ccms.caab.bean.validators.awards.RecoveryValidator;
import uk.gov.laa.ccms.caab.bean.validators.awards.TimeRecoveryValidator;
import uk.gov.laa.ccms.caab.client.CaabApiClientException;
import uk.gov.laa.ccms.caab.constants.FunctionConstants;
import uk.gov.laa.ccms.caab.mapper.CostAwardMapper;
import uk.gov.laa.ccms.caab.mapper.FinancialAwardMapper;
import uk.gov.laa.ccms.caab.mapper.LandAwardMapper;
import uk.gov.laa.ccms.caab.mapper.LiablePartiesDisplayMapper;
import uk.gov.laa.ccms.caab.mapper.OtherAssetAwardMapper;
import uk.gov.laa.ccms.caab.mapper.RecoveryMapper;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.CaseOutcomeDetail;
import uk.gov.laa.ccms.caab.model.CostAwardDetail;
import uk.gov.laa.ccms.caab.model.FinancialAwardDetail;
import uk.gov.laa.ccms.caab.model.FinancialAwardRequest;
import uk.gov.laa.ccms.caab.model.LandAwardDetail;
import uk.gov.laa.ccms.caab.model.LandAwardRequest;
import uk.gov.laa.ccms.caab.model.OtherAssetAwardDetail;
import uk.gov.laa.ccms.caab.model.OtherAssetAwardRequest;
import uk.gov.laa.ccms.caab.model.RecoveryDetail;
import uk.gov.laa.ccms.caab.model.TimeRecoveryDetail;
import uk.gov.laa.ccms.caab.model.TimeRecoveryRequest;
import uk.gov.laa.ccms.caab.model.sections.LiablePartiesDisplay;
import uk.gov.laa.ccms.caab.service.CaseOutcomeService;
import uk.gov.laa.ccms.caab.service.LookupService;
import uk.gov.laa.ccms.caab.util.DateUtils;
import uk.gov.laa.ccms.data.model.AwardTypeLookupDetail;
import uk.gov.laa.ccms.data.model.AwardTypeLookupValueDetail;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.UserDetail;

@ExtendWith(MockitoExtension.class)
class AwardControllerTest {

  @Mock private LookupService lookupService;
  @Mock private CaseOutcomeService caseOutcomeService;
  @Mock private AwardTypeValidator awardTypeValidator;
  @Mock private CostAwardValidator costAwardValidator;
  @Mock private FinancialAwardValidator financialAwardValidator;
  @Mock private CostAwardMapper costAwardMapper;
  @Mock private FinancialAwardMapper financialAwardMapper;
  @Mock private OtherAssetAwardValidator otherAssetAwardValidator;
  @Mock private OtherAssetAwardMapper otherAssetAwardMapper;
  @Mock private LandAwardValidator landAwardValidator;
  @Mock private LandAwardMapper landAwardMapper;
  @Spy private TimeRecoveryValidator timeRecoveryValidator = new TimeRecoveryValidator();
  @Spy private RecoveryValidator recoveryValidator = new RecoveryValidator();
  @Spy private RecoveryMapper recoveryMapper = Mappers.getMapper(RecoveryMapper.class);

  @Spy
  private LiablePartiesDisplayMapper awardDisplayMapper =
      Mappers.getMapper(LiablePartiesDisplayMapper.class);

  @InjectMocks private AwardController controller;

  private MockMvcTester mockMvc;
  private ApplicationDetail ebsCase;
  private UserDetail user;
  private CaseOutcomeDetail caseOutcome;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcTester.create(MockMvcBuilders.standaloneSetup(controller).build());
    ebsCase = new ApplicationDetail().caseReferenceNumber("300000001");
    ebsCase.setAvailableFunctions(List.of(FunctionConstants.OUTCOME_WITH_DISCHARGE));
    caseOutcome = new CaseOutcomeDetail().id(55);
    ebsCase.setCaseOutcome(caseOutcome);
    user = ApplicationTestUtils.buildUser();
    lenient().when(costAwardMapper.toCostAward(any())).thenReturn(new CostAwardDetail());
    lenient()
        .when(financialAwardMapper.toFinancialAwardRequest(any()))
        .thenReturn(new FinancialAwardRequest());
    lenient()
        .when(otherAssetAwardMapper.toOtherAssetAwardRequest(any()))
        .thenReturn(new OtherAssetAwardRequest());
    lenient().when(landAwardMapper.toLandAwardRequest(any())).thenReturn(new LandAwardRequest());
    lenient()
        .when(lookupService.getCommonValues(any()))
        .thenReturn(Mono.just(new CommonLookupDetail().content(Collections.emptyList())));
  }

  @Nested
  @DisplayName("Recovery for cost and financial awards")
  class RecoveryTests {
    private MockMvc mvc;

    @BeforeEach
    void setUpRecovery() {
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
    void includesActiveCaseBannerOnDisplayAndRedisplay(String path, String action)
        throws Exception {
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
      final var captor = ArgumentCaptor.forClass(RecoveryRequestData.class);
      verify(caseOutcomeService)
          .upsertRecovery(eq("300000001"), eq(123), eq(type), eq(7), captor.capture(), any());
      final var request = captor.getValue();
      assertThat(request.getSolicitorRecoveryAmount()).isEqualTo(new BigDecimal("10.50"));
      assertThat(request.getCourtRecoveryAmount()).isEqualTo(new BigDecimal("0.00"));
      assertThat(request.getClientRecoveryAmount()).isEqualTo(new BigDecimal("20.00"));
      assertThat(request.getCourtAmountPaidToLsc()).isEqualTo(new BigDecimal("2.00"));
      assertThat(request.getClientAmountPaidToLsc()).isEqualTo(new BigDecimal("3.00"));
      assertThat(request.getSolicitorRecoveryDate())
          .isEqualTo(DateUtils.convertToDate("14/12/1901"));
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
      final var captor = ArgumentCaptor.forClass(RecoveryRequestData.class);
      verify(caseOutcomeService)
          .upsertRecovery(eq("300000001"), eq(123), eq(type), eq(7), captor.capture(), any());
      final var request = captor.getValue();
      assertThat(request.getSolicitorRecoveryDate())
          .isEqualTo(DateUtils.convertToDate("14/12/1901"));
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
    void rejectsWhitespaceAroundAmountsRatherThanSilentlyChangingThem(String field)
        throws Exception {
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
      mvc.perform(
              post(url("cost-award")).sessionAttr(CASE, ebsCase).sessionAttr(USER_DETAILS, user))
          .andExpect(view().name("application/recovery"))
          .andExpect(model().attributeHasErrors("recovery"));
    }

    @Test
    void blocksReadOnlyRequestsBeforeLoadingOrSaving() throws Exception {
      final var readOnly = new ApplicationDetail().availableFunctions(List.of());
      mvc.perform(
              get(url("cost-award")).sessionAttr(CASE, readOnly).sessionAttr(USER_DETAILS, user))
          .andExpect(redirectedUrl("/case/outcome-and-awards"));
      mvc.perform(
              post(url("cost-award")).sessionAttr(CASE, readOnly).sessionAttr(USER_DETAILS, user))
          .andExpect(redirectedUrl("/case/outcome-and-awards"));
      verifyNoInteractions(caseOutcomeService);
    }

    private String url(String path) {
      return "/case/outcome-and-awards/" + path + "/7/recovery";
    }
  }

  @Nested
  @DisplayName("GET: award liable and interested parties")
  class LiablePartiesTests {

    @ParameterizedTest
    @CsvSource({
      "cost-award, false, cost-award/7",
      "financial-award, false, financial-award/7",
      "land, false, land-property/7",
      "asset, false, asset/7",
      "land, true, land/7/time-related-recovery",
      "asset, true, asset/7/time-related-recovery"
    })
    void loadsSelectedAwardWithinCurrentCaseAndProvider(
        String awardPath, boolean timeRelatedRecovery, String backPath) {
      final var award =
          new LiablePartiesDisplay(
              "LAND", "Property", BigDecimal.TEN, false, timeRelatedRecovery, List.of());
      when(caseOutcomeService.getLiablePartiesDisplay("300000001", 123, awardPath, 7))
          .thenReturn(award);

      assertThat(
              mockMvc.perform(
                  get("/case/outcome-and-awards/" + awardPath + "/7/liable-parties")
                      .sessionAttr(CASE, ebsCase)
                      .sessionAttr(USER_DETAILS, user)))
          .hasStatusOk()
          .hasViewName("application/liable-parties")
          .model()
          .containsEntry("award", award)
          .containsEntry("awardPath", awardPath)
          .containsEntry("awardId", 7)
          .containsEntry("backUrl", "/case/outcome-and-awards/" + backPath);

      verify(caseOutcomeService).getLiablePartiesDisplay("300000001", 123, awardPath, 7);
      verifyNoMoreInteractions(caseOutcomeService);
    }

    @Test
    void unsupportedAwardHasNoRoute() {
      assertThat(mockMvc.perform(get("/case/outcome-and-awards/unknown/7/liable-parties")))
          .hasStatus(404);
      verifyNoInteractions(caseOutcomeService);
    }
  }

  @Nested
  @DisplayName("Award deletion")
  class DeleteAwardTests {

    @ParameterizedTest
    @ValueSource(strings = {"COST", "DAMAGE", "LAND", "ASSET"})
    void confirmationPassesOnlyAwardTypeAndIdWithoutDeleting(String type) {
      assertThat(
              mockMvc.perform(
                  get("/case/outcome-and-awards/awards/{type}/7/delete", type)
                      .sessionAttr(CASE, ebsCase)))
          .hasStatusOk()
          .hasViewName("application/delete-award")
          .model()
          .containsEntry("awardType", type)
          .containsEntry("awardId", 7)
          .doesNotContainKey("award");

      verifyNoInteractions(caseOutcomeService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"COST", "DAMAGE", "LAND", "ASSET"})
    void confirmDeletesSelectedAwardAndRedirects(String type) {
      assertThat(
              mockMvc.perform(
                  post("/case/outcome-and-awards/awards/{type}/7/delete", type)
                      .sessionAttr(CASE, ebsCase)
                      .sessionAttr(USER_DETAILS, user)))
          .hasStatus3xxRedirection()
          .hasRedirectedUrl("/case/outcome-and-awards");

      verify(caseOutcomeService)
          .deleteAward(
              ebsCase.getCaseReferenceNumber(),
              user.getProvider().getId().intValue(),
              type,
              7,
              user.getLoginId());
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "POST"})
    void readOnlyCaseCannotLoadOrConfirmDeletion(String method) {
      ebsCase.setAvailableFunctions(List.of(FunctionConstants.VIEW_CASE_OUTCOME));
      MockHttpServletRequestBuilder request =
          "GET".equals(method)
              ? get("/case/outcome-and-awards/awards/COST/7/delete")
              : post("/case/outcome-and-awards/awards/COST/7/delete");

      assertThat(
              mockMvc.perform(request.sessionAttr(CASE, ebsCase).sessionAttr(USER_DETAILS, user)))
          .hasStatus3xxRedirection()
          .hasRedirectedUrl("/case/outcome-and-awards");
      verifyNoInteractions(caseOutcomeService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "POST"})
    void missingCaseCannotLoadOrConfirmDeletion(String method) {
      MockHttpServletRequestBuilder request =
          "GET".equals(method)
              ? get("/case/outcome-and-awards/awards/COST/7/delete")
              : post("/case/outcome-and-awards/awards/COST/7/delete");

      assertThat(mockMvc.perform(request.sessionAttr(USER_DETAILS, user)))
          .hasStatus3xxRedirection()
          .hasRedirectedUrl("/case/outcome-and-awards");
      verifyNoInteractions(caseOutcomeService);
    }

    @Test
    void deletionFailureDoesNotRedirectAsSuccess() {
      doThrow(new IllegalStateException("Deletion failed"))
          .when(caseOutcomeService)
          .deleteAward(any(), any(), any(), any(), any());

      assertThat(
              mockMvc.perform(
                  post("/case/outcome-and-awards/awards/COST/7/delete")
                      .sessionAttr(CASE, ebsCase)
                      .sessionAttr(USER_DETAILS, user)))
          .hasFailed()
          .failure()
          .hasCauseInstanceOf(IllegalStateException.class);
    }
  }

  @Nested
  @DisplayName("GET: /case/outcome-and-awards/award-type")
  class SelectAwardTypeTests {

    @Test
    @DisplayName("Should display award types returned by the lookup service to the model")
    void shouldDisplayAwardTypesReturnedByLookup() {
      final AwardTypeLookupValueDetail costAward =
          new AwardTypeLookupValueDetail()
              .code("COST_AWARD")
              .description("Cost award")
              .awardType("COST")
              .enabled(true);

      final AwardTypeLookupDetail awardTypes =
          new AwardTypeLookupDetail().addContentItem(costAward);

      when(lookupService.getAwardTypes()).thenReturn(Mono.just(awardTypes));

      assertThat(
              mockMvc.perform(
                  get("/case/outcome-and-awards/award-type").sessionAttr(CASE, ebsCase)))
          .hasStatusOk()
          .hasViewName("application/select-award-type")
          .model()
          .containsEntry("awardTypes", awardTypes.getContent())
          .hasEntrySatisfying(
              AWARD_TYPE_FORM, value -> assertThat(value).isInstanceOf(AwardTypeForm.class));

      verify(lookupService).getAwardTypes();
    }

    @Test
    @DisplayName("Should display an empty award type list when the lookup returns no data")
    void shouldDisplayEmptyAwardTypesWhenLookupReturnsNoData() {
      when(lookupService.getAwardTypes()).thenReturn(Mono.empty());

      assertThat(
              mockMvc.perform(
                  get("/case/outcome-and-awards/award-type").sessionAttr(CASE, ebsCase)))
          .hasStatusOk()
          .hasViewName("application/select-award-type")
          .model()
          .containsEntry("awardTypes", Collections.emptyList())
          .hasEntrySatisfying(
              AWARD_TYPE_FORM, value -> assertThat(value).isInstanceOf(AwardTypeForm.class));

      verify(lookupService).getAwardTypes();
    }

    @Test
    @DisplayName("Should redirect read-only cases away from award type management")
    void shouldRedirectReadOnlyCaseFromAwardTypeManagement() {
      ebsCase.setAvailableFunctions(List.of(FunctionConstants.VIEW_CASE_OUTCOME));

      assertThat(
              mockMvc.perform(
                  get("/case/outcome-and-awards/award-type")
                      .sessionAttr(CASE, ebsCase)
                      .sessionAttr(USER_DETAILS, user)))
          .hasStatus3xxRedirection()
          .hasRedirectedUrl("/case/outcome-and-awards");
    }

    @Test
    @DisplayName("Should redirect award type management when no case is in session")
    void shouldRedirectAwardTypeManagementWithoutCase() {
      assertThat(mockMvc.perform(get("/case/outcome-and-awards/award-type")))
          .hasStatus3xxRedirection()
          .hasRedirectedUrl("/case/outcome-and-awards");
      assertThat(
              mockMvc.perform(
                  post("/case/outcome-and-awards/award-type")
                      .sessionAttr(AWARD_TYPE_FORM, new AwardTypeForm())
                      .param("awardTypeCode", "COST")))
          .hasStatus3xxRedirection()
          .hasRedirectedUrl("/case/outcome-and-awards");

      verifyNoInteractions(lookupService, awardTypeValidator);
    }
  }

  @Nested
  @DisplayName("POST: /case/outcome-and-awards/award-type")
  class SubmitAwardTypeTests {

    @ParameterizedTest(name = "{0} resolves to {2} and redirects to {3}")
    @CsvSource({
      "FIN_ASSET, Financial Asset, ASSET, /case/outcome-and-awards/asset",
      "OTH_ASSET, Other Asset, ASSET, /case/outcome-and-awards/asset",
      "ENFORCEMENT, Enforcement Cost Award, COST, /case/outcome-and-awards/cost-award",
      "COST, Cost Award, COST, /case/outcome-and-awards/cost-award",
      "COST_AGR, Cost Settlement, COST, /case/outcome-and-awards/cost-award",
      "DAMAGE, Financial or Punitive Damages, DAMAGE, /case/outcome-and-awards/financial-award",
      "DAMAGE_AGR, Financial Settlement, DAMAGE, /case/outcome-and-awards/financial-award",
      "LAND, Land/Property, LAND, /case/outcome-and-awards/land-property"
    })
    void shouldRedirectAccordingToAwardType(
        final String code,
        final String description,
        final String awardType,
        final String expectedRedirect) {

      final AwardTypeLookupValueDetail lookupValue =
          new AwardTypeLookupValueDetail()
              .code(code)
              .description(description)
              .awardType(awardType)
              .enabled(true);

      final AwardTypeLookupDetail lookupDetail =
          new AwardTypeLookupDetail().addContentItem(lookupValue);

      when(lookupService.getAwardTypes()).thenReturn(Mono.just(lookupDetail));

      final AwardTypeForm awardTypeForm = new AwardTypeForm();

      assertThat(
              mockMvc.perform(
                  post("/case/outcome-and-awards/award-type")
                      .sessionAttr(CASE, ebsCase)
                      .sessionAttr(AWARD_TYPE_FORM, awardTypeForm)
                      .param("awardTypeCode", code)))
          .hasStatus3xxRedirection()
          .hasRedirectedUrl(expectedRedirect);

      assertThat(awardTypeForm.getAwardTypeCode()).isEqualTo(code);
      assertThat(awardTypeForm.getDescription())
          .isEqualTo(StringUtils.capitalize(awardType.toLowerCase()));
      assertThat(awardTypeForm.getAwardType()).isEqualTo(awardType);

      verify(awardTypeValidator).validate(any(AwardTypeForm.class), any());
      verify(lookupService).getAwardTypes();
    }

    @Test
    @DisplayName("Should return select award type view when validation fails")
    void shouldReturnSelectAwardTypeViewWhenValidationFails() {
      final AwardTypeLookupValueDetail lookupValue =
          new AwardTypeLookupValueDetail()
              .code("FIN_ASSET")
              .description("Financial Asset")
              .awardType("ASSET")
              .enabled(true);

      final AwardTypeLookupDetail lookupDetail =
          new AwardTypeLookupDetail().addContentItem(lookupValue);

      when(lookupService.getAwardTypes()).thenReturn(Mono.just(lookupDetail));

      doAnswer(
              invocation -> {
                final Errors errors = invocation.getArgument(1);
                errors.rejectValue(
                    "awardTypeCode", "required.awardTypeCode", "Please complete 'Award type'.");
                return null;
              })
          .when(awardTypeValidator)
          .validate(any(AwardTypeForm.class), any());

      final AwardTypeForm awardTypeForm = new AwardTypeForm();

      assertThat(
              mockMvc.perform(
                  post("/case/outcome-and-awards/award-type")
                      .sessionAttr(CASE, ebsCase)
                      .sessionAttr(AWARD_TYPE_FORM, awardTypeForm)
                      .param("awardTypeCode", "")))
          .hasStatusOk()
          .hasViewName("application/select-award-type")
          .model()
          .containsEntry("awardTypes", lookupDetail.getContent())
          .hasEntrySatisfying(
              BindingResult.MODEL_KEY_PREFIX + AWARD_TYPE_FORM,
              value -> {
                final BindingResult result = (BindingResult) value;

                assertThat(result.hasFieldErrors("awardTypeCode")).isTrue();
                assertThat(result.getFieldError("awardTypeCode")).isNotNull();
                assertThat(result.getFieldError("awardTypeCode").getCode())
                    .isEqualTo("required.awardTypeCode");
              });

      verify(awardTypeValidator).validate(any(AwardTypeForm.class), any());
      verify(lookupService).getAwardTypes();
    }

    @Test
    @DisplayName("Should reject an award type code that is not present in the lookup")
    void shouldRejectUnknownAwardTypeCode() {
      final AwardTypeLookupValueDetail lookupValue =
          new AwardTypeLookupValueDetail()
              .code("FIN_ASSET")
              .description("Financial Asset")
              .awardType("ASSET")
              .enabled(true);

      final AwardTypeLookupDetail lookupDetail =
          new AwardTypeLookupDetail().addContentItem(lookupValue);

      when(lookupService.getAwardTypes()).thenReturn(Mono.just(lookupDetail));

      final AwardTypeForm awardTypeForm = new AwardTypeForm();

      assertThat(
              mockMvc.perform(
                  post("/case/outcome-and-awards/award-type")
                      .sessionAttr(CASE, ebsCase)
                      .sessionAttr(AWARD_TYPE_FORM, awardTypeForm)
                      .param("awardTypeCode", "UNKNOWN")))
          .hasStatusOk()
          .hasViewName("application/select-award-type")
          .model()
          .containsEntry("awardTypes", lookupDetail.getContent())
          .hasEntrySatisfying(
              BindingResult.MODEL_KEY_PREFIX + AWARD_TYPE_FORM,
              value -> {
                final BindingResult result = (BindingResult) value;

                assertThat(result.hasFieldErrors("awardTypeCode")).isTrue();
                assertThat(result.getFieldError("awardTypeCode")).isNotNull();
                assertThat(result.getFieldError("awardTypeCode").getCode())
                    .isEqualTo("awardType.invalid");
                assertThat(result.getFieldError("awardTypeCode").getDefaultMessage())
                    .isEqualTo("Please select a valid award type.");
              });

      assertThat(awardTypeForm.getAwardTypeCode()).isEqualTo("UNKNOWN");

      verify(awardTypeValidator).validate(any(AwardTypeForm.class), any());
      verify(lookupService).getAwardTypes();
    }

    @Test
    @DisplayName("Should reject an unsupported award type category")
    void shouldRejectUnsupportedAwardTypeCategory() {
      final AwardTypeLookupValueDetail lookupValue =
          new AwardTypeLookupValueDetail()
              .code("NEW_AWARD")
              .description("New award")
              .awardType("UNKNOWN")
              .enabled(true);

      final AwardTypeLookupDetail lookupDetail =
          new AwardTypeLookupDetail().addContentItem(lookupValue);

      when(lookupService.getAwardTypes()).thenReturn(Mono.just(lookupDetail));

      final AwardTypeForm awardTypeForm = new AwardTypeForm();

      assertThat(
              mockMvc.perform(
                  post("/case/outcome-and-awards/award-type")
                      .sessionAttr(CASE, ebsCase)
                      .sessionAttr(AWARD_TYPE_FORM, awardTypeForm)
                      .param("awardTypeCode", "NEW_AWARD")))
          .hasStatusOk()
          .hasViewName("application/select-award-type")
          .model()
          .containsEntry("awardTypes", lookupDetail.getContent())
          .hasEntrySatisfying(
              BindingResult.MODEL_KEY_PREFIX + AWARD_TYPE_FORM,
              value -> {
                final BindingResult result = (BindingResult) value;

                assertThat(result.hasFieldErrors("awardTypeCode")).isTrue();
                assertThat(result.getFieldError("awardTypeCode")).isNotNull();
                assertThat(result.getFieldError("awardTypeCode").getCode())
                    .isEqualTo("awardType.unsupported");
                assertThat(result.getFieldError("awardTypeCode").getDefaultMessage())
                    .isEqualTo("The selected award type is not supported.");
              });

      assertThat(awardTypeForm.getAwardTypeCode()).isEqualTo("NEW_AWARD");
      assertThat(awardTypeForm.getDescription()).isEqualTo("Unknown");
      assertThat(awardTypeForm.getAwardType()).isEqualTo("UNKNOWN");

      verify(awardTypeValidator).validate(any(AwardTypeForm.class), any());
      verify(lookupService).getAwardTypes();
    }
  }

  @Nested
  @DisplayName("GET: /case/outcome-and-awards/cost-award")
  class CostAwardGetTests {

    @Test
    void getNewCostAwardPreservesSelectedAwardMetadataAndInitialisesTotals() {
      final AwardTypeForm awardTypeForm = new AwardTypeForm();
      awardTypeForm.setAwardTypeCode("COST_AGR");
      awardTypeForm.setAwardType("COST");
      awardTypeForm.setDescription("Cost");

      assertThat(
              mockMvc.perform(
                  get("/case/outcome-and-awards/cost-award")
                      .sessionAttr(AWARD_TYPE_FORM, awardTypeForm)
                      .sessionAttr(CASE, ebsCase)
                      .sessionAttr(USER_DETAILS, user)))
          .hasViewName("application/cost-award")
          .model()
          .containsEntry("totalPreCertificateCosts", "0.00")
          .containsEntry("totalCertificateCostsAwarded", "0.00")
          .containsEntry("totalCostsAwarded", "0.00")
          .hasEntrySatisfying(
              "costAward",
              value ->
                  assertThat(value)
                      .extracting(
                          "awardCode",
                          "awardType",
                          "description",
                          "laaFundedLegalCosts",
                          "otherPreCertificateCosts",
                          "laaRate",
                          "marketRate",
                          "interestRate")
                      .containsExactly(
                          "COST_AGR", "COST", "Cost", "0.00", "0.00", "0.00", "0.00", "0.00"));

      verify(lookupService).getCommonValues(COMMON_VALUE_AWARD_ASSESSMENT_STATUS);
    }

    @Test
    void getNewCostAwardWithNonCostSessionAwardTypeReturnsToOverview() {
      final AwardTypeForm awardTypeForm = new AwardTypeForm();
      awardTypeForm.setAwardTypeCode("DAMAGE_AGR");
      awardTypeForm.setAwardType("DAMAGE");
      awardTypeForm.setDescription("Financial Settlement");

      assertThat(
              mockMvc.perform(
                  get("/case/outcome-and-awards/cost-award")
                      .sessionAttr(AWARD_TYPE_FORM, awardTypeForm)
                      .sessionAttr(CASE, ebsCase)
                      .sessionAttr(USER_DETAILS, user)))
          .hasRedirectedUrl("/case/outcome-and-awards");
    }

    @Test
    void getNewCostAwardWithoutSelectedAwardMetadataReturnsToOverview() {
      assertThat(
              mockMvc.perform(
                  get("/case/outcome-and-awards/cost-award")
                      .sessionAttr(CASE, ebsCase)
                      .sessionAttr(USER_DETAILS, user)))
          .hasRedirectedUrl("/case/outcome-and-awards");
    }

    @Test
    void getExistingCostAwardLoadsPersistedValues() {
      final CostAwardDetail award =
          new CostAwardDetail()
              .id(7)
              .awardCode("COST")
              .awardType("COST")
              .description("Cost")
              .dateOfOrder(new Date())
              .awardAmount(new java.math.BigDecimal("123.45"))
              .awardedBy("COURT");
      when(caseOutcomeService.getCostAward("300000001", 123, 7)).thenReturn(Optional.of(award));
      final CostAwardFormData formData = new CostAwardFormData();
      formData.setId(7);
      formData.setLaaFundedLegalCosts("123.45");
      when(costAwardMapper.toCostAwardFormData(award)).thenReturn(formData);

      assertThat(
              mockMvc.perform(
                  get("/case/outcome-and-awards/cost-award/7")
                      .sessionAttr(CASE, ebsCase)
                      .sessionAttr(USER_DETAILS, user)))
          .hasViewName("application/cost-award")
          .model()
          .hasEntrySatisfying(
              "costAward",
              value ->
                  assertThat(value)
                      .extracting("id", "laaFundedLegalCosts", "otherPreCertificateCosts")
                      .containsExactly(7, "123.45", "0.00"));
    }
  }

  @Nested
  @DisplayName("POST: /case/outcome-and-awards/cost-award")
  class CostAwardPostTests {

    @Test
    void postWithoutIdCreatesCostAward() {
      when(caseOutcomeService.createCostAward(any(), any(), any(), any(), any())).thenReturn(19);
      assertThat(mockMvc.perform(validCostAwardPost()))
          .hasRedirectedUrl("/case/outcome-and-awards/cost-award/19/liable-parties");

      verify(caseOutcomeService)
          .createCostAward(
              eq("300000001"),
              eq(user.getProvider().getId().intValue()),
              any(CostAwardDetail.class),
              eq(user.getLoginId()),
              eq(caseOutcome));
    }

    @Test
    void postWithoutPermissionRedirectsForReadOnlyCase() {
      ebsCase.setAvailableFunctions(List.of(FunctionConstants.VIEW_CASE_OUTCOME));

      assertThat(mockMvc.perform(validCostAwardPost()))
          .hasRedirectedUrl("/case/outcome-and-awards");

      verify(caseOutcomeService, never()).createCostAward(any(), any(), any(), any(), any());
    }

    @Test
    void postWithIdUpdatesCostAward() {
      assertThat(mockMvc.perform(validCostAwardPost().param("id", "7")))
          .hasRedirectedUrl("/case/outcome-and-awards/cost-award/7/liable-parties");

      verify(caseOutcomeService)
          .updateCostAward(
              eq("300000001"),
              eq(user.getProvider().getId().intValue()),
              eq(7),
              any(CostAwardFormData.class),
              eq(user.getLoginId()));
    }

    @Test
    void postWithCalculateRedisplaysCostAwardPageWithoutSaving() {
      assertThat(mockMvc.perform(validCostAwardPost().param("action", "calculate")))
          .hasViewName("application/cost-award")
          .model()
          .containsEntry("totalPreCertificateCosts", "3.00")
          .containsEntry("totalCertificateCostsAwarded", "7.00")
          .containsEntry("totalCostsAwarded", "10.00");

      verify(caseOutcomeService, never()).createCostAward(any(), any(), any(), any(), any());
      verify(caseOutcomeService, never()).updateCostAward(any(), any(), any(), any(), any());
    }

    @Test
    void postWithCalculateOnlyValidatesCostAmounts() {
      assertThat(
              mockMvc.perform(
                  post("/case/outcome-and-awards/cost-award")
                      .sessionAttr(CASE, ebsCase)
                      .sessionAttr(USER_DETAILS, user)
                      .param("action", "calculate")
                      .param("laaFundedLegalCosts", "1.50")
                      .param("marketRate", "2.25")))
          .hasViewName("application/cost-award")
          .model()
          .doesNotHaveErrors()
          .containsEntry("totalPreCertificateCosts", "1.50")
          .containsEntry("totalCertificateCostsAwarded", "2.25")
          .containsEntry("totalCostsAwarded", "3.75");

      verify(costAwardValidator).validateCostAmounts(any(CostAwardFormData.class), any());
      verify(costAwardValidator, never()).validate(any(), any());
    }

    @Test
    void postWithCalculateTreatsInvalidAmountsAsZero() {
      assertThat(
              mockMvc.perform(
                  post("/case/outcome-and-awards/cost-award")
                      .sessionAttr(CASE, ebsCase)
                      .sessionAttr(USER_DETAILS, user)
                      .param("action", "calculate")
                      .param("laaFundedLegalCosts", "1.234")
                      .param("otherPreCertificateCosts", "2.00")
                      .param("laaRate", "abc")
                      .param("marketRate", "3.50")))
          .hasViewName("application/cost-award")
          .model()
          .containsEntry("totalPreCertificateCosts", "2.00")
          .containsEntry("totalCertificateCostsAwarded", "3.50")
          .containsEntry("totalCostsAwarded", "5.50");
    }

    @Test
    void invalidPostRedisplaysCostAwardPage() {
      doAnswer(
              invocation -> {
                invocation
                    .getArgument(1, Errors.class)
                    .rejectValue("dateOfOrder", "invalid.format", "Invalid date");
                return null;
              })
          .when(costAwardValidator)
          .validate(any(), any());

      assertThat(mockMvc.perform(validCostAwardPost()))
          .hasViewName("application/cost-award")
          .model()
          .hasErrors();
    }

    @Test
    void apiFailureRedisplaysCostAwardPageWithError() {
      doThrow(new CaabApiClientException("API failure"))
          .when(caseOutcomeService)
          .createCostAward(any(), any(), any(), any(), any());

      assertThat(mockMvc.perform(validCostAwardPost()))
          .hasViewName("application/cost-award")
          .model()
          .hasErrors();
    }

    @Test
    void postWithoutIdRejectsAwardTypeDetailsThatDoNotMatchTheSession() {
      final AwardTypeForm selectedAwardType = new AwardTypeForm();
      selectedAwardType.setAwardTypeCode("COST_AGR");
      selectedAwardType.setAwardType("COST");
      selectedAwardType.setDescription("Different");

      assertThat(
              mockMvc.perform(validCostAwardPost().sessionAttr(AWARD_TYPE_FORM, selectedAwardType)))
          .hasViewName("application/cost-award")
          .model()
          .hasErrors();
    }

    @Test
    void postWithoutIdRejectsNonCostSessionAwardType() {
      final AwardTypeForm selectedAwardType = new AwardTypeForm();
      selectedAwardType.setAwardTypeCode("DAMAGE_AGR");
      selectedAwardType.setAwardType("DAMAGE");
      selectedAwardType.setDescription("Financial Settlement");

      assertThat(
              mockMvc.perform(validCostAwardPost().sessionAttr(AWARD_TYPE_FORM, selectedAwardType)))
          .hasViewName("application/cost-award")
          .model()
          .hasErrors();
    }
  }

  @Nested
  @DisplayName("GET: /case/outcome-and-awards/financial-award")
  class FinancialAwardGetTests {

    @Test
    void getNewFinancialAwardPreservesSelectedAwardMetadata() {
      final AwardTypeForm awardTypeForm = new AwardTypeForm();
      awardTypeForm.setAwardTypeCode("DAMAGE_AGR");
      awardTypeForm.setAwardType("DAMAGE");
      awardTypeForm.setDescription("Financial Settlement");

      assertThat(
              mockMvc.perform(
                  get("/case/outcome-and-awards/financial-award")
                      .sessionAttr(AWARD_TYPE_FORM, awardTypeForm)
                      .sessionAttr(CASE, ebsCase)
                      .sessionAttr(USER_DETAILS, user)))
          .hasViewName("application/financial-award")
          .model()
          .hasEntrySatisfying(
              "financialAward",
              value ->
                  assertThat(value)
                      .extracting("awardCode", "awardType", "description")
                      .containsExactly("DAMAGE_AGR", "DAMAGE", "Financial Settlement"));
    }

    @Test
    void getNewFinancialAwardWithoutSelectedAwardMetadataReturnsToOverview() {
      assertThat(
              mockMvc.perform(
                  get("/case/outcome-and-awards/financial-award")
                      .sessionAttr(CASE, ebsCase)
                      .sessionAttr(USER_DETAILS, user)))
          .hasRedirectedUrl("/case/outcome-and-awards");
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
              .awardAmount(new java.math.BigDecimal("123.45"))
              .interimAward("0")
              .awardedBy("COURT");
      when(caseOutcomeService.getFinancialAward("300000001", 123, 7))
          .thenReturn(Optional.of(award));
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
  }

  @Nested
  @DisplayName("POST: /case/outcome-and-awards/financial-award")
  class FinancialAwardPostTests {

    @Test
    void postWithoutIdCreatesFinancialAward() {
      when(caseOutcomeService.createFinancialAward(any(), any(), any(), any(), any()))
          .thenReturn(7);
      assertThat(mockMvc.perform(validPost()))
          .hasRedirectedUrl("/case/outcome-and-awards/financial-award/7/liable-parties");

      verify(caseOutcomeService)
          .createFinancialAward(
              eq("300000001"),
              eq(user.getProvider().getId().intValue()),
              any(FinancialAwardRequest.class),
              eq(user.getLoginId()),
              eq(caseOutcome));
    }

    @Nested
    @DisplayName("GET: /case/outcome-and-awards/land-property")
    class LandAwardGetTests {

      @Test
      void getNewLandAwardPreservesSelectedAwardMetadata() {
        final AwardTypeForm awardTypeForm = landAwardType();

        assertThat(
                mockMvc.perform(
                    get("/case/outcome-and-awards/land-property")
                        .sessionAttr(AWARD_TYPE_FORM, awardTypeForm)
                        .sessionAttr(CASE, ebsCase)
                        .sessionAttr(USER_DETAILS, user)))
            .hasViewName("application/land-property")
            .model()
            .hasEntrySatisfying(
                "landAward",
                value ->
                    assertThat(value)
                        .extracting("awardCode", "awardType", "description")
                        .containsExactly("LAND", "LAND", null));
      }

      @Test
      void getNewLandAwardWithoutSelectedAwardMetadataReturnsToOverview() {
        assertThat(
                mockMvc.perform(
                    get("/case/outcome-and-awards/land-property")
                        .sessionAttr(CASE, ebsCase)
                        .sessionAttr(USER_DETAILS, user)))
            .hasRedirectedUrl("/case/outcome-and-awards");
      }

      @Test
      void getExistingLandAwardLoadsPersistedValues() {
        final LandAwardDetail award = new LandAwardDetail().id(7);
        final LandAwardFormData form = new LandAwardFormData();
        form.setId(7);
        form.setEquity("150000.00");
        when(caseOutcomeService.getLandAward("300000001", 123, 7)).thenReturn(Optional.of(award));
        when(landAwardMapper.toLandAwardFormData(award)).thenReturn(form);

        assertThat(
                mockMvc.perform(
                    get("/case/outcome-and-awards/land-property/7")
                        .sessionAttr(CASE, ebsCase)
                        .sessionAttr(USER_DETAILS, user)))
            .hasViewName("application/land-property")
            .model()
            .hasEntrySatisfying(
                "landAward",
                value ->
                    assertThat(value).extracting("id", "equity").containsExactly(7, "150000.00"));
      }
    }

    @Nested
    @DisplayName("POST: /case/outcome-and-awards/land-property")
    class LandAwardPostTests {

      @Test
      void postWithoutIdCalculatesEquityAndCreatesLandAwardThenOpensTimeRecovery() {
        when(caseOutcomeService.createLandAward(any(), any(), any(), any())).thenReturn(7);
        assertThat(mockMvc.perform(validLandPost()))
            .hasRedirectedUrl("/case/outcome-and-awards/land/7/time-related-recovery");

        verify(landAwardMapper)
            .toLandAwardRequest(
                org.mockito.ArgumentMatchers.argThat(form -> "150000.00".equals(form.getEquity())));
        verify(caseOutcomeService)
            .createLandAward(
                eq("300000001"),
                eq(user.getProvider().getId().intValue()),
                any(LandAwardRequest.class),
                eq(user.getLoginId()));
      }

      @Test
      void calculateRedisplaysCalculatedEquityWithoutSaving() {
        assertThat(mockMvc.perform(validLandPost().param("action", "calculate")))
            .hasViewName("application/land-property")
            .model()
            .hasEntrySatisfying(
                "landAward",
                value -> assertThat(value).extracting("equity").isEqualTo("150000.00"));

        verify(caseOutcomeService, org.mockito.Mockito.never())
            .createLandAward(any(), any(), any(), any());
      }

      @Test
      void postWithIdUpdatesLandAward() {
        assertThat(mockMvc.perform(validLandPost().param("id", "7")))
            .hasRedirectedUrl("/case/outcome-and-awards/land/7/time-related-recovery");

        verify(caseOutcomeService)
            .updateLandAward(
                eq("300000001"),
                eq(user.getProvider().getId().intValue()),
                eq(7),
                any(LandAwardRequest.class),
                eq(user.getLoginId()));
      }

      @Test
      void postWithoutTimeRecoveryOpensInterestedParties() {
        when(caseOutcomeService.createLandAward(any(), any(), any(), any())).thenReturn(7);
        assertThat(mockMvc.perform(validLandPost().param("recoveryOfAwardTimeRelated", "N")))
            .hasRedirectedUrl("/case/outcome-and-awards/land/7/liable-parties");
      }

      @Test
      void switchingExistingLandAwardToNoSkipsRecoveryPage() {
        assertThat(
                mockMvc.perform(
                    validLandPost().param("id", "7").param("recoveryOfAwardTimeRelated", "N")))
            .hasRedirectedUrl("/case/outcome-and-awards/land/7/liable-parties");
        verify(caseOutcomeService)
            .updateLandAward(
                eq("300000001"),
                eq(user.getProvider().getId().intValue()),
                eq(7),
                any(LandAwardRequest.class),
                eq(user.getLoginId()));
      }

      @Test
      void invalidPostRedisplaysLandAwardPage() {
        doAnswer(
                invocation -> {
                  invocation
                      .getArgument(1, Errors.class)
                      .rejectValue("valuationAmount", "invalid.currency", "Invalid amount");
                  return null;
                })
            .when(landAwardValidator)
            .validate(any(), any());

        assertThat(mockMvc.perform(validLandPost()))
            .hasViewName("application/land-property")
            .model()
            .hasErrors();
      }

      @Test
      void postWithoutIdRejectsAwardTypeDetailsThatDoNotMatchTheSession() {
        final AwardTypeForm selectedAwardType = landAwardType();
        selectedAwardType.setAwardTypeCode("OTHER");

        assertThat(mockMvc.perform(validLandPost().sessionAttr(AWARD_TYPE_FORM, selectedAwardType)))
            .hasViewName("application/land-property")
            .model()
            .hasErrors();
      }
    }

    @Test
    void postWithIdUpdatesFinancialAward() {
      assertThat(mockMvc.perform(validPost().param("id", "7")))
          .hasRedirectedUrl("/case/outcome-and-awards/financial-award/7/liable-parties");

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
    void missingCaseOutcomeRedisplaysFinancialAwardPageWithError() {
      doThrow(new IllegalStateException("No case outcome"))
          .when(caseOutcomeService)
          .createFinancialAward(any(), any(), any(), any(), any());

      assertThat(mockMvc.perform(validPost()))
          .hasViewName("application/financial-award")
          .model()
          .hasErrors();
    }

    @Test
    void apiFailureRedisplaysFinancialAwardPageWithError() {
      doThrow(new CaabApiClientException("API failure"))
          .when(caseOutcomeService)
          .createFinancialAward(any(), any(), any(), any(), any());

      assertThat(mockMvc.perform(validPost()))
          .hasViewName("application/financial-award")
          .model()
          .hasErrors();
    }

    @Test
    void postWithoutIdRejectsAwardTypeDetailsThatDoNotMatchTheSession() {
      final AwardTypeForm selectedAwardType = new AwardTypeForm();
      selectedAwardType.setAwardTypeCode("DAMAGE");
      selectedAwardType.setAwardType("DAMAGE");
      selectedAwardType.setDescription("Financial or Punitive Damages");

      assertThat(mockMvc.perform(validPost().sessionAttr(AWARD_TYPE_FORM, selectedAwardType)))
          .hasViewName("application/financial-award")
          .model()
          .hasErrors();
    }
  }

  @Nested
  @DisplayName("GET: /case/outcome-and-awards/asset")
  class OtherAssetAwardGetTests {

    @Test
    void getNewOtherAssetAwardPreservesSelectedAwardMetadata() {
      final AwardTypeForm awardTypeForm = otherAssetAwardType();

      assertThat(
              mockMvc.perform(
                  get("/case/outcome-and-awards/asset")
                      .sessionAttr(AWARD_TYPE_FORM, awardTypeForm)
                      .sessionAttr(CASE, ebsCase)
                      .sessionAttr(USER_DETAILS, user)))
          .hasViewName("application/other-asset-award")
          .model()
          .containsKey("valuationBasisOptions")
          .hasEntrySatisfying(
              "otherAssetAward",
              value ->
                  assertThat(value)
                      .extracting("awardCode", "awardType", "description")
                      .containsExactly("OTH_ASSET", "ASSET", null));

      verify(lookupService).getCommonValues(COMMON_VALUE_VALUATION_BASIS);
    }

    @Test
    void getNewOtherAssetAwardWithoutSelectedAwardMetadataReturnsToOverview() {
      assertThat(
              mockMvc.perform(
                  get("/case/outcome-and-awards/asset")
                      .sessionAttr(CASE, ebsCase)
                      .sessionAttr(USER_DETAILS, user)))
          .hasRedirectedUrl("/case/outcome-and-awards");
    }

    @Test
    void getExistingOtherAssetAwardLoadsPersistedValues() {
      final OtherAssetAwardDetail award =
          new OtherAssetAwardDetail()
              .id(8)
              .awardCode("OTH_ASSET")
              .awardType("ASSET")
              .description("Asset")
              .valuationAmount(new java.math.BigDecimal("123.45"));
      when(caseOutcomeService.getOtherAssetAward("300000001", 123, 8))
          .thenReturn(Optional.of(award));
      final OtherAssetAwardFormData formData = new OtherAssetAwardFormData();
      formData.setId(8);
      formData.setValuationAmount("123.45");
      when(otherAssetAwardMapper.toOtherAssetAwardFormData(award)).thenReturn(formData);

      assertThat(
              mockMvc.perform(
                  get("/case/outcome-and-awards/asset/8")
                      .sessionAttr(CASE, ebsCase)
                      .sessionAttr(USER_DETAILS, user)))
          .hasViewName("application/other-asset-award")
          .model()
          .containsKey("valuationBasisOptions")
          .hasEntrySatisfying(
              "otherAssetAward",
              value ->
                  assertThat(value)
                      .extracting("id", "valuationAmount")
                      .containsExactly(8, "123.45"));
    }
  }

  @Nested
  @DisplayName("POST: /case/outcome-and-awards/asset")
  class OtherAssetAwardPostTests {

    @Test
    void newTimeRelatedAssetOpensRecoveryUsingCreatedId() {
      when(caseOutcomeService.createOtherAssetAward(any(), any(), any(), any())).thenReturn(8);

      assertThat(mockMvc.perform(validOtherAssetPost(true)))
          .hasRedirectedUrl("/case/outcome-and-awards/asset/8/time-related-recovery");
    }

    @Test
    void existingTimeRelatedAssetOpensRecoveryUsingExistingId() {
      assertThat(mockMvc.perform(validOtherAssetPost(true).param("id", "8")))
          .hasRedirectedUrl("/case/outcome-and-awards/asset/8/time-related-recovery");
      verify(caseOutcomeService)
          .updateOtherAssetAward(
              eq("300000001"),
              eq(123),
              eq(8),
              any(OtherAssetAwardRequest.class),
              eq(user.getLoginId()));
    }

    @Test
    void postWithoutIdCreatesOtherAssetAward() {
      when(caseOutcomeService.createOtherAssetAward(any(), any(), any(), any())).thenReturn(8);
      assertThat(mockMvc.perform(validOtherAssetPost()))
          .hasRedirectedUrl("/case/outcome-and-awards/asset/8/liable-parties");

      verify(caseOutcomeService)
          .createOtherAssetAward(
              eq("300000001"),
              eq(user.getProvider().getId().intValue()),
              any(OtherAssetAwardRequest.class),
              eq(user.getLoginId()));
    }

    @Test
    void postWithIdUpdatesOtherAssetAward() {
      assertThat(mockMvc.perform(validOtherAssetPost().param("id", "8")))
          .hasRedirectedUrl("/case/outcome-and-awards/asset/8/liable-parties");

      verify(caseOutcomeService)
          .updateOtherAssetAward(
              eq("300000001"),
              eq(user.getProvider().getId().intValue()),
              eq(8),
              any(OtherAssetAwardRequest.class),
              eq(user.getLoginId()));
    }

    @Test
    void invalidPostRedisplaysOtherAssetAwardPage() {
      doAnswer(
              invocation -> {
                invocation
                    .getArgument(1, Errors.class)
                    .rejectValue("valuationAmount", "invalid.currency", "Invalid amount");
                return null;
              })
          .when(otherAssetAwardValidator)
          .validate(any(), any());

      assertThat(mockMvc.perform(validOtherAssetPost()))
          .hasViewName("application/other-asset-award")
          .model()
          .hasErrors();
    }

    @Test
    void missingRequiredFieldsRedisplayPageAndDoNotCallApi() {
      doAnswer(
              invocation -> {
                new OtherAssetAwardValidator()
                    .validate(invocation.getArgument(0), invocation.getArgument(1));
                return null;
              })
          .when(otherAssetAwardValidator)
          .validate(any(), any());

      final var result =
          mockMvc.perform(
              post("/case/outcome-and-awards/asset")
                  .param("awardCode", "OTH_ASSET")
                  .param("awardType", "ASSET")
                  .param("description", "Entered description is retained")
                  .sessionAttr(CASE, ebsCase)
                  .sessionAttr(USER_DETAILS, user)
                  .sessionAttr(AWARD_TYPE_FORM, otherAssetAwardType()));

      assertThat(result)
          .hasViewName("application/other-asset-award")
          .model()
          .hasErrors()
          .containsKeys(
              "otherAssetAward", "valuationBasisOptions", "awardedByOptions", "recoveryOptions")
          .hasEntrySatisfying(
              BindingResult.MODEL_KEY_PREFIX + "otherAssetAward",
              value ->
                  assertThat(((BindingResult) value).getFieldErrors())
                      .extracting("field")
                      .contains(
                          "dateOfOrder",
                          "awardedBy",
                          "valuationAmount",
                          "valuationCriteria",
                          "valuationDate",
                          "recoveryOfAwardTimeRelated"))
          .hasEntrySatisfying(
              "otherAssetAward",
              value ->
                  assertThat(value)
                      .extracting("description")
                      .isEqualTo("Entered description is retained"));
      verifyNoInteractions(otherAssetAwardMapper, caseOutcomeService);
    }

    @Test
    void apiFailureRedisplaysOtherAssetAwardPageWithError() {
      doThrow(new CaabApiClientException("API failure"))
          .when(caseOutcomeService)
          .createOtherAssetAward(any(), any(), any(), any());

      assertThat(mockMvc.perform(validOtherAssetPost()))
          .hasViewName("application/other-asset-award")
          .model()
          .hasErrors();
    }

    @Test
    void postWithoutIdRejectsAwardTypeDetailsThatDoNotMatchTheSession() {
      final AwardTypeForm selectedAwardType = otherAssetAwardType();
      selectedAwardType.setAwardTypeCode("FIN_ASSET");

      assertThat(
              mockMvc.perform(
                  validOtherAssetPost().sessionAttr(AWARD_TYPE_FORM, selectedAwardType)))
          .hasViewName("application/other-asset-award")
          .model()
          .hasErrors();
    }
  }

  @Nested
  @DisplayName("Time-related recovery for land and asset awards")
  class TimeRecoveryTests {
    @ParameterizedTest
    @ValueSource(strings = {"land", "asset"})
    void readOnlyCaseCannotDisplayOrSaveRecovery(String awardPath) {
      ebsCase.setAvailableFunctions(List.of(FunctionConstants.VIEW_CASE_OUTCOME));

      assertThat(mockMvc.perform(timeRecoveryGet(awardPath)))
          .hasRedirectedUrl("/case/outcome-and-awards");
      assertThat(mockMvc.perform(validTimeRecoveryPost(awardPath)))
          .hasRedirectedUrl("/case/outcome-and-awards");

      verifyNoInteractions(caseOutcomeService);
    }

    @ParameterizedTest
    @CsvSource({"land, LAND", "asset, ASSET"})
    void displaysNewRecoveryWithParentSummary(String awardPath, String awardType) {
      stubTimeRecoveryAward(awardPath, true, null);

      assertThat(mockMvc.perform(timeRecoveryGet(awardPath)))
          .hasViewName("application/time-related-recovery")
          .model()
          .hasEntrySatisfying(
              "award",
              value ->
                  assertThat(value)
                      .extracting("awardType", "description", "awardAmount")
                      .containsExactly(
                          awardType, "Property or asset", new BigDecimal("150000.00")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"land", "asset"})
    void prefillsExistingRecovery(String awardPath) {
      stubTimeRecoveryAward(
          awardPath,
          true,
          new TimeRecoveryDetail()
              .triggeringEvent("Previous event")
              .effectiveDate(
                  Date.from(
                      LocalDate.of(2027, 6, 15).atStartOfDay(ZoneId.systemDefault()).toInstant()))
              .timeRelatedRecoveryDetails("Previous details"));

      assertThat(mockMvc.perform(timeRecoveryGet(awardPath)))
          .hasViewName("application/time-related-recovery")
          .model()
          .hasEntrySatisfying(
              "timeRecovery",
              value ->
                  assertThat(value)
                      .extracting("triggeringEvent", "effectiveDate", "timeRelatedRecoveryDetails")
                      .containsExactly("Previous event", "15/06/2027", "Previous details"));
    }

    @ParameterizedTest
    @CsvSource({"land, land-property", "asset, asset"})
    void redirectsWhenParentFlagIsNotSet(String awardPath, String parentPath) {
      stubTimeRecoveryAward(awardPath, false, null);

      assertThat(mockMvc.perform(timeRecoveryGet(awardPath)))
          .hasRedirectedUrl("/case/outcome-and-awards/" + parentPath + "/7");
      assertThat(mockMvc.perform(validTimeRecoveryPost(awardPath)))
          .hasRedirectedUrl("/case/outcome-and-awards/" + parentPath + "/7");
      verify(caseOutcomeService, org.mockito.Mockito.never())
          .upsertTimeRecovery(any(), any(), any(), any(), any(), any());
    }

    @ParameterizedTest
    @CsvSource({
      "land, LAND, 01/01/2030",
      "asset, ASSET, 01/01/2030",
      "land, LAND, 1/1/2030",
      "asset, ASSET, 1/1/2030",
      "land, LAND, ''",
      "asset, ASSET, ''"
    })
    void validPostUpsertsAndRedirects(String awardPath, String awardType, String effectiveDate) {
      stubTimeRecoveryAward(awardPath, true, null);

      assertThat(mockMvc.perform(timeRecoveryPost(awardPath, "Triggered", effectiveDate)))
          .hasRedirectedUrl("/case/outcome-and-awards/" + awardPath + "/7/liable-parties");
      verify(caseOutcomeService)
          .upsertTimeRecovery(
              eq("300000001"),
              eq(user.getProvider().getId().intValue()),
              eq(awardType),
              eq(7),
              eq(
                  new TimeRecoveryRequest("Triggered", "Details")
                      .effectiveDate(
                          effectiveDate.isEmpty()
                              ? null
                              : Date.from(
                                  LocalDate.of(2030, 1, 1)
                                      .atStartOfDay(ZoneId.systemDefault())
                                      .toInstant()))),
              eq(user.getLoginId()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"land", "asset"})
    void invalidPostKeepsUserInputAndShowsErrors(String awardPath) {
      stubTimeRecoveryAward(awardPath, true, null);

      assertThat(mockMvc.perform(timeRecoveryPost(awardPath, "", "01/01/1899")))
          .hasViewName("application/time-related-recovery")
          .model()
          .hasErrors()
          .hasEntrySatisfying(
              "timeRecovery",
              value ->
                  assertThat(value)
                      .extracting("effectiveDate", "timeRelatedRecoveryDetails")
                      .containsExactly("01/01/1899", "Details"));
      verify(caseOutcomeService, org.mockito.Mockito.never())
          .upsertTimeRecovery(any(), any(), any(), any(), any(), any());
    }

    @ParameterizedTest
    @CsvSource({"land, false", "asset, false", "land, true", "asset, true"})
    void saveFailureRedisplaysRecoveryWithError(String awardPath, boolean missingCaseOutcome) {
      stubTimeRecoveryAward(awardPath, true, null);
      doThrow(
              missingCaseOutcome
                  ? new IllegalStateException("Case outcome unavailable")
                  : new CaabApiClientException("API failure"))
          .when(caseOutcomeService)
          .upsertTimeRecovery(any(), any(), any(), any(), any(), any());

      assertThat(mockMvc.perform(validTimeRecoveryPost(awardPath)))
          .hasViewName("application/time-related-recovery")
          .model()
          .hasErrors()
          .containsKeys("award", "awardPath", "awardId")
          .hasEntrySatisfying(
              "timeRecovery",
              value ->
                  assertThat(value)
                      .extracting("triggeringEvent", "effectiveDate", "timeRelatedRecoveryDetails")
                      .containsExactly("Triggered", "01/01/2030", "Details"));
    }

    @Test
    void unsupportedAwardTypeHasNoRecoveryRoute() {
      assertThat(mockMvc.perform(timeRecoveryGet("cost-award"))).hasStatus(404);
      assertThat(mockMvc.perform(validTimeRecoveryPost("cost-award"))).hasStatus(404);
      verifyNoInteractions(caseOutcomeService);
    }
  }

  private void stubTimeRecoveryAward(
      String awardPath, boolean timeRelated, TimeRecoveryDetail recovery) {
    if ("land".equals(awardPath)) {
      when(caseOutcomeService.getLandAward("300000001", 123, 7))
          .thenReturn(
              Optional.of(
                  new LandAwardDetail()
                      .id(7)
                      .description("Property or asset")
                      .valuationAmount(new BigDecimal("150000.00"))
                      .recoveryOfAwardTimeRelated(timeRelated)
                      .timeRecovery(recovery)));
    } else {
      when(caseOutcomeService.getOtherAssetAward("300000001", 123, 7))
          .thenReturn(
              Optional.of(
                  new OtherAssetAwardDetail()
                      .id(7)
                      .description("Property or asset")
                      .valuationAmount(new BigDecimal("150000.00"))
                      .awardedAmount(new BigDecimal("20000.00"))
                      .recoveryOfAwardTimeRelated(timeRelated)
                      .timeRecovery(recovery)));
    }
  }

  private MockHttpServletRequestBuilder timeRecoveryGet(String awardPath) {
    return get("/case/outcome-and-awards/" + awardPath + "/7/time-related-recovery")
        .sessionAttr(CASE, ebsCase)
        .sessionAttr(USER_DETAILS, user);
  }

  private MockHttpServletRequestBuilder validTimeRecoveryPost(String awardPath) {
    return timeRecoveryPost(awardPath, "Triggered", "01/01/2030");
  }

  private MockHttpServletRequestBuilder timeRecoveryPost(
      String awardPath, String triggeringEvent, String effectiveDate) {
    return post("/case/outcome-and-awards/" + awardPath + "/7/time-related-recovery")
        .sessionAttr(CASE, ebsCase)
        .sessionAttr(USER_DETAILS, user)
        .param("triggeringEvent", triggeringEvent)
        .param("effectiveDate", effectiveDate)
        .param("timeRelatedRecoveryDetails", "Details");
  }

  private MockHttpServletRequestBuilder validPost() {
    final AwardTypeForm selectedAwardType = new AwardTypeForm();
    selectedAwardType.setAwardTypeCode("DAMAGE_AGR");
    selectedAwardType.setAwardType("DAMAGE");
    selectedAwardType.setDescription("Financial Settlement");

    return post("/case/outcome-and-awards/financial-award")
        .param("awardCode", "DAMAGE_AGR")
        .param("awardType", "DAMAGE")
        .param("description", "Financial Settlement")
        .param("dateOfOrder", "01/01/2025")
        .param("awardAmount", "123.45")
        .param("interimAward", "0")
        .param("awardedBy", "COURT")
        .sessionAttr(CASE, ebsCase)
        .sessionAttr(USER_DETAILS, user)
        .sessionAttr(AWARD_TYPE_FORM, selectedAwardType);
  }

  private MockHttpServletRequestBuilder validCostAwardPost() {
    final AwardTypeForm selectedAwardType = new AwardTypeForm();
    selectedAwardType.setAwardTypeCode("COST_AGR");
    selectedAwardType.setAwardType("COST");
    selectedAwardType.setDescription("Cost");

    return post("/case/outcome-and-awards/cost-award")
        .param("awardCode", "COST_AGR")
        .param("awardType", "COST")
        .param("description", "Cost")
        .param("dateOfOrder", "01/01/2025")
        .param("courtAssessmentStatus", "A")
        .param("laaFundedLegalCosts", "1.00")
        .param("otherPreCertificateCosts", "2.00")
        .param("laaRate", "3.00")
        .param("marketRate", "4.00")
        .param("awardedBy", "COURT")
        .param("interestRate", "8.5")
        .sessionAttr(CASE, ebsCase)
        .sessionAttr(USER_DETAILS, user)
        .sessionAttr(AWARD_TYPE_FORM, selectedAwardType);
  }

  private MockHttpServletRequestBuilder validLandPost() {
    return post("/case/outcome-and-awards/land-property")
        .param("awardCode", "LAND")
        .param("awardType", "LAND")
        .param("description", "Land")
        .param("dateOfOrder", "01/01/2025")
        .param("addressLine1", "1 High Street")
        .param("valuationAmount", "250000.00")
        .param("valuationCriteria", "MARKET")
        .param("valuationDate", "02/01/2025")
        .param("disputedPercentage", "50.00")
        .param("awardedPercentage", "25.00")
        .param("mortgageAmountDue", "100000.00")
        .param("awardedBy", "COURT")
        .param("recovery", "RECOVERED")
        .param("recoveryOfAwardTimeRelated", "Y")
        .sessionAttr(CASE, ebsCase)
        .sessionAttr(USER_DETAILS, user)
        .sessionAttr(AWARD_TYPE_FORM, landAwardType());
  }

  private AwardTypeForm landAwardType() {
    final AwardTypeForm awardType = new AwardTypeForm();
    awardType.setAwardTypeCode("LAND");
    awardType.setAwardType("LAND");
    awardType.setDescription("Land");
    return awardType;
  }

  private MockHttpServletRequestBuilder validOtherAssetPost() {
    return validOtherAssetPost(false);
  }

  private MockHttpServletRequestBuilder validOtherAssetPost(boolean timeRelated) {
    return post("/case/outcome-and-awards/asset")
        .param("awardCode", "OTH_ASSET")
        .param("awardType", "ASSET")
        .param("description", "Antique jewellery")
        .param("dateOfOrder", "01/01/2025")
        .param("awardedBy", "COURT")
        .param("valuationAmount", "123.45")
        .param("valuationCriteria", "AGREED")
        .param("valuationDate", "02/01/2025")
        .param("awardedPercentage", "75")
        .param("recovery", "UNKNOWN")
        .param("recoveryOfAwardTimeRelated", Boolean.toString(timeRelated))
        .sessionAttr(CASE, ebsCase)
        .sessionAttr(USER_DETAILS, user)
        .sessionAttr(AWARD_TYPE_FORM, otherAssetAwardType());
  }

  private AwardTypeForm otherAssetAwardType() {
    final AwardTypeForm selectedAwardType = new AwardTypeForm();
    selectedAwardType.setAwardTypeCode("OTH_ASSET");
    selectedAwardType.setAwardType("ASSET");
    selectedAwardType.setDescription("Asset");
    return selectedAwardType;
  }
}
