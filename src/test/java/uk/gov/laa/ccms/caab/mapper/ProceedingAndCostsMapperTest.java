package uk.gov.laa.ccms.caab.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.laa.ccms.caab.bean.costs.CostsFormData;
import uk.gov.laa.ccms.caab.bean.priorauthority.PriorAuthorityFlowFormData;
import uk.gov.laa.ccms.caab.bean.priorauthority.PriorAuthorityFormDataDetails;
import uk.gov.laa.ccms.caab.bean.priorauthority.PriorAuthorityFormDataDynamicOption;
import uk.gov.laa.ccms.caab.bean.priorauthority.PriorAuthorityFormDataTypeDetails;
import uk.gov.laa.ccms.caab.bean.proceeding.ProceedingFlowFormData;
import uk.gov.laa.ccms.caab.bean.proceeding.ProceedingFormDataFurtherDetails;
import uk.gov.laa.ccms.caab.bean.proceeding.ProceedingFormDataMatterTypeDetails;
import uk.gov.laa.ccms.caab.bean.proceeding.ProceedingFormDataProceedingDetails;
import uk.gov.laa.ccms.caab.bean.scopelimitation.ScopeLimitationFlowFormData;
import uk.gov.laa.ccms.caab.model.CostStructure;
import uk.gov.laa.ccms.caab.model.PriorAuthority;
import uk.gov.laa.ccms.caab.model.Proceeding;
import uk.gov.laa.ccms.caab.model.ReferenceDataItem;
import uk.gov.laa.ccms.caab.model.ScopeLimitation;
import uk.gov.laa.ccms.caab.model.StringDisplayValue;
import uk.gov.laa.ccms.data.model.PriorAuthorityDetail;
import uk.gov.laa.ccms.data.model.PriorAuthorityTypeDetail;
import uk.gov.laa.ccms.data.model.ScopeLimitationDetail;

@ExtendWith(SpringExtension.class)
class ProceedingAndCostsMapperTest {

  private ProceedingAndCostsMapper mapper = new ProceedingAndCostsMapperImpl();

  @BeforeEach
  void setUp() {
    mapper = new ProceedingAndCostsMapperImpl();
  }

  @Test
  void testToProceeding_newProceeding() {
    // Create the form data objects with example values
    final ProceedingFormDataMatterTypeDetails matterTypeDetails = new ProceedingFormDataMatterTypeDetails();
    matterTypeDetails.setMatterType("MT001");
    matterTypeDetails.setMatterTypeDisplayValue("Matter Type 1");

    final ProceedingFormDataProceedingDetails proceedingDetails = new ProceedingFormDataProceedingDetails();
    proceedingDetails.setProceedingType("PT001");
    proceedingDetails.setProceedingTypeDisplayValue("Proceeding Type 1");
    proceedingDetails.setLarScope("Scope");

    final ProceedingFormDataFurtherDetails furtherDetails = new ProceedingFormDataFurtherDetails();
    furtherDetails.setClientInvolvementType("CI001");
    furtherDetails.setClientInvolvementTypeDisplayValue("Client Involvement 1");
    furtherDetails.setLevelOfService("LS001");
    furtherDetails.setLevelOfServiceDisplayValue("Level Of Service 1");
    furtherDetails.setTypeOfOrder("TO001");

    final ProceedingFlowFormData proceedingFlowFormData = new ProceedingFlowFormData("add");
    proceedingFlowFormData.setMatterTypeDetails(matterTypeDetails);
    proceedingFlowFormData.setProceedingDetails(proceedingDetails);
    proceedingFlowFormData.setFurtherDetails(furtherDetails);
    proceedingFlowFormData.setExistingProceedingId(12345);

    // Define the other parameters for the mapper method
    final BigDecimal costLimitation = new BigDecimal("5000");
    final String stage = "Initial";

    // Call the mapper method
    final Proceeding proceeding = mapper.toProceeding(proceedingFlowFormData, costLimitation, stage);

    // Assert the mapped values
    assertEquals(matterTypeDetails.getMatterType(), proceeding.getMatterType().getId());
    assertEquals(matterTypeDetails.getMatterTypeDisplayValue(), proceeding.getMatterType().getDisplayValue());
    assertEquals(proceedingDetails.getProceedingType(), proceeding.getProceedingType().getId());
    assertEquals(proceedingDetails.getProceedingTypeDisplayValue(), proceeding.getProceedingType().getDisplayValue());
    assertEquals(proceedingDetails.getProceedingTypeDisplayValue(), proceeding.getDescription());
    assertEquals(proceedingDetails.getLarScope(), proceeding.getLarScope());
    assertEquals(furtherDetails.getClientInvolvementType(), proceeding.getClientInvolvement().getId());
    assertEquals(furtherDetails.getClientInvolvementTypeDisplayValue(), proceeding.getClientInvolvement().getDisplayValue());
    assertEquals(furtherDetails.getLevelOfService(), proceeding.getLevelOfService().getId());
    assertEquals(furtherDetails.getLevelOfServiceDisplayValue(), proceeding.getLevelOfService().getDisplayValue());
    // Note: Type of order is set but not its display value in this case, so it's tested as null/ignored.
    assertEquals(12345, proceeding.getId());
    assertEquals(costLimitation, proceeding.getCostLimitation());
    assertEquals(stage, proceeding.getStage());
  }

