package uk.gov.laa.ccms.caab.advice;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import uk.gov.laa.ccms.caab.service.DataServiceException;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(value = {DataServiceException.class})
    public String handleDataServiceException(DataServiceException e, Model model) {
        // This exception is thrown when there's a low-level, resource-specific error, such as an I/O error
        // Log the error details
        log.error("DataServiceException caught by GlobalExceptionHandler", e);
        // return an appropriate response
        model.addAttribute("error", e.getLocalizedMessage());

        return "error";
    }
}
