package uk.gov.laa.ccms.caab.constants;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Constants for submission types used on the submission screens.
 */
@Component
@Data
public class SubmissionConstants {

  /**
   * submission used for create client.
   */
  public static final String SUBMISSION_CREATE_CLIENT = "client-create";

  /**
   * submission used for edit client.
   */
  public static final String SUBMISSION_UPDATE_CLIENT = "client-update";

  /**
   * the maximum amount of poll request for a submission.
   */
  @Value("${submission.max-poll-count:6}")
  private Integer maxPollCount;


}
