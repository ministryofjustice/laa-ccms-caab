package uk.gov.laa.ccms.caab.service;

import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.laa.ccms.caab.bean.ClientFormDataAddressDetails;
import uk.gov.laa.ccms.caab.bean.ClientFormDataAddressSearch;
import uk.gov.laa.ccms.caab.client.OrdinanceSurveyApiClient;
import uk.gov.laa.ccms.caab.mapper.ClientAddressResultDisplayMapper;
import uk.gov.laa.ccms.caab.model.ClientAddressResultRowDisplay;
import uk.gov.laa.ccms.caab.model.ClientAddressResultsDisplay;
import uk.gov.laa.ccms.caab.model.os.OrdinanceSurveyResponse;

/**
 * Service class to handle Addresses.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AddressService {

  private final OrdinanceSurveyApiClient ordinanceSurveyApiClient;

  private final ClientAddressResultDisplayMapper clientAddressResultDisplayMapper;


  /**
   * Fetches addresses based on the given postcode by using the Ordinance Survey API.
   * Maps the API response to ClientAddressResultsDisplay format.
   *
   * @param postcode The postcode to search for.
   * @return ClientAddressResultsDisplay containing a list of addresses associated with the given
   *         postcode.Returns an empty ClientAddressResultsDisplay if no results are found.
   */
  public ClientAddressResultsDisplay getAddresses(String postcode) {
    OrdinanceSurveyResponse response = ordinanceSurveyApiClient.getAddresses(postcode).block();

    return response.getResults() != null
        ? clientAddressResultDisplayMapper.toClientAddressResultsDisplay(response)
        : new ClientAddressResultsDisplay();
  }

  /**
   * Filters a list of addresses based on the provided house name or number.
   *
   * @param houseNameNumber The house name or number to filter by.
   * @param results The initial list of addresses to filter from.
   * @return ClientAddressResultsDisplay containing a filtered list of addresses.
   *         Returns the original list if no addresses match the filter.
   */
  public ClientAddressResultsDisplay filterByHouseNumber(
      String houseNameNumber, ClientAddressResultsDisplay results) {

    List<ClientAddressResultRowDisplay> filteredAddressList =
        results.getContent().stream()
        .filter(address -> address.getHouseNameNumber()
            .equalsIgnoreCase(houseNameNumber))
        .collect(Collectors.toList());

    ClientAddressResultsDisplay filteredResults = new ClientAddressResultsDisplay();
    filteredResults.setContent(filteredAddressList);

    return filteredAddressList.isEmpty() ? results : filteredResults;
  }

  /**
   * Adds the address associated with the given UPRN (Unique Property Reference Number)
   * to the ClientDetails object.
   *
   * @param uprn The Unique Property Reference Number (UPRN) for the address.
   * @param results The list of addresses to search the UPRN from.
   * @param addressDetails The address details object to which the address will be added.
   * @return Updated ClientDetails object with the address information added.
   */
  public void addAddressToClientDetails(
      String uprn, ClientAddressResultsDisplay results, ClientFormDataAddressDetails addressDetails) {

    ClientAddressResultRowDisplay clientAddress = (results != null)
        ? results.getContent().stream()
        .filter(result -> uprn
            .equals(result.getUprn()))
        .findFirst()
        .orElse(null)
        : null;

    clientAddressResultDisplayMapper.updateClientFormDataAddressDetails(addressDetails, clientAddress);

    addressDetails.setAddressSearch(new ClientFormDataAddressSearch());
  }

}
