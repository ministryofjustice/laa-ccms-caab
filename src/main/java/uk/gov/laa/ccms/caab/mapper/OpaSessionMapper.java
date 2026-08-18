package uk.gov.laa.ccms.caab.mapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentAttributeDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentEntityDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentEntityTypeDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentRelationshipDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentRelationshipTargetDetail;
import uk.gov.laa.ccms.caab.opa.session.OpaSessionJson;

/**
 * Converts between an {@link AssessmentDetail} and the OPA session shape the connector's assess
 * service exchanges.
 *
 * <p>The two models line up almost one to one - entity types hold entities, which hold attributes
 * and relationships - so this is a straight rename of the fields onto the connector's names.
 */
@Component
public class OpaSessionMapper {

  /**
   * Converts an assessment into the OPA session the connector's assess service expects.
   *
   * @param assessment the assessment to convert.
   * @param modifiedBy the login id of the user running the assessment.
   * @return the OPA session to send.
   */
  public OpaSessionJson toOpaSession(final AssessmentDetail assessment, final String modifiedBy) {
    final OpaSessionJson session = new OpaSessionJson();
    session.setId(assessment.getId());
    session.setAssessment(assessment.getName());
    session.setOwnerId(assessment.getProviderId());
    session.setTargetId(assessment.getCaseReferenceNumber());
    session.setStatus(assessment.getStatus());
    session.setCreatedBy(modifiedBy);
    session.setModifiedBy(modifiedBy);

    final List<OpaSessionJson.OpaListEntityJson> listEntities = new ArrayList<>();
    for (final AssessmentEntityTypeDetail entityType : orEmpty(assessment.getEntityTypes())) {
      final OpaSessionJson.OpaListEntityJson listEntity = new OpaSessionJson.OpaListEntityJson();
      listEntity.setId(entityType.getId());
      listEntity.setEntityType(entityType.getName());

      final List<OpaSessionJson.OpaEntityJson> entities = new ArrayList<>();
      for (final AssessmentEntityDetail entity : orEmpty(entityType.getEntities())) {
        final OpaSessionJson.OpaEntityJson entityJson = new OpaSessionJson.OpaEntityJson();
        entityJson.setId(entity.getId());
        entityJson.setEntityId(entity.getName());
        // The connector deserialises these into primitive booleans, so an absent field unboxes
        // null and fails the whole request with a NullPointerException. Always send a value.
        entityJson.setPrepopulated(isTrue(entity.getPrepopulated()));
        entityJson.setCompleted(isTrue(entity.getCompleted()));
        entityJson.setAttribute(toAttributes(entity.getAttributes()));
        entityJson.setOpaRelations(toRelationships(entity.getRelations()));
        entities.add(entityJson);
      }
      listEntity.setOpaEntities(entities);
      listEntities.add(listEntity);
    }
    session.setOpaListEntities(listEntities);

    return session;
  }

  /**
   * Merges an assessed OPA session back into an assessment.
   *
   * <p>This mirrors the legacy PUI {@code updateOpaSession}: the assess response is applied over
   * the existing session rather than replacing it, so entity types, entities and attributes the
   * response does not mention are kept. That matters because the response drops attributes CAAB
   * tracks but the connector does not carry (the {@code asked} flag), and adds the billing entities
   * the rulebase derives (bill and POA history, provider firms, prior authorities), which are the
   * whole point of the call.
   *
   * @param assessment the assessment to merge into, modified in place.
   * @param session the assessed OPA session returned by the connector.
   */
  public void mergeInto(final AssessmentDetail assessment, final OpaSessionJson session) {
    if (session == null) {
      return;
    }

    if (assessment.getEntityTypes() == null) {
      assessment.setEntityTypes(new ArrayList<>());
    }

    final Map<String, AssessmentEntityTypeDetail> entityTypesByName = new LinkedHashMap<>();
    for (final AssessmentEntityTypeDetail entityType : assessment.getEntityTypes()) {
      entityTypesByName.put(entityType.getName(), entityType);
    }

    for (final OpaSessionJson.OpaListEntityJson listEntity :
        orEmpty(session.getOpaListEntities())) {
      final AssessmentEntityTypeDetail entityType =
          entityTypesByName.computeIfAbsent(
              listEntity.getEntityType(),
              name -> {
                final AssessmentEntityTypeDetail added =
                    new AssessmentEntityTypeDetail().name(name).entities(new ArrayList<>());
                assessment.getEntityTypes().add(added);
                return added;
              });

      if (entityType.getEntities() == null) {
        entityType.setEntities(new ArrayList<>());
      }

      mergeEntities(entityType, listEntity);
    }
  }

