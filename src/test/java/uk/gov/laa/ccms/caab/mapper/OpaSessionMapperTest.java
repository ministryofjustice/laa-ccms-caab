package uk.gov.laa.ccms.caab.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentAttributeDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentEntityDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentEntityTypeDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentRelationshipDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentRelationshipTargetDetail;
import uk.gov.laa.ccms.caab.opa.session.OpaSessionJson;

@DisplayName("OPA session mapper tests")
class OpaSessionMapperTest {

  private final OpaSessionMapper mapper = new OpaSessionMapper();
  private final ObjectMapper objectMapper = new ObjectMapper();

  private AssessmentDetail assessment() {
    return new AssessmentDetail()
        .id(12L)
        .name("poaAssessment_PREPOP")
        .providerId("26517")
        .caseReferenceNumber("300000319502")
        .status("INCOMPLETE")
        .entityTypes(
            new ArrayList<>(
                List.of(
                    new AssessmentEntityTypeDetail()
                        .name("global")
                        .entities(
                            new ArrayList<>(
                                List.of(
                                    new AssessmentEntityDetail()
                                        .name("300000319502")
                                        .prepopulated(false)
                                        .attributes(
                                            new ArrayList<>(
                                                List.of(
                                                    new AssessmentAttributeDetail()
                                                        .name("POA_OR_BILL_FLAG")
                                                        .type("text")
                                                        .value("POA")
                                                        .prepopulated(true)
                                                        .asked(true))))
                                        .relations(
                                            new ArrayList<>(
                                                List.of(
                                                    new AssessmentRelationshipDetail()
                                                        .name("proceeding")
                                                        .prepopulated(true)
                                                        .relationshipTargets(
                                                            new ArrayList<>(
                                                                List.of(
                                                                    new AssessmentRelationshipTargetDetail()
                                                                        .targetEntityId(
                                                                            "P1")))))))))))));
  }

  @Test
  @DisplayName("Maps an assessment onto the connector's OPA session field names")
  void mapsToOpaSession() {
    OpaSessionJson session = mapper.toOpaSession(assessment(), "user1");

    assertThat(session.getAssessment()).isEqualTo("poaAssessment_PREPOP");
    assertThat(session.getOwnerId()).isEqualTo("26517");
    assertThat(session.getTargetId()).isEqualTo("300000319502");
    assertThat(session.getModifiedBy()).isEqualTo("user1");
    assertThat(session.getOpaListEntities()).hasSize(1);

    OpaSessionJson.OpaListEntityJson global = session.getOpaListEntities().get(0);
    assertThat(global.getEntityType()).isEqualTo("global");
    OpaSessionJson.OpaEntityJson entity = global.getOpaEntities().get(0);
    // CAAB's entity "name" is the OPA entity id.
    assertThat(entity.getEntityId()).isEqualTo("300000319502");
    assertThat(entity.getAttribute().get(0).getAttributeId()).isEqualTo("POA_OR_BILL_FLAG");
    assertThat(entity.getAttribute().get(0).getAttributeType()).isEqualTo("text");
    assertThat(entity.getOpaRelations().get(0).getName()).isEqualTo("proceeding");
    assertThat(entity.getOpaRelations().get(0).getRelationshipTargets().get(0).getTargetEntityId())
        .isEqualTo("P1");
  }

  @Test
  @DisplayName("Serialises using the connector's own key names, and omits created / modified")
  void serialisesWithConnectorKeys() throws Exception {
    String json = objectMapper.writeValueAsString(mapper.toOpaSession(assessment(), "user1"));

    // These names come from the connector's JsonAdapter and are not negotiable.
    assertThat(json)
        .contains("\"ownerID\"")
        .contains("\"targetID\"")
        .contains("\"opaListEntities\"")
        .contains("\"entityType\"")
        .contains("\"opaEntities\"")
        .contains("\"entityId\"")
        .contains("\"attribute\"")
        .contains("\"attributeId\"")
        .contains("\"attributeType\"")
        .contains("\"opaRelations\"")
        .contains("\"relationshipTargets\"")
        .contains("\"targetEntityId\"");
    // The connector deliberately does not serialise these, so neither do we.
    assertThat(json).doesNotContain("\"created\"").doesNotContain("\"modified\"");
  }

