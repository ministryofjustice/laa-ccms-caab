package uk.gov.laa.ccms.caab.mapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import java.util.Calendar;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.laa.ccms.caab.bean.ApplicationFormData;
import uk.gov.laa.ccms.caab.model.ApplicationType;
import uk.gov.laa.ccms.caab.model.DevolvedPowers;

@ExtendWith(SpringExtension.class)
class ApplicationFormDataMapperTest {
  private final ApplicationFormDataMapper mapper = new ApplicationFormDataMapperImpl();

  @Test
  void testToApplicationTypeFormData() {
    // Create a sample DevolvedPowers
    DevolvedPowers devolvedPowers = new DevolvedPowers();
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
    assertEquals("23", result.getDelegatedFunctionUsedDay());
    assertEquals("10", result.getDelegatedFunctionUsedMonth());
    assertEquals("2023", result.getDelegatedFunctionUsedYear());
    assertEquals("ContractFlag", result.getDevolvedPowersContractFlag());
  }

  // Helper method to create a Date object with a specific year, month, and day
  private Date createDate(int year, int month, int day) {
    Calendar calendar = Calendar.getInstance();
    calendar.set(year, month - 1, day); // Calendar.MONTH is 0-based
    return calendar.getTime();
  }
}