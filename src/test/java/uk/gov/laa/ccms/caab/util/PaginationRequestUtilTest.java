package uk.gov.laa.ccms.caab.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class PaginationRequestUtilTest {

  @Test
  void resolve_usesRequestParamsWhenProvided() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addParameter(PaginationRequestUtil.PAGE_PARAM, "1");
    request.addParameter(PaginationRequestUtil.SIZE_PARAM, "20");
    request.addParameter(PaginationRequestUtil.SORT_PARAM, "caseReference,desc");

    PaginationRequest result =
        PaginationRequestUtil.resolve(
            request, 1, 20, "caseReference,desc", 0, 10, "caseReference,desc", "dateAssigned,asc");

    assertEquals(1, result.page());
    assertEquals(20, result.size());
    assertEquals("caseReference,desc", result.sort());
    assertTrue(result.isNewPageRequest());
    assertFalse(result.isNewSort());
    assertTrue(result.isNewPage());
    assertTrue(result.isNewSize());
  }

  @Test
  void resolve_defaultsSortWhenParamBlank() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addParameter(PaginationRequestUtil.SORT_PARAM, "");

    PaginationRequest result =
        PaginationRequestUtil.resolve(
            request, null, null, "", 0, 10, "otherField,asc", "dateAssigned,asc");

    assertEquals("dateAssigned,asc", result.sort());
    assertTrue(result.isNewSort());
    assertFalse(result.isNewPage());
  }

  @Test
  void resolve_usesOriginalSortWhenNoParam() {
    MockHttpServletRequest request = new MockHttpServletRequest();

    PaginationRequest result =
        PaginationRequestUtil.resolve(
            request, null, null, null, 2, 10, "caseReference,desc", "dateAssigned,asc");

    assertEquals("caseReference,desc", result.sort());
    assertFalse(result.isNewPageRequest());
    assertFalse(result.isNewSort());
    assertFalse(result.isNewPage());
    assertFalse(result.isNewSize());
  }
}
