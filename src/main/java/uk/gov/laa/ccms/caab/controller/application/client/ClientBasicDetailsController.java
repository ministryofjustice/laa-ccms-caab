package uk.gov.laa.ccms.caab.controller.application.client;

import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_CONTACT_TITLE;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_GENDER;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_MARITAL_STATUS;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CLIENT_DETAILS;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CLIENT_SEARCH_CRITERIA;
import static uk.gov.laa.ccms.caab.constants.UniqueIdentifierTypeConstants.UNIQUE_IDENTIFIER_HOME_OFFICE_REFERENCE;
import static uk.gov.laa.ccms.caab.constants.UniqueIdentifierTypeConstants.UNIQUE_IDENTIFIER_NATIONAL_INSURANCE_NUMBER;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.bind.annotation.SessionAttributes;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.bean.ClientDetails;
import uk.gov.laa.ccms.caab.bean.ClientDetailsValidator;
import uk.gov.laa.ccms.caab.bean.ClientSearchCriteria;
import uk.gov.laa.ccms.caab.service.DataService;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;

/**
 * Controller for handling basic client details selection during the new application process.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
@SessionAttributes({
    CLIENT_DETAILS
})
@SuppressWarnings({"unchecked"})
public class ClientBasicDetailsController {

  private final DataService dataService;

  private final ClientDetailsValidator clientDetailsValidator;

  /**
   * Handles the GET request for client basic details page.
   *
   * @param clientSearchCriteria Search criteria for finding clients.
   * @param clientDetails The details of the client.
   * @param model The model for the view.
   * @return The view name for the client basic details page
   */
  @GetMapping("application/client/details/basic")
  public String clientDetailsBasic(
          @SessionAttribute(CLIENT_SEARCH_CRITERIA) ClientSearchCriteria clientSearchCriteria,
          @ModelAttribute(CLIENT_DETAILS) ClientDetails clientDetails,
          Model model) {
    log.info("GET /application/client/details/basic");

    populateDropdowns(model);
    populateFields(clientSearchCriteria, clientDetails, model);

    log.info("clientDetails: {}", clientDetails);

    return "application/client/basic-client-details";
  }

  /**
   * Handles the client search results submission.
   *
   * @param clientSearchCriteria Search criteria for finding clients.
   * @param clientDetails The details of the client.
   * @param bindingResult Validation result.
   * @param model The model for the view.
   * @return A redirect string to the agreement page.
   */
  @PostMapping("/application/client/details/basic")
  public String clientDetailsBasic(
          @SessionAttribute(CLIENT_SEARCH_CRITERIA) ClientSearchCriteria clientSearchCriteria,
          @ModelAttribute(CLIENT_DETAILS) ClientDetails clientDetails,
          BindingResult bindingResult,
          Model model) {
    log.info("POST /application/client/details/basic");

    clientDetailsValidator.validate(clientDetails, bindingResult);

    if (bindingResult.hasErrors()) {
      populateDropdowns(model);
      populateFields(clientSearchCriteria, clientDetails, model);

      model.addAttribute(CLIENT_DETAILS, clientDetails);
      log.info("clientDetails: {}", clientDetails);

      return "application/client/basic-client-details";
    }

    model.addAttribute(CLIENT_DETAILS, clientDetails);
    log.info("clientDetails: {}", clientDetails);

    return "redirect:/application/client/details/contact";
  }

  /**
   * Populates fields for the client basic details form.
   *
   * @param model The model for the view.
   */
  private void populateFields(
          ClientSearchCriteria clientSearchCriteria,
          ClientDetails clientDetails,
          Model model) {

    clientDetails.setFirstName(clientSearchCriteria.getForename());
    clientDetails.setSurnameAtBirth(clientSearchCriteria.getSurname());

    String dob = clientSearchCriteria.getDobDay() + "/"
            + clientSearchCriteria.getDobMonth() + "/"
            + clientSearchCriteria.getDobYear();

    clientDetails.setDateOfBirth(dob);

    clientDetails.setNationalInsuranceNumber(
            clientSearchCriteria.getUniqueIdentifier(UNIQUE_IDENTIFIER_NATIONAL_INSURANCE_NUMBER));

    clientDetails.setHomeOfficeNumber(
            clientSearchCriteria.getUniqueIdentifier(UNIQUE_IDENTIFIER_HOME_OFFICE_REFERENCE));

    String searchCriteriaGender = clientSearchCriteria.getGender();

    if (searchCriteriaGender != null && !searchCriteriaGender.isBlank()) {
      clientDetails.setGender(searchCriteriaGender);

      Optional.ofNullable((List<CommonLookupValueDetail>) model.getAttribute("genders"))
              .ifPresent(genderList -> {
                String genderDisplayValue = genderList.stream()
                        .filter(genderDetail -> searchCriteriaGender.equals(genderDetail.getCode()))
                        .map(CommonLookupValueDetail::getDescription)
                        .findFirst()
                        .orElse(null);

                model.addAttribute("genderDisplayValue", genderDisplayValue);
              });
    }
  }


  /**
   * Populates dropdown options for the client basic details form.
   *
   * @param model The model for the view.
   */
  private void populateDropdowns(Model model) {
    // Asynchronously fetch titles
    Mono<List<CommonLookupValueDetail>> titlesMono = dataService.getCommonValues(
                    COMMON_VALUE_CONTACT_TITLE)
            .map(commonLookupDetail -> Optional.ofNullable(commonLookupDetail)
                    .map(CommonLookupDetail::getContent)
                    .orElse(Collections.emptyList()));

    // Asynchronously fetch countries
    // remove any null objects
    Mono<List<CommonLookupValueDetail>> countriesMono = dataService.getCountries()
            .flatMap(commonLookupDetail -> {
              if (commonLookupDetail != null && commonLookupDetail.getContent() != null) {
                List<CommonLookupValueDetail> filteredContent = commonLookupDetail.getContent()
                        .stream()
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                return Mono.just(filteredContent);
              } else {
                return Mono.just(Collections.emptyList());
              }
            });

    Mono<List<CommonLookupValueDetail>> gendersMono = dataService.getCommonValues(
            COMMON_VALUE_GENDER)
            .map(commonLookupDetail -> Optional.ofNullable(commonLookupDetail)
                    .map(CommonLookupDetail::getContent)
                    .orElse(Collections.emptyList()));

    // Asynchronously fetch marital statuses
    Mono<List<CommonLookupValueDetail>> maritalStatusMono = dataService.getCommonValues(
                    COMMON_VALUE_MARITAL_STATUS)
            .map(commonLookupDetail -> Optional.ofNullable(commonLookupDetail)
                    .map(CommonLookupDetail::getContent)
                    .orElse(Collections.emptyList()));

    // Zip all Monos and populate the model once all results are available
    Mono.zip(titlesMono, countriesMono, gendersMono, maritalStatusMono)
            .doOnNext(tuple -> {
              model.addAttribute("titles", tuple.getT1());
              model.addAttribute("countries", tuple.getT2());
              model.addAttribute("genders", tuple.getT3());
              model.addAttribute("maritalStatusList", tuple.getT4());
            })
            .block();
  }
}
