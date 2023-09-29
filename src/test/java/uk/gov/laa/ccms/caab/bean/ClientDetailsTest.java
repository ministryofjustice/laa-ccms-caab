package uk.gov.laa.ccms.caab.bean;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
class ClientDetailsTest {

  private ClientDetails clientDetails;

  @BeforeEach
  public void setUp() {
    clientDetails = new ClientDetails();
  }

  @Test
  public void testGetDateOfBirth() {
    // Set the date components
    clientDetails.setDobDay("01");
    clientDetails.setDobMonth("02");
    clientDetails.setDobYear("1990");

    // Get the formatted date of birth
    String dateOfBirth = clientDetails.getDateOfBirth();

    // Check the result
    assertEquals("1990-02-01", dateOfBirth);
  }

  @ParameterizedTest
  @CsvSource({",01,2000",
      "01,,2000",
      "01,01,"})
  public void testGetDateOfBirth_NullComponent(String dobDay, String dobMonth, String dobYear) {
    clientDetails.setDobDay(dobDay);
    clientDetails.setDobMonth(dobMonth);
    clientDetails.setDobYear(dobYear);

    String dateOfBirth = clientDetails.getDateOfBirth();

    assertNull(dateOfBirth);
  }

}