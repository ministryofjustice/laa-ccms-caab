package uk.gov.laa.ccms.caab.opa.session;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * The OPA session as the connector serialises it, for the assess-at-start call the billing and POA
 * journeys make.
 *
 * <p>The field names are dictated by the connector's {@code uk.gov.laa.opa.json.JsonAdapter} and
 * are not the usual snake_case of the CAAB APIs, so each one is pinned with {@link JsonProperty}.
 * The connector deliberately omits {@code created} / {@code modified} when it serialises a session,
 * so they are not modelled here either.
 *
 * <p>Unknown fields are ignored rather than rejected: the connector is shared with the legacy PUI
 * and may add fields, and an assess response that carries one must not fail the interview.
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpaSessionJson {

  @JsonProperty("id")
  private Long id;

  /** The assessment name, e.g. "poaAssessment_PREPOP". */
  @JsonProperty("assessment")
  private String assessment;

  /** The provider id. */
  @JsonProperty("ownerID")
  private String ownerId;

  /** The case reference number. */
  @JsonProperty("targetID")
  private String targetId;

  @JsonProperty("status")
  private String status;

  @JsonProperty("createdBy")
  private String createdBy;

  @JsonProperty("modifiedBy")
  private String modifiedBy;

  @JsonProperty("opaListEntities")
  private List<OpaListEntityJson> opaListEntities = new ArrayList<>();

  /** An entity type (OPA table), e.g. "global" or "PROCEEDING". */
  @Data
  @JsonInclude(JsonInclude.Include.NON_NULL)
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class OpaListEntityJson {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("entityType")
    private String entityType;

    @JsonProperty("opaEntities")
    private List<OpaEntityJson> opaEntities = new ArrayList<>();
  }

  /** A single instance (OPA row) of an entity type. */
  @Data
  @JsonInclude(JsonInclude.Include.NON_NULL)
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class OpaEntityJson {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("entityId")
    private String entityId;

    @JsonProperty("prepopulated")
    private Boolean prepopulated;

    @JsonProperty("completed")
    private Boolean completed;

    @JsonProperty("attribute")
    private List<OpaAttributeJson> attribute = new ArrayList<>();

    @JsonProperty("opaRelations")
    private List<OpaRelationshipJson> opaRelations = new ArrayList<>();
  }

  /** A single attribute on an entity instance. */
  @Data
  @JsonInclude(JsonInclude.Include.NON_NULL)
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class OpaAttributeJson {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("attributeId")
    private String attributeId;

    @JsonProperty("attributeType")
    private String attributeType;

    @JsonProperty("defaultValue")
    private String defaultValue;

    @JsonProperty("inferencingType")
    private String inferencingType;

    @JsonProperty("value")
    private String value;

    @JsonProperty("prepopulated")
    private Boolean prepopulated;
  }

  /** A relationship from an entity instance to other instances. */
  @Data
  @JsonInclude(JsonInclude.Include.NON_NULL)
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class OpaRelationshipJson {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("prepopulated")
    private Boolean prepopulated;

    @JsonProperty("relationshipTargets")
    private List<OpaRelationshipTargetJson> relationshipTargets = new ArrayList<>();
  }

  /** A single target of a relationship. */
  @Data
  @JsonInclude(JsonInclude.Include.NON_NULL)
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class OpaRelationshipTargetJson {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("targetEntityId")
    private String targetEntityId;
  }
}
