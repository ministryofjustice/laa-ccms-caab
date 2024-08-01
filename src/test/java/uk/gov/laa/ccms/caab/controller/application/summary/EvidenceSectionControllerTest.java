package uk.gov.laa.ccms.caab.controller.application.summary;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static uk.gov.laa.ccms.caab.constants.CcmsModule.APPLICATION;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_DOCUMENT_TYPES;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.ACTIVE_CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.EVIDENCE_REQUIRED;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.EVIDENCE_UPLOAD_FORM_DATA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.Errors;
import org.springframework.web.context.WebApplicationContext;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.advice.GlobalExceptionHandler;
import uk.gov.laa.ccms.caab.bean.ActiveCase;
import uk.gov.laa.ccms.caab.bean.evidence.EvidenceRequired;
import uk.gov.laa.ccms.caab.bean.evidence.EvidenceUploadFormData;
import uk.gov.laa.ccms.caab.bean.validators.evidence.EvidenceUploadValidator;
import uk.gov.laa.ccms.caab.constants.CcmsModule;
import uk.gov.laa.ccms.caab.exception.AvScanException;
import uk.gov.laa.ccms.caab.mapper.EvidenceMapper;
import uk.gov.laa.ccms.caab.model.EvidenceDocumentDetail;
import uk.gov.laa.ccms.caab.model.EvidenceDocumentDetails;
import uk.gov.laa.ccms.caab.service.AvScanService;
import uk.gov.laa.ccms.caab.service.EvidenceService;
import uk.gov.laa.ccms.caab.service.LookupService;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;
import uk.gov.laa.ccms.data.model.UserDetail;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration
@WebAppConfiguration
class EvidenceSectionControllerTest {

    @Mock
    private EvidenceService evidenceService;

    @Mock
    private AvScanService avScanService;

    @Mock
    private LookupService lookupService;

    @Mock
    private EvidenceUploadValidator evidenceUploadValidator;

    @Mock
    private EvidenceMapper evidenceMapper;

    @InjectMocks
    private EvidenceSectionController controller;

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private final UserDetail user = new UserDetail()
        .userId(1)
        .userType("testUserType")
        .loginId("testLoginId");

    private final String applicationId = "123";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
            .standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @Test
    void viewEvidenceRequired() throws Exception {
        ActiveCase activeCase = buildActiveCase();

        when(evidenceService.getDocumentsRequired(
            String.valueOf(activeCase.getApplicationId()),
            activeCase.getCaseReferenceNumber(),
            activeCase.getProviderId()))
            .thenReturn(Mono.just(Collections.emptyList()));

        when(evidenceService.getEvidenceDocumentsForCase(
            activeCase.getCaseReferenceNumber(),
            APPLICATION))
            .thenReturn(Mono.just(new EvidenceDocumentDetails()));

        when(evidenceMapper.toEvidenceRequiredList(
            any(List.class),
            any(List.class))).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/application/sections/evidence")
                .sessionAttr(ACTIVE_CASE, activeCase))
                .andExpect(status().isOk())
                .andExpect(view().name("application/summary/evidence-section"));

        verify(evidenceService).getDocumentsRequired(
            String.valueOf(activeCase.getApplicationId()),
            activeCase.getCaseReferenceNumber(),
            activeCase.getProviderId());

        verify(evidenceService).getEvidenceDocumentsForCase(
            activeCase.getCaseReferenceNumber(),
            APPLICATION);

