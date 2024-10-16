package uk.gov.laa.ccms.caab.controller.application.section;


import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.DECLARATION_APPLICATION;
import static uk.gov.laa.ccms.caab.constants.CcmsModule.APPLICATION;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.ACTIVE_CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_ID;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.SUBMISSION_SUMMARY;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.SUBMISSION_TRANSACTION_ID;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;
import static uk.gov.laa.ccms.caab.constants.SubmissionConstants.SUBMISSION_CREATE_CASE;
import static uk.gov.laa.ccms.caab.util.AssessmentUtil.getAssessment;
import static uk.gov.laa.ccms.caab.util.AssessmentUtil.getMostRecentAssessmentDetail;

import jakarta.servlet.http.HttpSession;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.validation.Validator;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.SessionAttribute;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple3;
import reactor.util.function.Tuple4;
import reactor.util.function.Tuple7;
import reactor.util.function.Tuple8;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetails;
import uk.gov.laa.ccms.caab.bean.ActiveCase;
import uk.gov.laa.ccms.caab.bean.AddressFormData;
import uk.gov.laa.ccms.caab.bean.ApplicationFormData;
import uk.gov.laa.ccms.caab.bean.ClientFlowFormData;
import uk.gov.laa.ccms.caab.bean.SummarySubmissionFormData;
import uk.gov.laa.ccms.caab.bean.declaration.DynamicCheckbox;
import uk.gov.laa.ccms.caab.bean.opponent.AbstractOpponentFormData;
import uk.gov.laa.ccms.caab.bean.opponent.IndividualOpponentFormData;
import uk.gov.laa.ccms.caab.bean.opponent.OrganisationOpponentFormData;
import uk.gov.laa.ccms.caab.bean.priorauthority.PriorAuthorityFlowFormData;
import uk.gov.laa.ccms.caab.bean.validators.application.ProviderDetailsValidator;
import uk.gov.laa.ccms.caab.bean.validators.client.CorrespondenceAddressValidator;
import uk.gov.laa.ccms.caab.bean.validators.declaration.DeclarationSubmissionValidator;
import uk.gov.laa.ccms.caab.bean.validators.opponent.IndividualOpponentValidator;
import uk.gov.laa.ccms.caab.bean.validators.opponent.OrganisationOpponentValidator;
import uk.gov.laa.ccms.caab.bean.validators.priorauthority.PriorAuthorityDetailsValidator;
import uk.gov.laa.ccms.caab.bean.validators.priorauthority.PriorAuthorityTypeDetailsValidator;
import uk.gov.laa.ccms.caab.bean.validators.proceedings.ProceedingDetailsValidator;
import uk.gov.laa.ccms.caab.bean.validators.proceedings.ProceedingFurtherDetailsValidator;
import uk.gov.laa.ccms.caab.bean.validators.proceedings.ProceedingMatterTypeDetailsValidator;
import uk.gov.laa.ccms.caab.constants.SearchConstants;
import uk.gov.laa.ccms.caab.constants.assessment.AssessmentRulebase;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.mapper.ClientDetailMapper;
import uk.gov.laa.ccms.caab.mapper.ProceedingAndCostsMapper;
import uk.gov.laa.ccms.caab.mapper.SubmissionSummaryDisplayMapper;
import uk.gov.laa.ccms.caab.mapper.context.submission.GeneralDetailsSubmissionSummaryMappingContext;
import uk.gov.laa.ccms.caab.mapper.context.submission.OpponentSubmissionSummaryMappingContext;
import uk.gov.laa.ccms.caab.mapper.context.submission.ProceedingSubmissionSummaryMappingContext;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.BaseEvidenceDocumentDetail;
import uk.gov.laa.ccms.caab.model.EvidenceDocumentDetail;
import uk.gov.laa.ccms.caab.model.EvidenceDocumentDetails;
import uk.gov.laa.ccms.caab.model.PriorAuthorityDetail;
import uk.gov.laa.ccms.caab.model.ProceedingDetail;
import uk.gov.laa.ccms.caab.model.StringDisplayValue;
import uk.gov.laa.ccms.caab.model.assessment.AssessmentSummaryEntityDisplay;
import uk.gov.laa.ccms.caab.model.summary.GeneralDetailsSubmissionSummaryDisplay;
import uk.gov.laa.ccms.caab.model.summary.OpponentsAndOtherPartiesSubmissionSummaryDisplay;
import uk.gov.laa.ccms.caab.model.summary.ProceedingAndCostSubmissionSummaryDisplay;
import uk.gov.laa.ccms.caab.model.summary.ProviderSubmissionSummaryDisplay;
import uk.gov.laa.ccms.caab.model.summary.SubmissionSummaryDisplay;
import uk.gov.laa.ccms.caab.service.ApplicationService;
import uk.gov.laa.ccms.caab.service.AssessmentService;
import uk.gov.laa.ccms.caab.service.ClientService;
import uk.gov.laa.ccms.caab.service.EvidenceService;
import uk.gov.laa.ccms.caab.service.LookupService;
import uk.gov.laa.ccms.data.model.AssessmentSummaryEntityLookupDetail;
import uk.gov.laa.ccms.data.model.AssessmentSummaryEntityLookupValueDetail;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;
import uk.gov.laa.ccms.data.model.DeclarationLookupDetail;
import uk.gov.laa.ccms.data.model.UserDetail;
import uk.gov.laa.ccms.soa.gateway.model.CaseTransactionResponse;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetail;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetailDetails;

