package uk.gov.laa.ccms.caab.mapper;

import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import org.mapstruct.Mapper;


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

}
