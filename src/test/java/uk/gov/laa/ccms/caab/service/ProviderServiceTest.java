package uk.gov.laa.ccms.caab.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import uk.gov.laa.ccms.caab.client.EbsApiClient;
import uk.gov.laa.ccms.caab.client.SoaApiClient;
import uk.gov.laa.ccms.data.model.FeeEarnerDetail;
import uk.gov.laa.ccms.soa.gateway.model.ContractDetail;
import uk.gov.laa.ccms.soa.gateway.model.ContractDetails;

@ExtendWith(MockitoExtension.class)
public class ProviderServiceTest {
  @Mock
  private SoaApiClient soaApiClient;

  @Mock
  private EbsApiClient ebsApiClient;

  @InjectMocks
  private ProviderService providerService;

  @Test
  void getCategoryOfLawCodes_returnData() {

    Integer providerFirmId = 123;
    Integer officeId = 345;
    String loginId = "user1";
    String userType = "userType";

    ContractDetails contractDetails = new ContractDetails()
        .addContractsItem(
            createContractDetail("CAT1", true, true))
        .addContractsItem(
            createContractDetail("CAT2", true, true));

    when(soaApiClient.getContractDetails(providerFirmId, officeId, loginId, userType))
        .thenReturn(Mono.just(contractDetails));

    List<String> response =
        providerService.getCategoryOfLawCodes(providerFirmId, officeId, loginId, userType,
            Boolean.TRUE);

    assertNotNull(response);
    assertEquals(2, response.size());
    assertEquals("CAT1", response.get(0));
    assertEquals("CAT2", response.get(1));
  }

  @ParameterizedTest
  @CsvSource(value = {
      "null, true, true, CAT2, true, true, true",
      "CAT1, null, true, CAT2, true, true, true",
      "CAT1, false, null, CAT2, false, true, false",
      "CAT1, false, false, CAT2, false, true, false"}, nullValues = {"null"})
  void getCategoryOfLawCodes_filtersCorrectly(String cat1, Boolean newMatters1, Boolean remAuth1,
      String cat2, Boolean newMatters2, Boolean remAuth2,
      Boolean initialApp) {

    Integer providerFirmId = 123;
    Integer officeId = 345;
    String loginId = "user1";
    String userType = "userType";

    ContractDetails contractDetails = new ContractDetails()
        .addContractsItem(
            createContractDetail(cat1, newMatters1, remAuth1))
        .addContractsItem(
            createContractDetail(cat2, newMatters2, remAuth2));

    when(soaApiClient.getContractDetails(providerFirmId, officeId, loginId, userType))
        .thenReturn(Mono.just(contractDetails));

    List<String> response =
        providerService.getCategoryOfLawCodes(providerFirmId, officeId, loginId, userType,
            initialApp);

    assertNotNull(response);
    assertEquals(1, response.size());
    assertEquals(cat2, response.get(0));
 }

  @Test
  void getFeeEarners_returnsData() {
    Integer providerId = 123;
    FeeEarnerDetail feeEarnerDetail = new FeeEarnerDetail();

    when(ebsApiClient.getFeeEarners(providerId)).thenReturn(Mono.just(feeEarnerDetail));

    Mono<FeeEarnerDetail> feeEarnerDetailMono = providerService.getFeeEarners(providerId);

    StepVerifier.create(feeEarnerDetailMono)
        .expectNext(feeEarnerDetail)
        .verifyComplete();
  }

  private static ContractDetail createContractDetail(String cat, Boolean createNewMatters,
      Boolean remainderAuth) {
    return new ContractDetail()
        .categoryofLaw(cat)
        .subCategory("SUBCAT1")
        .createNewMatters(createNewMatters)
        .remainderAuthorisation(remainderAuth)
        .contractualDevolvedPowers("CATDEVPOW")
        .authorisationType("AUTHTYPE1");
  }


}
