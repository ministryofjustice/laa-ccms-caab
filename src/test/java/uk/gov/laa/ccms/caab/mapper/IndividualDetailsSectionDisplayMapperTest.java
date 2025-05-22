package uk.gov.laa.ccms.caab.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.laa.ccms.caab.model.OpponentDetail;
import uk.gov.laa.ccms.caab.model.sections.IndividualDetailsSectionDisplay;
import uk.gov.laa.ccms.caab.model.sections.IndividualGeneralDetailsSectionDisplay;

@DisplayName("IndividualDetailsSectionDisplay Mapper test")
class IndividualDetailsSectionDisplayMapperTest {

  IndividualDetailsSectionDisplayMapper mapper = new IndividualDetailsSectionDisplayMapperImpl();

  @Test
  @DisplayName("Should map individual details section display")
  void shouldMapGeneralDetails(){
    // Given
    OpponentDetail individualDetails = new OpponentDetail();
    individualDetails.setTitle("Title");
    individualDetails.setFirstName("First");
    individualDetails.setMiddleNames("Middle");
    individualDetails.setSurname("Last");
    individualDetails.setDateOfBirth(Date.from(LocalDate.of(2000, 1, 1).atStartOfDay().toInstant(
        ZoneOffset.UTC)));
    individualDetails.setRelationshipToClient("Related to client");
    individualDetails.setRelationshipToCase("Related to case");
    individualDetails.setPublicFundingApplied(true);
    individualDetails.setNationalInsuranceNumber("NI1234567A");
    // When
    IndividualDetailsSectionDisplay result
        = mapper.toIndividualDetailsSectionDisplay(individualDetails);
    // Then
    IndividualGeneralDetailsSectionDisplay generalDetails = result.generalDetails();
    assertThat(generalDetails).isNotNull();
    assertThat(generalDetails.getTitle()).isEqualTo("Title");
    assertThat(generalDetails.getFirstName()).isEqualTo("First");
    assertThat(generalDetails.getMiddleNames()).isEqualTo("Middle");
    assertThat(generalDetails.getSurname()).isEqualTo("Last");
    assertThat(generalDetails.getDateOfBirth()).isEqualTo(LocalDate.of(2000, 1, 1));
    assertThat(generalDetails.getRelationshipToClient()).isEqualTo("Related to client");
    assertThat(generalDetails.getRelationshipToCase()).isEqualTo("Related to case");
    assertThat(generalDetails.getPublicFundingApplied()).isTrue();
    assertThat(generalDetails.getNationalInsuranceNumber()).isEqualTo("NI1234567A");
  }

  /*@Test
  @DisplayName("Should map individual details section display")
  void shouldMapAddressDetails(){
    // Given
    OpponentDetail individualDetails = new OpponentDetail();
    individualDetails.setTitle("Title");
    individualDetails.setFirstName("First");
    individualDetails.setMiddleNames("Middle");
    individualDetails.setSurname("Last");
    individualDetails.setDateOfBirth(Date.from(LocalDate.of(2000, 1, 1).atStartOfDay().toInstant(
        ZoneOffset.UTC)));
    individualDetails.setRelationshipToClient("Related to client");
    individualDetails.setRelationshipToCase("Related to case");
    individualDetails.setPublicFundingApplied(true);
    individualDetails.setNationalInsuranceNumber("NI1234567A");
    // When
    IndividualDetailsSectionDisplay result
        = mapper.toIndividualDetailsSectionDisplay(individualDetails);
    // Then
    IndividualGeneralDetailsSectionDisplay generalDetails = result.generalDetails();
    assertThat(generalDetails).isNotNull();
    assertThat(generalDetails.getTitle()).isEqualTo("Title");
    assertThat(generalDetails.getFirstName()).isEqualTo("First");
    assertThat(generalDetails.getMiddleNames()).isEqualTo("Middle");
    assertThat(generalDetails.getSurname()).isEqualTo("Last");
    assertThat(generalDetails.getDateOfBirth()).isEqualTo(LocalDate.of(2000, 1, 1));
    assertThat(generalDetails.getRelationshipToClient()).isEqualTo("Related to client");
    assertThat(generalDetails.getRelationshipToCase()).isEqualTo("Related to case");
    assertThat(generalDetails.getPublicFundingApplied()).isTrue();
    assertThat(generalDetails.getNationalInsuranceNumber()).isEqualTo("NI1234567A");
  }*/
}
