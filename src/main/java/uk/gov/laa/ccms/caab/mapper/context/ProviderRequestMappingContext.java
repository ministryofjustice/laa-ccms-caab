package uk.gov.laa.ccms.caab.mapper.context;

import lombok.Builder;
import lombok.Data;
import uk.gov.laa.ccms.caab.bean.request.ProviderRequestDetailsFormData;
import uk.gov.laa.ccms.caab.bean.request.ProviderRequestTypeFormData;
import uk.gov.laa.ccms.data.model.UserDetail;

/**
 * Context for mapping provider request details.
 *
 * <p>Contains user details and form data required for creating a provider request.</p>
 */
@Builder
@Data
public class ProviderRequestMappingContext {

  /**
   * The user details associated with the provider request.
   */
  UserDetail user;

  /**
   * The form data containing the type of the provider request.
   */
  ProviderRequestTypeFormData typeData;

  /**
   * The form data containing additional details of the provider request.
   */
  ProviderRequestDetailsFormData detailsData;

}

