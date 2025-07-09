package uk.gov.laa.ccms.caab.util;

import java.util.Optional;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;

/** Utility class for handling file related operations. */
public final class FileUtil {

  /**
   * Extract the file extension from the filename of a MultipartFile. If the filename doesn't have a
   * '.' in it, then the whole filename will be returned.
   *
   * @param file - the multipart file.
   * @return the file extension or the whole filename.
   * @throws CaabApplicationException if the filename is null.
   */
  public static String getFileExtension(MultipartFile file) {
    return getFileExtension(file.getOriginalFilename());
  }

  /**
   * Extract the file extension from a filename. If the filename doesn't have a '.' in it, then the
   * whole filename will be returned.
   *
   * @param filename - the filename.
   * @return the file extension or the whole filename.
   * @throws CaabApplicationException if the filename is null.
   */
  public static String getFileExtension(String filename) {
    return Optional.ofNullable(filename)
        .map(s -> s.substring(s.lastIndexOf('.') + 1))
        .orElseThrow(() -> new CaabApplicationException("Failed to retrieve file extension"));
  }

  /**
   * Get a full filename consisting of the name and the extension if present.
   *
   * @param name of the file.
   * @param extension of the file.
   * @return the full filename.
   */
  public static String getFilename(String name, String extension) {
    return name + (extension == null || extension.equals(name) ? "" : "." + extension);
  }

  private FileUtil() {}
}
