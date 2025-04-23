package uk.gov.laa.ccms.caab.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.laa.ccms.caab.bean.common.DynamicOptionFormData;
import uk.gov.laa.ccms.caab.bean.evidence.EvidenceUploadFormData;
import uk.gov.laa.ccms.caab.bean.request.ProviderRequestDetailsFormData;
import uk.gov.laa.ccms.caab.bean.request.ProviderRequestFlowFormData;
import uk.gov.laa.ccms.caab.constants.CcmsModule;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.model.EvidenceDocumentDetail;
import uk.gov.laa.ccms.data.model.ProviderRequestDataLookupValueDetail;
import uk.gov.laa.ccms.data.model.ProviderRequestTypeLookupValueDetail;
import uk.gov.laa.ccms.soa.gateway.model.ProviderRequestAttribute;


@ExtendWith(SpringExtension.class)
class ProviderRequestsMapperTest {

  @Mock(answer = Answers.CALLS_REAL_METHODS)
  CommonMapper commonMapper;

  @InjectMocks
  ProviderRequestsMapper mapper;

  @Test
  @DisplayName("Should populate ProviderRequestDetailsFormData with dynamic options")
  void shouldPopulateProviderRequestDetailsForm() {
    final ProviderRequestDetailsFormData providerRequestDetailsFormData = new ProviderRequestDetailsFormData();
    providerRequestDetailsFormData.setDynamicOptions(new HashMap<>());

    final ProviderRequestTypeLookupValueDetail providerRequestType = new ProviderRequestTypeLookupValueDetail();
    final ProviderRequestDataLookupValueDetail formOption1 = new ProviderRequestDataLookupValueDetail();
    formOption1.setCode("option1");
    formOption1.setMandatoryFlag(true);
    formOption1.setLabel("Option 1");
    formOption1.setType("text");

    final ProviderRequestDataLookupValueDetail formOption2 = new ProviderRequestDataLookupValueDetail();
    formOption2.setCode("option2");
    formOption2.setMandatoryFlag(false);
    formOption2.setLabel("Option 2");
    formOption2.setType("checkbox");

    providerRequestType.setDataItems(List.of(formOption1, formOption2));

    mapper.populateProviderRequestDetailsForm(providerRequestDetailsFormData, providerRequestType);

    final Map<String, DynamicOptionFormData> dynamicOptions = providerRequestDetailsFormData.getDynamicOptions();

    assertEquals(2, dynamicOptions.size(), "Dynamic options size should match the number of data items");
    assertEquals(formOption1.getLabel(), dynamicOptions.get("option1").getFieldDescription(), "Option 1 should be mapped correctly");
    assertEquals(formOption2.getLabel(), dynamicOptions.get("option2").getFieldDescription(), "Option 2 should be mapped correctly");
    assertEquals(formOption1.getMandatoryFlag(), dynamicOptions.get("option1").isMandatory(), "Option 1 mandatory flag should match");
    assertEquals(formOption2.getMandatoryFlag(), dynamicOptions.get("option2").isMandatory(), "Option 2 mandatory flag should match");
  }

  @Test
  @DisplayName("Should map ProviderRequestDataLookupValueDetail to DynamicOptionFormData correctly")
  void shouldMapToDynamicOptionCorrectly() {
    final ProviderRequestDataLookupValueDetail formOption = new ProviderRequestDataLookupValueDetail();
    formOption.setMandatoryFlag(true);
    formOption.setLabel("Sample Label");
    formOption.setType("text");

    final DynamicOptionFormData result = mapper.toDynamicOption(formOption);

    assertEquals(formOption.getMandatoryFlag(), result.isMandatory(), "Mandatory field should match");
    assertEquals(formOption.getLabel(), result.getFieldDescription(), "Field description should match");
    assertEquals(formOption.getType(), result.getFieldType(), "Field type should match");
    assertNull(result.getFieldValue(), "Field value should be null as it is ignored");
    assertNull(result.getFieldValueDisplayValue(), "Field value display value should be null as it is ignored");
  }


  @Test
  @DisplayName("Should return null when input ProviderRequestDataLookupValueDetail is null")
  void shouldReturnNullWhenInputIsNull() {
    assertNull(mapper.toDynamicOption(null), "Result should be null when input is null");
  }

