package uk.gov.laa.ccms.caab.service;

import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.APP_TYPE_EMERGENCY;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.APP_TYPE_EMERGENCY_DEVOLVED_POWERS;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.APP_TYPE_SUBSTANTIVE_DEVOLVED_POWERS;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.AWARD_TYPE_COST;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.AWARD_TYPE_FINANCIAL;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.AWARD_TYPE_LAND;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.AWARD_TYPE_OTHER_ASSET;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.EMERGENCY_APPLICATION_TYPE_CODES;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.OPPONENT_TYPE_INDIVIDUAL;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.OPPONENT_TYPE_ORGANISATION;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.REFERENCE_DATA_ITEM_TYPE_LOV;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.STATUS_DRAFT;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.STATUS_UNSUBMITTED_ACTUAL_VALUE;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.STATUS_UNSUBMITTED_ACTUAL_VALUE_DISPLAY;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuple3;
import reactor.util.function.Tuple4;
import reactor.util.function.Tuple6;
import uk.gov.laa.ccms.caab.bean.AddressFormData;
import uk.gov.laa.ccms.caab.bean.ApplicationFormData;
import uk.gov.laa.ccms.caab.bean.CaseSearchCriteria;
import uk.gov.laa.ccms.caab.bean.OpponentFormData;
import uk.gov.laa.ccms.caab.builders.ApplicationBuilder;
import uk.gov.laa.ccms.caab.builders.ApplicationSummaryBuilder;
import uk.gov.laa.ccms.caab.builders.ApplicationTypeBuilder;
import uk.gov.laa.ccms.caab.client.CaabApiClient;
import uk.gov.laa.ccms.caab.client.EbsApiClient;
import uk.gov.laa.ccms.caab.client.SoaApiClient;
import uk.gov.laa.ccms.caab.constants.SearchConstants;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.exception.TooManyResultsException;
import uk.gov.laa.ccms.caab.mapper.AddressFormDataMapper;
import uk.gov.laa.ccms.caab.mapper.ApplicationFormDataMapper;
import uk.gov.laa.ccms.caab.mapper.ApplicationMapper;
import uk.gov.laa.ccms.caab.mapper.CopyApplicationMapper;
import uk.gov.laa.ccms.caab.mapper.OpponentMapper;
import uk.gov.laa.ccms.caab.mapper.ResultDisplayMapper;
import uk.gov.laa.ccms.caab.mapper.context.ApplicationMappingContext;
import uk.gov.laa.ccms.caab.mapper.context.CaseOutcomeMappingContext;
import uk.gov.laa.ccms.caab.mapper.context.PriorAuthorityMappingContext;
import uk.gov.laa.ccms.caab.mapper.context.ProceedingMappingContext;
import uk.gov.laa.ccms.caab.model.Address;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.ApplicationDetails;
import uk.gov.laa.ccms.caab.model.ApplicationProviderDetails;
import uk.gov.laa.ccms.caab.model.ApplicationSummaryDisplay;
import uk.gov.laa.ccms.caab.model.ApplicationType;
import uk.gov.laa.ccms.caab.model.BaseApplication;
import uk.gov.laa.ccms.caab.model.CostStructure;
import uk.gov.laa.ccms.caab.model.IntDisplayValue;
import uk.gov.laa.ccms.caab.model.LinkedCase;
import uk.gov.laa.ccms.caab.model.LinkedCaseResultRowDisplay;
import uk.gov.laa.ccms.caab.model.Opponent;
import uk.gov.laa.ccms.caab.model.OpponentRowDisplay;
import uk.gov.laa.ccms.caab.model.PriorAuthority;
import uk.gov.laa.ccms.caab.model.Proceeding;
import uk.gov.laa.ccms.caab.model.ResultsDisplay;
import uk.gov.laa.ccms.caab.model.StringDisplayValue;
import uk.gov.laa.ccms.caab.util.ReflectionUtils;
import uk.gov.laa.ccms.data.model.AmendmentTypeLookupDetail;
import uk.gov.laa.ccms.data.model.AwardTypeLookupDetail;
import uk.gov.laa.ccms.data.model.AwardTypeLookupValueDetail;
import uk.gov.laa.ccms.data.model.CaseStatusLookupDetail;
import uk.gov.laa.ccms.data.model.CaseStatusLookupValueDetail;
import uk.gov.laa.ccms.data.model.CategoryOfLawLookupValueDetail;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;
import uk.gov.laa.ccms.data.model.ContactDetail;
import uk.gov.laa.ccms.data.model.OfficeDetail;
import uk.gov.laa.ccms.data.model.OutcomeResultLookupDetail;
import uk.gov.laa.ccms.data.model.OutcomeResultLookupValueDetail;
import uk.gov.laa.ccms.data.model.PriorAuthorityDetail;
import uk.gov.laa.ccms.data.model.PriorAuthorityTypeDetail;
import uk.gov.laa.ccms.data.model.PriorAuthorityTypeDetails;
import uk.gov.laa.ccms.data.model.ProviderDetail;
import uk.gov.laa.ccms.data.model.RelationshipToCaseLookupDetail;
import uk.gov.laa.ccms.data.model.RelationshipToCaseLookupValueDetail;
import uk.gov.laa.ccms.data.model.ScopeLimitationDetail;
import uk.gov.laa.ccms.data.model.ScopeLimitationDetails;
import uk.gov.laa.ccms.data.model.StageEndLookupDetail;
import uk.gov.laa.ccms.data.model.StageEndLookupValueDetail;
import uk.gov.laa.ccms.data.model.UserDetail;
import uk.gov.laa.ccms.soa.gateway.model.AssessmentResult;
import uk.gov.laa.ccms.soa.gateway.model.Award;
import uk.gov.laa.ccms.soa.gateway.model.CaseDetail;
import uk.gov.laa.ccms.soa.gateway.model.CaseDetails;
import uk.gov.laa.ccms.soa.gateway.model.CaseReferenceSummary;
import uk.gov.laa.ccms.soa.gateway.model.CategoryOfLaw;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetail;
import uk.gov.laa.ccms.soa.gateway.model.ContractDetails;
import uk.gov.laa.ccms.soa.gateway.model.CostLimitation;
import uk.gov.laa.ccms.soa.gateway.model.PriorAuthorityAttribute;
import uk.gov.laa.ccms.soa.gateway.model.ProceedingDetail;
import uk.gov.laa.ccms.soa.gateway.model.ScopeLimitation;

