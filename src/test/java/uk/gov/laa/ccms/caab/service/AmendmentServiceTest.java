package uk.gov.laa.ccms.caab.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.APP_TYPE_SUBSTANTIVE;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.APP_TYPE_SUBSTANTIVE_DEVOLVED_POWERS;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.bean.ApplicationFormData;
import uk.gov.laa.ccms.caab.client.CaabApiClient;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.util.DateUtils;
import uk.gov.laa.ccms.data.model.BaseProvider;
import uk.gov.laa.ccms.data.model.UserDetail;

@DisplayName("Amendment service test")
@ExtendWith(MockitoExtension.class)
class AmendmentServiceTest {
  @Mock
  private CaabApiClient caabApiClient;

  @Mock
  private ApplicationService applicationService;

  private AmendmentService amendmentService;

  @BeforeEach
  void beforeEach(){
    amendmentService = new AmendmentService(applicationService, caabApiClient);
  }

  @Test
  @DisplayName("Should create and submit amendment for a case")
  void shouldCreateAndSubmitAmendmentForACase(){
    // Given
    ApplicationFormData applicationFormData = new ApplicationFormData();
    applicationFormData.setApplicationTypeCategory(APP_TYPE_SUBSTANTIVE);
    applicationFormData.setDelegatedFunctions(true);
    applicationFormData.setDelegatedFunctionUsedDate("01/01/2025");
    String caseRef = "12345";
    UserDetail userDetails = new UserDetail();
    userDetails.setProvider(new BaseProvider().id(1001));
    userDetails.setLoginId("LoginID");
    when(applicationService.getCase(any(), any(), any())).thenReturn(new ApplicationDetail());
    when(caabApiClient.createApplication(any(), any())).thenReturn(Mono.just("123"));
    // When
    ApplicationDetail result = amendmentService
        .createAndSubmitAmendmentForCase(applicationFormData, caseRef, userDetails);
    // Then
    assertNotNull(result);
    assertThat(result.getAmendment()).isTrue();
    assertThat(result.getApplicationType().getId()).isEqualTo(APP_TYPE_SUBSTANTIVE_DEVOLVED_POWERS);
    assertThat(result.getApplicationType().getDevolvedPowers().getUsed()).isTrue();
    assertThat(result.getApplicationType().getDevolvedPowers().getDateUsed()).isEqualTo(DateUtils.convertToDate("01/01/2025"));

  }

}
