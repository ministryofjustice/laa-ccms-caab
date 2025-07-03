package uk.gov.laa.ccms.caab.bean.common;

import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.util.DateUtils;

/** Represents the common attributes for an individual. */
public interface Individual {

  /**
   * Get the individual's date of birth.
   *
   * @return the individual's date of birth.
   */
  String getDateOfBirth();

  /**
   * Retrieves the formatted date of birth.
   *
   * @return The formatted date of birth (yyyy-MM-dd).
   */
  default String getFormattedDateOfBirth() {
    try {
      return DateUtils.convertToDateString(getDateOfBirth());
    } catch (Exception e) {
      throw new CaabApplicationException("Unable to format date of birth", e);
    }
  }
}
