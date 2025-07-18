package uk.gov.laa.ccms.caab.mapper;

import java.util.List;
import java.util.Map;
import org.mapstruct.Context;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import uk.gov.laa.ccms.caab.model.BaseApplicationDetail;
import uk.gov.laa.ccms.caab.model.ClientResultRowDisplay;
import uk.gov.laa.ccms.caab.model.ClientResultsDisplay;
import uk.gov.laa.ccms.caab.model.LinkedCaseDetail;
import uk.gov.laa.ccms.caab.model.LinkedCaseResultRowDisplay;
import uk.gov.laa.ccms.caab.model.OrganisationResultRowDisplay;
import uk.gov.laa.ccms.caab.model.ResultsDisplay;
import uk.gov.laa.ccms.data.model.ClientDetails;
import uk.gov.laa.ccms.data.model.ClientSummary;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetail;
import uk.gov.laa.ccms.soa.gateway.model.OrganisationDetails;
import uk.gov.laa.ccms.soa.gateway.model.OrganisationSummary;

/** Maps between multiple objects into results display objects. */
@Mapper(componentModel = "spring", uses = CommonMapper.class)
public interface ResultDisplayMapper {

  @Mapping(target = "firstName", source = "details.name.firstName")
  @Mapping(target = "surname", source = "details.name.surname")
  @Mapping(target = "surnameAtBirth", source = "details.name.surnameAtBirth")
  @Mapping(target = "postalCode", source = "details.address.postalCode")
  @Mapping(target = "clientReferenceNumber", ignore = true)
  ClientResultRowDisplay toClientResultRowDisplay(ClientDetail clientDetail);

  ClientResultRowDisplay toClientResultRowDisplay(ClientSummary clientDetail);

  ClientResultsDisplay toClientResultsDisplay(ClientDetails clientDetails);

  @Mapping(target = "clientFirstName", source = "client.firstName")
  @Mapping(target = "clientSurname", source = "client.surname")
  @Mapping(target = "clientReferenceNumber", source = "client.reference")
  @Mapping(
      target = "relationToCase",
      expression =
          """
          java(ResultDisplayMapper
          .getValueFromLookup(relationLookup, linkedCase.getRelationToCase()))""")
  LinkedCaseResultRowDisplay toLinkedCaseResultRowDisplay(
      LinkedCaseDetail linkedCase, @Context Map<String, String> relationLookup);

  @Mapping(target = "clientFirstName", source = "client.firstName")
  @Mapping(target = "clientSurname", source = "client.surname")
  @Mapping(target = "clientReferenceNumber", source = "client.reference")
  @Mapping(target = "lscCaseReference", source = "caseReferenceNumber")
  @Mapping(target = "providerCaseReference", source = "providerDetails.providerCaseReference")
  @Mapping(target = "categoryOfLaw", source = "categoryOfLaw.displayValue")
  @Mapping(target = "feeEarner", source = "providerDetails.feeEarner.displayValue")
  @Mapping(target = "status", source = "status.displayValue")
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "relationToCase", ignore = true)
  LinkedCaseResultRowDisplay toLinkedCaseResultRowDisplay(BaseApplicationDetail baseApplication);

  @Mapping(target = "auditTrail", ignore = true)
  @InheritInverseConfiguration(name = "toLinkedCaseResultRowDisplay")
  LinkedCaseDetail toLinkedCase(LinkedCaseResultRowDisplay linkedCaseResultRowDisplay);

  @Mapping(target = "typeDisplayValue", source = "type", qualifiedByName = "toDisplayValue")
  OrganisationResultRowDisplay toOrganisationResultRowDisplay(
      OrganisationSummary organisationSummary,
      @Context List<CommonLookupValueDetail> organisationTypes);

  ResultsDisplay<OrganisationResultRowDisplay> toOrganisationResultsDisplay(
      OrganisationDetails organisationDetails,
      @Context List<CommonLookupValueDetail> organisationTypes);

  static String getValueFromLookup(Map<String, String> map, String key) {
    return map.getOrDefault(key, key);
  }
}