  @Test
  void testToProceeding_existingProceeding() {
    // Create and setup ProceedingFlowFormData with example values
    final ProceedingFormDataMatterTypeDetails matterTypeDetails = new ProceedingFormDataMatterTypeDetails();
    matterTypeDetails.setMatterType("MT002");
    matterTypeDetails.setMatterTypeDisplayValue("Matter Type 2");

    final ProceedingFormDataProceedingDetails proceedingDetails = new ProceedingFormDataProceedingDetails();
    proceedingDetails.setProceedingType("PT002");
    proceedingDetails.setProceedingTypeDisplayValue("Proceeding Type 2");

    final ProceedingFormDataFurtherDetails furtherDetails = new ProceedingFormDataFurtherDetails();
    furtherDetails.setClientInvolvementType("CI002");
    furtherDetails.setClientInvolvementTypeDisplayValue("Client Involvement 2");
    furtherDetails.setLevelOfService("LS002");
    furtherDetails.setLevelOfServiceDisplayValue("Level Of Service 2");
    furtherDetails.setTypeOfOrder("TO002");

    final ProceedingFlowFormData proceedingFlowFormData = new ProceedingFlowFormData("edit");
    proceedingFlowFormData.setMatterTypeDetails(matterTypeDetails);
    proceedingFlowFormData.setProceedingDetails(proceedingDetails);
    proceedingFlowFormData.setFurtherDetails(furtherDetails);

    // Existing Proceeding object
    final Proceeding proceeding = new Proceeding();

    // Set different values to verify that they will be updated
    proceeding.setStage("Initial Stage");
    proceeding.setCostLimitation(new BigDecimal("10000"));

    // BigDecimal and stage to be used in the update
    final BigDecimal costLimitation = new BigDecimal("6000");
    final String stage = "Updated Stage";

    // Update the existing Proceeding object
    mapper.toProceeding(proceeding, proceedingFlowFormData, costLimitation, stage);

    // Validate the updated fields
    assertEquals(matterTypeDetails.getMatterType(), proceeding.getMatterType().getId());
    assertEquals(matterTypeDetails.getMatterTypeDisplayValue(), proceeding.getMatterType().getDisplayValue());
    assertEquals(proceedingDetails.getProceedingType(), proceeding.getProceedingType().getId());
    assertEquals(proceedingDetails.getProceedingTypeDisplayValue(), proceeding.getProceedingType().getDisplayValue());
    assertEquals(furtherDetails.getClientInvolvementType(), proceeding.getClientInvolvement().getId());
    assertEquals(furtherDetails.getClientInvolvementTypeDisplayValue(), proceeding.getClientInvolvement().getDisplayValue());
    assertEquals(furtherDetails.getLevelOfService(), proceeding.getLevelOfService().getId());
    assertEquals(furtherDetails.getLevelOfServiceDisplayValue(), proceeding.getLevelOfService().getDisplayValue());
    assertEquals(stage, proceeding.getStage());
    assertEquals(costLimitation, proceeding.getCostLimitation());

    assertNull(proceeding.getDescription());
  }

