package uk.gov.laa.ccms.caab.bean.proceeding;

import lombok.Data;
import org.springframework.util.StringUtils;

/** Form data for recording a proceeding outcome. */
@Data
public class ProceedingOutcomeFormData {

  private String dateOfFinalWork;

  private String stageEnd;

  private String resolutionMethod;

  private String result;

  private String resultInfo;

  private String alternativeResolution;

  private String adrInfo;

  private String courtCode;

  private String courtName;

  private String outcomeCourtCaseNo;

  private String widerBenefits;

  public boolean hasCourtSelection() {
    return StringUtils.hasText(normaliseCourtValue(courtCode))
        || StringUtils.hasText(normaliseCourtValue(courtName));
  }

  public void normaliseLegacyCourtFields() {
    courtCode = normaliseCourtValue(courtCode);
    courtName = normaliseCourtValue(courtName);
  }

  public static String normaliseCourtValue(final String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    final String trimmedValue = value.trim();
    return "Not Available".equalsIgnoreCase(trimmedValue) ? null : trimmedValue;
  }
}