        verify(evidenceMapper).toEvidenceRequiredList(
            any(List.class),
            any(List.class));
    }

    @Test
    void viewAddEvidenceScreen() throws Exception {
        ActiveCase activeCase = buildActiveCase();

        List<EvidenceRequired> evidenceRequired = List.of(
            new EvidenceRequired("code", "desc"));

        CommonLookupDetail documentTypesLookup = new CommonLookupDetail()
            .addContentItem(new CommonLookupValueDetail());
        when(lookupService.getCommonValues(COMMON_VALUE_DOCUMENT_TYPES))
            .thenReturn(Mono.just(documentTypesLookup));

        mockMvc.perform(get("/application/evidence/add")
                .sessionAttr(ACTIVE_CASE, activeCase)
                .sessionAttr(EVIDENCE_REQUIRED, evidenceRequired)
                .sessionAttr(USER_DETAILS, user))
            .andExpect(status().isOk())
            .andExpect(model().attributeExists(EVIDENCE_UPLOAD_FORM_DATA))
            .andExpect(model().attribute(EVIDENCE_REQUIRED, evidenceRequired))
            .andExpect(model().attribute("evidenceTypes", documentTypesLookup.getContent()))
            .andExpect(view().name("application/evidence/evidence-add"));
    }

    @Test
    void postAddEvidenceScreen_validationFailure_returnsToUploadScreen() throws Exception {
        EvidenceUploadFormData formData = buildEvidenceUploadFormData();

        doAnswer(invocation -> {
            Errors errors = (Errors) invocation.getArguments()[1];
            errors.rejectValue("file", "required.file",
                "Please choose a file.");
            return null;
        }).when(evidenceUploadValidator).validate(any(), any());

        List<EvidenceRequired> evidenceRequired = List.of(
            new EvidenceRequired("code", "desc"));

        CommonLookupDetail documentTypesLookup = new CommonLookupDetail()
            .addContentItem(new CommonLookupValueDetail());
        when(lookupService.getCommonValues(COMMON_VALUE_DOCUMENT_TYPES))
            .thenReturn(Mono.just(documentTypesLookup));

        mockMvc.perform(post("/application/evidence/add")
                .flashAttr(EVIDENCE_UPLOAD_FORM_DATA, formData)
                .sessionAttr(EVIDENCE_REQUIRED, evidenceRequired)
                .sessionAttr(USER_DETAILS, user))
            .andExpect(status().isOk())
            .andExpect(model().attributeExists(EVIDENCE_UPLOAD_FORM_DATA))
            .andExpect(model().attribute(EVIDENCE_REQUIRED, evidenceRequired))
            .andExpect(model().attribute("evidenceTypes", documentTypesLookup.getContent()))
            .andExpect(view().name("application/evidence/evidence-add"));
    }

    @Test
    void postAddEvidenceScreen_avScanFailure_returnsToUploadScreen() throws Exception {
        EvidenceUploadFormData formData = buildEvidenceUploadFormData();

        doThrow(new AvScanException("Virus alert")).when(avScanService).performAvScan(
            eq(formData.getCaseReferenceNumber()),
            eq(formData.getProviderId()),
            eq(formData.getDocumentSender()),
            eq(APPLICATION),
            eq(formData.getFile().getOriginalFilename()),
            any(InputStream.class));

        List<EvidenceRequired> evidenceRequired = List.of(
            new EvidenceRequired("code", "desc"));

        CommonLookupDetail documentTypesLookup = new CommonLookupDetail()
            .addContentItem(new CommonLookupValueDetail());
        when(lookupService.getCommonValues(COMMON_VALUE_DOCUMENT_TYPES))
            .thenReturn(Mono.just(documentTypesLookup));

        mockMvc.perform(post("/application/evidence/add")
                .flashAttr(EVIDENCE_UPLOAD_FORM_DATA, formData)
                .sessionAttr(EVIDENCE_REQUIRED, evidenceRequired)
                .sessionAttr(USER_DETAILS, user))
            .andExpect(status().isOk())
            .andExpect(model().attributeExists(EVIDENCE_UPLOAD_FORM_DATA))
            .andExpect(model().attribute(EVIDENCE_REQUIRED, evidenceRequired))
            .andExpect(model().attribute("evidenceTypes", documentTypesLookup.getContent()))
            .andExpect(view().name("application/evidence/evidence-add"));
    }

    @Test
    void postAddEvidenceScreen_registerDocumentFail_throwsException() throws Exception {
        EvidenceUploadFormData formData = buildEvidenceUploadFormData();
        final String filename = formData.getFile().getOriginalFilename();

        List<EvidenceRequired> evidenceRequired = List.of(
            new EvidenceRequired("code", "desc"));

        when(evidenceService.registerDocument(
            formData.getDocumentType(),
            filename.substring(filename.lastIndexOf(".") + 1),
            formData.getDocumentDescription(),
            user.getLoginId(),
            user.getUserType())).thenReturn(Mono.empty());

        mockMvc.perform(post("/application/evidence/add")
                .flashAttr(EVIDENCE_UPLOAD_FORM_DATA, formData)
                .sessionAttr(EVIDENCE_REQUIRED, evidenceRequired)
                .sessionAttr(USER_DETAILS, user))
            .andExpect(status().isOk())
            .andExpect(view().name("error"));

        verify(evidenceService, never()).addDocument(any(EvidenceDocumentDetail.class), any(String.class));
    }

    @Test
    void postAddEvidenceScreen_registerDocumentSuccess_addsDocumentToTds() throws Exception {
        final String registeredDocumentId = "238476";
        final String tdsId = "123";
        final EvidenceDocumentDetail evidenceDocumentDetail = new EvidenceDocumentDetail();

        EvidenceUploadFormData formData = buildEvidenceUploadFormData();
        final String filename = formData.getFile().getOriginalFilename();

        List<EvidenceRequired> evidenceRequired = List.of(
            new EvidenceRequired("code", "desc"));

        when(evidenceService.registerDocument(
            formData.getDocumentType(),
            filename.substring(filename.lastIndexOf(".") + 1),
            formData.getDocumentDescription(),
            user.getLoginId(),
            user.getUserType())).thenReturn(Mono.just(registeredDocumentId));

        when(evidenceMapper.toEvidenceDocumentDetail(
            any(EvidenceUploadFormData.class))).thenReturn(evidenceDocumentDetail);

        when(evidenceService.addDocument(evidenceDocumentDetail, user.getLoginId()))
            .thenReturn(Mono.just(tdsId));

        mockMvc.perform(post("/application/evidence/add")
                .flashAttr(EVIDENCE_UPLOAD_FORM_DATA, formData)
                .sessionAttr(EVIDENCE_REQUIRED, evidenceRequired)
                .sessionAttr(USER_DETAILS, user))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/application/sections/evidence"));

        // Update the formData now, for comparison purposes
        formData.setRegisteredDocumentId(registeredDocumentId);

        verify(evidenceMapper).toEvidenceDocumentDetail(formData);
        verify(evidenceService).addDocument(evidenceDocumentDetail, user.getLoginId());
    }

    @Test
    void removeEvidence_removesDocument() throws Exception {
        final Integer tdsId = 123;
        final ActiveCase activeCase = buildActiveCase();

        mockMvc.perform(get("/application/evidence/{evidence-document-id}/remove", tdsId)
                .sessionAttr(ACTIVE_CASE, activeCase)
                .sessionAttr(USER_DETAILS, user))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/application/sections/evidence"));

        verify(evidenceService).removeDocument(
            activeCase.getApplicationId().toString(),
            tdsId,
            APPLICATION,
            user.getLoginId());
    }

    private ActiveCase buildActiveCase() {
      return ActiveCase.builder()
            .applicationId(123)
            .caseReferenceNumber("caseRef")
            .providerId(789).build();
    }

    private EvidenceUploadFormData buildEvidenceUploadFormData() {
        EvidenceUploadFormData formData = new EvidenceUploadFormData();
        formData.setApplicationOrOutcomeId(123);
        formData.setCaseReferenceNumber("caseRef");
        formData.setCcmsModule(CcmsModule.APPLICATION);
        formData.setDocumentDescription("doc desc");
        formData.setDocumentSender("doc sender");
        formData.setDocumentType("docType");
        formData.setDocumentTypeDisplayValue("doc type");
        formData.setEvidenceTypes(List.of("type 1", "type 2"));
        formData.setFile(new MockMultipartFile(
            "theFile",
            "originalName.pdf",
            "contentType",
            "the file data".getBytes()));
        formData.setProviderId(789);
        formData.setRegisteredDocumentId("regId");
        return formData;
    }

}