  @Test
  void testToProceedingFlow() {
    final Proceeding proceeding = new Proceeding();
    proceeding.setMatterType(new StringDisplayValue().id("MT001").displayValue("Matter Type 1"));
    proceeding.setProceedingType(new StringDisplayValue().id("PT001").displayValue("Proceeding Type 1"));
    proceeding.setDescription("Description");
    proceeding.setLarScope("Scope");
    proceeding.setClientInvolvement(new StringDisplayValue().id("CI001").displayValue("Client Involvement 1"));
    proceeding.setLevelOfService(new StringDisplayValue().id("LS001").displayValue("Level Of Service 1"));
    proceeding.setTypeOfOrder(new StringDisplayValue().id("TO001"));
    proceeding.setId(12345);
    proceeding.setLeadProceedingInd(true);

    final String typeOfOrderDisplayValue = "Order Type Display Value";

    final ProceedingFlowFormData formData = mapper.toProceedingFlow(proceeding, typeOfOrderDisplayValue);

    assertEquals(proceeding.getMatterType().getId(), formData.getMatterTypeDetails().getMatterType());
    assertEquals(proceeding.getMatterType().getDisplayValue(), formData.getMatterTypeDetails().getMatterTypeDisplayValue());
    assertEquals(proceeding.getProceedingType().getId(), formData.getProceedingDetails().getProceedingType());
    assertEquals(proceeding.getProceedingType().getDisplayValue(), formData.getProceedingDetails().getProceedingTypeDisplayValue());
    assertEquals(proceeding.getDescription(), formData.getProceedingDetails().getProceedingDescription());
    assertEquals(proceeding.getLarScope(), formData.getProceedingDetails().getLarScope());
    assertEquals(proceeding.getClientInvolvement().getId(), formData.getFurtherDetails().getClientInvolvementType());
    assertEquals(proceeding.getClientInvolvement().getDisplayValue(), formData.getFurtherDetails().getClientInvolvementTypeDisplayValue());
    assertEquals(proceeding.getLevelOfService().getId(), formData.getFurtherDetails().getLevelOfService());
    assertEquals(proceeding.getLevelOfService().getDisplayValue(), formData.getFurtherDetails().getLevelOfServiceDisplayValue());
    assertEquals(proceeding.getTypeOfOrder().getId(), formData.getFurtherDetails().getTypeOfOrder());
    assertEquals(typeOfOrderDisplayValue, formData.getFurtherDetails().getTypeOfOrderDisplayValue());
    assertEquals("edit", formData.getAction());
    assertFalse(formData.isAmended());
    assertFalse(formData.isEditingScopeLimitations());
    assertEquals(proceeding.getId(), formData.getExistingProceedingId());
    assertEquals(proceeding.getLeadProceedingInd(), formData.isLeadProceeding());
  }

  @Test
  void testToScopeLimitationList() {
    final List<ScopeLimitationDetail> scopeLimitationDetailList = new ArrayList<>();
    final ScopeLimitationDetail detail1 = new ScopeLimitationDetail();
    detail1.setScopeLimitations("SL001");
    detail1.setDescription("Description 1");
    detail1.setDefaultCode(true);
    detail1.setEmergencyScopeDefault(false);
    detail1.setNonStandardWordingRequired(true);

    final ScopeLimitationDetail detail2 = new ScopeLimitationDetail();
    detail2.setScopeLimitations("SL002");
    detail2.setDescription("Description 2");
    detail2.setDefaultCode(false);
    detail2.setEmergencyScopeDefault(true);
    detail2.setNonStandardWordingRequired(false);

    scopeLimitationDetailList.add(detail1);
    scopeLimitationDetailList.add(detail2);

    final List<ScopeLimitation> result = mapper.toScopeLimitationList(scopeLimitationDetailList);

    assertNotNull(result);
    assertEquals(2, result.size());
    assertEquals(detail1.getScopeLimitations(), result.get(0).getScopeLimitation().getId());
    assertEquals(detail1.getDescription(), result.get(0).getScopeLimitation().getDisplayValue());
    assertEquals(detail2.getScopeLimitations(), result.get(1).getScopeLimitation().getId());
    assertEquals(detail2.getDescription(), result.get(1).getScopeLimitation().getDisplayValue());
  }


