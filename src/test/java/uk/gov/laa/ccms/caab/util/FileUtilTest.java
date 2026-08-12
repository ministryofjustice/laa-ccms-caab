package uk.gov.laa.ccms.caab.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

class FileUtilTest {

  @Test
  void testGetFileExtension_extractsCorrectExtension() {
    final String fileExt = "pdf";
    String filename = "originalName.%s".formatted(fileExt);

    final MultipartFile multipartFile =
        new MockMultipartFile("theFile", filename, "contentType", "the file data".getBytes());

    final String result = FileUtil.getFileExtension(multipartFile);

    assertNotNull(result);
    assertEquals(fileExt, result);
  }

  @Test
  void testGetFileExtension_noExtension_returnsFilename() {
    String filename = "originalName";

    final MultipartFile multipartFile =
        new MockMultipartFile("theFile", filename, "contentType", "the file data".getBytes());

    final String result = FileUtil.getFileExtension(multipartFile);

    assertNotNull(result);
    assertEquals(filename, result);
  }

  @Test
  void testSanitizeFileName_replacesDisallowedCharactersWithUnderscore() {
    final String result = FileUtil.sanitiseFileName("My interesting%filename!.pdf");

    assertEquals("My_interesting_filename_.pdf", result);
  }

  @Test
  void testSanitizeFileName_keepsAllowedCharacters() {
    final String result = FileUtil.sanitiseFileName("Valid_File-Name123.pdf");

    assertEquals("Valid_File-Name123.pdf", result);
  }

  @Test
  void testSanitiseFileName_blankFilename_returnsBlank() {
    final String result = FileUtil.sanitiseFileName("   ");

    assertEquals("", result);
  }
}
