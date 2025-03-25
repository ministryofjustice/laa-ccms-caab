package uk.gov.laa.ccms.caab.builders;

import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.APP_TYPE_EMERGENCY;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.APP_TYPE_EMERGENCY_DEVOLVED_POWERS;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.APP_TYPE_SUBSTANTIVE_DEVOLVED_POWERS;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.AWARD_TYPE_COST;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.AWARD_TYPE_FINANCIAL;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.AWARD_TYPE_LAND;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.AWARD_TYPE_OTHER_ASSET;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.REFERENCE_DATA_ITEM_TYPE_LOV;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.STATUS_DRAFT;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_APPLICATION_TYPE;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_CLIENT_INVOLVEMENT_TYPES;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_LEVEL_OF_SERVICE;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_MATTER_TYPES;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_PROCEEDING_STATUS;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_SCOPE_LIMITATIONS;

import java.math.BigDecimal;
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
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple3;
import reactor.util.function.Tuple5;
import uk.gov.laa.ccms.caab.client.EbsApiClient;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.mapper.context.SoaApplicationMappingContext;
import uk.gov.laa.ccms.caab.mapper.context.SoaCaseOutcomeMappingContext;
import uk.gov.laa.ccms.caab.mapper.context.SoaPriorAuthorityMappingContext;
import uk.gov.laa.ccms.caab.mapper.context.SoaProceedingMappingContext;
import uk.gov.laa.ccms.caab.service.LookupService;
import uk.gov.laa.ccms.caab.service.ProviderService;
import uk.gov.laa.ccms.data.model.AwardTypeLookupDetail;
import uk.gov.laa.ccms.data.model.AwardTypeLookupValueDetail;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;
import uk.gov.laa.ccms.data.model.ContactDetail;
import uk.gov.laa.ccms.data.model.OfficeDetail;
import uk.gov.laa.ccms.data.model.OutcomeResultLookupDetail;
import uk.gov.laa.ccms.data.model.OutcomeResultLookupValueDetail;
import uk.gov.laa.ccms.data.model.PriorAuthorityTypeDetail;
import uk.gov.laa.ccms.data.model.ProceedingDetail;
import uk.gov.laa.ccms.data.model.ProviderDetail;
import uk.gov.laa.ccms.data.model.StageEndLookupDetail;
import uk.gov.laa.ccms.data.model.StageEndLookupValueDetail;
import uk.gov.laa.ccms.soa.gateway.model.AssessmentResult;
import uk.gov.laa.ccms.soa.gateway.model.Award;
import uk.gov.laa.ccms.soa.gateway.model.CaseDetail;
import uk.gov.laa.ccms.soa.gateway.model.CategoryOfLaw;
import uk.gov.laa.ccms.soa.gateway.model.CostLimitation;
import uk.gov.laa.ccms.soa.gateway.model.PriorAuthority;
import uk.gov.laa.ccms.soa.gateway.model.PriorAuthorityAttribute;
import uk.gov.laa.ccms.soa.gateway.model.ScopeLimitation;
import uk.gov.laa.ccms.soa.gateway.model.SubmittedApplicationDetails;

@Service
@RequiredArgsConstructor
public class SoaApplicationMappingContextBuilder {

  private final ProviderService providerService;
  private final LookupService lookupService;
  private final EbsApiClient ebsApiClient;

