package uk.gov.laa.ccms.caab.bean.validators.client;


import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.ClientFormDataAddressDetails;
import uk.gov.laa.ccms.caab.bean.validators.AbstractValidator;

/**
 * Abstract validator used for client address validation.
 */
public abstract class AbstractClientAddressValidator extends AbstractValidator {

  protected void validateNoFixedAbode(ClientFormDataAddressDetails addressDetails, Errors errors) {
    if (StringUtils.hasText(addressDetails.getCountry())
        || StringUtils.hasText(addressDetails.getHouseNameNumber())
        || StringUtils.hasText(addressDetails.getPostcode())
        || StringUtils.hasText(addressDetails.getAddressLine1())
        || StringUtils.hasText(addressDetails.getAddressLine2())
        || StringUtils.hasText(addressDetails.getCityTown())
        || StringUtils.hasText(addressDetails.getCounty())) {
      //if any field populated
      errors.reject("invalid.noFixedAbode",
          "You have indicated 'No Fixed Abode'. Please remove main address details or "
              + "select 'No' to amend your entry.");
    }
  }
}
