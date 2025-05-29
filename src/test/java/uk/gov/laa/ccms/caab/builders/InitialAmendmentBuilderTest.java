package uk.gov.laa.ccms.caab.builders;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.data.model.BaseClient;
import uk.gov.laa.ccms.data.model.BaseProvider;
import uk.gov.laa.ccms.data.model.CaseDetail;
import uk.gov.laa.ccms.data.model.SubmittedApplicationDetails;
import uk.gov.laa.ccms.data.model.UserDetail;

@DisplayName("Initial amendment builder test")
class InitialAmendmentBuilderTest {

  private InitialAmendmentBuilder builder;

  @BeforeEach
  void setUp() {
    UserDetail userDetail = new UserDetail();
    BaseProvider provider = new BaseProvider();
    provider.setId(1001);
    userDetail.setProvider(provider);
    builder = new InitialAmendmentBuilder(userDetail);
  }

  @Test
  @DisplayName("Should set initial amendment properties")
  void shouldSetInitialAmendmentProperties() {
    // Given / When
    ApplicationDetail result = builder.build();
    // Then
    assertThat(result.getProviderDetails().getProvider().getId()).isEqualTo(1001);
    assertThat(result.getAmendment()).isTrue();
    assertThat(result.getCostLimit().getChanged()).isFalse();
  }

  @Test
  @DisplayName("Should copy original case reference number")
  void shouldCopyOriginalCaseReferenceNumber(){
    // Given
    CaseDetail caseDetail = new CaseDetail();
    caseDetail.setCaseReferenceNumber("REF: 1234");
    // When
    ApplicationDetail result = builder.withCaseDetail(caseDetail).build();
    // Then
    assertThat(result.getCaseReferenceNumber()).isEqualTo("REF: 1234");
  }

  @Test
  @DisplayName("Should copy original case client properties")
  void shouldCopyOriginalCaseClientValues(){
    // Given
    CaseDetail caseDetail = new CaseDetail();
    SubmittedApplicationDetails applicationDetails = new SubmittedApplicationDetails();
    BaseClient client = new BaseClient();
    client.setFirstName("First Name");
    client.setSurname("SurName");
    client.setClientReferenceNumber("REF 123");
    applicationDetails.setClient(client);
    caseDetail.setApplicationDetails(applicationDetails);
    // When
    ApplicationDetail result = builder.withCaseDetail(caseDetail).build();
    // Then
    assertThat(result.getClient().getFirstName()).isEqualTo("First Name");
    assertThat(result.getClient().getSurname()).isEqualTo("SurName");
    assertThat(result.getClient().getReference()).isEqualTo("REF 123");
  }

  @Test
  @DisplayName("Should copy original case base values")
  void shouldCopyOriginalCaseBaseValues(){
    // Given
    CaseDetail caseDetail = new CaseDetail();
    SubmittedApplicationDetails applicationDetails = new SubmittedApplicationDetails();
    // When
    ApplicationDetail result = builder.withCaseDetail(caseDetail).build();
    // Then

  }
}