  @Test
  @DisplayName("Always sends the boolean flags, which the connector reads into primitives")
  void alwaysSendsBooleanFlags() throws Exception {
    // The connector's JsonAdapter does setPrepopulated(...) / setCompleted(...) straight into
    // primitive booleans, so an omitted flag unboxes null and fails the whole request with a
    // NullPointerException. Nothing may be left out, however incomplete the assessment is.
    AssessmentDetail sparse =
        new AssessmentDetail()
            .name("poaAssessment")
            .entityTypes(
                new ArrayList<>(
                    List.of(
                        new AssessmentEntityTypeDetail()
                            .name("global")
                            .entities(
                                new ArrayList<>(
                                    List.of(
                                        new AssessmentEntityDetail()
                                            .name("300000319502")
                                            .attributes(
                                                new ArrayList<>(
                                                    List.of(
                                                        new AssessmentAttributeDetail()
                                                            .name("POA_OR_BILL_FLAG"))))
                                            .relations(
                                                new ArrayList<>(
                                                    List.of(
                                                        new AssessmentRelationshipDetail()
                                                            .name("proceeding"))))))))));

    String json = objectMapper.writeValueAsString(mapper.toOpaSession(sparse, "user1"));

    assertThat(json).contains("\"prepopulated\":false").contains("\"completed\":false");
    assertThat(json).doesNotContain("\"prepopulated\":null").doesNotContain("\"completed\":null");

    OpaSessionJson session = mapper.toOpaSession(sparse, "user1");
    OpaSessionJson.OpaEntityJson entity =
        session.getOpaListEntities().get(0).getOpaEntities().get(0);
    assertThat(entity.getPrepopulated()).isNotNull();
    assertThat(entity.getCompleted()).isNotNull();
    assertThat(entity.getAttribute().get(0).getPrepopulated()).isNotNull();
    assertThat(entity.getOpaRelations().get(0).getPrepopulated()).isNotNull();
  }

  @Test
  @DisplayName("Merges the derived billing entities from an assess response into the assessment")
  void mergesDerivedEntities() throws Exception {
    // A cut-down assess response: the rulebase has derived a POA_HISTORY row that CAAB never sent.
    String response =
        """
        {"assessment":"poaAssessment_PREPOP","ownerID":"26517","targetID":"300000319502",
         "opaListEntities":[
           {"entityType":"POA_HISTORY","opaEntities":[
             {"entityId":"3000003195020001","prepopulated":true,"attribute":[
               {"attributeId":"POA_HISTORY_AMOUNT","attributeType":"currency","value":"223.23"}]}]},
           {"entityType":"global","opaEntities":[
             {"entityId":"300000319502","attribute":[
               {"attributeId":"BILLING_IS_COMPLETE","attributeType":"boolean","value":"true"}]}]}]}
        """;

    AssessmentDetail assessment = assessment();
    mapper.mergeInto(assessment, objectMapper.readValue(response, OpaSessionJson.class));

    AssessmentEntityTypeDetail poaHistory =
        assessment.getEntityTypes().stream()
            .filter(entityType -> "POA_HISTORY".equals(entityType.getName()))
            .findFirst()
            .orElseThrow();
    assertThat(poaHistory.getEntities()).hasSize(1);
    assertThat(poaHistory.getEntities().get(0).getName()).isEqualTo("3000003195020001");
    assertThat(poaHistory.getEntities().get(0).getAttributes().get(0).getValue())
        .isEqualTo("223.23");

    AssessmentEntityDetail global =
        assessment.getEntityTypes().stream()
            .filter(entityType -> "global".equals(entityType.getName()))
            .findFirst()
            .orElseThrow()
            .getEntities()
            .get(0);

    // The derived goal is added...
    assertThat(
            global.getAttributes().stream()
                .filter(attribute -> "BILLING_IS_COMPLETE".equals(attribute.getName()))
                .findFirst()
                .orElseThrow()
                .getValue())
        .isEqualTo("true");

    // ...and what CAAB already held is preserved, including the "asked" flag the connector's
    // session format does not carry.
    AssessmentAttributeDetail poaFlag =
        global.getAttributes().stream()
            .filter(attribute -> "POA_OR_BILL_FLAG".equals(attribute.getName()))
            .findFirst()
            .orElseThrow();
    assertThat(poaFlag.getValue()).isEqualTo("POA");
    assertThat(poaFlag.getAsked()).isTrue();
    assertThat(global.getRelations()).hasSize(1);
  }

  @Test
  @DisplayName("Tolerates unknown fields, so a connector addition cannot break the interview")
  void toleratesUnknownFields() throws Exception {
    String response =
        "{\"assessment\":\"poaAssessment\",\"somethingNew\":\"x\",\"opaListEntities\":[]}";

    OpaSessionJson session = objectMapper.readValue(response, OpaSessionJson.class);

    assertThat(session.getAssessment()).isEqualTo("poaAssessment");
  }
}
