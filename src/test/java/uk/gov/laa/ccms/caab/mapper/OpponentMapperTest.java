package uk.gov.laa.ccms.caab.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.OPPONENT_TYPE_INDIVIDUAL;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.OPPONENT_TYPE_ORGANISATION;
import static uk.gov.laa.ccms.caab.util.CaabModelUtils.buildOpponent;
import static uk.gov.laa.ccms.caab.util.SoaModelUtils.buildOrganisationDetail;

import java.text.SimpleDateFormat;
import java.util.Date;
import org.junit.jupiter.api.Test;
import uk.gov.laa.ccms.caab.bean.opponent.AbstractOpponentFormData;
import uk.gov.laa.ccms.caab.bean.opponent.IndividualOpponentFormData;
import uk.gov.laa.ccms.caab.bean.opponent.OrganisationOpponentFormData;
import uk.gov.laa.ccms.caab.model.OpponentDetail;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;
import uk.gov.laa.ccms.soa.gateway.model.OrganisationDetail;

public class OpponentMapperTest {
  OpponentMapper opponentMapper = new OpponentMapperImpl();

  @Test
  void testOrganisationDetail_toOrganisationOpponentFormData() {
    OrganisationDetail organisationDetail = buildOrganisationDetail("");
    CommonLookupValueDetail orgTypeLookup =
        new CommonLookupValueDetail().code("thecode").description("org type");

    OrganisationOpponentFormData result =
        opponentMapper.toOrganisationOpponentFormData(organisationDetail, orgTypeLookup);

    assertNotNull(result);
    assertEquals(organisationDetail.getAddress().getAddressLine1(), result.getAddressLine1());
    assertEquals(organisationDetail.getAddress().getAddressLine2(), result.getAddressLine2());
    assertEquals(organisationDetail.getAddress().getCity(), result.getCity());
    assertEquals(organisationDetail.getAddress().getCountry(), result.getCountry());
    assertEquals(organisationDetail.getAddress().getPostalCode(), result.getPostcode());
    assertEquals(
        organisationDetail.getContactDetails().getEmailAddress(), result.getEmailAddress());
    assertEquals(organisationDetail.getContactDetails().getFax(), result.getFaxNumber());
    assertEquals(
        organisationDetail.getContactDetails().getTelephoneWork(), result.getTelephoneWork());
    assertEquals(organisationDetail.getContactName(), result.getContactNameRole());
    assertEquals(organisationDetail.getName(), result.getOrganisationName());
    assertEquals(organisationDetail.getType(), result.getOrganisationType());
    assertEquals(orgTypeLookup.getDescription(), result.getOrganisationTypeDisplayValue());
    assertEquals(organisationDetail.getOtherInformation(), result.getOtherInformation());
    assertEquals(organisationDetail.getPartyId(), result.getPartyId());
  }

  @Test
  void testOrganisationOpponentFormData_toOpponent() {
    OrganisationOpponentFormData opponentFormData = buildOrganisationOpponentFormData();

    OpponentDetail result = opponentMapper.toOpponent(opponentFormData);

    assertNotNull(result);
    assertEquals(
        opponentFormData.getHouseNameOrNumber(), result.getAddress().getHouseNameOrNumber());
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
    assertEquals(opponentFormData.getOrganisationType(), result.getOrganisationType());
    assertEquals(opponentFormData.getOtherInformation(), result.getOtherInformation());
    assertEquals(opponentFormData.getPartyId(), result.getPartyId());
    assertEquals(opponentFormData.getPartyId(), result.getEbsId());
    assertEquals(opponentFormData.getRelationshipToCase(), result.getRelationshipToCase());
    assertEquals(opponentFormData.getRelationshipToClient(), result.getRelationshipToClient());
    assertEquals(opponentFormData.getType(), result.getType());
    assertEquals(opponentFormData.getTelephoneWork(), result.getTelephoneWork());
  }