  @Test
  void testToScopeLimitationList_withNull() {
    final List<ScopeLimitation> result = mapper.toScopeLimitationList(null);
    assertNull(result);
  }

  @Test
  void testToScopeLimitationFlow() {
    final ScopeLimitation scopeLimitation = new ScopeLimitation();
    scopeLimitation.setId(123);
    final StringDisplayValue displayValue = new StringDisplayValue().id("SL001").displayValue("Scope Limitation Description");
    scopeLimitation.setScopeLimitation(displayValue);

    final ScopeLimitationFlowFormData result = mapper.toScopeLimitationFlow(scopeLimitation);

    assertNotNull(result);
    assertEquals("edit", result.getAction());
    assertEquals(scopeLimitation.getId(), result.getScopeLimitationId());
    assertEquals(scopeLimitation.getScopeLimitation().getId(), result.getScopeLimitationDetails().getScopeLimitation());
  }

  @Test
  void testToScopeLimitation_withNull() {
    final ScopeLimitationFlowFormData result = mapper.toScopeLimitationFlow(null);
    assertNull(result);
  }

  @Test
  void testToCostsFormData() {
    final BigDecimal costLimitation = new BigDecimal("5000.00");
    final CostsFormData result = mapper.toCostsFormData(costLimitation);

    assertNotNull(result);
    assertEquals(costLimitation.toString(), result.getRequestedCostLimitation());
  }

  @Test
  void testToCostsFormData_withNull() {
    final CostsFormData result = mapper.toCostsFormData(null);

    assertNull(result);
  }

  @Test
  void testToCostStructure() {
    final CostStructure costStructure = new CostStructure();
    final CostsFormData costsFormData = new CostsFormData();
    costsFormData.setRequestedCostLimitation("10000.00");

    mapper.toCostStructure(costStructure, costsFormData);

    assertNotNull(costStructure.getRequestedCostLimitation());
    assertEquals(new BigDecimal("10000.00"), costStructure.getRequestedCostLimitation());
  }

  @Test
  void testToCostStructure_withNull() {
    final CostStructure costStructure = new CostStructure();
    mapper.toCostStructure(costStructure, null);

    assertNull(costStructure.getRequestedCostLimitation());
  }

  @Test
  void testToPriorAuthorityFlowFormData() {
    final PriorAuthority priorAuthority = new PriorAuthority();
    priorAuthority.setId(123);
    priorAuthority.setType(new StringDisplayValue().id("PA001").displayValue("Prior Authority Type"));
    final PriorAuthorityFormDataDetails formDataDetails = new PriorAuthorityFormDataDetails();
    formDataDetails.setSummary("Test Summary");

    final PriorAuthorityFlowFormData result = mapper.toPriorAuthorityFlowFormData(priorAuthority);

    assertNotNull(result);
    assertEquals("edit", result.getAction());
    assertEquals(priorAuthority.getId(), result.getPriorAuthorityId());
    assertEquals(priorAuthority.getType().getId(), result.getPriorAuthorityTypeFormDataDetails().getPriorAuthorityType());
    assertEquals(priorAuthority.getType().getDisplayValue(), result.getPriorAuthorityTypeFormDataDetails().getPriorAuthorityTypeDisplayValue());
  }

  @Test
  void testToPriorAuthorityFlowFormData_withNull() {
    final PriorAuthorityFlowFormData result = mapper.toPriorAuthorityFlowFormData(null);
    assertNull(result);
  }

