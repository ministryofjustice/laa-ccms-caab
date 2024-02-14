package uk.gov.laa.ccms.caab.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.laa.ccms.caab.model.Client;
import uk.gov.laa.ccms.caab.model.ClientResultRowDisplay;
import uk.gov.laa.ccms.caab.model.ClientResultsDisplay;
import uk.gov.laa.ccms.caab.model.LinkedCase;
import uk.gov.laa.ccms.caab.model.LinkedCaseResultRowDisplay;
import uk.gov.laa.ccms.soa.gateway.model.AddressDetail;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetail;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetailDetails;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetails;
import uk.gov.laa.ccms.soa.gateway.model.NameDetail;
import uk.gov.laa.ccms.soa.gateway.model.ClientSummary;

@ExtendWith(SpringExtension.class)
class ResultDisplayMapperTest {

  private final ResultDisplayMapper mapper = new ResultDisplayMapperImpl();


  private ClientDetail clientDetail;
  private ClientSummary clientSummary;
  private LinkedCase linkedCase;
  private LinkedCaseResultRowDisplay linkedCaseResultRowDisplay;
  private ClientDetails clientDetails;

  @BeforeEach
  void setUp() {
    final NameDetail name = new NameDetail().firstName("John").surname("Doe").surnameAtBirth("Smith");
    final AddressDetail address = new AddressDetail().postalCode("12345");

    final ClientDetailDetails details = new ClientDetailDetails().name(name).address(address);
    clientDetail = new ClientDetail().details(details);

    clientSummary = new ClientSummary()
        .clientReferenceNumber("client123")
        .firstName("Jane")
        .surname("Smith")
        .surnameAtBirth("Doe")
        .postalCode("54321");

    final Client client = new Client().firstName("Alice").surname("Brown");
    linkedCase = new LinkedCase().client(client).id(1).lscCaseReference("LSC123")
        .relationToCase("Sibling").providerCaseReference("PC123")
        .categoryOfLaw("Family Law").feeEarner("John Doe").status("Active");

    linkedCaseResultRowDisplay = new LinkedCaseResultRowDisplay();
    linkedCaseResultRowDisplay.setClientFirstName(client.getFirstName());
    linkedCaseResultRowDisplay.setClientSurname(client.getSurname());
    linkedCaseResultRowDisplay.setClientReferenceNumber(client.getReference());

    linkedCaseResultRowDisplay.setId(linkedCase.getId());
    linkedCaseResultRowDisplay.setLscCaseReference(linkedCase.getLscCaseReference());
    linkedCaseResultRowDisplay.setRelationToCase(linkedCase.getRelationToCase());
    linkedCaseResultRowDisplay.setProviderCaseReference(linkedCase.getProviderCaseReference());
    linkedCaseResultRowDisplay.setCategoryOfLaw(linkedCase.getCategoryOfLaw());
    linkedCaseResultRowDisplay.setFeeEarner(linkedCase.getFeeEarner());
    linkedCaseResultRowDisplay.setStatus(linkedCase.getStatus());


    clientDetails = new ClientDetails()
        .content(Collections.singletonList(clientSummary))
        .totalPages(1)
        .totalElements(1)
        .number(0)
        .size(10);
  }

  @Test
  void testToClientResultRowDisplay_FromClientDetail() {
    final ClientResultRowDisplay result = mapper.toClientResultRowDisplay(clientDetail);

    assertEquals("John", result.getFirstName());
    assertEquals("Doe", result.getSurname());
    assertEquals("Smith", result.getSurnameAtBirth());
    assertEquals("12345", result.getPostalCode());
  }

  @Test
  void testToClientResultRowDisplay_FromClientSummary() {
    final ClientResultRowDisplay result = mapper.toClientResultRowDisplay(clientSummary);

    assertEquals("Jane", result.getFirstName());
    assertEquals("Smith", result.getSurname());
    assertEquals("Doe", result.getSurnameAtBirth());
    assertEquals("54321", result.getPostalCode());
    assertEquals("client123", result.getClientReferenceNumber());
  }

  @Test
  void testToClientResultsDisplay_FromClientDetails() {
    final ClientResultsDisplay result = mapper.toClientResultsDisplay(clientDetails);

    assertEquals(1, result.getTotalPages());
    assertEquals(1, result.getTotalElements());
    assertEquals(0, result.getNumber());
    assertEquals(10, result.getSize());
    assertEquals(1, result.getContent().size());
    assertEquals("Jane", result.getContent().get(0).getFirstName());
  }

  @Test
  void testToLinkedCaseResultRowDisplay() {
    final LinkedCaseResultRowDisplay result = mapper.toLinkedCaseResultRowDisplay(linkedCase);

    assertEquals("Alice", result.getClientFirstName());
    assertEquals("Brown", result.getClientSurname());
    assertEquals(1, result.getId());
    assertEquals("LSC123", result.getLscCaseReference());
    assertEquals("Sibling", result.getRelationToCase());
    assertEquals("PC123", result.getProviderCaseReference());
    assertEquals("Family Law", result.getCategoryOfLaw());
    assertEquals("John Doe", result.getFeeEarner());
    assertEquals("Active", result.getStatus());
  }

}