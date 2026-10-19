package uk.gov.laa.ccms.caab.bean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import org.junit.jupiter.api.Test;
import uk.gov.laa.ccms.caab.model.AddressResultRowDisplay;
import uk.gov.laa.ccms.caab.model.ResultsDisplay;

class AddressLookupFlowDataTest {

  @Test
  void beginSearchRetainsOriginalTermsAndClearsStaleCandidates() {
    AddressLookupFlowData<String> flow = new AddressLookupFlowData<>("case:123");
    flow.setSearchResults(results("old-uprn"));
    flow.setSelectedAddress(address("old-uprn"));

    flow.beginSearch("sa58ab", " 2 ");

    assertEquals("sa58ab", flow.getOriginalPostcode());
    assertEquals(" 2 ", flow.getOriginalBuildingSearch());
    assertEquals("SA5 8AB", flow.getNormalizedPostcode());
    assertNull(flow.getSearchResults());
    assertNull(flow.getSelectedAddress());
    assertEquals(AddressLookupFlowData.Step.SEARCH, flow.getStep());
  }

  @Test
  void selectingAndEnteringManualModeTrackDistinctSources() {
    AddressLookupFlowData<String> flow = new AddressLookupFlowData<>("case:123");
    flow.setSelectedAddress(address("123"));
    assertEquals(AddressLookupFlowData.Source.SEARCHED, flow.getSource());
    assertEquals(AddressLookupFlowData.Step.SELECTED_ADDRESS, flow.getStep());

    flow.enterManualEntry("blank");
    assertEquals(AddressLookupFlowData.Source.MANUAL, flow.getSource());
    assertEquals(AddressLookupFlowData.Step.MANUAL_ENTRY, flow.getStep());
    assertEquals("blank", flow.getAddressDetails());
    assertNull(flow.getSelectedAddress());
  }

  private ResultsDisplay<AddressResultRowDisplay> results(String uprn) {
    ResultsDisplay<AddressResultRowDisplay> results = new ResultsDisplay<>();
    results.setContent(List.of(address(uprn)));
    return results;
  }

  private AddressResultRowDisplay address(String uprn) {
    AddressResultRowDisplay address = new AddressResultRowDisplay();
    address.setUprn(uprn);
    return address;
  }
}
