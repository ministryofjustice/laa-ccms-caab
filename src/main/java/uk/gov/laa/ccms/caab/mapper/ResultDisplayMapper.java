package uk.gov.laa.ccms.caab.mapper;

import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import uk.gov.laa.ccms.caab.model.ClientResultRowDisplay;
import uk.gov.laa.ccms.caab.model.ClientResultsDisplay;
import uk.gov.laa.ccms.caab.model.LinkedCase;
import uk.gov.laa.ccms.caab.model.LinkedCaseResultRowDisplay;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetail;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetails;
import uk.gov.laa.ccms.soa.gateway.model.ClientSummary;

/**
 * Maps between multiple objects into results display objects.
 */
@Mapper(componentModel = "spring")
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
  LinkedCaseResultRowDisplay toLinkedCaseResultRowDisplay(LinkedCase linkedCase);

  @Mapping(target = "auditTrail", ignore = true)
  @InheritInverseConfiguration(name = "toLinkedCaseResultRowDisplay")
  LinkedCase toLinkedCase(LinkedCaseResultRowDisplay linkedCaseResultRowDisplay);



}
