package uk.gov.laa.ccms.caab.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_ORGANISATION_TYPES;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.bean.opponent.AbstractOpponentFormData;
import uk.gov.laa.ccms.caab.bean.opponent.OrganisationOpponentFormData;
import uk.gov.laa.ccms.caab.bean.opponent.OrganisationSearchCriteria;
import uk.gov.laa.ccms.caab.client.SoaApiClient;
import uk.gov.laa.ccms.caab.constants.SearchConstants;
import uk.gov.laa.ccms.caab.exception.TooManyResultsException;
import uk.gov.laa.ccms.caab.mapper.OpponentMapper;
import uk.gov.laa.ccms.caab.mapper.ResultDisplayMapper;
import uk.gov.laa.ccms.caab.model.OrganisationResultRowDisplay;
import uk.gov.laa.ccms.caab.model.ResultsDisplay;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;
import uk.gov.laa.ccms.soa.gateway.model.OrganisationDetail;
import uk.gov.laa.ccms.soa.gateway.model.OrganisationDetails;

@ExtendWith(MockitoExtension.class)
public class OpponentServiceTest {
  @Mock private SoaApiClient soaApiClient;

  @Mock private LookupService lookupService;

  @Mock private ResultDisplayMapper resultDisplayMapper;

  @Mock private SearchConstants searchConstants;

  @Mock private OpponentMapper opponentMapper;

  @InjectMocks private OpponentService opponentService;

  @Test
  void getOrganisations_returnsdata() {
    String loginId = "login";
    String userType = "test";
    int page = 1;
    int size = 20;

    OrganisationSearchCriteria organisationSearchCriteria = new OrganisationSearchCriteria();
    OrganisationDetails organisationDetails = new OrganisationDetails().totalElements(1);

    ResultsDisplay<OrganisationResultRowDisplay> expectedResults = new ResultsDisplay<>();

    when(searchConstants.getMaxSearchResultsOrganisations()).thenReturn(10);
    when(soaApiClient.getOrganisations(organisationSearchCriteria, loginId, userType, page, size))
        .thenReturn(Mono.just(organisationDetails));
    CommonLookupDetail commonLookupDetail =
        new CommonLookupDetail().addContentItem(new CommonLookupValueDetail());
    when(lookupService.getCommonValues(COMMON_VALUE_ORGANISATION_TYPES))
        .thenReturn(Mono.just(commonLookupDetail));
    when(resultDisplayMapper.toOrganisationResultsDisplay(
            organisationDetails, commonLookupDetail.getContent()))
        .thenReturn(expectedResults);

    ResultsDisplay<OrganisationResultRowDisplay> result =
        opponentService.getOrganisations(organisationSearchCriteria, loginId, userType, page, size);

    assertEquals(expectedResults, result);
  }

  @Test
  void getOrganisations_tooManyResults_throwsException() {
    String loginId = "login";
    String userType = "test";
    int page = 1;
    int size = 20;

    OrganisationSearchCriteria organisationSearchCriteria = new OrganisationSearchCriteria();
    OrganisationDetails organisationDetails = new OrganisationDetails().totalElements(11);

    when(searchConstants.getMaxSearchResultsOrganisations()).thenReturn(10);
    when(soaApiClient.getOrganisations(organisationSearchCriteria, loginId, userType, page, size))
        .thenReturn(Mono.just(organisationDetails));

    assertThrows(
        TooManyResultsException.class,
        () ->
            opponentService.getOrganisations(
                organisationSearchCriteria, loginId, userType, page, size));

    verifyNoInteractions(lookupService);
    verifyNoInteractions(resultDisplayMapper);
  }

  @Test
  void getOrganisation_returnsdata() {
    String loginId = "login";
    String userType = "test";
    String orgId = "123";

    OrganisationDetail organisationDetail = new OrganisationDetail().type("type1");

    OrganisationOpponentFormData expectedResults = new OrganisationOpponentFormData();

    when(soaApiClient.getOrganisation(orgId, loginId, userType))
        .thenReturn(Mono.just(organisationDetail));
    CommonLookupValueDetail orgLookup = new CommonLookupValueDetail();
    when(lookupService.getCommonValue(
            COMMON_VALUE_ORGANISATION_TYPES, organisationDetail.getType()))
        .thenReturn(Mono.just(Optional.of(orgLookup)));
    when(opponentMapper.toOrganisationOpponentFormData(organisationDetail, orgLookup))
        .thenReturn(expectedResults);

    AbstractOpponentFormData result =
        opponentService.getOrganisationOpponent(orgId, loginId, userType);

    assertEquals(expectedResults, result);
  }
}
