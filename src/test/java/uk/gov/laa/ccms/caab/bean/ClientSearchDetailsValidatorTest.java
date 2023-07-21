package uk.gov.laa.ccms.caab.bean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

import static org.junit.jupiter.api.Assertions.*;
import static uk.gov.laa.ccms.caab.constants.UniqueIdentifierTypeConstants.*;

@ExtendWith(SpringExtension.class)
public class ClientSearchDetailsValidatorTest {

    @InjectMocks
    private ClientSearchDetailsValidator validator;
    private ClientSearchDetails clientSearchDetails;
    private Errors errors;

    @BeforeEach
    public void setUp() {
        clientSearchDetails = new ClientSearchDetails();
        errors = new BeanPropertyBindingResult(clientSearchDetails, "clientSearchDetails");
    }

    @Test
    public void supports_ReturnsTrueForClientSearchDetailsClass() {
        assertTrue(validator.supports(ClientSearchDetails.class));
    }

    @Test
    public void testValidateForename_Valid() {
        clientSearchDetails.setForename("John");
        validator.validateForename(clientSearchDetails, errors);
        assertFalse(errors.hasErrors());
    }

    @Test
    public void testValidateForename_Invalid() {
        validator.validateForename(clientSearchDetails, errors);
        assertTrue(errors.hasErrors());
        assertNotNull(errors.getFieldError("forename"));
        assertEquals("required.forename", errors.getFieldError("forename").getCode());
    }

    @Test
    public void testValidateSurnameAtBirth_Valid() {
        clientSearchDetails.setSurname("Doe");
        validator.validateSurnameAtBirth(clientSearchDetails, errors);
        assertFalse(errors.hasErrors());
    }

    @Test
    public void testValidateSurnameAtBirth_Invalid() {
        validator.validateSurnameAtBirth(clientSearchDetails, errors);
        assertTrue(errors.hasErrors());
        assertNotNull(errors.getFieldError("surname"));
        assertEquals("required.surname", errors.getFieldError("surname").getCode());
    }

    @Test
    public void testValidateDateOfBirth_Valid() {
        clientSearchDetails.setDobDay("01");
        clientSearchDetails.setDobMonth("12");
        clientSearchDetails.setDobYear("1990");
        validator.validateDateOfBirth(clientSearchDetails, errors);
        assertFalse(errors.hasErrors());
    }

    @Test
    public void testValidateDateOfBirth_Invalid() {
        clientSearchDetails.setDobDay("");
        clientSearchDetails.setDobMonth("");
        clientSearchDetails.setDobYear("");
        validator.validateDateOfBirth(clientSearchDetails, errors);
        assertTrue(errors.hasErrors());
        assertNotNull(errors.getFieldError("dobDay"));
        assertEquals("required.dob-day", errors.getFieldError("dobDay").getCode());
        assertNotNull(errors.getFieldError("dobMonth"));
        assertEquals("required.dob-month", errors.getFieldError("dobMonth").getCode());
        assertNotNull(errors.getFieldError("dobYear"));
        assertEquals("required.dob-year", errors.getFieldError("dobYear").getCode());
    }

    @ParameterizedTest
    @CsvSource({
            "abc, 12, 1990, dobDay",
            "1, ab, 1990, dobMonth",
            "1, 12, abcd, dobYear"
    })
    public void testValidateDateOfBirth_InvalidNumeric(String dobDay, String dobMonth, String dobYear, String field) {
        clientSearchDetails.setDobDay(dobDay);
        clientSearchDetails.setDobMonth(dobMonth);
        clientSearchDetails.setDobYear(dobYear);
        validator.validateDateOfBirth(clientSearchDetails, errors);
        assertTrue(errors.hasErrors());
        assertNotNull(errors.getFieldError(field));
        assertEquals("invalid.numeric", errors.getFieldError(field).getCode());
    }

