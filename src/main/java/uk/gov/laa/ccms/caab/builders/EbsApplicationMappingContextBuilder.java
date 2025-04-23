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
import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import uk.gov.laa.ccms.caab.mapper.context.EbsApplicationMappingContext;
import uk.gov.laa.ccms.caab.mapper.context.EbsCaseOutcomeMappingContext;
import uk.gov.laa.ccms.caab.mapper.context.EbsPriorAuthorityMappingContext;
import uk.gov.laa.ccms.caab.mapper.context.EbsProceedingMappingContext;
import uk.gov.laa.ccms.caab.service.LookupService;
import uk.gov.laa.ccms.caab.service.ProviderService;
import uk.gov.laa.ccms.data.model.AssessmentResult;
import uk.gov.laa.ccms.data.model.Award;
import uk.gov.laa.ccms.data.model.AwardTypeLookupDetail;
import uk.gov.laa.ccms.data.model.AwardTypeLookupValueDetail;
import uk.gov.laa.ccms.data.model.CaseDetail;
import uk.gov.laa.ccms.data.model.CategoryOfLaw;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;
import uk.gov.laa.ccms.data.model.ContactDetail;
import uk.gov.laa.ccms.data.model.CostLimitation;
import uk.gov.laa.ccms.data.model.OfficeDetail;
import uk.gov.laa.ccms.data.model.OutcomeResultLookupDetail;
import uk.gov.laa.ccms.data.model.OutcomeResultLookupValueDetail;
import uk.gov.laa.ccms.data.model.PriorAuthority;
import uk.gov.laa.ccms.data.model.PriorAuthorityAttribute;
import uk.gov.laa.ccms.data.model.PriorAuthorityDetail;
import uk.gov.laa.ccms.data.model.PriorAuthorityTypeDetail;
import uk.gov.laa.ccms.data.model.Proceeding;
import uk.gov.laa.ccms.data.model.ProceedingDetail;
import uk.gov.laa.ccms.data.model.ProviderDetail;
import uk.gov.laa.ccms.data.model.ProviderDetails;
import uk.gov.laa.ccms.data.model.ScopeLimitation;
import uk.gov.laa.ccms.data.model.ScopeLimitationDetail;
import uk.gov.laa.ccms.data.model.StageEndLookupDetail;
import uk.gov.laa.ccms.data.model.StageEndLookupValueDetail;
import uk.gov.laa.ccms.data.model.SubmittedApplicationDetails;

/**
 * The {@link EbsApplicationMappingContextBuilder} class is responsible for building
 *     a {@link EbsApplicationMappingContext} object.
 *
 * <p>Various look up services are used to build this object, such as:</p>
 * <ul>
 *    <li>{@link ProviderService}</li>
 *    <li>{@link LookupService}</li>
 *    <li>{@link EbsApiClient}</li>
 * </ul>
 *
 * @author Jamie Briggs
 **/
@Service
@RequiredArgsConstructor
public class EbsApplicationMappingContextBuilder {

  private final ProviderService providerService;
  private final LookupService lookupService;
  private final EbsApiClient ebsApiClient;

