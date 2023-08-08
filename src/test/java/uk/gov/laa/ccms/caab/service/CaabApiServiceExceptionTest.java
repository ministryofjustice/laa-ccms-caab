package uk.gov.laa.ccms.caab.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;


@ExtendWith(MockitoExtension.class)
public class CaabApiServiceExceptionTest {

    @Test
    public void testConstructorWithMessage() {
        String expectedMessage = "Test Exception";
        CaabApiServiceException exception = new CaabApiServiceException(expectedMessage);

        assertEquals(expectedMessage, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    public void testConstructorWithMessageAndCause() {
        String expectedMessage = "Test Exception";
        Exception cause = new Exception("Cause Exception");
        CaabApiServiceException exception = new CaabApiServiceException(expectedMessage, cause);

        assertEquals(expectedMessage, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }
}
