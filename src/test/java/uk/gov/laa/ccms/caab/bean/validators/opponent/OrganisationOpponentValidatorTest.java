package uk.gov.laa.ccms.caab.bean.validators.opponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CURRENT_OPPONENT;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.opponent.OrganisationOpponentFormData;

@ExtendWith(SpringExtension.class)
class OrganisationOpponentValidatorTest {

  @InjectMocks
  private OrganisationOpponentValidator validator;

  private OrganisationOpponentFormData opponentFormData;

  private Errors errors;

  @BeforeEach
  public void setUp() {
    opponentFormData =
        new OrganisationOpponentFormData();
    errors = new BeanPropertyBindingResult(opponentFormData, CURRENT_OPPONENT);
  }

  @Test
  public void supports_ReturnsTrueForCorrectClass() {
    assertTrue(validator.supports(OrganisationOpponentFormData.class));
  }

  @Test
  public void supports_ReturnsFalseForOtherClasses() {
    assertFalse(validator.supports(Object.class));
  }

  @Test
  public void validate_sharedOrg_noErrors() {
    opponentFormData.setShared(true);
    opponentFormData.setRelationshipToCase("rel2case");
    opponentFormData.setRelationshipToClient("rel2client");

    validator.validate(opponentFormData, errors);
    assertFalse(errors.hasErrors());
  }

  @Test
  public void validate_sharedOrg_allErrors() {
    opponentFormData.setOtherInformation(StringUtils.repeat("a", 2001));

    validator.validate(opponentFormData, errors);
    assertTrue(errors.hasErrors());

    assertNotNull(errors.getFieldError("relationshipToCase"));
    assertEquals("required.relationshipToCase", errors.getFieldError("relationshipToCase").getCode());

    assertNotNull(errors.getFieldError("relationshipToClient"));
    assertEquals("required.relationshipToCase", errors.getFieldError("relationshipToCase").getCode());

    assertNotNull(errors.getFieldError("otherInformation"));
    assertEquals("length.exceeds.max", errors.getFieldError("otherInformation").getCode());
  }

  @Test
  public void validate_nonSharedOrg_noErrors() {
    opponentFormData.setShared(false);
    opponentFormData.setRelationshipToCase("rel2case");
    opponentFormData.setRelationshipToClient("rel2client");

    opponentFormData.setOrganisationName("orgname");
    opponentFormData.setOrganisationType("orgtype");

    validator.validate(opponentFormData, errors);
    assertFalse(errors.hasErrors());
  }

  @Test
  public void validate_nonSharedOrg_telephoneAndFaxValid() {
    opponentFormData.setShared(false);
    opponentFormData.setRelationshipToCase("rel2case");
    opponentFormData.setRelationshipToClient("rel2client");
    opponentFormData.setOrganisationName("orgname");
    opponentFormData.setOrganisationType("orgtype");

    opponentFormData.setTelephoneWork("12345678");
    opponentFormData.setFaxNumber("87654321");

    validator.validate(opponentFormData, errors);
    assertFalse(errors.hasErrors());
  }

  @Test
  public void validate_nonSharedOrg_telephoneAndFaxInvalidFormat() {
    opponentFormData.setShared(false);
    opponentFormData.setRelationshipToCase("rel2case");
    opponentFormData.setRelationshipToClient("rel2client");
    opponentFormData.setOrganisationName("orgname");
    opponentFormData.setOrganisationType("orgtype");

    opponentFormData.setTelephoneWork("n  ");
    opponentFormData.setFaxNumber("n  ");

    validator.validate(opponentFormData, errors);
    assertTrue(errors.hasErrors());
    assertEquals(6, errors.getErrorCount());

    // Should be 3 errors on telephoneWork. Minlength, format and doublespaces.
    assertEquals(3, errors.getFieldErrors("telephoneWork").size());
    assertEquals("length.below.min", errors.getFieldErrors("telephoneWork").get(0).getCode());
    assertEquals("invalid.format", errors.getFieldErrors("telephoneWork").get(1).getCode());
    assertEquals("double.spaces", errors.getFieldErrors("telephoneWork").get(2).getCode());

    // Should be 3 errors on telephoneWork. Minlength, format and doublespaces.
    assertEquals(3, errors.getFieldErrors("faxNumber").size());
    assertEquals("length.below.min", errors.getFieldErrors("faxNumber").get(0).getCode());
    assertEquals("invalid.format", errors.getFieldErrors("faxNumber").get(1).getCode());
    assertEquals("double.spaces", errors.getFieldErrors("faxNumber").get(2).getCode());
  }

