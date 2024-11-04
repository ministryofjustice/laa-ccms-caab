package uk.gov.laa.ccms.caab.mapper;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import uk.gov.laa.ccms.caab.bean.common.DynamicOptionFormData;
import uk.gov.laa.ccms.caab.bean.priorauthority.PriorAuthorityDetailsFormData;
import uk.gov.laa.ccms.caab.bean.priorauthority.PriorAuthorityFlowFormData;
import uk.gov.laa.ccms.caab.bean.request.ProviderRequestDetailsFormData;
import uk.gov.laa.ccms.caab.bean.request.ProviderRequestFlowFormData;
import uk.gov.laa.ccms.data.model.ProviderRequestDataLookupValueDetail;
import uk.gov.laa.ccms.data.model.ProviderRequestTypeLookupValueDetail;

/**
 * Maps and populates provider request details form data between related objects.
 */
@Mapper(componentModel = "spring")
public interface ProviderRequestsMapper {

  /**
   * Populates the dynamic options in provider request details form data using the specified
   * provider request type details.
   *
   * @param providerRequestDetailsFormData the form data to be populated.
   * @param providerRequestType the source type containing dynamic option details.
   */
  @AfterMapping
  default void populateProviderRequestDetailsForm(
      @MappingTarget final ProviderRequestDetailsFormData providerRequestDetailsFormData,
      final ProviderRequestTypeLookupValueDetail providerRequestType) {

    for (final ProviderRequestDataLookupValueDetail formOption :
        providerRequestType.getDataItems()) {
      final DynamicOptionFormData dynamicOption = toDynamicOption(formOption);
      providerRequestDetailsFormData.getDynamicOptions().put(formOption.getCode(), dynamicOption);
    }
  }

  /**
   * Maps a single form option to a dynamic option form data object.
   *
   * @param formOption provider request form option to map.
   * @return the mapped DynamicOptionFormData instance.
   */
  @Mapping(target = "mandatory", source = "mandatoryFlag")
  @Mapping(target = "fieldDescription", source = "label")
  @Mapping(target = "fieldType", source = "type")
  @Mapping(target = "fieldValue", ignore = true)
  @Mapping(target = "fieldValueDisplayValue", ignore = true)
  DynamicOptionFormData toDynamicOption(
      ProviderRequestDataLookupValueDetail formOption);

  /**
   * Maps provider request flow form data to provider request details form data,
   * ignoring pre-populated fields.
   *
   * @param providerRequestDetails the target form data to map to.
   * @param providerRequestFlowFormData the source flow form data.
   */
  @Mapping(target = "dynamicOptions", ignore = true)
  @Mapping(target = "additionalInformation", ignore = true)
  @Mapping(target = "file", ignore = true)
  @Mapping(target = "fileExtension", ignore = true)
  @Mapping(target = "documentType", ignore = true)
  @Mapping(target = "documentTypeDisplayValue", ignore = true)
  @Mapping(target = "documentDescription", ignore = true)
  @Mapping(target = "fileUploadLabel",
      source = "providerRequestFlowFormData.requestDetailsFormData.fileUploadLabel")
  @Mapping(target = "additionalInformationLabel",
      source = "providerRequestFlowFormData.requestDetailsFormData.additionalInformationLabel")
  @Mapping(target = "fileUploadEnabled",
      source = "providerRequestFlowFormData.requestDetailsFormData.fileUploadEnabled")
  void toProviderRequestDetailsFormData(
      @MappingTarget ProviderRequestDetailsFormData providerRequestDetails,
      ProviderRequestFlowFormData providerRequestFlowFormData);

  /**
   * Maps dynamic options from request flow form data to provider request details.
   *
   * @param providerRequestDetails the target provider request details form data.
   * @param providerRequestFlowFormData the source provider request flow form data.
   */
  @AfterMapping
  default void mapDynamicOptions(
      @MappingTarget final ProviderRequestDetailsFormData providerRequestDetails,
      final ProviderRequestFlowFormData providerRequestFlowFormData) {

    if (providerRequestFlowFormData.getRequestDetailsFormData().getDynamicOptions() != null) {
      providerRequestFlowFormData.getRequestDetailsFormData()
          .getDynamicOptions().forEach((key, value) -> {
            if (providerRequestDetails.getDynamicOptions().containsKey(key)) {
              providerRequestDetails.getDynamicOptions().get(key)
                  .setMandatory(value.isMandatory());
              providerRequestDetails.getDynamicOptions().get(key)
                  .setFieldDescription(value.getFieldDescription());
              providerRequestDetails.getDynamicOptions().get(key)
                  .setFieldType(value.getFieldType());
            }
          });
    }
  }
}
