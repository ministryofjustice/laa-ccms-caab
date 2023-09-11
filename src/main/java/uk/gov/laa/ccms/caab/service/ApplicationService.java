package uk.gov.laa.ccms.caab.service;

import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_CATEGORY_OF_LAW;

import java.text.ParseException;
import java.util.Collections;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple4;
import uk.gov.laa.ccms.caab.bean.ApplicationDetails;
import uk.gov.laa.ccms.caab.bean.CopyCaseSearchCriteria;
import uk.gov.laa.ccms.caab.client.CaabApiClient;
import uk.gov.laa.ccms.caab.client.EbsApiClient;
import uk.gov.laa.ccms.caab.client.SoaApiClient;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.util.ApplicationBuilder;
import uk.gov.laa.ccms.data.model.AmendmentTypeLookupDetail;
import uk.gov.laa.ccms.data.model.CaseStatusLookupDetail;
import uk.gov.laa.ccms.data.model.CaseStatusLookupValueDetail;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
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
   * @param applicationDetails - The details of the Application to create
   * @param clientDetail - The client details
   * @param user - The related User.
   * @return a Mono Void
   */
  public Mono<Void> createApplication(ApplicationDetails applicationDetails,
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
              ebsApiClient.getCommonValues(COMMON_VALUE_CATEGORY_OF_LAW),
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

}
