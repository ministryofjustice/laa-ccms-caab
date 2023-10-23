package uk.gov.laa.ccms.caab.builders;

import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.APP_TYPE_EMERGENCY;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.APP_TYPE_EMERGENCY_DEVOLVED_POWERS;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.APP_TYPE_EMERGENCY_DEVOLVED_POWERS_DISPLAY;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.APP_TYPE_EMERGENCY_DISPLAY;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.APP_TYPE_EXCEPTIONAL_CASE_FUNDING;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.APP_TYPE_EXCEPTIONAL_CASE_FUNDING_DISPLAY;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.APP_TYPE_SUBSTANTIVE;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.APP_TYPE_SUBSTANTIVE_DEVOLVED_POWERS;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.APP_TYPE_SUBSTANTIVE_DEVOLVED_POWERS_DISPLAY;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.APP_TYPE_SUBSTANTIVE_DISPLAY;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.ApplicationType;
import uk.gov.laa.ccms.caab.model.DevolvedPowers;

/**
 * Helper class for constructing an {@link ApplicationType} instance using a builder pattern.
 */
public class ApplicationTypeBuilder {

  private final ApplicationType applicationType;

  /**
   * Default builder method for the application type builder.
   */
  public ApplicationTypeBuilder() {
    this.applicationType = new ApplicationType();
  }

  /**
   * Builder method for application type.
   *
   * @param applicationTypeCategory the category selected for the appliocation type.
   * @param isDelegatedFunctions the boolean whether delegate functions used.
   * @return the builder with amended contract flag.
   */
  public ApplicationTypeBuilder applicationType(
      final String applicationTypeCategory,
      final boolean isDelegatedFunctions) {

    if (APP_TYPE_SUBSTANTIVE.equals(applicationTypeCategory)
        || APP_TYPE_SUBSTANTIVE_DEVOLVED_POWERS.equals(applicationTypeCategory)) {
      applicationType.setId(isDelegatedFunctions
          ? APP_TYPE_SUBSTANTIVE_DEVOLVED_POWERS : APP_TYPE_SUBSTANTIVE);
      applicationType.setDisplayValue(isDelegatedFunctions
          ? APP_TYPE_SUBSTANTIVE_DEVOLVED_POWERS_DISPLAY : APP_TYPE_SUBSTANTIVE_DISPLAY);
    } else if (APP_TYPE_EMERGENCY.equals(applicationTypeCategory)
        || APP_TYPE_EMERGENCY_DEVOLVED_POWERS.equals(applicationTypeCategory)) {
      applicationType.setId(isDelegatedFunctions
          ? APP_TYPE_EMERGENCY_DEVOLVED_POWERS : APP_TYPE_EMERGENCY);
      applicationType.setDisplayValue(isDelegatedFunctions
          ? APP_TYPE_EMERGENCY_DEVOLVED_POWERS_DISPLAY : APP_TYPE_EMERGENCY_DISPLAY);
    } else {
      applicationType.setId(APP_TYPE_EXCEPTIONAL_CASE_FUNDING);
      applicationType.setDisplayValue(APP_TYPE_EXCEPTIONAL_CASE_FUNDING_DISPLAY);
    }

    return this;
  }

  /**
   * Builder method for devolved powers.
   *
   * @param isDelegatedFunctions the boolean whether delegate functions used.
   * @param day the day when the delegate function was used.
   * @param month the month when the delegate function was used.
   * @param year the year when the delegate function was used.
   * @return the builder with amended contract flag.
   */
  public ApplicationTypeBuilder devolvedPowers(
      final boolean isDelegatedFunctions,
      final String day,
      final String month,
      final String year) throws ParseException {

    DevolvedPowers devolvedPowers = new DevolvedPowers();
    devolvedPowers.setUsed(isDelegatedFunctions);

    if (isDelegatedFunctions) {
      String dateString = day + "-" + month + "-" + year;
      SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
      devolvedPowers.setDateUsed(sdf.parse(dateString));
    }

    applicationType.setDevolvedPowers(devolvedPowers);
    return this;
  }

  /**
   * Builder method for devolved powers contract flag.
   *
   * @param contractFlag the applications devolved powers contract flag.
   * @return the builder with amended contract flag.
   */
  public ApplicationTypeBuilder devolvedPowersContractFlag(
      final String contractFlag) {
    if (applicationType.getDevolvedPowers() == null) {
      applicationType.setDevolvedPowers(new DevolvedPowers());
    }
    applicationType.getDevolvedPowers().setContractFlag(contractFlag);
    return this;
  }

  /**
   * Finalizes and returns the constructed ApplicationType instance.
   *
   * @return The constructed ApplicationType.
   */
  public ApplicationType build() {
    return applicationType;
  }
}
