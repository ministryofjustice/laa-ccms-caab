package uk.gov.laa.ccms.caab.mapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.laa.ccms.caab.model.ClientResultRowDisplay;
import uk.gov.laa.ccms.soa.gateway.model.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
public class ClientResultDisplayMapperTest {

    private ClientResultDisplayMapper mapper;

    @BeforeEach
    public void setUp() {
        mapper = Mappers.getMapper(ClientResultDisplayMapper.class);
    }

    @Test
    public void testToClientResultRowDisplay_FromClientDetail() {
        ClientNameDetail name = new ClientNameDetail()
                .firstName("John")
                .surname("Doe")
                .surnameAtBirth("Smith");

        ClientAddressDetail address = new ClientAddressDetail();
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
        clientSummary.setCaseReferenceNumber("case123");

        ClientResultRowDisplay result = mapper.toClientResultRowDisplay(clientSummary);

        assertEquals("case123", result.getClientReferenceNumber());
    }

}