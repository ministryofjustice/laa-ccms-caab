package uk.gov.laa.ccms.caab.mapper;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.laa.ccms.caab.bean.AddressFormData;
import uk.gov.laa.ccms.caab.model.AddressDetail;
import uk.gov.laa.ccms.caab.model.AddressResultRowDisplay;

@ExtendWith(SpringExtension.class)
class AddressFormDataMapperTest {

  private AddressFormDataMapper mapper = new AddressFormDataMapperImpl();

  @BeforeEach
  void setUp() {
    mapper = new AddressFormDataMapperImpl();
  }

  @Test
  void testToAddressFormDataNotNull() {
    final AddressDetail address = new AddressDetail();
    address.setHouseNameOrNumber("123");
    address.setCity("CityName");
    address.setPostcode("Postcode");
    address.setAddressLine1("Line1");
    address.setAddressLine2("Line2");
    address.setCounty("County");
    address.setCountry("Country");
    address.setCareOf("CareOf");
    address.setPreferredAddress("Yes");

    final AddressFormData result = mapper.toAddressFormData(address);

    assertEquals("123", result.getHouseNameNumber());
    assertEquals("CityName", result.getCityTown());
    assertEquals("Postcode", result.getPostcode());
    assertEquals("Line1", result.getAddressLine1());
    assertEquals("Line2", result.getAddressLine2());
    assertEquals("County", result.getCounty());
    assertEquals("Country", result.getCountry());
    assertEquals("CareOf", result.getCareOf());
    assertEquals("Yes", result.getPreferredAddress());
  }

  @Test
  void testToAddressFormDataWithNull() {
    final AddressFormData result = mapper.toAddressFormData(null);
    assertNull(result);
  }

  @Test
  void testToAddressNotNull() {
    final AddressFormData addressFormData = new AddressFormData();
    addressFormData.setHouseNameNumber("123");
    addressFormData.setCityTown("CityName");
    addressFormData.setPostcode("Postcode");
    addressFormData.setAddressLine1("Line1");
    addressFormData.setAddressLine2("Line2");
    addressFormData.setCounty("County");
    addressFormData.setCountry("Country");
    addressFormData.setCareOf("CareOf");
    addressFormData.setPreferredAddress("Yes");

    final AddressDetail result = mapper.toAddress(addressFormData);

    assertEquals("123", result.getHouseNameOrNumber());
    assertEquals("CityName", result.getCity());
    assertEquals("Postcode", result.getPostcode());
    assertEquals("Line1", result.getAddressLine1());
    assertEquals("Line2", result.getAddressLine2());
    assertEquals("County", result.getCounty());
    assertEquals("Country", result.getCountry());
    assertEquals("CareOf", result.getCareOf());
    assertEquals("Yes", result.getPreferredAddress());
  }

  @Test
  void testToAddressWithNull() {
    final AddressDetail result = mapper.toAddress(null);
    assertNull(result);
  }

  @Test
  void testUpdateAddressFormDataNotNull() {
    final AddressFormData addressDetails = new AddressFormData();
    final AddressResultRowDisplay addressResultRowDisplay = new AddressResultRowDisplay();
    addressResultRowDisplay.setCountry("Country");
    addressResultRowDisplay.setHouseNameNumber("123");
    addressResultRowDisplay.setPostcode("Postcode");
    addressResultRowDisplay.setAddressLine1("Line1");
    addressResultRowDisplay.setAddressLine2("Line2");
    addressResultRowDisplay.setCityTown("CityName");
    addressResultRowDisplay.setCounty("County");

    mapper.updateAddressFormData(addressDetails, addressResultRowDisplay);

    assertEquals("Country", addressDetails.getCountry());
    assertEquals("123", addressDetails.getHouseNameNumber());
    assertEquals("Postcode", addressDetails.getPostcode());
    assertEquals("Line1", addressDetails.getAddressLine1());
    assertEquals("Line2", addressDetails.getAddressLine2());
    assertEquals("CityName", addressDetails.getCityTown());
    assertEquals("County", addressDetails.getCounty());
  }

  @Test
  void testUpdateAddressFormDataWithNull() {
    final AddressFormData addressDetails = new AddressFormData();
    mapper.updateAddressFormData(addressDetails, null);

    assertNull(addressDetails.getCountry());
    assertNull(addressDetails.getHouseNameNumber());
    assertNull(addressDetails.getPostcode());
    assertNull(addressDetails.getAddressLine1());
    assertNull(addressDetails.getAddressLine2());
    assertNull(addressDetails.getCityTown());
    assertNull(addressDetails.getCounty());
  }

}