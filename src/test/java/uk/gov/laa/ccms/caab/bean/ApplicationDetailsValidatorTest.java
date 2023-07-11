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
    public void validate_ValidatesApplicationTypeId() {
        validator.validate(applicationDetails, errors);
        assertTrue(errors.hasErrors());
        assertNotNull(errors.getFieldError("applicationTypeId"));
        assertEquals("required.applicationTypeId", errors.getFieldError("applicationTypeId").getCode());
    }

    @Test
    public void validate_ValidatesDelegatedFunction() {
        validator.validateDelegatedFunction(applicationDetails, errors);
        assertTrue(errors.hasErrors());
        assertNotNull(errors.getFieldError("delegatedFunctionsOption"));
        assertEquals("required.delegatedFunctionsOption", errors.getFieldError("delegatedFunctionsOption").getCode());
    }

    @Test
    public void validate_ValidatesDelegatedFunction_WhenDelegatedFunctionsOptionIsNo() {
        applicationDetails.setDelegatedFunctionsOption("N");
        validator.validateDelegatedFunction(applicationDetails, errors);
        assertFalse(errors.hasErrors());
    }

    @ParameterizedTest
    @CsvSource({"ab, 1, 2000, delegatedFunctionUsedDay",
                "1, ab, 2000, delegatedFunctionUsedMonth",
                "1, 1, abcd, delegatedFunctionUsedYear"})
    public void validate_ValidatesDelegatedFunction_WhenDelegatedFunctionsOptionIsYesAndInvalidDate(String day, String month, String year, String field) {
        applicationDetails.setDelegatedFunctionsOption("Y");
        applicationDetails.setDelegatedFunctionUsedDay(day);
        applicationDetails.setDelegatedFunctionUsedMonth(month);
        applicationDetails.setDelegatedFunctionUsedYear(year);

        validator.validateDelegatedFunction(applicationDetails, errors);
        assertTrue(errors.hasErrors());
        assertEquals("invalid.numeric", errors.getFieldError(field).getCode());
    }


}