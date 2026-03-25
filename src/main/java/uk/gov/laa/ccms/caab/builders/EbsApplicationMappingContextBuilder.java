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
import java.util.LinkedHashMap;
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
 * The {@link EbsApplicationMappingContextBuilder} class is responsible for building a {@link
 * EbsApplicationMappingContext} object.
 *
 * <p>Various look up services are used to build this object, such as:
 *
 * <ul>
 *   <li>{@link ProviderService}
 *   <li>{@link LookupService}
 *   <li>{@link EbsApiClient}
 * </ul>
 *
 * @author Jamie Briggs
 */
@Service
@RequiredArgsConstructor
public class EbsApplicationMappingContextBuilder {

  private final ProviderService providerService;
  private final LookupService lookupService;
  private final EbsApiClient ebsApiClient;

  /**
   * Before a CaseDetail can be mapped to a CAAB ApplicationDetail further lookup data and
   * calculations need to be performed. This method builds a wrapper object to hold all the required
   * data for the mapping.
   *
   * @param ebsCase - the EBS CaseDetail.
   * @return a Mono containing an ApplicationMappingContext for the CaseDetail.
   */
  public EbsApplicationMappingContext buildApplicationMappingContext(final CaseDetail ebsCase) {
    final SubmittedApplicationDetails ebsApplicationDetails = ebsCase.getApplicationDetails();
    final ProviderDetails ebsProvider = ebsApplicationDetails.getProviderDetails();

    // Determine whether all proceedings are at status DRAFT
    final boolean caseWithOnlyDraftProceedings =
        ebsApplicationDetails.getProceedings() != null
            && ebsApplicationDetails.getProceedings().stream()
                .allMatch(p -> STATUS_DRAFT.equalsIgnoreCase(p.getStatus()));

    // Retrieve the full provider details
    final ProviderDetail providerDetail =
        Optional.ofNullable(
                providerService
                    .getProvider(Integer.parseInt(ebsProvider.getProviderFirmId()))
                    .block())
            .orElseThrow(
                () ->
                    new CaabApplicationException(
                        "Failed to query lookup data for Application mapping"));

    // Lookup the certificate display value
    final CommonLookupValueDetail certificateLookup =
        ebsCase.getCertificateType() != null
            ? lookupService
                .getCommonValue(COMMON_VALUE_APPLICATION_TYPE, ebsCase.getCertificateType())
                .map(
                    detail ->
                        detail.orElse(
                            new CommonLookupValueDetail()
                                .code(ebsCase.getCertificateType())
                                .description(ebsCase.getCertificateType())))
                .blockOptional()
                .orElseThrow(
                    () ->
                        new CaabApplicationException(
                            "Failed to retrieve applicationtype with code: %s"
                                .formatted(ebsCase.getCertificateType())))
            : null;

    // Lookup the application type display value — based on the application/amendment type
    // if present, otherwise falls back to the certificate type.
    final CommonLookupValueDetail applicationTypeLookup =
        ebsApplicationDetails.getApplicationAmendmentType() != null
            ? lookupService
                .getCommonValue(
                    COMMON_VALUE_APPLICATION_TYPE,
                    ebsApplicationDetails.getApplicationAmendmentType())
                .mapNotNull(detail -> detail.orElse(certificateLookup))
                .blockOptional()
                .orElseThrow(
                    () ->
                        new CaabApplicationException(
                            "Failed to retrieve applicationtype with code: %s"
                                .formatted(ebsApplicationDetails.getApplicationAmendmentType())))
            : certificateLookup;

    // Find the correct provider office
    final OfficeDetail providerOffice =
        providerDetail.getOffices().stream()
            .filter(
                office -> ebsProvider.getProviderOfficeId().equals(String.valueOf(office.getId())))
            .findAny()
            .orElseThrow(
                () ->
                    new CaabApplicationException(
                        "Failed to find Office with id: %s"
                            .formatted(ebsProvider.getProviderOfficeId())));

    // Get the fee earners for the relevant office, mapped by contact id
    final Map<Integer, ContactDetail> feeEarnerById =
        providerOffice.getFeeEarners().stream()
            .collect(Collectors.toMap(ContactDetail::getId, Function.identity()));

    // Resolve supervisor and fee earner contacts
    final ContactDetail supervisorContact =
        Objects.nonNull(ebsProvider.getSupervisorContactId())
            ? feeEarnerById.get(Integer.valueOf(ebsProvider.getSupervisorContactId()))
            : null;

    final ContactDetail feeEarnerContact =
        Objects.nonNull(ebsProvider.getFeeEarnerContactId())
            ? feeEarnerById.get(Integer.valueOf(ebsProvider.getFeeEarnerContactId()))
            : null;

    // Set the DevolvedPowers for the Application based on the ApplicationAmendmentType
    final boolean isDevolvedPowers =
        APP_TYPE_EMERGENCY_DEVOLVED_POWERS.equalsIgnoreCase(
                ebsApplicationDetails.getApplicationAmendmentType())
            || APP_TYPE_SUBSTANTIVE_DEVOLVED_POWERS.equalsIgnoreCase(
                ebsApplicationDetails.getApplicationAmendmentType());
    final Pair<Boolean, LocalDate> devolvedPowersInfo =
        Pair.of(
            isDevolvedPowers,
            isDevolvedPowers ? ebsApplicationDetails.getDevolvedPowersDate() : null);

    // Calculate the CurrentProviderBilledAmount for the Application's Costs
    final BigDecimal currentProviderBilledAmount =
        calculateCurrentProviderBilledAmount(ebsApplicationDetails.getCategoryOfLaw());

    // Find the most recent assessments
    final AssessmentResult meansAssessment =
        getMostRecentAssessment(ebsApplicationDetails.getMeansAssessments());
    final AssessmentResult meritsAssessment =
        getMostRecentAssessment(ebsApplicationDetails.getMeritsAssessments());

    /*
     * Split the proceeding list based on status, and build a ProceedingMappingContext
     * for each to hold the necessary lookup data.
     */
    final List<EbsProceedingMappingContext> amendmentProceedingsInEbs =
        ebsApplicationDetails.getProceedings() != null
            ? ebsApplicationDetails.getProceedings().stream()
                .filter(
                    p ->
                        !caseWithOnlyDraftProceedings
                            && STATUS_DRAFT.equalsIgnoreCase(p.getStatus()))
                .map(p -> buildProceedingMappingContext(p, ebsCase))
                .toList()
            : Collections.emptyList();

    final List<EbsProceedingMappingContext> proceedings =
        ebsApplicationDetails.getProceedings() != null
            ? ebsApplicationDetails.getProceedings().stream()
                .filter(
                    p ->
                        caseWithOnlyDraftProceedings
                            || !STATUS_DRAFT.equalsIgnoreCase(p.getStatus()))
                .map(p -> buildProceedingMappingContext(p, ebsCase))
                .toList()
            : Collections.emptyList();

    // Build a mapping context for each Prior Authority in the application
    final List<EbsPriorAuthorityMappingContext> priorAuthorities =
        ebsCase.getPriorAuthorities() != null
            ? ebsCase.getPriorAuthorities().stream()
                .map(this::buildPriorAuthorityMappingContext)
                .toList()
            : Collections.emptyList();

    // Build a mapping context for the case outcome
    final EbsCaseOutcomeMappingContext caseOutcomeMappingContext =
        buildCaseOutcomeMappingContext(
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
    return assessmentResults != null
        ? assessmentResults.stream()
            .max(
                Comparator.comparing(
                    AssessmentResult::getDate, Comparator.nullsFirst(Comparator.naturalOrder())))
            .orElse(null)
        : null;
  }

  protected EbsProceedingMappingContext buildProceedingMappingContext(
      final Proceeding ebsProceeding, final CaseDetail ebsCase) {

    Tuple5<
            ProceedingDetail,
            Optional<CommonLookupValueDetail>,
            Optional<CommonLookupValueDetail>,
            Optional<CommonLookupValueDetail>,
            Optional<CommonLookupValueDetail>>
        lookupTuple =
            Optional.ofNullable(
                    Mono.zip(
                            ebsApiClient.getProceeding(ebsProceeding.getProceedingType()),
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
                .orElseThrow(
                    () ->
                        new CaabApplicationException(
                            "Failed to retrieve lookup data for ProceedingDetail"));

    // Calculate the overall cost limitation for this proceeding
    final BigDecimal proceedingCostLimitation =
        calculateProceedingCostLimitation(ebsProceeding, ebsCase);

    // Build a list of pairs of ScopeLimitation and associated lookup
    final List<Pair<ScopeLimitation, CommonLookupValueDetail>> scopeLimitations =
        ebsProceeding.getScopeLimitations().stream()
            .map(
                scopeLimitation ->
                    Pair.of(
                        scopeLimitation,
                        lookupService
                            .getCommonValue(
                                COMMON_VALUE_SCOPE_LIMITATIONS,
                                scopeLimitation.getScopeLimitation())
                            .map(
                                detail ->
                                    detail.orElse(
                                        new CommonLookupValueDetail()
                                            .code(scopeLimitation.getScopeLimitation())
                                            .description(scopeLimitation.getScopeLimitation())))
                            .block()))
            .toList();

    final CommonLookupValueDetail proceedingStatusLookup =
        lookupTuple
            .getT2()
            .orElse(
                new CommonLookupValueDetail()
                    .code(ebsProceeding.getStatus())
                    .description(ebsProceeding.getStatus()));

    final CommonLookupValueDetail matterTypeLookup =
        lookupTuple
            .getT3()
            .orElse(
                new CommonLookupValueDetail()
                    .code(ebsProceeding.getMatterType())
                    .description(ebsProceeding.getMatterType()));

    final CommonLookupValueDetail levelOfServiceLookup =
        lookupTuple
            .getT4()
            .orElse(
                new CommonLookupValueDetail()
                    .code(ebsProceeding.getLevelOfService())
                    .description(ebsProceeding.getLevelOfService()));

    final CommonLookupValueDetail clientInvolvementLookup =
        lookupTuple
            .getT5()
            .orElse(
                new CommonLookupValueDetail()
                    .code(ebsProceeding.getClientInvolvementType())
                    .description(ebsProceeding.getClientInvolvementType()));

    EbsProceedingMappingContext.EbsProceedingMappingContextBuilder contextBuilder =
        EbsProceedingMappingContext.builder()
            .ebsProceeding(ebsProceeding)
            .proceedingLookup(lookupTuple.getT1())
            .proceedingStatusLookup(proceedingStatusLookup)
            .proceedingCostLimitation(proceedingCostLimitation)
            .matterType(matterTypeLookup)
            .levelOfService(levelOfServiceLookup)
            .clientInvolvement(clientInvolvementLookup)
            .scopeLimitations(scopeLimitations);

    addProceedingOutcomeContext(contextBuilder, ebsProceeding);

    return contextBuilder.build();
  }

  protected BigDecimal calculateProceedingCostLimitation(
      final Proceeding proceeding, final CaseDetail ebsCase) {

    if (ebsCase.getApplicationDetails().getCategoryOfLaw() == null
        || proceeding.getMatterType() == null
        || proceeding.getProceedingType() == null
        || proceeding.getLevelOfService() == null
        || proceeding.getScopeLimitations() == null
        || proceeding.getScopeLimitations().isEmpty()) {
      return BigDecimal.ZERO;
    }

    final String applicationType = ebsCase.getApplicationDetails().getApplicationAmendmentType();
    final boolean isEmergency =
        APP_TYPE_EMERGENCY.equalsIgnoreCase(applicationType)
            || APP_TYPE_EMERGENCY_DEVOLVED_POWERS.equalsIgnoreCase(applicationType);

    // Build the scope limitation search criteria.
    // Only include the emergency flag if the app type is classified as emergency.
    final ScopeLimitationDetail searchCriteria =
        new ScopeLimitationDetail()
            .categoryOfLaw(
                ebsCase.getApplicationDetails().getCategoryOfLaw().getCategoryOfLawCode())
            .matterType(proceeding.getMatterType())
            .proceedingCode(proceeding.getProceedingType())
            .levelOfService(proceeding.getLevelOfService())
            .emergency(isEmergency ? Boolean.TRUE : null);

    return proceeding.getScopeLimitations().stream()
        .map(
            limitation -> {
              searchCriteria.setScopeLimitations(limitation.getScopeLimitation());
              return lookupService
                  .getScopeLimitationDetails(searchCriteria)
                  .map(
                      details ->
                          details.getContent() == null
                              ? BigDecimal.ZERO
                              : details.getContent().stream()
                                  .findFirst()
                                  .map(
                                      d ->
                                          isEmergency
                                              ? d.getEmergencyCostLimitation()
                                              : d.getCostLimitation())
                                  .orElse(BigDecimal.ZERO))
                  .block();
            })
        .filter(Objects::nonNull)
        .reduce(BigDecimal.ZERO, BigDecimal::max);
  }

  protected void addProceedingOutcomeContext(
      final EbsProceedingMappingContext.EbsProceedingMappingContextBuilder contextBuilder,
      final Proceeding ebsProceeding) {

    if (ebsProceeding.getOutcome() == null) {
      return;
    }

    // Lookup extra data relating to the ProceedingDetail Outcome
    final Tuple3<CommonLookupDetail, OutcomeResultLookupDetail, StageEndLookupDetail>
        combinedOutcomeResults =
            Optional.ofNullable(
                    Mono.zip(
                            lookupService.getCourts(ebsProceeding.getOutcome().getCourtCode()),
                            lookupService.getOutcomeResults(
                                ebsProceeding.getProceedingType(),
                                ebsProceeding.getOutcome().getResult()),
                            lookupService.getStageEnds(
                                ebsProceeding.getProceedingType(),
                                ebsProceeding.getOutcome().getStageEnd()))
                        .block())
                .orElseThrow(() -> new CaabApplicationException("Failed to query lookup data"));

    /*
     * Only use the looked-up Court data if we got a single match.
     * Otherwise, default to the court code for display.
     */
    final CommonLookupValueDetail courtLookup =
        combinedOutcomeResults.getT1().getContent().size() == 1
            ? combinedOutcomeResults.getT1().getContent().getFirst()
            : new CommonLookupValueDetail()
                .code(ebsProceeding.getOutcome().getCourtCode())
                .description(ebsProceeding.getOutcome().getCourtCode());

    final OutcomeResultLookupValueDetail outcomeResultLookup =
        combinedOutcomeResults.getT2().getContent().isEmpty()
            ? null
            : combinedOutcomeResults.getT2().getContent().getFirst();

    final StageEndLookupValueDetail stageEndLookup =
        combinedOutcomeResults.getT3().getContent().isEmpty()
            ? null
            : combinedOutcomeResults.getT3().getContent().getFirst();

    contextBuilder
        .courtLookup(courtLookup)
        .outcomeResultLookup(outcomeResultLookup)
        .stageEndLookup(stageEndLookup);
  }

  protected EbsCaseOutcomeMappingContext buildCaseOutcomeMappingContext(
      final CaseDetail ebsCase, final List<EbsProceedingMappingContext> proceedingMappingContexts) {

    // Look up all Award Types and map by their code
    final Map<String, AwardTypeLookupValueDetail> awardTypes =
        Optional.ofNullable(lookupService.getAwardTypes().block())
            .map(AwardTypeLookupDetail::getContent)
            .orElseThrow(() -> new CaabApplicationException("Failed to retrieve AwardTypes"))
            .stream()
            .collect(Collectors.toMap(AwardTypeLookupValueDetail::getCode, Function.identity()));

    // Split awards into typed lists in a single pass
    final Map<String, List<Award>> awardsByType =
        ebsCase.getAwards() != null
            ? ebsCase.getAwards().stream()
                .collect(Collectors.groupingBy(award -> findAwardType(awardTypes, award)))
            : Collections.emptyMap();

    return EbsCaseOutcomeMappingContext.builder()
        .ebsCase(ebsCase)
        .costAwards(awardsByType.get(AWARD_TYPE_COST))
        .financialAwards(awardsByType.get(AWARD_TYPE_FINANCIAL))
        .landAwards(awardsByType.get(AWARD_TYPE_LAND))
        .otherAssetAwards(awardsByType.get(AWARD_TYPE_OTHER_ASSET))
        .proceedingOutcomes(proceedingMappingContexts)
        .build();
  }

  private String findAwardType(
      final Map<String, AwardTypeLookupValueDetail> awardTypes, final Award award) {
    return Optional.ofNullable(awardTypes.get(award.getAwardType()))
        .map(AwardTypeLookupValueDetail::getAwardType)
        .orElseThrow(
            () ->
                new CaabApplicationException(
                    "Failed to find AwardType with code: %s".formatted(award.getAwardType())));
  }

  /**
   * Build a mapping context to hold an EBS PriorAuthorityDetail and associated lookup data.
   *
   * @param ebsPriorAuthority - the PriorAuthorityDetail to map.
   * @return a PriorAuthorityMappingContext containing all data to support mapping to a CAAB
   *     PriorAuthorityDetail.
   */
  protected EbsPriorAuthorityMappingContext buildPriorAuthorityMappingContext(
      final PriorAuthority ebsPriorAuthority) {

    // Find the correct PriorAuthorityType lookup
    final PriorAuthorityTypeDetail priorAuthorityType =
        lookupService
            .getPriorAuthorityType(ebsPriorAuthority.getPriorAuthorityType())
            .map(
                detail ->
                    detail.orElse(
                        new PriorAuthorityTypeDetail()
                            .code(ebsPriorAuthority.getPriorAuthorityType())
                            .description(ebsPriorAuthority.getPriorAuthorityType())))
            .blockOptional()
            .orElseThrow(
                () ->
                    new CaabApplicationException(
                        "Failed to find PriorAuthorityType with code: %s"
                            .formatted(ebsPriorAuthority.getPriorAuthorityType())));

    // Build a Map of PriorAuthorityDetail keyed on code
    final Map<String, PriorAuthorityDetail> priorAuthDetailMap =
        priorAuthorityType.getPriorAuthorities().stream()
            .collect(Collectors.toMap(PriorAuthorityDetail::getCode, Function.identity()));

    // Build a list of priorAuthorityDetails paired with the common lookup for display info
    final List<Pair<PriorAuthorityDetail, CommonLookupValueDetail>> priorAuthorityDetails =
        ebsPriorAuthority.getDetails().stream()
            .map(
                attr -> {
                  PriorAuthorityDetail detail = priorAuthDetailMap.get(attr.getName());
                  return Pair.of(detail, getPriorAuthLookup(detail, attr));
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

    // If this attribute is of type LOV, look up the corresponding LOV record for display value
    final String description;
    if (priorAuthorityDetail != null
        && REFERENCE_DATA_ITEM_TYPE_LOV.equals(priorAuthorityDetail.getDataType())) {
      description =
          lookupService
              .getCommonValue(priorAuthorityDetail.getLovCode(), priorAuthorityAttribute.getValue())
              .map(
                  detail ->
                      detail
                          .map(CommonLookupValueDetail::getDescription)
                          .orElse(priorAuthorityAttribute.getValue()))
              .blockOptional()
              .orElseThrow(
                  () ->
                      new CaabApplicationException(
                          "Failed to find common value with code: %s"
                              .formatted(priorAuthorityAttribute.getValue())));
    } else {
      description = priorAuthorityAttribute.getValue();
    }

    return new CommonLookupValueDetail()
        .code(priorAuthorityAttribute.getValue())
        .description(description);
  }

  private BigDecimal calculateCurrentProviderBilledAmount(final CategoryOfLaw categoryOfLaw) {
    if (categoryOfLaw == null) {
      return BigDecimal.ZERO;
    }

    final List<CostLimitation> costLimitations = categoryOfLaw.getCostLimitations();
    if (costLimitations != null && !costLimitations.isEmpty()) {
      deduplicateCostLimitations(costLimitations);
    }

    if (costLimitations == null || categoryOfLaw.getTotalPaidToDate() == null) {
      return BigDecimal.ZERO;
    }

    final BigDecimal totalPaidToDate =
        costLimitations.stream()
            .filter(Objects::nonNull)
            .map(CostLimitation::getPaidToDate)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    return categoryOfLaw.getTotalPaidToDate().subtract(totalPaidToDate);
  }

  private void deduplicateCostLimitations(final List<CostLimitation> costLimitations) {
    final var deduplicated = new LinkedHashMap<String, CostLimitation>();

    for (int i = 0; i < costLimitations.size(); i++) {
      final CostLimitation cl = costLimitations.get(i);
      final String key = computeCostLimitationKey(cl, i);

      if (cl == null) {
        deduplicated.put(key, null);
      } else {
        deduplicated.merge(key, cl, this::mergeCostLimitations);
      }
    }

    if (deduplicated.size() != costLimitations.size()) {
      costLimitations.clear();
      costLimitations.addAll(deduplicated.values());
    }
  }

  /** Aligns with legacy PUI uniqueness: LSC resource + cost category. */
  private String computeCostLimitationKey(final CostLimitation cl, final int index) {
    if (cl == null) {
      return "idx:" + index;
    }

    final String costCategory = Objects.toString(cl.getCostCategory(), "");

    if (isNotBlank(cl.getBillingProviderId())) {
      return cl.getBillingProviderId() + "|" + costCategory;
    }
    if (isNotBlank(cl.getCostLimitId())) {
      return "ebs:" + cl.getCostLimitId();
    }
    if (isNotBlank(cl.getBillingProviderName())) {
      return "name:" + cl.getBillingProviderName() + "|" + costCategory;
    }
    return "idx:" + index;
  }

  private CostLimitation mergeCostLimitations(
      final CostLimitation existing, final CostLimitation incoming) {

    final boolean existingHasValues = hasPositiveValues(existing);
    final boolean incomingHasValues = hasPositiveValues(incoming);

    if (existingHasValues && !incomingHasValues) {
      return existing;
    }
    if (!existingHasValues && incomingHasValues) {
      return incoming;
    }

    final BigDecimal existingAmount = existing.getAmount();
    final BigDecimal incomingAmount = incoming.getAmount();
    if (incomingAmount != null
        && existingAmount != null
        && incomingAmount.compareTo(existingAmount) > 0) {
      return incoming;
    }

    final BigDecimal existingPaid = existing.getPaidToDate();
    final BigDecimal incomingPaid = incoming.getPaidToDate();
    if (incomingPaid != null && existingPaid != null && incomingPaid.compareTo(existingPaid) > 0) {
      return incoming;
    }

    return existing;
  }

  private boolean hasPositiveValues(final CostLimitation cl) {
    return isPositive(cl.getAmount()) || isPositive(cl.getPaidToDate());
  }

  private boolean isPositive(final BigDecimal value) {
    return value != null && value.compareTo(BigDecimal.ZERO) > 0;
  }

  private boolean isNotBlank(final String value) {
    return value != null && !value.isBlank();
  }
}