  private void mergeEntities(
      final AssessmentEntityTypeDetail entityType,
      final OpaSessionJson.OpaListEntityJson listEntity) {

    final Map<String, AssessmentEntityDetail> entitiesById = new LinkedHashMap<>();
    for (final AssessmentEntityDetail entity : entityType.getEntities()) {
      entitiesById.put(entity.getName(), entity);
    }

    for (final OpaSessionJson.OpaEntityJson entityJson : orEmpty(listEntity.getOpaEntities())) {
      final AssessmentEntityDetail entity =
          entitiesById.computeIfAbsent(
              entityJson.getEntityId(),
              entityId -> {
                final AssessmentEntityDetail added =
                    new AssessmentEntityDetail()
                        .name(entityId)
                        // The assessment API requires this, and rejects the whole save with a 400
                        // when it is null. Entities the rulebase derives while assessing were not
                        // seeded by pre-population, so false is the right default; the connector
                        // overrides it below when it says otherwise.
                        .prepopulated(false)
                        .attributes(new ArrayList<>())
                        .relations(new ArrayList<>());
                entityType.getEntities().add(added);
                return added;
              });

      if (entityJson.getPrepopulated() != null) {
        entity.setPrepopulated(entityJson.getPrepopulated());
      }
      if (entityJson.getCompleted() != null) {
        entity.setCompleted(entityJson.getCompleted());
      }

      mergeAttributes(entity, entityJson);
      mergeRelationships(entity, entityJson);
    }
  }

  private void mergeAttributes(
      final AssessmentEntityDetail entity, final OpaSessionJson.OpaEntityJson entityJson) {

    if (entity.getAttributes() == null) {
      entity.setAttributes(new ArrayList<>());
    }

    final Map<String, AssessmentAttributeDetail> attributesByName = new LinkedHashMap<>();
    for (final AssessmentAttributeDetail attribute : entity.getAttributes()) {
      attributesByName.put(attribute.getName(), attribute);
    }

    for (final OpaSessionJson.OpaAttributeJson attributeJson : orEmpty(entityJson.getAttribute())) {
      final AssessmentAttributeDetail attribute =
          attributesByName.computeIfAbsent(
              attributeJson.getAttributeId(),
              name -> {
                final AssessmentAttributeDetail added = new AssessmentAttributeDetail().name(name);
                entity.getAttributes().add(added);
                return added;
              });

      attribute.setValue(attributeJson.getValue());
      if (attributeJson.getAttributeType() != null) {
        attribute.setType(attributeJson.getAttributeType());
      }
      if (attributeJson.getInferencingType() != null) {
        attribute.setInferencingType(attributeJson.getInferencingType());
      }
      if (attributeJson.getPrepopulated() != null) {
        attribute.setPrepopulated(attributeJson.getPrepopulated());
      }
    }
  }

