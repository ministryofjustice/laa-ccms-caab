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
import org.junit.jupiter.params.provider.CsvSource;
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
    assertEquals(3, errors.getErrorCount());

    // Should be a required error for each date component.
    assertEquals(1, errors.getFieldErrors("dobDay").size());
    assertEquals(1, errors.getFieldErrors("dobMonth").size());
    assertEquals(1, errors.getFieldErrors("dobYear").size());
  }

  @ParameterizedTest
  @CsvSource(value = {
      "null,     rel2client, null,      null,     null, null, null,     null,     null,       null,             title,    firstname,  null,       surname,  null,       null, null,     null,     null,     null,     null, null, null, relationshipToCase, 1",
      "rel2case, null,       null,      null,     null, null, null,     null,     null,       null,             title,    firstname,  null,       surname,  null,       null, null,     null,     null,     null,     null, null, null, relationshipToClient, 1",
      "rel2case, rel2client, <,         addline1, null, null, null,     country,  null,       null,             title,    firstname,  null,       surname,  null,       null, null,     null,     null,     null,     null, null, null, houseNameOrNumber, 1",
      "rel2case, rel2client, housenum,  null,     null, null, null,     country,  null,       null,             title,    firstname,  null,       surname,  null,       null, null,     null,     null,     null,     null, null, null, addressLine1, 1",
      "rel2case, rel2client, null,      addline1, null, null, null,     null,     null,       null,             title,    firstname,  null,       surname,  null,       null, null,     null,     null,     null,     null, null, null, country, 1",
      "rel2case, rel2client, null,      addline1, null, null, null,     country,  <postcode,  null,             title,    firstname,  null,       surname,  null,       null, null,     null,     null,     null,     null, null, null, postcode, 1",
      "rel2case, rel2client, null,      addline1, null, null, null,     country,  null,       email,            title,    firstname,  null,       surname,  null,       null, null,     null,     null,     null,     null, null, null, emailAddress, 1",
      "rel2case, rel2client, null,      null    , null, null, null,     null,     null,       email@email.com,  null,     firstname,  null,       surname,  null,       null, null,     null,     null,     null,     null, null, null, title, 1",
      "rel2case, rel2client, null,      null    , null, null, null,     null,     null,       null,             title,    null,       null,       surname,  null,       null, null,     null,     null,     null,     null, null, null, firstName, 1",
      "rel2case, rel2client, null,      null    , null, null, null,     null,     null,       null,             title,    <  >,       null,       surname,  null,       null, null,     null,     null,     null,     null, null, null, firstName, 3",
      "rel2case, rel2client, null,      null    , null, null, null,     null,     null,       null,             title,    firstname,  <  >,       surname,  null,       null, null,     null,     null,     null,     null, null, null, middleNames, 3",
      "rel2case, rel2client, null,      null    , null, null, null,     null,     null,       null,             title,    firstname,  middleNames, null,    null,       null, null,     null,     null,     null,     null, null, null, surname, 1",
      "rel2case, rel2client, null,      null    , null, null, null,     null,     null,       null,             title,    firstname,  middleNames, <  >,    null,       null, null,     null,     null,     null,     null, null, null, surname, 3",
      "rel2case, rel2client, null,      null    , null, null, null,     null,     null,       null,             title,    firstname,  middleNames, surname, nino,       null, null,     null,     null,     null,     null, null, null, nationalInsuranceNumber, 1",
      "rel2case, rel2client, null,      null    , null, null, null,     null,     null,       null,             title,    firstname,  middleNames, surname, JP671245A,  <  >, null,     null,     null,     null,     null, null, null, certificateNumber, 1",
      "rel2case, rel2client, null,      null    , null, null, null,     null,     null,       null,             title,    firstname,  middleNames, surname, null,       1234, <  >,     null,     null,     null,     null, null, null, telephoneHome, 3",
      "rel2case, rel2client, null,      null    , null, null, null,     null,     null,       null,             title,    firstname,  middleNames, surname, null,       1234, 12345678, <  >,     null,     null,     null, null, null, telephoneWork, 3",
      "rel2case, rel2client, null,      null    , null, null, null,     null,     null,       null,             title,    firstname,  middleNames, surname, null,       1234, null,     12345678, <  >,     null,     null, null, null, telephoneMobile, 3",
      "rel2case, rel2client, null,      null    , null, null, null,     null,     null,       null,             title,    firstname,  middleNames, surname, null,       1234, null,     null,     12345678, <  >,     null, null, null, faxNumber, 3",
      "rel2case, rel2client, null,      null    , null, null, null,     null,     null,       null,             title,    firstname,  middleNames, surname, null,       1234, null,     null,     null,     12345678, null, 1,    2024, dobDay, 1",
      "rel2case, rel2client, null,      null    , null, null, null,     null,     null,       null,             title,    firstname,  middleNames, surname, null,       1234, null,     null,     null,     12345678, day,  1,    2024, dobDay, 1",
      "rel2case, rel2client, null,      null    , null, null, null,     null,     null,       null,             title,    firstname,  middleNames, surname, null,       1234, null,     null,     null,     12345678, 1,    null, 2024, dobMonth, 1",
      "rel2case, rel2client, null,      null    , null, null, null,     null,     null,       null,             title,    firstname,  middleNames, surname, null,       1234, null,     null,     null,     12345678, 1,    mth,  2024, dobMonth, 1",
      "rel2case, rel2client, null,      null    , null, null, null,     null,     null,       null,             title,    firstname,  middleNames, surname, null,       1234, null,     null,     null,     12345678, 1,    1,    null, dobYear, 1",
      "rel2case, rel2client, null,      null    , null, null, null,     null,     null,       null,             title,    firstname,  middleNames, surname, null,       1234, null,     null,     null,     12345678, 1,    1,    year, dobYear, 1"
  }, nullValues = "null")
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
      final String dobDay,
      final String dobMonth,
      final String dobYear,
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
    opponentFormData.setDobDay(dobDay);
    opponentFormData.setDobMonth(dobMonth);
    opponentFormData.setDobYear(dobYear);

    validator.validate(opponentFormData, errors);
    assertTrue(errors.hasErrors());
    assertEquals(errorCount, errors.getErrorCount());

    assertNotNull(errors.getFieldErrors(errorFieldName));
    assertEquals(errorCount, errors.getFieldErrors(errorFieldName).size());
  }

}