  /**
   * Before a CaseDetail can be mapped to a CAAB ApplicationDetail further lookup
   * data and calculations need to be performed. This method builds a wrapper object to
   * hold all the required data for the mapping.
   *
   * @param ebsCase - the EBS CaseDetail.
   * @return a Mono containing an ApplicationMappingContext for the CaseDetail.
   */
  public EbsApplicationMappingContext buildApplicationMappingContext(final CaseDetail ebsCase) {
    final SubmittedApplicationDetails ebsApplicationDetails =
        ebsCase.getApplicationDetails();

    final ProviderDetails ebsProvider =
        ebsApplicationDetails.getProviderDetails();

    // Determine whether all the proceedings in the ebsCase are at status DRAFT
    final boolean caseWithOnlyDraftProceedings =
        ebsApplicationDetails.getProceedings() != null
            && ebsApplicationDetails.getProceedings().stream().allMatch(
              proceedingDetail
                  -> STATUS_DRAFT.equalsIgnoreCase(proceedingDetail.getStatus()));

    // Retrieve the full provider details
    final ProviderDetail providerDetail =
        Optional.ofNullable(providerService.getProvider(
                Integer.parseInt(ebsProvider.getProviderFirmId())).block())
            .orElseThrow(() -> new CaabApplicationException(
                "Failed to query lookup data for Application mapping"));

    // Lookup the certificate display value
    final CommonLookupValueDetail certificateLookup = ebsCase.getCertificateType() != null
        ? lookupService.getCommonValue(COMMON_VALUE_APPLICATION_TYPE, ebsCase.getCertificateType())
        .map(commonLookupValueDetail -> commonLookupValueDetail
            .orElse(new CommonLookupValueDetail()
                .code(ebsCase.getCertificateType())
                .description(ebsCase.getCertificateType())))
        .blockOptional()
        .orElseThrow(() -> new CaabApplicationException(
            "Failed to retrieve applicationtype with code: %s".formatted(
                ebsCase.getCertificateType()))) : null;

    // Lookup the application type display value - this should be based on the
    // application/amendment type (if it has one), or the certificate type.
    final CommonLookupValueDetail applicationTypeLookup =
        ebsApplicationDetails.getApplicationAmendmentType() != null
            ? lookupService.getCommonValue(
                COMMON_VALUE_APPLICATION_TYPE, ebsApplicationDetails.getApplicationAmendmentType())
            .mapNotNull(commonLookupValueDetail
                -> commonLookupValueDetail
                .orElse(certificateLookup))
            .blockOptional()
            .orElseThrow(() -> new CaabApplicationException(
                "Failed to retrieve applicationtype with code: %s".formatted(
                    ebsApplicationDetails.getApplicationAmendmentType()))) : certificateLookup;

    // Find the correct provider office.
    final OfficeDetail providerOffice = providerDetail.getOffices().stream()
        .filter(officeDetail -> ebsProvider.getProviderOfficeId().equals(
            String.valueOf(officeDetail.getId())))
        .findAny()
        .orElseThrow(() -> new CaabApplicationException(
        "Failed to find Office with id: %s".formatted(
            ebsProvider.getProviderOfficeId())));

    // Get the Fee Earners for the relevant office, and Map them by contact id.
    final Map<Integer, ContactDetail> feeEarnerById =
        providerOffice.getFeeEarners().stream().collect(
            Collectors.toMap(ContactDetail::getId, Function.identity()));

    // Get the correct Supervisor and Fee Earner for this Provider.
    final ContactDetail supervisorContact = Objects.nonNull(ebsProvider.getSupervisorContactId())
        ? feeEarnerById.get(Integer.valueOf(ebsProvider.getSupervisorContactId())) : null;

    final ContactDetail feeEarnerContact = Objects.nonNull(ebsProvider.getFeeEarnerContactId())
        ? feeEarnerById.get(Integer.valueOf(ebsProvider.getFeeEarnerContactId())) : null;

    // Set the DevolvedPowers for the Application based on the ApplicationAmendmentType.
    boolean isDevolvedPowers =
        APP_TYPE_EMERGENCY_DEVOLVED_POWERS.equalsIgnoreCase(
            ebsApplicationDetails.getApplicationAmendmentType())
            || APP_TYPE_SUBSTANTIVE_DEVOLVED_POWERS.equalsIgnoreCase(
            ebsApplicationDetails.getApplicationAmendmentType());
    Pair<Boolean, LocalDate> devolvedPowersInfo = Pair.of(
        isDevolvedPowers,
        isDevolvedPowers ? ebsApplicationDetails.getDevolvedPowersDate() : null);

    // Calculate the CurrentProviderBilledAmount for the Application's Costs.
    BigDecimal currentProviderBilledAmount = BigDecimal.ZERO;
    CategoryOfLaw categoryOfLaw = ebsApplicationDetails.getCategoryOfLaw();
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
        ebsApplicationDetails.getMeansAssessments());

    final AssessmentResult meritsAssessment = getMostRecentAssessment(
        ebsApplicationDetails.getMeritsAssessments());