  @Test
  void testIndividualOpponentFormData_toOpponent() {
    IndividualOpponentFormData opponentFormData = buildIndividualOpponentFormData();

    OpponentDetail result = opponentMapper.toOpponent(opponentFormData);

    assertNotNull(result);
    assertEquals(
        opponentFormData.getHouseNameOrNumber(), result.getAddress().getHouseNameOrNumber());
    assertEquals(opponentFormData.getAddressLine1(), result.getAddress().getAddressLine1());
    assertEquals(opponentFormData.getAddressLine2(), result.getAddress().getAddressLine2());
    assertEquals(opponentFormData.getCity(), result.getAddress().getCity());
    assertEquals(opponentFormData.getPostcode(), result.getAddress().getPostcode());
    assertEquals(opponentFormData.getCounty(), result.getAddress().getCounty());
    assertEquals(opponentFormData.getCountry(), result.getAddress().getCountry());
    assertEquals(opponentFormData.getEmailAddress(), result.getEmailAddress());
    assertEquals(opponentFormData.getFaxNumber(), result.getFaxNumber());

    assertEquals(opponentFormData.getTitle(), result.getTitle());
    assertEquals(opponentFormData.getFirstName(), result.getFirstName());
    assertEquals(opponentFormData.getMiddleNames(), result.getMiddleNames());
    assertEquals(opponentFormData.getSurname(), result.getSurname());
    assertEquals(
        opponentFormData.getDateOfBirth(),
        new SimpleDateFormat("d/M/yyyy").format(result.getDateOfBirth()));
    assertEquals(
        opponentFormData.getNationalInsuranceNumber(), result.getNationalInsuranceNumber());
    assertEquals(opponentFormData.getLegalAided(), result.getLegalAided());
    assertEquals(opponentFormData.getCertificateNumber(), result.getCertificateNumber());

    assertEquals(opponentFormData.getPartyId(), result.getPartyId());
    assertEquals(opponentFormData.getPartyId(), result.getEbsId());
    assertEquals(opponentFormData.getRelationshipToCase(), result.getRelationshipToCase());
    assertEquals(opponentFormData.getRelationshipToClient(), result.getRelationshipToClient());
    assertEquals(opponentFormData.getType(), result.getType());
    assertEquals(opponentFormData.getTelephoneHome(), result.getTelephoneHome());
    assertEquals(opponentFormData.getTelephoneWork(), result.getTelephoneWork());
    assertEquals(opponentFormData.getTelephoneMobile(), result.getTelephoneMobile());
  }

  @Test
  void testOrganisationOpponent_toOrganisationOpponentFormData() {
    OpponentDetail opponent = buildOpponent(new Date());
    opponent.setType(OPPONENT_TYPE_ORGANISATION);

    final String partyName = "party";
    final String organisationTypeDisplayValue = "org type";
    final String relationshipToCaseDisplayValue = "relationship 2 case";
    final String relationshipToClientDisplayValue = "relationship 2 client";
    AbstractOpponentFormData result =
        opponentMapper.toOpponentFormData(
            opponent,
            partyName,
            organisationTypeDisplayValue,
            relationshipToCaseDisplayValue,
            relationshipToClientDisplayValue,
            true);

    assertNotNull(result);
    assertInstanceOf(OrganisationOpponentFormData.class, result);

    OrganisationOpponentFormData orgResult = (OrganisationOpponentFormData) result;
    assertEquals(opponent.getId(), orgResult.getId());
    assertEquals(opponent.getAddress().getAddressLine1(), orgResult.getAddressLine1());
    assertEquals(opponent.getAddress().getAddressLine2(), orgResult.getAddressLine2());
    assertEquals(opponent.getAddress().getCity(), orgResult.getCity());
    assertEquals(opponent.getAddress().getCounty(), orgResult.getCounty());
    assertEquals(opponent.getAddress().getCountry(), orgResult.getCountry());
    assertEquals(opponent.getAddress().getPostcode(), orgResult.getPostcode());
    assertEquals(opponent.getAddress().getHouseNameOrNumber(), orgResult.getHouseNameOrNumber());
    assertEquals(opponent.getAmendment(), orgResult.getAmendment());
    assertEquals(opponent.getAppMode(), orgResult.getAppMode());
    assertEquals(opponent.getContactNameRole(), orgResult.getContactNameRole());
    assertEquals(opponent.getCurrentlyTrading(), orgResult.getCurrentlyTrading());
    assertEquals(opponent.getDeleteInd(), orgResult.getDeletable());
    assertEquals(opponent.getPartyId(), orgResult.getPartyId());
    assertEquals(opponent.getEmailAddress(), orgResult.getEmailAddress());
    assertEquals(opponent.getFaxNumber(), orgResult.getFaxNumber());
    assertEquals(opponent.getOrganisationName(), orgResult.getOrganisationName());
    assertEquals(opponent.getOrganisationType(), orgResult.getOrganisationType());
    assertEquals(organisationTypeDisplayValue, orgResult.getOrganisationTypeDisplayValue());
    assertEquals(opponent.getOtherInformation(), orgResult.getOtherInformation());
    assertEquals(opponent.getPartyId(), orgResult.getPartyId());
    assertEquals(opponent.getRelationshipToCase(), orgResult.getRelationshipToCase());
    assertEquals(opponent.getRelationshipToClient(), orgResult.getRelationshipToClient());
    assertEquals(opponent.getSharedInd(), orgResult.getShared());
    assertEquals(opponent.getTelephoneWork(), orgResult.getTelephoneWork());
    assertEquals(opponent.getType(), orgResult.getType());
  }

