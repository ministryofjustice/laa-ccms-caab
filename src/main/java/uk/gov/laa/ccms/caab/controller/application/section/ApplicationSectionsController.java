package uk.gov.laa.ccms.caab.controller.application.section;


import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_CASE_ADDRESS_OPTION;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.ACTIVE_CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_ID;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CLIENT_FLOW_FORM_DATA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;

import jakarta.servlet.http.HttpSession;
import java.util.ArrayList;
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
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetails;
import uk.gov.laa.ccms.caab.bean.ActiveCase;
import uk.gov.laa.ccms.caab.bean.ClientFlowFormData;
import uk.gov.laa.ccms.caab.constants.assessment.AssessmentRulebase;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.mapper.ClientDetailMapper;
import uk.gov.laa.ccms.caab.mapper.ResultDisplayMapper;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.AssessmentSummaryEntityDisplay;
import uk.gov.laa.ccms.caab.model.summary.ApplicationSummaryDisplay;
import uk.gov.laa.ccms.caab.model.summary.ProviderSummaryDisplay;
import uk.gov.laa.ccms.caab.service.ApplicationService;
import uk.gov.laa.ccms.caab.service.AssessmentService;
import uk.gov.laa.ccms.caab.service.ClientService;
import uk.gov.laa.ccms.caab.service.LookupService;
import uk.gov.laa.ccms.data.model.AssessmentSummaryEntityLookupDetail;
import uk.gov.laa.ccms.data.model.AssessmentSummaryEntityLookupValueDetail;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;
import uk.gov.laa.ccms.data.model.UserDetail;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetail;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetailDetails;

