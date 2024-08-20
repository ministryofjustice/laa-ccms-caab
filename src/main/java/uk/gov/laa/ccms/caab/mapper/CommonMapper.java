package uk.gov.laa.ccms.caab.mapper;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Named;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.model.NotificationAttachmentDetail;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;
import uk.gov.laa.ccms.data.model.RelationshipToCaseLookupDetail;
import uk.gov.laa.ccms.data.model.RelationshipToCaseLookupValueDetail;


/**
 * Mapper interface for converting common data between different representations.
 */
@Mapper(componentModel = "spring")
public interface CommonMapper {

  /**
   * Convert a Base64 encoded string to an array of bytes.
   *
   * @param base64EncodedString the Base64 encoded string.
   * @return the corresponding array of bytes.
   */
  default byte[] toByteArrayFromBase64EncodedString(String base64EncodedString) {
    return Optional.ofNullable(base64EncodedString)
        .map(s -> Base64.getDecoder().decode(s))
        .orElse(null);
  }

  /**
   * Convenience method to retrieve the uploaded file bytes, and handle any IOException.
   *
   * @return file data bytes.
   * @throws CaabApplicationException if an error occurs.
   */
  default byte[] toFileBytes(MultipartFile file) throws CaabApplicationException {
    byte[] fileData = null;

    if (file != null) {
      try {
        fileData = file.getBytes();
      } catch (IOException ioe) {
        throw new CaabApplicationException("Failed to get uploaded file content", ioe);
      }
    }

    return fileData;
  }

  /**
   * Create a MultipartFile based on a NotificationAttachmentDetail.
   *
   * @return the MultipartFile.
   */
  @Named("toMultipartFile")
  default MultipartFile toMultipartFile(NotificationAttachmentDetail notificationAttachmentDetail) {

    return new TempMultipartFile(
        null,
        notificationAttachmentDetail.getFileName(),
        null,
        toByteArrayFromBase64EncodedString(notificationAttachmentDetail.getFileData()));
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

  /**
   * A simplified implementation of MultipartFile used for mapping.
   */
  @Getter
  @Setter
  @RequiredArgsConstructor
  class TempMultipartFile implements MultipartFile {

    private final String name;
    private final String originalFilename;
    private final String contentType;
    private final byte[] bytes;

    @Override
    public boolean isEmpty() {
      return this.getBytes() == null || this.getBytes().length == 0;
    }

    @Override
    public long getSize() {
      return this.getBytes().length;
    }

    @Override
    public InputStream getInputStream() throws IOException {
      return new ByteArrayInputStream(bytes);
    }

    @Override
    public void transferTo(File dest) throws IOException, IllegalStateException {
      throw new IllegalStateException();
    }
  }

}
