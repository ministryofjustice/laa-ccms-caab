package uk.gov.laa.ccms.caab.mapper;


import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.stereotype.Component;
import uk.gov.laa.ccms.caab.model.ClientResultRowDisplay;
import uk.gov.laa.ccms.caab.model.ClientResultsDisplay;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetail;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetails;
import uk.gov.laa.ccms.soa.gateway.model.ClientSummary;

/**
 * Maps between CAAB client models and SOA gateway client models.
 */
@Mapper(componentModel = "spring")
public interface ClientResultDisplayMapper {

  @Mapping(target = "firstName", source = "details.name.firstName")
  @Mapping(target = "surname", source = "details.name.surname")
  @Mapping(target = "surnameAtBirth", source = "details.name.surnameAtBirth")
  @Mapping(target = "postalCode", source = "details.address.postalCode")
  @Mapping(target = "clientReferenceNumber", ignore = true)
  ClientResultRowDisplay toClientResultRowDisplay(ClientDetail clientDetail);

  ClientResultRowDisplay toClientResultRowDisplay(ClientSummary clientDetail);

  ClientResultsDisplay toClientResultsDisplay(ClientDetails clientDetails);

}
