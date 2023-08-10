package uk.gov.laa.ccms.caab.service;

public class CaabApiServiceException extends RuntimeException{
    public CaabApiServiceException(String message) {
        super(message);
    }

    public CaabApiServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