  @Test
  @DisplayName("Should map ProviderRequestFlowFormData to ProviderRequestDetailsFormData correctly")
  void shouldMapToProviderRequestDetailsFormData() {
    final ProviderRequestDetailsFormData providerRequestDetails = new ProviderRequestDetailsFormData();
    final ProviderRequestFlowFormData providerRequestFlowFormData = new ProviderRequestFlowFormData();
    final ProviderRequestDetailsFormData requestDetails = new ProviderRequestDetailsFormData();
    requestDetails.setClaimUploadLabel("Test Label");
    requestDetails.setAdditionalInformationLabel("Additional Info");
    requestDetails.setClaimUploadEnabled(true);
    requestDetails.setIsAdditionalInformationPromptRequired(true);

    providerRequestFlowFormData.setRequestDetailsFormData(requestDetails);

    mapper.toProviderRequestDetailsFormData(providerRequestDetails, providerRequestFlowFormData);

    assertEquals("Test Label", providerRequestDetails.getClaimUploadLabel(), "Claim upload label should match");
    assertEquals("Additional Info", providerRequestDetails.getAdditionalInformationLabel(), "Additional information label should match");
    assertTrue(providerRequestDetails.isClaimUploadEnabled(), "Claim upload enabled flag should match");
    assertTrue(providerRequestDetails.getIsAdditionalInformationPromptRequired(), "IsAdditionalInformationPromptRequired flag should match");

    assertNull(providerRequestDetails.getAdditionalInformation(), "Additional information should be ignored");
    assertNull(providerRequestDetails.getFile(), "File should be ignored");
    assertNull(providerRequestDetails.getFileExtension(), "File extension should be ignored");
    assertNull(providerRequestDetails.getDocumentType(), "Document type should be ignored");
    assertNull(providerRequestDetails.getDocumentTypeDisplayValue(), "Document type display value should be ignored");
    assertNull(providerRequestDetails.getDocumentDescription(), "Document description should be ignored");
    assertNull(providerRequestDetails.getDocumentIdToDelete(), "Document ID to delete should be ignored");
  }

  @Test
  @DisplayName("Should handle null ProviderRequestFlowFormData gracefully")
  void shouldHandleNullProviderRequestFlowFormData() {
    final ProviderRequestDetailsFormData providerRequestDetails = new ProviderRequestDetailsFormData();

    mapper.toProviderRequestDetailsFormData(new ProviderRequestDetailsFormData(), null);

    assertNull(providerRequestDetails.getClaimUploadLabel(), "Claim upload label should remain null");
    assertNull(providerRequestDetails.getAdditionalInformationLabel(), "Additional information label should remain null");
    assertFalse(providerRequestDetails.isClaimUploadEnabled(), "Claim upload enabled flag should remain false by default");
  }

  @Test
  @DisplayName("Should map dynamic options correctly")
  void shouldMapDynamicOptionsCorrectly() {
    final ProviderRequestDetailsFormData providerRequestDetails = new ProviderRequestDetailsFormData();
    providerRequestDetails.setDynamicOptions(new HashMap<>());
    final DynamicOptionFormData existingOption = new DynamicOptionFormData();
    existingOption.setMandatory(false);
    providerRequestDetails.getDynamicOptions().put("key1", existingOption);

    final ProviderRequestFlowFormData providerRequestFlowFormData = new ProviderRequestFlowFormData();
    final ProviderRequestDetailsFormData requestDetails = new ProviderRequestDetailsFormData();
    final Map<String, DynamicOptionFormData> dynamicOptions = new HashMap<>();

    final DynamicOptionFormData updatedOption = new DynamicOptionFormData();
    updatedOption.setMandatory(true);
    updatedOption.setFieldDescription("Updated Description");
    updatedOption.setFieldType("Updated Type");
    dynamicOptions.put("key1", updatedOption);

    requestDetails.setDynamicOptions(dynamicOptions);
    providerRequestFlowFormData.setRequestDetailsFormData(requestDetails);

    mapper.mapDynamicOptions(providerRequestDetails, providerRequestFlowFormData);

    final DynamicOptionFormData resultOption = providerRequestDetails.getDynamicOptions().get("key1");
    assertNotNull(resultOption, "The dynamic option should exist in the target map");
    assertTrue(resultOption.isMandatory(), "Mandatory flag should be updated");
    assertEquals("Updated Description", resultOption.getFieldDescription(), "Field description should be updated");
    assertEquals("Updated Type", resultOption.getFieldType(), "Field type should be updated");
  }

  @Test
  @DisplayName("Should handle null dynamic options in source")
  void shouldHandleNullDynamicOptionsInSource() {
    final ProviderRequestDetailsFormData providerRequestDetails = new ProviderRequestDetailsFormData();
    providerRequestDetails.setDynamicOptions(new HashMap<>());

    final ProviderRequestFlowFormData providerRequestFlowFormData = new ProviderRequestFlowFormData();
    final ProviderRequestDetailsFormData requestDetails = new ProviderRequestDetailsFormData();
    requestDetails.setDynamicOptions(null);
    providerRequestFlowFormData.setRequestDetailsFormData(requestDetails);

    mapper.mapDynamicOptions(providerRequestDetails, providerRequestFlowFormData);

    assertTrue(providerRequestDetails.getDynamicOptions().isEmpty(), "Dynamic options should remain unchanged");
  }

