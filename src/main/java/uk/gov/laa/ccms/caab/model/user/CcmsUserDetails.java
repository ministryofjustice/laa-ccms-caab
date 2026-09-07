package uk.gov.laa.ccms.caab.model.user;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The CCMS user the User Details API holds against a SiLAS identity.
 *
 * <p>Only the login id is modelled. Everything else this application needs about a user - their
 * provider, firms and functions - is read from EBS once the login id is known, exactly as it is for
 * a user who was found by login id in the first place.
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CcmsUserDetails {

  /** The CCMS username. */
  private String userLoginId;
}
