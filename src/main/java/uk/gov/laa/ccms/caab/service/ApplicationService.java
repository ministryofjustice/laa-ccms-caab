package uk.gov.laa.ccms.caab.service;

import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.APP_TYPE_EMERGENCY;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.APP_TYPE_EMERGENCY_DEVOLVED_POWERS;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.APP_TYPE_SUBSTANTIVE_DEVOLVED_POWERS;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.AWARD_TYPE_COST;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.AWARD_TYPE_FINANCIAL;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.AWARD_TYPE_LAND;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.AWARD_TYPE_OTHER_ASSET;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.REFERENCE_DATA_ITEM_TYPE_LOV;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.STATUS_DRAFT;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple4;
import uk.gov.laa.ccms.caab.bean.ApplicationFormData;
import uk.gov.laa.ccms.caab.bean.CopyCaseSearchCriteria;
import uk.gov.laa.ccms.caab.builders.ApplicationBuilder;
import uk.gov.laa.ccms.caab.builders.ApplicationSummaryBuilder;
import uk.gov.laa.ccms.caab.builders.ApplicationTypeBuilder;
import uk.gov.laa.ccms.caab.client.CaabApiClient;
import uk.gov.laa.ccms.caab.client.EbsApiClient;
import uk.gov.laa.ccms.caab.client.SoaApiClient;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.mapper.ApplicationFormDataMapper;
import uk.gov.laa.ccms.caab.mapper.ApplicationMapper;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.ApplicationProviderDetails;
import uk.gov.laa.ccms.caab.model.ApplicationSummaryDisplay;
import uk.gov.laa.ccms.caab.model.ApplicationType;
import uk.gov.laa.ccms.caab.model.CaseOutcome;
import uk.gov.laa.ccms.caab.model.CostEntry;
import uk.gov.laa.ccms.caab.model.DevolvedPowers;
import uk.gov.laa.ccms.caab.model.IntDisplayValue;
import uk.gov.laa.ccms.caab.model.Opponent;
import uk.gov.laa.ccms.caab.model.PriorAuthority;
import uk.gov.laa.ccms.caab.model.Proceeding;
import uk.gov.laa.ccms.caab.model.ProceedingOutcome;
import uk.gov.laa.ccms.caab.model.ReferenceDataItem;
import uk.gov.laa.ccms.caab.model.StringDisplayValue;
import uk.gov.laa.ccms.data.model.AmendmentTypeLookupDetail;
import uk.gov.laa.ccms.data.model.AwardTypeLookupDetail;
import uk.gov.laa.ccms.data.model.AwardTypeLookupValueDetail;
import uk.gov.laa.ccms.data.model.CaseStatusLookupDetail;
import uk.gov.laa.ccms.data.model.CaseStatusLookupValueDetail;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;
import uk.gov.laa.ccms.data.model.ContactDetail;
import uk.gov.laa.ccms.data.model.OfficeDetail;
import uk.gov.laa.ccms.data.model.OutcomeResultLookupValueDetail;
import uk.gov.laa.ccms.data.model.PriorAuthorityTypeDetail;
import uk.gov.laa.ccms.data.model.PriorAuthorityTypeDetails;
import uk.gov.laa.ccms.data.model.ProviderDetail;
import uk.gov.laa.ccms.data.model.RelationshipToCaseLookupDetail;
import uk.gov.laa.ccms.data.model.RelationshipToCaseLookupValueDetail;
import uk.gov.laa.ccms.data.model.ScopeLimitationDetail;
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
import uk.gov.laa.ccms.soa.gateway.model.OtherParty;
import uk.gov.laa.ccms.soa.gateway.model.OutcomeDetail;
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

  private final ApplicationMapper applicationMapper;

  private final LookupService lookupService;

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
  public Mono<ApplicationDetail> getCase(String caseReferenceNumber, String loginId,
      String userType) {
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
   * @param applicationFormData - The details of the Application to create
   * @param clientDetail - The client details
   * @param user - The related User.
   * @return a String containing the id of the application
   */
  public Mono<String> createApplication(ApplicationFormData applicationFormData,
                                        ClientDetail clientDetail, UserDetail user)
      throws ParseException {

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

    //need to do this first in order to get amendment types
    ApplicationDetail baseApplication = new ApplicationBuilder()
        .applicationType(applicationType)
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
                  applicationFormData.getOfficeId(),
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
            .categoryOfLaw(
                applicationFormData.getCategoryOfLawId(),
                categoryOfLawValues)
            .office(
                applicationFormData.getOfficeId(),
                user.getProvider().getOffices())
            .contractualDevolvedPower(
                contractDetails.getContracts(),
                applicationFormData.getCategoryOfLawId())
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
                  application.getApplicationType())
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

  protected ApplicationDetail mapSoaCaseToApplication(CaseDetail soaCase) {
    uk.gov.laa.ccms.soa.gateway.model.ApplicationDetails soaApplicationDetails =
        soaCase.getApplicationDetails();

    // Retrieve the relevant Application Type detail from EbsApi.
    CommonLookupValueDetail applicationTypeValue =
        Optional.ofNullable(lookupService.getApplicationType(soaCase.getCertificateType()).block())
            .orElseThrow(() -> new CaabApplicationException(
                String.format("Failed to find ApplicationType: %s", soaCase.getCertificateType())));

    // Retrieve full details of the Case's related Provider.
    uk.gov.laa.ccms.soa.gateway.model.ProviderDetail soaProvider =
        soaApplicationDetails.getProviderDetails();

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

    CategoryOfLaw categoryOfLaw = soaApplicationDetails.getCategoryOfLaw();
    if (categoryOfLaw.getCostLimitations() != null && categoryOfLaw.getTotalPaidToDate() != null) {
      // Add the total amount billed across all cost entries.
      BigDecimal totalProviderAmount = caabApplication.getCosts().getCostEntries().stream()
          .map(CostEntry::getAmountBilled)
          .reduce(BigDecimal.ZERO, BigDecimal::add);

      caabApplication.getCosts().setCurrentProviderBilledAmount(
          categoryOfLaw.getTotalPaidToDate().subtract(totalProviderAmount));
    }

    convertProceedings(soaApplicationDetails.getProceedings(), caabApplication);

    caabApplication.setMeansAssessment(convertAssessment(
        soaApplicationDetails.getMeansAssesments()));
    caabApplication.setMeritsAssessment(convertAssessment(
        soaApplicationDetails.getMeritsAssesments()));

    caabApplication.setOpponents(convertOpponents(soaApplicationDetails.getOtherParties()));

    boolean isDevolvedPowers = APP_TYPE_EMERGENCY_DEVOLVED_POWERS.equalsIgnoreCase(
        soaApplicationDetails.getApplicationAmendmentType())
        || APP_TYPE_SUBSTANTIVE_DEVOLVED_POWERS.equalsIgnoreCase(
        soaApplicationDetails.getApplicationAmendmentType());

    caabApplication.getApplicationType().setDevolvedPowers(
        new DevolvedPowers()
            .used(isDevolvedPowers)
            .dateUsed(isDevolvedPowers ? soaApplicationDetails.getDevolvedPowersDate() : null));

    if (soaCase.getPriorAuthorities() != null) {
      caabApplication.setPriorAuthorities(convertPriorAuthorities(soaCase.getPriorAuthorities()));
    }


    caabApplication.setCaseOutcome(convertCaseOutcome(soaCase, caabApplication));

    return caabApplication;
  }

  protected List<PriorAuthority> convertPriorAuthorities(
      List<uk.gov.laa.ccms.soa.gateway.model.PriorAuthority> soaPriorAuthorities) {
    // Look up all prior auth types
    List<PriorAuthorityTypeDetail> priorAuthorityTypesLookup =
        Optional.ofNullable(lookupService.getPriorAuthorityTypes().block())
            .map(PriorAuthorityTypeDetails::getContent)
            .orElseThrow(() ->
                new CaabApplicationException("Failed to retrieve PriorAuthorityTypes"));

    Map<String, PriorAuthorityTypeDetail> priorAuthTypes =
        priorAuthorityTypesLookup.stream().collect(
            Collectors.toMap(PriorAuthorityTypeDetail::getCode, Function.identity()));

    List<PriorAuthority> caabPriorAuthorities = new ArrayList<>();
    for (uk.gov.laa.ccms.soa.gateway.model.PriorAuthority soaPriorAuthority : soaPriorAuthorities) {
      caabPriorAuthorities.add(convertPriorAuthority(
          soaPriorAuthority,
          priorAuthTypes));
    }

    return caabPriorAuthorities;
  }

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

    // Check whether all soa proceedings are at status DRAFT
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

  protected Proceeding convertProceeding(ProceedingDetail soaProceeding,
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

  public ApplicationFormData getApplicationTypeFormData(final String id) {
    return caabApiClient.getApplicationType(id)
        .map(applicationFormDataMapper::toApplicationTypeFormData).block();
  }

  public ApplicationFormData getProviderDetailsFormData(final String id) {
    return caabApiClient.getProviderDetails(id)
        .map(applicationFormDataMapper::toApplicationProviderDetailsFormData).block();
  }

  /**
   * Patches an application's application type in the CAAB's Transient Data Store.
   *
   * @param id the ID associated with the application
   * @param applicationFormData the details of the Application to amend
   * @param user the related User.
   */
  public void patchApplicationType(
      final String id,
      final ApplicationFormData applicationFormData,
      final UserDetail user)
      throws ParseException {

    ApplicationType applicationType = new ApplicationTypeBuilder()
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

    caabApiClient.patchApplication(
        id, user.getLoginId(), applicationType, "application-type").block();
  }

  /**
   * Patches an application's provider details in the CAAB's Transient Data Store.
   *
   * @param id the ID associated with the application
   * @param applicationFormData the details of the Application to amend
   * @param user the related User.
   */
  public void patchProviderDetails(
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

    caabApiClient.patchApplication(
        id, user.getLoginId(), providerDetails, "provider-details").block();

  }

  private void calculateProceedingCostLimitation(Proceeding proceeding,
      ApplicationDetail applicationDetail) {
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

  private ProceedingOutcome convertProceedingOutcome(
      final Proceeding caabProceeding, final OutcomeDetail soaOutcome) {
    /*
     * Lookup the name of the Court for this proceeding
     * - only use the result if we got a single match.
     */
    CommonLookupValueDetail courtLookup =
        Optional.ofNullable(lookupService.getCourts(soaOutcome.getCourtCode()).block())
            .map(commonLookupDetail -> commonLookupDetail.getTotalElements() == 1
                ? commonLookupDetail.getContent().get(0) : null)
            .orElse(null);

    /*
     * Lookup the outcome result display value.
     */
    OutcomeResultLookupValueDetail outcomeResultLookup =
        Optional.ofNullable(lookupService.getOutcomeResults(
                caabProceeding.getProceedingType().getId(), soaOutcome.getResult()).block())
            .map(outcomeResultsLookupDetail -> outcomeResultsLookupDetail.getTotalElements() > 0
                ? outcomeResultsLookupDetail.getContent().get(0) : null)
            .orElse(null);

    /*
     * Lookup the stage end display value.
     */
    StageEndLookupValueDetail stageEndLookup =
        Optional.ofNullable(lookupService.getStageEnds(caabProceeding.getProceedingType().getId(),
                soaOutcome.getStageEnd()).block())
            .map(stageEndLookupDetail -> stageEndLookupDetail.getTotalElements() > 0
                ? stageEndLookupDetail.getContent().get(0) : null)
            .orElse(null);

    return applicationMapper.toProceedingOutcome(
        caabProceeding,
        soaOutcome,
        courtLookup,
        outcomeResultLookup,
        stageEndLookup);
  }

  /**
   * Convert an AssessmentResult from SOA to CAAB model. The structure of these models is actually
   * identical, so it's a like-for-like mapping.
   * <p/>
   * If there are multiple assessments results, the most recent is returned. If none of the
   * AssessmentResults have a date then we take the first in the List.
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

  private PriorAuthority convertPriorAuthority(
      uk.gov.laa.ccms.soa.gateway.model.PriorAuthority soaPriorAuthority,
      Map<String, PriorAuthorityTypeDetail> priorAuthTypes) {
    PriorAuthority priorAuthority = applicationMapper.toPriorAuthority(
        soaPriorAuthority,
        priorAuthTypes.get(soaPriorAuthority.getPriorAuthorityType()));

    // Build a Map of ReferenceDataItem keyed on code
    Map<String, ReferenceDataItem> referenceDataItemMap =
        priorAuthority.getItems().stream().collect(
            Collectors.toMap(referenceDataItem -> referenceDataItem.getCode().getId(),
                Function.identity()));

    // Now set the value for each item
    for (PriorAuthorityAttribute attribElementType : soaPriorAuthority.getDetails()) {
      ReferenceDataItem item = referenceDataItemMap.get(attribElementType.getName());
      if (item != null) {
        item.setValue(new StringDisplayValue().id(attribElementType.getValue()));

        // If this item is of type LOV, lookup the corresponding LOV record to get the
        // display value.
        if (REFERENCE_DATA_ITEM_TYPE_LOV.equals(item.getType())) {
          CommonLookupValueDetail lovLookup =
              Optional.ofNullable(lookupService.getCommonValue(
                      item.getLovLookUp(), attribElementType.getValue()).block())
                  .orElse(new CommonLookupValueDetail());

          item.getValue().setDisplayValue(lovLookup.getDescription());
        }
      }
    }

    return priorAuthority;
  }

  protected List<Opponent> convertOpponents(List<OtherParty> otherParties) {
    List<Opponent> opponents = new ArrayList<>();

    if (otherParties != null) {
      for (OtherParty otherParty : otherParties) {
        opponents.add(
            otherParty.getPerson() != null
                ? applicationMapper.toIndividualOpponent(otherParty) :
                applicationMapper.toOrganisationOpponent(otherParty));
      }
    }

    return opponents;
  }


  /**
   * Convert a CaseOutcome.
   *
   * @param soaCaseDetails - the Soa Case Details to convert.
   * @return a converted CaseOutcome.
   */
  protected CaseOutcome convertCaseOutcome(final CaseDetail soaCaseDetails,
      final ApplicationDetail caabApplication) {
    CaseOutcome outcome = applicationMapper.toCaseOutcome(soaCaseDetails);

    List<Award> soaAwards = soaCaseDetails.getAwards();
    if (soaAwards != null) {
      convertAwards(soaAwards, outcome);
    }

    /*
     * Copy the ProceedingOutcome for each Proceeding into a flat List in the CaseOutcome
     */
    if (caabApplication.getProceedings() != null) {
      for (Proceeding proceeding : caabApplication.getProceedings()) {
        if (proceeding.getOutcome() != null) {
          outcome.addProceedingOutcomesItem(proceeding.getOutcome());
        }
      }
    }


    return outcome;
  }

  private void convertAwards(List<Award> soaAwards, CaseOutcome caabCaseOutcome) {
    // Look up all Award Types
    List<AwardTypeLookupValueDetail> awardTypesLookup =
        Optional.ofNullable(lookupService.getAwardTypes().block())
            .map(AwardTypeLookupDetail::getContent)
            .orElseThrow(() -> new CaabApplicationException("Failed to retrieve AwardTypes"));

    Map<String, AwardTypeLookupValueDetail> awardTypeMap =
        awardTypesLookup.stream().collect(
            Collectors.toMap(AwardTypeLookupValueDetail::getCode, Function.identity()));

    for (Award award : soaAwards) {
      String awardType =
          Optional.ofNullable(awardTypeMap.get(award.getAwardType()))
              .map(AwardTypeLookupValueDetail::getAwardType)
              .orElseThrow(() -> new CaabApplicationException(
                  String.format("Failed to find AwardType with code: %s", award.getAwardType())));

      switch (awardType) {
        case AWARD_TYPE_COST -> caabCaseOutcome.addCostAwardsItem(
            applicationMapper.toCostAward(award));
        case AWARD_TYPE_FINANCIAL -> caabCaseOutcome.addFinancialAwardsItem(
            applicationMapper.toFinancialAward(award));
        case AWARD_TYPE_LAND -> caabCaseOutcome.addLandAwardsItem(
            applicationMapper.toLandAward(award));
        case AWARD_TYPE_OTHER_ASSET -> caabCaseOutcome.addOtherAssetAwardsItem(
            applicationMapper.toOtherAssetAward(award));
        default -> log.warn("Unknown AwardType: " + awardType);
      }
    }
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
    if (commonLookupDetail == null) {
      throw new CaabApplicationException("Failed to lookup common values");
    }

    return commonLookupDetail.getContent().stream().collect(
        Collectors.toMap(CommonLookupValueDetail::getCode, Function.identity()));
  }
}