  @Test
  @DisplayName("Should handle null dynamic options in target")
  void shouldHandleNullDynamicOptionsInTarget() {
    final ProviderRequestDetailsFormData providerRequestDetails = new ProviderRequestDetailsFormData();
    providerRequestDetails.setDynamicOptions(new HashMap<>());

    final ProviderRequestFlowFormData providerRequestFlowFormData = new ProviderRequestFlowFormData();
    final ProviderRequestDetailsFormData requestDetails = new ProviderRequestDetailsFormData();
    final Map<String, DynamicOptionFormData> dynamicOptions = new HashMap<>();

    final DynamicOptionFormData newOption = new DynamicOptionFormData();
    newOption.setMandatory(true);
    newOption.setFieldDescription("New Description");
    newOption.setFieldType("New Type");
    dynamicOptions.put("key2", newOption);

    requestDetails.setDynamicOptions(dynamicOptions);
    providerRequestFlowFormData.setRequestDetailsFormData(requestDetails);

    mapper.mapDynamicOptions(providerRequestDetails, providerRequestFlowFormData);

    assertTrue(providerRequestDetails.getDynamicOptions().isEmpty(), "Target dynamic options should remain unchanged for unmatched keys");
  }

  @Test
  @DisplayName("Should handle null file in EvidenceUploadFormData gracefully")
  void shouldHandleNullFileInEvidenceUploadFormData() {
    final EvidenceUploadFormData formData = new EvidenceUploadFormData();
    formData.setDocumentDescription("Test Description");
    formData.setDocumentType("docTypeId");
    formData.setDocumentTypeDisplayValue("docTypeDisplay");
    formData.setFile(null);

    final EvidenceDocumentDetail result = mapper.toProviderRequestDocumentDetail(formData);

    assertNotNull(result, "Mapped result should not be null");
    assertNull(result.getFileName(), "File name should be null when file is not provided");
    assertNull(result.getFileData(), "File data should be null when file is not provided");
    assertEquals("Test Description", result.getDescription(), "Description should match the document description");
    assertEquals("docTypeId", result.getDocumentType().getId(), "Document type ID should match");
    assertEquals("docTypeDisplay", result.getDocumentType().getDisplayValue(), "Document type display value should match");
    assertEquals(0, result.getTransferRetryCount(), "Transfer retry count should be set to 0");
  }

  @Test
  @DisplayName("Should map EvidenceUploadFormData to EvidenceDocumentDetail correctly")
  void shouldMapToProviderRequestDocumentDetail() {
    final EvidenceUploadFormData formData = new EvidenceUploadFormData();
    formData.setDocumentDescription("Test Description");
    formData.setCcmsModule(CcmsModule.REQUEST);
    formData.setApplicationOrOutcomeId("app123");
    formData.setCaseReferenceNumber("caseRef456");
    formData.setProviderId(789);
    formData.setFileExtension("pdf");
    formData.setRegisteredDocumentId("regDocId123");
    formData.setDocumentSender("SenderXYZ");

    final MultipartFile mockFile = mock(MultipartFile.class);
    when(mockFile.getOriginalFilename()).thenReturn("testFile.pdf");
    when(commonMapper.toFileBytes(mockFile)).thenReturn(new byte[]{1, 2, 3});
    when(commonMapper.toBase64EncodedStringFromByteArray(new byte[]{1, 2, 3})).thenReturn("encodedString");
    formData.setFile(mockFile);

    final EvidenceDocumentDetail result = mapper.toProviderRequestDocumentDetail(formData);

    assertNotNull(result, "Mapped result should not be null");
    assertEquals("Test Description", result.getDescription(), "Description should match");
    assertEquals(CcmsModule.REQUEST.getCode(), result.getCcmsModule(), "CCMS module should match");
    assertEquals("app123", result.getApplicationOrOutcomeId(), "Application or outcome ID should match");
    assertEquals("caseRef456", result.getCaseReferenceNumber(), "Case reference number should match");
    assertEquals("789", result.getProviderId(), "Provider ID should match");
    assertEquals("pdf", result.getFileExtension(), "File extension should match");
    assertEquals("regDocId123", result.getRegisteredDocumentId(), "Registered document ID should match");
    assertEquals("SenderXYZ", result.getDocumentSender(), "Document sender should match");
    assertEquals("encodedString", result.getFileData(), "File data should be base64 encoded");
    assertEquals("testFile.pdf", result.getFileName(), "File name should match the original filename");
    assertEquals(0, result.getTransferRetryCount(), "Transfer retry count should be set to 0");
  }

