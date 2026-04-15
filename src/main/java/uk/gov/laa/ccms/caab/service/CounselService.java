package uk.gov.laa.ccms.caab.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.laa.ccms.caab.bean.CounselSearchCriteria;
import uk.gov.laa.ccms.caab.client.EbsApiClient;
import uk.gov.laa.ccms.caab.util.ReflectionUtils;
import uk.gov.laa.ccms.data.model.CounselLookupDetail;

/** Service class to handle counsel related methods. */
@Service
@RequiredArgsConstructor
@Slf4j
public class CounselService {

  private final EbsApiClient ebsApiClient;

  /**
   * Service method to pull counsel details for the search criteria specified.
   *
   * @param criteria counsel search criteria.
   * @return CounselLookupDetail Counsel details returned.
   */
  public @org.jspecify.annotations.Nullable CounselLookupDetail getCounselSearch(
      CounselSearchCriteria criteria) {

    ReflectionUtils.nullifyStrings(criteria);
    return ebsApiClient.getCounselDetails(criteria).block();
  }
}