  @Test
  void testToDynamicOptions() {
    final List<ReferenceDataItem> items = new ArrayList<>();
    final ReferenceDataItem item1 = new ReferenceDataItem();
    item1.setCode(new StringDisplayValue().id("Item1").displayValue("Item 1 Description"));
    item1.setValue(new StringDisplayValue().id("Value1").displayValue("Value 1 Description"));
    item1.setType("Type1");
    item1.setMandatory(true);

    final ReferenceDataItem item2 = new ReferenceDataItem();
    item2.setCode(new StringDisplayValue().id("Item2").displayValue("Item 2 Description"));
    item2.setValue(new StringDisplayValue().id("Value2").displayValue("Value 2 Description"));
    item2.setType("Type2");
    item2.setMandatory(false);

    items.add(item1);
    items.add(item2);

    final Map<String, PriorAuthorityFormDataDynamicOption> result = mapper.toDynamicOptions(items);

    assertNotNull(result);
    assertEquals(2, result.size());

    final PriorAuthorityFormDataDynamicOption option1 = result.get("Item1");
    assertNotNull(option1);
    assertEquals("Item 1 Description", option1.getFieldDescription());
    assertEquals("Type1", option1.getFieldType());
    assertTrue(option1.isMandatory());
    assertEquals("Value1", option1.getFieldValue());
    assertEquals("Value 1 Description", option1.getFieldValueDisplayValue());

    final PriorAuthorityFormDataDynamicOption option2 = result.get("Item2");
    assertNotNull(option2);
    assertEquals("Item 2 Description", option2.getFieldDescription());
    assertEquals("Type2", option2.getFieldType());
    assertFalse(option2.isMandatory());
    assertEquals("Value2", option2.getFieldValue());
    assertEquals("Value 2 Description", option2.getFieldValueDisplayValue());
  }

  @Test
  void testToDynamicOptions_withNull() {
    final Map<String, PriorAuthorityFormDataDynamicOption> result = mapper.toDynamicOptions(null);

    assertNull(result);
  }


  @Test
  void testToPriorAuthorityFormDataDetails() {
    final PriorAuthorityFormDataDetails priorAuthorityDetails = new PriorAuthorityFormDataDetails();
    final PriorAuthorityFlowFormData priorAuthorityFlowFormData = new PriorAuthorityFlowFormData("edit");
    final PriorAuthorityFormDataDetails formDataDetails = new PriorAuthorityFormDataDetails();
    formDataDetails.setValueRequired(true);
    priorAuthorityFlowFormData.setPriorAuthorityFormDataDetails(formDataDetails);

    mapper.toPriorAuthorityFormDataDetails(priorAuthorityDetails, priorAuthorityFlowFormData);

    assertEquals(formDataDetails.isValueRequired(), priorAuthorityDetails.isValueRequired());
  }

  @Test
  void testToPriorAuthorityFormDataDetails_withNull() {
    final PriorAuthorityFormDataDetails priorAuthorityDetails = new PriorAuthorityFormDataDetails();

    mapper.toPriorAuthorityFormDataDetails(priorAuthorityDetails, null);

    assertFalse(priorAuthorityDetails.isValueRequired());
  }

  @Test
  void testMapDynamicOptions() {
    final PriorAuthorityFormDataDetails priorAuthorityDetails = new PriorAuthorityFormDataDetails();
    priorAuthorityDetails.setDynamicOptions(new HashMap<>());

    final String key = "Option1";
    final PriorAuthorityFormDataDynamicOption optionDetails = new PriorAuthorityFormDataDynamicOption();
    optionDetails.setFieldDescription("Description 1");
    optionDetails.setFieldType("Type 1");
    optionDetails.setMandatory(true);
    priorAuthorityDetails.getDynamicOptions().put(key, optionDetails);

    final PriorAuthorityFlowFormData priorAuthorityFlowFormData = new PriorAuthorityFlowFormData("edit");
    final PriorAuthorityFormDataDetails formDataDetails = new PriorAuthorityFormDataDetails();
    formDataDetails.setDynamicOptions(new HashMap<>());
    final PriorAuthorityFormDataDynamicOption newOptionDetails = new PriorAuthorityFormDataDynamicOption();
    newOptionDetails.setFieldDescription("Updated Description 1");
    newOptionDetails.setFieldType("Updated Type 1");
    newOptionDetails.setMandatory(false);
    formDataDetails.getDynamicOptions().put(key, newOptionDetails);

    priorAuthorityFlowFormData.setPriorAuthorityFormDataDetails(formDataDetails);

    mapper.mapDynamicOptions(priorAuthorityDetails, priorAuthorityFlowFormData);

    assertNotNull(priorAuthorityDetails.getDynamicOptions().get(key));
    assertEquals("Updated Description 1", priorAuthorityDetails.getDynamicOptions().get(key).getFieldDescription());
    assertEquals("Updated Type 1", priorAuthorityDetails.getDynamicOptions().get(key).getFieldType());
    assertFalse(priorAuthorityDetails.getDynamicOptions().get(key).isMandatory());
  }