  @Test
  @DisplayName("Should handle null EvidenceUploadFormData gracefully")
  void shouldHandleNullEvidenceUploadFormData() {
    assertNull(mapper.toProviderRequestDocumentDetail(null), "Result should be null when input is null");
  }

  @Test
  @DisplayName("Should throw RuntimeException when file conversion fails")
  void shouldThrowRuntimeExceptionWhenFileConversionFails() {
    final EvidenceUploadFormData formData = new EvidenceUploadFormData();
    final MultipartFile mockFile = mock(MultipartFile.class);
    formData.setFile(mockFile);

    when(commonMapper.toFileBytes(mockFile)).thenThrow(new CaabApplicationException("File conversion error"));

    final RuntimeException exception = assertThrows(RuntimeException.class,
        () -> mapper.toProviderRequestDocumentDetail(formData),
        "Should throw RuntimeException on file conversion failure");
    assertInstanceOf(CaabApplicationException.class, exception.getCause(),
        "Cause should be CaabApplicationException");
  }

  @Test
  @DisplayName("Should return null for null ProviderRequestMappingContext")
  void shouldReturnNullForNullProviderRequestMappingContext() {
    assertNull(mapper.toProviderRequestDetail(null), "Result should be null when input is null");
  }

  @Test
  @DisplayName("Should map DynamicOptionFormData to ProviderRequestAttribute correctly")
  void shouldMapDynamicOptionToProviderRequestAttribute() {
    final DynamicOptionFormData dynamicOption = new DynamicOptionFormData();
    dynamicOption.setFieldDescription("Option Label");
    dynamicOption.setFieldValue("Option Value");

    final ProviderRequestAttribute result = mapper.dynamicOptionToProviderRequestAttribute(dynamicOption);

    assertNotNull(result, "Result should not be null");
    assertEquals("Option Label", result.getLabel(), "Label should match");
    assertEquals("Option Value", result.getText(), "Text should match");
  }

  @Test
  @DisplayName("Should return null for null DynamicOptionFormData")
  void shouldReturnNullForNullDynamicOption() {
    assertNull(mapper.dynamicOptionToProviderRequestAttribute(null), "Result should be null when input is null");
  }

  @Test
  @DisplayName("Should map additional info to ProviderRequestAttribute correctly")
  void shouldMapAdditionalInfoToProviderRequestAttribute() {
    final ProviderRequestDetailsFormData formData = new ProviderRequestDetailsFormData();
    formData.setAdditionalInformationLabel("Additional Info Label");
    formData.setAdditionalInformation("Some additional information");

    final ProviderRequestAttribute result = mapper.additionalInfoToProviderRequestAttribute(formData);

    assertNotNull(result, "Result should not be null");
    assertEquals("Additional Info Label", result.getLabel(), "Label should match");
    assertEquals("Some additional information", result.getText(), "Text should match");
  }

  @Test
  @DisplayName("Should return null for null ProviderRequestDetailsFormData (Additional Info)")
  void shouldReturnNullForNullAdditionalInfo() {
    assertNull(mapper.additionalInfoToProviderRequestAttribute(null), "Result should be null when input is null");
  }

  @Test
  @DisplayName("Should map file to ProviderRequestAttribute correctly")
  void shouldMapFileToProviderRequestAttribute() {
    final ProviderRequestDetailsFormData formData = new ProviderRequestDetailsFormData();
    formData.setClaimUploadLabel("Claim Upload Label");

    final MultipartFile mockFile = mock(MultipartFile.class);
    when(mockFile.getOriginalFilename()).thenReturn("testFile.pdf");
    when(commonMapper.toFileBytes(mockFile)).thenReturn(new byte[]{1, 2, 3});
    when(commonMapper.toBase64EncodedStringFromByteArray(new byte[]{1, 2, 3})).thenReturn("Base64String");

    formData.setFile(mockFile);

    final ProviderRequestAttribute result = mapper.fileToProviderRequestAttribute(formData);

    assertNotNull(result, "Result should not be null");
    assertEquals("Claim Upload Label", result.getLabel(), "Label should match");
    assertEquals("testFile.pdf", result.getText(), "File name should match");
  }

  @Test
  @DisplayName("Should return null for null ProviderRequestDetailsFormData (File)")
  void shouldReturnNullForNullFileData() {
    assertNull(mapper.fileToProviderRequestAttribute(null), "Result should be null when input is null");
  }

}
