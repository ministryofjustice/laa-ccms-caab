package uk.gov.laa.ccms.caab.util;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

class FileUtilTest {

  @Test
  void getFileExtensionExtractsCorrectExtension() {
    final String fileExt = "pdf";
    String filename = "originalName.%s".formatted(fileExt);

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
  void getFileExtensionNoExtensionReturnsFilename() {
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
