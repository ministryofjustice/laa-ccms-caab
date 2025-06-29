package uk.gov.laa.ccms.caab.mapper;

import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.OPPONENT_TYPE_ORGANISATION;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import uk.gov.laa.ccms.caab.bean.opponent.AbstractOpponentFormData;
import uk.gov.laa.ccms.caab.bean.opponent.IndividualOpponentFormData;
import uk.gov.laa.ccms.caab.bean.opponent.OrganisationOpponentFormData;
import uk.gov.laa.ccms.caab.model.OpponentDetail;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;
import uk.gov.laa.ccms.soa.gateway.model.OrganisationDetail;

/** Mapper class to convert Opponents between various formats. */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OpponentMapper {

  @Mapping(target = ".", source = "organisation")
  @Mapping(target = "houseNameOrNumber", source = "organisation.address.house")
  @Mapping(target = "addressLine1", source = "organisation.address.addressLine1")
  @Mapping(target = "addressLine2", source = "organisation.address.addressLine2")
  @Mapping(target = "city", source = "organisation.address.city")
  @Mapping(target = "county", source = "organisation.address.county")
  @Mapping(target = "country", source = "organisation.address.country")
  @Mapping(target = "currentlyTrading", source = "organisation.currentlyTrading")
  @Mapping(target = "postcode", source = "organisation.address.postalCode")
  @Mapping(target = "organisationName", source = "organisation.name")
  @Mapping(target = "organisationType", source = "organisation.type")
  @Mapping(target = "organisationTypeDisplayValue", source = "orgTypeLookup.description")
  @Mapping(target = "contactNameRole", source = "organisation.contactName")
  @Mapping(target = "telephoneWork", source = "organisation.contactDetails.telephoneWork")
  @Mapping(target = "faxNumber", source = "organisation.contactDetails.fax")
  @Mapping(target = "emailAddress", source = "organisation.contactDetails.emailAddress")
  @Mapping(target = "shared", constant = "true")
  @Mapping(target = "type", ignore = true)
  OrganisationOpponentFormData toOrganisationOpponentFormData(
      OrganisationDetail organisation, CommonLookupValueDetail orgTypeLookup);

  @Mapping(target = "id", source = "opponent.id")
  @Mapping(target = ".", source = "opponent")
  @Mapping(target = "partyName", source = "partyName")
  @Mapping(target = "relationshipToCaseDisplayValue", source = "relationshipToCaseDisplayValue")
  @Mapping(target = "relationshipToClientDisplayValue", source = "relationshipToClientDisplayValue")
  @Mapping(target = ".", source = "opponent.address")
  @Mapping(target = "organisationType", source = "opponent.organisationType")
  @Mapping(target = "organisationTypeDisplayValue", source = "organisationTypeDisplayValue")
  @Mapping(target = "deletable", source = "opponent.deleteInd")
  @Mapping(target = "shared", source = "opponent.sharedInd")
  @Mapping(target = "editable", source = "editable")
  OrganisationOpponentFormData toOrganisationOpponentFormData(
      final OpponentDetail opponent,
      final String partyName,
      final String organisationTypeDisplayValue,
      final String relationshipToCaseDisplayValue,
      final String relationshipToClientDisplayValue,
      final boolean editable);

  @Mapping(target = "id", source = "opponent.id")
  @Mapping(target = ".", source = "opponent")
  @Mapping(target = "partyName", source = "partyName")
  @Mapping(target = "relationshipToCaseDisplayValue", source = "relationshipToCaseDisplayValue")
  @Mapping(target = "relationshipToClientDisplayValue", source = "relationshipToClientDisplayValue")
  @Mapping(target = ".", source = "opponent.address")
  @Mapping(target = "dateOfBirth", source = "opponent.dateOfBirth", dateFormat = "d/M/yyyy")
  @Mapping(target = "deletable", source = "opponent.deleteInd")
  @Mapping(target = "editable", source = "editable")
  @Mapping(target = "dateOfBirthMandatory", ignore = true)
  IndividualOpponentFormData toIndividualOpponentFormData(
      final OpponentDetail opponent,
      final String partyName,
      final String relationshipToCaseDisplayValue,
      final String relationshipToClientDisplayValue,
      final boolean editable);

  /**
   * Convert an opponent to its form data version, depending on opponent type.
   *
   * @param opponent - the opponent.
   * @param partyName - the party name for the opponent.
   * @param organisationTypeDisplayValue - the organisation type display value.
   * @param relationshipToCaseDisplayValue - the relationship to case display value.
   * @param relationshipToClientDisplayValue - the relationship to client display value.
   * @param editable - flag to indicate that the opponent is editable.
   * @return mapped AbstractOpponentFormData.
   */
  default AbstractOpponentFormData toOpponentFormData(
      final OpponentDetail opponent,
      final String partyName,
      final String organisationTypeDisplayValue,
      final String relationshipToCaseDisplayValue,
      final String relationshipToClientDisplayValue,
      final boolean editable) {
    return OPPONENT_TYPE_ORGANISATION.equals(opponent.getType())
        ? toOrganisationOpponentFormData(
            opponent,
            partyName,
            organisationTypeDisplayValue,
            relationshipToCaseDisplayValue,
            relationshipToClientDisplayValue,
            editable)
        : toIndividualOpponentFormData(
            opponent,
            partyName,
            relationshipToCaseDisplayValue,
            relationshipToClientDisplayValue,
            editable);
  }

  @Mapping(target = "type", source = "type", defaultValue = "Organisation")
  @Mapping(target = "ebsId", source = "partyId")
  @Mapping(target = "deleteInd", source = "deletable")
  @Mapping(target = "confirmed", constant = "true")
  @Mapping(target = "address.houseNameOrNumber", source = "houseNameOrNumber")
  @Mapping(target = "address.addressLine1", source = "addressLine1")
  @Mapping(target = "address.addressLine2", source = "addressLine2")
  @Mapping(target = "address.city", source = "city")
  @Mapping(target = "address.county", source = "county")
  @Mapping(target = "address.country", source = "country")
  @Mapping(target = "address.postcode", source = "postcode")
  @Mapping(target = "sharedInd", source = "shared")
  @Mapping(target = "organisationName", source = "organisationName")
  @Mapping(target = "organisationType", source = "organisationType")
  @Mapping(target = "currentlyTrading", source = "currentlyTrading")
  OpponentDetail toOrganisationOpponent(OrganisationOpponentFormData opponentFormData);

  @Mapping(target = "type", source = "type", defaultValue = "Individual")
  @Mapping(target = "ebsId", source = "partyId")
  @Mapping(target = "deleteInd", source = "deletable")
  @Mapping(target = "confirmed", constant = "true")
  @Mapping(target = "address.houseNameOrNumber", source = "houseNameOrNumber")
  @Mapping(target = "address.addressLine1", source = "addressLine1")
  @Mapping(target = "address.addressLine2", source = "addressLine2")
  @Mapping(target = "address.city", source = "city")
  @Mapping(target = "address.county", source = "county")
  @Mapping(target = "address.country", source = "country")
  @Mapping(target = "address.postcode", source = "postcode")
  @Mapping(target = "title", source = "title")
  @Mapping(target = "firstName", source = "firstName")
  @Mapping(target = "middleNames", source = "middleNames")
  @Mapping(target = "surname", source = "surname")
  @Mapping(target = "dateOfBirth", source = "dateOfBirth", dateFormat = "d/M/yyyy")
  @Mapping(target = "nationalInsuranceNumber", source = "nationalInsuranceNumber")
  @Mapping(target = "telephoneHome", source = "telephoneHome")
  @Mapping(target = "telephoneMobile", source = "telephoneMobile")
  @Mapping(target = "legalAided", source = "legalAided")
  @Mapping(target = "certificateNumber", source = "certificateNumber")
  OpponentDetail toIndividualOpponent(IndividualOpponentFormData opponentFormData);

  /**
   * Convert form data to an opponent, depending on opponent type.
   *
   * @param opponentFormData - the opponent form data.
   * @return mapped OpponentDetail.
   */
  default OpponentDetail toOpponent(final AbstractOpponentFormData opponentFormData) {
    return opponentFormData instanceof OrganisationOpponentFormData formData
        ? toOrganisationOpponent(formData)
        : toIndividualOpponent((IndividualOpponentFormData) opponentFormData);
  }
}
