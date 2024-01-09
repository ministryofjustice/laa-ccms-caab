package uk.gov.laa.ccms.caab.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.bean.ClientFormDataAddressDetails;
import uk.gov.laa.ccms.caab.client.OrdinanceSurveyApiClient;
import uk.gov.laa.ccms.caab.mapper.ClientAddressResultDisplayMapper;
import uk.gov.laa.ccms.caab.model.AddressResultRowDisplay;
import uk.gov.laa.ccms.caab.model.AddressResultsDisplay;
import uk.gov.laa.ccms.caab.model.os.DeliveryPointAddress;
import uk.gov.laa.ccms.caab.model.os.OrdinanceSurveyResponse;
import uk.gov.laa.ccms.caab.model.os.OrdinanceSurveyResult;

@ExtendWith(MockitoExtension.class)
public class AddressServiceTest {

  @Mock
  private OrdinanceSurveyApiClient ordinanceSurveyApiClient;

  @Mock
  private ClientAddressResultDisplayMapper clientAddressResultDisplayMapper;

  @InjectMocks
  private AddressService addressService;

  private final int MAX_RESULTS = 10;


  @Test
  void getAddresses_ReturnsAddresses_empty() {
    String postcode = "SW1A1AA";

    OrdinanceSurveyResponse mockResponse = new OrdinanceSurveyResponse();
    Mono<OrdinanceSurveyResponse> mockResponseMono = Mono.just(mockResponse);

    AddressResultsDisplay mockDisplay = new AddressResultsDisplay();

    when(ordinanceSurveyApiClient.getAddresses(postcode)).thenReturn(mockResponseMono);

    AddressResultsDisplay result = addressService.getAddresses(postcode);

    assertNotNull(result);
    assertNull(result.getContent());
  }

  @Test
  void getAddresses_ReturnsAddresses() {
    String postcode = "SW1A1AA";

    OrdinanceSurveyResponse mockResponse = buildOrdinanceSurveyResponse();
    Mono<OrdinanceSurveyResponse> mockResponseMono = Mono.just(mockResponse);

    AddressResultsDisplay mockDisplay = buildClientAddressResultsDisplay();

    when(ordinanceSurveyApiClient.getAddresses(postcode)).thenReturn(mockResponseMono);
    when(clientAddressResultDisplayMapper.toClientAddressResultsDisplay(mockResponse)).thenReturn(mockDisplay);

    AddressResultsDisplay result = addressService.getAddresses(postcode);

    assertNotNull(result);
    assertEquals(MAX_RESULTS, result.getContent().size());
  }

  @ParameterizedTest
  @CsvSource({"0","1","2","3","4","5","6","7","8","9"})
  void filterByHouseNumber_ReturnsFilteredAddresses(String houseNameNumber) {

    AddressResultRowDisplay row = new AddressResultRowDisplay();
    row.setFullAddress("TEST, ADDRESS, DATA");
    row.setHouseNameNumber(houseNameNumber);

    AddressResultsDisplay initialResults = buildClientAddressResultsDisplay();

    AddressResultsDisplay
        filteredResults = addressService.filterByHouseNumber(houseNameNumber, initialResults);

    assertNotNull(filteredResults);
    assertEquals(Collections.singletonList(row), filteredResults.getContent());
  }

  @Test
  void filterByHouseNumber_ReturnsOriginalResults_WhenNoAddressesMatchFilter() {
    String houseNameNumber = "nonexistent";
    AddressResultsDisplay initialResults = buildClientAddressResultsDisplay();

    AddressResultsDisplay
        filteredResults = addressService.filterByHouseNumber(houseNameNumber, initialResults);

    assertNotNull(filteredResults);
    assertEquals(initialResults.getContent(), filteredResults.getContent());
  }


