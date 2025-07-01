package uk.gov.laa.ccms.caab.builders;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import org.junit.jupiter.api.Test;
import uk.gov.laa.ccms.caab.model.ApplicationType;
import uk.gov.laa.ccms.caab.model.DevolvedPowersDetail;

public class ApplicationTypeBuilderTest {

  private final ApplicationTypeBuilder builder = new ApplicationTypeBuilder();

  @Test
  void testApplicationTypeForSubstantiveWithoutDelegatedFunctions() {
    ApplicationType result = builder.applicationType("SUB", false).build();
    assertEquals("SUB", result.getId());
    assertEquals("Substantive", result.getDisplayValue());
  }

  @Test
  void testApplicationTypeForSubstantiveWithDelegatedFunctions() {
    ApplicationType result = builder.applicationType("SUB", true).build();
    assertEquals("SUBDP", result.getId());
    assertEquals("Substantive Delegated Functions", result.getDisplayValue());
  }

  @Test
  void testApplicationTypeForEmergencyWithoutDelegatedFunctions() {
    ApplicationType result = builder.applicationType("EMER", false).build();
    assertEquals("EMER", result.getId());
    assertEquals("Emergency", result.getDisplayValue());
  }

  @Test
  void testApplicationTypeForEmergencyWithDelegatedFunctions() {
    ApplicationType result = builder.applicationType("EMER", true).build();
    assertEquals("DP", result.getId());
    assertEquals("Emergency Delegated Functions", result.getDisplayValue());
  }

  @Test
  void testApplicationTypeForExceptionalCaseFunding() {
    ApplicationType result = builder.applicationType("ECF", false).build();
    assertEquals("ECF", result.getId());
    assertEquals("Exceptional Case Funding", result.getDisplayValue());
  }

  @Test
  void testDevolvedPowers() throws ParseException {
    SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
    DevolvedPowersDetail devolvedPowers = new DevolvedPowersDetail();
    devolvedPowers.setUsed(true);
    devolvedPowers.setDateUsed(sdf.parse("01-01-2022"));

    ApplicationType result = builder.devolvedPowers(true, "1/1/2022").build();
    assertEquals(devolvedPowers, result.getDevolvedPowers());
  }

  @Test
  void testDevolvedPowersContractFlag() {
    DevolvedPowersDetail devolvedPowers = new DevolvedPowersDetail();
    devolvedPowers.setContractFlag("ContractFlag");

    ApplicationType result = builder.devolvedPowersContractFlag("ContractFlag").build();
    assertEquals("ContractFlag", result.getDevolvedPowers().getContractFlag());
  }
}