    @ParameterizedTest
    @CsvSource({"1, AB123456C",
                "2, TEST",
                "3, TEST"})
    public void testValidateUniqueIdentifierType_Valid(Integer uniqueIdentifierType, String uniqueIdentifierValue) {
        clientSearchDetails.setUniqueIdentifierType(uniqueIdentifierType);
        clientSearchDetails.setUniqueIdentifierValue(uniqueIdentifierValue);
        validator.validateUniqueIdentifierType(clientSearchDetails, errors);
        assertFalse(errors.hasErrors());
    }

    @Test
    public void testValidateUniqueIdentifierType_InvalidNationalInsuranceNumber() {
        clientSearchDetails.setUniqueIdentifierType(UNIQUE_IDENTIFIER_NATIONAL_INSURANCE_NUMBER);
        clientSearchDetails.setUniqueIdentifierValue("ABC123");
        validator.validateUniqueIdentifierType(clientSearchDetails, errors);
        assertTrue(errors.hasErrors());
        assertNotNull(errors.getFieldError("uniqueIdentifierValue"));
        assertEquals("invalid.uniqueIdentifierValue", errors.getFieldError("uniqueIdentifierValue").getCode());
    }

    @ParameterizedTest
    @CsvSource({"----"})
    public void testValidateUniqueIdentifierType_InvalidHomeOfficeReference(String uniqueIdentifierValue) {
        clientSearchDetails.setUniqueIdentifierType(UNIQUE_IDENTIFIER_HOME_OFFICE_REFERENCE);
        clientSearchDetails.setUniqueIdentifierValue(uniqueIdentifierValue);
        validator.validateUniqueIdentifierType(clientSearchDetails, errors);
        assertTrue(errors.hasErrors());
        assertNotNull(errors.getFieldError("uniqueIdentifierValue"));
        assertEquals("invalid.uniqueIdentifierValue", errors.getFieldError("uniqueIdentifierValue").getCode());
    }

    @ParameterizedTest
    @CsvSource({"TEST  TEST",
                "----"})
    public void testValidateUniqueIdentifierType_InvalidCaseReferenceNumber(String uniqueIdentifierValue) {
        clientSearchDetails.setUniqueIdentifierType(UNIQUE_IDENTIFIER_CASE_REFERENCE_NUMBER);
        clientSearchDetails.setUniqueIdentifierValue(uniqueIdentifierValue);
        validator.validateUniqueIdentifierType(clientSearchDetails, errors);
        assertTrue(errors.hasErrors());
        assertNotNull(errors.getFieldError("uniqueIdentifierValue"));
        assertEquals("invalid.uniqueIdentifierValue", errors.getFieldError("uniqueIdentifierValue").getCode());
    }

    @Test
    public void testValidate_Valid() {
        clientSearchDetails.setForename("John");
        clientSearchDetails.setSurname("Doe");
        clientSearchDetails.setDobDay("01");
        clientSearchDetails.setDobMonth("12");
        clientSearchDetails.setDobYear("1990");
        clientSearchDetails.setUniqueIdentifierType(1);
        clientSearchDetails.setUniqueIdentifierValue("AB123456C");
        validator.validate(clientSearchDetails, errors);
        assertFalse(errors.hasErrors());
    }

    @ParameterizedTest
    @CsvSource({
            "'','','','','', 5",
            "'',Doe,'',12,1990,2",
            "John,'','',12,1990,2",
            "John,Doe,'','',1990,2",
            "John,Doe,1,'',1990,1",
            "John,Doe,1,12,'',1",
    })
    public void testValidate_Invalid(String forename, String surname, String dobDay,
                                     String dobMonth, String dobYear, int expectedErrorCount) {
        clientSearchDetails.setForename(forename);
        clientSearchDetails.setSurname(surname);
        clientSearchDetails.setDobDay(dobDay);
        clientSearchDetails.setDobMonth(dobMonth);
        clientSearchDetails.setDobYear(dobYear);

        validator.validate(clientSearchDetails, errors);

        assertTrue(errors.hasErrors());
        assertEquals(expectedErrorCount, errors.getErrorCount());
    }
}