/**
 * Service class to handle Applications.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationService {

  private final SoaApiClient soaApiClient;

  private final EbsApiClient ebsApiClient;

  private final CaabApiClient caabApiClient;

  private final ProviderService providerService;

  private final ApplicationFormDataMapper applicationFormDataMapper;

  private final AddressFormDataMapper addressFormDataMapper;

  private final ApplicationMapper applicationMapper;

  private final CopyApplicationMapper copyApplicationMapper;

  private final LookupService lookupService;

  private final ResultDisplayMapper resultDisplayMapper;

  private final OpponentMapper opponentMapper;

  private final SearchConstants searchConstants;


  private static final String UPDATE_APPLICATION_APPLICATION_TYPE = "application-type";
  private static final String UPDATE_APPLICATION_CORRESPONDENCE_ADDRESS = "correspondence-address";
  private static final String UPDATE_APPLICATION_PROVIDER_DETAILS = "provider-details";



  /**
   * Performs a combined search of SOA cases and TDS applications based on provided search criteria.
   * Each result is mapped to a BaseApplication to summarise the details.
   *
   * @param caseSearchCriteria The search criteria to use when fetching cases.
   * @param loginId                The login identifier for the user.
   * @param userType               Type of the user (e.g., admin, user).
   * @return A List of BaseApplication.
   */
  public List<BaseApplication> getCases(
      final CaseSearchCriteria caseSearchCriteria,
      final String loginId,
      final String userType) throws TooManyResultsException {

    ReflectionUtils.nullifyStrings(caseSearchCriteria);

    final List<BaseApplication> searchResults = new ArrayList<>();

    // Only search for SOA Cases if the user hasn't selected status 'UNSUBMITTED'.
    if (!STATUS_UNSUBMITTED_ACTUAL_VALUE.equals(caseSearchCriteria.getStatus())) {
      // Set page and size to min and max respectively. Because we are combining 2 searches
      // we will have to return all records for pagination by the caller.
      final CaseDetails caseDetails = Optional.ofNullable(
              soaApiClient.getCases(
                  caseSearchCriteria,
                  loginId,
                  userType,
                  0,
                  searchConstants.getMaxSearchResultsCases()).block())
          .orElseThrow(() -> new CaabApplicationException("Failed to retrieve SOA Cases"));

      if (caseDetails.getTotalElements() > searchConstants.getMaxSearchResultsCases()) {
        throw new TooManyResultsException(
            String.format("Case Search returned %s results", caseDetails.getTotalElements()));
      }

      searchResults.addAll(caseDetails.getContent().stream()
          .map(applicationMapper::toBaseApplication)
          .toList());
    }

    // Now retrieve applications from the Transient Data Store
    final List<BaseApplication> tdsApplications = this.getTdsApplications(
        caseSearchCriteria,
        0,
        searchConstants.getMaxSearchResultsCases()).getContent();

    /*
     * TODO: Exclude (and remove) any Pending Applications where the SOA
     *  transaction has now completed.
     */
    // tdsApplications = pollPendingApplications(tdsApplications, data, ccmsUser);

    // Remove any duplicates (remove the TDS applications as they are amendments, keep the cases)
    tdsApplications.removeIf(
        app -> searchResults.stream().anyMatch(
                soaCase -> soaCase.getCaseReferenceNumber().equals(app.getCaseReferenceNumber())));

    // Now add the remaining TDS applications into the list
    searchResults.addAll(tdsApplications);

    // Final check of the number of results now that the two searches have been combined.
    if (searchResults.size() > searchConstants.getMaxSearchResultsCases()) {
      throw new TooManyResultsException(
          String.format("Case Search returned %s results", searchResults.size()));
    }

    // Sort the combined list by Case Reference
    searchResults.sort(Comparator.comparing(BaseApplication::getCaseReferenceNumber));

    return searchResults;
  }

  /**
   * Query for Applications in the TDS based on the supplied search criteria.
   *
   * @param caseSearchCriteria - the search criteria
   * @param page               - the page number
   * @param size               - the page size
   * @return ApplicationDetails containing a List of BaseApplication.
   */
  public ApplicationDetails getTdsApplications(
      final CaseSearchCriteria caseSearchCriteria,
      final Integer page,
      final Integer size) {

    return Optional.ofNullable(
        caabApiClient.getApplications(caseSearchCriteria, page, size).block())
        .orElseThrow(() -> new CaabApplicationException("Failed to query for applications"));
  }

  /**
   * Retrieve the full details of a Case.
   *
   * @param caseReferenceNumber The reference of the case to be retrieved.
   * @param loginId             The login identifier for the user.
   * @param userType            Type of the user (e.g., admin, user).
   * @return A Mono wrapping the CaseDetails.
   */
  public ApplicationDetail getCase(
      final String caseReferenceNumber,
      final String loginId,
      final String userType) {
    CaseDetail soaCase = Optional.ofNullable(
        soaApiClient.getCase(caseReferenceNumber, loginId, userType).block())
        .orElseThrow(() -> new CaabApplicationException(
            String.format("Failed to retrieve SOA Case with ref: %s", caseReferenceNumber)));

    return applicationMapper.toApplicationDetail(buildApplicationMappingContext(soaCase));
  }


  /**
   * Fetches a unique case reference.
   *
   * @param loginId  The login identifier for the user.
   * @param userType Type of the user (e.g., admin, user).
   * @return A Mono wrapping the CaseReferenceSummary.
   */
  public Mono<CaseReferenceSummary> getCaseReference(final String loginId, final String userType) {
    return soaApiClient.getCaseReference(loginId, userType);
  }

  /**
   * Create a draft Application in the CAAB's Transient Data Store.
   *
   * @param applicationFormData - The details of the Application to create
   * @param clientDetail - The client details
   * @param user - The related User.
   * @return a String containing the id of the application
   */
  public Mono<String> createApplication(
      final ApplicationFormData applicationFormData,
      final ClientDetail clientDetail,
      final UserDetail user)
      throws ParseException {
    Mono<ApplicationDetail> applicationMono;

    if (StringUtils.hasText(applicationFormData.getCopyCaseReferenceNumber())) {
      ApplicationDetail applicationToCopy = this.getCase(
          applicationFormData.getCopyCaseReferenceNumber(),
          user.getLoginId(),
          user.getUserType());

      applicationMono = copyApplication(applicationToCopy, clientDetail, user);
    } else {
      applicationMono = buildNewApplication(applicationFormData, clientDetail, user);
    }

    return applicationMono
        .flatMap(applicationDetail -> caabApiClient.createApplication(
            user.getLoginId(), applicationDetail));
  }

  protected Mono<ApplicationDetail> buildNewApplication(
      final ApplicationFormData applicationFormData,
      final ClientDetail clientDetail,
      final UserDetail user) throws ParseException {
    ApplicationType applicationType = new ApplicationTypeBuilder()
        .applicationType(
            applicationFormData.getApplicationTypeCategory(),
            applicationFormData.isDelegatedFunctions())
        .devolvedPowers(
            applicationFormData.isDelegatedFunctions(),
            applicationFormData.getDelegatedFunctionUsedDay(),
            applicationFormData.getDelegatedFunctionUsedMonth(),
            applicationFormData.getDelegatedFunctionUsedYear())
        .build();

    // get case reference Number, category of law value, contractual devolved powers,
    // amendment types
    Mono<Tuple4<CaseReferenceSummary,
        CategoryOfLawLookupValueDetail,
        ContractDetails,
        AmendmentTypeLookupDetail>> combinedResult =
        Mono.zip(
            this.getCaseReference(user.getLoginId(), user.getUserType()),
            lookupService.getCategoryOfLaw(applicationFormData.getCategoryOfLawId()),
            soaApiClient.getContractDetails(
                user.getProvider().getId(),
                applicationFormData.getOfficeId(),
                user.getLoginId(),
                user.getUserType()
            ),
            ebsApiClient.getAmendmentTypes(applicationType.getId())
        );

    return combinedResult.map(tuple -> {
      CaseReferenceSummary caseReferenceSummary = tuple.getT1();
      CategoryOfLawLookupValueDetail categoryOfLawLookup = tuple.getT2();
      ContractDetails contractDetails = tuple.getT3();
      AmendmentTypeLookupDetail amendmentTypes = tuple.getT4();

      return new ApplicationBuilder()
          .applicationType(applicationType)
          .caseReference(caseReferenceSummary)
          .provider(user)
          .client(clientDetail)
          .categoryOfLaw(applicationFormData.getCategoryOfLawId(), categoryOfLawLookup)
          .office(
              applicationFormData.getOfficeId(),
              user.getProvider().getOffices())
          .contractualDevolvedPower(
              contractDetails.getContracts(),
              applicationFormData.getCategoryOfLawId())
          .larScopeFlag(amendmentTypes)
          .status()
          .costStructure()
          .correspondenceAddress()
          .build();
    });
  }

  protected Mono<ApplicationDetail> copyApplication(
      final ApplicationDetail applicationToCopy,
      final ClientDetail clientDetail,
      final UserDetail user) {

    // get case reference Number, category of law value, contractual devolved powers,
    // amendment types
    Mono<Tuple3<CaseReferenceSummary,
        CategoryOfLawLookupValueDetail,
        RelationshipToCaseLookupDetail>> combinedResult =
        Mono.zip(
            this.getCaseReference(user.getLoginId(), user.getUserType()),
            lookupService.getCategoryOfLaw(applicationToCopy.getCategoryOfLaw().getId()),
            lookupService.getPersonToCaseRelationships());

    return combinedResult.map(tuple -> {
      String caseReferenceNumber = tuple.getT1().getCaseReferenceNumber();

      // Check whether the cost limit should be copied for the case's category of law
      Boolean copyCostLimit = tuple.getT2().getCopyCostLimit();
      BigDecimal requestedCostLimitation =
          Boolean.TRUE.equals(copyCostLimit)
              ? applicationToCopy.getCosts().getRequestedCostLimitation() : BigDecimal.ZERO;

      // Get a Map of RelationshipToCase by code, filtered for those with the 'copyParty'
      // flag set.
      RelationshipToCaseLookupDetail relationshipToCaseLookupDetail =
          tuple.getT3();
      Map<String, RelationshipToCaseLookupValueDetail> copyPartyRelationships =
          relationshipToCaseLookupDetail.getContent() != null
              ? relationshipToCaseLookupDetail.getContent().stream()
              .filter(RelationshipToCaseLookupValueDetail::getCopyParty)
              .collect(Collectors.toMap(
                  RelationshipToCaseLookupValueDetail::getCode, Function.identity()))
              : Collections.emptyMap();



      // Find the max cost limitation across the Proceedings, and set this as the
      // default cost limitation for the application.
      BigDecimal defaultCostLimitation = BigDecimal.ZERO;
      if (applicationToCopy.getProceedings() != null) {
        defaultCostLimitation = applicationToCopy.getProceedings().stream()
            .map(Proceeding::getCostLimitation)
            .max(Comparator.comparingDouble(BigDecimal::doubleValue))
            .orElse(BigDecimal.ZERO);
      }

      StringDisplayValue initialStatus = new StringDisplayValue()
          .id(STATUS_UNSUBMITTED_ACTUAL_VALUE)
          .displayValue(STATUS_UNSUBMITTED_ACTUAL_VALUE_DISPLAY);

      // Use a mapper to copy the relevant attributes
      ApplicationDetail application = copyApplicationMapper.copyApplication(
          applicationToCopy,
          caseReferenceNumber,
          initialStatus,
          clientDetail,
          requestedCostLimitation,
          defaultCostLimitation);

      // Clear the ebsId for an opponent if it is of type INDIVIDUAL AND it is NOT shared AND
      // the relationship to case for the opponent is of type Copy Party.
      if (application.getOpponents() != null) {
        application.getOpponents().stream()
            .filter(opponent -> OPPONENT_TYPE_INDIVIDUAL.equalsIgnoreCase(opponent.getType())
                && copyPartyRelationships.containsKey(opponent.getRelationshipToCase())
                && !opponent.getSharedInd())
            .forEach(opponent -> opponent.setEbsId(null));
      }

      return application;
    });
  }

  /**
   * Retrieves the case status lookup value that is eligible for copying.
   *
   * @return The CaseStatusLookupValueDetail representing the eligible case status for copying.
   */
  public CaseStatusLookupValueDetail getCopyCaseStatus() {
    CaseStatusLookupDetail caseStatusLookupDetail =
        lookupService.getCaseStatusValues(Boolean.TRUE).block();

    return Optional.ofNullable(caseStatusLookupDetail)
        .map(CaseStatusLookupDetail::getContent)
        .orElse(Collections.emptyList())
        .stream().findFirst().orElse(null);
  }

  public Mono<ApplicationDetail> getApplication(final String id) {
    return caabApiClient.getApplication(id);
  }


  /**
   * Retrieves the application Summary display values.
   *
   * @param id the identifier of the application to retrieve a summary for.
   * @return A Mono of ApplicationSummaryDisplay representing the case summary display values.
   */
  public Mono<ApplicationSummaryDisplay> getApplicationSummary(
      final String id) {

    final Mono<RelationshipToCaseLookupDetail> organisationRelationshipsMono =
        lookupService.getOrganisationToCaseRelationships();

    final Mono<RelationshipToCaseLookupDetail> personRelationshipsMono =
        lookupService.getPersonToCaseRelationships();

    final Mono<ApplicationDetail> applicationMono
        = caabApiClient.getApplication(id);

    return Mono.zip(organisationRelationshipsMono,
            personRelationshipsMono,
            applicationMono)
        .map(tuple -> {

          final List<RelationshipToCaseLookupValueDetail> organisationRelationships
              = tuple.getT1().getContent();
          final List<RelationshipToCaseLookupValueDetail> personsRelationships
              = tuple.getT2().getContent();
          final ApplicationDetail application = tuple.getT3();

          return new ApplicationSummaryBuilder(application.getAuditTrail())
              .clientFullName(
                  application.getClient().getFirstName(),
                  application.getClient().getSurname())
              .clientReferenceNumber(
                  application.getClient().getReference())
              .caseReferenceNumber(
                  application.getCaseReferenceNumber())
              .providerCaseReferenceNumber(
                  application.getProviderDetails().getProviderCaseReference())
              .applicationType(
                  application.getApplicationType())
              .providerDetails(
                  application.getProviderDetails().getProviderContact())
              .generalDetails(
                  application.getCorrespondenceAddress())
              .proceedingsAndCosts(
                  application.getProceedings(),
                  application.getPriorAuthorities(),
                  application.getCosts())
              .opponentsAndOtherParties(
                  application.getOpponents(),
                  organisationRelationships,
                  personsRelationships)
              .build();
        });
  }

  public ApplicationFormData getApplicationTypeFormData(final String id) {
    return caabApiClient.getApplicationType(id)
        .map(applicationFormDataMapper::toApplicationTypeFormData).block();
  }

  public ApplicationFormData getProviderDetailsFormData(final String id) {
    return caabApiClient.getProviderDetails(id)
        .map(applicationFormDataMapper::toApplicationProviderDetailsFormData).block();
  }

  public AddressFormData getCorrespondenceAddressFormData(final String id) {
    return caabApiClient.getCorrespondenceAddress(id)
        .map(addressFormDataMapper::toAddressFormData).block();
  }

  /**
   * Retrieves linked cases associated with a specific application id.
   * This method fetches a list of linked cases, transforms them into a
   * {@code ResultsDisplay<LinkedCaseResultRowDisplay>} format, and returns the result.
   *
   * @param applicationId The unique identifier for the cases to be retrieved.
   * @return A {@code ResultsDisplay<LinkedCaseResultRowDisplay>} containing the linked cases.
   */
  public ResultsDisplay<LinkedCaseResultRowDisplay> getLinkedCases(final String applicationId) {
    final ResultsDisplay<LinkedCaseResultRowDisplay> results = new ResultsDisplay<>();

    return caabApiClient.getLinkedCases(applicationId)
        .flatMapMany(Flux::fromIterable) // Convert to Flux<LinkedCase>
        .map(resultDisplayMapper::toLinkedCaseResultRowDisplay) // Map to ResultRowDisplay
        .collectList() // Collect into a List
        .map(list -> {
          results.setContent(list); // Set the content of ResultsDisplay
          return results; // Return the populated ResultsDisplay
        }).block();
  }

  /**
   * Retrieves the default scope limitation details for a given proceeding.
   * The scope limitation details are fetched based on the category of law, matter type, proceeding
   * code, level of service, and application type. If the application type is emergency or
   * substantive devolved powers, the method sets the emergency and emergency scope default flags
   * to true. Otherwise, it sets the scope default flag to true.
   *
   * @param categoryOfLaw The category of law.
   * @param matterType The type of the matter.
   * @param proceedingCode The code of the proceeding.
   * @param levelOfService The level of service.
   * @param applicationType The type of the application.
   * @return A Mono of ScopeLimitationDetails containing the default scope limitation details.
   */
  public Mono<ScopeLimitationDetails> getDefaultScopeLimitation(
      final String categoryOfLaw,
      final String matterType,
      final String proceedingCode,
      final String levelOfService,
      final String applicationType) {

    final ScopeLimitationDetail criteria = new ScopeLimitationDetail()
        .categoryOfLaw(categoryOfLaw)
        .matterType(matterType)
        .proceedingCode(proceedingCode)
        .levelOfService(levelOfService);

    // Could be possible to amend this if app type is SUBDP,
    // it could include both the emergency and non-emergency scope limitations by default
    // left as is per PUI code.
    if (EMERGENCY_APPLICATION_TYPE_CODES.contains(applicationType)
        || APP_TYPE_SUBSTANTIVE_DEVOLVED_POWERS.equals(applicationType)) {
      criteria.emergency(true);
      criteria.emergencyScopeDefault(true);
    } else {
      criteria.scopeDefault(true);
    }

    return lookupService.getScopeLimitationDetails(criteria);
  }

  /**
   * Calculates the maximum cost limitation for a proceeding.
   *
   * @param categoryOfLaw The category of law.
   * @param matterType The type of the matter.
   * @param proceedingCode The code of the proceeding.
   * @param levelOfService The level of service.
   * @param applicationType The type of the application.
   * @param scopeLimitations The list of scope limitations.
   * @return The maximum cost limitation for the proceeding.
   */
  public BigDecimal getProceedingCostLimitation(
      final String categoryOfLaw,
      final String matterType,
      final String proceedingCode,
      final String levelOfService,
      final String applicationType,
      final List<uk.gov.laa.ccms.caab.model.ScopeLimitation> scopeLimitations) {

    BigDecimal maxValue = new BigDecimal(0);
    final List<Float> costLimitations = new ArrayList<>();

    for (final uk.gov.laa.ccms.caab.model.ScopeLimitation scopeLimitation : scopeLimitations) {
      final ScopeLimitationDetail criteria = new ScopeLimitationDetail()
          .categoryOfLaw(categoryOfLaw)
          .matterType(matterType)
          .proceedingCode(proceedingCode)
          .levelOfService(levelOfService)
          .scopeLimitations(scopeLimitation.getScopeLimitation().getId());

      if (EMERGENCY_APPLICATION_TYPE_CODES.contains(applicationType)) {
        criteria.emergency(true);
      }

      Optional.ofNullable(lookupService.getScopeLimitationDetails(criteria).block())
          .orElseThrow(() -> new CaabApplicationException(
              "Failed to retrieve scope limitiation details"))
          .getContent()
          .stream()
          .findFirst()
          .map(ScopeLimitationDetail::getCostLimitation)
          .ifPresent(costLimitation -> costLimitations.add(costLimitation.floatValue()));

    }

    if (!costLimitations.isEmpty()) {
      maxValue = BigDecimal.valueOf(
          costLimitations
              .stream()
              .max(Float::compareTo)
              .orElse(null));
    }

    return maxValue;
  }

  /**
   * Determines the proceeding stage based on various parameters.
   *
   * @param categoryOfLaw The category of law.
   * @param matterType The type of the matter.
   * @param proceedingCode The code of the proceeding.
   * @param levelOfService The level of service.
   * @param scopeLimitations The list of scope limitations.
   * @param isAmendment Flag indicating if it's an amendment.
   * @return The proceeding stage as an Integer.
   */
  public Integer getProceedingStage(
      final String categoryOfLaw,
      final String matterType,
      final String proceedingCode,
      final String levelOfService,
      final List<uk.gov.laa.ccms.caab.model.ScopeLimitation> scopeLimitations,
      final boolean isAmendment) {

    // String existingStage = null;
    //todo - see GetDefaultScopeLimitation in pui
    //    if (isAmendment) {
    //
    //      // for (Proceeding caseProceeding : myCase.getProceedings()) {
    //      //   if (caseProceeding.getEbsId().equalsIgnoreCase(proceeding.getEbsId())) {
    //      //     existingStage = caseProceeding.getStage();
    //      //   }
    //      // }
    //      // see - getAmendmentProceedingStage
    //    }

    final List<List<Integer>> allStages = new ArrayList<>();
    final List<Integer> minStageList = new ArrayList<>();
    for (final uk.gov.laa.ccms.caab.model.ScopeLimitation scopeLimitation : scopeLimitations) {
      final ScopeLimitationDetail criteria = new ScopeLimitationDetail()
          .categoryOfLaw(categoryOfLaw)
          .matterType(matterType)
          .proceedingCode(proceedingCode)
          .levelOfService(levelOfService)
          .scopeLimitations(scopeLimitation.getScopeLimitation().getId());

      final List<Integer> stageList =
          Optional.ofNullable(lookupService.getScopeLimitationDetails(criteria).block())
              .orElseThrow(() -> new CaabApplicationException(
                  "Failed to retrieve scope limitation details"))
              .getContent()
              .stream()
              .map(ScopeLimitationDetail::getStage)
              .toList();

      allStages.add(stageList);
      minStageList.add(getMinValue(stageList));

      //common Stages stuff
      final List<Integer> commonStages = getCommonStages(allStages);
      if (!commonStages.isEmpty()) {
        return getMinValue(commonStages);
      }
    }

    return getMinValue(minStageList);
  }

  /**
   * Finds the common stages across all scope limitations.
   *
   * @param allStages A list of lists, where each inner list represents the stages of a scope
   *                  limitation.
   * @return A list of common stages across all scope limitations.
   */
  private List<Integer> getCommonStages(final List<List<Integer>> allStages) {
    return allStages.get(0).stream()
        .filter(stage -> allStages.stream().allMatch(scopeLimStages ->
            scopeLimStages.contains(stage)))
        .collect(Collectors.toList());
  }

  /**
   * Finds the minimum value in a collection of integers.
   *
   * @param values A collection of integer values.
   * @return The minimum integer value in the collection. Returns null if the collection is empty.
   */
  private Integer getMinValue(final Collection<Integer> values) {
    return values.stream()
        .min(Integer::compareTo)
        .orElse(null);
  }

  /**
   * Updates the lead proceeding for a specific application.
   * This method communicates with the CAAB API client to update the lead proceeding.
   *
   * @param applicationId The id of the application for which the lead proceeding should be updated.
   * @param newLeadProceedingId The id of the new lead proceeding.
   * @param user The user performing the operation, identified by {@code UserDetail}.
   */
  public void makeLeadProceeding(
      final String applicationId,
      final Integer newLeadProceedingId,
      final UserDetail user) {

    final List<Proceeding> proceedings = caabApiClient.getProceedings(applicationId).block();

    if (proceedings == null) {
      throw new CaabApplicationException(
          "No proceedings found for applicationId: " + applicationId);
    }

    // Find and update the current lead proceeding if it exists
    proceedings.stream()
        .filter(Proceeding::getLeadProceedingInd)
        .findFirst()
        .ifPresent(proceeding -> {
          proceeding.setLeadProceedingInd(false);
          caabApiClient.updateProceeding(
              proceeding.getId(),
              proceeding,
              user.getLoginId()).block();
        });

    // Set the new lead proceeding
    final Proceeding newLeadProceeding = proceedings.stream()
        .filter(proceeding -> proceeding.getId().equals(newLeadProceedingId))
        .findFirst()
        .orElseThrow(() ->
            new CaabApplicationException("Error: New lead proceeding not found with id: "
                + newLeadProceedingId));

    newLeadProceeding.setLeadProceedingInd(true);
    caabApiClient.updateProceeding(
        newLeadProceedingId,
        newLeadProceeding,
        user.getLoginId()).block();

    //TODO application opa means and merits reset
    //Requires a new endpoint to be added to the CAAB API
    //caabApiClient.updateApplication();
    //see application.setMeritsReassessmentReqdInd(true); in pui
  }

  /**
   * Removes a linked case from a primary case.
   * This method communicates with the CAAB API client to un-link a case identified by
   * {@code linkedCaseId} from a primary case identified by {@code id}.
   *
   * @param applicationId The id of the application for which linked cases should be retrieved
   * @param linkedCaseId The ID of the linked case to be removed.
   * @param user         The user performing the operation, identified by {@code UserDetail}.
   */
  public void removeLinkedCase(
      final String applicationId,
      final String linkedCaseId,
      final UserDetail user) {
    caabApiClient.removeLinkedCase(applicationId, linkedCaseId, user.getLoginId()).block();
  }

  /**
   * Updates a specific linked case with new data.
   * This method maps the provided {@code data} to a {@code LinkedCase} object and updates
   * the linked case identified by {@code linkedCaseId} in relation to the primary case
   * identified by {@code id}.
   *
   * @param applicationId The ID of the case related to the linked case.
   * @param linkedCaseId The ID of the linked case to be updated.
   * @param data         The new data for the linked case, encapsulated in
   *                     {@code LinkedCaseResultRowDisplay}.
   * @param user         The user performing the update, identified by {@code UserDetail}.
   */
  public void updateLinkedCase(
      final String applicationId,
      final String linkedCaseId,
      final LinkedCaseResultRowDisplay data,
      final UserDetail user) {

    final LinkedCase linkedCase = resultDisplayMapper.toLinkedCase(data);
    caabApiClient.updateLinkedCase(
        applicationId,
        linkedCaseId,
        linkedCase, 
        user.getLoginId()).block();
  }

  /**
   * Adds a linked case to an application.
   *
   * @param applicationId The ID of the application to link the case to.
   * @param data          The display data of the linked case.
   * @param user          The user performing the operation.
   */
  public void addLinkedCase(
      final String applicationId,
      final LinkedCaseResultRowDisplay data,
      final UserDetail user) {

    final LinkedCase linkedCase = resultDisplayMapper.toLinkedCase(data);
    caabApiClient.addLinkedCase(
        applicationId,
        linkedCase,
        user.getLoginId()).block();
  }

  /**
   * Patches an application's correspondence address in the CAAB's Transient Data Store.
   *
   * @param id the ID associated with the application
   * @param addressFormData the details of the Application to amend
   * @param user the related User.
   */
  public void updateCorrespondenceAddress(
      final String id,
      final AddressFormData addressFormData,
      final UserDetail user) {

    final Address correspondenceAddress = addressFormDataMapper.toAddress(addressFormData);

    caabApiClient.putApplication(
        id,
        user.getLoginId(),
        correspondenceAddress,
        UPDATE_APPLICATION_CORRESPONDENCE_ADDRESS).block();
  }

  /**
   * Patches an application's application type in the CAAB's Transient Data Store.
   *
   * @param id the ID associated with the application
   * @param applicationFormData the details of the Application to amend
   * @param user the related User.
   */
  public void updateApplicationType(
      final String id,
      final ApplicationFormData applicationFormData,
      final UserDetail user)
      throws ParseException {

    final ApplicationType applicationType = new ApplicationTypeBuilder()
        .applicationType(
            applicationFormData.getApplicationTypeCategory(),
            applicationFormData.isDelegatedFunctions())
        .devolvedPowers(
            applicationFormData.isDelegatedFunctions(),
            applicationFormData.getDelegatedFunctionUsedDay(),
            applicationFormData.getDelegatedFunctionUsedMonth(),
            applicationFormData.getDelegatedFunctionUsedYear())
        .devolvedPowersContractFlag(
            applicationFormData.getDevolvedPowersContractFlag())
        .build();

    caabApiClient.putApplication(
        id, user.getLoginId(), applicationType, UPDATE_APPLICATION_APPLICATION_TYPE).block();
  }

  /**
   * Patches an application's provider details in the CAAB's Transient Data Store.
   *
   * @param id the ID associated with the application
   * @param applicationFormData the details of the Application to amend
   * @param user the related User.
   */
  public void updateProviderDetails(
      final String id,
      final ApplicationFormData applicationFormData,
      final UserDetail user) {

    ProviderDetail provider = Optional.ofNullable(user.getProvider())
        .map(providerData -> providerService.getProvider(providerData.getId()).block())
        .orElseThrow(() -> new CaabApplicationException("Error retrieving provider"));

    ContactDetail feeEarner = providerService.getFeeEarnerByOfficeAndId(
        provider, applicationFormData.getOfficeId(), applicationFormData.getFeeEarnerId());
    ContactDetail supervisor = providerService.getFeeEarnerByOfficeAndId(
        provider, applicationFormData.getOfficeId(), applicationFormData.getSupervisorId());

    ContactDetail contactName = provider.getContactNames().stream()
        .filter(contactDetail -> contactDetail.getId().toString()
            .equals(applicationFormData.getContactNameId()))
        .findFirst()
        .orElseThrow(() -> new CaabApplicationException("Error retrieving contact name"));

    ApplicationProviderDetails providerDetails = new ApplicationProviderDetails()
        .provider(new IntDisplayValue()
            .id(provider.getId())
            .displayValue(provider.getName()))
        .office(new IntDisplayValue()
            .id(applicationFormData.getOfficeId())
            .displayValue(applicationFormData.getOfficeName()))
        .feeEarner(new StringDisplayValue()
            .id(feeEarner.getId().toString())
            .displayValue(feeEarner.getName()))
        .supervisor(new StringDisplayValue()
            .id(supervisor.getId().toString())
            .displayValue(supervisor.getName()))
        .providerContact(new StringDisplayValue()
            .id(contactName.getId().toString())
            .displayValue(contactName.getName()))
        .providerCaseReference(applicationFormData.getProviderCaseReference());

    caabApiClient.putApplication(
        id, user.getLoginId(), providerDetails, UPDATE_APPLICATION_PROVIDER_DETAILS).block();

  }

  /**
   * Fetches the opponents associated with a specific application id.
   * This method communicates with the CAAB API client to fetch the proceedings and
   * transforms them into a {@code ResultsDisplay<Opponent>} format.
   *
   * @param applicationId The id of the application for which proceedings should be retrieved.
   * @return A {@code Mono<ResultsDisplay<OpponentRowDisplay>>} containing the opponents.
   */
  public ResultsDisplay<OpponentRowDisplay> getOpponents(final String applicationId) {
    final ResultsDisplay<OpponentRowDisplay> results = new ResultsDisplay<>();

    // Retrieve the lookup data required to translate the opponent attributes to display data.
    Tuple4<CommonLookupDetail,
        RelationshipToCaseLookupDetail,
        RelationshipToCaseLookupDetail,
        CommonLookupDetail> combinedResponse = Optional.ofNullable(Mono.zip(
            lookupService.getContactTitles(),
            lookupService.getPersonToCaseRelationships(),
            lookupService.getOrganisationToCaseRelationships(),
            lookupService.getRelationshipsToClient()).block())
        .orElseThrow(() -> new CaabApplicationException("Failed to retrieve lookup data"));

    // Get the list of opponents for the application, and transform to display model.
    return caabApiClient.getOpponents(applicationId)
        .flatMapMany(Flux::fromIterable) // Convert to Flux<LinkedCase>
        .map(opponent ->
          buildOpponentRowDisplay(
              opponent,
              combinedResponse.getT1(),
              combinedResponse.getT2(),
              combinedResponse.getT3(),
              combinedResponse.getT4())
        )
        .collectList() // Collect into a List
        .map(list -> {
          results.setContent(list); // Set the content of ResultsDisplay
          return results; // Return the populated ResultsDisplay
        }).block();
  }

  /**
   * Add a new Opponent to an application based on the supplied form data.
   *
   * @param applicationId - the id of the application.
   * @param opponentFormData - the opponent form data.
   * @param userDetail - the user related user.
   */
  public void addOpponent(
      final String applicationId,
      final OpponentFormData opponentFormData,
      final UserDetail userDetail) {

    Opponent opponent = opponentMapper.toOpponent(opponentFormData);

    // Set the remaining flags on the opponent based on the application state.
    ApplicationDetail application =
        Optional.ofNullable(this.getApplication(applicationId).block())
            .orElseThrow(() -> new CaabApplicationException("Failed to retrieve application"));

    opponent.setAppMode(application.getAppMode());
    opponent.setAmendment(application.getAmendment());

    caabApiClient.addOpponent(
        applicationId,
        opponent,
        userDetail.getLoginId()).block();
  }

  /**
   * Build an OpponentRowDisplay for the provided Opponent.
   * Codes will be translated to their display value depending on the type of opponent.
   *
   * @param opponent - the opponent
   * @param contactTitles - a lookup of all contact titles
   * @param personRelationshipsToCase - a lookup of all person relationships to case
   * @param orgRelationshipsToCase - a lookup of all organisation relationships to case
   * @param relationshipsToClient - a lookup of all relationships to client
   * @return OpponentRowDisplay for the Opponent
   */
  protected OpponentRowDisplay buildOpponentRowDisplay(
      final Opponent opponent,
      final CommonLookupDetail contactTitles,
      final RelationshipToCaseLookupDetail personRelationshipsToCase,
      final RelationshipToCaseLookupDetail orgRelationshipsToCase,
      final CommonLookupDetail relationshipsToClient) {

    boolean isOrganisation = OPPONENT_TYPE_ORGANISATION.equals(opponent.getType());

    // Build a name for the opponent depending on the opponent type.
    String partyName = isOrganisation ? opponent.getOrganisationName()
        : toIndividualOpponentPartyName(opponent, contactTitles);

    // Look up the relationship to case display value depending on opponent type.
    List<RelationshipToCaseLookupValueDetail> relationships = isOrganisation
        ? orgRelationshipsToCase.getContent() : personRelationshipsToCase.getContent();

    String relationshipToCase = relationships.stream()
        .filter(relationship -> relationship.getCode().equals(opponent.getRelationshipToCase()))
        .findFirst()
        .map(RelationshipToCaseLookupValueDetail::getDescription)
        .orElse(opponent.getRelationshipToCase());

    // Look up the relationship to client display value.
    String relationshipToClient = relationshipsToClient.getContent().stream()
        .filter(relationship -> relationship.getCode().equals(opponent.getRelationshipToClient()))
        .findFirst()
        .map(CommonLookupValueDetail::getDescription)
        .orElse(opponent.getRelationshipToClient());

    return OpponentRowDisplay.builder()
        .id(opponent.getId())
        .partyName(partyName)
        .partyType(opponent.getType())
        .relationshipToCase(relationshipToCase)
        .relationshipToClient(relationshipToClient)
        .editable(isOrganisation || OPPONENT_TYPE_INDIVIDUAL.equals(opponent.getType()))
        .deletable(opponent.getDeleteInd())
        .build();
  }

  /**
   * Build the full name for an Individual Opponent.
   *
   * @param opponent - the opponent
   * @param contactTitles - a lookup of all contact titles
   * @return The opponent's full name
   */
  protected String toIndividualOpponentPartyName(final Opponent opponent,
      final CommonLookupDetail contactTitles) {
    StringBuilder builder = new StringBuilder();

    if (StringUtils.hasText(opponent.getTitle())) {
      // Lookup the display value for the contact title
      final String titleDisplayValue = contactTitles.getContent().stream()
          .filter(title -> title.getCode().equals(opponent.getTitle()))
          .findFirst()
          .map(CommonLookupValueDetail::getDescription)
          .orElse(opponent.getTitle());
      builder.append(titleDisplayValue);
    }

    if (StringUtils.hasText(opponent.getFirstName())) {
      if (!builder.isEmpty()) {
        builder.append(" ");
      }
      builder.append(opponent.getFirstName());
    }

    if (StringUtils.hasText(opponent.getSurname())) {
      if (!builder.isEmpty()) {
        builder.append(" ");
      }
      builder.append(opponent.getSurname());
    }

    return builder.isEmpty() ? "undefined" : builder.toString();
  }

  /**
   * Before a CaseDetail can be mapped to a CAAB ApplicationDetail further lookup
   * data and calculations need to be performed. This method builds a wrapper object to
   * hold all the required data for the mapping.
   *
   * @param soaCase - the SOA CaseDetail.
   * @return a Mono containing an ApplicationMappingContext for the CaseDetail.
   */
  protected ApplicationMappingContext buildApplicationMappingContext(final CaseDetail soaCase) {
    uk.gov.laa.ccms.soa.gateway.model.ApplicationDetails soaApplicationDetails =
        soaCase.getApplicationDetails();

    uk.gov.laa.ccms.soa.gateway.model.ProviderDetail soaProvider =
        soaApplicationDetails.getProviderDetails();

    // Determine whether all the proceedings in the soaCase are at status DRAFT
    boolean caseWithOnlyDraftProceedings =
        soaApplicationDetails.getProceedings() != null
            && soaApplicationDetails.getProceedings().stream().allMatch(
                proceedingDetail -> STATUS_DRAFT.equalsIgnoreCase(proceedingDetail.getStatus()));

    // Query for the various lookups needed to populate the mapping contexts
    Tuple6<ProviderDetail,
        CommonLookupDetail,
        CommonLookupDetail,
        CommonLookupDetail,
        CommonLookupDetail,
        PriorAuthorityTypeDetails> lookupTuple = Optional.ofNullable(
        Mono.zip(
            providerService.getProvider(Integer.parseInt(soaProvider.getProviderFirmId())),
            lookupService.getMatterTypes(),
            lookupService.getLevelsOfService(),
            lookupService.getClientInvolvementTypes(),
            lookupService.getScopeLimitations(),
            lookupService.getPriorAuthorityTypes()).block())
        .orElseThrow(() -> new CaabApplicationException(
            "Failed to query lookup data for Application mapping"));

    ProviderDetail providerDetail = lookupTuple.getT1();
    Map<String, CommonLookupValueDetail> matterTypes = toCommonValueMap(lookupTuple.getT2());
    Map<String, CommonLookupValueDetail> levelsOfService = toCommonValueMap(lookupTuple.getT3());
    Map<String, CommonLookupValueDetail> clientInvTypes = toCommonValueMap(lookupTuple.getT4());
    Map<String, CommonLookupValueDetail> scopeLimitations = toCommonValueMap(lookupTuple.getT5());
    PriorAuthorityTypeDetails priorAuthorityTypes = lookupTuple.getT6();

    // Lookup the application type if the SOA Case has a Certificate Type
    CommonLookupValueDetail applicationType = soaCase.getCertificateType() != null
        ? Optional.ofNullable(lookupService.getApplicationType(soaCase.getCertificateType())
            .block()).orElseThrow(
                () -> new CaabApplicationException(
                  String.format("Failed to retrieve applicationtype with code: %s",
                      soaCase.getCertificateType()))) : null;

    // Find the correct provider office.
    OfficeDetail providerOffice = providerDetail.getOffices().stream()
        .filter(officeDetail -> soaProvider.getProviderOfficeId().equals(
            String.valueOf(officeDetail.getId())))
        .findAny()
        .orElseThrow(() -> new CaabApplicationException(
            String.format("Failed to find Office with id: %s",
                soaProvider.getProviderOfficeId())));

    // Get the Fee Earners for the relevant office, and Map them by contact id.
    Map<Integer, ContactDetail> feeEarnerById =
        providerOffice.getFeeEarners().stream().collect(
            Collectors.toMap(ContactDetail::getId, Function.identity()));

    // Get the correct Supervisor and Fee Earner for this Provider.
    ContactDetail supervisorContact = feeEarnerById.get(Integer.valueOf(
        soaProvider.getSupervisorContactId()));

    ContactDetail feeEarnerContact = feeEarnerById.get(Integer.valueOf(
        soaProvider.getFeeEarnerContactId()));

    // Set the DevolvedPowers for the Application based on the ApplicationAmendmentType.
    boolean isDevolvedPowers =
        APP_TYPE_EMERGENCY_DEVOLVED_POWERS.equalsIgnoreCase(
            soaApplicationDetails.getApplicationAmendmentType())
            || APP_TYPE_SUBSTANTIVE_DEVOLVED_POWERS.equalsIgnoreCase(
            soaApplicationDetails.getApplicationAmendmentType());
    Pair<Boolean, Date> devolvedPowersInfo = Pair.of(
        isDevolvedPowers,
        isDevolvedPowers ? soaApplicationDetails.getDevolvedPowersDate() : null);

    // Calculate the CurrentProviderBilledAmount for the Application's Costs.
    BigDecimal currentProviderBilledAmount = BigDecimal.ZERO;
    CategoryOfLaw categoryOfLaw = soaApplicationDetails.getCategoryOfLaw();
    if (categoryOfLaw.getCostLimitations() != null
        && categoryOfLaw.getTotalPaidToDate() != null) {
      // Add the total amount billed across all cost entries.
      BigDecimal totalProviderAmount = categoryOfLaw.getCostLimitations().stream()
          .map(CostLimitation::getPaidToDate)
          .reduce(BigDecimal.ZERO, BigDecimal::add);

      currentProviderBilledAmount =
          categoryOfLaw.getTotalPaidToDate().subtract(totalProviderAmount);
    }

    // Find the most recent Assessments
    AssessmentResult meansAssessment = getMostRecentAssessment(
        soaApplicationDetails.getMeansAssesments());

    AssessmentResult meritsAssessment = getMostRecentAssessment(
        soaApplicationDetails.getMeritsAssesments());

    /*
     * Split the proceeding list based on status, and build a ProceedingMappingContext
     * for each to hold the necessary lookup data.
     */
    List<ProceedingMappingContext> amendmentProceedingsInEbs =
        soaApplicationDetails.getProceedings() != null
            ? soaApplicationDetails.getProceedings().stream()
            .filter(proceedingDetail -> !caseWithOnlyDraftProceedings
                && STATUS_DRAFT.equalsIgnoreCase(proceedingDetail.getStatus()))
            .map(proceedingDetail -> Optional.ofNullable(
                    buildProceedingMappingContext(
                        proceedingDetail,
                        soaCase,
                        matterTypes,
                        levelsOfService,
                        clientInvTypes,
                        scopeLimitations))
                .orElseThrow(() -> new CaabApplicationException(
                    "Failed to build mapping context")))
            .toList() : Collections.emptyList();

    List<ProceedingMappingContext> proceedings =
        soaApplicationDetails.getProceedings() != null
            ? soaApplicationDetails.getProceedings().stream()
            .filter(proceedingDetail -> caseWithOnlyDraftProceedings
                || !STATUS_DRAFT.equalsIgnoreCase(proceedingDetail.getStatus()))
            .map(proceedingDetail -> Optional.ofNullable(
                    buildProceedingMappingContext(
                        proceedingDetail,
                        soaCase,
                        matterTypes,
                        levelsOfService,
                        clientInvTypes,
                        scopeLimitations))
                .orElseThrow(() -> new CaabApplicationException(
                    "Failed to build mapping context")))
            .toList() : Collections.emptyList();

    // Build a mapping context for each Prior Authority in the application
    List<PriorAuthorityMappingContext> priorAuthorities =
        soaCase.getPriorAuthorities() != null
            ? soaCase.getPriorAuthorities().stream()
            .map(priorAuthority ->
                buildPriorAuthorityMappingContext(priorAuthority, priorAuthorityTypes))
            .toList() : Collections.emptyList();

    // Build a mapping context for the case outcome
    CaseOutcomeMappingContext caseOutcomeMappingContext = buildCaseOutcomeMappingContext(
        soaCase,
        Stream.concat(amendmentProceedingsInEbs.stream(), proceedings.stream()).toList());

    return ApplicationMappingContext.builder()
        .soaCaseDetail(soaCase)
        .applicationType(applicationType)
        .providerDetail(providerDetail)
        .providerOffice(providerOffice)
        .supervisorContact(supervisorContact)
        .feeEarnerContact(feeEarnerContact)
        .caseWithOnlyDraftProceedings(caseWithOnlyDraftProceedings)
        .devolvedPowers(devolvedPowersInfo)
        .amendmentProceedingsInEbs(amendmentProceedingsInEbs)
        .proceedings(proceedings)
        .meansAssessment(meansAssessment)
        .meritsAssessment(meritsAssessment)
        .priorAuthorities(priorAuthorities)
        .caseOutcome(caseOutcomeMappingContext)
        .currentProviderBilledAmount(currentProviderBilledAmount)
        .build();
  }

  protected ProceedingMappingContext buildProceedingMappingContext(
      final ProceedingDetail soaProceeding,
      final CaseDetail soaCase,
      final Map<String, CommonLookupValueDetail> matterTypeLookups,
      final Map<String, CommonLookupValueDetail> levelOfServiceLookups,
      final Map<String, CommonLookupValueDetail> clientInvolvementLookups,
      final Map<String, CommonLookupValueDetail> scopeLimitationLookups) {

    Tuple2<uk.gov.laa.ccms.data.model.ProceedingDetail,
                CommonLookupValueDetail> lookupTuple = Optional.ofNullable(
        Mono.zip(ebsApiClient.getProceeding(soaProceeding.getProceedingType()),
            lookupService.getProceedingStatus(soaProceeding.getStatus())).block())
        .orElseThrow(() -> new CaabApplicationException(
            "Failed to retrieve lookup data for Proceeding"));

    // Look up the Proceeding info from the EbsApi
    uk.gov.laa.ccms.data.model.ProceedingDetail proceedingLookup = lookupTuple.getT1();

    // Look up the Proceeding Status display info
    CommonLookupValueDetail proceedingStatusLookup = lookupTuple.getT2();

    // Calculate the overall cost limitation for this proceeding
    BigDecimal proceedingCostLimitation =
        this.calculateProceedingCostLimitation(soaProceeding, soaCase);

    // Build a List of pairs of Scope Limitation and associated lookup
    List<Pair<ScopeLimitation, CommonLookupValueDetail>> scopeLimitations =
        soaProceeding.getScopeLimitations().stream()
            .map(scopeLimitation -> Pair.of(
                scopeLimitation,
                scopeLimitationLookups.get(scopeLimitation.getScopeLimitation())))
            .toList();

    ProceedingMappingContext.ProceedingMappingContextBuilder contextBuilder =
        ProceedingMappingContext.builder()
            .soaProceeding(soaProceeding)
            .proceedingLookup(proceedingLookup)
            .proceedingStatusLookup(proceedingStatusLookup)
            .proceedingCostLimitation(proceedingCostLimitation)
            .matterType(matterTypeLookups.get(soaProceeding.getMatterType()))
            .levelOfService(levelOfServiceLookups.get(soaProceeding.getLevelOfService()))
            .clientInvolvement(
                clientInvolvementLookups.get(soaProceeding.getClientInvolvementType()))
            .scopeLimitations(scopeLimitations);

    this.addProceedingOutcomeContext(contextBuilder, soaProceeding);

    return contextBuilder.build();
  }

  protected void addProceedingOutcomeContext(
      final ProceedingMappingContext.ProceedingMappingContextBuilder contextBuilder,
      final ProceedingDetail soaProceeding) {

    if (soaProceeding.getOutcome() == null) {
      return; // Nothing to add
    }

    // Lookup extra data relating to the Proceeding Outcome
    Tuple3<CommonLookupDetail,
        OutcomeResultLookupDetail,
        StageEndLookupDetail> combinedOutcomeResults = Optional.ofNullable(Mono.zip(
            lookupService.getCourts(
                soaProceeding.getOutcome().getCourtCode()),
            lookupService.getOutcomeResults(soaProceeding.getProceedingType(),
                soaProceeding.getOutcome().getResult()),
            lookupService.getStageEnds(soaProceeding.getProceedingType(),
                soaProceeding.getOutcome().getStageEnd())).block())
        .orElseThrow(() -> new CaabApplicationException("Failed to query lookup data"));

    /*
     * Only use the looked up Court data if we got a single match.
     * Otherwise, default to the court code for display.
     */
    CommonLookupValueDetail courtLookup =
        combinedOutcomeResults.getT1().getContent().size() == 1
        ? combinedOutcomeResults.getT1().getContent().get(0) :
        new CommonLookupValueDetail()
            .code(soaProceeding.getOutcome().getCourtCode())
            .description(soaProceeding.getOutcome().getCourtCode());

    // Use the outcome result display data, if we have it.
    OutcomeResultLookupValueDetail outcomeResultLookup =
        !combinedOutcomeResults.getT2().getContent().isEmpty()
        ? combinedOutcomeResults.getT2().getContent().get(0) : null;

    // Lookup the stage end display value.
    StageEndLookupValueDetail stageEndLookup =
        !combinedOutcomeResults.getT3().getContent().isEmpty()
        ? combinedOutcomeResults.getT3().getContent().get(0) : null;

    // Update the builder with outcome-related lookup data
    contextBuilder.courtLookup(courtLookup)
        .outcomeResultLookup(outcomeResultLookup)
        .stageEndLookup(stageEndLookup);
  }

  /**
   * Build a mapping context to hold a SOA PriorAuthority and associated
   * lookup data.
   *
   * @param soaPriorAuthority - the PriorAuthority to map.
   * @param priorAuthorityTypes - all PriorAuthorityTypes.
   * @return a PriorAuthorityMappingContext containing all data to support mapping
   *     to a CAAB PriorAuthority.
   */
  protected PriorAuthorityMappingContext buildPriorAuthorityMappingContext(
      final uk.gov.laa.ccms.soa.gateway.model.PriorAuthority soaPriorAuthority,
      final PriorAuthorityTypeDetails priorAuthorityTypes) {

    // Find the correct PriorAuthorityType lookup
    PriorAuthorityTypeDetail priorAuthorityType =
        priorAuthorityTypes.getContent().stream()
        .filter(priorAuthorityTypeDetail -> priorAuthorityTypeDetail.getCode().equals(
            soaPriorAuthority.getPriorAuthorityType()))
        .findFirst()
        .orElseThrow(() -> new CaabApplicationException(
            String.format("Failed to find PriorAuthorityType with code: %s",
                soaPriorAuthority.getPriorAuthorityType())));

    // Build a Map of PriorAuthorityDetail keyed on code
    Map<String, PriorAuthorityDetail> priorAuthDetailMap =
        priorAuthorityType.getPriorAuthorities().stream().collect(
            Collectors.toMap(PriorAuthorityDetail::getCode, Function.identity()));

    // Build a List of priorAuthorityDetails paired with the common lookup for display info.
    List<Pair<PriorAuthorityDetail, CommonLookupValueDetail>> priorAuthorityDetails =
        soaPriorAuthority.getDetails().stream()
            .map(priorAuthorityAttribute -> {
              PriorAuthorityDetail priorAuthorityDetail =
                  priorAuthDetailMap.get(priorAuthorityAttribute.getName());
              return Pair.of(
                  priorAuthorityDetail,
                  getPriorAuthLookup(priorAuthorityDetail, priorAuthorityAttribute));
            })
            .toList();

    return PriorAuthorityMappingContext.builder()
        .soaPriorAuthority(soaPriorAuthority)
        .priorAuthorityTypeLookup(priorAuthorityType)
        .items(priorAuthorityDetails)
        .build();
  }

  private CommonLookupValueDetail getPriorAuthLookup(
      final PriorAuthorityDetail priorAuthorityDetail,
      final PriorAuthorityAttribute priorAuthorityAttribute) {
    String description;

    // If this attribute is of type LOV, lookup the corresponding LOV record to get the
    // display value.
    if (priorAuthorityDetail != null
        && REFERENCE_DATA_ITEM_TYPE_LOV.equals(priorAuthorityDetail.getDataType())) {
      description = Optional.ofNullable(lookupService.getCommonValue(
          priorAuthorityDetail.getLovCode(), priorAuthorityAttribute.getValue()).block())
          .map(CommonLookupValueDetail::getDescription)
          .orElseThrow(() -> new CaabApplicationException(
              String.format("Failed to find common value with code: %s",
                  priorAuthorityAttribute.getValue())));
    } else {
      description = priorAuthorityAttribute.getValue();
    }

    return new CommonLookupValueDetail()
        .code(priorAuthorityAttribute.getValue())
        .description(description);
  }

  protected CaseOutcomeMappingContext buildCaseOutcomeMappingContext(
      final CaseDetail soaCase,
      final List<ProceedingMappingContext> proceedingMappingContexts) {
    // Look up all Award Types and map by their code
    Map<String, AwardTypeLookupValueDetail> awardTypes =
        Optional.ofNullable(lookupService.getAwardTypes().block())
            .map(AwardTypeLookupDetail::getContent)
            .orElseThrow(() -> new CaabApplicationException("Failed to retrieve AwardTypes"))
            .stream().collect(
                Collectors.toMap(AwardTypeLookupValueDetail::getCode, Function.identity()));

    // Split the soa Awards into separate lists based on their award type.
    List<Award> costAwards = null;
    List<Award> financialAwards = null;
    List<Award> landAwards = null;
    List<Award> otherAssetAwards = null;

    if (soaCase.getAwards() != null) {
      costAwards = soaCase.getAwards().stream()
          .filter(award -> AWARD_TYPE_COST.equals(findAwardType(awardTypes, award)))
          .toList();

      financialAwards = soaCase.getAwards().stream()
          .filter(award -> AWARD_TYPE_FINANCIAL.equals(findAwardType(awardTypes, award)))
          .toList();

      landAwards = soaCase.getAwards().stream()
          .filter(award -> AWARD_TYPE_LAND.equals(findAwardType(awardTypes, award)))
          .toList();

      otherAssetAwards = soaCase.getAwards().stream()
          .filter(award -> AWARD_TYPE_OTHER_ASSET.equals(findAwardType(awardTypes, award)))
          .toList();
    }

    return CaseOutcomeMappingContext.builder()
        .soaCase(soaCase)
        .costAwards(costAwards)
        .financialAwards(financialAwards)
        .landAwards(landAwards)
        .otherAssetAwards(otherAssetAwards)
        .proceedingOutcomes(proceedingMappingContexts)
        .build();
  }

  protected BigDecimal calculateProceedingCostLimitation(
      final ProceedingDetail proceeding,
      final CaseDetail soaCase) {
    BigDecimal maxCostLimitation = BigDecimal.ZERO;
    if (soaCase.getApplicationDetails().getCategoryOfLaw() != null
        && proceeding.getMatterType() != null
        && proceeding.getProceedingType() != null
        && proceeding.getLevelOfService() != null
        && proceeding.getScopeLimitations() != null
        && !proceeding.getScopeLimitations().isEmpty()) {

      final String applicationType = soaCase.getApplicationDetails().getApplicationAmendmentType();
      boolean isEmergency = APP_TYPE_EMERGENCY.equalsIgnoreCase(applicationType)
          || APP_TYPE_EMERGENCY_DEVOLVED_POWERS.equalsIgnoreCase(applicationType);

      ScopeLimitationDetail searchCriteria = new ScopeLimitationDetail()
          .categoryOfLaw(soaCase.getApplicationDetails().getCategoryOfLaw().getCategoryOfLawCode())
          .matterType(proceeding.getMatterType())
          .proceedingCode(proceeding.getProceedingType())
          .levelOfService(proceeding.getLevelOfService())
          .emergency(isEmergency);

      for (ScopeLimitation limitation :
          proceeding.getScopeLimitations()) {
        searchCriteria.setScopeLimitations(limitation.getScopeLimitation());
        BigDecimal costLimitation = lookupService.getScopeLimitationDetails(searchCriteria)
            .map(scopeLimitationDetails -> scopeLimitationDetails.getContent() != null
                ? scopeLimitationDetails.getContent().stream()
                .findFirst()
                .map(scopeLimitationDetail ->
                    isEmergency ? scopeLimitationDetail.getEmergencyCostLimitation()
                        : scopeLimitationDetail.getCostLimitation())
                .orElse(BigDecimal.ZERO) : BigDecimal.ZERO)
            .block();

        maxCostLimitation = maxCostLimitation.max(costLimitation);
      }
    }

    return maxCostLimitation;
  }

  private Map<String, CommonLookupValueDetail> toCommonValueMap(
      final CommonLookupDetail commonLookupDetail) {
    if (commonLookupDetail == null) {
      throw new CaabApplicationException("Failed to lookup common values");
    }

    return commonLookupDetail.getContent().stream().collect(
        Collectors.toMap(CommonLookupValueDetail::getCode, Function.identity()));
  }

  private AssessmentResult getMostRecentAssessment(final List<AssessmentResult> assessmentResults) {
    return assessmentResults != null ? assessmentResults.stream()
        .max(Comparator.comparing(uk.gov.laa.ccms.soa.gateway.model.AssessmentResult::getDate,
            Comparator.nullsFirst(Comparator.naturalOrder())))
        .orElse(null) : null;
  }

  private String findAwardType(
      final Map<String, AwardTypeLookupValueDetail> awardTypes, final Award award) {
    return Optional.ofNullable(awardTypes.get(award.getAwardType()))
        .map(AwardTypeLookupValueDetail::getAwardType)
        .orElseThrow(() -> new CaabApplicationException(
            String.format("Failed to find AwardType with code: %s", award.getAwardType())));
  }

  /**
   * Prepares the proceeding summary for a specific application.
   *
   * @param id The ID of the application.
   * @param application The application details.
   * @param user The user performing the operation.
   */
  public void prepareProceedingSummary(
      final String id,
      final ApplicationDetail application,
      final UserDetail user) {

    setCostLimitations(application);

    if (Boolean.FALSE.equals(application.getAmendment())
        && application.getCosts().getRequestedCostLimitation() == null) {
      application.getCosts()
          .setRequestedCostLimitation(application.getCosts().getDefaultCostLimitation());
    }

    caabApiClient.updateCostStructure(id, application.getCosts(), user.getLoginId()).block();
  }

  private void setCostLimitations(final ApplicationDetail application) {
    BigDecimal defaultCostLimitation = new BigDecimal("0.00");

    final BigDecimal currentDefault = application.getCosts().getDefaultCostLimitation();
    final BigDecimal currentRequested = application.getCosts().getRequestedCostLimitation();

    final boolean costManuallyChanged = currentDefault != null && currentRequested != null
        && currentDefault.compareTo(currentRequested) != 0;
    for (final Proceeding proceeding : application.getProceedings()) {
      if (proceeding.getCostLimitation() != null
          && defaultCostLimitation.compareTo(proceeding.getCostLimitation()) < 0) {
        defaultCostLimitation = proceeding.getCostLimitation();
      }
    }

    if (defaultCostLimitation.scale() < 2) {
      defaultCostLimitation = defaultCostLimitation.setScale(2);
    }

    application.getCosts().setDefaultCostLimitation(defaultCostLimitation);

    if (application.getCosts().getRequestedCostLimitation() != null && !application.getAmendment()
        && !costManuallyChanged) {
      application.getCosts().setRequestedCostLimitation(defaultCostLimitation);
    } else if (application.getCosts().getRequestedCostLimitation() == null) {
      application.getCosts().setRequestedCostLimitation(defaultCostLimitation);
    }
  }

  /**
   * Retrieves details for a given prior authority type.
   *
   * @param priorAuthorityType the type of the prior authority to retrieve details for.
   * @return the detail of the specified prior authority type or null if not found.
   */
  public PriorAuthorityTypeDetail getPriorAuthorityTypeDetail(final String priorAuthorityType) {
    return lookupService.getPriorAuthorityTypes(priorAuthorityType, null)
        .block()
        .getContent()
        .stream()
        .findFirst()
        .orElse(null);
  }

  /**
   * Adds a proceeding associated to a specific application.
   *
   * @param applicationId the ID of the application to which the proceeding is added
   * @param proceeding the proceeding to add
   * @param user the user details initiating the action
   */
  public void addProceeding(
      final String applicationId,
      final Proceeding proceeding,
      final UserDetail user) {
    caabApiClient.addProceeding(applicationId, proceeding, user.getLoginId()).block();
  }

  /**
   * Updates a specified proceeding.
   *
   * @param proceeding the proceeding to update
   * @param user the user details initiating the update
   */
  public void updateProceeding(
      final Proceeding proceeding,
      final UserDetail user) {
    caabApiClient.updateProceeding(proceeding.getId(), proceeding, user.getLoginId()).block();
  }

  /**
   * Deletes a specified proceeding.
   *
   * @param proceedingId the ID of the proceeding to delete
   * @param user the user details initiating the deletion
   */
  public void deleteProceeding(
      final Integer proceedingId,
      final UserDetail user) {
    caabApiClient.deleteProceeding(proceedingId, user.getLoginId()).block();
  }

  /**
   * Retrieves scope limitations for a specified proceeding.
   *
   * @param proceedingId the ID of the proceeding
   * @return List of scope limitations associated with the proceeding
   */
  public List<uk.gov.laa.ccms.caab.model.ScopeLimitation> getScopeLimitations(
      final Integer proceedingId) {
    return caabApiClient.getScopeLimitations(proceedingId).block();
  }

  /**
   * Updates the cost structure for a specified application.
   *
   * @param applicationId the ID of the application to update
   * @param costStructure the new cost structure details
   * @param user the user details initiating the update
   */
  public void updateCostStructure(
      final String applicationId,
      final CostStructure costStructure,
      final UserDetail user) {
    caabApiClient.updateCostStructure(
        applicationId,
        costStructure,
        user.getLoginId()).block();
  }

  /**
   * Adds a priorAuthority associated to a specific application.
   *
   * @param applicationId the ID of the application to which the priorAuthority is added
   * @param priorAuthority the priorAuthority to add
   * @param user the user details initiating the action
   */
  public void addPriorAuthority(
      final String applicationId,
      final PriorAuthority priorAuthority,
      final UserDetail user) {
    caabApiClient.addPriorAuthority(applicationId, priorAuthority, user.getLoginId()).block();
  }

  /**
   * Updates a specified priorAuthority.
   *
   * @param priorAuthority the priorAuthority to update
   * @param user the user details initiating the update
   */
  public void updatePriorAuthority(
      final PriorAuthority priorAuthority,
      final UserDetail user) {
    caabApiClient.updatePriorAuthority(priorAuthority.getId(), priorAuthority,
        user.getLoginId()).block();
  }

  /**
   * Deletes a specified priorAuthority.
   *
   * @param priorAuthorityId the ID of the priorAuthority to delete
   * @param user the user details initiating the deletion
   */
  public void deletePriorAuthority(
      final Integer priorAuthorityId,
      final UserDetail user) {
    caabApiClient.deletePriorAuthority(priorAuthorityId, user.getLoginId()).block();
  }
}