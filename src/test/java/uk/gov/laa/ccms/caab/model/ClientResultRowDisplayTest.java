package uk.gov.laa.ccms.caab.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ClientResultRowDisplayTest {

  private ClientResultRowDisplay clientResultRowDisplay;

  @BeforeEach
  void setUp() {
    clientResultRowDisplay = new ClientResultRowDisplay();
  }

  @Test
  void getPostCodeDistrictWithSpace() {
    clientResultRowDisplay.setPostalCode("SW1A 1AA");
    String postCodeDistrict = clientResultRowDisplay.getPostCodeDistrict();
    assertEquals("SW1A", postCodeDistrict);
  }

  @Test
  void getPostCodeDistrictWithoutSpace() {
    clientResultRowDisplay.setPostalCode("SW1A1AA");
    String postCodeDistrict = clientResultRowDisplay.getPostCodeDistrict();
    assertEquals("SW1A1AA", postCodeDistrict);
  }

  @Test
  void getPostCodeDistrictWithShortCode() {
    clientResultRowDisplay.setPostalCode("SW");
    String postCodeDistrict = clientResultRowDisplay.getPostCodeDistrict();
    assertEquals("SW", postCodeDistrict);
  }

  @Test
  void getPostCodeDistrictWithNullPostalCode() {
    clientResultRowDisplay.setPostalCode(null);
    String postCodeDistrict = clientResultRowDisplay.getPostCodeDistrict();
    assertNull(postCodeDistrict);
  }

  @Test
  void getPostCodeDistrictWithEmptyPostalCode() {
    clientResultRowDisplay.setPostalCode("");
    String postCodeDistrict = clientResultRowDisplay.getPostCodeDistrict();
    assertEquals("", postCodeDistrict);
  }

  @Test
  void getPostCodeDistrictWithOnlySpaces() {
    clientResultRowDisplay.setPostalCode("   ");
    String postCodeDistrict = clientResultRowDisplay.getPostCodeDistrict();
    assertEquals("   ", postCodeDistrict);
  }

}