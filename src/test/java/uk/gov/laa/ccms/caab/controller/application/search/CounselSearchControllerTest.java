package uk.gov.laa.ccms.caab.controller.application.search;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static uk.gov.laa.ccms.caab.constants.CounselLookupConstants.TOO_MANY_RESULTS;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.COUNSEL_SEARCH_CRITERIA;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.gov.laa.ccms.caab.bean.CounselSearchCriteria;
import uk.gov.laa.ccms.caab.client.EbsApiClientException;
import uk.gov.laa.ccms.data.model.CounselLookupDetail;
import uk.gov.laa.ccms.data.model.CounselLookupValueDetail;

// @WebMvcTest(CounselSearchController.class)

// @Import({CounselSearchValidator.class, CounselLookupMapper.class})
@DisplayName("Counsel Search Controller Test")
class CounselSearchControllerTest extends BaseCounselSearchControllerTest {

  @Nested
  class CounselLookupTestCases {

    @Test
    @DisplayName(
        "WHEN -> expected data found for criteria, THEN -> redirect to GET:/lookup/counsels.")
    void testCase1() throws Exception {

      CounselSearchCriteria criteria = new CounselSearchCriteria();
      criteria.setName("SHAUN S DODDS");

      List<CounselLookupValueDetail> valueDetails =
          List.of(
              new CounselLookupValueDetail()
                  .name("SHAUN S DODDS")
                  .company("SHAUN S DODDS")
                  .legalAidSupplierNumber("1099V")
                  .category("Junior")
                  .county(null));

      CounselLookupDetail counselLookupDetail = getCounselLookupDetail(valueDetails);

      when(service.getCounselSearch(eq(criteria))).thenReturn(counselLookupDetail);

      mockMvc
          .perform(
              post("/lookup/counsels")
                  .sessionAttr(COUNSEL_SEARCH_CRITERIA, criteria)
                  .param("page", "0")
                  .param("size", "10")
                  .param("sort", "name,asc"))
          .andExpect(status().is3xxRedirection()) // Expect redirection
          .andExpect(status().isFound()) // Status 302
          .andExpect(redirectedUrl("/lookup/counsels"));
    }

    @Test
    @DisplayName(
        "WHEN -> no rows found, THEN -> return view:application/counsel-search-no-results.")
    void testCase2() throws Exception {

      CounselSearchCriteria criteria = new CounselSearchCriteria();
      criteria.setName("SHAUN S DODDS");

      CounselLookupDetail counselLookupDetail = getCounselLookupDetail(List.of());

      when(service.getCounselSearch(eq(criteria))).thenReturn(counselLookupDetail);

      mockMvc
          .perform(
              post("/lookup/counsels")
                  .sessionAttr(COUNSEL_SEARCH_CRITERIA, criteria)
                  .param("page", "0")
                  .param("size", "10")
                  .param("sort", "name,asc"))
          .andExpect(status().isOk()) // SUCCESSFUL
          .andExpect(view().name("application/counsel-search-no-results"));
    }

    @Test
    @DisplayName(
        "WHEN -> rows more than 500 found, THEN -> return view:application/counsel-search-no-results.")
    void testCase3() throws Exception {

      CounselSearchCriteria criteria = new CounselSearchCriteria();
      criteria.setName("ASHU");

      when(service.getCounselSearch(any(CounselSearchCriteria.class)))
          .thenThrow(new EbsApiClientException(TOO_MANY_RESULTS));

      mockMvc
          .perform(
              post("/lookup/counsels")
                  .sessionAttr(COUNSEL_SEARCH_CRITERIA, criteria)
                  .param("page", "0")
                  .param("size", "10")
                  .param("sort", "name,asc"))
          .andExpect(status().isOk()) // SUCCESSFUL
          .andExpect(view().name("application/counsel-search-too-many-results"));
    }

    @Test
    @DisplayName("WHEN -> all params empty, THEN -> return view:application/counsel-search.")
    void testCase4() throws Exception {

      CounselSearchCriteria criteria = new CounselSearchCriteria();

      mockMvc
          .perform(
              post("/lookup/counsels")
                  .sessionAttr(COUNSEL_SEARCH_CRITERIA, criteria)
                  .param("page", "0")
                  .param("size", "10")
                  .param("sort", "name,asc"))
          .andExpect(status().isOk()) // SUCCESSFUL
          .andExpect(view().name("application/counsel-search"));
    }
  }
}
