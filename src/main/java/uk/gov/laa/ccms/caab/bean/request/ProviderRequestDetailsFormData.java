package uk.gov.laa.ccms.caab.bean.request;

import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import uk.gov.laa.ccms.caab.bean.common.DynamicOptionFormData;
import uk.gov.laa.ccms.caab.bean.file.FileUploadFormData;

/**
 * Represents the form data for provider request details, including file upload options,
 * dynamic options, and additional information.
 */
@Data
@Slf4j
@EqualsAndHashCode(callSuper = false)
public class ProviderRequestDetailsFormData extends FileUploadFormData {

  private boolean fileUploadEnabled;

  private String fileUploadLabel;

  private Map<String, DynamicOptionFormData> dynamicOptions = new HashMap<>();

  private String additionalInformationLabel;

  private String additionalInformation;

}
