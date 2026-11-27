package uk.gov.laa.ccms.caab.controller.application.section;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_DOCUMENT_TYPES;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.ACTIVE_CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.EVIDENCE_REQUIRED;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;
import static uk.gov.laa.ccms.caab.util.ConversionServiceUtils.getConversionService;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.bean.ActiveCase;
import uk.gov.laa.ccms.caab.bean.evidence.EvidenceRequired;
import uk.gov.laa.ccms.caab.bean.validators.evidence.EvidenceUploadValidator;
import uk.gov.laa.ccms.caab.mapper.EvidenceMapper;
import uk.gov.laa.ccms.caab.service.AvScanResultHandler;
import uk.gov.laa.ccms.caab.service.AvScanService;
import uk.gov.laa.ccms.caab.service.EvidenceService;
import uk.gov.laa.ccms.caab.service.LookupService;
import uk.gov.laa.ccms.caab.util.ThymeleafTestUtils;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;
import uk.gov.laa.ccms.data.model.UserDetail;

@ExtendWith(MockitoExtension.class)
class EvidenceAddTemplateRenderTest {

  private static final String EVIDENCE_LEGEND = "Which evidence documents does this file contain?";
  private static final String EVIDENCE_CHECKBOX = "name=\"evidenceTypes\"";

  @Mock private EvidenceService evidenceService;
  @Mock private AvScanService avScanService;
  @Mock private AvScanResultHandler avScanResultHandler;
  @Mock private LookupService lookupService;
  @Mock private EvidenceUploadValidator evidenceUploadValidator;
  @Mock private EvidenceMapper evidenceMapper;

  @InjectMocks private EvidenceSectionController controller;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    final ThymeleafViewResolver viewResolver = ThymeleafTestUtils.createViewResolver();
    viewResolver.setStaticVariables(
        Map.of(
            "cspNonce", "test-nonce",
            "researchPanelLink", "/research",
            "user", new UserDetail().functions(List.of())));

    mockMvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setConversionService(getConversionService())
            .setViewResolvers(viewResolver)
            .build();

    when(lookupService.getCommonValues(COMMON_VALUE_DOCUMENT_TYPES))
        .thenReturn(
            Mono.just(
                new CommonLookupDetail()
                    .addContentItem(
                        new CommonLookupValueDetail().code("BANK").description("Bank statement"))));
  }

  @Test
  @DisplayName("An application upload lists the required evidence to choose from")
  void applicationWithRequiredEvidenceShowsCheckboxes() throws Exception {
    final String html = render("application", List.of(requiredEvidence()));

    assertThat(html)
        .contains(EVIDENCE_LEGEND)
        .contains(EVIDENCE_CHECKBOX)
        .contains("Proof of income");
  }

  @Test
  @DisplayName("An application upload keeps the evidence fieldset when none is listed")
  void applicationWithoutRequiredEvidenceKeepsFieldset() throws Exception {
    final String html = render("application", Collections.emptyList());

    assertThat(html).contains(EVIDENCE_LEGEND).doesNotContain(EVIDENCE_CHECKBOX);
  }

  @Test
  @DisplayName("An amendment upload lists the required evidence to choose from")
  void amendmentWithRequiredEvidenceShowsCheckboxes() throws Exception {
    final String html = render("amendments", List.of(requiredEvidence()));

    assertThat(html)
        .contains(EVIDENCE_LEGEND)
        .contains(EVIDENCE_CHECKBOX)
        .contains("Proof of income");
  }

  @Test
  @DisplayName("An amendment upload with no required evidence shows no evidence fieldset")
  void amendmentWithoutRequiredEvidenceOmitsFieldset() throws Exception {
    final String html = render("amendments", Collections.emptyList());

    assertThat(html)
        .doesNotContain(EVIDENCE_LEGEND)
        .doesNotContain(EVIDENCE_CHECKBOX)
        .doesNotContain("Select all that apply")
        .contains("type=\"file\"")
        .contains("type=\"submit\"");
  }

  private static EvidenceRequired requiredEvidence() {
    return new EvidenceRequired("CODE", "Proof of income");
  }

  private String render(String caseContext, List<EvidenceRequired> evidenceRequired)
      throws Exception {
    final ActiveCase activeCase =
        ActiveCase.builder().applicationId(1).caseReferenceNumber("CASE-1").providerId(2).build();

    return mockMvc
        .perform(
            get("/%s/evidence/add".formatted(caseContext))
                .sessionAttr(ACTIVE_CASE, activeCase)
                .sessionAttr(EVIDENCE_REQUIRED, evidenceRequired)
                .sessionAttr(
                    USER_DETAILS,
                    new UserDetail()
                        .userId(1)
                        .userType("type")
                        .loginId("login")
                        .functions(List.of())))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();
  }
}
