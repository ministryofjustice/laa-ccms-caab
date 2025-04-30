package uk.gov.laa.ccms.caab.bean;

import static uk.gov.laa.ccms.caab.constants.CharacterLimitationConstants.SPECIAL_CONSIDERATIONS_CHARACTER_SIZE;

import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Represents the client monitoring details form.
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class ClientFormDataMonitoringDetails extends AbstractClientFormData {

  private String ethnicOrigin;
  private String disability;

  @Size(max = SPECIAL_CONSIDERATIONS_CHARACTER_SIZE)
  private String specialConsiderations;
}
