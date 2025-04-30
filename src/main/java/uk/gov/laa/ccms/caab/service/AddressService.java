package uk.gov.laa.ccms.caab.service;

import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.laa.ccms.caab.bean.AddressFormData;
import uk.gov.laa.ccms.caab.bean.AddressSearchFormData;
import uk.gov.laa.ccms.caab.bean.ClientFormDataAddressDetails;
import uk.gov.laa.ccms.caab.client.OrdinanceSurveyApiClient;
import uk.gov.laa.ccms.caab.mapper.AddressFormDataMapper;
import uk.gov.laa.ccms.caab.mapper.ClientAddressResultDisplayMapper;
import uk.gov.laa.ccms.caab.model.AddressResultRowDisplay;
import uk.gov.laa.ccms.caab.model.ResultsDisplay;
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

  private final AddressFormDataMapper addressFormDataMapper;


  /**
   * Fetches addresses based on the given postcode by using the Ordinance Survey API.
   * Maps the API response to ClientAddressResultsDisplay format.
   *
   * @param postcode The postcode to search for.
   * @return ClientAddressResultsDisplay containing a list of addresses associated with the given
   *         postcode.Returns an empty ClientAddressResultsDisplay if no results are found.
   */
  public ResultsDisplay<AddressResultRowDisplay> getAddresses(final String postcode) {
    final OrdinanceSurveyResponse response =
        ordinanceSurveyApiClient.getAddresses(postcode).block();

    return response.getResults() != null
        ? clientAddressResultDisplayMapper.toClientAddressResultsDisplay(response)
        : new ResultsDisplay<AddressResultRowDisplay>();
  }

  /**
   * Filters a list of addresses based on the provided house name or number.
   *
   * @param houseNameNumber The house name or number to filter by.
   * @param results The initial list of addresses to filter from.
   * @return ClientAddressResultsDisplay containing a filtered list of addresses.
   *         Returns the original list if no addresses match the filter.
   */
  public ResultsDisplay<AddressResultRowDisplay> filterByHouseNumber(
      final String houseNameNumber, final ResultsDisplay<AddressResultRowDisplay> results) {

    final List<AddressResultRowDisplay> filteredAddressList =
        results.getContent().stream()
        .filter(address -> address.getHouseNameNumber()
            .equalsIgnoreCase(houseNameNumber))
        .collect(Collectors.toList());

    final ResultsDisplay<AddressResultRowDisplay> filteredResults =
        new ResultsDisplay<>();
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
   */
  public void addAddressToClientDetails(
      final String uprn,
      final ResultsDisplay<AddressResultRowDisplay> results,
      final ClientFormDataAddressDetails addressDetails) {

    final AddressResultRowDisplay clientAddress = results != null
        ? results.getContent().stream()
        .filter(result -> uprn
            .equals(result.getUprn()))
        .findFirst()
        .orElse(null)
        : null;

    clientAddressResultDisplayMapper.updateClientFormDataAddressDetails(
        addressDetails, clientAddress);

    addressDetails.setAddressSearch(new AddressSearchFormData());
  }

  /**
   * Adds the address associated with the given UPRN (Unique Property Reference Number)
   * to the addressFormData Object.
   *
   * @param uprn The Unique Property Reference Number (UPRN) for the address.
   * @param results The list of addresses to search the UPRN from.
   */
  public void filterAndUpdateAddressFormData(
      final String uprn,
      final ResultsDisplay<AddressResultRowDisplay> results,
      final AddressFormData addressFormData) {

    final AddressResultRowDisplay addressResultRowDisplay = results != null
        ? results.getContent().stream()
        .filter(result -> uprn
            .equals(result.getUprn()))
        .findFirst()
        .orElse(null)
        : null;

    addressFormDataMapper.updateAddressFormData(addressFormData, addressResultRowDisplay);
  }


}
