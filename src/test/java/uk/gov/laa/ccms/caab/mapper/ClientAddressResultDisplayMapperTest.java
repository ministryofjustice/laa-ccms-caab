package uk.gov.laa.ccms.caab.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.laa.ccms.caab.bean.ClientDetails;
import uk.gov.laa.ccms.caab.model.ClientAddressResultRowDisplay;
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

    ClientAddressResultRowDisplay resultRowDisplay = mapper.toClientAddressResultRowDisplay(surveyResult);

    assertEquals("123456789", resultRowDisplay.getUprn());
    assertEquals("10,Some Building", resultRowDisplay.getAddressLine1());
    assertNull(resultRowDisplay.getAddressLine2());
    assertEquals("AB12 3CD", resultRowDisplay.getPostcode());
    assertEquals("London", resultRowDisplay.getCityTown());
    assertEquals("GBR", resultRowDisplay.getCountry());
  }

  @Test
  public void testUpdateClientDetails() {
    ClientDetails clientDetails = new ClientDetails();  // Assuming this is an empty object you want to populate
    ClientAddressResultRowDisplay addressRowDisplay = new ClientAddressResultRowDisplay();

    addressRowDisplay.setFullAddress("Full Address, Example Street, ExCity");
    addressRowDisplay.setUprn("987654321");
    addressRowDisplay.setAddressLine1("Example Street");
    addressRowDisplay.setAddressLine2("ExArea");
    addressRowDisplay.setHouseNameNumber("123");
    addressRowDisplay.setPostcode("EX1 2YZ");
    addressRowDisplay.setCityTown("ExCity");
    addressRowDisplay.setCounty("ExCounty");
    addressRowDisplay.setCountry("GBR");

    mapper.updateClientDetails(clientDetails, addressRowDisplay);

    assertEquals("987654321", clientDetails.getUprn());
    assertEquals("Example Street", clientDetails.getAddressLine1());
    assertEquals("ExArea", clientDetails.getAddressLine2());
    assertEquals("123", clientDetails.getHouseNameNumber());
    assertEquals("EX1 2YZ", clientDetails.getPostcode());
    assertEquals("ExCity", clientDetails.getCityTown());
    assertEquals("ExCounty", clientDetails.getCounty());
    assertEquals("GBR", clientDetails.getCountry());
  }


}