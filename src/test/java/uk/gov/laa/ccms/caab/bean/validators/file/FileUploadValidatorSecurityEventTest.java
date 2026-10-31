package uk.gov.laa.ccms.caab.bean.validators.file;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.evidence.EvidenceUploadFormData;
import uk.gov.laa.ccms.caab.bean.validators.evidence.EvidenceUploadValidator;
import uk.gov.laa.ccms.caab.security.SecurityEventLogger;

class FileUploadValidatorSecurityEventTest {

  private final SecurityEventLogger securityEventLogger = mock(SecurityEventLogger.class);
  private final FileUploadValidator validator =
      new EvidenceUploadValidator(
          List.of("pdf", "doc"),
          "20B",
          List.of("application/pdf", "application/msword", "text/plain"));

  private EvidenceUploadFormData formData;
  private Errors errors;

  @BeforeEach
  void setUp() {
    validator.setSecurityEventLogger(securityEventLogger);
    formData = new EvidenceUploadFormData();
    errors = new BeanPropertyBindingResult(formData, "evidenceUploadFormData");
  }

  @Test
  void aDisallowedExtensionIsLoggedWithItsReasonCode() {
    formData.setFile(new MockMultipartFile("file", "x.exe", "application/pdf", "data".getBytes()));

    validator.validateFile(formData, errors);

    verify(securityEventLogger).fileRejected("validation.error.invalidExtension");
  }

  @Test
  void aWrongMimeTypeIsLogged() {
    formData.setFile(
        new MockMultipartFile("file", "x.pdf", "application/x-msdownload", "data".getBytes()));

    validator.validateFile(formData, errors);

    verify(securityEventLogger).fileRejected("validation.error.invalidMimeType");
  }

  @Test
  void aMissingFileIsAnOrdinaryMistakeAndIsNotLogged() {
    validator.validateFile(formData, errors);

    verifyNoInteractions(securityEventLogger);
  }

  @Test
  void anAcceptableFileIsNotLogged() {
    formData.setFile(
        new MockMultipartFile("file", "x.pdf", "application/pdf", "the file data".getBytes()));

    validator.validateFile(formData, errors);

    verifyNoInteractions(securityEventLogger);
  }
}