  @Test
  void testPopulatePriorAuthorityDetailsForm() {
    final PriorAuthorityFormDataDetails priorAuthorityDetails = new PriorAuthorityFormDataDetails();
    priorAuthorityDetails.setDynamicOptions(new HashMap<>());

    final PriorAuthorityTypeDetail priorAuthorityTypeDetail = new PriorAuthorityTypeDetail();
    List<PriorAuthorityDetail> priorAuthorityDetailsList = new ArrayList<>();

    PriorAuthorityDetail detail1 = new PriorAuthorityDetail();
    detail1.setCode("Detail1");
    detail1.setDescription("Description 1");
    detail1.setDataType("Type1");
    detail1.setMandatoryFlag(true);

    PriorAuthorityDetail detail2 = new PriorAuthorityDetail();
    detail2.setCode("Detail2");
    detail2.setDescription("Description 2");
    detail2.setDataType("Type2");
    detail2.setMandatoryFlag(false);

    priorAuthorityDetailsList.add(detail1);
    priorAuthorityDetailsList.add(detail2);
    priorAuthorityTypeDetail.setPriorAuthorities(priorAuthorityDetailsList);

    mapper.populatePriorAuthorityDetailsForm(priorAuthorityDetails, priorAuthorityTypeDetail);

    assertNotNull(priorAuthorityDetails.getDynamicOptions());
    assertEquals(2, priorAuthorityDetails.getDynamicOptions().size());
    assertTrue(priorAuthorityDetails.getDynamicOptions().containsKey("Detail1"));
    assertTrue(priorAuthorityDetails.getDynamicOptions().containsKey("Detail2"));

    PriorAuthorityFormDataDynamicOption option1 = priorAuthorityDetails.getDynamicOptions().get("Detail1");
    assertEquals("Description 1", option1.getFieldDescription());
    assertEquals("Type1", option1.getFieldType());
    assertTrue(option1.isMandatory());

    PriorAuthorityFormDataDynamicOption option2 = priorAuthorityDetails.getDynamicOptions().get("Detail2");
    assertEquals("Description 2", option2.getFieldDescription());
    assertEquals("Type2", option2.getFieldType());
    assertFalse(option2.isMandatory());
  }

  @Test
  void testPopulatePriorAuthorityDetailsForm_withEmptyDetails() {
    final PriorAuthorityFormDataDetails priorAuthorityDetails = new PriorAuthorityFormDataDetails();
    priorAuthorityDetails.setDynamicOptions(new HashMap<>());

    final PriorAuthorityTypeDetail priorAuthorityTypeDetail = new PriorAuthorityTypeDetail();
    // Assume no prior authorities are set in the detail.
    priorAuthorityTypeDetail.setPriorAuthorities(new ArrayList<>());

    mapper.populatePriorAuthorityDetailsForm(priorAuthorityDetails, priorAuthorityTypeDetail);

    assertTrue(priorAuthorityDetails.getDynamicOptions().isEmpty());
  }


  @Test
  void testToPriorAuthorityFormDataDynamicOption() {
    final PriorAuthorityDetail formOption = new PriorAuthorityDetail();
    formOption.setMandatoryFlag(true);
    formOption.setDescription("Option Description");
    formOption.setDataType("Option Type");

    final PriorAuthorityFormDataDynamicOption result = mapper.toPriorAuthorityFormDataDynamicOption(formOption);

    assertNotNull(result);
    assertTrue(result.isMandatory());
    assertEquals("Option Description", result.getFieldDescription());
    assertEquals("Option Type", result.getFieldType());
  }

  @Test
  void testToPriorAuthorityFormDataDynamicOption_withNull() {
    final PriorAuthorityDetail formOption = null;

    final PriorAuthorityFormDataDynamicOption result = mapper.toPriorAuthorityFormDataDynamicOption(formOption);

    assertNull(result);
  }

