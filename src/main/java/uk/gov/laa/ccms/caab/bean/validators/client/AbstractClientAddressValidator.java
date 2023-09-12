package uk.gov.laa.ccms.caab.bean.validators.client;

import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.ClientDetails;
import uk.gov.laa.ccms.caab.bean.validators.AbstractValidator;

public abstract class AbstractClientAddressValidator extends AbstractValidator {

  protected void validateNoFixedAbode(ClientDetails clientDetails, Errors errors) {
    if ((clientDetails.getCountry() != null && !clientDetails.getCountry().isEmpty())
        || (clientDetails.getHouseNameNumber() != null
        && !clientDetails.getHouseNameNumber().isEmpty())
        || (clientDetails.getPostcode() != null && !clientDetails.getPostcode().isEmpty())
        || (clientDetails.getAddressLine1() != null && !clientDetails.getAddressLine1().isEmpty())
        || (clientDetails.getAddressLine2() != null && !clientDetails.getAddressLine2().isEmpty())
        || (clientDetails.getCityTown() != null && !clientDetails.getCityTown().isEmpty())
        || (clientDetails.getCounty() != null && !clientDetails.getCounty().isEmpty())) {
      //if any field populated
      errors.reject("invalid.noFixedAbode",
          "You have indicated 'No Fixed Abode'. Please remove main address details or "
              + "uncheck box to amend your entry.");
    }
  }
}
