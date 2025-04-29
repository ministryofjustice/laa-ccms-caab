package uk.gov.laa.ccms.caab.bean.validators.opponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CURRENT_OPPONENT;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.opponent.IndividualOpponentFormData;

@ExtendWith(SpringExtension.class)
class IndividualOpponentValidatorTest {

  @InjectMocks
  private IndividualOpponentValidator validator;

  private IndividualOpponentFormData opponentFormData;

  private Errors errors;

  @BeforeEach
  public void setUp() {
    opponentFormData =
        new IndividualOpponentFormData();
    errors = new BeanPropertyBindingResult(opponentFormData, CURRENT_OPPONENT);
  }

  @Test
  public void supports_ReturnsTrueForCorrectClass() {
    assertTrue(validator.supports(IndividualOpponentFormData.class));
  }

  @Test
  public void supports_ReturnsFalseForOtherClasses() {
    assertFalse(validator.supports(Object.class));
  }

  @Test
  public void validate_noErrors() {
    opponentFormData.setTitle("title");
    opponentFormData.setFirstName("first");
    opponentFormData.setSurname("surname");
    opponentFormData.setRelationshipToCase("rel2case");
    opponentFormData.setRelationshipToClient("rel2client");

    validator.validate(opponentFormData, errors);
    assertFalse(errors.hasErrors());
  }

  @Test
  public void validate_dateOfBirthMandatory() {
    opponentFormData.setTitle("title");
    opponentFormData.setFirstName("first");
    opponentFormData.setSurname("surname");
    opponentFormData.setRelationshipToCase("rel2case");
    opponentFormData.setRelationshipToClient("rel2client");
    opponentFormData.setDateOfBirthMandatory(true);

    validator.validate(opponentFormData, errors);
    assertEquals(1, errors.getErrorCount());
    assertEquals(1, errors.getFieldErrors("dateOfBirth").size());
  }

  @ParameterizedTest
  @CsvFileSource(resources = "/csv/IndividualOpponentValidatorTest_values.csv", nullValues = "null")
  public void testIndividualOpponentValidation(
      final String relationshipToCase,
      final String relationshipToClient,
      final String houseNameOrNumber,
      final String addressLine1,
      final String addressLine2,
      final String city,
      final String county,
      final String country,
      final String postcode,
      final String emailAddress,
      final String title,
      final String firstName,
      final String middleNames,
      final String surname,
      final String nino,
      final String certificateNumber,
      final String telephoneHome,
      final String telephoneWork,
      final String telephoneMobile,
      final String faxNumber,
      final String dateOfBirth,
      final boolean dateOfBirthMandatory,
      final String errorFieldName,
      final Integer errorCount) {
    opponentFormData.setRelationshipToCase(relationshipToCase);
    opponentFormData.setRelationshipToClient(relationshipToClient);
    
    opponentFormData.setHouseNameOrNumber(houseNameOrNumber);
    opponentFormData.setAddressLine1(addressLine1);
    opponentFormData.setAddressLine2(addressLine2);
    opponentFormData.setCity(city);
    opponentFormData.setCounty(county);
    opponentFormData.setCountry(country);
    opponentFormData.setPostcode(postcode);
    opponentFormData.setEmailAddress(emailAddress);

    opponentFormData.setTitle(title);
    opponentFormData.setFirstName(firstName);
    opponentFormData.setMiddleNames(middleNames);
    opponentFormData.setSurname(surname);
    opponentFormData.setNationalInsuranceNumber(nino);
    opponentFormData.setCertificateNumber(certificateNumber);
    opponentFormData.setTelephoneHome(telephoneHome);
    opponentFormData.setTelephoneWork(telephoneWork);
    opponentFormData.setTelephoneMobile(telephoneMobile);
    opponentFormData.setFaxNumber(faxNumber);
    opponentFormData.setDateOfBirth(dateOfBirth);
    opponentFormData.setDateOfBirthMandatory(dateOfBirthMandatory);

    validator.validate(opponentFormData, errors);
    assertTrue(errors.hasErrors());
    assertEquals(errorCount, errors.getErrorCount());

    assertNotNull(errors.getFieldErrors(errorFieldName));
    assertEquals(errorCount, errors.getFieldErrors(errorFieldName).size());
  }

  @ParameterizedTest
  @ValueSource(strings = {"USA", "GBR"})
  public void validate_invalidPostcodeFormat(String country)
  {
    opponentFormData.setRelationshipToCase("OPP");
    opponentFormData.setRelationshipToClient("CUSTOMER");
    opponentFormData.setTitle("Mr.");
    opponentFormData.setFirstName("ken");
    opponentFormData.setSurname("Smith");
    opponentFormData.setAddressLine1("1 The High Street");
    opponentFormData.setCountry(country);
    opponentFormData.setPostcode("@@@@@@");
    validator.validate(opponentFormData, errors);
    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("postcode"));
    assertEquals("invalid.format", errors.getFieldError("postcode").getCode());
    assertEquals(1, errors.getErrorCount());
  }

  @ParameterizedTest
  @ValueSource(strings = {"USA", "GBR"})
  public void validate_validPostcodeFormat(String country)
  {
    opponentFormData.setRelationshipToCase("OPP");
    opponentFormData.setRelationshipToClient("CUSTOMER");
    opponentFormData.setTitle("Mr.");
    opponentFormData.setFirstName("ken");
    opponentFormData.setSurname("Smith");
    opponentFormData.setAddressLine1("1 The High Street");
    opponentFormData.setCountry(country);
    opponentFormData.setPostcode("SA5 7DF");
    validator.validate(opponentFormData, errors);
    assertFalse(errors.hasErrors());
    assertEquals(0, errors.getErrorCount());
  }
}