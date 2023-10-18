package uk.gov.laa.ccms.caab.service;

import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.APP_TYPE_EMERGENCY;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.APP_TYPE_EMERGENCY_DEVOLVED_POWERS;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.STATUS_DRAFT;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple4;
import uk.gov.laa.ccms.caab.bean.ApplicationDetails;
import uk.gov.laa.ccms.caab.bean.CopyCaseSearchCriteria;
import uk.gov.laa.ccms.caab.builders.ApplicationBuilder;
import uk.gov.laa.ccms.caab.builders.ApplicationSummaryBuilder;
import uk.gov.laa.ccms.caab.client.CaabApiClient;
import uk.gov.laa.ccms.caab.client.EbsApiClient;
import uk.gov.laa.ccms.caab.client.SoaApiClient;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.mapper.ApplicationMapper;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.CostEntry;
import uk.gov.laa.ccms.caab.model.Proceeding;
import uk.gov.laa.ccms.caab.model.ProceedingOutcome;
import uk.gov.laa.ccms.caab.util.ApplicationBuilder;
import uk.gov.laa.ccms.caab.model.ApplicationSummaryDisplay;
import uk.gov.laa.ccms.data.model.AmendmentTypeLookupDetail;
import uk.gov.laa.ccms.data.model.CaseStatusLookupDetail;
import uk.gov.laa.ccms.data.model.CaseStatusLookupValueDetail;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;
import uk.gov.laa.ccms.data.model.ContactDetail;
import uk.gov.laa.ccms.data.model.OfficeDetail;
import uk.gov.laa.ccms.data.model.OutcomeResultLookupValueDetail;
import uk.gov.laa.ccms.data.model.ProviderDetail;
import uk.gov.laa.ccms.data.model.ScopeLimitationDetail;
import uk.gov.laa.ccms.data.model.StageEndLookupValueDetail;
import uk.gov.laa.ccms.data.model.RelationshipToCaseLookupDetail;
import uk.gov.laa.ccms.data.model.RelationshipToCaseLookupValueDetail;
import uk.gov.laa.ccms.data.model.UserDetail;
import uk.gov.laa.ccms.soa.gateway.model.AssessmentResult;
import uk.gov.laa.ccms.soa.gateway.model.CaseDetail;
import uk.gov.laa.ccms.soa.gateway.model.CaseDetails;
import uk.gov.laa.ccms.soa.gateway.model.CaseReferenceSummary;
import uk.gov.laa.ccms.soa.gateway.model.CategoryOfLaw;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetail;
import uk.gov.laa.ccms.soa.gateway.model.ContractDetails;
import uk.gov.laa.ccms.soa.gateway.model.OutcomeDetail;
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

  private final ApplicationMapper applicationMapper;

  private final LookupService lookupService;

  private final ProviderService providerService;

  /**
   * Searches and retrieves case details based on provided search criteria.
   *
   * @param copyCaseSearchCriteria The search criteria to use when fetching cases.
   * @param loginId                The login identifier for the user.
   * @param userType               Type of the user (e.g., admin, user).
   * @param page                   The page number for pagination.
   * @param size                   The size or number of records per page.
   * @return A Mono wrapping the CaseDetails.
   */
  public Mono<CaseDetails> getCases(CopyCaseSearchCriteria copyCaseSearchCriteria, String loginId,
      String userType, Integer page, Integer size) {
    return soaApiClient.getCases(copyCaseSearchCriteria, loginId, userType, page, size);
  }

  /**
   * Retrieve the full details of a Case.
   *
   * @param caseReferenceNumber The reference of the case to be retrieved.
   * @param loginId             The login identifier for the user.
   * @param userType            Type of the user (e.g., admin, user).
   * @return A Mono wrapping the CaseDetails.
   */
  public Mono<ApplicationDetail> getCase(String caseReferenceNumber, String loginId, String userType) {
    return soaApiClient.getCase(caseReferenceNumber, loginId, userType)
        .map(this::mapSoaCaseToApplication);
  }


  /**
   * Fetches a unique case reference.
   *
   * @param loginId  The login identifier for the user.
   * @param userType Type of the user (e.g., admin, user).
   * @return A Mono wrapping the CaseReferenceSummary.
   */
  public Mono<CaseReferenceSummary> getCaseReference(String loginId,
      String userType) {
    return soaApiClient.getCaseReference(loginId, userType);
  }

  /**
   * Create a draft Application in the CAAB's Transient Data Store.
   *
   * @param applicationDetails - The details of the Application to create
   * @param clientDetail - The client details
   * @param user - The related User.
   * @return a String containing the id of the application
   */
  public Mono<String> createApplication(ApplicationDetails applicationDetails,
      ClientDetail clientDetail, UserDetail user) {
    //need to do this first in order to get amendment types
    ApplicationDetail baseApplication = new ApplicationBuilder()
        .applicationType(
            applicationDetails.getApplicationTypeCategory(),
            applicationDetails.isDelegatedFunctions())
        .build();

    // get case reference Number, category of law value, contractual devolved powers,
    // amendment types
    Mono<Tuple4<CaseReferenceSummary,
        CommonLookupDetail,
        ContractDetails,
        AmendmentTypeLookupDetail>> combinedResult =
        Mono.zip(
            this.getCaseReference(user.getLoginId(), user.getUserType()),
            lookupService.getCategoriesOfLaw(),
            soaApiClient.getContractDetails(
                user.getProvider().getId(),
                applicationDetails.getOfficeId(),
                user.getLoginId(),
                user.getUserType()
            ),
            ebsApiClient.getAmendmentTypes(baseApplication.getApplicationType().getId())
        );

    return combinedResult.flatMap(tuple -> {
      CaseReferenceSummary caseReferenceSummary = tuple.getT1();
      CommonLookupDetail categoryOfLawValues = tuple.getT2();
      ContractDetails contractDetails = tuple.getT3();
      AmendmentTypeLookupDetail amendmentTypes = tuple.getT4();

      ApplicationDetail application;
      try {
        application = new ApplicationBuilder(baseApplication)
            .caseReference(caseReferenceSummary)
            .provider(user)
            .client(clientDetail)
            .categoryOfLaw(applicationDetails.getCategoryOfLawId(), categoryOfLawValues)
            .office(applicationDetails.getOfficeId(), user.getProvider().getOffices())
            .devolvedPowers(contractDetails.getContracts(), applicationDetails)
            .larScopeFlag(amendmentTypes)
            .status()
            .build();
      } catch (ParseException e) {
        return Mono.error(new RuntimeException(e));
      }

      // Create the application and block until it's done
      return caabApiClient.createApplication(user.getLoginId(), application);
    });
  }

  /**
   * Retrieves the case status lookup details based on the provided copyAllowed flag.
   *
   * @param copyAllowed A boolean flag indicating whether copying is allowed.
   * @return A Mono containing the CaseStatusLookupDetail or an error handler if an error occurs.
   */
  public Mono<CaseStatusLookupDetail> getCaseStatusValues(Boolean copyAllowed) {
    return ebsApiClient.getCaseStatusValues(copyAllowed);
  }

  /**
   * Retrieves the case status lookup value that is eligible for copying.
   *
   * @return The CaseStatusLookupValueDetail representing the eligible case status for copying.
   */
  public CaseStatusLookupValueDetail getCopyCaseStatus() {
    CaseStatusLookupDetail caseStatusLookupDetail = this.getCaseStatusValues(Boolean.TRUE).block();

    return Optional.ofNullable(caseStatusLookupDetail)
        .map(CaseStatusLookupDetail::getContent)
        .orElse(Collections.emptyList())
        .stream().findFirst().orElse(null);
  }

  protected ApplicationDetail mapSoaCaseToApplication(CaseDetail soaCase) {
    // Retrieve the relevant Application Type detail from EbsApi.
    CommonLookupValueDetail applicationTypeValue =
        Optional.ofNullable(lookupService.getApplicationType(soaCase.getCertificateType()).block())
            .orElseThrow(() -> new CaabApplicationException(
                String.format("Failed to find ApplicationType: %s", soaCase.getCertificateType())));

    // Retrieve full details of the Case's related Provider.
    uk.gov.laa.ccms.soa.gateway.model.ProviderDetail soaProvider = soaCase.getApplicationDetails()
        .getProviderDetails();

    ProviderDetail ebsProvider = getEbsProvider(Integer.valueOf(soaProvider.getProviderFirmId()));

    // Find the correct office
    OfficeDetail providerOffice = getProviderOffice(
        soaProvider.getProviderOfficeId(),
        ebsProvider);

    // Get the Fee Earners for the relevant office, and Map them by contact id.
    Map<Integer, ContactDetail> feeEarnerById =
        providerOffice.getFeeEarners().stream().collect(
            Collectors.toMap(ContactDetail::getId, Function.identity()));

    // Get the correct Supervisor and Fee Earner for this Provider.
    ContactDetail supervisorContact = feeEarnerById.get(Integer.valueOf(
        soaProvider.getSupervisorContactId()));
    ContactDetail feeEarnerContact = feeEarnerById.get(Integer.valueOf(
        soaProvider.getFeeEarnerContactId()));

    // Map what we can based on the ebsCase
    ApplicationDetail caabApplication = applicationMapper.toApplicationDetail(
        soaCase,
        applicationTypeValue,
        ebsProvider,
        providerOffice,
        supervisorContact,
        feeEarnerContact
        );

    CategoryOfLaw categoryOfLaw = soaCase.getApplicationDetails().getCategoryOfLaw();
    if (categoryOfLaw.getCostLimitations() != null && categoryOfLaw.getTotalPaidToDate() != null) {
      // Add the total amount billed across all cost entries.
      BigDecimal totalProviderAmount = caabApplication.getCosts().getCostEntries().stream()
          .map(CostEntry::getAmountBilled)
          .reduce(BigDecimal.ZERO, BigDecimal::add);

      caabApplication.getCosts().setCurrentProviderBilledAmount(
          categoryOfLaw.getTotalPaidToDate().subtract(totalProviderAmount));
    }

    convertProceedings(soaCase.getApplicationDetails().getProceedings(), caabApplication);

    caabApplication.setMeansAssessment(convertAssessment(
        soaCase.getApplicationDetails().getMeansAssesments()));
    caabApplication.setMeritsAssessment(convertAssessment(
        soaCase.getApplicationDetails().getMeritsAssesments()));

//    if (applicationDetails.getOtherParties() != null) {
//      result.setOpponents(convertOpponents(applicationDetails.getOtherParties(), result));
//    }
//    if (applicationDetails.getApplicationAmendmentType() != null) {
//      result.setApplicationType(applicationDetails.getApplicationAmendmentType());
//      result.setApplicationTypeDisplayValue(getValueFromOptions(applicationTypeOptions,
//          applicationDetails.getApplicationAmendmentType()));
//      if ((applicationDetails.getApplicationAmendmentType().equalsIgnoreCase(
//          CcmsConstants.APP_TYPE_EMERGENCY_DEVOLVED_POWERS)) || (applicationDetails
//          .getApplicationAmendmentType().equalsIgnoreCase(
//              CcmsConstants.APP_TYPE_SUBSTANTIVE_DEVOLVED_POWERS))) {
//        result.setDevolvedPowersUsed(CcmsConstants.OPTION_VALUE_YES);
//        result.setDateDevolvedPowersUsed(
//            DateUtils.convertXMLGregorianCalendarToDate(applicationDetails
//                .getDevolvedPowersDate()));
//      } else {
//        result.setDevolvedPowersUsed(CcmsConstants.OPTION_VALUE_NO);
//      }
//    } else if (applicationDetails.getApplicationAmendmentType() == null) {
//      if (caseDetails.getCertificateType() != null) {
//        result.setApplicationType(caseDetails.getCertificateType());
//        result.setApplicationTypeDisplayValue(
//            getValueFromOptions(applicationTypeOptions, caseDetails.getCertificateType()));
//
//      }
//    }
//    if (applicationDetails.getLARDetails() != null) {
//      if (applicationDetails.getLARDetails().isLARScopeFlag() != null) {
//        if (applicationDetails.getLARDetails().isLARScopeFlag()) {
//          result.setLarScopeFlag("Y");
//        } else {
//          result.setLarScopeFlag("N");
//        }
//      } else {
//        //setting the lar scope flag to N for pre lar cases if not received from ebs
//        result.setLarScopeFlag("N");
//      }
//    } else {
//      //setting the lar scope flag to N for pre lar cases if not received from ebs
//      result.setLarScopeFlag("N");
//    }
//  }
//
//      if (caseDetails.getLinkedCases() != null) {
//    result.setLinkedCases(convertLinkedCases(caseDetails.getLinkedCases(), result));
//  }
//
//      if (caseDetails.getCaseStatus() != null) {
//    result.setDisplayStatus(caseDetails.getCaseStatus().getDisplayCaseStatus());
//    result.setActualStatus(caseDetails.getCaseStatus().getActualCaseStatus());
//  }
//      if (caseDetails.getAvailableFunctions() != null) {
//    result.setAvailableFunctions(caseDetails.getAvailableFunctions().getFunction());
//  }
//      if (caseDetails.getPriorAuthorities() != null) {
//    result.setPriorAuthorities(
//        convertPriorAuthorities(caseDetails.getPriorAuthorities(), result));
//  }
//
//      result.setCaseOutcome(convertCaseOutcome(caseDetails, result));
//
//}
    return caabApplication;
  }

  /**
   * @param soaProceedings an EBS proceedings element
   * @param application the application to populate with the proceedings
   */
  protected void convertProceedings(final List<ProceedingDetail> soaProceedings,
      final ApplicationDetail application) {
    // Build Maps of the lookup values required to fill in missing display values in the
    // SOA-returned data.
    Map<String, CommonLookupValueDetail> matterTypes = toCommonValueMap(
        lookupService.getMatterTypes().block());
    Map<String, CommonLookupValueDetail> levelsOfService = toCommonValueMap(
        lookupService.getLevelsOfService().block());
    Map<String, CommonLookupValueDetail> clientInvolvementTypes = toCommonValueMap(
        lookupService.getClientInvolvementTypes().block());
    Map<String, CommonLookupValueDetail> statuses = toCommonValueMap(
        lookupService.getProceedingStatuses().block());

    // Check whether all of the soa proceedings are at status DRAFT
    boolean caseWithOnlyDraftProceedings = soaProceedings.stream().allMatch(proceedingDetail ->
        STATUS_DRAFT.equalsIgnoreCase(proceedingDetail.getStatus()));
    application.setSubmitted(caseWithOnlyDraftProceedings);

    for (ProceedingDetail soaProceeding : soaProceedings) {
      Proceeding caabProceeding = convertProceeding(
          soaProceeding,
          matterTypes,
          levelsOfService,
          clientInvolvementTypes,
          statuses,
          application);

      // Add the proceeding to the amendment proceedings if it is of type
      // draft and the case has live proceedings (not only drafts)
      if (STATUS_DRAFT.equalsIgnoreCase(caabProceeding.getStatus().getId())
          && !caseWithOnlyDraftProceedings) {
        application.addAmendmentProceedingsInEbsItem(caabProceeding);
      } else {
        application.addProceedingsItem(caabProceeding);
      }
    }
  }

  protected Proceeding convertProceeding (ProceedingDetail soaProceeding,
      Map<String, CommonLookupValueDetail> matterTypes,
      Map<String, CommonLookupValueDetail> levelsOfService,
      Map<String, CommonLookupValueDetail> clientInvolvementTypes,
      Map<String, CommonLookupValueDetail> statuses,
      ApplicationDetail applicationDetail
      ) {
    // Look up the Proceeding info from the EbsApi
    uk.gov.laa.ccms.data.model.ProceedingDetail ebsProceeding =
        ebsApiClient.getProceeding(soaProceeding.getProceedingType()).block();

    CommonLookupValueDetail matterType =
        matterTypes.get(soaProceeding.getMatterType());
    CommonLookupValueDetail levelOfService =
        levelsOfService.get(soaProceeding.getLevelOfService());
    CommonLookupValueDetail clientInvolvementType =
        clientInvolvementTypes.get(soaProceeding.getClientInvolvementType());
    CommonLookupValueDetail proceedingStatus =
        statuses.get(soaProceeding.getStatus());

    if (proceedingStatus == null) {
      proceedingStatus = new CommonLookupValueDetail()
          .code(soaProceeding.getStatus())
          .description(soaProceeding.getStatus());
    }
  /**
   * Retrieves the application Summary display values.
   *
   * @param id the identifier of the application to retrieve a summary for.
   * @return A Mono of ApplicationSummaryDisplay representing the case summary display values.
   */
  public Mono<ApplicationSummaryDisplay> getApplicationSummary(final String id) {

    Mono<RelationshipToCaseLookupDetail> organisationRelationshipsMono =
        ebsApiClient.getOrganisationRelationshipsToCaseValues();

    Mono<RelationshipToCaseLookupDetail> personRelationshipsMono =
        ebsApiClient.getPersonRelationshipsToCaseValues();

    Mono<ApplicationDetail> applicationMono
        = caabApiClient.getApplication(id);

    return Mono.zip(organisationRelationshipsMono,
            personRelationshipsMono,
            applicationMono)
        .map(tuple -> {

          List<RelationshipToCaseLookupValueDetail> organisationRelationships
              = tuple.getT1().getContent();
          List<RelationshipToCaseLookupValueDetail> personsRelationships
              = tuple.getT2().getContent();
          ApplicationDetail application = tuple.getT3();

          return new ApplicationSummaryBuilder(application.getAuditTrail())
              .clientFullName(
                  application.getClient().getFirstName(),
                  application.getClient().getSurname())
              .caseReferenceNumber(
                  application.getCaseReferenceNumber())
              .providerCaseReferenceNumber(
                  application.getProviderCaseReference())
              .applicationType(
                  application.getApplicationType().getDisplayValue())
              .providerDetails(
                  application.getProviderContact())
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

}

    Proceeding caabProceeding = applicationMapper.toProceeding(
        soaProceeding,
        ebsProceeding,
        matterType,
        levelOfService,
        clientInvolvementType,
        proceedingStatus);

    Map<String, CommonLookupValueDetail> scopeLimitationLookups =
        toCommonValueMap(lookupService.getScopeLimitations().block());
    for (ScopeLimitation soaScopeLimitation : soaProceeding.getScopeLimitations()) {
      caabProceeding.addScopeLimitationsItem(
          applicationMapper.toScopeLimitation(
              soaScopeLimitation,
              scopeLimitationLookups.get(soaScopeLimitation.getScopeLimitation())));
    }

    calculateProceedingCostLimitation(caabProceeding, applicationDetail);

    caabProceeding.setOutcome(convertProceedingOutcome(
        caabProceeding, soaProceeding.getOutcome()));

    return caabProceeding;
  }

  private void calculateProceedingCostLimitation(Proceeding proceeding, ApplicationDetail applicationDetail) {
    if (applicationDetail.getCategoryOfLaw().getId() != null
        && proceeding.getMatterType().getId() != null
        && proceeding.getProceedingType().getId() != null
        && proceeding.getLevelOfService().getId() != null
        && proceeding.getScopeLimitations() != null
        && !proceeding.getScopeLimitations().isEmpty()) {

      BigDecimal maxCostLimitation = new BigDecimal(0);
      final String applicationType = applicationDetail.getApplicationType().getId();
      boolean isEmergency = APP_TYPE_EMERGENCY.equalsIgnoreCase(applicationType)
          || APP_TYPE_EMERGENCY_DEVOLVED_POWERS.equalsIgnoreCase(applicationType);

      ScopeLimitationDetail searchCriteria = new ScopeLimitationDetail()
          .categoryOfLaw(applicationDetail.getCategoryOfLaw().getId())
          .matterType(proceeding.getMatterType().getId())
          .proceedingCode(proceeding.getProceedingType().getId())
          .levelOfService(proceeding.getLevelOfService().getId())
          .emergency(isEmergency);

      for (uk.gov.laa.ccms.caab.model.ScopeLimitation limitation :
          proceeding.getScopeLimitations()) {
        searchCriteria.setScopeLimitations(limitation.getScopeLimitation().getId());
        BigDecimal costLimitation = lookupService.getScopeLimitationDetails(searchCriteria)
            .map(scopeLimitationDetails -> scopeLimitationDetails.getContent().stream()
                .findFirst()
                .map(scopeLimitationDetail ->
                    isEmergency ? scopeLimitationDetail.getEmergencyCostLimitation()
                        : scopeLimitationDetail.getCostLimitation())
                .orElse(BigDecimal.ZERO))
            .block();

        maxCostLimitation = maxCostLimitation.max(costLimitation);
      }

      proceeding.setCostLimitation(maxCostLimitation);
    }
  }

  /**
   * @param soaOutcome - the outcome to convert into CAAB model.
   * @param caabProceeding - the proceeding that this outcome relates to.
   * @return the converted proceeding outcome
   */
  private ProceedingOutcome convertProceedingOutcome(
      final Proceeding caabProceeding, final OutcomeDetail soaOutcome) {
    /*
     * Lookup the name of the Court for this proceeding
     * - only use the result if we got a single match.
     */
    CommonLookupValueDetail courtLookup =
        Optional.ofNullable(lookupService.getCourts(soaOutcome.getCourtCode()).block())
            .map(commonLookupDetail -> commonLookupDetail.getTotalElements() == 1 ?
                commonLookupDetail.getContent().get(0) : null)
            .orElse(null);

    /*
     * Lookup the outcome result display value.
     */
    OutcomeResultLookupValueDetail outcomeResultLookup =
        Optional.ofNullable(lookupService.getOutcomeResults(
            caabProceeding.getProceedingType().getId(), soaOutcome.getResult()).block())
            .map(outcomeResultsLookupDetail -> outcomeResultsLookupDetail.getTotalElements() > 0 ?
                outcomeResultsLookupDetail.getContent().get(0) : null)
            .orElse(null);

    /*
     * Lookup the stage end display value.
     */
    StageEndLookupValueDetail stageEndLookup =
        Optional.ofNullable(lookupService.getStageEnds(caabProceeding.getProceedingType().getId(),
                soaOutcome.getStageEnd()).block())
            .map(stageEndLookupDetail -> stageEndLookupDetail.getTotalElements() > 0 ?
                stageEndLookupDetail.getContent().get(0) : null)
            .orElse(null);

    return applicationMapper.toProceedingOutcome(
        caabProceeding,
        soaOutcome,
        courtLookup,
        outcomeResultLookup,
        stageEndLookup);
  }

  /**
   * Convert an AssessmentResult from SOA to CAAB model.
   * The structure of these models is actually identical, so it's a like-for-like mapping.
   * <p/>
   * If there are multiple assessments results, the most recent is returned.
   * If none of the AssessmentResults have a date then we take the first in the List.
   *
   * @param assessmentResults list with assesment results.
   * @return most recent AssessmentResult
   */
  private uk.gov.laa.ccms.caab.model.AssessmentResult convertAssessment(
      final List<AssessmentResult> assessmentResults) {
    return assessmentResults != null ? assessmentResults.stream()
        .max(Comparator.comparing(AssessmentResult::getDate,
            Comparator.nullsFirst(Comparator.naturalOrder())))
        .map(applicationMapper::toAssessmentResult)
        .orElse(null) : null;
  }


  private ProviderDetail getEbsProvider(
      Integer providerFirmId) {
    return Optional.ofNullable(
            providerService.getProvider(providerFirmId).block())
        .orElseThrow(() ->
            new CaabApplicationException(
                String.format("Failed to retrieve Provider with id: %s", providerFirmId)));
  }

  private OfficeDetail getProviderOffice(
      String providerOfficeId, ProviderDetail ebsProvider) {
    return ebsProvider.getOffices().stream()
        .filter(officeDetail -> providerOfficeId.equals(String.valueOf(officeDetail.getId())))
        .findAny()
        .orElseThrow(() ->
            new CaabApplicationException(
                String.format("Failed to find Office with id: %s", providerOfficeId)));
  }

  private Map<String, CommonLookupValueDetail> toCommonValueMap(
      CommonLookupDetail commonLookupDetail) {
    if( commonLookupDetail == null ) {
      throw new CaabApplicationException("Failed to lookup common values");
    }

    return commonLookupDetail.getContent().stream().collect(
        Collectors.toMap(CommonLookupValueDetail::getCode, Function.identity()));
  }
}