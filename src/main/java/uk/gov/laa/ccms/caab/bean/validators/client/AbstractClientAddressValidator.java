package uk.gov.laa.ccms.caab.bean.validators.client;


import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.ClientDetails;
import uk.gov.laa.ccms.caab.bean.validators.AbstractValidator;

/**
 * Abstract validator used for client address validation.
 */
public abstract class AbstractClientAddressValidator extends AbstractValidator {

  protected void validateNoFixedAbode(ClientDetails clientDetails, Errors errors) {
    if (StringUtils.hasText(clientDetails.getCountry())
        || StringUtils.hasText(clientDetails.getHouseNameNumber())
        || StringUtils.hasText(clientDetails.getPostcode())
        || StringUtils.hasText(clientDetails.getAddressLine1())
        || StringUtils.hasText(clientDetails.getAddressLine2())
        || StringUtils.hasText(clientDetails.getCityTown())
        || StringUtils.hasText(clientDetails.getCounty())) {
      //if any field populated
      errors.reject("invalid.noFixedAbode",
          "You have indicated 'No Fixed Abode'. Please remove main address details or "
              + "uncheck box to amend your entry.");
    }
  }
}