/**
 * Controller for the application sections.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class ApplicationSectionsController {

  private final ApplicationService applicationService;
  private final AssessmentService assessmentService;
  private final LookupService lookupService;
  private final ClientService clientService;
  private final ClientDetailMapper clientDetailsMapper;
  private final ResultDisplayMapper resultDisplayMapper;
  private final String PARENT_LOOKUP = "PARENT";
  private final String CHILD_LOOKUP = "CHILD";

  /**
   * Handles the GET request for application summary page.
   *
   * @param applicationId The id of the application.
   * @param user The user requesting the summary.
   * @param session The http session for the view.
   * @param model The model for the view.
   * @return The view name for the application summary page.
   */
  @GetMapping("/application/sections")
  public String applicationSections(
      @SessionAttribute(APPLICATION_ID) final String applicationId,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      final HttpSession session,
      final Model model) {

    final ApplicationDetail application =
        Optional.ofNullable(applicationService.getApplication(applicationId).block())
            .orElseThrow(() -> new CaabApplicationException(
                "Failed to retrieve application detail"));

    final ApplicationSummaryDisplay summary =
        Optional.ofNullable(applicationService.getApplicationSummary(application, user))
            .orElseThrow(() -> new CaabApplicationException(
                "Failed to retrieve application summary"));

    model.addAttribute("summary", summary);

    final ActiveCase activeCase = ActiveCase.builder()
        .applicationId(application.getId())
        .caseReferenceNumber(summary.getCaseReferenceNumber())
        .providerId(application.getProviderDetails().getProvider().getId())
        .client(summary.getClient().getClientFullName())
        .clientReferenceNumber(summary.getClient().getClientReferenceNumber())
        .providerCaseReferenceNumber(summary.getProvider().getProviderCaseReferenceNumber())
        .build();

    model.addAttribute(ACTIVE_CASE, activeCase);
    session.setAttribute(ACTIVE_CASE, activeCase);
    session.removeAttribute(CLIENT_FLOW_FORM_DATA);

    return "application/sections/task-page";
  }

  /**
   * Handles the GET request for the in-progress application summary page.
   *
   * @param activeCase The active case details
   * @param user The user requesting the summary.
   * @param model The model for the view.
   * @return The view name for the full application summary page.
   */
  @GetMapping("/application/sections/summary")
  public String viewInProgressSummary(
      @SessionAttribute(ACTIVE_CASE) final ActiveCase activeCase,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      final Model model) {

    final ApplicationDetail application =
        Optional.ofNullable(applicationService.getApplication(
                activeCase.getApplicationId().toString()).block())
            .orElseThrow(() -> new CaabApplicationException(
                "Failed to retrieve application detail"));

    final ApplicationSummaryDisplay summary =
        Optional.ofNullable(applicationService.getApplicationSummary(application, user))
            .orElseThrow(() -> new CaabApplicationException(
                "Failed to retrieve application summary"));

    model.addAttribute("summary", summary);

    return "application/sections/application-summary";
  }

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

  @PostMapping("/application/validate")
  public String applicationValidate(){
    //todo validate the application summary details

    return "redirect:/application/summary";
  }

  @GetMapping("/application/summary")
  public String applicationSummary(
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      @SessionAttribute(ACTIVE_CASE) final ActiveCase activeCase,
      final HttpSession session,
      final Model model) {

    final Mono<ApplicationDetail> applicationMono =
        applicationService.getApplication(activeCase.getApplicationId().toString());

    final Mono<AssessmentDetails> assessmentDetailsMono =
        assessmentService.getAssessments(
            List.of(
                AssessmentRulebase.MEANS.getName(),
                AssessmentRulebase.MERITS.getName()),
            activeCase.getProviderId().toString(),
            activeCase.getCaseReferenceNumber());

    final Mono<List<AssessmentSummaryEntityLookupValueDetail>> parentMono =
        lookupService.getAssessmentSummaryAttributes(PARENT_LOOKUP)
            .map(AssessmentSummaryEntityLookupDetail::getContent);

    final Mono<List<AssessmentSummaryEntityLookupValueDetail>> childMono =
        lookupService.getAssessmentSummaryAttributes(CHILD_LOOKUP)
            .map(AssessmentSummaryEntityLookupDetail::getContent);

    final Mono<ClientDetailDetails> clientMono =
        clientService.getClient(
            activeCase.getClientReferenceNumber(),
            user.getLoginId(),
            user.getUserType())
            .map(ClientDetail::getDetails);

    final Tuple5<AssessmentDetails,
                List<AssessmentSummaryEntityLookupValueDetail>,
                List<AssessmentSummaryEntityLookupValueDetail>,
                ClientDetailDetails,
                ApplicationDetail> preprocessingData = Mono.zip(
            assessmentDetailsMono,
            parentMono,
            childMono,
            clientMono,
            applicationMono)
        .blockOptional().orElseThrow(() ->
            new CaabApplicationException("Failed to retrieve assessment data"));

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

    final Mono<List<AssessmentSummaryEntityDisplay>> meansAssessmentSummaryMono =
        Mono.fromCallable(() ->
                assessmentService.getAssessmentSummaryToDisplay(meansAssessmentDetail, parentSummaryLookups, childSummaryLookups))
            .subscribeOn(Schedulers.boundedElastic());

    final Mono<List<AssessmentSummaryEntityDisplay>> meritsAssessmentSummaryMono =
        Mono.fromCallable(() ->
                assessmentService.getAssessmentSummaryToDisplay(meritsAssessmentDetail, parentSummaryLookups, childSummaryLookups))
            .subscribeOn(Schedulers.boundedElastic());

    final Mono<ProviderSummaryDisplay> providerSummaryMono =
        Mono.fromCallable(() ->
                resultDisplayMapper.toProviderSummaryDisplay(application))
            .subscribeOn(Schedulers.boundedElastic());

    final Mono<List<CommonLookupValueDetail>> clientSummaryLookupsMono =
        getClientSummaryListLookups(clientFlowFormData, model);

    final Mono<List<CommonLookupValueDetail>> generalDetailsSummaryLookupsMono =
        getGeneralDetailsListLookups(application, model);

    final Tuple5<List<AssessmentSummaryEntityDisplay>,
        List<AssessmentSummaryEntityDisplay>,
        List<CommonLookupValueDetail>,
        List<CommonLookupValueDetail>,
        ProviderSummaryDisplay> postProcessingData =
        Mono.zip(
                meansAssessmentSummaryMono,
                meritsAssessmentSummaryMono,
                clientSummaryLookupsMono,
                generalDetailsSummaryLookupsMono,
                providerSummaryMono)
            .blockOptional()
            .orElseThrow(() -> new CaabApplicationException("Failed to process summary data"));

    model.addAttribute("meansAssessmentSummary", postProcessingData.getT1());
    model.addAttribute("meritsAssessmentSummary", postProcessingData.getT2());
    model.addAttribute("clientSummary", clientFlowFormData);
    model.addAttribute("providerSummaryData", postProcessingData.getT5());

    return "application/sections/application-summary-complete";
  }

  //todo move to assessment service
  private AssessmentDetail getAssessment(
      final AssessmentDetails assessmentDetails,
      final AssessmentRulebase assessmentRulebase) {

    return assessmentDetails.getContent()
        .stream()
        .filter(assessmentDetail -> assessmentDetail.getName().equalsIgnoreCase(assessmentRulebase.getName()))
        .findFirst()
        .orElseThrow(() -> new CaabApplicationException("Failed to retrieve assessment"));
  }

  //todo move to lookup service
  protected Mono<List<CommonLookupValueDetail>> getClientSummaryListLookups(
      final ClientFlowFormData clientFlowFormData, final Model model) {

    // Create a list of Mono calls and their respective attribute keys
    final List<Pair<String, Mono<Optional<CommonLookupValueDetail>>>> lookups =
        lookupService.getClientLookups(clientFlowFormData);

    // Fetch all Monos asynchronously
    return lookupService.addCommonLookupsToModel(lookups, model);
  }

  //todo move to lookup service
  protected Mono<List<CommonLookupValueDetail>> getGeneralDetailsListLookups(
      final ApplicationDetail application, final Model model) {

    // Create a list of Mono calls and their respective attribute keys
    final List<Pair<String, Mono<Optional<CommonLookupValueDetail>>>> lookups = new ArrayList<>();

    if (application.getCorrespondenceAddress() != null
        && application.getCorrespondenceAddress().getPreferredAddress() != null) {
      lookups.add(Pair.of("preferredAddress",
            lookupService.getCommonValue(
                COMMON_VALUE_CASE_ADDRESS_OPTION,
                application.getCorrespondenceAddress().getPreferredAddress())));
    }

    return lookupService.addCommonLookupsToModel(lookups, model);
  }

}