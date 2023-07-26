package uk.gov.laa.ccms.caab.controller.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.SessionAttributes;
import uk.gov.laa.ccms.caab.bean.ApplicationDetails;
import uk.gov.laa.ccms.caab.bean.ApplicationDetailsValidator;

import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_DETAILS;

@Controller
@RequiredArgsConstructor
@Slf4j
@SessionAttributes(APPLICATION_DETAILS)
public class PrivacyNoticeAgreementController {

    private final ApplicationDetailsValidator applicationValidator;

    @GetMapping("/application/agreement")
    public String privacyNoticeAgreement(@ModelAttribute(APPLICATION_DETAILS) ApplicationDetails applicationDetails){
        log.info("GET /application/agreement: {}", applicationDetails);
        return "application/privacy-notice-agreement";
    }

    @PostMapping("/application/agreement")
    public String privacyNoticeAgreement(@ModelAttribute(APPLICATION_DETAILS) ApplicationDetails applicationDetails,
                                         BindingResult bindingResult){
        log.info("POST /application/agreement: {}", applicationDetails);
        applicationValidator.validateAgreementAcceptance(applicationDetails, bindingResult);

        if (bindingResult.hasErrors()) {
            return "application/privacy-notice-agreement";
        }else {
            if (applicationDetails.getClient() != null){
                //using an existing client
                return "redirect:/application/summary";
            } else {
                //registering a new client
                return "redirect:/application/client/basic-details";
            }
        }
    }

    @GetMapping("/application/agreement/print")
    public String privacyNoticeAgreementPrintable(){
        log.info("GET /application/agreement/printable");
        return "application/privacy-notice-agreement-printable";
    }

}


