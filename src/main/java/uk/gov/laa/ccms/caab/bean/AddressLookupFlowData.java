package uk.gov.laa.ccms.caab.bean;

import lombok.Data;
import uk.gov.laa.ccms.caab.model.AddressResultRowDisplay;
import uk.gov.laa.ccms.caab.model.ResultsDisplay;
import uk.gov.laa.ccms.caab.util.UkPostcode;

/** Session state for one address lookup flow. */
@Data
public class AddressLookupFlowData<T> {

  public enum Step {
    SEARCH,
    RESULTS,
    NO_RESULTS,
    LOOKUP_UNAVAILABLE,
    SELECTED_ADDRESS,
    MANUAL_ENTRY,
    EXISTING_ADDRESS
  }

  public enum Source {
    UNKNOWN,
    SEARCHED,
    MANUAL,
    EXISTING
  }

  private String context;
  private T addressDetails;
  private ResultsDisplay<AddressResultRowDisplay> searchResults;
  private AddressResultRowDisplay selectedAddress;
  private String originalPostcode;
  private String originalBuildingSearch;
  private String normalizedPostcode;
  private Step step = Step.SEARCH;
  private Source source = Source.UNKNOWN;

  public AddressLookupFlowData(final String context) {
    this.context = context;
  }

  /** Starts a fresh query while retaining the exact terms entered by the user. */
  public void beginSearch(final String postcode, final String buildingSearch) {
    originalPostcode = postcode;
    originalBuildingSearch = buildingSearch;
    normalizedPostcode = UkPostcode.normalize(postcode);
    searchResults = null;
    selectedAddress = null;
    step = Step.SEARCH;
  }

  public void setSearchResults(final ResultsDisplay<AddressResultRowDisplay> searchResults) {
    this.searchResults = searchResults;
    step =
        searchResults != null
                && searchResults.getContent() != null
                && !searchResults.getContent().isEmpty()
            ? Step.RESULTS
            : Step.NO_RESULTS;
  }

  public void setSelectedAddress(final AddressResultRowDisplay selectedAddress) {
    this.selectedAddress = selectedAddress;
    if (selectedAddress != null) {
      source = Source.SEARCHED;
      step = Step.SELECTED_ADDRESS;
    }
  }

  /** Starts a blank manual-entry branch without carrying lookup candidates into it. */
  public void enterManualEntry(final T blankAddressDetails) {
    addressDetails = blankAddressDetails;
    searchResults = null;
    selectedAddress = null;
    source = Source.MANUAL;
    step = Step.MANUAL_ENTRY;
  }

  public void markLookupUnavailable() {
    searchResults = null;
    selectedAddress = null;
    step = Step.LOOKUP_UNAVAILABLE;
  }

  public void showExistingAddress() {
    searchResults = null;
    selectedAddress = null;
    source = Source.EXISTING;
    step = Step.EXISTING_ADDRESS;
  }
}
