package uk.gov.laa.ccms.caab.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.laa.ccms.caab.model.AddressDetail;
import uk.gov.laa.ccms.caab.model.OpponentDetail;
import uk.gov.laa.ccms.caab.model.sections.IndividualAddressContactDetailsSectionDisplay;
import uk.gov.laa.ccms.caab.model.sections.IndividualDetailsSectionDisplay;
import uk.gov.laa.ccms.caab.model.sections.IndividualEmploymentDetailsSectionDisplay;
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

  @Test
  @DisplayName("Should map individual address section display")
  void shouldMapAddressDetails(){
    // Given
    OpponentDetail individualDetails = new OpponentDetail();
    AddressDetail address = new AddressDetail();
    address.setHouseNameOrNumber("123");
    address.setAddressLine1("Line 1");
    address.setAddressLine2("Line 2");
    address.setCity("City town");
    address.setCounty("County");
    address.setCountry("Country");
    address.setPostcode("SW1 1AB");
    individualDetails.setAddress(address);
    individualDetails.setTelephoneHome("12345678900");
    individualDetails.setTelephoneWork("09876543211");
    individualDetails.setTelephoneMobile("1122334455");
    individualDetails.setEmailAddress("email@address.com");
    individualDetails.setFaxNumber("00000111111");
    // When
    IndividualDetailsSectionDisplay result
        = mapper.toIndividualDetailsSectionDisplay(individualDetails);
    // Then
    IndividualAddressContactDetailsSectionDisplay addressDetails = result.addressContactDetails();
    assertThat(addressDetails).isNotNull();
    assertThat(addressDetails.getTelephoneHome()).isEqualTo("12345678900");
    assertThat(addressDetails.getHouseNameNumber()).isEqualTo("123");
    assertThat(addressDetails.getAddressLineOne()).isEqualTo("Line 1");
    assertThat(addressDetails.getAddressLineTwo()).isEqualTo("Line 2");
    assertThat(addressDetails.getCityTown()).isEqualTo("City town");
    assertThat(addressDetails.getCounty()).isEqualTo("County");
    assertThat(addressDetails.getCountry()).isEqualTo("Country");
    assertThat(addressDetails.getPostcode()).isEqualTo("SW1 1AB");
    assertThat(addressDetails.getTelephoneWork()).isEqualTo("09876543211");
    assertThat(addressDetails.getTelephoneMobile()).isEqualTo("1122334455");
    assertThat(addressDetails.getEmail()).isEqualTo("email@address.com");
    assertThat(addressDetails.getFax()).isEqualTo("00000111111");
  }

  @Test
  @DisplayName("Should map individual employment section display")
  void shouldMapEmploymentDetails(){
    // Given
    OpponentDetail individualDetails = new OpponentDetail();
    individualDetails.setEmployerName("Employer name");
    individualDetails.setEmploymentStatus("Employer status");
    individualDetails.setEmployerAddress("Employer address");
    individualDetails.setCertificateNumber("Certificate number");
    individualDetails.setAssessedIncomeFrequency("Frequency");
    individualDetails.setAssessmentDate(Date.from(LocalDate.of(2010, 5, 1).atStartOfDay().toInstant(ZoneOffset.UTC)));
    individualDetails.setCourtOrderedMeansAssessment(true);
    individualDetails.setLegalAided(true);
    individualDetails.setAssessedIncome(BigDecimal.valueOf(123.45));
    individualDetails.setAssessedAssets(BigDecimal.valueOf(678.90));
    individualDetails.setOtherInformation("Other info");
    // When
    IndividualDetailsSectionDisplay result
        = mapper.toIndividualDetailsSectionDisplay(individualDetails);
    // Then
    IndividualEmploymentDetailsSectionDisplay employmentDetails = result.employmentDetails();
    assertThat(employmentDetails).isNotNull();
    assertThat(employmentDetails.getEmployersName()).isEqualTo("Employer name");
    assertThat(employmentDetails.getEmploymentStatus()).isEqualTo("Employer status");
    assertThat(employmentDetails.getEmployersAddress()).isEqualTo("Employer address");
    assertThat(employmentDetails.getCertificateNumber()).isEqualTo("Certificate number");
    assertThat(employmentDetails.getAssessedIncomeFrequency()).isEqualTo("Frequency");
    assertThat(employmentDetails.getAssessmentDate()).isEqualTo(LocalDate.of(2010, 5, 1));
    assertThat(employmentDetails.getHadCourtOrderedMeansAssessment()).isTrue();
    assertThat(employmentDetails.getPartyIsLegalAided()).isTrue();
    assertThat(employmentDetails.getAssessedIncome()).isEqualTo(BigDecimal.valueOf(123.45));
    assertThat(employmentDetails.getAssessedAssets()).isEqualTo(BigDecimal.valueOf(678.90));
    assertThat(employmentDetails.getOtherInformation()).isEqualTo("Other info");
  }
}
