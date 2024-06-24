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
    String filename = String.format("originalName.%s", fileExt);

    final MultipartFile multipartFile = new MockMultipartFile(
        "theFile",
        filename,
        "contentType",
        "the file data".getBytes());

    final String result = FileUtil.getFileExtension(multipartFile);

    assertNotNull(result);
    assertEquals(fileExt, result);
  }

  @Test
  void testGetFileExtension_noExtension_returnsFilename() {
    String filename = "originalName";

    final MultipartFile multipartFile = new MockMultipartFile(
        "theFile",
        filename,
        "contentType",
        "the file data".getBytes());

    final String result = FileUtil.getFileExtension(multipartFile);

    assertNotNull(result);
    assertEquals(filename, result);
  }
}