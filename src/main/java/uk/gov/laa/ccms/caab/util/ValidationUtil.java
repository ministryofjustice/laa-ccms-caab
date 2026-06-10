package uk.gov.laa.ccms.caab.util;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.validation.Validator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetail;
import uk.gov.laa.ccms.caab.bean.AddressFormData;
import uk.gov.laa.ccms.caab.bean.ApplicationFormData;
import uk.gov.laa.ccms.caab.bean.opponent.AbstractOpponentFormData;
import uk.gov.laa.ccms.caab.bean.opponent.IndividualOpponentFormData;
import uk.gov.laa.ccms.caab.bean.opponent.OrganisationOpponentFormData;
import uk.gov.laa.ccms.caab.bean.priorauthority.PriorAuthorityFlowFormData;
import uk.gov.laa.ccms.caab.bean.validators.application.ProviderDetailsValidator;
import uk.gov.laa.ccms.caab.bean.validators.client.CorrespondenceAddressValidator;
import uk.gov.laa.ccms.caab.bean.validators.opponent.IndividualOpponentValidator;
import uk.gov.laa.ccms.caab.bean.validators.opponent.OrganisationOpponentValidator;
import uk.gov.laa.ccms.caab.bean.validators.priorauthority.PriorAuthorityDetailsValidator;
import uk.gov.laa.ccms.caab.bean.validators.priorauthority.PriorAuthorityTypeDetailsValidator;
import uk.gov.laa.ccms.caab.bean.validators.proceedings.ProceedingDetailsValidator;
import uk.gov.laa.ccms.caab.bean.validators.proceedings.ProceedingFurtherDetailsValidator;
import uk.gov.laa.ccms.caab.bean.validators.proceedings.ProceedingMatterTypeDetailsValidator;
import uk.gov.laa.ccms.caab.constants.assessment.AssessmentName;
import uk.gov.laa.ccms.caab.mapper.ProceedingAndCostsMapper;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.PriorAuthorityDetail;
import uk.gov.laa.ccms.caab.model.ProceedingDetail;
import uk.gov.laa.ccms.caab.model.StringDisplayValue;
import uk.gov.laa.ccms.caab.service.ApplicationService;
import uk.gov.laa.ccms.caab.service.AssessmentService;
import uk.gov.laa.ccms.caab.service.LookupService;
import uk.gov.laa.ccms.data.model.UserDetail;

@Component
@RequiredArgsConstructor
@Slf4j
public class ValidationUtil {

  private final ApplicationService applicationService;
  private final AssessmentService assessmentService;
  private final ProceedingAndCostsMapper proceedingAndCostsMapper;
  private final LookupService lookupService;

  private final ProviderDetailsValidator providerDetailsValidator;
  private final CorrespondenceAddressValidator correspondenceAddressValidator;
  public final ProceedingMatterTypeDetailsValidator matterTypeValidator;
  private final ProceedingDetailsValidator proceedingTypeValidator;
  public final ProceedingFurtherDetailsValidator furtherDetailsValidator;
  private final PriorAuthorityTypeDetailsValidator priorAuthorityTypeValidator;
  private final PriorAuthorityDetailsValidator priorAuthorityDetailsValidator;
  private final OrganisationOpponentValidator organisationOpponentValidator;
  private final IndividualOpponentValidator individualOpponentValidator;

  public Mono<Boolean> validateForAmendment(String applicationId, UserDetail user, Model model) {
    return Mono.zip(
            applicationService.getMonoProviderDetailsFormData(applicationId),
            applicationService.getMonoCorrespondenceAddressFormData(applicationId),
            applicationService.getApplication(applicationId),
            Mono.fromCallable(() -> applicationService.getOpponents(applicationId))
                .subscribeOn(Schedulers.boundedElastic()))
        .flatMap(
            tuple -> {
              ApplicationFormData providerDetails = tuple.getT1();
              AddressFormData generalDetails = tuple.getT2();
              ApplicationDetail amendment = tuple.getT3();
              List<AbstractOpponentFormData> opponents = tuple.getT4();

              boolean hasFormErrors =
                  processValidations(providerDetails, generalDetails, amendment, opponents, model);

              boolean meritsReassessmentRequired =
                  checkMeritsReassessmentRequired(amendment, user, model);

              boolean hasErrors = hasFormErrors || meritsReassessmentRequired;

              return Mono.just(hasErrors);
            });
  }