  @Test
  public void validate_nonSharedOrg_otherInformation() {
    opponentFormData.setShared(false);
    opponentFormData.setRelationshipToCase("rel2case");
    opponentFormData.setRelationshipToClient("rel2client");

    opponentFormData.setOrganisationName("orgname");
    opponentFormData.setOrganisationType("orgtype");

    opponentFormData.setOtherInformation(StringUtils.repeat("<", 2001));

    validator.validate(opponentFormData, errors);
    // Should be two errors on otherInformation. Length and format.
    assertEquals(2, errors.getFieldErrors("otherInformation").size());
    assertEquals("invalid.format", errors.getFieldErrors("otherInformation").get(0).getCode());
    assertEquals("length.exceeds.max", errors.getFieldErrors("otherInformation").get(1).getCode());
  }

  @ParameterizedTest
  @CsvSource(value = {
      "null, rel2client, orgname, orgtype, null, null, null, null, null, null, null, null, null, relationshipToCase, required.relationshipToCase",
      "rel2case, null, orgname, orgtype, null, null, null, null, null, null, null, null, null, relationshipToClient, required.relationshipToClient",
      "rel2case, rel2client, null, orgtype, null, null, null, null, null, null, null, null, null,organisationName, required.organisationName",
      "rel2case, rel2client, orgname, null, null, null, null, null, null, null, null, null, null, organisationType, required.organisationType",
      "rel2case, rel2client, orgname, orgtype, <, addline1, null, null, null, country, null, null, null,houseNameOrNumber, invalid.format",
      "rel2case, rel2client, orgname, orgtype, housenum, null, null, null, null, country, null, null, null, addressLine1, required.addressLine1",
      "rel2case, rel2client, orgname, orgtype, null, addline1, null, null, null, null, null, null, null, country, required.country",
      "rel2case, rel2client, orgname, orgtype, null, addline1, null, null, null, country, <postcode, null, null, postcode, invalid.format",
      "rel2case, rel2client, orgname, orgtype, null, addline1, null, null, null, country, SE1 1PP, <contact, email@email.com, contactNameRole, invalid.format",
      "rel2case, rel2client, orgname, orgtype, null, addline1, null, null, null, country, null, contact name, email, emailAddress, invalid.format"
  }, nullValues = "null")
  public void validate_nonSharedOrg_relationshipToCase(
      final String relationshipToCase,
      final String relationshipToClient,
      final String organisationName,
      final String organisationType,
      final String houseNameOrNumber,
      final String addressLine1,
      final String addressLine2,
      final String city,
      final String county,
      final String country,
      final String postcode,
      final String contactNameRole,
      final String emailAddress,
      final String errorFieldName,
      final String errorCode) {
    opponentFormData.setShared(false);
    opponentFormData.setRelationshipToCase(relationshipToCase);
    opponentFormData.setRelationshipToClient(relationshipToClient);

    opponentFormData.setOrganisationName(organisationName);
    opponentFormData.setOrganisationType(organisationType);

    opponentFormData.setHouseNameOrNumber(houseNameOrNumber);
    opponentFormData.setAddressLine1(addressLine1);
    opponentFormData.setAddressLine2(addressLine2);
    opponentFormData.setCity(city);
    opponentFormData.setCounty(county);
    opponentFormData.setCountry(country);
    opponentFormData.setPostcode(postcode);
    opponentFormData.setContactNameRole(contactNameRole);
    opponentFormData.setEmailAddress(emailAddress);

    validator.validate(opponentFormData, errors);
    assertTrue(errors.hasErrors());
    assertEquals(1, errors.getErrorCount());

    assertNotNull(errors.getFieldError(errorFieldName));
    assertEquals(errorCode, errors.getFieldError(errorFieldName).getCode());
  }

}