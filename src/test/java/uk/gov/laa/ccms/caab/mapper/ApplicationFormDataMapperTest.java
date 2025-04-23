package uk.gov.laa.ccms.caab.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.laa.ccms.caab.bean.ApplicationFormData;
import uk.gov.laa.ccms.caab.model.ApplicationProviderDetails;
import uk.gov.laa.ccms.caab.model.ApplicationType;
import uk.gov.laa.ccms.caab.model.DevolvedPowersDetail;
import uk.gov.laa.ccms.caab.model.IntDisplayValue;
import uk.gov.laa.ccms.caab.model.StringDisplayValue;

@ExtendWith(SpringExtension.class)
class ApplicationFormDataMapperTest {
  private final ApplicationFormDataMapper mapper = new ApplicationFormDataMapperImpl();

  @Test
  void toApplicationTypeFormData() {
    // Create a sample DevolvedPowersDetail
    DevolvedPowersDetail devolvedPowers = new DevolvedPowersDetail();
    devolvedPowers.setUsed(true);
    Date dateUsed = createDate(2023, 10, 23);
    devolvedPowers.setDateUsed(dateUsed);
    devolvedPowers.setContractFlag("ContractFlag");

    ApplicationType applicationType = new ApplicationType()
        .id("SampleId")
        .devolvedPowers(devolvedPowers);


    // Call the method to be tested
    ApplicationFormData result = mapper.toApplicationTypeFormData(applicationType);

    // Verify that the mapping is correct
    assertNotNull(result);
    assertEquals("SampleId", result.getApplicationTypeCategory());
    assertEquals("23/10/2023", result.getDelegatedFunctionUsedDate());
    assertEquals("ContractFlag", result.getDevolvedPowersContractFlag());
  }

  @Test
  void toApplicationProviderDetailsFormData() {
    final ApplicationProviderDetails providerDetails = new ApplicationProviderDetails();
    providerDetails.setOffice(new IntDisplayValue().id(1).displayValue("OfficeName"));
    providerDetails.setFeeEarner(new StringDisplayValue().id("10").displayValue("Fee Earner Name"));
    providerDetails.setSupervisor(new StringDisplayValue().id("20").displayValue("Supervisor Name"));
    providerDetails.setProviderContact(new StringDisplayValue().id("30").displayValue("Contact Name"));
    providerDetails.setProviderCaseReference("CaseRef123");

    final ApplicationFormData result = mapper.toApplicationProviderDetailsFormData(providerDetails);

    assertNotNull(result);
    assertEquals(Integer.valueOf(1), result.getOfficeId());
    assertEquals("OfficeName", result.getOfficeName());
    assertEquals(Integer.valueOf(10), result.getFeeEarnerId());
    assertEquals(Integer.valueOf(20), result.getSupervisorId());
    assertEquals("30", result.getContactNameId());
    assertEquals("CaseRef123", result.getProviderCaseReference());
  }

  @Test
  void toApplicationProviderDetailsFormDataWithNull() {
    final ApplicationFormData result = mapper.toApplicationProviderDetailsFormData(null);
    assertNull(result);
  }


  // Helper method to create a Date object with a specific year, month, and day
  private Date createDate(int year, int month, int day) {
    LocalDate localDate = LocalDate.of(year, month, day);
    return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
  }
}
