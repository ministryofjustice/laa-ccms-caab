package uk.gov.laa.ccms.caab.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.bean.request.ProviderRequestDetailsFormData;
import uk.gov.laa.ccms.caab.bean.request.ProviderRequestTypeFormData;
import uk.gov.laa.ccms.caab.client.SoaApiClient;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.mapper.ProviderRequestsMapper;
import uk.gov.laa.ccms.caab.mapper.context.ProviderRequestMappingContext;
import uk.gov.laa.ccms.data.model.UserDetail;
import uk.gov.laa.ccms.soa.gateway.model.ProviderRequestDetail;
import uk.gov.laa.ccms.soa.gateway.model.ProviderRequestResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProviderRequestServiceTest {

  @Mock
  private SoaApiClient soaApiClient;

  @Mock
  private ProviderRequestsMapper mapper;

  @InjectMocks
  private ProviderRequestService providerRequestService;

  @Test
  @DisplayName("submitProviderRequest - Successfully submits a provider request and returns notification ID")
  void submitProviderRequest_Success() {
    final ProviderRequestTypeFormData typeFormData = mock(ProviderRequestTypeFormData.class);
    final ProviderRequestDetailsFormData detailsFormData = mock(ProviderRequestDetailsFormData.class);
    final UserDetail userDetail = mock(UserDetail.class);
    final ProviderRequestMappingContext mappingContext = ProviderRequestMappingContext.builder()
        .user(userDetail)
        .typeData(typeFormData)
        .detailsData(detailsFormData)
        .build();

    final String notificationId = "12345";
    final ProviderRequestDetail providerRequestDetail = mock(ProviderRequestDetail.class);
    final ProviderRequestResponse providerRequestResponse = new ProviderRequestResponse();
    providerRequestResponse.notificationId(notificationId);


    when(mapper.toProviderRequestDetail(mappingContext)).thenReturn(providerRequestDetail);
    when(userDetail.getLoginId()).thenReturn("testLoginId");
    when(userDetail.getUserType()).thenReturn("testUserType");
    when(soaApiClient.submitProviderRequest(providerRequestDetail, "testLoginId", "testUserType"))
        .thenReturn(Mono.just(providerRequestResponse));

    final String result = providerRequestService.submitProviderRequest(typeFormData, detailsFormData, userDetail);

    assertEquals(notificationId, result);
    verify(mapper).toProviderRequestDetail(mappingContext);
    verify(soaApiClient).submitProviderRequest(providerRequestDetail, "testLoginId", "testUserType");
  }

  @Test
  @DisplayName("submitProviderRequest - Throws CaabApplicationException on exception from SoaApiClient")
  void submitProviderRequest_ThrowsException() {
    final ProviderRequestTypeFormData typeFormData = new ProviderRequestTypeFormData();
    final ProviderRequestDetailsFormData detailsFormData = new ProviderRequestDetailsFormData();
    final UserDetail userDetail = new UserDetail();
    userDetail.setLoginId("testLoginId");
    userDetail.setUserType("testUserType");

    final ProviderRequestDetail providerRequestDetail = new ProviderRequestDetail();

    final ProviderRequestMappingContext mappingContext = ProviderRequestMappingContext.builder()
        .user(userDetail)
        .typeData(typeFormData)
        .detailsData(detailsFormData)
        .build();

    when(mapper.toProviderRequestDetail(eq(mappingContext))).thenReturn(providerRequestDetail);
    when(soaApiClient.submitProviderRequest(any(), anyString(), anyString()))
        .thenThrow(new RuntimeException("API error"));

    final CaabApplicationException exception = assertThrows(CaabApplicationException.class, () ->
        providerRequestService.submitProviderRequest(typeFormData, detailsFormData, userDetail));

    assertEquals("Failed to submit provider request", exception.getMessage());
    verify(mapper).toProviderRequestDetail(any(ProviderRequestMappingContext.class));
    verify(soaApiClient).submitProviderRequest(providerRequestDetail, "testLoginId", "testUserType");
  }
}
