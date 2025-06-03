package uk.gov.laa.ccms.caab.service.thymeleaf;

import org.springframework.stereotype.Service;
import uk.gov.laa.ccms.caab.constants.CaseContext;

/**
 * Service responsible for generating navigation-related text based on the context
 * of the case.
 *
 * @author Jamie Briggs
 */
@Service
public class CaseContextService {

  /**
   * Returns the text value indicating where to navigate based on the provided case context.
   *
   * @param caseContext the context of the case, e.g., "amendments" or "application"
   * @return a string representing the navigation text; returns "site.returnToCaseOverview" if the
   *         context is "amendments", otherwise returns "site.cancelAndReturnToApplication"
   */
  public String getGoBackText(CaseContext caseContext) {
    return switch (caseContext) {
      case APPLICATION -> "site.cancelAndReturnToApplication";
      case AMENDMENTS -> "site.returnToCaseOverview";
    };
  }
}