/**
 * Controller for the application sections.
 */
@Controller
@RequiredArgsConstructor
@Slf4j

public class ApplicationSubmissionController {

  //services
  private final ApplicationService applicationService;
  private final AssessmentService assessmentService;
  private final LookupService lookupService;
  private final ClientService clientService;
  private final EvidenceService evidenceService;

  //mappers
  private final ClientDetailMapper clientDetailsMapper;
  private final SubmissionSummaryDisplayMapper submissionSummaryDisplayMapper;
  private final ProceedingAndCostsMapper proceedingAndCostsMapper;

  //validators
  private final DeclarationSubmissionValidator declarationSubmissionValidator;
  private final ProviderDetailsValidator providerDetailsValidator;
  private final CorrespondenceAddressValidator correspondenceAddressValidator;
  private final ProceedingMatterTypeDetailsValidator matterTypeValidator;
  private final ProceedingDetailsValidator proceedingTypeValidator;
  private final ProceedingFurtherDetailsValidator furtherDetailsValidator;
  private final PriorAuthorityTypeDetailsValidator priorAuthorityTypeValidator;
  private final PriorAuthorityDetailsValidator priorAuthorityDetailsValidator;
  private final OrganisationOpponentValidator organisationOpponentValidator;
  private final IndividualOpponentValidator individualOpponentValidator;

  private final SearchConstants searchConstants;

  protected static final String PARENT_LOOKUP = "PARENT";
  protected static final String CHILD_LOOKUP = "CHILD";

  /**
   * Handles the GET request for the abandon application confirmation page.
   *
   * @return The view name for the abandon application confirmation page.
   */
  @GetMapping("/application/abandon")
  public String viewAbandonApplicationConfirmation() {
    return "application/application-abandon-confirmation";
  }

  /**
   * Handles the POST request to abandon an application.
   *
   * @param activeCase The active case details
   * @param user The user requesting the summary.
   * @return Redirect to the home page.
   */
  @PostMapping("/application/abandon/confirmed")
  public String abandonApplication(
      @SessionAttribute(ACTIVE_CASE) final ActiveCase activeCase,
      @SessionAttribute(USER_DETAILS) final UserDetail user) {

    final ApplicationDetail application =
        Optional.ofNullable(applicationService.getApplication(
                activeCase.getApplicationId().toString()).block())
            .orElseThrow(() -> new CaabApplicationException(
                "Failed to retrieve application detail"));

    applicationService.abandonApplication(application, user);

    return "redirect:/home";

  }

