package uk.gov.laa.ccms.caab.mapper;

import org.openapitools.jackson.nullable.JsonNullable;

/** Preserves unanswered yes/no fields when mapping nullable API properties. */
public interface NullableBooleanMapper {
  default Boolean unwrapBoolean(final JsonNullable<Boolean> value) {
    return value == null ? null : value.orElse(null);
  }

  default JsonNullable<Boolean> wrapBoolean(final Boolean value) {
    return JsonNullable.of(value);
  }
}
