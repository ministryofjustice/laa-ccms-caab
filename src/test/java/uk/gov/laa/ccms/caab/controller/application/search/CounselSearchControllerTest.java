package uk.gov.laa.ccms.caab.controller.application.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static uk.gov.laa.ccms.caab.constants.CounselLookupConstants.TOO_MANY_RESULTS;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.ACTIVE_CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.COST_ALLOCATION_FORM_DATA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.COUNSEL_SEARCH_CRITERIA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.COUNSEL_SEARCH_RESULTS;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.SELECTED_COUNSEL;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.laa.ccms.caab.bean.CounselSearchCriteria;
import uk.gov.laa.ccms.caab.bean.costs.AllocateCostsFormData;
import uk.gov.laa.ccms.caab.client.EbsApiClientException;
import uk.gov.laa.ccms.caab.model.CostEntryDetail;
import uk.gov.laa.ccms.data.model.CounselLookupDetail;
import uk.gov.laa.ccms.data.model.CounselLookupValueDetail;

@DisplayName("Counsel Search Controller Test")
class CounselSearchControllerTest extends BaseCounselSearchControllerTest {

  @Test
  @DisplayName("WHEN -> counsel is selected, THEN -> it is added to the session and redirects.")
  void testSelectCounsel() throws Exception {
    List<CounselLookupValueDetail> valueDetails =
        List.of(
            new CounselLookupValueDetail()
                .name("TEST COUNSEL XYZ")
                .company("TEST COUNSEL XYZ")
                .legalAidSupplierNumber("1001T")
                .category("TestCategory")
                .county(null));

    mockMvc
        .perform(
            get("/counsel/select")
                .param("index", "0")
                .sessionAttr(COUNSEL_SEARCH_RESULTS, valueDetails))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/application/counsel/confirm"))
        .andExpect(
            result ->
                assertEquals(
                    valueDetails.get(0),
                    result.getRequest().getSession().getAttribute(SELECTED_COUNSEL)));
  }

