package uk.gov.laa.ccms.caab.bean.validators.file;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.validation.BeanPropertyBindingResult;
import uk.gov.laa.ccms.caab.bean.evidence.EvidenceUploadFormData;
import uk.gov.laa.ccms.caab.bean.validators.evidence.EvidenceUploadValidator;
import uk.gov.laa.ccms.caab.security.SecurityEventLogger;

@SpringJUnitConfig(FileUploadValidatorWiringTest.Config.class)
@TestPropertySource(
    properties = {
      "laa.ccms.caab.upload.valid-extensions=pdf",
      "spring.servlet.multipart.max-file-size=20B",
      "laa.ccms.caab.upload.valid-mime-types=application/pdf"
    })
class FileUploadValidatorWiringTest {

  @Autowired private EvidenceUploadValidator validator;
  @Autowired private SecurityEventLogger securityEventLogger;

  @Test
  void springInjectsTheLoggerIntoTheValidators() {
    var formData = new EvidenceUploadFormData();
    formData.setFile(new MockMultipartFile("file", "x.exe", "application/pdf", "data".getBytes()));

    validator.validateFile(formData, new BeanPropertyBindingResult(formData, "form"));

    verify(securityEventLogger).fileRejected("validation.error.invalidExtension");
  }

  @Configuration
  @Import(EvidenceUploadValidator.class)
  static class Config {

    @Bean
    SecurityEventLogger securityEventLogger() {
      return mock(SecurityEventLogger.class);
    }
  }
}
