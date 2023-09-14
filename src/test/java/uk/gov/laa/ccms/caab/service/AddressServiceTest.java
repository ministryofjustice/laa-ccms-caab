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
import uk.gov.laa.ccms.caab.bean.ClientDetails;
import uk.gov.laa.ccms.caab.client.OrdinanceSurveyApiClient;
import uk.gov.laa.ccms.caab.mapper.ClientAddressResultDisplayMapper;
import uk.gov.laa.ccms.caab.model.ClientAddressResultRowDisplay;
import uk.gov.laa.ccms.caab.model.ClientAddressResultsDisplay;
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

    ClientAddressResultsDisplay mockDisplay = new ClientAddressResultsDisplay();

    when(ordinanceSurveyApiClient.getAddresses(postcode)).thenReturn(mockResponseMono);

    ClientAddressResultsDisplay result = addressService.getAddresses(postcode);

    assertNotNull(result);
    assertNull(result.getContent());
  }

  @Test
  void getAddresses_ReturnsAddresses() {
    String postcode = "SW1A1AA";

    OrdinanceSurveyResponse mockResponse = buildOrdinanceSurveyResponse();
    Mono<OrdinanceSurveyResponse> mockResponseMono = Mono.just(mockResponse);

    ClientAddressResultsDisplay mockDisplay = buildClientAddressResultsDisplay();

    when(ordinanceSurveyApiClient.getAddresses(postcode)).thenReturn(mockResponseMono);
    when(clientAddressResultDisplayMapper.toClientAddressResultsDisplay(mockResponse)).thenReturn(mockDisplay);

    ClientAddressResultsDisplay result = addressService.getAddresses(postcode);

    assertNotNull(result);
    assertEquals(MAX_RESULTS, result.getContent().size());
  }

  @ParameterizedTest
  @CsvSource({"0","1","2","3","4","5","6","7","8","9"})
  void filterByHouseNumber_ReturnsFilteredAddresses(String houseNameNumber) {

    ClientAddressResultRowDisplay row = new ClientAddressResultRowDisplay();
    row.setFullAddress("TEST, ADDRESS, DATA");
    row.setHouseNameNumber(houseNameNumber);

    ClientAddressResultsDisplay initialResults = buildClientAddressResultsDisplay();

    ClientAddressResultsDisplay filteredResults = addressService.filterByHouseNumber(houseNameNumber, initialResults);

    assertNotNull(filteredResults);
    assertEquals(Collections.singletonList(row), filteredResults.getContent());
  }

  @Test
  void filterByHouseNumber_ReturnsOriginalResults_WhenNoAddressesMatchFilter() {
    String houseNameNumber = "nonexistent";
    ClientAddressResultsDisplay initialResults = buildClientAddressResultsDisplay();

    ClientAddressResultsDisplay filteredResults = addressService.filterByHouseNumber(houseNameNumber, initialResults);

    assertNotNull(filteredResults);
    assertEquals(initialResults.getContent(), filteredResults.getContent());
  }


  @Test
  void addAddressToClientDetails_UpdatesClientDetails() {
    String uprn = "12345";
    ClientAddressResultsDisplay results = buildClientAddressResultsDisplay();

    ClientAddressResultRowDisplay targetRow = new ClientAddressResultRowDisplay();
    targetRow.setFullAddress("TARGET, ADDRESS, DATA");
    targetRow.setHouseNameNumber("100");
    targetRow.setUprn(uprn);
    results.getContent().add(targetRow);

    ClientDetails clientDetails = buildClientDetails(); // Use the helper method

    doAnswer(invocation -> {
      ClientDetails cd = invocation.getArgument(0);
      ClientAddressResultRowDisplay row = invocation.getArgument(1);
      cd.setAddressLine1(row.getFullAddress());
      cd.setHouseNameNumber(row.getHouseNameNumber());
      return null;
    }).when(clientAddressResultDisplayMapper).updateClientDetails(any(ClientDetails.class), any(ClientAddressResultRowDisplay.class));

    ClientDetails updatedClientDetails = addressService.addAddressToClientDetails(uprn, results, clientDetails);

    assertNotNull(updatedClientDetails);
    assertEquals("TARGET, ADDRESS, DATA", updatedClientDetails.getAddressLine1());
    assertEquals("100", updatedClientDetails.getHouseNameNumber());
    assertEquals("GBR", updatedClientDetails.getCountry());
    assertEquals("SW1A1AA", updatedClientDetails.getPostcode());
    assertEquals("Westminster", updatedClientDetails.getAddressLine2());
    assertEquals("London", updatedClientDetails.getCityTown());
    assertEquals("Greater London", updatedClientDetails.getCounty());
    assertFalse(updatedClientDetails.getNoFixedAbode());
  }

  @Test
  void addAddressToClientDetails_DoesNotUpdateClientDetails_WhenResultsAreNull() {
    String uprn = "12345";
    ClientDetails clientDetails = buildClientDetails();

    ClientDetails updatedClientDetails = addressService.addAddressToClientDetails(uprn, null, clientDetails);

    assertNotNull(updatedClientDetails);
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

  private ClientAddressResultsDisplay buildClientAddressResultsDisplay(){
    ClientAddressResultsDisplay clientAddressResultsDisplay = new ClientAddressResultsDisplay();

    List<ClientAddressResultRowDisplay> content = new ArrayList<>();
    for (int i = 0; i < MAX_RESULTS; i++){
      ClientAddressResultRowDisplay result = new ClientAddressResultRowDisplay();
      result.setFullAddress("TEST, ADDRESS, DATA");
      result.setHouseNameNumber(String.valueOf(i));
      content.add(result);
    }

    clientAddressResultsDisplay.setContent(content);

    return clientAddressResultsDisplay;
  }

  private ClientDetails buildClientDetails() {
    ClientDetails clientDetails = new ClientDetails();
    clientDetails.setNoFixedAbode(false);
    clientDetails.setCountry("GBR");
    clientDetails.setHouseNameNumber("123");
    clientDetails.setPostcode("SW1A1AA");
    clientDetails.setAddressLine1("10 Downing Street");
    clientDetails.setAddressLine2("Westminster");
    clientDetails.setCityTown("London");
    clientDetails.setCounty("Greater London");
    return clientDetails;
  }
}