  /**
   * Validates the application details and redirects based on validation results.
   *
   * @param applicationId the ID of the application to validate
   * @param model the model to add validation errors to
   * @return a Mono that emits the view name based on validation outcome
   */
  @GetMapping("/application/validate")
  public Mono<String> applicationValidate(
      @SessionAttribute(APPLICATION_ID) final String applicationId,
      final Model model
  ) {
    // Fetch data reactively
    final Mono<ApplicationFormData> providerDetailsMono =
        applicationService.getMonoProviderDetailsFormData(applicationId);
    final Mono<AddressFormData> generalDetailsMono =
        applicationService.getMonoCorrespondenceAddressFormData(applicationId);
    final Mono<ApplicationDetail> applicationMono =
        applicationService.getApplication(applicationId);
    final Mono<List<AbstractOpponentFormData>> opponentsMono = Mono.fromCallable(() ->
            applicationService.getOpponents(applicationId))
        .subscribeOn(Schedulers.boundedElastic());

    // Combine all Monos in a reactive manner using Mono.zip
    return Mono.zip(providerDetailsMono, generalDetailsMono, applicationMono, opponentsMono)
        .flatMap(tuple -> {
          final ApplicationFormData providerDetailsFormData = tuple.getT1();
          final AddressFormData generalDetailsFormData = tuple.getT2();
          final ApplicationDetail application = tuple.getT3();
          final List<AbstractOpponentFormData> opponents = tuple.getT4();

          boolean validationFailed = false;

          // Validate Provider Details
          if (validateAndAddErrors(
              providerDetailsFormData,
              providerDetailsValidator, model, "providerDetailsErrors")) {
            validationFailed = true;
          }

          // Validate General Details
          if (validateAndAddErrors(
              generalDetailsFormData,
              correspondenceAddressValidator, model, "generalDetailsErrors")) {
            validationFailed = true;
          }

          // Validate Proceedings - Now returns a reactive Mono<Boolean>
          final Mono<Boolean> proceedingsValidationMono =
              application.getProceedings() != null && !application.getProceedings().isEmpty()
              ? validateProceedings(application.getProceedings(), model)
              : Mono.just(false);

          // Validate Opponents
          if (validateOpponents(opponents, model)) {
            validationFailed = true;
          }

          // Validate Prior Authorities
          if (validatePriorAuthorities(application.getPriorAuthorities(), model)) {
            validationFailed = true;
          }

          final boolean finalValidationFailed = validationFailed;
          // Wait for the proceedings validation to complete
          return proceedingsValidationMono.map(proceedingsValidationFailed -> {
            if (proceedingsValidationFailed || finalValidationFailed) {
              return "application/application-validation-error-correction";
            } else {
              return "redirect:/application/summary";
            }
          });
        });
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
    final BindingResult bindingResult = new BeanPropertyBindingResult(
        formData, formData.getClass().getSimpleName());
    validator.validate(formData, bindingResult);

    if (bindingResult.hasErrors()) {
      final List<String> errors = bindingResult.getAllErrors()
          .stream()
          .map(ObjectError::getDefaultMessage)
          .collect(Collectors.toList());
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
  protected Mono<Boolean> validateProceedings(
      final List<ProceedingDetail> proceedings, final Model model) {
    if (proceedings == null || proceedings.isEmpty()) {
      return Mono.just(false);
    }

    final Set<String> proceedingsErrors = new HashSet<>();

    return Flux.fromIterable(proceedings)
        .flatMap(proceeding -> Optional.ofNullable(proceeding.getTypeOfOrder())
            .map(StringDisplayValue::getId)
            .map(lookupService::getOrderTypeDescription)
            .orElse(Mono.empty())
            .map(orderTypeDisplayValue ->
                proceedingAndCostsMapper.toProceedingFlow(proceeding, orderTypeDisplayValue)
            )
            .flatMap(proceedingDetailsFormData -> Mono.just(
                validateAndAddErrors(proceedingDetailsFormData.getMatterTypeDetails(),
                    matterTypeValidator, model, "proceedingMatterTypeDetails"))
                .flatMap(isError -> {
                  if (isError) {
                    proceedingsErrors.addAll(
                        getErrorsFromModel(model, "proceedingMatterTypeDetails"));
                  }
                  return Mono.just(isError);
                })
                .flatMap(isError -> {
                  if (!isError) {
                    return Mono.just(validateAndAddErrors(
                        proceedingDetailsFormData.getProceedingDetails(),
                        proceedingTypeValidator, model, "proceedingTypeDetails"));
                  } else {
                    return Mono.just(true);
                  }
                })
                .flatMap(isError -> {
                  if (isError) {
                    proceedingsErrors.addAll(getErrorsFromModel(model, "proceedingTypeDetails"));
                  }
                  return Mono.just(validateAndAddErrors(proceedingDetailsFormData,
                      furtherDetailsValidator, model, "proceedingFurtherDetails"));
                })
                .doOnNext(isError -> {
                  if (isError) {
                    proceedingsErrors.addAll(getErrorsFromModel(model, "proceedingFurtherDetails"));
                  }
                })))
        .then(Mono.defer(() -> {
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
  protected boolean validatePriorAuthorities(
      final List<PriorAuthorityDetail> priorAuthorities,
      final Model model) {
    if (priorAuthorities == null || priorAuthorities.isEmpty()) {
      return false;
    }

    final Set<String> priorAuthorityErrors = new HashSet<>();
    for (final PriorAuthorityDetail priorAuthority : priorAuthorities) {
      final PriorAuthorityFlowFormData priorAuthorityFlow =
          proceedingAndCostsMapper.toPriorAuthorityFlowFormData(priorAuthority);

      if (validateAndAddErrors(priorAuthorityFlow.getPriorAuthorityTypeFormDataDetails(),
          priorAuthorityTypeValidator, model, "priorAuthorityType")) {
        priorAuthorityErrors.addAll(getErrorsFromModel(model, "priorAuthorityType"));
      }

      if (validateAndAddErrors(priorAuthorityFlow.getPriorAuthorityFormDataDetails(),
          priorAuthorityDetailsValidator, model, "priorAuthorityDetails")) {
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
  protected boolean validateOpponents(
      final List<AbstractOpponentFormData> opponents,
      final Model model) {
    if (opponents == null || opponents.isEmpty()) {
      return false;
    }

    final Set<String> opponentErrors = new HashSet<>();
    for (final AbstractOpponentFormData opponent : opponents) {
      if (opponent instanceof IndividualOpponentFormData) {
        if (validateAndAddErrors(opponent,
            individualOpponentValidator, model, "individualOpponent")) {
          opponentErrors.addAll(getErrorsFromModel(model, "individualOpponent"));
        }
      } else if (opponent instanceof OrganisationOpponentFormData) {
        if (validateAndAddErrors(opponent,
            organisationOpponentValidator, model, "organisationOpponent")) {
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
  @SuppressWarnings("unchecked")
  protected List<String> getErrorsFromModel(final Model model, final String attributeName) {
    return model.containsAttribute(attributeName)
        ? (List<String>) model.getAttribute(attributeName) : Collections.emptyList();
  }


  /**
   * Returns the user to the application section task page after continue is clicked.
   *
   * @return the redirection URL to the application summary page
   */
  @PostMapping("/application/validate")
  public String applicationValidatePost() {
    return "redirect:/application/sections";
  }

  /**
   * Handles the GET request for the application summary page.
   *
   * @param user The user requesting the summary.
   * @param activeCase The active case details
   * @param session The session
   * @param model The model
   * @return The view name for the application summary page.
   */
  @GetMapping("/application/summary")
  public String applicationSummary(
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      @SessionAttribute(ACTIVE_CASE) final ActiveCase activeCase,
      final HttpSession session,
      final Model model) {

    //Pre-processing data - application data
    final Mono<ApplicationDetail> applicationMono =
        applicationService.getApplication(activeCase.getApplicationId().toString());

    //Pre-processing data - means and merits assessment data
    final Mono<AssessmentDetails> assessmentDetailsMono =
        assessmentService.getAssessments(
            List.of(
                AssessmentRulebase.MEANS.getName(),
                AssessmentRulebase.MERITS.getName()),
            activeCase.getProviderId().toString(),
            activeCase.getCaseReferenceNumber());

    //Pre-processing data - assessment parent summary lookups
    final Mono<List<AssessmentSummaryEntityLookupValueDetail>> parentMono =
        lookupService.getAssessmentSummaryAttributes(PARENT_LOOKUP)
            .map(AssessmentSummaryEntityLookupDetail::getContent);

    //Pre-processing data - assessment child summary lookups
    final Mono<List<AssessmentSummaryEntityLookupValueDetail>> childMono =
        lookupService.getAssessmentSummaryAttributes(CHILD_LOOKUP)
            .map(AssessmentSummaryEntityLookupDetail::getContent);

    //Pre-processing data - client data
    final Mono<ClientDetailDetails> clientMono =
        clientService.getClient(
                activeCase.getClientReferenceNumber(),
                user.getLoginId(),
                user.getUserType())
            .map(ClientDetail::getDetails);

    //Pre-processing data - proceeding context data
    final Mono<ProceedingSubmissionSummaryMappingContext> proceedingContextMono =
        lookupService.getProceedingSubmissionMappingContext();

    //Pre-processing data - opponent context data
    final Mono<OpponentSubmissionSummaryMappingContext> opponentContextMono =
        lookupService.getOpponentSubmissionMappingContext();

    //Pre-processing data - general details context data
    final Mono<GeneralDetailsSubmissionSummaryMappingContext> generalDetailsContextMono =
        lookupService.getGeneralDetailsSubmissionMappingContext();

    final Tuple8<
        AssessmentDetails,
        List<AssessmentSummaryEntityLookupValueDetail>,
        List<AssessmentSummaryEntityLookupValueDetail>,
        ClientDetailDetails,
        ApplicationDetail,
        ProceedingSubmissionSummaryMappingContext,
        OpponentSubmissionSummaryMappingContext,
        GeneralDetailsSubmissionSummaryMappingContext> preprocessingData =
        Mono.zip(
            assessmentDetailsMono,
            parentMono,
            childMono,
            clientMono,
            applicationMono,
            proceedingContextMono,
            opponentContextMono,
            generalDetailsContextMono).blockOptional().orElseThrow(() ->
            new CaabApplicationException("Failed to pre-process summary data"));

    final AssessmentDetail meansAssessmentDetail = getAssessment(
        preprocessingData.getT1(), AssessmentRulebase.MEANS);

    final AssessmentDetail meritsAssessmentDetail = getAssessment(
        preprocessingData.getT1(), AssessmentRulebase.MERITS);

    final List<AssessmentSummaryEntityLookupValueDetail> parentSummaryLookups =
        preprocessingData.getT2();
    final List<AssessmentSummaryEntityLookupValueDetail> childSummaryLookups =
        preprocessingData.getT3();

    final ClientFlowFormData clientFlowFormData =
        clientDetailsMapper.toClientFlowFormData(preprocessingData.getT4());

    final ApplicationDetail application = preprocessingData.getT5();

    // Post-processing data - means assessment summary data
    final Mono<List<AssessmentSummaryEntityDisplay>> meansAssessmentSummaryMono =
        Mono.fromCallable(() ->
                assessmentService.getAssessmentSummaryToDisplay(
                    meansAssessmentDetail, parentSummaryLookups, childSummaryLookups))
            .subscribeOn(Schedulers.boundedElastic());

    // Post-processing data - merits assessment summary data
    final Mono<List<AssessmentSummaryEntityDisplay>> meritsAssessmentSummaryMono =
        Mono.fromCallable(() ->
                assessmentService.getAssessmentSummaryToDisplay(
                    meritsAssessmentDetail, parentSummaryLookups, childSummaryLookups))
            .subscribeOn(Schedulers.boundedElastic());

    // Post-processing data - provider summary data
    final Mono<ProviderSubmissionSummaryDisplay> providerSummaryMono =
        Mono.fromCallable(() ->
                submissionSummaryDisplayMapper.toProviderSummaryDisplay(application))
            .subscribeOn(Schedulers.boundedElastic());

    // Post-processing data - client summary data
    final Mono<HashMap<String, CommonLookupValueDetail>> clientSummaryLookupsMono =
        lookupService.getClientSummaryListLookups(clientFlowFormData);

    // Post-processing data - general details summary data
    final Mono<GeneralDetailsSubmissionSummaryDisplay> generalDetailsSummaryMono =
        Mono.fromCallable(() ->
                submissionSummaryDisplayMapper.toGeneralDetailsSummaryDisplay(application,
                    preprocessingData.getT8()))
            .subscribeOn(Schedulers.boundedElastic());

    // Post-processing data - proceedings and costs summary data
    final Mono<ProceedingAndCostSubmissionSummaryDisplay> proceedingAndCostSummaryMono =
        Mono.fromCallable(() ->
                submissionSummaryDisplayMapper.toProceedingAndCostSummaryDisplay(
                    application, preprocessingData.getT6()))
            .subscribeOn(Schedulers.boundedElastic());

    // Post-processing data - opponents and other parties summary data
    final Mono<OpponentsAndOtherPartiesSubmissionSummaryDisplay>
        opponentsAndOtherPartiesSummaryMono =
        Mono.fromCallable(() ->
                submissionSummaryDisplayMapper.toOpponentsAndOtherPartiesSummaryDisplay(
                    application, preprocessingData.getT7()))
            .subscribeOn(Schedulers.boundedElastic());

    final Tuple7<List<AssessmentSummaryEntityDisplay>,
        List<AssessmentSummaryEntityDisplay>,
        HashMap<String, CommonLookupValueDetail>,
        GeneralDetailsSubmissionSummaryDisplay,
        ProviderSubmissionSummaryDisplay,
        ProceedingAndCostSubmissionSummaryDisplay,
        OpponentsAndOtherPartiesSubmissionSummaryDisplay> postProcessingData =
        Mono.zip(
                meansAssessmentSummaryMono,
                meritsAssessmentSummaryMono,
                clientSummaryLookupsMono,
                generalDetailsSummaryMono,
                providerSummaryMono,
                proceedingAndCostSummaryMono,
                opponentsAndOtherPartiesSummaryMono)
            .blockOptional()
            .orElseThrow(() -> new CaabApplicationException("Failed to post-process summary data"));


    //create final summary object
    final SubmissionSummaryDisplay submissionSummary = SubmissionSummaryDisplay.builder()
        .client(clientFlowFormData)
        .meansAssessment(postProcessingData.getT1())
        .meritsAssessment(postProcessingData.getT2())
        .clientLookups(postProcessingData.getT3())
        .generalDetails(postProcessingData.getT4())
        .providerDetails(postProcessingData.getT5())
        .proceedingsAndCosts(postProcessingData.getT6())
        .opponentsAndOtherParties(postProcessingData.getT7())
        .build();

    session.setAttribute(SUBMISSION_SUMMARY, submissionSummary);
    model.addAttribute("submissionSummary", submissionSummary);
    model.addAttribute("summarySubmissionFormData", new SummarySubmissionFormData());

    return "application/sections/application-summary-complete";
  }

  /**
   * Handles the GET request for the printable application summary page.
   *
   * @param submissionSummary the summary information for the application.
   * @param model The model
   * @return The view name for the application summary page.
   */
  @GetMapping("/application/summary/print")
  public String applicationSummaryPrint(
      @SessionAttribute(SUBMISSION_SUMMARY) final SubmissionSummaryDisplay submissionSummary,
      final Model model) {

    model.addAttribute("submissionSummary", submissionSummary);
    return "application/sections/application-summary-complete-printable";
  }


  /**
   * Handles POST requests to submit the application summary form.
   *
   * @param submissionSummary the summary of the submission stored in the session
   * @param summarySubmissionFormData the form data for the summary submission
   * @param bindingResult validation results for the form data
   * @param model the model to hold attributes for rendering views
   * @return the view to render or a redirect to the declaration page
   */
  @PostMapping("/application/summary")
  public String applicationSummaryPost(
      @SessionAttribute(SUBMISSION_SUMMARY)
      final SubmissionSummaryDisplay submissionSummary,
      @ModelAttribute("summarySubmissionFormData")
      final SummarySubmissionFormData summarySubmissionFormData,
      final BindingResult bindingResult,
      final Model model) {

    if (bindingResult.hasErrors()) {
      model.addAttribute("submissionSummary", submissionSummary);
      return "application/sections/application-summary-complete";
    }

    return "redirect:/application/declaration";
  }

  /**
   * Handles GET requests to display the application declaration page.
   *
   * @param summarySubmissionFormData the form data for the summary submission
   * @param model the model to hold attributes for rendering views
   * @return the view to render for the declaration page
   */
  @GetMapping("/application/declaration")
  public String applicationDeclaration(
      @ModelAttribute("summarySubmissionFormData")
      final SummarySubmissionFormData summarySubmissionFormData,
      final Model model) {

    return applicationDeclarationDetails(model, summarySubmissionFormData);
  }

  /**
   * Handles POST requests to submit the application declaration form.
   *
   * @param summarySubmissionFormData the form data for the summary submission
   * @param bindingResult validation results for the form data
   * @param model the model to hold attributes for rendering views
   * @return the view to render or a redirect after successful submission
   */
  @PostMapping("/application/declaration")
  public String applicationDeclarationPost(
      @ModelAttribute("summarySubmissionFormData")
      final SummarySubmissionFormData summarySubmissionFormData,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      @SessionAttribute(ACTIVE_CASE) final ActiveCase activeCase,
      final BindingResult bindingResult,
      final Model model,
      final HttpSession session) {

    declarationSubmissionValidator.validate(summarySubmissionFormData, bindingResult);

    if (bindingResult.hasErrors()) {
      return applicationDeclarationDetails(model, summarySubmissionFormData);
    }

    final EvidenceDocumentDetails evidenceDocumentDetails =
        evidenceService.getEvidenceDocumentsForCase(
            activeCase.getCaseReferenceNumber(), APPLICATION).block();

    //register previously uploaded documents
    evidenceService.registerPreviouslyUploadedDocuments(evidenceDocumentDetails, user);

    evidenceService.uploadAndUpdateDocuments(
        evidenceDocumentDetails, activeCase.getCaseReferenceNumber(), user)
        .block();


    //map application to case
    final Mono<ApplicationDetail> applicationMono =
        applicationService.getApplication(
            String.valueOf(activeCase.getApplicationId()));

    final Mono<AssessmentDetails> meansAssessmentsMono =
        assessmentService.getAssessments(
            List.of(AssessmentRulebase.MEANS.getName()),
            user.getProvider().getId().toString(),
            activeCase.getCaseReferenceNumber());

    final Mono<AssessmentDetails> meritsAssessmentsMono =
        assessmentService.getAssessments(
            List.of(AssessmentRulebase.MERITS.getName()),
            user.getProvider().getId().toString(),
            activeCase.getCaseReferenceNumber());

    final Mono<EvidenceDocumentDetails> caseDocsMono =
        evidenceService.getEvidenceDocumentsForCase(
            activeCase.getCaseReferenceNumber(), APPLICATION);

    final Tuple4<ApplicationDetail, AssessmentDetails, AssessmentDetails, EvidenceDocumentDetails>
        applicationMonos = Mono.zip(
            applicationMono, meansAssessmentsMono, meritsAssessmentsMono, caseDocsMono)
            .blockOptional().orElseThrow(() ->
                new CaabApplicationException("Failed to retrieve application and assessment data"));

    final ApplicationDetail application = applicationMonos.getT1();

    final AssessmentDetail meansAssessment =
        getMostRecentAssessmentDetail(applicationMonos.getT2().getContent());

    final AssessmentDetail meritsAssessment =
        getMostRecentAssessmentDetail(applicationMonos.getT3().getContent());

    final List<BaseEvidenceDocumentDetail> caseDocs = applicationMonos.getT4().getContent();

    final CaseTransactionResponse response =
        applicationService.createCase(
            user,
            application,
            meansAssessment,
            meritsAssessment,
            caseDocs);

    session.setAttribute(SUBMISSION_TRANSACTION_ID, response.getTransactionId());

    return "redirect:/submissions/%s".formatted(SUBMISSION_CREATE_CASE);
  }

  /**
   * Prepares the model for displaying the application declaration details.
   *
   * @param model the model to hold attributes for rendering views
   * @param summarySubmissionFormData the form data for the summary submission
   * @return the view to render for the declaration details page
   */
  private String applicationDeclarationDetails(
      final Model model,
      final SummarySubmissionFormData summarySubmissionFormData) {
    final DeclarationLookupDetail declarations =
        lookupService.getDeclarations(DECLARATION_APPLICATION).block();
    final List<DynamicCheckbox> declarationOptions =
        submissionSummaryDisplayMapper.toDeclarationFormDataDynamicOptionList(declarations);
    summarySubmissionFormData.setDeclarationOptions(declarationOptions);
    model.addAttribute("summarySubmissionFormData", summarySubmissionFormData);
    return "application/sections/application-submit-declaration";
  }

}