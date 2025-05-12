package uk.gov.laa.ccms.caab.util;

import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentAttribute.APPLICATION_CASE_REF;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentEntityType.GLOBAL;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentAttributeDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetails;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentEntityDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentEntityTypeDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentRelationshipDetail;
import uk.gov.laa.ccms.caab.constants.assessment.AssessmentAttribute;
import uk.gov.laa.ccms.caab.constants.assessment.AssessmentEntityType;
import uk.gov.laa.ccms.caab.constants.assessment.AssessmentName;
import uk.gov.laa.ccms.caab.constants.assessment.AssessmentRelationship;
import uk.gov.laa.ccms.caab.constants.assessment.AssessmentRulebase;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.data.model.AssessmentSummaryAttributeLookupValueDetail;
import uk.gov.laa.ccms.data.model.AssessmentSummaryEntityLookupValueDetail;

/**
 * Utility class for handling assessment-related operations.
 */
@Slf4j
public final class AssessmentUtil {

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

    if (assessment == null || entityType == null) {
      return null;
    }

    return assessment.getEntityTypes().stream()
        .filter(entityTypeDetail -> entityTypeDetail.getName()
            .equalsIgnoreCase(entityType.getType()))
        .findFirst()
        .orElse(null);
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
      final String entityType) {

    if (assessment == null || entityType == null) {
      return null;
    }

    return assessment.getEntityTypes().stream()
        .filter(entityTypeDetail -> entityTypeDetail.getName()
            .equalsIgnoreCase(entityType))
        .findFirst()
        .orElse(null);
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
   * Retrieves a list of assessment entities for the specified entity type.
   *
   * @param assessment the assessment detail object
   * @param entityType the type of the assessment entity
   * @return a list of assessment entity details, or an empty list if none found
   */
  public static List<AssessmentEntityDetail> getAssessmentEntitiesForEntityType(
      final AssessmentDetail assessment,
      final String entityType) {

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

    if (assessmentEntity == null || relationship == null) {
      return null;
    }

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
   * Retrieves the related entities based on the given relationship and assessment details.
   *
   * @param relationship the relationship detail to match against
   * @param assessment the assessment containing entity types and their details
   * @return a list of related assessment entity details
   */
  public static List<AssessmentEntityDetail> getRelatedEntities(
      final AssessmentRelationshipDetail relationship,
      final AssessmentDetail assessment) {

    final List<AssessmentEntityDetail> relatedEntities = new ArrayList<>();

    for (final AssessmentEntityTypeDetail entityType : assessment.getEntityTypes()) {
      if (entityType.getName().equalsIgnoreCase(
          relationship.getName().replace("_", ""))) {
        relatedEntities.addAll(entityType.getEntities());
      }
    }
    return relatedEntities;
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

    if (attribute == null || assessmentEntity == null) {
      return null;
    }

    return assessmentEntity.getAttributes().stream()
        .filter(attributeDetail -> attributeDetail.getName()
            .equalsIgnoreCase(attribute.name()))
        .findFirst()
        .orElse(null);
  }

  /**
   * Retrieves the detail of a specified attribute from the given assessment entity.
   *
   * @param assessmentEntity the assessment entity containing attribute details
   * @param attribute the name of the attribute to retrieve
   * @return the detail of the specified attribute, or null if not found or if any parameter is null
   */
  public static AssessmentAttributeDetail getAssessmentAttribute(
      final AssessmentEntityDetail assessmentEntity,
      final String attribute) {

    if (attribute == null || assessmentEntity == null) {
      return null;
    }

    return assessmentEntity.getAttributes().stream()
        .filter(attributeDetail -> attributeDetail.getName()
            .equalsIgnoreCase(attribute))
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
  
  /**
   * Formats the value of an AssessmentAttributeDetail based on its type.
   *
   * @param attribute the AssessmentAttributeDetail containing the value and type to be formatted
   * @return the formatted value as a String, or null if the value is "UNCERTAIN_VALUE_STRING"
   */
  public static String getFormattedAttributeValue(final AssessmentAttributeDetail attribute) {
    if ("UNCERTAIN_VALUE_STRING".equalsIgnoreCase(attribute.getValue())) {
      return null;
    }

    String formattedValue;
    if ("DATE".equalsIgnoreCase(attribute.getType()) && attribute.getValue() != null) {
      try {
        // Parse the date string and format it to the desired display format
        final Date storedDate = new SimpleDateFormat("yyyy-MM-dd").parse(attribute.getValue());
        formattedValue = new SimpleDateFormat("dd/MM/yyyy").format(storedDate);
      } catch (final ParseException e) {
        log.debug("Unable to parse stored date according to format, "
            + "will use the un-formatted date on the overview screen", e);
        formattedValue = attribute.getValue();
      }
    } else if ("CURRENCY".equalsIgnoreCase(attribute.getType()) && attribute.getValue() != null) {
      try {
        // Convert the value to a number and format it as currency
        final BigDecimal attributeAsNumber = new BigDecimal(attribute.getValue());
        formattedValue = attributeAsNumber.setScale(2, RoundingMode.HALF_UP)
            .toPlainString();
        formattedValue = "£" + formattedValue;
      } catch (final NumberFormatException e) {
        log.debug("Unable to convert attribute value to a number, "
            + "will use the un-formatted value on the overview screen.", e);
        formattedValue = attribute.getValue();
      }
    } else if ("NUMBER".equalsIgnoreCase(attribute.getType()) && attribute.getValue() != null) {
      try {
        // Convert the value to a number and format it, stripping trailing zeros
        final BigDecimal attributeAsNumber = new BigDecimal(attribute.getValue());
        formattedValue = attributeAsNumber.setScale(2, RoundingMode.HALF_UP)
            .stripTrailingZeros().toPlainString();
      } catch (final NumberFormatException e) {
        log.debug("Unable to convert attribute value to a number, "
            + "will use the un-formatted value on the overview screen.", e);
        formattedValue = attribute.getValue();
      }
    } else if ("BOOLEAN".equalsIgnoreCase(attribute.getType()) && attribute.getValue() != null) {
      if ("true".equalsIgnoreCase(attribute.getValue())) {
        formattedValue = "Yes";
      } else {
        formattedValue = "No";
      }
    } else {
      formattedValue = attribute.getValue();
      if (formattedValue != null) {
        // Normalize newlines in the string
        String newline = System.lineSeparator();
        newline = newline != null ? newline : "\n";
        formattedValue = formattedValue.replaceAll("(\r\n|\r|\n)", newline);
      }
    }
    return formattedValue;
  }

  /**
   * Retrieves the display name for a specified attribute from the given summary entity lookup.
   *
   * @param summaryEntityLookup the lookup entity containing attribute details
   * @param attributeName the name of the attribute to get the display name for
   * @return the display name of the specified attribute, or the attribute name if not found
   */
  public static String getDisplayNameForAttribute(
      final AssessmentSummaryEntityLookupValueDetail summaryEntityLookup,
      final String attributeName) {
    return summaryEntityLookup.getAttributes().stream()
        .filter(attributeLookup -> attributeLookup.getName().equals(attributeName))
        .findFirst()
        .map(AssessmentSummaryAttributeLookupValueDetail::getDisplayName)
        .orElse(attributeName);
  }

  /**
   * Retrieves the assessment detail from the given assessment details based on the rulebase name.
   *
   * @param assessmentDetails the assessment details containing the assessment to retrieve
   * @param assessmentRulebase the rulebase containing the name of the assessment to retrieve
   * @return the matching assessment detail, or throws an exception if not found
   */
  public static AssessmentDetail getAssessment(
      final AssessmentDetails assessmentDetails,
      final AssessmentRulebase assessmentRulebase) {

    return assessmentDetails.getContent()
        .stream()
        .filter(assessmentDetail -> assessmentDetail.getName()
            .equalsIgnoreCase(assessmentRulebase.getName()))
        .findFirst()
        .orElseThrow(() -> new CaabApplicationException("Failed to retrieve assessment"));
  }

  private AssessmentUtil() {
  }

}
