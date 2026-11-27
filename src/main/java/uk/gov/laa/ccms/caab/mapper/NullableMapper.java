package uk.gov.laa.ccms.caab.mapper;

import org.openapitools.jackson.nullable.JsonNullable;

/** Preserves nullable API values, including explicit nulls and unanswered fields. */
public final class NullableMapper {
  private NullableMapper() {}

  public static <T> T unwrap(final JsonNullable<T> value) {
    return value == null ? null : value.orElse(null);
  }

  public static <T> JsonNullable<T> wrap(final T value) {
    return JsonNullable.of(value);
  }
}
