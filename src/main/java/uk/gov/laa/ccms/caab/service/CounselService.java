package uk.gov.laa.ccms.caab.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.laa.ccms.caab.client.EbsApiClient;

/** Service class to handle counsel related methods. */
@Service
@RequiredArgsConstructor
@Slf4j
public class CounselService {

  private final EbsApiClient ebsApiClient;
}
