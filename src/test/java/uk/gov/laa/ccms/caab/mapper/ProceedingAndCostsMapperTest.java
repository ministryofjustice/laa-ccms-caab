package uk.gov.laa.ccms.caab.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.laa.ccms.caab.bean.common.DynamicOptionFormData;
import uk.gov.laa.ccms.caab.bean.costs.CostsFormData;
import uk.gov.laa.ccms.caab.bean.priorauthority.PriorAuthorityDetailsFormData;
import uk.gov.laa.ccms.caab.bean.priorauthority.PriorAuthorityFlowFormData;
import uk.gov.laa.ccms.caab.bean.priorauthority.PriorAuthorityTypeFormData;
import uk.gov.laa.ccms.caab.bean.proceeding.ProceedingFlowFormData;
import uk.gov.laa.ccms.caab.bean.proceeding.ProceedingFormDataFurtherDetails;
import uk.gov.laa.ccms.caab.bean.proceeding.ProceedingFormDataMatterTypeDetails;
import uk.gov.laa.ccms.caab.bean.proceeding.ProceedingFormDataProceedingDetails;
import uk.gov.laa.ccms.caab.bean.scopelimitation.ScopeLimitationFlowFormData;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.ApplicationProviderDetails;
import uk.gov.laa.ccms.caab.model.CostEntryDetail;
import uk.gov.laa.ccms.caab.model.CostStructureDetail;
import uk.gov.laa.ccms.caab.model.IntDisplayValue;
import uk.gov.laa.ccms.caab.model.PriorAuthorityDetail;
import uk.gov.laa.ccms.caab.model.ProceedingDetail;
import uk.gov.laa.ccms.caab.model.ReferenceDataItemDetail;
import uk.gov.laa.ccms.caab.model.ScopeLimitationDetail;
import uk.gov.laa.ccms.caab.model.StringDisplayValue;
import uk.gov.laa.ccms.data.model.PriorAuthorityTypeDetail;

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
    final ProceedingFormDataMatterTypeDetails matterTypeDetails =
        new ProceedingFormDataMatterTypeDetails();
    matterTypeDetails.setMatterType("MT001");
    matterTypeDetails.setMatterTypeDisplayValue("Matter Type 1");

    final ProceedingFormDataProceedingDetails proceedingDetails =
        new ProceedingFormDataProceedingDetails();
    proceedingDetails.setProceedingType("PT001");
    proceedingDetails.setProceedingTypeDisplayValue("ProceedingDetail Type 1");
    proceedingDetails.setProceedingDescription("Description");
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
    final ProceedingDetail proceeding =
        mapper.toProceeding(proceedingFlowFormData, costLimitation, stage);

    // Assert the mapped values
    assertEquals(matterTypeDetails.getMatterType(), proceeding.getMatterType().getId());
    assertEquals(
        matterTypeDetails.getMatterTypeDisplayValue(),
        proceeding.getMatterType().getDisplayValue());
    assertEquals(proceedingDetails.getProceedingType(), proceeding.getProceedingType().getId());
    assertEquals(
        proceedingDetails.getProceedingTypeDisplayValue(),
        proceeding.getProceedingType().getDisplayValue());
    assertEquals(proceedingDetails.getProceedingDescription(), proceeding.getDescription());
    assertEquals(proceedingDetails.getLarScope(), proceeding.getLarScope());
    assertEquals(
        furtherDetails.getClientInvolvementType(), proceeding.getClientInvolvement().getId());
    assertEquals(
        furtherDetails.getClientInvolvementTypeDisplayValue(),
        proceeding.getClientInvolvement().getDisplayValue());
    assertEquals(furtherDetails.getLevelOfService(), proceeding.getLevelOfService().getId());
    assertEquals(
        furtherDetails.getLevelOfServiceDisplayValue(),
        proceeding.getLevelOfService().getDisplayValue());
    // Note: Type of order is set but not its display value in this case, so it's tested as
    // null/ignored.
    assertEquals(12345, proceeding.getId());
    assertEquals(costLimitation, proceeding.getCostLimitation());
    assertEquals(stage, proceeding.getStage());
  }

  @Test
  void testToProceeding_existingProceeding() {
    // Create and setup ProceedingFlowFormData with example values
    final ProceedingFormDataMatterTypeDetails matterTypeDetails =
        new ProceedingFormDataMatterTypeDetails();
    matterTypeDetails.setMatterType("MT002");
    matterTypeDetails.setMatterTypeDisplayValue("Matter Type 2");

    final ProceedingFormDataProceedingDetails proceedingDetails =
        new ProceedingFormDataProceedingDetails();
    proceedingDetails.setProceedingType("PT002");
    proceedingDetails.setProceedingTypeDisplayValue("ProceedingDetail Type 2");

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

    // Existing ProceedingDetail object
    final ProceedingDetail proceeding = new ProceedingDetail();

    // Set different values to verify that they will be updated
    proceeding.setStage("Initial Stage");
    proceeding.setCostLimitation(new BigDecimal("10000"));

    // BigDecimal and stage to be used in the update
    final BigDecimal costLimitation = new BigDecimal("6000");
    final String stage = "Updated Stage";

    // Update the existing ProceedingDetail object
    mapper.toProceeding(proceeding, proceedingFlowFormData, costLimitation, stage);

    // Validate the updated fields
    assertEquals(matterTypeDetails.getMatterType(), proceeding.getMatterType().getId());
    assertEquals(
        matterTypeDetails.getMatterTypeDisplayValue(),
        proceeding.getMatterType().getDisplayValue());
    assertEquals(proceedingDetails.getProceedingType(), proceeding.getProceedingType().getId());
    assertEquals(
        proceedingDetails.getProceedingTypeDisplayValue(),
        proceeding.getProceedingType().getDisplayValue());
    assertEquals(
        furtherDetails.getClientInvolvementType(), proceeding.getClientInvolvement().getId());
    assertEquals(
        furtherDetails.getClientInvolvementTypeDisplayValue(),
        proceeding.getClientInvolvement().getDisplayValue());
    assertEquals(furtherDetails.getLevelOfService(), proceeding.getLevelOfService().getId());
    assertEquals(
        furtherDetails.getLevelOfServiceDisplayValue(),
        proceeding.getLevelOfService().getDisplayValue());
    assertEquals(stage, proceeding.getStage());
    assertEquals(costLimitation, proceeding.getCostLimitation());

    assertNull(proceeding.getDescription());
  }

  @Test
  void testToProceedingFlow() {
    final ProceedingDetail proceeding = new ProceedingDetail();
    proceeding.setMatterType(new StringDisplayValue().id("MT001").displayValue("Matter Type 1"));
    proceeding.setProceedingType(
        new StringDisplayValue().id("PT001").displayValue("ProceedingDetail Type 1"));
    proceeding.setDescription("Description");
    proceeding.setLarScope("Scope");
    proceeding.setClientInvolvement(
        new StringDisplayValue().id("CI001").displayValue("Client Involvement 1"));
    proceeding.setLevelOfService(
        new StringDisplayValue().id("LS001").displayValue("Level Of Service 1"));
    proceeding.setTypeOfOrder(new StringDisplayValue().id("TO001"));
    proceeding.setId(12345);
    proceeding.setLeadProceedingInd(true);

    final String typeOfOrderDisplayValue = "Order Type Display Value";

    final ProceedingFlowFormData formData =
        mapper.toProceedingFlow(proceeding, typeOfOrderDisplayValue);

    assertEquals(
        proceeding.getMatterType().getId(), formData.getMatterTypeDetails().getMatterType());
    assertEquals(
        proceeding.getMatterType().getDisplayValue(),
        formData.getMatterTypeDetails().getMatterTypeDisplayValue());
    assertEquals(
        proceeding.getProceedingType().getId(),
        formData.getProceedingDetails().getProceedingType());
    assertEquals(
        proceeding.getProceedingType().getDisplayValue(),
        formData.getProceedingDetails().getProceedingTypeDisplayValue());
    assertEquals(
        proceeding.getDescription(), formData.getProceedingDetails().getProceedingDescription());
    assertEquals(proceeding.getLarScope(), formData.getProceedingDetails().getLarScope());
    assertEquals(
        proceeding.getClientInvolvement().getId(),
        formData.getFurtherDetails().getClientInvolvementType());
    assertEquals(
        proceeding.getClientInvolvement().getDisplayValue(),
        formData.getFurtherDetails().getClientInvolvementTypeDisplayValue());
    assertEquals(
        proceeding.getLevelOfService().getId(), formData.getFurtherDetails().getLevelOfService());
    assertEquals(
        proceeding.getLevelOfService().getDisplayValue(),
        formData.getFurtherDetails().getLevelOfServiceDisplayValue());
    assertEquals(
        proceeding.getTypeOfOrder().getId(), formData.getFurtherDetails().getTypeOfOrder());
    assertEquals(
        typeOfOrderDisplayValue, formData.getFurtherDetails().getTypeOfOrderDisplayValue());
    assertEquals("edit", formData.getAction());
    assertFalse(formData.isAmended());
    assertFalse(formData.isEditingScopeLimitations());
    assertEquals(proceeding.getId(), formData.getExistingProceedingId());
    assertEquals(proceeding.getLeadProceedingInd(), formData.isLeadProceeding());
  }

  @Test
  void testToScopeLimitationList() {
    final List<uk.gov.laa.ccms.data.model.ScopeLimitationDetail> scopeLimitationDetailList =
        new ArrayList<>();
    final uk.gov.laa.ccms.data.model.ScopeLimitationDetail ebsScopeLimitation1 =
        new uk.gov.laa.ccms.data.model.ScopeLimitationDetail();
    ebsScopeLimitation1.setScopeLimitations("SL001");
    ebsScopeLimitation1.setDescription("Description 1");
    ebsScopeLimitation1.setDefaultCode(true);
    ebsScopeLimitation1.setEmergencyScopeDefault(false);
    ebsScopeLimitation1.setNonStandardWordingRequired(true);

    final uk.gov.laa.ccms.data.model.ScopeLimitationDetail ebsScopeLimitation2 =
        new uk.gov.laa.ccms.data.model.ScopeLimitationDetail();
    ebsScopeLimitation2.setScopeLimitations("SL002");
    ebsScopeLimitation2.setDescription("Description 2");
    ebsScopeLimitation2.setDefaultCode(false);
    ebsScopeLimitation2.setEmergencyScopeDefault(true);
    ebsScopeLimitation2.setNonStandardWordingRequired(false);

    scopeLimitationDetailList.add(ebsScopeLimitation1);
    scopeLimitationDetailList.add(ebsScopeLimitation2);

    final List<ScopeLimitationDetail> result =
        mapper.toScopeLimitationList(scopeLimitationDetailList);

    assertNotNull(result);
    assertEquals(2, result.size());
    assertEquals(
        ebsScopeLimitation1.getScopeLimitations(), result.getFirst().getScopeLimitation().getId());
    assertEquals(
        ebsScopeLimitation1.getDescription(),
        result.getFirst().getScopeLimitation().getDisplayValue());
    assertEquals(
        ebsScopeLimitation2.getScopeLimitations(), result.get(1).getScopeLimitation().getId());
    assertEquals(
        ebsScopeLimitation2.getDescription(), result.get(1).getScopeLimitation().getDisplayValue());
  }

  @Test
  void testToScopeLimitationList_withNull() {
    final List<ScopeLimitationDetail> result = mapper.toScopeLimitationList(null);
    assertNull(result);
  }

  @Test
  void testToScopeLimitationFlow() {
    final ScopeLimitationDetail scopeLimitation = new ScopeLimitationDetail();
    scopeLimitation.setId(123);
    final StringDisplayValue displayValue =
        new StringDisplayValue().id("SL001").displayValue("Scope Limitation Description");
    scopeLimitation.setScopeLimitation(displayValue);

    final ScopeLimitationFlowFormData result = mapper.toScopeLimitationFlow(scopeLimitation);

    assertNotNull(result);
    assertEquals("edit", result.getAction());
    assertEquals(scopeLimitation.getId(), result.getScopeLimitationId());
    assertEquals(
        scopeLimitation.getScopeLimitation().getId(),
        result.getScopeLimitationDetails().getScopeLimitation());
  }

  @Test
  void testToScopeLimitation_withNull() {
    final ScopeLimitationFlowFormData result = mapper.toScopeLimitationFlow(null);
    assertNull(result);
  }

  @Test
  void testToCostsFormData() {
    final BigDecimal grantedCostLimitation = new BigDecimal("3000.00");
    final BigDecimal costLimitation = new BigDecimal("5000.00");
    final CostStructureDetail costStructureDetail =
        new CostStructureDetail()
            .requestedCostLimitation(costLimitation)
            .grantedCostLimitation(grantedCostLimitation);
    final CostsFormData result = mapper.toCostsFormData(costStructureDetail);

    assertNotNull(result);
    assertEquals(grantedCostLimitation, result.getGrantedCostLimitation());
    assertEquals(costLimitation.toString(), result.getRequestedCostLimitation());
  }

  @Test
  void testToCostsData() {
    final BigDecimal grantedCostLimitation = new BigDecimal("3000.00");
    final BigDecimal costLimitation = new BigDecimal("5000.00");
    final ApplicationProviderDetails provider =
        new ApplicationProviderDetails().provider(new IntDisplayValue().displayValue("City Law"));
    final CostEntryDetail costEntryDetail =
        new CostEntryDetail()
            .resourceName("Patrick")
            .costCategory("counsel")
            .resourceName("Marie Bowe")
            .requestedCosts(new BigDecimal("3000.00"))
            .amountBilled(new BigDecimal("100.00"));

    final CostStructureDetail costStructureDetail =
        new CostStructureDetail()
            .requestedCostLimitation(costLimitation)
            .grantedCostLimitation(grantedCostLimitation)
            .costEntries(Collections.singletonList(costEntryDetail));
    final ApplicationDetail applicationDetail =
        new ApplicationDetail().providerDetails(provider).costs(costStructureDetail);
    final CostsFormData result = mapper.toCostsForm(applicationDetail);

    assertNotNull(result);
    assertEquals(grantedCostLimitation, result.getGrantedCostLimitation());
    assertEquals(provider.getProvider().getDisplayValue(), result.getProviderName());
    assertEquals(costEntryDetail, result.getCostEntries().get(0));
    assertEquals(costLimitation.toString(), result.getRequestedCostLimitation());
  }

  @Test
  void testToCostsForm_withNull() {
    final CostsFormData result = mapper.toCostsForm(null);

    assertNull(result);
  }

  @Test
  void testToCostsFormData_withNull() {
    final CostsFormData result = mapper.toCostsFormData(null);

    assertNull(result);
  }

  @Test
  void testToCostStructure() {
    final CostStructureDetail costStructure = new CostStructureDetail();
    final CostsFormData costsFormData = new CostsFormData(new BigDecimal("20000.00"));
    costsFormData.setRequestedCostLimitation("10000.00");

    mapper.toCostStructure(costStructure, costsFormData);

    assertNotNull(costStructure.getRequestedCostLimitation());
    assertEquals(new BigDecimal("10000.00"), costStructure.getRequestedCostLimitation());
  }

  @Test
  void testToCostStructure_withNull() {
    final CostStructureDetail costStructure = new CostStructureDetail();
    mapper.toCostStructure(costStructure, null);

    assertNull(costStructure.getRequestedCostLimitation());
  }

  @Test
  void testToPriorAuthorityFlowFormData() {
    final PriorAuthorityDetail priorAuthority = new PriorAuthorityDetail();
    priorAuthority.setId(123);
    priorAuthority.setType(
        new StringDisplayValue().id("PA001").displayValue("Prior Authority Type"));
    final PriorAuthorityDetailsFormData formDataDetails = new PriorAuthorityDetailsFormData();
    formDataDetails.setSummary("Test Summary");

    final PriorAuthorityFlowFormData result = mapper.toPriorAuthorityFlowFormData(priorAuthority);

    assertNotNull(result);
    assertEquals("edit", result.getAction());
    assertEquals(priorAuthority.getId(), result.getPriorAuthorityId());
    assertEquals(
        priorAuthority.getType().getId(),
        result.getPriorAuthorityTypeFormData().getPriorAuthorityType());
    assertEquals(
        priorAuthority.getType().getDisplayValue(),
        result.getPriorAuthorityTypeFormData().getPriorAuthorityTypeDisplayValue());
  }

  @Test
  void testToPriorAuthorityFlowFormData_withNull() {
    final PriorAuthorityFlowFormData result = mapper.toPriorAuthorityFlowFormData(null);
    assertNull(result);
  }

  @Test
  void testToDynamicOptions() {
    final List<ReferenceDataItemDetail> items = new ArrayList<>();
    final ReferenceDataItemDetail item1 = new ReferenceDataItemDetail();
    item1.setCode(new StringDisplayValue().id("Item1").displayValue("Item 1 Description"));
    item1.setValue(new StringDisplayValue().id("Value1").displayValue("Value 1 Description"));
    item1.setType("Type1");
    item1.setMandatory(true);

    final ReferenceDataItemDetail item2 = new ReferenceDataItemDetail();
    item2.setCode(new StringDisplayValue().id("Item2").displayValue("Item 2 Description"));
    item2.setValue(new StringDisplayValue().id("Value2").displayValue("Value 2 Description"));
    item2.setType("Type2");
    item2.setMandatory(false);

    items.add(item1);
    items.add(item2);

    final Map<String, DynamicOptionFormData> result = mapper.toDynamicOptions(items);

    assertNotNull(result);
    assertEquals(2, result.size());

    final DynamicOptionFormData option1 = result.get("Item1");
    assertNotNull(option1);
    assertEquals("Item 1 Description", option1.getFieldDescription());
    assertEquals("Type1", option1.getFieldType());
    assertTrue(option1.isMandatory());
    assertEquals("Value1", option1.getFieldValue());
    assertEquals("Value 1 Description", option1.getFieldValueDisplayValue());

    final DynamicOptionFormData option2 = result.get("Item2");
    assertNotNull(option2);
    assertEquals("Item 2 Description", option2.getFieldDescription());
    assertEquals("Type2", option2.getFieldType());
    assertFalse(option2.isMandatory());
    assertEquals("Value2", option2.getFieldValue());
    assertEquals("Value 2 Description", option2.getFieldValueDisplayValue());
  }

  @Test
  void testToDynamicOptions_withNull() {
    final Map<String, DynamicOptionFormData> result = mapper.toDynamicOptions(null);

    assertNull(result);
  }

  @Test
  void testToPriorAuthorityFormDataDetails() {
    final PriorAuthorityDetailsFormData priorAuthorityDetails = new PriorAuthorityDetailsFormData();
    final PriorAuthorityFlowFormData priorAuthorityFlowFormData =
        new PriorAuthorityFlowFormData("edit");
    final PriorAuthorityDetailsFormData formDataDetails = new PriorAuthorityDetailsFormData();
    formDataDetails.setValueRequired(true);
    priorAuthorityFlowFormData.setPriorAuthorityDetailsFormData(formDataDetails);

    mapper.toPriorAuthorityDetailsFormData(priorAuthorityDetails, priorAuthorityFlowFormData);

    assertEquals(formDataDetails.isValueRequired(), priorAuthorityDetails.isValueRequired());
  }

  @Test
  void testToPriorAuthorityFormDataDetails_withNull() {
    final PriorAuthorityDetailsFormData priorAuthorityDetails = new PriorAuthorityDetailsFormData();

    mapper.toPriorAuthorityDetailsFormData(priorAuthorityDetails, null);

    assertFalse(priorAuthorityDetails.isValueRequired());
  }

  @Test
  void testMapDynamicOptions() {
    final PriorAuthorityDetailsFormData priorAuthorityDetails = new PriorAuthorityDetailsFormData();
    priorAuthorityDetails.setDynamicOptions(new HashMap<>());

    final String key = "Option1";
    final DynamicOptionFormData optionDetails = new DynamicOptionFormData();
    optionDetails.setFieldDescription("Description 1");
    optionDetails.setFieldType("Type 1");
    optionDetails.setMandatory(true);
    priorAuthorityDetails.getDynamicOptions().put(key, optionDetails);

    final PriorAuthorityFlowFormData priorAuthorityFlowFormData =
        new PriorAuthorityFlowFormData("edit");
    final PriorAuthorityDetailsFormData formDataDetails = new PriorAuthorityDetailsFormData();
    formDataDetails.setDynamicOptions(new HashMap<>());
    final DynamicOptionFormData newOptionDetails = new DynamicOptionFormData();
    newOptionDetails.setFieldDescription("Updated Description 1");
    newOptionDetails.setFieldType("Updated Type 1");
    newOptionDetails.setMandatory(false);
    formDataDetails.getDynamicOptions().put(key, newOptionDetails);

    priorAuthorityFlowFormData.setPriorAuthorityDetailsFormData(formDataDetails);

    mapper.mapDynamicOptions(priorAuthorityDetails, priorAuthorityFlowFormData);

    assertNotNull(priorAuthorityDetails.getDynamicOptions().get(key));
    assertEquals(
        "Updated Description 1",
        priorAuthorityDetails.getDynamicOptions().get(key).getFieldDescription());
    assertEquals(
        "Updated Type 1", priorAuthorityDetails.getDynamicOptions().get(key).getFieldType());
    assertFalse(priorAuthorityDetails.getDynamicOptions().get(key).isMandatory());
  }

  @Test
  void testPopulatePriorAuthorityDetailsForm() {
    final PriorAuthorityDetailsFormData priorAuthorityDetails = new PriorAuthorityDetailsFormData();
    priorAuthorityDetails.setDynamicOptions(new HashMap<>());

    final PriorAuthorityTypeDetail priorAuthorityTypeDetail = new PriorAuthorityTypeDetail();
    List<uk.gov.laa.ccms.data.model.PriorAuthorityDetail> priorAuthorityDetailsList =
        new ArrayList<>();

    uk.gov.laa.ccms.data.model.PriorAuthorityDetail ebsPriorAuth1 =
        new uk.gov.laa.ccms.data.model.PriorAuthorityDetail();
    ebsPriorAuth1.setCode("Detail1");
    ebsPriorAuth1.setDescription("Description 1");
    ebsPriorAuth1.setDataType("Type1");
    ebsPriorAuth1.setMandatoryFlag(true);

    uk.gov.laa.ccms.data.model.PriorAuthorityDetail ebsPriorAuth2 =
        new uk.gov.laa.ccms.data.model.PriorAuthorityDetail();
    ebsPriorAuth2.setCode("Detail2");
    ebsPriorAuth2.setDescription("Description 2");
    ebsPriorAuth2.setDataType("Type2");
    ebsPriorAuth2.setMandatoryFlag(false);

    priorAuthorityDetailsList.add(ebsPriorAuth1);
    priorAuthorityDetailsList.add(ebsPriorAuth2);
    priorAuthorityTypeDetail.setPriorAuthorities(priorAuthorityDetailsList);

    mapper.populatePriorAuthorityDetailsForm(priorAuthorityDetails, priorAuthorityTypeDetail);

    assertNotNull(priorAuthorityDetails.getDynamicOptions());
    assertEquals(2, priorAuthorityDetails.getDynamicOptions().size());
    assertTrue(priorAuthorityDetails.getDynamicOptions().containsKey("Detail1"));
    assertTrue(priorAuthorityDetails.getDynamicOptions().containsKey("Detail2"));

    DynamicOptionFormData option1 = priorAuthorityDetails.getDynamicOptions().get("Detail1");
    assertEquals("Description 1", option1.getFieldDescription());
    assertEquals("Type1", option1.getFieldType());
    assertTrue(option1.isMandatory());

    DynamicOptionFormData option2 = priorAuthorityDetails.getDynamicOptions().get("Detail2");
    assertEquals("Description 2", option2.getFieldDescription());
    assertEquals("Type2", option2.getFieldType());
    assertFalse(option2.isMandatory());
  }

  @Test
  void testPopulatePriorAuthorityDetailsForm_withEmptyDetails() {
    final PriorAuthorityDetailsFormData priorAuthorityDetails = new PriorAuthorityDetailsFormData();
    priorAuthorityDetails.setDynamicOptions(new HashMap<>());

    final PriorAuthorityTypeDetail priorAuthorityTypeDetail = new PriorAuthorityTypeDetail();
    // Assume no prior authorities are set in the detail.
    priorAuthorityTypeDetail.setPriorAuthorities(new ArrayList<>());

    mapper.populatePriorAuthorityDetailsForm(priorAuthorityDetails, priorAuthorityTypeDetail);

    assertTrue(priorAuthorityDetails.getDynamicOptions().isEmpty());
  }

  @Test
  void testToPriorAuthorityFormDataDynamicOption() {
    final uk.gov.laa.ccms.data.model.PriorAuthorityDetail formOption =
        new uk.gov.laa.ccms.data.model.PriorAuthorityDetail();
    formOption.setMandatoryFlag(true);
    formOption.setDescription("Option Description");
    formOption.setDataType("Option Type");

    final DynamicOptionFormData result = mapper.toPriorAuthorityFormDataDynamicOption(formOption);

    assertNotNull(result);
    assertTrue(result.isMandatory());
    assertEquals("Option Description", result.getFieldDescription());
    assertEquals("Option Type", result.getFieldType());
  }

  @Test
  void testToPriorAuthorityFormDataDynamicOption_withNull() {
    final uk.gov.laa.ccms.data.model.PriorAuthorityDetail formOption = null;

    final DynamicOptionFormData result = mapper.toPriorAuthorityFormDataDynamicOption(formOption);

    assertNull(result);
  }

  @Test
  void testToPriorAuthority() {
    final PriorAuthorityFlowFormData priorAuthorityFlowFormData =
        new PriorAuthorityFlowFormData("edit");
    priorAuthorityFlowFormData.setPriorAuthorityId(123);

    final PriorAuthorityTypeFormData typeDetails = new PriorAuthorityTypeFormData();
    typeDetails.setPriorAuthorityType("Type1");
    typeDetails.setPriorAuthorityTypeDisplayValue("Type Display 1");
    priorAuthorityFlowFormData.setPriorAuthorityTypeFormData(typeDetails);

    final PriorAuthorityDetailsFormData formDataDetails = new PriorAuthorityDetailsFormData();
    formDataDetails.setSummary("Test Summary");
    formDataDetails.setJustification("Test Justification");
    formDataDetails.setValueRequired(true);
    formDataDetails.setAmountRequested("2000");
    priorAuthorityFlowFormData.setPriorAuthorityDetailsFormData(formDataDetails);

    final PriorAuthorityTypeDetail priorAuthorityDynamicForm = new PriorAuthorityTypeDetail();

    final PriorAuthorityDetail result =
        mapper.toPriorAuthority(priorAuthorityFlowFormData, priorAuthorityDynamicForm);

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
    final PriorAuthorityDetail result = mapper.toPriorAuthority(null, null);
    assertNull(result);
  }

  @Test
  void testToReferenceDataItems() {
    final Map<String, DynamicOptionFormData> dynamicOptionsMap = new HashMap<>();
    final DynamicOptionFormData dynamicOption1 = new DynamicOptionFormData();
    dynamicOption1.setFieldDescription("Description 1");
    dynamicOption1.setFieldType("Type1");
    dynamicOption1.setMandatory(true);
    dynamicOption1.setFieldValue("Value1");
    dynamicOptionsMap.put("Key1", dynamicOption1);

    final PriorAuthorityTypeDetail priorAuthorityDynamicForm = new PriorAuthorityTypeDetail();
    final List<uk.gov.laa.ccms.data.model.PriorAuthorityDetail> detailsList = new ArrayList<>();
    final uk.gov.laa.ccms.data.model.PriorAuthorityDetail detail1 =
        new uk.gov.laa.ccms.data.model.PriorAuthorityDetail();
    detail1.setCode("Key1");
    detail1.setLovCode("LOV1");
    detailsList.add(detail1);
    priorAuthorityDynamicForm.setPriorAuthorities(detailsList);

    final List<ReferenceDataItemDetail> result =
        mapper.toReferenceDataItems(dynamicOptionsMap, priorAuthorityDynamicForm);

    assertNotNull(result);
    assertEquals(1, result.size());
    ReferenceDataItemDetail item = result.getFirst();
    assertEquals("Key1", item.getCode().getId());
    assertEquals("Description 1", item.getCode().getDisplayValue());
    assertEquals("LOV1", item.getLovLookUp());
  }

  @Test
  void testToReferenceDataItem() {
    final String key = "key1";
    final DynamicOptionFormData dynamicOption = new DynamicOptionFormData();
    dynamicOption.setFieldDescription("Field Description");
    dynamicOption.setFieldType("Field Type");
    dynamicOption.setMandatory(true);
    dynamicOption.setFieldValue("Value1");
    dynamicOption.setFieldValueDisplayValue("Value Display 1");

    final ReferenceDataItemDetail result = mapper.toReferenceDataItem(key, dynamicOption);

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
    final ReferenceDataItemDetail result = mapper.toReferenceDataItem(null, null);
    assertNull(result);
  }
}
