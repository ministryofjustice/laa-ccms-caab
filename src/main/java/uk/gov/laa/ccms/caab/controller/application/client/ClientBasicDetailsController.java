package uk.gov.laa.ccms.caab.controller.application.client;

import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_CONTACT_TITLE;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_GENDER;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_MARITAL_STATUS;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CLIENT_FLOW_FORM_DATA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CLIENT_SEARCH_CRITERIA;
import static uk.gov.laa.ccms.caab.constants.UniqueIdentifierTypeConstants.UNIQUE_IDENTIFIER_HOME_OFFICE_REFERENCE;
import static uk.gov.laa.ccms.caab.constants.UniqueIdentifierTypeConstants.UNIQUE_IDENTIFIER_NATIONAL_INSURANCE_NUMBER;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.bind.annotation.SessionAttributes;
import uk.gov.laa.ccms.caab.bean.ClientFlowFormData;
import uk.gov.laa.ccms.caab.bean.ClientFormDataBasicDetails;
import uk.gov.laa.ccms.caab.bean.ClientSearchCriteria;
import uk.gov.laa.ccms.caab.bean.validators.client.ClientBasicDetailsValidator;
import uk.gov.laa.ccms.caab.builders.DropdownBuilder;
import uk.gov.laa.ccms.caab.service.LookupService;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;

/**
 * Controller for handling basic client details selection during the new application process.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
@SessionAttributes({CLIENT_FLOW_FORM_DATA})
@SuppressWarnings({"unchecked"})
public class ClientBasicDetailsController {

  private final LookupService lookupService;

  private final ClientBasicDetailsValidator clientBasicDetailsValidator;

  @ModelAttribute("basicDetails")
  public ClientFormDataBasicDetails getBasicDetails() {
    return new ClientFormDataBasicDetails();
  }

  /**
   * Handles the GET request for client basic details page.
   *
   * @param clientSearchCriteria Search criteria for finding clients.
   * @param model The model for the view.
   * @return The view name for the client basic details page
   */
  @GetMapping("/application/client/details/basic")
  public String clientDetailsBasic(
          @SessionAttribute(CLIENT_SEARCH_CRITERIA) ClientSearchCriteria clientSearchCriteria,
          @SessionAttribute(CLIENT_FLOW_FORM_DATA) ClientFlowFormData clientFlowFormData,
          @ModelAttribute("basicDetails") ClientFormDataBasicDetails basicDetails,
          Model model) {

    populateDropdowns(model);
    basicDetails.setClientFlowFormAction(clientFlowFormData.getAction());

    if (clientFlowFormData.getBasicDetails() != null) {
      model.addAttribute("basicDetails", clientFlowFormData.getBasicDetails());
      populateFields(clientSearchCriteria, clientFlowFormData.getBasicDetails(), model);
    } else {
      populateFields(clientSearchCriteria, basicDetails, model);
    }

    return "application/client/basic-client-details";
  }

  /**
   * Handles the client search results submission.
   *
   * @param clientSearchCriteria Search criteria for finding clients.
   * @param basicDetails The basic details of the client.
   * @param bindingResult Validation result
   * @param model The model for the view.
   * @return A redirect string to the agreement page.
   */
  @PostMapping("/application/client/details/basic")
  public String clientDetailsBasic(
          @SessionAttribute(CLIENT_SEARCH_CRITERIA) ClientSearchCriteria clientSearchCriteria,
          @SessionAttribute(CLIENT_FLOW_FORM_DATA) ClientFlowFormData clientFlowFormData,
          @Validated @ModelAttribute("basicDetails") ClientFormDataBasicDetails basicDetails,
          BindingResult bindingResult,
          Model model) {

    clientBasicDetailsValidator.validate(basicDetails, bindingResult);

    if (bindingResult.hasErrors()) {
      populateDropdowns(model);
      populateFields(clientSearchCriteria, basicDetails, model);
      return "application/client/basic-client-details";
    }

    clientFlowFormData.setBasicDetails(basicDetails);
    model.addAttribute(CLIENT_FLOW_FORM_DATA, clientFlowFormData);

    return "redirect:/application/client/details/contact";
  }

  /**
   * Populates fields for the client basic details form.
   *
   * @param model The model for the view.
   */
  private void populateFields(
          ClientSearchCriteria clientSearchCriteria,
          ClientFormDataBasicDetails basicDetails,
          Model model) {

    basicDetails.setFirstName(clientSearchCriteria.getForename());
    basicDetails.setSurnameAtBirth(clientSearchCriteria.getSurname());
    basicDetails.setDateOfBirth(clientSearchCriteria.getDateOfBirth());

    String nationalInsuranceNumber =
        clientSearchCriteria.getUniqueIdentifier(UNIQUE_IDENTIFIER_NATIONAL_INSURANCE_NUMBER);

    if (StringUtils.hasText(nationalInsuranceNumber)) {
      basicDetails.setNationalInsuranceNumber(nationalInsuranceNumber.toUpperCase());
    }

    String homeOfficeNumber =
        clientSearchCriteria.getUniqueIdentifier(UNIQUE_IDENTIFIER_HOME_OFFICE_REFERENCE);

    if (StringUtils.hasText(homeOfficeNumber)) {
      basicDetails.setHomeOfficeNumber(homeOfficeNumber.toUpperCase());
    }

    String searchCriteriaGender = clientSearchCriteria.getGender();

    if (searchCriteriaGender != null && !searchCriteriaGender.isBlank()) {
      basicDetails.setGender(searchCriteriaGender);

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
    new DropdownBuilder(model)
        .addDropdown("titles",
            lookupService.getCommonValues(COMMON_VALUE_CONTACT_TITLE))
        .addDropdown("countries",
            lookupService.getCountries())
        .addDropdown("genders",
            lookupService.getCommonValues(COMMON_VALUE_GENDER))
        .addDropdown("maritalStatusList",
            lookupService.getCommonValues(COMMON_VALUE_MARITAL_STATUS))
        .build();
  }
}
