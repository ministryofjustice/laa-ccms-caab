package uk.gov.laa.ccms.caab.mapper;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;
import uk.gov.laa.ccms.caab.bean.common.DynamicOptionFormData;
import uk.gov.laa.ccms.caab.bean.evidence.EvidenceUploadFormData;
import uk.gov.laa.ccms.caab.bean.request.ProviderRequestDetailsFormData;
import uk.gov.laa.ccms.caab.bean.request.ProviderRequestFlowFormData;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.mapper.context.ProviderRequestMappingContext;
import uk.gov.laa.ccms.caab.model.EvidenceDocumentDetail;
import uk.gov.laa.ccms.data.model.ProviderRequestDataLookupValueDetail;
import uk.gov.laa.ccms.data.model.ProviderRequestTypeLookupValueDetail;
import uk.gov.laa.ccms.soa.gateway.model.ProviderRequestAttribute;
import uk.gov.laa.ccms.soa.gateway.model.ProviderRequestDetail;

/**
 * Maps and populates provider request details form data between related objects.
 */
@Mapper(componentModel = "spring", uses = CommonMapper.class)
public interface ProviderRequestsMapper {

  CommonMapper COMMON_MAPPER = Mappers.getMapper(CommonMapper.class);

  String DATE_DYNAMIC_OPTION = "DAT";

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
  @Mapping(target = "documentIdToDelete", ignore = true)
  @Mapping(target = "claimUploadLabel",
      source = "providerRequestFlowFormData.requestDetailsFormData.claimUploadLabel")
  @Mapping(target = "additionalInformationLabel",
      source = "providerRequestFlowFormData.requestDetailsFormData.additionalInformationLabel")
  @Mapping(target = "claimUploadEnabled",
      source = "providerRequestFlowFormData.requestDetailsFormData.claimUploadEnabled")
  @Mapping(target = "documentSessionId",
      source = "providerRequestFlowFormData.requestDetailsFormData.documentSessionId")
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

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "auditTrail", ignore = true)
  @Mapping(target = "notificationReference", ignore = true)
  @Mapping(target = "transferStatus", ignore = true)
  @Mapping(target = "transferResponseCode", ignore = true)
  @Mapping(target = "transferResponseDescription", ignore = true)
  @Mapping(target = "evidenceDescriptions", ignore = true)
  @Mapping(target = "transferRetryCount", constant = "0")
  @Mapping(target = "fileName", source = "file.originalFilename")
  @Mapping(target = "fileData", source = "file")
  @Mapping(target = "description", source = "documentDescription")
  @Mapping(target = "documentType.id", source = "documentType")
  @Mapping(target = "documentType.displayValue", source = "documentTypeDisplayValue")
  @Mapping(target = "ccmsModule", source = "ccmsModule.code")
  EvidenceDocumentDetail toProviderRequestDocumentDetail(final EvidenceUploadFormData formData);



  //todo amend mapping for case related requests (future story)
  @Mapping(target = "caseReferenceNumber", ignore = true)
  @Mapping(target = "username", source = "user.username")
  @Mapping(target = "requestType",
      source = "typeData.providerRequestType")
  @Mapping(target = "attributes",
      source = "detailsData",
      qualifiedByName = "toProviderRequestDetailAttributes")
  ProviderRequestDetail toProviderRequestDetail(
      final ProviderRequestMappingContext mappingContext);

  @Mapping(target = "label", source = "fieldDescription")
  @Mapping(target = "text", expression = "java(formatDynamicOption(dynamicOption))")
  @Mapping(target = "document", ignore = true)
  ProviderRequestAttribute dynamicOptionToProviderRequestAttribute(
      DynamicOptionFormData dynamicOption);

  /**
   * Formats the date field value from the provided dynamic option.
   *
   * @param dynamicOption the dynamic option containing the date field
   * @return the formatted date as a string in 'yyyy-MM-dd' format,
   *         or the original value if parsing fails
   */
  default String formatDynamicOption(final DynamicOptionFormData dynamicOption) {
    if (DATE_DYNAMIC_OPTION.equals(dynamicOption.getFieldType())
        && dynamicOption.getFieldValue() != null) {
      try {
        final DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        final DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        final LocalDate date = LocalDate.parse(dynamicOption.getFieldValue(), inputFormatter);
        return date.format(outputFormatter);
      } catch (final DateTimeParseException e) {
        // Handle the exception or return the original value
        return dynamicOption.getFieldValue();
      }
    }
    return dynamicOption.getFieldValue();
  }

  @Mapping(target = "label", source = "additionalInformationLabel")
  @Mapping(target = "text", source = "additionalInformation")
  @Mapping(target = "document", ignore = true)
  ProviderRequestAttribute additionalInfoToProviderRequestAttribute(
      ProviderRequestDetailsFormData formData);

  @Mapping(target = "label", source = "claimUploadLabel")
  @Mapping(target = "text", source = "file.originalFilename")
  @Mapping(target = "document",
      expression = "java(COMMON_MAPPER.toBase64EncodedStringFromByteArray("
          + "COMMON_MAPPER.toFileBytes(formData.getFile())))")
  ProviderRequestAttribute fileToProviderRequestAttribute(ProviderRequestDetailsFormData formData)
      throws CaabApplicationException;

  /**
   * Converts form data into a list of provider request attributes.
   *
   * @param formData the form data to be converted
   * @return a list of {@code ProviderRequestAttribute} objects derived from the form data
   */
  @Named("toProviderRequestDetailAttributes")
  default List<ProviderRequestAttribute> toProviderRequestDetailAttributes(
      final ProviderRequestDetailsFormData formData) {
    if (formData == null) {
      return null;
    }

    final List<ProviderRequestAttribute> attributes = new ArrayList<>();

    if (formData.getDynamicOptions() != null) {
      formData.getDynamicOptions().values().forEach(dynamicOption -> {
        attributes.add(dynamicOptionToProviderRequestAttribute(dynamicOption));
      });
    }

    if (formData.getAdditionalInformationLabel() != null) {
      attributes.add(additionalInfoToProviderRequestAttribute(formData));
    }

    if (formData.isClaimUploadEnabled() && formData.getFile() != null) {
      try {
        attributes.add(fileToProviderRequestAttribute(formData));
      } catch (CaabApplicationException e) {
        throw new RuntimeException(e);
      }
    }

    return attributes;
  }

}
