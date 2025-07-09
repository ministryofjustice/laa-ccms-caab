package uk.gov.laa.ccms.caab.service.thymeleaf;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.laa.ccms.caab.constants.CaseContext;

@DisplayName("CaseContextService test")
class CaseContextServiceTest {

  CaseContextService caseContextService;

  @BeforeEach
  void beforeEach() {
    caseContextService = new CaseContextService();
  }

  @Test
  @DisplayName("Should provide return to case overview message when application")
  void shouldProvideReturnToCaseOverviewMessageWhenApplication() {
    // Given
    CaseContext caseContext = CaseContext.APPLICATION;
    // When
    String result = caseContextService.getGoBackText(caseContext);
    // Then
    assertThat(result).isEqualTo("site.cancelAndReturnToApplication");
  }

  @Test
  @DisplayName("Should provide return to case overview when amendment")
  void shouldProvideReturnToCaseOverviewWhenAmendment() {
    // Given
    CaseContext caseContext = CaseContext.AMENDMENTS;
    // When
    String result = caseContextService.getGoBackText(caseContext);
    // Then
    assertThat(result).isEqualTo("site.returnToCaseOverview");
  }
}
