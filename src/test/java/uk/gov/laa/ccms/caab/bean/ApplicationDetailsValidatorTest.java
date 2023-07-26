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

@ExtendWith(SpringExtension.class)
public class ApplicationDetailsValidatorTest {

    @InjectMocks
    private ApplicationDetailsValidator validator;

    private ApplicationDetails applicationDetails;

    private Errors errors;

    @BeforeEach
    public void setUp() {
        applicationDetails = new ApplicationDetails(); // Assuming that the default constructor sets all fields to null.
        errors = new BeanPropertyBindingResult(applicationDetails, "applicationDetails");
    }

    @Test
    public void supports_ReturnsTrueForApplicationDetailsClass() {
        assertTrue(validator.supports(ApplicationDetails.class));
    }

    @Test
    public void supports_ReturnsFalseForOtherClasses() {
        assertFalse(validator.supports(Object.class));
    }

    @Test
    public void validate_ValidatesOfficeId() {
        validator.validate(applicationDetails, errors);
        assertTrue(errors.hasErrors());
        assertNotNull(errors.getFieldError("officeId"));
        assertEquals("required.officeId", errors.getFieldError("officeId").getCode());
    }

    @Test
    public void validate_ValidatesCategoryOfLawId() {
        validator.validate(applicationDetails, errors);
        assertTrue(errors.hasErrors());
        assertNotNull(errors.getFieldError("categoryOfLawId"));
        assertEquals("required.categoryOfLawId", errors.getFieldError("categoryOfLawId").getCode());
    }

    @Test
    public void validate_ValidatesApplicationTypeCategory() {
        validator.validate(applicationDetails, errors);
        assertTrue(errors.hasErrors());
        assertNotNull(errors.getFieldError("applicationTypeCategory"));
        assertEquals("required.applicationTypeCategory", errors.getFieldError("applicationTypeCategory").getCode());
    }

    @Test
    public void validate_ValidatesDelegatedFunction_WhenDelegatedFunctionsIsNo() {
        applicationDetails.setDelegatedFunctions(false);
        validator.validateDelegatedFunction(applicationDetails, errors);
        assertFalse(errors.hasErrors());
    }

    @ParameterizedTest
    @CsvSource({"ab, 1, 2000, delegatedFunctionUsedDay",
                "1, ab, 2000, delegatedFunctionUsedMonth",
                "1, 1, abcd, delegatedFunctionUsedYear"})
    public void validate_ValidatesDelegatedFunction_WhenDelegatedFunctionsIsYesAndInvalidDate(String day, String month, String year, String field) {
        applicationDetails.setDelegatedFunctions(true);
        applicationDetails.setDelegatedFunctionUsedDay(day);
        applicationDetails.setDelegatedFunctionUsedMonth(month);
        applicationDetails.setDelegatedFunctionUsedYear(year);

        validator.validateDelegatedFunction(applicationDetails, errors);
        assertTrue(errors.hasErrors());
        assertEquals("invalid.numeric", errors.getFieldError(field).getCode());
    }

    @Test
    public void validate_ValidatesAgreementAcceptance_NotAccepted() {
        // Arrange
        applicationDetails.setAgreementAccepted(false); // Set agreement acceptance to false
        validator.validateAgreementAcceptance(applicationDetails, errors);

        // Assert
        assertTrue(errors.hasErrors());
        assertNotNull(errors.getFieldError("agreementAccepted"));
        assertEquals("agreement.not.accepted", errors.getFieldError("agreementAccepted").getCode());
        assertEquals("Please complete 'I confirm my client (or their representative) has read and agreed to the Privacy Notice'.",
                errors.getFieldError("agreementAccepted").getDefaultMessage());
    }

    @Test
    public void validate_ValidatesAgreementAcceptance_Accepted() {
        // Arrange
        applicationDetails.setAgreementAccepted(true); // Set agreement acceptance to true
        validator.validateAgreementAcceptance(applicationDetails, errors);

        // Assert
        assertFalse(errors.hasErrors());
    }



}