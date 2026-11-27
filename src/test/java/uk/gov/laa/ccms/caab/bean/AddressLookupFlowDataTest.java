package uk.gov.laa.ccms.caab.bean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import uk.gov.laa.ccms.caab.model.AddressResultRowDisplay;
import uk.gov.laa.ccms.caab.model.ResultsDisplay;

class AddressLookupFlowDataTest {

  @Test
  void boundFlowsHaveUniqueIdentitiesEvenForTheSameOwner() {
    AddressFlowOwner owner = new AddressFlowOwner("registration:123", "client-address");
    AddressLookupFlowData<String> first = new AddressLookupFlowData<>(owner);
    AddressLookupFlowData<String> second = new AddressLookupFlowData<>(owner);

    assertEquals(owner, first.getOwner());
    assertEquals(owner.context(), first.getContext());
    assertEquals(first.getFlowId(), UUID.fromString(first.getFlowId()).toString());
    assertNotEquals(first.getFlowId(), second.getFlowId());
    assertEquals(0L, first.getRevision());
  }

  @Test
  void legacyCorrespondenceFlowsKeepTheirContextAndInitialState() {
    AddressLookupFlowData<String> flow = new AddressLookupFlowData<>("amendments:123");

    assertEquals("amendments:123", flow.getContext());
    assertNull(flow.getOwner());
    assertEquals(AddressLookupFlowData.Step.SEARCH, flow.getStep());
    assertEquals(AddressLookupFlowData.Source.UNKNOWN, flow.getSource());
    assertEquals(0L, flow.getRevision());
  }

  @Test
  void ownerEqualityIncludesBothContextAndPurpose() {
    AddressFlowOwner owner = new AddressFlowOwner("amendments:123", "correspondence-address");

    assertEquals(new AddressFlowOwner("amendments:123", "correspondence-address"), owner);
    assertNotEquals(new AddressFlowOwner("amendments:456", "correspondence-address"), owner);
    assertNotEquals(new AddressFlowOwner("amendments:123", "client-address"), owner);
  }

  @Test
  void ownerRejectsMissingOrBlankIdentityParts() {
    assertThrows(NullPointerException.class, () -> new AddressFlowOwner(null, "client-address"));
    assertThrows(NullPointerException.class, () -> new AddressFlowOwner("registration:123", null));
    assertThrows(IllegalArgumentException.class, () -> new AddressFlowOwner(" ", "client-address"));
    assertThrows(
        IllegalArgumentException.class, () -> new AddressFlowOwner("registration:123", ""));
    assertThrows(
        NullPointerException.class,
        () -> new AddressLookupFlowData<String>((AddressFlowOwner) null));
  }

  @Test
  void transitionsAdvanceRevisionWithoutChangingIdentity() {
    AddressFlowOwner owner = new AddressFlowOwner("registration:123", "client-address");
    AddressLookupFlowData<String> flow = new AddressLookupFlowData<>(owner);
    String flowId = flow.getFlowId();

    flow.beginSearch("SW1A 1AA", "10");
    assertEquals(1L, flow.getRevision());
    flow.setSearchResults(results("123"));
    assertEquals(2L, flow.getRevision());
    flow.setSelectedAddress(address("123"));
    assertEquals(3L, flow.getRevision());
    flow.confirmSelectedAddress();
    assertEquals(4L, flow.getRevision());
    flow.searchAgain();
    assertEquals(5L, flow.getRevision());
    flow.enterManualEntry("blank");
    assertEquals(6L, flow.getRevision());
    flow.markLookupUnavailable();
    assertEquals(7L, flow.getRevision());
    flow.showExistingAddress();
    assertEquals(8L, flow.getRevision());

    assertEquals(flowId, flow.getFlowId());
    assertEquals(owner, flow.getOwner());
  }

  @Test
  void stateSettersAdvanceRevision() {
    AddressLookupFlowData<String> flow = new AddressLookupFlowData<>("case:123");

    flow.setAddressDetails("address");
    assertEquals(1L, flow.getRevision());
    flow.setOriginalPostcode("sw1a1aa");
    assertEquals(2L, flow.getRevision());
    flow.setOriginalBuildingSearch("10");
    assertEquals(3L, flow.getRevision());
    flow.setNormalizedPostcode("SW1A 1AA");
    assertEquals(4L, flow.getRevision());
    flow.setStep(AddressLookupFlowData.Step.MANUAL_ENTRY);
    assertEquals(5L, flow.getRevision());
    flow.setSource(AddressLookupFlowData.Source.MANUAL);
    assertEquals(6L, flow.getRevision());
    flow.setSelectedAddress(null);
    assertEquals(7L, flow.getRevision());
  }

  @Test
  void invalidAndRepeatedConfirmationsDoNotAdvanceRevision() {
    AddressLookupFlowData<String> flow = new AddressLookupFlowData<>("case:123");

    flow.confirmSelectedAddress();
    assertEquals(0L, flow.getRevision());
    flow.setSelectedAddress(address("123"));
    flow.confirmSelectedAddress();
    flow.confirmSelectedAddress();
    assertEquals(2L, flow.getRevision());
    assertEquals(AddressLookupFlowData.Step.CONFIRMED_ADDRESS, flow.getStep());
  }

  @Test
  void identityAndRevisionCannotBeSetThroughBeanSetters() {
    List<String> forbiddenSetters = List.of("setOwner", "setFlowId", "setRevision", "setContext");
    assertFalse(
        Arrays.stream(AddressLookupFlowData.class.getMethods())
            .anyMatch(method -> forbiddenSetters.contains(method.getName())));
  }

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

  @Test
  void confirmingSelectionKeepsCandidatesAndSearchAgainRetainsTerms() {
    AddressLookupFlowData<String> flow = new AddressLookupFlowData<>("case:123");
    flow.beginSearch("SW1A 1AA", "10");
    flow.setSearchResults(results("123"));
    flow.setSelectedAddress(address("123"));

    flow.confirmSelectedAddress();
    assertEquals(AddressLookupFlowData.Step.CONFIRMED_ADDRESS, flow.getStep());
    assertEquals("123", flow.getSelectedAddress().getUprn());
    assertEquals(1, flow.getSearchResults().getContent().size());

    flow.searchAgain();
    assertEquals(AddressLookupFlowData.Step.SEARCH, flow.getStep());
    assertEquals("SW1A 1AA", flow.getOriginalPostcode());
    assertEquals("10", flow.getOriginalBuildingSearch());
    assertNull(flow.getSelectedAddress());
    assertNull(flow.getSearchResults());
    assertEquals(AddressLookupFlowData.Source.UNKNOWN, flow.getSource());
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
