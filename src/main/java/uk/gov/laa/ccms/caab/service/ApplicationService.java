package uk.gov.laa.ccms.caab.service;

import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_CATEGORY_OF_LAW;

import java.text.ParseException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
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
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.ApplicationProviderDetails;
import uk.gov.laa.ccms.caab.model.ApplicationSummaryDisplay;
import uk.gov.laa.ccms.caab.model.ApplicationType;
import uk.gov.laa.ccms.caab.model.IntDisplayValue;
import uk.gov.laa.ccms.caab.model.StringDisplayValue;
import uk.gov.laa.ccms.data.model.AmendmentTypeLookupDetail;
import uk.gov.laa.ccms.data.model.CaseStatusLookupDetail;
import uk.gov.laa.ccms.data.model.CaseStatusLookupValueDetail;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.ContactDetail;
import uk.gov.laa.ccms.data.model.ProviderDetail;
import uk.gov.laa.ccms.data.model.RelationshipToCaseLookupDetail;
import uk.gov.laa.ccms.data.model.RelationshipToCaseLookupValueDetail;
import uk.gov.laa.ccms.data.model.UserDetail;
import uk.gov.laa.ccms.soa.gateway.model.CaseDetails;
import uk.gov.laa.ccms.soa.gateway.model.CaseReferenceSummary;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetail;
import uk.gov.laa.ccms.soa.gateway.model.ContractDetails;

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
   * Fetches a unique case reference.
   *
   * @param loginId   The login identifier for the user.
   * @param userType  Type of the user (e.g., admin, user).
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
              ebsApiClient.getCommonValues(COMMON_VALUE_CATEGORY_OF_LAW),
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
              .clientReferenceNumber(
                  application.getClient().getReference())
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


}
