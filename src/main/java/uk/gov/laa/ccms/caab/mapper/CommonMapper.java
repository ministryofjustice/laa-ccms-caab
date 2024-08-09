package uk.gov.laa.ccms.caab.mapper;

import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Named;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;
import uk.gov.laa.ccms.data.model.RelationshipToCaseLookupDetail;
import uk.gov.laa.ccms.data.model.RelationshipToCaseLookupValueDetail;


/**
 * Mapper interface for converting common data between different representations.
 */
@Mapper(componentModel = "spring")
public interface CommonMapper {

  default byte[] toByteArrayFromBase64EncodedString(String base64EncodedString) {
    return Base64.getDecoder().decode(base64EncodedString);
  }

  /**
   * Convert an array of bytes to a Base64 encoded string.
   *
   * @param bytes - the byte array.
   * @return Base64 encoded String, or null.
   */
  default String toBase64EncodedStringFromByteArray(byte[] bytes) {
    return Optional.ofNullable(bytes)
        .map(b -> Base64.getEncoder().encodeToString(b))
        .orElse(null);
  }

  default String toCaretSeparatedString(List<String> items) {
    return String.join("^", items);
  }

  default List<String> toListFromDelimitedString(final String items, final String delimiter) {
    return Arrays.stream(items.split(delimiter)).toList();
  }

  /**
   * Convert a code to its display value.
   *
   * @param code - the code
   * @param lookups - the lookups containing the display values
   * @return display value for code, or the original code if no match found.
   */
  @Named("toDisplayValue")
  default String toDisplayValue(
      final String code,
      @Context final List<CommonLookupValueDetail> lookups) {
    return lookups.stream()
        .filter(lookup -> lookup.getCode().equals(code))
        .findFirst()
        .map(CommonLookupValueDetail::getDescription)
        .orElse(code);
  }

  /**
   * Converts the given code to its display value using the provided lookup details.
   *
   * @param code the code to be converted
   * @param lookup the lookup details to be used for conversion
   * @return the display value corresponding to the code, or {@code null} if the lookup is
   *         {@code null}
   */
  default String toDisplayValue(
      final String code,
      final CommonLookupDetail lookup) {
    return lookup != null && lookup.getContent() != null
        ? toDisplayValue(code, lookup.getContent()) : null;
  }

  /**
   * Converts the given code to its display value using a list of relationship lookup details.
   *
   * @param code the code to be converted
   * @param lookups the list of relationship lookup details to be used for conversion
   * @return the display value corresponding to the code, or the code itself if not found
   */
  default String toRelationshipDisplayValue(
      final String code,
      @Context final List<RelationshipToCaseLookupValueDetail> lookups) {
    return lookups.stream()
        .filter(lookup -> lookup.getCode().equals(code))
        .findFirst()
        .map(RelationshipToCaseLookupValueDetail::getDescription)
        .orElse(code);
  }

  /**
   * Converts the given code to its display value using the provided relationship lookup details.
   *
   * @param code the code to be converted
   * @param lookup the relationship lookup details to be used for conversion
   * @return the display value corresponding to the code, or {@code null} if the lookup is
   *         {@code null}
   */
  default String toRelationshipDisplayValue(
      final String code,
      final RelationshipToCaseLookupDetail lookup) {
    return lookup != null && lookup.getContent() != null
        ? toRelationshipDisplayValue(code, lookup.getContent()) : null;
  }

}