  @Test
  void addAddressToClientDetails_UpdatesClientDetails() {
    String uprn = "12345";
    AddressResultsDisplay results = buildClientAddressResultsDisplay();

    AddressResultRowDisplay targetRow = new AddressResultRowDisplay();
    targetRow.setFullAddress("TARGET, ADDRESS, DATA");
    targetRow.setHouseNameNumber("100");
    targetRow.setUprn(uprn);
    results.getContent().add(targetRow);

    ClientFormDataAddressDetails addressDetails = buildAddressDetails(); // Use the helper method

    doAnswer(invocation -> {
      ClientFormDataAddressDetails ad = invocation.getArgument(0);
      AddressResultRowDisplay row = invocation.getArgument(1);
      ad.setAddressLine1(row.getFullAddress());
      ad.setHouseNameNumber(row.getHouseNameNumber());
      return null;
    }).when(clientAddressResultDisplayMapper).updateClientFormDataAddressDetails(
        any(ClientFormDataAddressDetails.class),
        any(AddressResultRowDisplay.class));

    addressService.addAddressToClientDetails(uprn, results, addressDetails);

    assertNotNull(addressDetails);
    assertEquals("TARGET, ADDRESS, DATA", addressDetails.getAddressLine1());
    assertEquals("100", addressDetails.getHouseNameNumber());
    assertEquals("GBR", addressDetails.getCountry());
    assertEquals("SW1A1AA", addressDetails.getPostcode());
    assertEquals("Westminster", addressDetails.getAddressLine2());
    assertEquals("London", addressDetails.getCityTown());
    assertEquals("Greater London", addressDetails.getCounty());
    assertFalse(addressDetails.getNoFixedAbode());
  }

  @Test
  void addAddressToClientDetails_DoesNotUpdateClientDetails_WhenResultsAreNull() {
    String uprn = "12345";
    ClientFormDataAddressDetails addressDetails = buildAddressDetails();

    addressService.addAddressToClientDetails(uprn, null, addressDetails);

    assertNotNull(addressDetails);
  }

  private OrdinanceSurveyResponse buildOrdinanceSurveyResponse(){
    OrdinanceSurveyResponse response = new OrdinanceSurveyResponse();
    response.setResults(buildOrdinanceSurveyResults());
    return response;
  }

  private List<OrdinanceSurveyResult> buildOrdinanceSurveyResults(){
    List<OrdinanceSurveyResult> results = new ArrayList<>();

    for (int i = 0; i < MAX_RESULTS; i++){
      OrdinanceSurveyResult result = new OrdinanceSurveyResult();
      result.setDeliveryPointAddress(buildDeliveryPointAddress(String.valueOf(i)));
      results.add(result);
    }

    return results;
  }

  private DeliveryPointAddress buildDeliveryPointAddress(String number){
    DeliveryPointAddress dpa = new DeliveryPointAddress();
    dpa.setAddress("TEST, ADDRESS, DATA");
    dpa.setBuildingNumber(number);
    return dpa;
  }

  private AddressResultsDisplay buildClientAddressResultsDisplay(){
    AddressResultsDisplay addressResultsDisplay = new AddressResultsDisplay();

    List<AddressResultRowDisplay> content = new ArrayList<>();
    for (int i = 0; i < MAX_RESULTS; i++){
      AddressResultRowDisplay result = new AddressResultRowDisplay();
      result.setFullAddress("TEST, ADDRESS, DATA");
      result.setHouseNameNumber(String.valueOf(i));
      content.add(result);
    }

    addressResultsDisplay.setContent(content);

    return addressResultsDisplay;
  }

  private ClientFormDataAddressDetails buildAddressDetails() {
    ClientFormDataAddressDetails addressDetails = new ClientFormDataAddressDetails();
    addressDetails.setNoFixedAbode(false);
    addressDetails.setCountry("GBR");
    addressDetails.setHouseNameNumber("123");
    addressDetails.setPostcode("SW1A1AA");
    addressDetails.setAddressLine1("10 Downing Street");
    addressDetails.setAddressLine2("Westminster");
    addressDetails.setCityTown("London");
    addressDetails.setCounty("Greater London");
    return addressDetails;
  }
}