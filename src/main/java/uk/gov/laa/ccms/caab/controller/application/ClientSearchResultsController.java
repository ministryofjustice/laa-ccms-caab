package uk.gov.laa.ccms.caab.controller.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.SessionAttributes;
import uk.gov.laa.ccms.caab.bean.ClientSearchDetails;

@Controller
@RequiredArgsConstructor
@Slf4j
@SessionAttributes("applicationDetails")
public class ClientSearchResultsController {

    @GetMapping("/application/client-search/results")
    public String clientSearchResults(@ModelAttribute("clientSearchDetails") ClientSearchDetails clientSearchDetails,
                                      Model model) {
        log.info("GET /application/client-search/results");

        return "/application/client-search-results";
    }
}

