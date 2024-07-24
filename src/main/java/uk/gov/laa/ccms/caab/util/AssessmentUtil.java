package uk.gov.laa.ccms.caab.util;

import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentAttribute.APPLICATION_CASE_REF;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentEntityType.GLOBAL;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentAttributeDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentEntityDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentEntityTypeDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentRelationshipDetail;
import uk.gov.laa.ccms.caab.constants.assessment.AssessmentAttribute;
import uk.gov.laa.ccms.caab.constants.assessment.AssessmentEntityType;
import uk.gov.laa.ccms.caab.constants.assessment.AssessmentName;
import uk.gov.laa.ccms.caab.constants.assessment.AssessmentRelationship;
import uk.gov.laa.ccms.caab.constants.assessment.AssessmentRulebase;

/**
 * Utility class for handling assessment-related operations.
 */
@Slf4j
public class AssessmentUtil {

  /**
   * Retrieves the most recent assessment detail from a list of assessments based on the
   * last saved date within their audit details.
   *
   * @param assessments the list of assessment details
   * @return the most recent assessment detail, or null if the list is empty or null
   */
  public static AssessmentDetail getMostRecentAssessmentDetail(
      final List<AssessmentDetail> assessments) {
    return assessments != null ? assessments.stream()
        .max(Comparator.comparing(assessment -> assessment.getAuditDetail().getLastSaved(),
            Comparator.nullsFirst(Comparator.naturalOrder())))
        .orElse(null) : null;
  }

  /**
   * Retrieves the specific type of assessment entity from the assessment details.
   *
   * @param assessment the assessment containing entity types
   * @param entityType the entity type to retrieve
   * @return the matching assessment entity type detail, or null if not found
   */
  public static AssessmentEntityTypeDetail getAssessmentEntityType(
      final AssessmentDetail assessment,
      final AssessmentEntityType entityType) {

    return assessment != null ? assessment.getEntityTypes().stream()
        .filter(entityTypeDetail -> entityTypeDetail.getName()
            .equalsIgnoreCase(entityType.getType()))
        .findFirst()
        .orElse(null) : null;
  }

  /**
   * Retrieves a list of assessment entities for the specified entity type.
   *
   * @param assessment the assessment detail object
   * @param entityType the type of the assessment entity
   * @return a list of assessment entity details, or an empty list if none found
   */
  public static List<AssessmentEntityDetail> getAssessmentEntitiesForEntityType(
      final AssessmentDetail assessment,
      final AssessmentEntityType entityType) {

    return Optional.ofNullable(getAssessmentEntityType(assessment, entityType))
        .map(AssessmentEntityTypeDetail::getEntities)
        .orElse(new ArrayList<>());
  }


  /**
   * Retrieves the relationship detail for the specified assessment entity and relationship.
   *
   * @param assessmentEntity the assessment entity detail object
   * @param relationship the relationship to find
   * @return the assessment relationship detail, or null if not found
   */
  public static AssessmentRelationshipDetail getEntityRelationship(
      final AssessmentEntityDetail assessmentEntity,
      final AssessmentRelationship relationship) {

    return assessmentEntity.getRelations().stream()
        .filter(relation -> relation.getName()
            .equalsIgnoreCase(relationship.getRelationship()))
        .findFirst()
        .orElse(null);
  }

  /**
   * Retrieves a specific assessment entity from the given assessment entity type.
   *
   * @param assessmentEntityType the assessment entity type containing entities
   * @param entityName the name of the entity to retrieve
   * @return the matching assessment entity detail, or null if not found
   */
  public static AssessmentEntityDetail getAssessmentEntity(
      final AssessmentEntityTypeDetail assessmentEntityType,
      final String entityName) {

    return assessmentEntityType.getEntities().stream()
        .filter(entity -> entity.getName().equals(entityName))
        .findFirst()
        .orElse(null);
  }

  /**
   * Retrieves a specific assessment attribute from the given assessment entity.
   *
   * @param assessmentEntity the assessment entity containing attributes
   * @param attribute the attribute to retrieve
   * @return the matching assessment attribute detail, or null if not found
   */
  public static AssessmentAttributeDetail getAssessmentAttribute(
      final AssessmentEntityDetail assessmentEntity,
      final AssessmentAttribute attribute) {

    return assessmentEntity.getAttributes().stream()
        .filter(attributeDetail -> attributeDetail.getName()
            .equalsIgnoreCase(attribute.name()))
        .findFirst()
        .orElse(null);
  }

  /**
   * Checks if the Assessment details has a consistent reference to the session,
   * this checks over the global entities and attributes to ensure the case reference is consistent.
   *
   * @param assessment the assessment detail to check
   * @return {@code true} if the assessment is consistent, {@code false} otherwise
   */
  public static boolean isAssessmentReferenceConsistent(final AssessmentDetail assessment) {

    final List<AssessmentEntityDetail> globalEntites =
        getAssessmentEntitiesForEntityType(assessment, GLOBAL);

    //Check entity ID matches
    for (final AssessmentEntityDetail globalEntity : globalEntites) {
      if (!assessment.getCaseReferenceNumber().equalsIgnoreCase(globalEntity.getName())) {
        log.info(
            "CORRUPTED : Assessment : " + assessment.getCaseReferenceNumber() + "Assessment : "
                + assessment.getName() + ", EntityId : " + globalEntity.getName());

        return false;
      }
    }

    //Check attribute APPLICATION_CASE_REF matches targetID
    for (final AssessmentEntityDetail globalEntity : globalEntites) {

      final AssessmentAttributeDetail attribute =
          getAssessmentAttribute(globalEntity, APPLICATION_CASE_REF);

      if (attribute != null) {

        if (assessment.getCaseReferenceNumber().equalsIgnoreCase(globalEntity.getName())
            && globalEntity.getName().equalsIgnoreCase(attribute.getValue())
            && attribute.getValue().equalsIgnoreCase(assessment.getCaseReferenceNumber())) {
          return true;
        } else {
          log.info("CORRUPTED : Assessment : " + assessment.getCaseReferenceNumber()
              + ", EntityId : " + globalEntity.getName()
              + ", Attribute : " + attribute.getValue());

          return false;
        }
      }
    }
    return true;
  }

  /**
   * Get a list of assessments names for non-financial assessments, including the Prepopulated
   * assessments.
   *
   * @return List of non-financial assessment names including prepop.
   */
  public static List<String> getNonFinancialAssessmentNamesIncludingPrepop() {
    return AssessmentRulebase.getNonFinancialRulebases().stream()
        .map(AssessmentRulebase::getType)
        .flatMap(category -> AssessmentName.findAssessmentNamesByCategory(category).stream())
        .toList();
  }
}