    /*
     * Split the proceeding list based on status, and build a ProceedingMappingContext
     * for each to hold the necessary lookup data.
     */
    final List<EbsProceedingMappingContext> amendmentProceedingsInEbs =
        ebsApplicationDetails.getProceedings() != null
            ? ebsApplicationDetails.getProceedings().stream()
            .filter(proceedingDetail -> !caseWithOnlyDraftProceedings
                && STATUS_DRAFT.equalsIgnoreCase(proceedingDetail.getStatus()))
            .map(proceedingDetail -> Optional.ofNullable(
                    buildProceedingMappingContext(
                        proceedingDetail,
                        ebsCase))
                .orElseThrow(() -> new CaabApplicationException(
                    "Failed to build mapping context")))
            .toList() : Collections.emptyList();

    final List<EbsProceedingMappingContext> proceedings =
        ebsApplicationDetails.getProceedings() != null
            ? ebsApplicationDetails.getProceedings().stream()
            .filter(proceedingDetail -> caseWithOnlyDraftProceedings
                || !STATUS_DRAFT.equalsIgnoreCase(proceedingDetail.getStatus()))
            .map(proceedingDetail -> Optional.ofNullable(
                    buildProceedingMappingContext(
                        proceedingDetail,
                        ebsCase))
                .orElseThrow(() -> new CaabApplicationException(
                    "Failed to build mapping context")))
            .toList() : Collections.emptyList();

    // Build a mapping context for each Prior Authority in the application
    List<EbsPriorAuthorityMappingContext> priorAuthorities =
        ebsCase.getPriorAuthorities() != null
            ? ebsCase.getPriorAuthorities().stream()
            .map(this::buildPriorAuthorityMappingContext)
            .toList() : Collections.emptyList();

    // Build a mapping context for the case outcome
    EbsCaseOutcomeMappingContext caseOutcomeMappingContext = buildCaseOutcomeMappingContext(
        ebsCase,
        Stream.concat(amendmentProceedingsInEbs.stream(), proceedings.stream()).toList());