  @Test
  void testToPriorAuthority() {
    final PriorAuthorityFlowFormData priorAuthorityFlowFormData = new PriorAuthorityFlowFormData("edit");
    priorAuthorityFlowFormData.setPriorAuthorityId(123);

    final PriorAuthorityFormDataTypeDetails typeDetails = new PriorAuthorityFormDataTypeDetails();
    typeDetails.setPriorAuthorityType("Type1");
    typeDetails.setPriorAuthorityTypeDisplayValue("Type Display 1");
    priorAuthorityFlowFormData.setPriorAuthorityTypeFormDataDetails(typeDetails);

    final PriorAuthorityFormDataDetails formDataDetails = new PriorAuthorityFormDataDetails();
    formDataDetails.setSummary("Test Summary");
    formDataDetails.setJustification("Test Justification");
    formDataDetails.setValueRequired(true);
    formDataDetails.setAmountRequested("2000");
    priorAuthorityFlowFormData.setPriorAuthorityFormDataDetails(formDataDetails);

    final PriorAuthorityTypeDetail priorAuthorityDynamicForm = new PriorAuthorityTypeDetail();

    final PriorAuthority result = mapper.toPriorAuthority(priorAuthorityFlowFormData, priorAuthorityDynamicForm);

    assertNotNull(result);
    assertEquals(Integer.valueOf(123), result.getId());
    assertEquals("Test Summary", result.getSummary());
    assertEquals("Test Justification", result.getJustification());
    assertTrue(result.getValueRequired());
    assertEquals(new BigDecimal("2000"), result.getAmountRequested());
    assertEquals("Draft", result.getStatus());
  }

  @Test
  void testToPriorAuthority_withNull() {
    final PriorAuthority result = mapper.toPriorAuthority(null, null);
    assertNull(result);
  }

  @Test
  void testToReferenceDataItems() {
    final Map<String, PriorAuthorityFormDataDynamicOption> dynamicOptionsMap = new HashMap<>();
    final PriorAuthorityFormDataDynamicOption dynamicOption1 = new PriorAuthorityFormDataDynamicOption();
    dynamicOption1.setFieldDescription("Description 1");
    dynamicOption1.setFieldType("Type1");
    dynamicOption1.setMandatory(true);
    dynamicOption1.setFieldValue("Value1");
    dynamicOptionsMap.put("Key1", dynamicOption1);

    final PriorAuthorityTypeDetail priorAuthorityDynamicForm = new PriorAuthorityTypeDetail();
    final List<PriorAuthorityDetail> detailsList = new ArrayList<>();
    final PriorAuthorityDetail detail1 = new PriorAuthorityDetail();
    detail1.setCode("Key1");
    detail1.setLovCode("LOV1");
    detailsList.add(detail1);
    priorAuthorityDynamicForm.setPriorAuthorities(detailsList);

    final List<ReferenceDataItem> result = mapper.toReferenceDataItems(dynamicOptionsMap, priorAuthorityDynamicForm);

    assertNotNull(result);
    assertEquals(1, result.size());
    ReferenceDataItem item = result.get(0);
    assertEquals("Key1", item.getCode().getId());
    assertEquals("Description 1", item.getCode().getDisplayValue());
    assertEquals("LOV1", item.getLovLookUp());
  }

  @Test
  void testToReferenceDataItem() {
    final String key = "key1";
    final PriorAuthorityFormDataDynamicOption dynamicOption = new PriorAuthorityFormDataDynamicOption();
    dynamicOption.setFieldDescription("Field Description");
    dynamicOption.setFieldType("Field Type");
    dynamicOption.setMandatory(true);
    dynamicOption.setFieldValue("Value1");
    dynamicOption.setFieldValueDisplayValue("Value Display 1");

    final ReferenceDataItem result = mapper.toReferenceDataItem(key, dynamicOption);

    assertNotNull(result);
    assertNotNull(result.getCode());
    assertEquals(key, result.getCode().getId());
    assertEquals("Field Description", result.getCode().getDisplayValue());
    assertEquals("Field Type", result.getType());
    assertTrue(result.getMandatory());
    assertNotNull(result.getValue());
    assertEquals("Value1", result.getValue().getId());
    assertEquals("Value Display 1", result.getValue().getDisplayValue());
  }

  @Test
  void testToReferenceDataItem_withNull() {
    final ReferenceDataItem result = mapper.toReferenceDataItem(null, null);
    assertNull(result);
  }

}
