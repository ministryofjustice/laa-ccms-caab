package uk.gov.laa.ccms.caab.controller.application.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static uk.gov.laa.ccms.caab.constants.CounselLookupConstants.TOO_MANY_RESULTS;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.COST_ALLOCATION_FORM_DATA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.COUNSEL_SEARCH_CRITERIA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.COUNSEL_SEARCH_RESULTS;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.gov.laa.ccms.caab.bean.CounselSearchCriteria;
import uk.gov.laa.ccms.caab.bean.costs.AllocateCostsFormData;
import uk.gov.laa.ccms.caab.client.EbsApiClientException;
import uk.gov.laa.ccms.caab.model.CostEntryDetail;
import uk.gov.laa.ccms.data.model.CounselLookupDetail;
import uk.gov.laa.ccms.data.model.CounselLookupValueDetail;

@DisplayName("Counsel Search Controller Test")
class CounselSearchControllerTest extends BaseCounselSearchControllerTest {

  @Nested
  class SelectCounselTestCases {

    @Test
    @DisplayName("WHEN -> counsel is selected, THEN -> it is added to the session and redirects.")
    void testSelectCounsel() throws Exception {
      List<CounselLookupValueDetail> valueDetails =
          List.of(
              new CounselLookupValueDetail()
                  .name("SHAUN S DODDS")
                  .company("SHAUN S DODDS")
                  .legalAidSupplierNumber("1099V")
                  .category("Junior")
                  .county(null));

      AllocateCostsFormData formData = new AllocateCostsFormData();
      formData.setCostEntries(new ArrayList<>());

      mockMvc
          .perform(
              get("/counsel/select")
                  .param("index", "0")
                  .sessionAttr(COUNSEL_SEARCH_RESULTS, valueDetails)
                  .sessionAttr(COST_ALLOCATION_FORM_DATA, formData))
          .andExpect(status().is3xxRedirection())
          .andExpect(redirectedUrl("/allocate-cost-limit"));

      assertNotNull(formData.getCostEntries());
      assertEquals(1, formData.getCostEntries().size());
      CostEntryDetail entry = formData.getCostEntries().get(0);
      assertEquals("SHAUN S DODDS", entry.getResourceName());
      assertEquals(BigDecimal.ZERO, entry.getAmountBilled());
      assertEquals(BigDecimal.ZERO, entry.getRequestedCosts());
    }
  }

  @Nested
  class CounselLookupTestCases {

    @Test
    @DisplayName(
        "WHEN -> expected data found for criteria, THEN -> redirect to GET:/counsel/results.")
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
              post("/counsel/search")
                  .sessionAttr(COUNSEL_SEARCH_CRITERIA, criteria)
                  .param("page", "0")
                  .param("size", "10")
                  .param("sort", "name,asc"))
          .andExpect(status().is3xxRedirection()) // Expect redirection
          .andExpect(status().isFound()) // Status 302
          .andExpect(redirectedUrl("/counsel/results"));
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
              post("/counsel/search")
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
              post("/counsel/search")
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
              post("/counsel/search")
                  .sessionAttr(COUNSEL_SEARCH_CRITERIA, criteria)
                  .param("page", "0")
                  .param("size", "10")
                  .param("sort", "name,asc"))
          .andExpect(status().isOk()) // SUCCESSFUL
          .andExpect(view().name("application/counsel-search"));
    }
  }
}
