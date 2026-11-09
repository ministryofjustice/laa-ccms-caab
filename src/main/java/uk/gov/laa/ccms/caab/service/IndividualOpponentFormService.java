package uk.gov.laa.ccms.caab.service;

import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_CONTACT_TITLE;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_RELATIONSHIP_TO_CLIENT;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import uk.gov.laa.ccms.caab.bean.opponent.IndividualOpponentFormData;
import uk.gov.laa.ccms.caab.builders.DropdownBuilder;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.util.DateUtils;
import uk.gov.laa.ccms.data.model.RelationshipToCaseLookupValueDetail;

/** Shared reference data and date preparation for application and award individuals. */
@Service
@RequiredArgsConstructor
public class IndividualOpponentFormService {
  private final LookupService lookupService;

  /**
   * Derives the date-of-birth requirement from the case relationship and normalises valid dates.
   *
   * @param individual the contact form to prepare in place
   * @throws CaabApplicationException if the relationship lookup produces no response
   */
  public void prepareForValidation(final IndividualOpponentFormData individual) {
    individual.setDateOfBirthMandatory(false);
    if (StringUtils.hasText(individual.getRelationshipToCase())) {
      final var relationship =
          lookupService
              .getPersonToCaseRelationship(individual.getRelationshipToCase())
              .map(
                  value ->
                      value.orElse(
                          new RelationshipToCaseLookupValueDetail()
                              .code(individual.getRelationshipToCase())
                              .description(individual.getRelationshipToCase())))
              .blockOptional()
              .orElseThrow(
                  () ->
                      new CaabApplicationException(
                          "Failed to retrieve relationship to case with code: %s"
                              .formatted(individual.getRelationshipToCase())));
      individual.setDateOfBirthMandatory(
          Boolean.TRUE.equals(relationship.getDateOfBirthMandatory()));
    }
    individual.setDateOfBirth(DateUtils.normaliseComponentDateIfValid(individual.getDateOfBirth()));
  }

  /**
   * Adds relationship, title, country and legal-aid options to a contact form's model.
   *
   * @param model the model to populate
   * @throws CaabApplicationException if the relationship list cannot be retrieved
   */
  public void populateContactDropdowns(final Model model) {
    final var relationships =
        lookupService
            .getPersonToCaseRelationships()
            .blockOptional()
            .orElseThrow(() -> new CaabApplicationException("Failed to retrieve lookup data"));
    model.addAttribute("relationshipsToCase", relationships.getContent());
    model.addAttribute("legalAidedOptions", yesNoOptions());
    new DropdownBuilder(model)
        .addDropdown("contactTitles", lookupService.getCommonValues(COMMON_VALUE_CONTACT_TITLE))
        .addDropdown(
            "relationshipsToClient",
            lookupService.getCommonValues(COMMON_VALUE_RELATIONSHIP_TO_CLIENT))
        .addDropdown("countries", lookupService.getCountries())
        .build();
  }

  /**
   * Supplies the values and labels for optional boolean dropdowns.
   *
   * @return the No and Yes options, in display order
   */
  public List<BooleanDropdownOption> yesNoOptions() {
    return List.of(
        new BooleanDropdownOption("false", "No"), new BooleanDropdownOption("true", "Yes"));
  }

  /** Boolean values used by optional yes/no form controls. */
  public record BooleanDropdownOption(String code, String description) {}
}
