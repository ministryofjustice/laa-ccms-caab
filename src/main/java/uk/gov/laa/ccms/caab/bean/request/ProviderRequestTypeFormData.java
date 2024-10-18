package uk.gov.laa.ccms.caab.bean.request;

import lombok.Data;

/**
 * Represents form data for provider request type details.
 */
@Data
public class ProviderRequestTypeFormData {

  /**
   * The type of the provider request.
   */
  private String providerRequestType;

  /**
   * The display value of the provider request type.
   */
  private String providerRequestTypeDisplayValue;
}
