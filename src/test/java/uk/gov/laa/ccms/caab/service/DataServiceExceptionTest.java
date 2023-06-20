package uk.gov.laa.ccms.caab.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;


@ExtendWith(MockitoExtension.class)
public class DataServiceExceptionTest {

    @Test
    public void testConstructorWithMessage() {
        String expectedMessage = "Test Exception";
        DataServiceException exception = new DataServiceException(expectedMessage);

        assertEquals(expectedMessage, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    public void testConstructorWithMessageAndCause() {
        String expectedMessage = "Test Exception";
        Exception cause = new Exception("Cause Exception");
        DataServiceException exception = new DataServiceException(expectedMessage, cause);

        assertEquals(expectedMessage, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }
}