  /** */
  private boolean checkMeritsReassessmentRequired(
      ApplicationDetail amendment, UserDetail user, Model model) {
    final AssessmentDetail meritsAssessment =
        assessmentService
            .getAssessments(
                List.of(AssessmentName.MERITS.getName()),
                user.getProvider().getId().toString(),
                amendment.getCaseReferenceNumber())
            .map(details -> AssessmentUtil.getMostRecentAssessmentDetail(details.getContent()))
            .blockOptional()
            .orElse(null);

    boolean reassessmentRequired =
        assessmentService.isMeritsReassessmentRequiredForAmendment(
            amendment, meritsAssessment, user);

    if (reassessmentRequired) {
      model.addAttribute(
          "meritsReassessmentErrors",
          List.of("Merits reassessment is required before this amendment can be submitted."));
      return true;
    }

    return false;
  }

  /**
   * Processes validation for multiple sections of an amendment.
   *
   * @param providerDetails form data and provider section
   * @param generalDetails general details
   * @param amendment full amendment details
   * @param opponents list of opponents to validate
   * @param model the model to attach error messages and form objects
   * @return
   */
  public boolean processValidations(
      ApplicationFormData providerDetails,
      AddressFormData generalDetails,
      ApplicationDetail amendment,
      List<AbstractOpponentFormData> opponents,
      Model model) {
    boolean hasErrors = false;

    if (validateAndAddErrors(
        providerDetails, providerDetailsValidator, model, "providerDetailsErrors")) {
      model.addAttribute("providerDetailsFormData", providerDetails);
      hasErrors = true;
    }
    if (validateAndAddErrors(
        generalDetails, correspondenceAddressValidator, model, "generalDetailsErrors")) {
      model.addAttribute("generalDetailsFormData", generalDetails);
      hasErrors = true;
    }
    hasErrors |= validateOpponents(opponents, model);
    hasErrors |= validatePriorAuthorities(amendment.getPriorAuthorities(), model);

    return hasErrors;
  }

  /**
   * Validates the form data and adds any validation errors to the model.
   *
   * @param formData the form data object to validate
   * @param validator the validator used to perform validation
   * @param model the model to add any validation errors to
   * @param errorAttribute the model attribute key under which the errors will be added
   * @return {@code true} if there are validation errors, {@code false} otherwise
   */
  protected boolean validateAndAddErrors(
      final Object formData,
      final Validator validator,
      final Model model,
      final String errorAttribute) {
    final BindingResult bindingResult =
        new BeanPropertyBindingResult(formData, formData.getClass().getSimpleName());
    validator.validate(formData, bindingResult);

    if (bindingResult.hasErrors()) {
      final List<String> errors =
          bindingResult.getAllErrors().stream().map(ObjectError::getDefaultMessage).toList();
      model.addAttribute(errorAttribute, errors);
      return true;
    }
    return false;
  }

  /**
   * Validates a list of proceedings and adds any errors to the model.
   *
   * @param proceedings the list of proceedings to validate
   * @param model the model to add any validation errors to
   * @return {@code true} if there are validation errors, {@code false} otherwise
   */
  public Mono<Boolean> validateProceedings(
      final List<ProceedingDetail> proceedings, final Model model) {
    if (proceedings == null || proceedings.isEmpty()) {
      return Mono.just(false);
    }

    final Set<String> proceedingsErrors = new HashSet<>();

    return Flux.fromIterable(proceedings)
        .flatMap(
            proceeding ->
                Optional.ofNullable(proceeding.getTypeOfOrder())
                    .map(StringDisplayValue::getId)
                    .map(lookupService::getOrderTypeDescription)
                    .orElse(Mono.empty())
                    .map(
                        orderTypeDisplayValue ->
                            proceedingAndCostsMapper.toProceedingFlow(
                                proceeding, orderTypeDisplayValue))
                    .flatMap(
                        proceedingDetailsFormData ->
                            Mono.just(
                                    validateAndAddErrors(
                                        proceedingDetailsFormData.getMatterTypeDetails(),
                                        matterTypeValidator,
                                        model,
                                        "proceedingMatterTypeDetails"))
                                .flatMap(
                                    isError -> {
                                      if (isError) {
                                        proceedingsErrors.addAll(
                                            getErrorsFromModel(
                                                model, "proceedingMatterTypeDetails"));
                                      }
                                      return Mono.just(isError);
                                    })
                                .flatMap(
                                    isError -> {
                                      if (!isError) {
                                        return Mono.just(
                                            validateAndAddErrors(
                                                proceedingDetailsFormData.getProceedingDetails(),
                                                proceedingTypeValidator,
                                                model,
                                                "proceedingTypeDetails"));
                                      } else {
                                        return Mono.just(true);
                                      }
                                    })
                                .flatMap(
                                    isError -> {
                                      if (isError) {
                                        proceedingsErrors.addAll(
                                            getErrorsFromModel(model, "proceedingTypeDetails"));
                                      }
                                      return Mono.just(
                                          validateAndAddErrors(
                                              proceedingDetailsFormData,
                                              furtherDetailsValidator,
                                              model,
                                              "proceedingFurtherDetails"));
                                    })
                                .doOnNext(
                                    isError -> {
                                      if (isError) {
                                        proceedingsErrors.addAll(
                                            getErrorsFromModel(model, "proceedingFurtherDetails"));
                                      }
                                    })))
        .then(
            Mono.defer(
                () -> {
                  if (!proceedingsErrors.isEmpty()) {
                    model.addAttribute("proceedingsErrors", proceedingsErrors);
                    return Mono.just(true);
                  }
                  return Mono.just(false);
                }));
  }

