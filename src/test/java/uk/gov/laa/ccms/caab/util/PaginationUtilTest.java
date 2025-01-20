package uk.gov.laa.ccms.caab.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.data.model.CaseSummary;
import uk.gov.laa.ccms.data.model.Notification;

class PaginationUtilTest {

  @Test
  void testSortDescendingCorrectInPaginationUtil() {
    final Pageable pageable = PageRequest.of(0, 10, Sort.by("caseReferenceNumber").descending());
    final List<Notification> notificationList = new ArrayList<>();
    notificationList.add(new Notification().caseReferenceNumber("12345"));
    notificationList.add(new Notification().caseReferenceNumber("11234"));
    notificationList.sort((n1, n2) -> n2.getCaseReferenceNumber().compareTo(n1.getCaseReferenceNumber()));
    final Page<Notification> notificationPage = PaginationUtil.paginateList(pageable, notificationList);
    assertEquals(2, notificationPage.getTotalElements());
    assertEquals("12345", notificationPage.getContent().get(0).getCaseReferenceNumber());
  }

  @Test
  void testSortAscendingCorrectInPaginationUtil() {
    final Pageable pageable = PageRequest.of(0, 10, Sort.by("caseReferenceNumber").ascending());
    final List<Notification> notificationList = new ArrayList<>();
    notificationList.add(new Notification().caseReferenceNumber("12345"));
    notificationList.add(new Notification().caseReferenceNumber("11234"));
    notificationList.sort(Comparator.comparing(Notification::getCaseReferenceNumber));
    final Page<Notification> notificationPage = PaginationUtil.paginateList(pageable, notificationList);
    assertEquals(2, notificationPage.getTotalElements());
    assertEquals("11234", notificationPage.getContent().get(0).getCaseReferenceNumber());
  }

  @Test
  void testSortEmptyListInPaginationUtil() {
    final Pageable pageable = PageRequest.of(0, 10, Sort.by("caseReferenceNumber").ascending());
    final List<Notification> notificationList = new ArrayList<>();
    final Page<Notification> notificationPage = PaginationUtil.paginateList(pageable, notificationList);
    assertEquals(0, notificationPage.getTotalElements());
  }

  @Test
  void testInvalidSortThrowsException() {
    final Pageable pageable = PageRequest.of(0, 10, Sort.by("invalidProperty").ascending());
    final List<Notification> notificationList = new ArrayList<>();
    notificationList.add(new Notification().caseReferenceNumber("12345"));
    notificationList.add(new Notification().caseReferenceNumber("11234"));
    assertThrows(CaabApplicationException.class,
        () -> PaginationUtil.paginateList(pageable, notificationList));
  }

  @Test
  void testSortOnOtherDomainObjects() {
    final List<CaseSummary> summary = new ArrayList<>();
    summary.add(buildCaseSummary().feeEarnerName("bbb"));
    summary.add(buildCaseSummary().feeEarnerName("aaa"));
    summary.sort(Comparator.comparing(CaseSummary::getFeeEarnerName));
    final Pageable pageable = PageRequest.of(0, 10, Sort.by("feeEarnerName").ascending());
    final Page<CaseSummary> caseSummariesPage = PaginationUtil.paginateList(pageable, summary);
    assertEquals("aaa", caseSummariesPage.getContent().get(0).getFeeEarnerName());
  }

  private CaseSummary buildCaseSummary() {
    return new CaseSummary()
        .caseReferenceNumber("1234567890")
        .providerCaseReferenceNumber("ABCDEF")
        .caseStatusDisplay("APPL")
        .feeEarnerName("feeearner")
        .categoryOfLaw("CAT1");
  }
}