  @Test
  @DisplayName(
      "WHEN -> expected data found for criteria, THEN -> redirect to GET:/counsel/results.")
  void shouldRedirectToResultsWhenCounselFound() throws Exception {

    CounselSearchCriteria criteria = new CounselSearchCriteria();
    criteria.setName("TEST COUNSEL XYZ");

    List<CounselLookupValueDetail> valueDetails =
        List.of(
            new CounselLookupValueDetail()
                .name("TEST COUNSEL XYZ")
                .company("TEST COUNSEL XYZ")
                .legalAidSupplierNumber("1001T")
                .category("TestCategory")
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
  @DisplayName("WHEN -> no rows found, THEN -> return view:application/counsel-search-no-results.")
  void shouldReturnNoResultsViewWhenSearchEmpty() throws Exception {

    CounselSearchCriteria criteria = new CounselSearchCriteria();
    criteria.setName("TEST COUNSEL XYZ");

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
      "WHEN -> rows more than 500 found, THEN -> return view:application/counsel-search-too-many-results.")
  void shouldReturnTooManyResultsViewWhenSearchTooLarge() throws Exception {

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
  void shouldReturnSearchViewWhenCriteriaEmpty() throws Exception {

    CounselSearchCriteria criteria = new CounselSearchCriteria();

    mockMvc
        .perform(
            post("/counsel/search")
                .sessionAttr(COUNSEL_SEARCH_CRITERIA, criteria)
                .param("page", "0")
                .param("size", "10")
                .param("sort", "name,asc"))
        .andExpect(status().isOk()) // SUCCESSFUL
        .andExpect(view().name("application/counsel-search"))
        .andExpect(
            model()
                .attributeHasFieldErrorCode(
                    COUNSEL_SEARCH_CRITERIA, "name", "at.least.one.search.required"));
  }

  @Test
  @DisplayName("WHEN -> name is too short, THEN -> return error.")
  void shouldReturnErrorWhenNameTooShort() throws Exception {
    CounselSearchCriteria criteria = new CounselSearchCriteria();
    criteria.setName("Jo");

    mockMvc
        .perform(
            post("/counsel/search")
                .sessionAttr(COUNSEL_SEARCH_CRITERIA, criteria))
        .andExpect(status().isOk())
        .andExpect(view().name("application/counsel-search"))
        .andExpect(
            model()
                .attributeHasFieldErrorCode(
                    COUNSEL_SEARCH_CRITERIA, "name", "length.below.min"));
  }

  @Test
  @DisplayName("WHEN -> company is too short, THEN -> return error.")
  void shouldReturnErrorWhenCompanyTooShort() throws Exception {
    CounselSearchCriteria criteria = new CounselSearchCriteria();
    criteria.setCompany("AC");

    mockMvc
        .perform(
            post("/counsel/search")
                .sessionAttr(COUNSEL_SEARCH_CRITERIA, criteria))
        .andExpect(status().isOk())
        .andExpect(view().name("application/counsel-search"))
        .andExpect(
            model()
                .attributeHasFieldErrorCode(
                    COUNSEL_SEARCH_CRITERIA, "company", "length.below.min"));
  }

  @Test
  @DisplayName(
      "WHEN -> counsel confirmation is requested, THEN -> the confirmation view is returned.")
  void testConfirmCounselGet() throws Exception {
    CounselLookupValueDetail selectedCounsel =
        new CounselLookupValueDetail()
            .name("SHAUN S DODDS")
            .company("SHAUN S DODDS")
            .legalAidSupplierNumber("1099V")
            .category("Junior");

    mockMvc
        .perform(get("/application/counsel/confirm").sessionAttr(SELECTED_COUNSEL, selectedCounsel))
        .andExpect(status().isOk())
        .andExpect(view().name("application/counsel-confirm"));
  }

  @Test
  @DisplayName("WHEN -> counsel is confirmed, THEN -> it is added to cost entries and redirects.")
  void testConfirmCounselPost() throws Exception {
    CounselLookupValueDetail selectedCounsel =
        new CounselLookupValueDetail()
            .name("SHAUN S DODDS")
            .company("SHAUN S DODDS")
            .legalAidSupplierNumber("1099V")
            .category("Junior");

    AllocateCostsFormData formData = new AllocateCostsFormData();
    formData.setCostEntries(new ArrayList<>());

    mockMvc
        .perform(
            post("/application/counsel/confirm")
                .sessionAttr(SELECTED_COUNSEL, selectedCounsel)
                .sessionAttr(COST_ALLOCATION_FORM_DATA, formData))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/allocate-cost-limit"));

    assertEquals(1, formData.getCostEntries().size());
    CostEntryDetail entry = formData.getCostEntries().get(0);
    assertEquals("SHAUN S DODDS", entry.getResourceName());
    assertEquals("1099V", entry.getLscResourceId());
    assertEquals("counsel", entry.getCostCategory());
  }

  @Test
  @DisplayName(
      "WHEN -> counsel is confirmed but form data is missing, THEN -> redirect to allocate costs.")
  void testConfirmCounselPostMissingFormData() throws Exception {
    CounselLookupValueDetail selectedCounsel =
        new CounselLookupValueDetail()
            .name("SHAUN S DODDS")
            .legalAidSupplierNumber("1099V")
            .category("Junior");

    mockMvc
        .perform(
            post("/application/counsel/confirm").sessionAttr(SELECTED_COUNSEL, selectedCounsel))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/allocate-cost-limit"));
  }

  @Test
  @DisplayName(
      "WHEN -> counsel is confirmed but already exists, THEN -> redirect to confirmation with error.")
  void testConfirmCounselPostDuplicate() throws Exception {
    CounselLookupValueDetail selectedCounsel =
        new CounselLookupValueDetail()
            .name("SHAUN S DODDS")
            .company("SHAUN S DODDS")
            .legalAidSupplierNumber("1099V")
            .category("Junior");

    AllocateCostsFormData formData = new AllocateCostsFormData();
    List<CostEntryDetail> entries = new ArrayList<>();
    CostEntryDetail existingEntry = new CostEntryDetail();
    existingEntry.setResourceName("SHAUN S DODDS");
    existingEntry.setLscResourceId("1099V");
    entries.add(existingEntry);
    formData.setCostEntries(entries);

    mockMvc
        .perform(
            post("/application/counsel/confirm")
                .sessionAttr(SELECTED_COUNSEL, selectedCounsel)
                .sessionAttr(COST_ALLOCATION_FORM_DATA, formData))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/application/counsel/confirm?error=duplicate"));

    // Size should still be 1
    assertEquals(1, formData.getCostEntries().size());
  }

  @Test
  @DisplayName(
      "WHEN -> counsel is confirmed but already exists (different case), THEN -> redirect to confirmation with error.")
  void testConfirmCounselPostDuplicateCaseInsensitive() throws Exception {
    CounselLookupValueDetail selectedCounsel =
        new CounselLookupValueDetail()
            .name("shaun s dodds")
            .legalAidSupplierNumber("1099V")
            .category("Junior");

    AllocateCostsFormData formData = new AllocateCostsFormData();
    List<CostEntryDetail> entries = new ArrayList<>();
    CostEntryDetail existingEntry = new CostEntryDetail();
    existingEntry.setResourceName("SHAUN S DODDS");
    existingEntry.setLscResourceId("1099V");
    entries.add(existingEntry);
    formData.setCostEntries(entries);

    mockMvc
        .perform(
            post("/application/counsel/confirm")
                .sessionAttr(SELECTED_COUNSEL, selectedCounsel)
                .sessionAttr(COST_ALLOCATION_FORM_DATA, formData))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/application/counsel/confirm?error=duplicate"));

    // Size should still be 1
    assertEquals(1, formData.getCostEntries().size());
  }

  @Test
  @DisplayName(
      "WHEN -> counsel is confirmed but already exists by reference only, THEN -> redirect to confirmation with error.")
  void testConfirmCounselPostDuplicateByReference() throws Exception {
    CounselLookupValueDetail selectedCounsel =
        new CounselLookupValueDetail()
            .name("DIFFERENT NAME")
            .legalAidSupplierNumber("1099V")
            .category("Junior");

    AllocateCostsFormData formData = new AllocateCostsFormData();
    List<CostEntryDetail> entries = new ArrayList<>();
    CostEntryDetail existingEntry = new CostEntryDetail();
    existingEntry.setResourceName("SHAUN S DODDS");
    existingEntry.setLscResourceId("1099V");
    entries.add(existingEntry);
    formData.setCostEntries(entries);

    mockMvc
        .perform(
            post("/application/counsel/confirm")
                .sessionAttr(SELECTED_COUNSEL, selectedCounsel)
                .sessionAttr(COST_ALLOCATION_FORM_DATA, formData))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/application/counsel/confirm?error=duplicate"));

    // Size should still be 1
    assertEquals(1, formData.getCostEntries().size());
  }

  @Test
  @DisplayName(
      "WHEN -> counsel is confirmed but already exists by name only, THEN -> redirect to confirmation with error.")
  void testConfirmCounselPostDuplicateByNameOnly() throws Exception {
    CounselLookupValueDetail selectedCounsel =
        new CounselLookupValueDetail()
            .name("SHAUN S DODDS")
            .legalAidSupplierNumber("NEW_REF")
            .category("Junior");

    AllocateCostsFormData formData = new AllocateCostsFormData();
    List<CostEntryDetail> entries = new ArrayList<>();
    CostEntryDetail existingEntry = new CostEntryDetail();
    existingEntry.setResourceName("SHAUN S DODDS");
    existingEntry.setLscResourceId("1099V");
    entries.add(existingEntry);
    formData.setCostEntries(entries);

    mockMvc
        .perform(
            post("/application/counsel/confirm")
                .sessionAttr(SELECTED_COUNSEL, selectedCounsel)
                .sessionAttr(COST_ALLOCATION_FORM_DATA, formData))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/application/counsel/confirm?error=duplicate"));

    // Size should still be 1
    assertEquals(1, formData.getCostEntries().size());
  }

  @Test
  @DisplayName(
      "WHEN -> counsel is confirmed but already exists in original case costs, THEN -> redirect to confirmation with error.")
  void testConfirmCounselPostDuplicateExistingInCase() throws Exception {
    CounselLookupValueDetail selectedCounsel =
        new CounselLookupValueDetail()
            .name("SHAUN S DODDS")
            .legalAidSupplierNumber("1099V")
            .category("Junior");

    AllocateCostsFormData formData = new AllocateCostsFormData();
    List<CostEntryDetail> entries = new ArrayList<>();
    
    // Simulating an existing counsel from EBS case costs
    CostEntryDetail existingEntry = new CostEntryDetail();
    existingEntry.setResourceName("SHAUN S DODDS");
    existingEntry.setLscResourceId("1099V");
    existingEntry.setNewEntry(false); // Not a new entry
    entries.add(existingEntry);
    
    formData.setCostEntries(entries);

    mockMvc
        .perform(
            post("/application/counsel/confirm")
                .sessionAttr(SELECTED_COUNSEL, selectedCounsel)
                .sessionAttr(COST_ALLOCATION_FORM_DATA, formData))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/application/counsel/confirm?error=duplicate"));

    // Size should still be 1
    assertEquals(1, formData.getCostEntries().size());
  }

  @Test
  @DisplayName("WHEN -> counsel results are requested, THEN -> the current URL is correctly set and the results view is returned.")
  void testCounselLookupGet() throws Exception {
    List<CounselLookupValueDetail> valueDetails = List.of(
        new CounselLookupValueDetail().name("COUNSEL 1"),
        new CounselLookupValueDetail().name("COUNSEL 2")
    );

    mockMvc.perform(get("/counsel/results")
            .sessionAttr(COUNSEL_SEARCH_RESULTS, valueDetails)
            .param("page", "0")
            .param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(view().name("application/counsel-search-results"))
        .andExpect(model().attribute("currentUrl", "/counsel/results"))
        .andExpect(model().attributeExists("counselResultsPage"));
  }

  @Test
  @DisplayName("WHEN -> API call fails with generic error, THEN -> return to search with error.")
  void shouldReturnSearchViewWhenApiError() throws Exception {

    CounselSearchCriteria criteria = new CounselSearchCriteria();
    criteria.setName("ERROR");

    when(service.getCounselSearch(any(CounselSearchCriteria.class)))
        .thenThrow(new EbsApiClientException("Some API Error"));

    mockMvc
        .perform(
            post("/counsel/search")
                .sessionAttr(COUNSEL_SEARCH_CRITERIA, criteria)
                .param("page", "0")
                .param("size", "10")
                .param("sort", "name,asc"))
        .andExpect(status().isOk())
        .andExpect(view().name("application/counsel-search"))
        .andExpect(model().hasErrors())
        .andExpect(model().attributeHasErrors(COUNSEL_SEARCH_CRITERIA));
  }
}
