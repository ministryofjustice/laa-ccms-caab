package uk.gov.laa.ccms.caab.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static uk.gov.laa.ccms.caab.util.SoaModelUtils.buildOrganisationDetail;

import org.junit.jupiter.api.Test;
import uk.gov.laa.ccms.caab.bean.OpponentFormData;
import uk.gov.laa.ccms.caab.model.Opponent;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;
import uk.gov.laa.ccms.soa.gateway.model.OrganisationDetail;

public class OpponentMapperTest {
  OpponentMapper opponentMapper = new OpponentMapperImpl();

  @Test
  void testToOpponentFormData() {
    OrganisationDetail organisationDetail = buildOrganisationDetail("");
    CommonLookupValueDetail orgTypeLookup = new CommonLookupValueDetail()
        .code("thecode")
        .description("org type");

    OpponentFormData result =
        opponentMapper.toOpponentFormData(organisationDetail, orgTypeLookup);

    assertNotNull(result);
    assertEquals("Organisation", result.getType());
    assertEquals(organisationDetail.getAddress().getAddressLine1(), result.getAddressLine1());
    assertEquals(organisationDetail.getAddress().getAddressLine2(), result.getAddressLine2());
    assertEquals(organisationDetail.getAddress().getCity(), result.getCity());
    assertEquals(organisationDetail.getAddress().getCountry(), result.getCountry());
    assertEquals(organisationDetail.getAddress().getPostalCode(), result.getPostcode());
    assertEquals(organisationDetail.getContactDetails().getEmailAddress(), result.getEmailAddress());
    assertEquals(organisationDetail.getContactDetails().getFax(), result.getFaxNumber());
    assertEquals(organisationDetail.getContactDetails().getMobileNumber(), result.getTelephoneMobile());
    assertEquals(organisationDetail.getContactDetails().getTelephoneHome(), result.getTelephoneHome());
    assertEquals(organisationDetail.getContactDetails().getTelephoneWork(), result.getTelephoneWork());
    assertEquals(organisationDetail.getContactName(), result.getContactNameRole());
    assertEquals(organisationDetail.getName(), result.getOrganisationName());
    assertEquals(organisationDetail.getType(), result.getOrganisationType());
    assertEquals(orgTypeLookup.getDescription(), result.getOrganisationTypeDisplayValue());
    assertEquals(organisationDetail.getOtherInformation(), result.getOtherInformation());
    assertEquals(organisationDetail.getPartyId(), result.getPartyId());
  }

  @Test
  void testToOpponent() {
    OpponentFormData opponentFormData = buildOpponentFormData();

    Opponent result = opponentMapper.toOpponent(opponentFormData);

    assertNotNull(result);
    assertEquals(opponentFormData.getHouseNameOrNumber(), result.getAddress().getHouseNameOrNumber());
    assertEquals(opponentFormData.getAddressLine1(), result.getAddress().getAddressLine1());
    assertEquals(opponentFormData.getAddressLine2(), result.getAddress().getAddressLine2());
    assertEquals(opponentFormData.getCity(), result.getAddress().getCity());
    assertEquals(opponentFormData.getPostcode(), result.getAddress().getPostcode());
    assertEquals(opponentFormData.getCounty(), result.getAddress().getCounty());
    assertEquals(opponentFormData.getCountry(), result.getAddress().getCountry());
    assertEquals(opponentFormData.getContactNameRole(), result.getContactNameRole());
    assertEquals(opponentFormData.getEmailAddress(), result.getEmailAddress());
    assertEquals(opponentFormData.getFaxNumber(), result.getFaxNumber());
    assertEquals(opponentFormData.getOrganisationName(), result.getOrganisationName());
    assertEquals(opponentFormData.getOrganisationType(), result.getOrganisationType().getId());
    assertEquals(opponentFormData.getOrganisationTypeDisplayValue(), result.getOrganisationType().getDisplayValue());
    assertEquals(opponentFormData.getOtherInformation(), result.getOtherInformation());
    assertEquals(opponentFormData.getPartyId(), result.getPartyId());
    assertEquals(opponentFormData.getPartyId(), result.getEbsId());
    assertEquals(opponentFormData.getRelationshipToCase(), result.getRelationshipToCase());
    assertEquals(opponentFormData.getRelationshipToClient(), result.getRelationshipToClient());
    assertEquals(opponentFormData.getType(), result.getType());
  }

  private static OpponentFormData buildOpponentFormData() {
    OpponentFormData opponentFormData = new OpponentFormData();
    opponentFormData.setAddressLine1("add1");
    opponentFormData.setAddressLine2("add2");
    opponentFormData.setCity("thecity");
    opponentFormData.setContactNameRole("thecontact");
    opponentFormData.setCountry("thecountry");
    opponentFormData.setCounty("thecounty");
    opponentFormData.setCurrentlyTrading(true);
    opponentFormData.setDobDay("1");
    opponentFormData.setDobMonth("10");
    opponentFormData.setDobYear("2024");
    opponentFormData.setEmailAddress("email");
    opponentFormData.setFaxNumber("fax");
    opponentFormData.setHouseNameOrNumber("nameornum");
    opponentFormData.setOrganisationName("orgname");
    opponentFormData.setOrganisationType("orgtype");
    opponentFormData.setOrganisationTypeDisplayValue("orgdisplay");
    opponentFormData.setOtherInformation("otherinf");
    opponentFormData.setPartyId("party");
    opponentFormData.setPostcode("post");
    opponentFormData.setRelationshipToCase("rel2case");
    opponentFormData.setRelationshipToClient("rel2client");
    opponentFormData.setShared(true);
    opponentFormData.setTelephoneHome("telhome");
    opponentFormData.setTelephoneMobile("telmob");
    opponentFormData.setTelephoneWork("telwork");
    opponentFormData.setType("type");
    return opponentFormData;
  }
}