  private void mergeRelationships(
      final AssessmentEntityDetail entity, final OpaSessionJson.OpaEntityJson entityJson) {

    if (entity.getRelations() == null) {
      entity.setRelations(new ArrayList<>());
    }

    final Map<String, AssessmentRelationshipDetail> relationsByName = new LinkedHashMap<>();
    for (final AssessmentRelationshipDetail relation : entity.getRelations()) {
      relationsByName.put(relation.getName(), relation);
    }

    for (final OpaSessionJson.OpaRelationshipJson relationJson :
        orEmpty(entityJson.getOpaRelations())) {
      final AssessmentRelationshipDetail relation =
          relationsByName.computeIfAbsent(
              relationJson.getName(),
              name -> {
                final AssessmentRelationshipDetail added =
                    new AssessmentRelationshipDetail()
                        .name(name)
                        .relationshipTargets(new ArrayList<>());
                entity.getRelations().add(added);
                return added;
              });

      if (relationJson.getPrepopulated() != null) {
        relation.setPrepopulated(relationJson.getPrepopulated());
      }
      if (relation.getRelationshipTargets() == null) {
        relation.setRelationshipTargets(new ArrayList<>());
      }

      // Targets are identified only by the entity they point at, so they are matched on that
      // rather than merged field by field.
      final List<String> existingTargets =
          relation.getRelationshipTargets().stream()
              .map(AssessmentRelationshipTargetDetail::getTargetEntityId)
              .toList();

      for (final OpaSessionJson.OpaRelationshipTargetJson targetJson :
          orEmpty(relationJson.getRelationshipTargets())) {
        if (!existingTargets.contains(targetJson.getTargetEntityId())) {
          relation
              .getRelationshipTargets()
              .add(
                  new AssessmentRelationshipTargetDetail()
                      .targetEntityId(targetJson.getTargetEntityId()));
        }
      }
    }
  }

  private List<OpaSessionJson.OpaAttributeJson> toAttributes(
      final List<AssessmentAttributeDetail> attributes) {
    final List<OpaSessionJson.OpaAttributeJson> result = new ArrayList<>();
    for (final AssessmentAttributeDetail attribute : orEmpty(attributes)) {
      final OpaSessionJson.OpaAttributeJson json = new OpaSessionJson.OpaAttributeJson();
      json.setId(attribute.getId());
      json.setAttributeId(attribute.getName());
      json.setAttributeType(attribute.getType());
      json.setInferencingType(attribute.getInferencingType());
      json.setValue(attribute.getValue());
      json.setPrepopulated(isTrue(attribute.getPrepopulated()));
      result.add(json);
    }
    return result;
  }

  private List<OpaSessionJson.OpaRelationshipJson> toRelationships(
      final List<AssessmentRelationshipDetail> relations) {
    final List<OpaSessionJson.OpaRelationshipJson> result = new ArrayList<>();
    for (final AssessmentRelationshipDetail relation : orEmpty(relations)) {
      final OpaSessionJson.OpaRelationshipJson json = new OpaSessionJson.OpaRelationshipJson();
      json.setId(relation.getId());
      json.setName(relation.getName());
      json.setPrepopulated(isTrue(relation.getPrepopulated()));

      final List<OpaSessionJson.OpaRelationshipTargetJson> targets = new ArrayList<>();
      for (final AssessmentRelationshipTargetDetail target :
          orEmpty(relation.getRelationshipTargets())) {
        final OpaSessionJson.OpaRelationshipTargetJson targetJson =
            new OpaSessionJson.OpaRelationshipTargetJson();
        targetJson.setId(target.getId());
        targetJson.setTargetEntityId(target.getTargetEntityId());
        targets.add(targetJson);
      }
      json.setRelationshipTargets(targets);
      result.add(json);
    }
    return result;
  }

  /**
   * Coerces a nullable flag to a definite value. The connector holds these as primitive booleans,
   * so it cannot accept a missing one.
   *
   * @param value the nullable flag.
   * @return the flag, false when not set.
   */
  private boolean isTrue(final Boolean value) {
    return Boolean.TRUE.equals(value);
  }

  private <T> List<T> orEmpty(final List<T> list) {
    return Optional.ofNullable(list).orElseGet(List::of);
  }
}
