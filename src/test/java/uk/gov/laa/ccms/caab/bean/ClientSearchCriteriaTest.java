package uk.gov.laa.ccms.caab.bean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;

@ExtendWith(SpringExtension.class)
public class ClientSearchCriteriaTest {

  private ClientSearchCriteria clientSearchCriteria;

  @BeforeEach
  public void setUp() {
    clientSearchCriteria = new ClientSearchCriteria();
  }

  @Test
  public void testGetDateOfBirth() {
    // Set the date components
    clientSearchCriteria.setDobDay("01");
    clientSearchCriteria.setDobMonth("02");
    clientSearchCriteria.setDobYear("1990");

    // Get the formatted date of birth
    String dateOfBirth = clientSearchCriteria.getDateOfBirth();

    // Check the result
    assertEquals("1990-02-01", dateOfBirth);
  }

  @ParameterizedTest
  @CsvSource({",01,2000",
      "01,,2000",
      "01,01,"})
  public void testGetDateOfBirth_NullComponent(String dobDay, String dobMonth, String dobYear) {
    clientSearchCriteria.setDobDay(dobDay);
    clientSearchCriteria.setDobMonth(dobMonth);
    clientSearchCriteria.setDobYear(dobYear);

    Assertions.assertThrows(CaabApplicationException.class, () -> {
      clientSearchCriteria.getDateOfBirth();
    });
  }

  @Test
  public void testGetUniqueIdentifier_MatchingType() {
    // Set the unique identifier type and value
    clientSearchCriteria.setUniqueIdentifierType(1); // Assuming 1 represents the matching type
    clientSearchCriteria.setUniqueIdentifierValue("ABC123");

    // Get the unique identifier
    String uniqueIdentifier =
        clientSearchCriteria.getUniqueIdentifier(1); // Passing matching type 1

    // Check the result
    assertEquals("ABC123", uniqueIdentifier);
  }

  @Test
  public void testGetUniqueIdentifier_NonMatchingType() {
    // Set the unique identifier type and value
    clientSearchCriteria.setUniqueIdentifierType(1); // Assuming 1 represents the matching type
    clientSearchCriteria.setUniqueIdentifierValue("ABC123");

    // Get the unique identifier with a different matching type
    String uniqueIdentifier =
        clientSearchCriteria.getUniqueIdentifier(2); // Passing a non-matching type 2

    // Check the result, it should be null as the matching type doesn't match
    assertNull(uniqueIdentifier);
  }
}