package uk.gov.laa.ccms.caab.bean.proceeding;

import lombok.Data;

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

  private String outcomeCourtCaseNo;

  private String widerBenefits;
}
