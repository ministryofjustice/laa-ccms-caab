package uk.gov.laa.ccms.caab.controller.application.section;


import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_CASE_ADDRESS_OPTION;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_CONTACT_TITLE;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_PROCEEDING_ORDER_TYPE;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_RELATIONSHIP_TO_CLIENT;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.ACTIVE_CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;
import static uk.gov.laa.ccms.caab.util.AssessmentUtil.getAssessment;

import jakarta.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.SessionAttribute;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple5;
import reactor.util.function.Tuple7;
import reactor.util.function.Tuple8;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetails;
import uk.gov.laa.ccms.caab.bean.ActiveCase;
import uk.gov.laa.ccms.caab.bean.ClientFlowFormData;
import uk.gov.laa.ccms.caab.constants.assessment.AssessmentRulebase;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.mapper.ClientDetailMapper;
import uk.gov.laa.ccms.caab.mapper.SubmissionSummaryDisplayMapper;
import uk.gov.laa.ccms.caab.mapper.context.submission.GeneralDetailsSubmissionSummaryMappingContext;
import uk.gov.laa.ccms.caab.mapper.context.submission.OpponentSubmissionSummaryMappingContext;
import uk.gov.laa.ccms.caab.mapper.context.submission.ProceedingSubmissionSummaryMappingContext;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.assessment.AssessmentSummaryEntityDisplay;
import uk.gov.laa.ccms.caab.model.summary.GeneralDetailsSubmissionSummaryDisplay;
import uk.gov.laa.ccms.caab.model.summary.OpponentsAndOtherPartiesSubmissionSummaryDisplay;
import uk.gov.laa.ccms.caab.model.summary.ProceedingAndCostSubmissionSummaryDisplay;
import uk.gov.laa.ccms.caab.model.summary.ProviderSubmissionSummaryDisplay;
import uk.gov.laa.ccms.caab.model.summary.SubmissionSummaryDisplay;
import uk.gov.laa.ccms.caab.service.ApplicationService;
import uk.gov.laa.ccms.caab.service.AssessmentService;
import uk.gov.laa.ccms.caab.service.ClientService;
import uk.gov.laa.ccms.caab.service.LookupService;
import uk.gov.laa.ccms.data.model.AssessmentSummaryEntityLookupDetail;
import uk.gov.laa.ccms.data.model.AssessmentSummaryEntityLookupValueDetail;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;
import uk.gov.laa.ccms.data.model.RelationshipToCaseLookupDetail;
import uk.gov.laa.ccms.data.model.RelationshipToCaseLookupValueDetail;
import uk.gov.laa.ccms.data.model.UserDetail;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetail;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetailDetails;

/**
 * Controller for the application sections.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class ApplicationSubmissionController {

  private final ApplicationService applicationService;
  private final AssessmentService assessmentService;
  private final LookupService lookupService;
  private final ClientService clientService;
  private final ClientDetailMapper clientDetailsMapper;
  private final SubmissionSummaryDisplayMapper submissionSummaryDisplayMapper;
  private static final String PARENT_LOOKUP = "PARENT";
  private static final String CHILD_LOOKUP = "CHILD";

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
   * Validates the application summary details and redirects to the summary page.
   *
   * @return the redirection URL to the application summary page
   */
  @PostMapping("/application/validate")
  public String applicationValidate() {
    //todo validate the application summary details

    return "redirect:/application/summary";
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
    ;

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

    session.setAttribute("submissionSummary", submissionSummary);
    model.addAttribute("submissionSummary", submissionSummary);

    return "application/sections/application-summary-complete";
  }



}