package uk.gov.laa.ccms.caab.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
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
import uk.gov.laa.ccms.data.model.ContactDetail;
import uk.gov.laa.ccms.data.model.OfficeDetail;
import uk.gov.laa.ccms.data.model.ProviderDetail;
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
  void getProvider_returnsData() {
    Integer providerId = 123;
    ProviderDetail providerDetail = new ProviderDetail();

    when(ebsApiClient.getProvider(providerId)).thenReturn(Mono.just(providerDetail));

    Mono<ProviderDetail> providerMono = providerService.getProvider(providerId);

    StepVerifier.create(providerMono)
        .expectNext(providerDetail)
        .verifyComplete();
  }

  @Test
  void getAllFeeEarners_returnsData() {
    ProviderDetail providerDetail = buildProvider();

    List<ContactDetail> feeEarners = providerService.getAllFeeEarners(providerDetail);

    assertNotNull(feeEarners);
    assertEquals(3, feeEarners.size());
    assertEquals("FeeEarner1", feeEarners.get(0).getName());
    assertEquals("FeeEarner2", feeEarners.get(1).getName());
    assertEquals("FeeEarner3", feeEarners.get(2).getName());
  }

  @Test
  void getAllFeeEarners_handlesNoResults() {
    ProviderDetail providerDetail = new ProviderDetail()
        .addOfficesItem(new OfficeDetail()
            .feeEarners(new ArrayList<>()));

    List<ContactDetail> feeEarners = providerService.getAllFeeEarners(providerDetail);

    assertNotNull(feeEarners);
    assertTrue(feeEarners.isEmpty());
  }


  private ContractDetail createContractDetail(String cat, Boolean createNewMatters,
      Boolean remainderAuth) {
    return new ContractDetail()
        .categoryofLaw(cat)
        .subCategory("SUBCAT1")
        .createNewMatters(createNewMatters)
        .remainderAuthorisation(remainderAuth)
        .contractualDevolvedPowers("CATDEVPOW")
        .authorisationType("AUTHTYPE1");
  }

  private ProviderDetail buildProvider() {
    return new ProviderDetail()
        .id(123)
        .name("provider1")
        .addOfficesItem(new OfficeDetail()
            .id(10)
            .name("Office 1")
            .addFeeEarnersItem(new ContactDetail()
                .id(1)
                .name("FeeEarner1"))
            .addFeeEarnersItem(new ContactDetail()
                .id(2)
                .name("FeeEarner2")))
        .addOfficesItem(new OfficeDetail()
            .id(11)
            .name("Office 2")
            .addFeeEarnersItem(new ContactDetail()
                .id(1)
                .name("FeeEarner1"))
            .addFeeEarnersItem(new ContactDetail()
                .id(3)
                .name("FeeEarner3")));
  }
}
