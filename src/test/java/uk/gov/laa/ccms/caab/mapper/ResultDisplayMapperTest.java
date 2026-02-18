package uk.gov.laa.ccms.caab.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.laa.ccms.caab.model.ClientDetail;
import uk.gov.laa.ccms.caab.model.ClientResultRowDisplay;
import uk.gov.laa.ccms.caab.model.ClientResultsDisplay;
import uk.gov.laa.ccms.caab.model.LinkedCaseDetail;
import uk.gov.laa.ccms.caab.model.LinkedCaseResultRowDisplay;
import uk.gov.laa.ccms.caab.model.OrganisationResultRowDisplay;
import uk.gov.laa.ccms.caab.model.ResultsDisplay;
import uk.gov.laa.ccms.data.model.ClientDetails;
import uk.gov.laa.ccms.data.model.ClientSummary;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;
import uk.gov.laa.ccms.soa.gateway.model.AddressDetail;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetailDetails;
import uk.gov.laa.ccms.soa.gateway.model.NameDetail;
import uk.gov.laa.ccms.soa.gateway.model.OrganisationDetails;
import uk.gov.laa.ccms.soa.gateway.model.OrganisationSummary;

@ExtendWith(MockitoExtension.class)
class ResultDisplayMapperTest {

  @Mock(answer = Answers.CALLS_REAL_METHODS)
  CommonMapper commonMapper;

  @InjectMocks ResultDisplayMapper mapper = new ResultDisplayMapperImpl();

  private uk.gov.laa.ccms.soa.gateway.model.ClientDetail clientDetail;
  private ClientSummary clientSummary;
  private LinkedCaseDetail linkedCase;
  private LinkedCaseResultRowDisplay linkedCaseResultRowDisplay;
  private ClientDetails clientDetails;

  @BeforeEach
  void setUp() {
    final NameDetail name =
        new NameDetail().firstName("John").surname("Doe").surnameAtBirth("Smith");
    final AddressDetail address = new AddressDetail().postalCode("12345");

    final ClientDetailDetails details = new ClientDetailDetails().name(name).address(address);
    clientDetail = new uk.gov.laa.ccms.soa.gateway.model.ClientDetail().details(details);

    clientSummary =
        new ClientSummary()
            .clientReferenceNumber("client123")
            .firstName("Jane")
            .surname("Smith")
            .surnameAtBirth("Doe")
            .postalCode("54321");

    final ClientDetail client = new ClientDetail().firstName("Alice").surname("Brown");
    linkedCase =
        new LinkedCaseDetail()
            .client(client)
            .id(1)
            .lscCaseReference("LSC123")
            .relationToCase("LEGAL")
            .providerCaseReference("PC123")
            .categoryOfLaw("Family Law")
            .feeEarner("John Doe")
            .status("Active");

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

    clientDetails =
        new ClientDetails()
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
    assertEquals("Jane", result.getContent().getFirst().getFirstName());
  }

  @Test
  void testToLinkedCaseResultRowDisplay() {
    final LinkedCaseResultRowDisplay result =
        mapper.toLinkedCaseResultRowDisplay(linkedCase, Map.of("LEGAL", "Linked Legal Issue"));

    assertEquals("Alice", result.getClientFirstName());
    assertEquals("Brown", result.getClientSurname());
    assertEquals(1, result.getId());
    assertEquals("LSC123", result.getLscCaseReference());
    assertEquals("Linked Legal Issue", result.getRelationToCase());
    assertEquals("PC123", result.getProviderCaseReference());
    assertEquals("Family Law", result.getCategoryOfLaw());
    assertEquals("John Doe", result.getFeeEarner());
    assertEquals("Active", result.getStatus());
  }

  @Test
  void testToOrganisationResultRowDisplay() {
    OrganisationSummary organisationSummary =
        new OrganisationSummary()
            .city("thecity")
            .name("thename")
            .partyId("123")
            .postcode("postcode")
            .type("thetype");

    List<CommonLookupValueDetail> orgTypesLookup =
        List.of(
            new CommonLookupValueDetail()
                .code(organisationSummary.getType())
                .description("the description"));

    final OrganisationResultRowDisplay result =
        mapper.toOrganisationResultRowDisplay(organisationSummary, orgTypesLookup);

    assertEquals(organisationSummary.getName(), result.getName());
    assertEquals(organisationSummary.getType(), result.getType());
    assertEquals(orgTypesLookup.getFirst().getDescription(), result.getTypeDisplayValue());
    assertEquals(organisationSummary.getPartyId(), result.getPartyId());
    assertEquals(organisationSummary.getPostcode(), result.getPostcode());
    assertEquals(organisationSummary.getCity(), result.getCity());
  }

  @Test
  void testToOrganisationResultRowDisplay_noTypeMatchReturnsCode() {
    OrganisationSummary organisationSummary =
        new OrganisationSummary()
            .city("thecity")
            .name("thename")
            .partyId("123")
            .postcode("postcode")
            .type("thetype");

    List<CommonLookupValueDetail> orgTypesLookup =
        List.of(new CommonLookupValueDetail().code("wrong type").description("the description"));

    final OrganisationResultRowDisplay result =
        mapper.toOrganisationResultRowDisplay(organisationSummary, orgTypesLookup);

    assertEquals(organisationSummary.getName(), result.getName());
    assertEquals(organisationSummary.getType(), result.getType());
    assertEquals(organisationSummary.getType(), result.getTypeDisplayValue());
    assertEquals(organisationSummary.getPartyId(), result.getPartyId());
    assertEquals(organisationSummary.getPostcode(), result.getPostcode());
    assertEquals(organisationSummary.getCity(), result.getCity());
  }

  @Test
  void testToOrganisationResultDisplay() {
    OrganisationDetails organisationDetails =
        new OrganisationDetails()
            .addContentItem(new OrganisationSummary())
            .totalElements(1)
            .size(1)
            .totalPages(1)
            .number(1);

    List<CommonLookupValueDetail> orgTypesLookup =
        List.of(new CommonLookupValueDetail().code("thecode").description("thedescr"));

    final ResultsDisplay<OrganisationResultRowDisplay> result =
        mapper.toOrganisationResultsDisplay(organisationDetails, orgTypesLookup);

    assertNotNull(result);
    assertEquals(1, result.getContent().size());
    assertEquals(organisationDetails.getNumber(), result.getNumber());
    assertEquals(organisationDetails.getSize(), result.getSize());
    assertEquals(organisationDetails.getTotalElements(), result.getTotalElements());
    assertEquals(organisationDetails.getTotalPages(), result.getTotalPages());
  }
}
