package uk.gov.laa.ccms.caab.model.user;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The CCMS user EBS holds against an EntraID email address, from the {@code XXCCMS_ENTRA_ID_USERS}
 * mapping table.
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class EntraUserMapping {

  /** The EntraID email address the mapping was looked up by. */
  @JsonProperty("entra_email_address")
  private String entraEmailAddress;

  /** The CCMS username that email address maps to. */
  @JsonProperty("ccms_login_id")
  private String ccmsLoginId;
}
