package uk.gov.laa.ccms.caab.model.user;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The response returned by the User Details API for a SiLAS identity.
 *
 * <p>The API also returns the user's responsibilities. They are not modelled here because this
 * application takes a user's permissions from their EBS functions rather than from SiLAS.
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserViewModel {

  /** The CCMS user mapped to the requested SiLAS identity. */
  private CcmsUserDetails ccmsUserDetails;
}
