package uk.gov.laa.ccms.caab.mapper;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_ORGANISATION_TYPES;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_RELATIONSHIP_TO_CLIENT;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.model.AddressDetail;
import uk.gov.laa.ccms.caab.model.OpponentDetail;
import uk.gov.laa.ccms.caab.model.sections.OrganisationAddressDetailsSectionDisplay;
import uk.gov.laa.ccms.caab.model.sections.OrganisationDetailsSectionDisplay;
import uk.gov.laa.ccms.caab.model.sections.OrganisationOrganisationDetailsSectionDisplay;
import uk.gov.laa.ccms.caab.service.LookupService;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;
import uk.gov.laa.ccms.data.model.RelationshipToCaseLookupDetail;
import uk.gov.laa.ccms.data.model.RelationshipToCaseLookupValueDetail;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrganisationDetailsSectionDisplay Mapper test")
class OrganisationDetailsSectionDisplayMapperTest {

  OrganisationDetailsSectionDisplayMapper mapper = new OrganisationDetailsSectionDisplayMapperImpl();

  @Mock
  LookupService lookupService;

  @BeforeEach
  void beforeEach() {
    mapper.lookupService = lookupService;
  }

  @Test
  @DisplayName("Should map Organisation details section display")
  void shouldMapOrganisationDetails() {
    // Given
    OpponentDetail organisationDetails = new OpponentDetail();
    organisationDetails.setOrganisationName("TestOrganisation");
    organisationDetails.setCurrentlyTrading(true);
    organisationDetails.setContactNameRole("Joe Blogs");
    organisationDetails.setOrganisationType("LA");
    organisationDetails.setRelationshipToClient("REL");
    organisationDetails.setRelationshipToCase("Related to case as organisation");
    when(lookupService.getCommonValues(COMMON_VALUE_RELATIONSHIP_TO_CLIENT))
        .thenReturn(Mono.just(new CommonLookupDetail()
            .content(singletonList(new CommonLookupValueDetail()
                .code("REL").description("Related to client")))));
    when(lookupService.getCommonValues(COMMON_VALUE_ORGANISATION_TYPES))
        .thenReturn(Mono.just(new CommonLookupDetail()
            .content(singletonList(new CommonLookupValueDetail()
                .code("LA").description("Local Authority")))));
    final RelationshipToCaseLookupDetail orgRelationshipsDetail =
        new RelationshipToCaseLookupDetail();
    orgRelationshipsDetail.addContentItem(new RelationshipToCaseLookupValueDetail().code("REL")
        .description("Related to case as organisation"));
    final RelationshipToCaseLookupDetail personRelationshipsDetail =
        new RelationshipToCaseLookupDetail();
    personRelationshipsDetail.addContentItem(new RelationshipToCaseLookupValueDetail().code("REL")
        .description("Related to case as individual"));
    when(lookupService.getOrganisationToCaseRelationships()).thenReturn(
        Mono.just(orgRelationshipsDetail));
    // When
    OrganisationDetailsSectionDisplay result
        = mapper.toOrganisationDetailsSectionDisplay(organisationDetails);
    // Then
    OrganisationOrganisationDetailsSectionDisplay organisationOrganisationDetails = result.organisationDetails();
    assertThat(organisationOrganisationDetails).isNotNull();
    assertThat(organisationOrganisationDetails.getOrganisationName()).isEqualTo("TestOrganisation");
    assertThat(organisationOrganisationDetails.getCurrentlyTrading()).isEqualTo(true);
    assertThat(organisationOrganisationDetails.getContactNameRole()).isEqualTo("Joe Blogs");
    assertThat(organisationOrganisationDetails.getOrganisationType()).isEqualTo("Local Authority");
    assertThat(organisationOrganisationDetails.getRelationshipToClient()).isEqualTo("Related to client");
    assertThat(organisationOrganisationDetails.getRelationshipToCase()).isEqualTo("Related to case as organisation");
  }

  @Test
  @DisplayName("Should map organisation address section display")
  void shouldMapAddressDetails() {
    // Given
    OpponentDetail organisationDetails = new OpponentDetail();
    AddressDetail address = new AddressDetail();
    address.setHouseNameOrNumber("123");
    address.setAddressLine1("Line 1");
    address.setAddressLine2("Line 2");
    address.setCity("City town");
    address.setCounty("County");
    address.setCountry("Country");
    address.setPostcode("SW1 1AB");
    organisationDetails.setAddress(address);
    organisationDetails.setTelephoneHome("12345678900");
    organisationDetails.setEmailAddress("email@address.com");
    organisationDetails.setFaxNumber("00000111111");
    organisationDetails.setOtherInformation("More information");
    when(lookupService.getCommonValues(COMMON_VALUE_RELATIONSHIP_TO_CLIENT)).thenReturn(Mono.empty());
    when(lookupService.getCommonValues(COMMON_VALUE_ORGANISATION_TYPES)).thenReturn(Mono.empty());
    when(lookupService.getOrganisationToCaseRelationships()).thenReturn(Mono.empty());
    // When
    OrganisationDetailsSectionDisplay result
        = mapper.toOrganisationDetailsSectionDisplay(organisationDetails);
    // Then
    OrganisationAddressDetailsSectionDisplay addressDetails = result.addressDetails();
    assertThat(addressDetails).isNotNull();
    assertThat(addressDetails.getTelephone()).isEqualTo("12345678900");
    assertThat(addressDetails.getHouseNameNumber()).isEqualTo("123");
    assertThat(addressDetails.getAddressLineOne()).isEqualTo("Line 1");
    assertThat(addressDetails.getAddressLineTwo()).isEqualTo("Line 2");
    assertThat(addressDetails.getCityTown()).isEqualTo("City town");
    assertThat(addressDetails.getCounty()).isEqualTo("County");
    assertThat(addressDetails.getCountry()).isEqualTo("Country");
    assertThat(addressDetails.getPostcode()).isEqualTo("SW1 1AB");
    assertThat(addressDetails.getEmail()).isEqualTo("email@address.com");
    assertThat(addressDetails.getFax()).isEqualTo("00000111111");
    assertThat(addressDetails.getOtherInformation()).isEqualTo("More information");
  }

}