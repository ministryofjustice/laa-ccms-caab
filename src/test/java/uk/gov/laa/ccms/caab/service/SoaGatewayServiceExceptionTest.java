package uk.gov.laa.ccms.caab.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class SoaGatewayServiceExceptionTest {

    @Test
    public void testConstructorWithMessage() {
        String expectedMessage = "Test Exception";
        SoaGatewayServiceException exception = new SoaGatewayServiceException(expectedMessage);

        assertEquals(expectedMessage, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    public void testConstructorWithMessageAndCause() {
        String expectedMessage = "Test Exception";
        Exception cause = new Exception("Cause Exception");
        SoaGatewayServiceException exception = new SoaGatewayServiceException(expectedMessage, cause);

        assertEquals(expectedMessage, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

}