  @Test
  void testIndividualOpponent_toIndividualOpponentFormData() {
    Date date = new Date();
    OpponentDetail opponent = buildOpponent(date);
    opponent.setType(OPPONENT_TYPE_INDIVIDUAL);

    final String partyName = "party";
    final String organisationTypeDisplayValue = "org type";
    final String relationshipToCaseDisplayValue = "relationship 2 case";
    final String relationshipToClientDisplayValue = "relationship 2 client";
    AbstractOpponentFormData result =
        opponentMapper.toOpponentFormData(
            opponent,
            partyName,
            organisationTypeDisplayValue,
            relationshipToCaseDisplayValue,
            relationshipToClientDisplayValue,
            true);

    assertNotNull(result);
    assertInstanceOf(IndividualOpponentFormData.class, result);

    IndividualOpponentFormData orgResult = (IndividualOpponentFormData) result;
    assertEquals(opponent.getId(), orgResult.getId());
    assertEquals(opponent.getAddress().getAddressLine1(), orgResult.getAddressLine1());
    assertEquals(opponent.getAddress().getAddressLine2(), orgResult.getAddressLine2());
    assertEquals(opponent.getAddress().getCity(), orgResult.getCity());
    assertEquals(opponent.getAddress().getCounty(), orgResult.getCounty());
    assertEquals(opponent.getAddress().getCountry(), orgResult.getCountry());
    assertEquals(opponent.getAddress().getPostcode(), orgResult.getPostcode());
    assertEquals(opponent.getAddress().getHouseNameOrNumber(), orgResult.getHouseNameOrNumber());
    assertEquals(opponent.getAmendment(), orgResult.getAmendment());
    assertEquals(opponent.getAppMode(), orgResult.getAppMode());
    assertEquals(opponent.getDeleteInd(), orgResult.getDeletable());
    assertEquals(opponent.getPartyId(), orgResult.getPartyId());
    assertEquals(opponent.getEmailAddress(), orgResult.getEmailAddress());

    assertEquals(opponent.getTitle(), orgResult.getTitle());
    assertEquals(opponent.getFirstName(), orgResult.getFirstName());
    assertEquals(opponent.getMiddleNames(), orgResult.getMiddleNames());
    assertEquals(opponent.getSurname(), orgResult.getSurname());

    assertEquals(
        new SimpleDateFormat("d/M/yyyy").format(opponent.getDateOfBirth()),
        orgResult.getDateOfBirth());

    assertEquals(opponent.getPartyId(), orgResult.getPartyId());
    assertEquals(opponent.getRelationshipToCase(), orgResult.getRelationshipToCase());
    assertEquals(opponent.getRelationshipToClient(), orgResult.getRelationshipToClient());

    assertEquals(opponent.getTelephoneHome(), orgResult.getTelephoneHome());
    assertEquals(opponent.getTelephoneWork(), orgResult.getTelephoneWork());
    assertEquals(opponent.getTelephoneMobile(), orgResult.getTelephoneMobile());
    assertEquals(opponent.getFaxNumber(), orgResult.getFaxNumber());

    assertEquals(opponent.getType(), orgResult.getType());
  }

  private OrganisationOpponentFormData buildOrganisationOpponentFormData() {
    OrganisationOpponentFormData opponentFormData = new OrganisationOpponentFormData();
    opponentFormData.setAddressLine1("add1");
    opponentFormData.setAddressLine2("add2");
    opponentFormData.setCity("thecity");
    opponentFormData.setContactNameRole("thecontact");
    opponentFormData.setCountry("thecountry");
    opponentFormData.setCounty("thecounty");
    opponentFormData.setCurrentlyTrading(true);
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
    opponentFormData.setTelephoneWork("telwork");
    opponentFormData.setType("type");
    return opponentFormData;
  }

  private IndividualOpponentFormData buildIndividualOpponentFormData() {
    IndividualOpponentFormData opponentFormData = new IndividualOpponentFormData();
    opponentFormData.setAddressLine1("add1");
    opponentFormData.setAddressLine2("add2");
    opponentFormData.setCity("thecity");
    opponentFormData.setCountry("thecountry");
    opponentFormData.setCounty("thecounty");
    opponentFormData.setEmailAddress("email");
    opponentFormData.setFaxNumber("fax");
    opponentFormData.setHouseNameOrNumber("nameornum");

    opponentFormData.setTitle("thetitle");
    opponentFormData.setFirstName("thefirstname");
    opponentFormData.setMiddleNames("themiddles");
    opponentFormData.setSurname("thesurname");
    opponentFormData.setNationalInsuranceNumber("nino");
    opponentFormData.setDateOfBirth("1/10/2024");
    opponentFormData.setLegalAided(true);
    opponentFormData.setCertificateNumber("128376");

    opponentFormData.setPartyId("party");
    opponentFormData.setPostcode("post");
    opponentFormData.setRelationshipToCase("rel2case");
    opponentFormData.setRelationshipToClient("rel2client");

    opponentFormData.setTelephoneHome("telhome");
    opponentFormData.setTelephoneWork("telwork");
    opponentFormData.setTelephoneMobile("mob");

    opponentFormData.setType("type");
    return opponentFormData;
  }
}
