package uk.gov.laa.ccms.caab.controller.application.client;

import static uk.gov.laa.ccms.caab.constants.SessionConstants.CLIENT_DETAILS;

import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.SessionAttributes;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.bean.ClientDetails;
import uk.gov.laa.ccms.caab.bean.validators.client.ClientAddressDetailsValidator;
import uk.gov.laa.ccms.caab.bean.validators.client.ClientBasicDetailsValidator;
import uk.gov.laa.ccms.caab.bean.validators.client.ClientContactDetailsValidator;
import uk.gov.laa.ccms.caab.bean.validators.client.ClientEqualOpportunitiesMonitoringDetailsValidator;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.service.CommonLookupService;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;

/**
 * Controller for handling client summary details during the new application process.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
@SessionAttributes({
    CLIENT_DETAILS
})
public class ClientSummaryController {

  private final CommonLookupService commonLookupService;

  private final ClientBasicDetailsValidator basicValidator;

  private final ClientContactDetailsValidator contactValidator;

  private final ClientAddressDetailsValidator addressValidator;

  private final ClientEqualOpportunitiesMonitoringDetailsValidator opportunitiesValidator;

  /**
   * Handles the GET request for the client summary page.
   *
   * @return The view name for the client summary details
   */
  @GetMapping("application/client/details/summary")
  public String clientDetailsSummary(
      @ModelAttribute(CLIENT_DETAILS) ClientDetails clientDetails,
      Model model) {
    log.info("GET /application/client/details/summary");

    populateSummaryListLookups(clientDetails, model);

    return "application/client/client-summary-details";
  }

  /**
   * Handles the POST request for the client summary page.
   *
   * @return The view name for the client summary details
   */
  @PostMapping("application/client/details/summary")
  public String clientDetailsSummary(
      @ModelAttribute(CLIENT_DETAILS) ClientDetails clientDetails,
      BindingResult bindingResult) {
    log.info("POST /application/client/details/summary");

    basicValidator.validate(clientDetails, bindingResult);
    contactValidator.validate(clientDetails, bindingResult);
    addressValidator.validate(clientDetails, bindingResult);
    opportunitiesValidator.validate(clientDetails, bindingResult);

    if (bindingResult.hasErrors()) {
      throw new CaabApplicationException(
          "Client submission containing missing or invalid client details.");
    }

    return "redirect:/submissions/client-create";
  }

  private void populateSummaryListLookups(ClientDetails clientDetails, Model model) {

    //handle separately due to optionality
    Mono<CommonLookupValueDetail> correspondenceLanguageMono =
        StringUtils.hasText(clientDetails.getCorrespondenceLanguage())
        ? commonLookupService.getCorrespondenceLanguage(clientDetails.getCorrespondenceLanguage())
        : Mono.just(new CommonLookupValueDetail());

    // Create a list of Mono calls and their respective attribute keys
    List<Pair<String, Mono<CommonLookupValueDetail>>> lookups = List.of(
        Pair.of("contactTitle",
            commonLookupService.getContactTitle(clientDetails.getTitle())),
        Pair.of("countryOfOrigin",
            commonLookupService.getCountry(clientDetails.getCountryOfOrigin())),
        Pair.of("maritalStatus",
            commonLookupService.getMaritalStatus(clientDetails.getMaritalStatus())),
        Pair.of("gender",
            commonLookupService.getGender(clientDetails.getGender())),
        Pair.of("correspondenceMethod",
            commonLookupService.getCorrespondenceMethod(clientDetails.getCorrespondenceMethod())),
        Pair.of("country",
            commonLookupService.getCountry(clientDetails.getCountry())),
        Pair.of("ethnicity",
            commonLookupService.getEthnicOrigin(clientDetails.getEthnicOrigin())),
        Pair.of("disability",
            commonLookupService.getDisability(clientDetails.getDisability())),
        Pair.of("correspondenceLanguage",
            correspondenceLanguageMono)
    );

    // Fetch all Monos asynchronously
    Mono<List<CommonLookupValueDetail>> allMonos = Flux.fromIterable(lookups)
        .flatMap(pair -> pair.getRight().map(value -> Pair.of(pair.getLeft(), value)))
        .collectList()
        .doOnNext(list -> list.forEach(pair -> model.addAttribute(pair.getLeft(), pair.getRight())))
        .map(list -> list.stream().map(Pair::getRight).collect(Collectors.toList()));

    // Block until all results are available
    allMonos.block();
  }
}
