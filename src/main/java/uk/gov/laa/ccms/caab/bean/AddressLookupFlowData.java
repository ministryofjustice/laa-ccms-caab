package uk.gov.laa.ccms.caab.bean;

import lombok.Data;
import uk.gov.laa.ccms.caab.model.AddressResultRowDisplay;
import uk.gov.laa.ccms.caab.model.ResultsDisplay;

/** Session state for one address lookup flow. */
@Data
public class AddressLookupFlowData<T> {

  private String context;
  private T addressDetails;
  private ResultsDisplay<AddressResultRowDisplay> searchResults;
  private AddressResultRowDisplay selectedAddress;

  public AddressLookupFlowData(final String context) {
    this.context = context;
  }
}