  /**
   * Before a CaseDetail can be mapped to a CAAB ApplicationDetail further lookup
   * data and calculations need to be performed. This method builds a wrapper object to
   * hold all the required data for the mapping.
   *
   * @param soaCase - the SOA CaseDetail.
   * @return a Mono containing an ApplicationMappingContext for the CaseDetail.
   */
  public SoaApplicationMappingContext buildApplicationMappingContext(final CaseDetail soaCase) {
    final SubmittedApplicationDetails soaApplicationDetails =
        soaCase.getApplicationDetails();

    final uk.gov.laa.ccms.soa.gateway.model.ProviderDetail soaProvider =
        soaApplicationDetails.getProviderDetails();

    // Determine whether all the proceedings in the soaCase are at status DRAFT
    final boolean caseWithOnlyDraftProceedings =
        soaApplicationDetails.getProceedings() != null
            && soaApplicationDetails.getProceedings().stream().allMatch(
            proceedingDetail -> STATUS_DRAFT.equalsIgnoreCase(proceedingDetail.getStatus()));

    // Retrieve the full provider details
    final ProviderDetail providerDetail =
        Optional.ofNullable(providerService.getProvider(
                Integer.parseInt(soaProvider.getProviderFirmId())).block())
            .orElseThrow(() -> new CaabApplicationException(
                "Failed to query lookup data for Application mapping"));

    // Lookup the certificate display value
    final CommonLookupValueDetail certificateLookup = soaCase.getCertificateType() != null
        ? lookupService.getCommonValue(COMMON_VALUE_APPLICATION_TYPE, soaCase.getCertificateType())
        .map(commonLookupValueDetail -> commonLookupValueDetail
            .orElse(new CommonLookupValueDetail()
                .code(soaCase.getCertificateType())
                .description(soaCase.getCertificateType())))
        .blockOptional()
        .orElseThrow(() -> new CaabApplicationException(
            String.format("Failed to retrieve applicationtype with code: %s",
                soaCase.getCertificateType()))) : null;

    // Lookup the application type display value - this should be based on the
    // application/amendment type (if it has one), or the certificate type.
    final CommonLookupValueDetail applicationTypeLookup =
        soaApplicationDetails.getApplicationAmendmentType() != null
            ? lookupService.getCommonValue(
                COMMON_VALUE_APPLICATION_TYPE, soaApplicationDetails.getApplicationAmendmentType())
            .mapNotNull(commonLookupValueDetail -> commonLookupValueDetail
                .orElse(certificateLookup))
            .blockOptional()
            .orElseThrow(() -> new CaabApplicationException(
                String.format("Failed to retrieve applicationtype with code: %s",
                    soaApplicationDetails.getApplicationAmendmentType()))) : certificateLookup;

    // Find the correct provider office.
    final OfficeDetail providerOffice = providerDetail.getOffices().stream()
        .filter(officeDetail -> soaProvider.getProviderOfficeId().equals(
            String.valueOf(officeDetail.getId())))
        .findAny()
        .orElseThrow(() -> new CaabApplicationException(
            String.format("Failed to find Office with id: %s",
                soaProvider.getProviderOfficeId())));

    // Get the Fee Earners for the relevant office, and Map them by contact id.
    final Map<Integer, ContactDetail> feeEarnerById =
        providerOffice.getFeeEarners().stream().collect(
            Collectors.toMap(ContactDetail::getId, Function.identity()));

    // Get the correct Supervisor and Fee Earner for this Provider.
    final ContactDetail supervisorContact = feeEarnerById.get(Integer.valueOf(
        soaProvider.getSupervisorContactId()));

    final ContactDetail feeEarnerContact = feeEarnerById.get(Integer.valueOf(
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
      final BigDecimal totalProviderAmount = categoryOfLaw.getCostLimitations().stream()
          .map(CostLimitation::getPaidToDate)
          .reduce(BigDecimal.ZERO, BigDecimal::add);

      currentProviderBilledAmount =
          categoryOfLaw.getTotalPaidToDate().subtract(totalProviderAmount);
    }

    // Find the most recent Assessments
    final AssessmentResult meansAssessment = getMostRecentAssessment(
        soaApplicationDetails.getMeansAssesments());

    final AssessmentResult meritsAssessment = getMostRecentAssessment(
        soaApplicationDetails.getMeritsAssesments());

    /*
     * Split the proceeding list based on status, and build a ProceedingMappingContext
     * for each to hold the necessary lookup data.
     */
    final List<SoaProceedingMappingContext> amendmentProceedingsInEbs =
        soaApplicationDetails.getProceedings() != null
            ? soaApplicationDetails.getProceedings().stream()
            .filter(proceedingDetail -> !caseWithOnlyDraftProceedings
                && STATUS_DRAFT.equalsIgnoreCase(proceedingDetail.getStatus()))
            .map(proceedingDetail -> Optional.ofNullable(
                    buildProceedingMappingContext(
                        proceedingDetail,
                        soaCase))
                .orElseThrow(() -> new CaabApplicationException(
                    "Failed to build mapping context")))
            .toList() : Collections.emptyList();

    final List<SoaProceedingMappingContext> proceedings =
        soaApplicationDetails.getProceedings() != null
            ? soaApplicationDetails.getProceedings().stream()
            .filter(proceedingDetail -> caseWithOnlyDraftProceedings
                || !STATUS_DRAFT.equalsIgnoreCase(proceedingDetail.getStatus()))
            .map(proceedingDetail -> Optional.ofNullable(
                    buildProceedingMappingContext(
                        proceedingDetail,
                        soaCase))
                .orElseThrow(() -> new CaabApplicationException(
                    "Failed to build mapping context")))
            .toList() : Collections.emptyList();

    // Build a mapping context for each Prior Authority in the application
    List<SoaPriorAuthorityMappingContext> priorAuthorities =
        soaCase.getPriorAuthorities() != null
            ? soaCase.getPriorAuthorities().stream()
            .map(this::buildPriorAuthorityMappingContext)
            .toList() : Collections.emptyList();

    // Build a mapping context for the case outcome
    SoaCaseOutcomeMappingContext caseOutcomeMappingContext = buildCaseOutcomeMappingContext(
        soaCase,
        Stream.concat(amendmentProceedingsInEbs.stream(), proceedings.stream()).toList());

    return SoaApplicationMappingContext.builder()
        .soaCaseDetail(soaCase)
        .applicationType(applicationTypeLookup)
        .certificate(certificateLookup)
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

  private AssessmentResult getMostRecentAssessment(final List<AssessmentResult> assessmentResults) {
    return assessmentResults != null ? assessmentResults.stream()
        .max(Comparator.comparing(uk.gov.laa.ccms.soa.gateway.model.AssessmentResult::getDate,
            Comparator.nullsFirst(Comparator.naturalOrder())))
        .orElse(null) : null;
  }

  protected SoaProceedingMappingContext buildProceedingMappingContext(
      final uk.gov.laa.ccms.soa.gateway.model.ProceedingDetail soaProceeding,
      final CaseDetail soaCase) {

    Tuple5<ProceedingDetail,
        Optional<CommonLookupValueDetail>,
        Optional<CommonLookupValueDetail>,
        Optional<CommonLookupValueDetail>,
        Optional<CommonLookupValueDetail>> lookupTuple = Optional.ofNullable(
            Mono.zip(ebsApiClient.getProceeding(soaProceeding.getProceedingType()),
                    lookupService.getCommonValue(
                        COMMON_VALUE_PROCEEDING_STATUS, soaProceeding.getStatus()),
                    lookupService.getCommonValue(
                        COMMON_VALUE_MATTER_TYPES, soaProceeding.getMatterType()),
                    lookupService.getCommonValue(
                        COMMON_VALUE_LEVEL_OF_SERVICE, soaProceeding.getLevelOfService()),
                    lookupService.getCommonValue(
                        COMMON_VALUE_CLIENT_INVOLVEMENT_TYPES, soaProceeding.getClientInvolvementType()))
                .block())
        .orElseThrow(() -> new CaabApplicationException(
            "Failed to retrieve lookup data for ProceedingDetail"));

    // Calculate the overall cost limitation for this proceeding
    BigDecimal proceedingCostLimitation =
        this.calculateProceedingCostLimitation(soaProceeding, soaCase);

    // Build a List of pairs of Scope Limitation and associated lookup
    List<Pair<ScopeLimitation, CommonLookupValueDetail>> scopeLimitations =
        soaProceeding.getScopeLimitations().stream()
            .map(scopeLimitation -> Pair.of(
                scopeLimitation,
                lookupService.getCommonValue(
                        COMMON_VALUE_SCOPE_LIMITATIONS,
                        scopeLimitation.getScopeLimitation())
                    .map(commonLookupValueDetail -> commonLookupValueDetail
                        .orElse(new CommonLookupValueDetail()
                            .code(scopeLimitation.getScopeLimitation())
                            .description(scopeLimitation.getScopeLimitation())))
                    .block()))
            .toList();

    final uk.gov.laa.ccms.data.model.ProceedingDetail proceedingLookup =
        lookupTuple.getT1();

    final CommonLookupValueDetail proceedingStatusLookup =
        lookupTuple.getT2().orElse(new CommonLookupValueDetail()
            .code(soaProceeding.getStatus())
            .description(soaProceeding.getStatus()));

    final CommonLookupValueDetail matterTypeLookup =
        lookupTuple.getT3().orElse(new CommonLookupValueDetail()
            .code(soaProceeding.getMatterType())
            .description(soaProceeding.getMatterType()));

    final CommonLookupValueDetail levelOfServiceLookup =
        lookupTuple.getT4().orElse(new CommonLookupValueDetail()
            .code(soaProceeding.getLevelOfService())
            .description(soaProceeding.getLevelOfService()));

    final CommonLookupValueDetail clientInvolvementLookup =
        lookupTuple.getT5().orElse(new CommonLookupValueDetail()
            .code(soaProceeding.getClientInvolvementType())
            .description(soaProceeding.getClientInvolvementType()));

    SoaProceedingMappingContext.SoaProceedingMappingContextBuilder contextBuilder =
        SoaProceedingMappingContext.builder()
            .soaProceeding(soaProceeding)
            .proceedingLookup(proceedingLookup)
            .proceedingStatusLookup(proceedingStatusLookup)
            .proceedingCostLimitation(proceedingCostLimitation)
            .matterType(matterTypeLookup)
            .levelOfService(levelOfServiceLookup)
            .clientInvolvement(clientInvolvementLookup)
            .scopeLimitations(scopeLimitations);

    this.addProceedingOutcomeContext(contextBuilder, soaProceeding);

    return contextBuilder.build();
  }

  protected BigDecimal calculateProceedingCostLimitation(
      final uk.gov.laa.ccms.soa.gateway.model.ProceedingDetail proceeding,
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

      // Build the scope limitation search criteria.
      // Only include the emergency flag in the criteria if the app type is classified as emergency.
      uk.gov.laa.ccms.data.model.ScopeLimitationDetail searchCriteria =
          new uk.gov.laa.ccms.data.model.ScopeLimitationDetail()
              .categoryOfLaw(soaCase.getApplicationDetails().getCategoryOfLaw().getCategoryOfLawCode())
              .matterType(proceeding.getMatterType())
              .proceedingCode(proceeding.getProceedingType())
              .levelOfService(proceeding.getLevelOfService())
              .emergency(isEmergency ? Boolean.TRUE : null);

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


  protected void addProceedingOutcomeContext(
      final SoaProceedingMappingContext.SoaProceedingMappingContextBuilder contextBuilder,
      final uk.gov.laa.ccms.soa.gateway.model.ProceedingDetail soaProceeding) {

    if (soaProceeding.getOutcome() == null) {
      return; // Nothing to add
    }

    // Lookup extra data relating to the ProceedingDetail Outcome
    final Tuple3<CommonLookupDetail,
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
    final CommonLookupValueDetail courtLookup =
        combinedOutcomeResults.getT1().getContent().size() == 1
            ? combinedOutcomeResults.getT1().getContent().getFirst() :
            new CommonLookupValueDetail()
                .code(soaProceeding.getOutcome().getCourtCode())
                .description(soaProceeding.getOutcome().getCourtCode());

    // Use the outcome result display data, if we have it.
    final OutcomeResultLookupValueDetail outcomeResultLookup =
        !combinedOutcomeResults.getT2().getContent().isEmpty()
            ? combinedOutcomeResults.getT2().getContent().getFirst() : null;

    // Lookup the stage end display value.
    final StageEndLookupValueDetail stageEndLookup =
        !combinedOutcomeResults.getT3().getContent().isEmpty()
            ? combinedOutcomeResults.getT3().getContent().getFirst() : null;

    // Update the builder with outcome-related lookup data
    contextBuilder.courtLookup(courtLookup)
        .outcomeResultLookup(outcomeResultLookup)
        .stageEndLookup(stageEndLookup);
  }


  protected SoaCaseOutcomeMappingContext buildCaseOutcomeMappingContext(
      final CaseDetail soaCase,
      final List<SoaProceedingMappingContext> proceedingMappingContexts) {
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

    return SoaCaseOutcomeMappingContext.builder()
        .soaCase(soaCase)
        .costAwards(costAwards)
        .financialAwards(financialAwards)
        .landAwards(landAwards)
        .otherAssetAwards(otherAssetAwards)
        .proceedingOutcomes(proceedingMappingContexts)
        .build();
  }

  private String findAwardType(
      final Map<String, AwardTypeLookupValueDetail> awardTypes, final Award award) {
    return Optional.ofNullable(awardTypes.get(award.getAwardType()))
        .map(AwardTypeLookupValueDetail::getAwardType)
        .orElseThrow(() -> new CaabApplicationException(
            String.format("Failed to find AwardType with code: %s", award.getAwardType())));
  }

  /**
   * Build a mapping context to hold a SOA PriorAuthorityDetail and associated
   * lookup data.
   *
   * @param soaPriorAuthority - the PriorAuthorityDetail to map.
   * @return a PriorAuthorityMappingContext containing all data to support mapping
   *     to a CAAB PriorAuthorityDetail.
   */
  protected SoaPriorAuthorityMappingContext buildPriorAuthorityMappingContext(
      final PriorAuthority soaPriorAuthority) {

    // Find the correct PriorAuthorityType lookup
    PriorAuthorityTypeDetail priorAuthorityType =
        lookupService.getPriorAuthorityType(soaPriorAuthority.getPriorAuthorityType())
            .map(priorAuthorityTypeDetail -> priorAuthorityTypeDetail
                .orElse(new PriorAuthorityTypeDetail()
                    .code(soaPriorAuthority.getPriorAuthorityType())
                    .description(soaPriorAuthority.getPriorAuthorityType())))
            .blockOptional()
            .orElseThrow(() -> new CaabApplicationException(
                String.format("Failed to find PriorAuthorityType with code: %s",
                    soaPriorAuthority.getPriorAuthorityType())));

    // Build a Map of PriorAuthorityDetail keyed on code
    Map<String, uk.gov.laa.ccms.data.model.PriorAuthorityDetail> priorAuthDetailMap =
        priorAuthorityType.getPriorAuthorities().stream().collect(
            Collectors.toMap(uk.gov.laa.ccms.data.model.PriorAuthorityDetail::getCode,
                Function.identity()));

    // Build a List of priorAuthorityDetails paired with the common lookup for display info.
    List<Pair<uk.gov.laa.ccms.data.model.PriorAuthorityDetail,
        CommonLookupValueDetail>> priorAuthorityDetails =
        soaPriorAuthority.getDetails().stream()
            .map(priorAuthorityAttribute -> {
              uk.gov.laa.ccms.data.model.PriorAuthorityDetail priorAuthorityDetail =
                  priorAuthDetailMap.get(priorAuthorityAttribute.getName());
              return Pair.of(
                  priorAuthorityDetail,
                  getPriorAuthLookup(priorAuthorityDetail, priorAuthorityAttribute));
            })
            .toList();

    return SoaPriorAuthorityMappingContext.builder()
        .soaPriorAuthority(soaPriorAuthority)
        .priorAuthorityTypeLookup(priorAuthorityType)
        .items(priorAuthorityDetails)
        .build();
  }

  private CommonLookupValueDetail getPriorAuthLookup(
      final uk.gov.laa.ccms.data.model.PriorAuthorityDetail priorAuthorityDetail,
      final PriorAuthorityAttribute priorAuthorityAttribute) {
    String description;

    // If this attribute is of type LOV, lookup the corresponding LOV record to get the
    // display value.
    if (priorAuthorityDetail != null
        && REFERENCE_DATA_ITEM_TYPE_LOV.equals(priorAuthorityDetail.getDataType())) {
      description = lookupService.getCommonValue(
              priorAuthorityDetail.getLovCode(), priorAuthorityAttribute.getValue())
          .map(commonLookupValueDetail -> commonLookupValueDetail
              .map(CommonLookupValueDetail::getDescription)
              .orElse(priorAuthorityAttribute.getValue()))
          .blockOptional()
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


}