    return EbsApplicationMappingContext.builder()
        .ebsCaseDetail(ebsCase)
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
        .max(Comparator.comparing(AssessmentResult::getDate,
            Comparator.nullsFirst(Comparator.naturalOrder())))
        .orElse(null) : null;
  }

  protected EbsProceedingMappingContext buildProceedingMappingContext(
      final Proceeding ebsProceeding,
      final CaseDetail ebsCase) {

    Tuple5<ProceedingDetail,
        Optional<CommonLookupValueDetail>,
        Optional<CommonLookupValueDetail>,
        Optional<CommonLookupValueDetail>,
        Optional<CommonLookupValueDetail>> lookupTuple = Optional.ofNullable(
            Mono.zip(ebsApiClient.getProceeding(ebsProceeding.getProceedingType()),
                    lookupService.getCommonValue(
                        COMMON_VALUE_PROCEEDING_STATUS, ebsProceeding.getStatus()),
                    lookupService.getCommonValue(
                        COMMON_VALUE_MATTER_TYPES, ebsProceeding.getMatterType()),
                    lookupService.getCommonValue(
                        COMMON_VALUE_LEVEL_OF_SERVICE, ebsProceeding.getLevelOfService()),
                    lookupService.getCommonValue(
                        COMMON_VALUE_CLIENT_INVOLVEMENT_TYPES,
                        ebsProceeding.getClientInvolvementType()))
                .block())
        .orElseThrow(() -> new CaabApplicationException(
            "Failed to retrieve lookup data for ProceedingDetail"));

    // Calculate the overall cost limitation for this proceeding
    BigDecimal proceedingCostLimitation =
        this.calculateProceedingCostLimitation(ebsProceeding, ebsCase);

    // Build a List of pairs of Scope Limitation and associated lookup
    List<Pair<ScopeLimitation, CommonLookupValueDetail>> scopeLimitations =
        ebsProceeding.getScopeLimitations().stream()
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

    final ProceedingDetail proceedingLookup =
        lookupTuple.getT1();

    final CommonLookupValueDetail proceedingStatusLookup =
        lookupTuple.getT2().orElse(new CommonLookupValueDetail()
            .code(ebsProceeding.getStatus())
            .description(ebsProceeding.getStatus()));

    final CommonLookupValueDetail matterTypeLookup =
        lookupTuple.getT3().orElse(new CommonLookupValueDetail()
            .code(ebsProceeding.getMatterType())
            .description(ebsProceeding.getMatterType()));

    final CommonLookupValueDetail levelOfServiceLookup =
        lookupTuple.getT4().orElse(new CommonLookupValueDetail()
            .code(ebsProceeding.getLevelOfService())
            .description(ebsProceeding.getLevelOfService()));

    final CommonLookupValueDetail clientInvolvementLookup =
        lookupTuple.getT5().orElse(new CommonLookupValueDetail()
            .code(ebsProceeding.getClientInvolvementType())
            .description(ebsProceeding.getClientInvolvementType()));

    EbsProceedingMappingContext.EbsProceedingMappingContextBuilder contextBuilder =
        EbsProceedingMappingContext.builder()
            .ebsProceeding(ebsProceeding)
            .proceedingLookup(proceedingLookup)
            .proceedingStatusLookup(proceedingStatusLookup)
            .proceedingCostLimitation(proceedingCostLimitation)
            .matterType(matterTypeLookup)
            .levelOfService(levelOfServiceLookup)
            .clientInvolvement(clientInvolvementLookup)
            .scopeLimitations(scopeLimitations);

    this.addProceedingOutcomeContext(contextBuilder, ebsProceeding);

    return contextBuilder.build();
  }

  protected BigDecimal calculateProceedingCostLimitation(
      final Proceeding proceeding,
      final CaseDetail ebsCase) {
    BigDecimal maxCostLimitation = BigDecimal.ZERO;
    if (ebsCase.getApplicationDetails().getCategoryOfLaw() != null
        && proceeding.getMatterType() != null
        && proceeding.getProceedingType() != null
        && proceeding.getLevelOfService() != null
        && proceeding.getScopeLimitations() != null
        && !proceeding.getScopeLimitations().isEmpty()) {

      final String applicationType = ebsCase.getApplicationDetails().getApplicationAmendmentType();
      boolean isEmergency = APP_TYPE_EMERGENCY.equalsIgnoreCase(applicationType)
          || APP_TYPE_EMERGENCY_DEVOLVED_POWERS.equalsIgnoreCase(applicationType);

      // Build the scope limitation search criteria.
      // Only include the emergency flag in the criteria if the app type is classified as emergency.
      ScopeLimitationDetail searchCriteria =
          new ScopeLimitationDetail()
              .categoryOfLaw(ebsCase.getApplicationDetails().getCategoryOfLaw()
                  .getCategoryOfLawCode())
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
      final EbsProceedingMappingContext.EbsProceedingMappingContextBuilder contextBuilder,
      final Proceeding ebsProceeding) {

    if (ebsProceeding.getOutcome() == null) {
      return; // Nothing to add
    }

    // Lookup extra data relating to the ProceedingDetail Outcome
    final Tuple3<CommonLookupDetail,
        OutcomeResultLookupDetail,
        StageEndLookupDetail> combinedOutcomeResults = Optional.ofNullable(Mono.zip(
            lookupService.getCourts(
                ebsProceeding.getOutcome().getCourtCode()),
            lookupService.getOutcomeResults(ebsProceeding.getProceedingType(),
                ebsProceeding.getOutcome().getResult()),
            lookupService.getStageEnds(ebsProceeding.getProceedingType(),
                ebsProceeding.getOutcome().getStageEnd())).block())
        .orElseThrow(() -> new CaabApplicationException("Failed to query lookup data"));

    /*
     * Only use the looked up Court data if we got a single match.
     * Otherwise, default to the court code for display.
     */
    final CommonLookupValueDetail courtLookup =
        combinedOutcomeResults.getT1().getContent().size() == 1
            ? combinedOutcomeResults.getT1().getContent().getFirst() :
            new CommonLookupValueDetail()
                .code(ebsProceeding.getOutcome().getCourtCode())
                .description(ebsProceeding.getOutcome().getCourtCode());

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


  protected EbsCaseOutcomeMappingContext buildCaseOutcomeMappingContext(
      final CaseDetail ebsCase,
      final List<EbsProceedingMappingContext> proceedingMappingContexts) {
    // Look up all Award Types and map by their code
    Map<String, AwardTypeLookupValueDetail> awardTypes =
        Optional.ofNullable(lookupService.getAwardTypes().block())
            .map(AwardTypeLookupDetail::getContent)
            .orElseThrow(() -> new CaabApplicationException("Failed to retrieve AwardTypes"))
            .stream().collect(
                Collectors.toMap(AwardTypeLookupValueDetail::getCode, Function.identity()));

    // Split the ebs Awards into separate lists based on their award type.
    List<Award> costAwards = null;
    List<Award> financialAwards = null;
    List<Award> landAwards = null;
    List<Award> otherAssetAwards = null;

    if (ebsCase.getAwards() != null) {
      costAwards = ebsCase.getAwards().stream()
          .filter(award -> AWARD_TYPE_COST.equals(findAwardType(awardTypes, award)))
          .toList();

      financialAwards = ebsCase.getAwards().stream()
          .filter(award -> AWARD_TYPE_FINANCIAL.equals(findAwardType(awardTypes, award)))
          .toList();

      landAwards = ebsCase.getAwards().stream()
          .filter(award -> AWARD_TYPE_LAND.equals(findAwardType(awardTypes, award)))
          .toList();

      otherAssetAwards = ebsCase.getAwards().stream()
          .filter(award -> AWARD_TYPE_OTHER_ASSET.equals(findAwardType(awardTypes, award)))
          .toList();
    }

    return EbsCaseOutcomeMappingContext.builder()
        .ebsCase(ebsCase)
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
        "Failed to find AwardType with code: %s".formatted(award.getAwardType())));
  }

  /**
   * Build a mapping context to hold a EBS PriorAuthorityDetail and associated
   * lookup data.
   *
   * @param ebsPriorAuthority - the PriorAuthorityDetail to map.
   * @return a PriorAuthorityMappingContext containing all data to support mapping
   *     to a CAAB PriorAuthorityDetail.
   */
  protected EbsPriorAuthorityMappingContext buildPriorAuthorityMappingContext(
      final PriorAuthority ebsPriorAuthority) {

    // Find the correct PriorAuthorityType lookup
    PriorAuthorityTypeDetail priorAuthorityType =
        lookupService.getPriorAuthorityType(ebsPriorAuthority.getPriorAuthorityType())
            .map(priorAuthorityTypeDetail -> priorAuthorityTypeDetail
                .orElse(new PriorAuthorityTypeDetail()
                    .code(ebsPriorAuthority.getPriorAuthorityType())
                    .description(ebsPriorAuthority.getPriorAuthorityType())))
            .blockOptional()
            .orElseThrow(() -> new CaabApplicationException(
            "Failed to find PriorAuthorityType with code: %s".formatted(
                ebsPriorAuthority.getPriorAuthorityType())));

    // Build a Map of PriorAuthorityDetail keyed on code
    Map<String, PriorAuthorityDetail> priorAuthDetailMap =
        priorAuthorityType.getPriorAuthorities().stream().collect(
            Collectors.toMap(PriorAuthorityDetail::getCode,
                Function.identity()));

    // Build a List of priorAuthorityDetails paired with the common lookup for display info.
    List<Pair<PriorAuthorityDetail,
        CommonLookupValueDetail>> priorAuthorityDetails =
        ebsPriorAuthority.getDetails().stream()
            .map(priorAuthorityAttribute -> {
              PriorAuthorityDetail priorAuthorityDetail =
                  priorAuthDetailMap.get(priorAuthorityAttribute.getName());
              return Pair.of(
                  priorAuthorityDetail,
                  getPriorAuthLookup(priorAuthorityDetail, priorAuthorityAttribute));
            })
            .toList();

    return EbsPriorAuthorityMappingContext.builder()
        .ebsPriorAuthority(ebsPriorAuthority)
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
      description = lookupService.getCommonValue(
              priorAuthorityDetail.getLovCode(), priorAuthorityAttribute.getValue())
          .map(commonLookupValueDetail -> commonLookupValueDetail
              .map(CommonLookupValueDetail::getDescription)
              .orElse(priorAuthorityAttribute.getValue()))
          .blockOptional()
          .orElseThrow(() -> new CaabApplicationException(
          "Failed to find common value with code: %s".formatted(
              priorAuthorityAttribute.getValue())));
    } else {
      description = priorAuthorityAttribute.getValue();
    }

    return new CommonLookupValueDetail()
        .code(priorAuthorityAttribute.getValue())
        .description(description);
  }


}
