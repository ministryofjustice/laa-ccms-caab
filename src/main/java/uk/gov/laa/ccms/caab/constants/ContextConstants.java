package uk.gov.laa.ccms.caab.constants;

import lombok.Getter;

/**
 * Class that defines constants for different context values
 * used in submissions and amendments processes.
 *
 * @author Jamie Briggs
 */
@Getter
public final class ContextConstants {

  /*
   * Context param name
   */
  public static final String CONTEXT_NAME = "context";

  /*
   * Context name for new submissions
   */
  public static final String APPLICATION = "application";

  /*
   * Context name for amendments
   */
  public static final String AMENDMENTS = "amendments";
}
