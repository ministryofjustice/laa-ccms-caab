package uk.gov.laa.ccms.caab.model.assessment;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * Represents the display details of an assessment summary entity.
 */
@Data
public class AssessmentSummaryEntityDisplay {
  /**
   * The name of the assessment summary entity.
   */
  private String name;

  /**
   * The display name of the assessment summary entity.
   */
  private String displayName;

  /**
   * The level of the assessment summary entity.
   */
  private Integer entityLevel;

  /**
   * The list of attributes for the assessment summary entity.
   */
  private List<AssessmentSummaryAttributeDisplay> attributes;

  /**
   * Constructs an instance of AssessmentSummaryEntityDisplay.
   *
   * @param name the name of the entity
   * @param displayName the display name of the entity
   * @param entityLevel the level of the entity
   */
  public AssessmentSummaryEntityDisplay(
      final String name,
      final String displayName,
      final Integer entityLevel) {
    this.name = name;
    this.displayName = displayName;
    this.entityLevel = entityLevel;
    this.attributes = new ArrayList<>();
  }
}
