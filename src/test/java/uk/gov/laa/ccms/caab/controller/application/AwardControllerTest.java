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
import static uk.gov.laa.ccms.caab.constants.SessionConstants.AWARD_TYPE_FORM;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;

import java.util.Collections;
import java.util.Date;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.bean.AwardTypeForm;
import uk.gov.laa.ccms.caab.bean.award.FinancialAwardFormData;
import uk.gov.laa.ccms.caab.bean.validators.application.AwardTypeValidator;
import uk.gov.laa.ccms.caab.bean.validators.awards.FinancialAwardValidator;
import uk.gov.laa.ccms.caab.client.CaabApiClientException;
import uk.gov.laa.ccms.caab.mapper.FinancialAwardMapper;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.FinancialAwardDetail;
import uk.gov.laa.ccms.caab.model.FinancialAwardRequest;
import uk.gov.laa.ccms.caab.service.CaseOutcomeService;
import uk.gov.laa.ccms.caab.service.LookupService;
import uk.gov.laa.ccms.data.model.AwardTypeLookupDetail;
import uk.gov.laa.ccms.data.model.AwardTypeLookupValueDetail;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.UserDetail;

@ExtendWith(MockitoExtension.class)
class AwardControllerTest {

  @Mock private LookupService lookupService;
  @Mock private CaseOutcomeService caseOutcomeService;
  @Mock private AwardTypeValidator awardTypeValidator;
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

      assertThat(mockMvc.perform(get("/case/outcome-and-awards/award-type")))
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

      assertThat(mockMvc.perform(get("/case/outcome-and-awards/award-type")))
          .hasStatusOk()
          .hasViewName("application/select-award-type")
          .model()
          .containsEntry("awardTypes", Collections.emptyList())
          .hasEntrySatisfying(
              AWARD_TYPE_FORM, value -> assertThat(value).isInstanceOf(AwardTypeForm.class));

      verify(lookupService).getAwardTypes();
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
                      .sessionAttr(AWARD_TYPE_FORM, awardTypeForm)
                      .param("awardTypeCode", code)))
          .hasStatus3xxRedirection()
          .hasRedirectedUrl(expectedRedirect);

      assertThat(awardTypeForm.getAwardTypeCode()).isEqualTo(code);
      assertThat(awardTypeForm.getDescription()).isEqualTo(description);
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
      assertThat(awardTypeForm.getDescription()).isEqualTo("New award");
      assertThat(awardTypeForm.getAwardType()).isEqualTo("UNKNOWN");

      verify(awardTypeValidator).validate(any(AwardTypeForm.class), any());
      verify(lookupService).getAwardTypes();
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
}
