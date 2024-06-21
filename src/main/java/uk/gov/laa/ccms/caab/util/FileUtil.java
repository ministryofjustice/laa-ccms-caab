package uk.gov.laa.ccms.caab.util;

import java.util.Optional;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;

/**
 * Utility class for handling file related operations.
 */
public class FileUtil {

  /**
   * Extract the file extension from the filename of a MultipartFile.
   * If the filename doesn't have a '.' in it, then the whole filename will be returned.
   *
   * @param file - the multipart file.
   * @return the file extension or the whole filename.
   * @throws CaabApplicationException if the filename is null.
   */
  public static String getFileExtension(MultipartFile file) {
    return Optional.ofNullable(file.getOriginalFilename())
        .map(s -> s.substring(s.lastIndexOf(".") + 1))
        .orElseThrow(() -> new CaabApplicationException("Failed to retrieve file extension"));
  }


}
