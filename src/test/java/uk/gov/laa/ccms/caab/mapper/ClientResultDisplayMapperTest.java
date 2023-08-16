package uk.gov.laa.ccms.caab.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.laa.ccms.caab.model.ClientResultRowDisplay;
import uk.gov.laa.ccms.soa.gateway.model.AddressDetail;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetail;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetailDetails;
import uk.gov.laa.ccms.soa.gateway.model.ClientNameDetail;
import uk.gov.laa.ccms.soa.gateway.model.ClientSummary;

@ExtendWith(SpringExtension.class)
public class ClientResultDisplayMapperTest {

  private final ClientResultDisplayMapper mapper = new ClientResultDisplayMapperImpl();
  

  @Test
  public void testToClientResultRowDisplay_FromClientDetail() {
    ClientNameDetail name = new ClientNameDetail()
        .firstName("John")
        .surname("Doe")
        .surnameAtBirth("Smith");

    AddressDetail address = new AddressDetail();
    address.setPostalCode("12345");
    ClientDetail clientDetail = new ClientDetail();

    ClientDetailDetails details = new ClientDetailDetails();
    details.setName(name);
    details.setAddress(address);

    clientDetail.setDetails(details);

    ClientResultRowDisplay result = mapper.toClientResultRowDisplay(clientDetail);

    assertEquals("John", result.getFirstName());
    assertEquals("Doe", result.getSurname());
    assertEquals("Smith", result.getSurnameAtBirth());
    assertEquals("12345", result.getPostalCode());
  }

  @Test
  public void testToClientResultRowDisplay_FromClientSummary() {
    ClientSummary clientSummary = new ClientSummary();
    clientSummary.setClientReferenceNumber("client123");

    ClientResultRowDisplay result = mapper.toClientResultRowDisplay(clientSummary);

    assertEquals("client123", result.getClientReferenceNumber());
  }

}