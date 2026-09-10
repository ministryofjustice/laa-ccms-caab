package uk.gov.laa.ccms.caab.bean;

import lombok.Data;

/** Holds the selected award-type details. */
@Data
public class AwardTypeForm {

  /** Lookup code, for example FIN_ASSET. */
  private String awardTypeCode;

  /** Display description, for example Financial Asset. */
  private String description;

  /** General award category, for example ASSET, COST, DAMAGE, or LAND. */
  private String awardType;
}
