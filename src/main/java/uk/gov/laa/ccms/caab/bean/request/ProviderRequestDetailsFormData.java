package uk.gov.laa.ccms.caab.bean.request;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
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

  private boolean claimUploadEnabled;

  private String claimUploadLabel;

  private Map<String, DynamicOptionFormData> dynamicOptions = new HashMap<>();

  private String additionalInformationLabel;

  private String additionalInformation;

  private Boolean isAdditionalInformationPromptRequired;

  private Integer documentIdToDelete;

  private UUID documentSessionId;

  public ProviderRequestDetailsFormData() {
    super();
    this.documentSessionId = UUID.randomUUID();
  }

}