  /**
   * Validates a list of prior authorities and adds any errors to the model.
   *
   * @param priorAuthorities the list of prior authorities to validate
   * @param model the model to add any validation errors to
   * @return {@code true} if there are validation errors, {@code false} otherwise
   */
  public boolean validatePriorAuthorities(
      final List<PriorAuthorityDetail> priorAuthorities, final Model model) {
    if (priorAuthorities == null || priorAuthorities.isEmpty()) {
      return false;
    }

    final Set<String> priorAuthorityErrors = new HashSet<>();
    for (final PriorAuthorityDetail priorAuthority : priorAuthorities) {
      final PriorAuthorityFlowFormData priorAuthorityFlow =
          proceedingAndCostsMapper.toPriorAuthorityFlowFormData(priorAuthority);

      if (validateAndAddErrors(
          priorAuthorityFlow.getPriorAuthorityTypeFormData(),
          priorAuthorityTypeValidator,
          model,
          "priorAuthorityType")) {
        priorAuthorityErrors.addAll(getErrorsFromModel(model, "priorAuthorityType"));
      }

      if (validateAndAddErrors(
          priorAuthorityFlow.getPriorAuthorityDetailsFormData(),
          priorAuthorityDetailsValidator,
          model,
          "priorAuthorityDetails")) {
        priorAuthorityErrors.addAll(getErrorsFromModel(model, "priorAuthorityDetails"));
      }
    }

    if (!priorAuthorityErrors.isEmpty()) {
      model.addAttribute("priorAuthorityErrors", priorAuthorityErrors);
      return true;
    }
    return false;
  }

  /**
   * Validates a list of opponents and collects any validation errors.
   *
   * @param opponents the list of opponents to validate
   * @param model the model used to store validation errors
   * @return {@code true} if there are validation errors, {@code false} otherwise
   */
  public boolean validateOpponents(
      final List<AbstractOpponentFormData> opponents, final Model model) {
    if (opponents == null || opponents.isEmpty()) {
      return false;
    }

    final Set<String> opponentErrors = new HashSet<>();
    for (final AbstractOpponentFormData opponent : opponents) {
      if (opponent instanceof IndividualOpponentFormData) {
        if (validateAndAddErrors(
            opponent, individualOpponentValidator, model, "individualOpponent")) {
          opponentErrors.addAll(getErrorsFromModel(model, "individualOpponent"));
        }
      } else if (opponent instanceof OrganisationOpponentFormData) {
        if (validateAndAddErrors(
            opponent, organisationOpponentValidator, model, "organisationOpponent")) {
          opponentErrors.addAll(getErrorsFromModel(model, "organisationOpponent"));
        }
      }
    }

    if (!opponentErrors.isEmpty()) {
      model.addAttribute("opponentErrors", opponentErrors);
      return true;
    }
    return false;
  }

  /**
   * Retrieves a list of error messages from the model by attribute name.
   *
   * @param model the model containing attributes
   * @param attributeName the name of the attribute to retrieve errors from
   * @return a list of error messages, or an empty list if the attribute is not present
   */
  public List<String> getErrorsFromModel(final Model model, final String attributeName) {
    return model.containsAttribute(attributeName)
        ? (List<String>) model.getAttribute(attributeName)
        : Collections.emptyList();
  }
}
