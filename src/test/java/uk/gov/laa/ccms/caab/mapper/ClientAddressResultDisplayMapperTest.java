package uk.gov.laa.ccms.caab.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.laa.ccms.caab.bean.ClientFormDataAddressDetails;
import uk.gov.laa.ccms.caab.model.AddressResultRowDisplay;
import uk.gov.laa.ccms.caab.model.os.DeliveryPointAddress;
import uk.gov.laa.ccms.caab.model.os.OrdinanceSurveyResult;

@ExtendWith(SpringExtension.class)
public class ClientAddressResultDisplayMapperTest {

  private ClientAddressResultDisplayMapper mapper;

  @BeforeEach
  public void setUp() {
    mapper = Mappers.getMapper(ClientAddressResultDisplayMapper.class);
  }

  @Test
  public void testToAddressLine1_withSubBuildingNameOrOrganizationName() {
    DeliveryPointAddress address = new DeliveryPointAddress();
    address.setOrganisationName("Some Org");
    address.setSubBuildingName("Sub Building");

    String addressLine1 = mapper.toAddressLine1(address);

    assertEquals("Some Org,Sub Building", addressLine1);
  }

  @Test
  public void testToAddressLine1_withoutSubBuildingNameOrOrganizationName() {
    DeliveryPointAddress address = new DeliveryPointAddress();
    address.setBuildingNumber("10");
    address.setBuildingName("Some Building");
    address.setThoroughfareName("Some Street");

    String addressLine1 = mapper.toAddressLine1(address);

    assertEquals("10,Some Building,Some Street", addressLine1);
  }

  @Test
  public void testToAddressLine2_withSubBuildingNameOrOrganizationName() {
    DeliveryPointAddress address = new DeliveryPointAddress();
    address.setOrganisationName("Some Org");
    address.setSubBuildingName("Sub Building");
    address.setBuildingNumber("10");
    address.setBuildingName("Some Building");
    address.setThoroughfareName("Some Street");

    String addressLine2 = mapper.toAddressLine2(address);

    assertEquals("10,Some Building,Some Street", addressLine2);
  }

  @Test
  public void testToAddressLine2_withoutSubBuildingNameOrOrganizationName() {
    DeliveryPointAddress address = new DeliveryPointAddress();
    address.setBuildingNumber("10");
    address.setBuildingName("Some Building");

    String addressLine2 = mapper.toAddressLine2(address);

    assertNull(addressLine2);
  }

  @Test
  public void testToHouseNameNumber() {
    DeliveryPointAddress address = new DeliveryPointAddress();
    address.setBuildingNumber("10");
    address.setBuildingName("Some Building");

    String houseNameNumber = mapper.toHouseNameNumber(address);

    assertEquals("10,Some Building", houseNameNumber);
  }

  @Test
  public void testToClientAddressResultRowDisplay() {
    OrdinanceSurveyResult surveyResult = new OrdinanceSurveyResult();
    DeliveryPointAddress deliveryPointAddress = new DeliveryPointAddress();
    deliveryPointAddress.setUprn("123456789");
    deliveryPointAddress.setPostcode("AB12 3CD");
    deliveryPointAddress.setPostTown("London");
    deliveryPointAddress.setDependentLocality("Sometown");
    deliveryPointAddress.setBuildingNumber("10");
    deliveryPointAddress.setBuildingName("Some Building");

    surveyResult.setDeliveryPointAddress(deliveryPointAddress);

    AddressResultRowDisplay resultRowDisplay = mapper.toClientAddressResultRowDisplay(surveyResult);

    assertEquals("123456789", resultRowDisplay.getUprn());
    assertEquals("10,Some Building", resultRowDisplay.getAddressLine1());
    assertNull(resultRowDisplay.getAddressLine2());
    assertEquals("AB12 3CD", resultRowDisplay.getPostcode());
    assertEquals("London", resultRowDisplay.getCityTown());
    assertEquals("GBR", resultRowDisplay.getCountry());
  }

  @Test
  public void testUpdateClientDetails() {
    ClientFormDataAddressDetails addressDetails = new ClientFormDataAddressDetails();  // Assuming this is an empty object you want to populate
    AddressResultRowDisplay addressRowDisplay =
        buildAddressRowDisplay();

    mapper.updateClientFormDataAddressDetails(addressDetails, addressRowDisplay);
    
    assertEquals("Example Street", addressDetails.getAddressLine1());
    assertEquals("ExArea", addressDetails.getAddressLine2());
    assertEquals("123", addressDetails.getHouseNameNumber());
    assertEquals("EX1 2YZ", addressDetails.getPostcode());
    assertEquals("ExCity", addressDetails.getCityTown());
    assertEquals("ExCounty", addressDetails.getCounty());
    assertEquals("GBR", addressDetails.getCountry());
  }

  private static AddressResultRowDisplay buildAddressRowDisplay() {
    AddressResultRowDisplay addressRowDisplay = new AddressResultRowDisplay();

    addressRowDisplay.setFullAddress("Full Address, Example Street, ExCity");
    addressRowDisplay.setUprn("987654321");
    addressRowDisplay.setAddressLine1("Example Street");
    addressRowDisplay.setAddressLine2("ExArea");
    addressRowDisplay.setHouseNameNumber("123");
    addressRowDisplay.setPostcode("EX1 2YZ");
    addressRowDisplay.setCityTown("ExCity");
    addressRowDisplay.setCounty("ExCounty");
    addressRowDisplay.setCountry("GBR");
    return addressRowDisplay;
  }


}