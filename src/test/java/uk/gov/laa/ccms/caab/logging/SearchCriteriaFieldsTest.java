package uk.gov.laa.ccms.caab.logging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import uk.gov.laa.ccms.caab.bean.CaseSearchCriteria;
import uk.gov.laa.ccms.caab.bean.NotificationSearchCriteria;

class SearchCriteriaFieldsTest {

  @Test
  void listsOnlyTheNamesOfThePopulatedFieldsInOrder() {
    var criteria = new CaseSearchCriteria();
    criteria.setClientSurname("Smith");
    criteria.setCaseReference("300001");
    criteria.setStatus("   ");

    assertThat(SearchCriteriaFields.populated(criteria))
        .containsExactly("caseReference", "clientSurname");
  }

  @Test
  void neverIncludesAValue() {
    var criteria = new CaseSearchCriteria();
    criteria.setClientSurname("Smith");

    assertThat(String.join(",", SearchCriteriaFields.populated(criteria))).doesNotContain("Smith");
  }

  @Test
  void aFalseFlagAndPagingAreNotCountedAsCriteria() {
    var criteria = new NotificationSearchCriteria();
    criteria.setIncludeClosed(false);
    criteria.setPage(0);
    criteria.setSize(10);
    criteria.setSort("date,desc");
    criteria.setNotificationType("OPEN");
    criteria.setIncludeClosed(true);

    assertThat(SearchCriteriaFields.populated(criteria))
        .containsExactly("includeClosed", "notificationType");
  }

  @Test
  void nothingPopulatedGivesAnEmptyList() {
    assertThat(SearchCriteriaFields.populated(new CaseSearchCriteria())).isEmpty();
    assertThat(SearchCriteriaFields.populated(null)).isEmpty();
  }

  @Test
  void aDerivedGetterThatThrowsIsNeverCalled() {
    var criteria = new uk.gov.laa.ccms.caab.bean.ClientSearchCriteria();
    criteria.setSurname("Smith");

    assertThat(SearchCriteriaFields.populated(criteria)).containsExactly("surname");
  }
}
