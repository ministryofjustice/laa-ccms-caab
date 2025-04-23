package uk.gov.laa.ccms.caab.service;

import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_ORGANISATION_TYPES;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.laa.ccms.caab.bean.opponent.AbstractOpponentFormData;
import uk.gov.laa.ccms.caab.bean.opponent.OrganisationOpponentFormData;
import uk.gov.laa.ccms.caab.bean.opponent.OrganisationSearchCriteria;
import uk.gov.laa.ccms.caab.client.CaabApiClient;
import uk.gov.laa.ccms.caab.client.SoaApiClient;
import uk.gov.laa.ccms.caab.constants.SearchConstants;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.exception.TooManyResultsException;
import uk.gov.laa.ccms.caab.mapper.OpponentMapper;
import uk.gov.laa.ccms.caab.mapper.ResultDisplayMapper;
import uk.gov.laa.ccms.caab.model.OrganisationResultRowDisplay;
import uk.gov.laa.ccms.caab.model.ResultsDisplay;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;
import uk.gov.laa.ccms.data.model.UserDetail;
import uk.gov.laa.ccms.soa.gateway.model.OrganisationDetail;
import uk.gov.laa.ccms.soa.gateway.model.OrganisationDetails;

/**
 * Service class to handle Opponents.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OpponentService {
  private final SoaApiClient soaApiClient;

  private final CaabApiClient caabApiClient;

  private final LookupService lookupService;

  private final ResultDisplayMapper resultDisplayMapper;

  private final SearchConstants searchConstants;

  private final OpponentMapper opponentMapper;


  /**
   * Searches and retrieves organisation details based on provided search criteria.
   *
   * @param searchCriteria        The search criteria to use when fetching organisations.
   * @param loginId               The login identifier for the user.
   * @param userType              Type of the user (e.g., admin, user).
   * @param page                  The page number for pagination.
   * @param size                  The size or number of records per page.
   * @return A Mono wrapping the OrganisationDetails.
   */
  public ResultsDisplay<OrganisationResultRowDisplay> getOrganisations(
      final OrganisationSearchCriteria searchCriteria,
      final String loginId,
      final String userType,
      final Integer page,
      final Integer size) {
    log.debug("SOA Organisations to get using criteria: {}", searchCriteria);

    OrganisationDetails organisationDetails =
        Optional.ofNullable(soaApiClient.getOrganisations(
                searchCriteria,
                loginId,
                userType,
                page,
                size).block())
            .orElseThrow(() -> new CaabApplicationException("Failed to query for organisations"));

    if (organisationDetails.getTotalElements()
        > searchConstants.getMaxSearchResultsOrganisations()) {
      throw new TooManyResultsException(
          "Organisation Search returned %s results".formatted(
              organisationDetails.getTotalElements()));
    }

    CommonLookupDetail organisationTypes = Optional.ofNullable(
            lookupService.getCommonValues(COMMON_VALUE_ORGANISATION_TYPES).block())
        .orElse(new CommonLookupDetail());

    return resultDisplayMapper.toOrganisationResultsDisplay(
        organisationDetails,
        organisationTypes.getContent());
  }

  /**
   * Get a shared organisation opponent for the supplied id.
   *
   * @param organisationId        The id of the organisation to retrieve and convert to an opponent.
   * @param loginId               The login identifier for the user.
   * @param userType              Type of the user (e.g., admin, user).
   * @return AbstractOpponentFormData based on the organisation data.
   */
  public OrganisationOpponentFormData getOrganisationOpponent(
      final String organisationId,
      final String loginId,
      final String userType) {

    OrganisationDetail organisation =
        Optional.ofNullable(soaApiClient.getOrganisation(
                organisationId,
                loginId,
                userType).block())
            .orElseThrow(() -> new CaabApplicationException("Failed to query for organisation"));

    // Lookup the display value for the organisation type.
    CommonLookupValueDetail orgType =
        lookupService.getCommonValue(COMMON_VALUE_ORGANISATION_TYPES, organisation.getType())
            .map(commonLookupValueDetail -> commonLookupValueDetail
                .orElse(new CommonLookupValueDetail()
                    .code(organisation.getType())
                    .description(organisation.getType())))
            .blockOptional()
            .orElseThrow(
                () -> new CaabApplicationException("Failed to retrieve organisation type lookup"));

    return opponentMapper.toOrganisationOpponentFormData(organisation, orgType);
  }

  /**
   * Update an opponent based on the supplied form data.
   *
   * @param opponentFormData - the opponent form data.
   * @param userDetail - the user related user.
   */
  public void updateOpponent(
      final Integer opponentId,
      final AbstractOpponentFormData opponentFormData,
      final UserDetail userDetail) {

    caabApiClient.updateOpponent(
        opponentId,
        opponentMapper.toOpponent(opponentFormData),
        userDetail.getLoginId()).block();
  }

  /**
   * Deletes a specified opponent.
   *
   * @param opponentId the ID of the opponent to delete
   * @param user the user details initiating the deletion
   */
  public void deleteOpponent(
      final Integer opponentId,
      final UserDetail user) {
    caabApiClient.deleteOpponent(opponentId, user.getLoginId()).block();
  }

}
