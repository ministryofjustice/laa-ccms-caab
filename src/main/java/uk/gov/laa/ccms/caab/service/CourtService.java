package uk.gov.laa.ccms.caab.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.laa.ccms.caab.bean.CourtSearchCriteria;
import uk.gov.laa.ccms.caab.client.EbsApiClient;
import uk.gov.laa.ccms.caab.util.ReflectionUtils;

/** Service class to handle court related methods. */
@Service
@RequiredArgsConstructor
@Slf4j
public class CourtService {

  private final EbsApiClient ebsApiClient;

  public @org.jspecify.annotations.Nullable CourtLookupDetail getCourtSearch(
      CourtSearchCriteria criteria) {
    ReflectionUtils.nullifyStrings(criteria);
    return ebsApiClient.getCourtDetails(criteria).block();
  }
}
