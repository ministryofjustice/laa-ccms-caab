package uk.gov.laa.ccms.caab.bean;

import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Setter;
import lombok.ToString;
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
    CONFIRMED_ADDRESS,
    MANUAL_ENTRY,
    EXISTING_ADDRESS
  }

  public enum Source {
    UNKNOWN,
    SEARCHED,
    MANUAL,
    EXISTING
  }

  private final String context;
  @EqualsAndHashCode.Exclude @ToString.Exclude private final AddressFlowOwner owner;

  @EqualsAndHashCode.Exclude @ToString.Exclude
  private final String flowId = UUID.randomUUID().toString();

  /** Mutation version, not a lock. Callers must coordinate revision checks and writes. */
  @Setter(AccessLevel.NONE)
  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  private long revision;

  private T addressDetails;
  private ResultsDisplay<AddressResultRowDisplay> searchResults;
  private AddressResultRowDisplay selectedAddress;
  private String originalPostcode;
  private String originalBuildingSearch;
  private String normalizedPostcode;
  private Step step = Step.SEARCH;
  private Source source = Source.UNKNOWN;

  /** Legacy, unbound flow for consumers that have not adopted ownership validation yet. */
  public AddressLookupFlowData(final String context) {
    this.context = Objects.requireNonNull(context);
    this.owner = null;
  }

  /** Creates a new flow bound to a server-resolved owner and address purpose. */
  public AddressLookupFlowData(final AddressFlowOwner owner) {
    this.owner = Objects.requireNonNull(owner);
    this.context = owner.context();
  }

  public void setAddressDetails(final T addressDetails) {
    this.addressDetails = addressDetails;
    revision++;
  }

  public void setOriginalPostcode(final String originalPostcode) {
    this.originalPostcode = originalPostcode;
    revision++;
  }

  public void setOriginalBuildingSearch(final String originalBuildingSearch) {
    this.originalBuildingSearch = originalBuildingSearch;
    revision++;
  }

  public void setNormalizedPostcode(final String normalizedPostcode) {
    this.normalizedPostcode = normalizedPostcode;
    revision++;
  }

  public void setStep(final Step step) {
    this.step = step;
    revision++;
  }

  public void setSource(final Source source) {
    this.source = source;
    revision++;
  }

  /** Starts a fresh query while retaining the exact terms entered by the user. */
  public void beginSearch(final String postcode, final String buildingSearch) {
    originalPostcode = postcode;
    originalBuildingSearch = buildingSearch;
    normalizedPostcode = UkPostcode.normalize(postcode);
    searchResults = null;
    selectedAddress = null;
    source = Source.UNKNOWN;
    step = Step.SEARCH;
    revision++;
  }

  public void setSearchResults(final ResultsDisplay<AddressResultRowDisplay> searchResults) {
    this.searchResults = searchResults;
    step =
        searchResults != null
                && searchResults.getContent() != null
                && !searchResults.getContent().isEmpty()
            ? Step.RESULTS
            : Step.NO_RESULTS;
    revision++;
  }

  public void setSelectedAddress(final AddressResultRowDisplay selectedAddress) {
    this.selectedAddress = selectedAddress;
    if (selectedAddress != null) {
      source = Source.SEARCHED;
      step = Step.SELECTED_ADDRESS;
    }
    revision++;
  }

  public void confirmSelectedAddress() {
    if (selectedAddress != null && step == Step.SELECTED_ADDRESS) {
      step = Step.CONFIRMED_ADDRESS;
      revision++;
    }
  }

  /** Returns to the search step while retaining the original terms. */
  public void searchAgain() {
    searchResults = null;
    selectedAddress = null;
    source = Source.UNKNOWN;
    step = Step.SEARCH;
    revision++;
  }

  /** Starts a blank manual-entry branch without carrying lookup candidates into it. */
  public void enterManualEntry(final T blankAddressDetails) {
    addressDetails = blankAddressDetails;
    searchResults = null;
    selectedAddress = null;
    source = Source.MANUAL;
    step = Step.MANUAL_ENTRY;
    revision++;
  }

  public void markLookupUnavailable() {
    searchResults = null;
    selectedAddress = null;
    step = Step.LOOKUP_UNAVAILABLE;
    revision++;
  }

  public void showExistingAddress() {
    searchResults = null;
    selectedAddress = null;
    source = Source.EXISTING;
    step = Step.EXISTING_ADDRESS;
    revision++;
  }
}
