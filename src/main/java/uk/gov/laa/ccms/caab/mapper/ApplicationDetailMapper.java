package uk.gov.laa.ccms.caab.mapper;

import java.util.Date;
import java.util.Map;
import java.util.Set;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.context.ApplicationContext;
import uk.gov.laa.ccms.caab.model.Application;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.soa.gateway.model.CaseDetail;

@Mapper(componentModel = "spring")
public interface ApplicationDetailMapper {

  @Mapping(target = "applicationType.id", source = "applicationDetails.applicationAmendmentType")
  @Mapping(target = "dateCreated", source = "recordHistory.dateCreated")
  @Mapping(target = "provider.id", source = "applicationDetails.providerDetails.providerFirmId")
  @Mapping(target = "provider.caseReference", source = "applicationDetails.providerDetails.providerCaseReferenceNumber")
  @Mapping(target = "providerContact.id", source = "applicationDetails.providerDetails.contactUserId.userLoginId")
  @Mapping(target = "providerContact.displayValue", source = "applicationDetails.providerDetails.contactUserId.userName")
  @Mapping(target = "provider.displayValue", source = "applicationDetails.providerDetails.providerFirmId")
  Application toApplication(CaseDetail caseDetail);

//  result
//      .setProviderDisplayValue(providerDao.getProviderName(providerDetails.getProviderFirmID()));
//    result.setOfficeId(Long.valueOf(providerDetails.getProviderOfficeID()));
//    result.setOfficeDisplayValue(providerDao.getOfficeName(providerDetails.getProviderFirmID(),
//        providerDetails.getProviderOfficeID()));
//    result.setSupervisor(providerDetails.getSupervisorContactID());
//
//    if (providerDetails.getSupervisorContactID() != null
//        || providerDetails.getFeeEarnerContactID() != null) {
//      Map<String, String> feeEarnerKeyValues = lovDao.getFeeEarnerKeyValues(
//          Long.valueOf(providerDetails.getProviderFirmID()),
//          Long.valueOf(providerDetails.getProviderOfficeID()));
//      if (providerDetails.getSupervisorContactID() != null) {
//        result.setSupervisorDisplayValue(
//            feeEarnerKeyValues.get(providerDetails.getSupervisorContactID()));
//      }
//      if (providerDetails.getFeeEarnerContactID() != null) {
//        result.setFeeEarner(providerDetails.getFeeEarnerContactID());
//        result.setFeeEarnerDisplayValue(
//            feeEarnerKeyValues.get(providerDetails.getFeeEarnerContactID()));
//      }
//    }
//  }


//    ListOfValuesDao lovDao = applicationContext
//        .getBean("listOfValuesDao", ListOfValuesDao.class);
//    Set<Option> applicationTypeOptions = lovDao
//        .getOptionsByType(ListOfValuesDao.OPTION_TYPE_APPLICATION_TYPE);

//    Case result = new Application(new Client(), esbCase.getCaseReferenceNumber());
//    if (esbCase.getCaseDetails() != null) {
//      CaseDetails caseDetails = esbCase.getCaseDetails();
//      if (caseDetails.getCertificateType() != null) {
//        result.setCertificateType(caseDetails.getCertificateType());
//        result
//            .setCertificateTypeDisplayValue(
//                getValueFromOptions(applicationTypeOptions, caseDetails.getCertificateType()));
//      }
//      if (caseDetails.getRecordHistory() != null) {
//        Date dateCreated = DateUtils
//            .convertXMLGregorianCalendarToDate(caseDetails.getRecordHistory().getDateCreated());
//        result.setDateCreated(dateCreated);
//      }
//      log.debug("caseDetails.getApplicationDetails() : " + caseDetails.getApplicationDetails());
//      if (caseDetails.getApplicationDetails() != null) {






//        ApplicationDetails applicationDetails = caseDetails.getApplicationDetails();
//        setProviderDetails(result, applicationDetails.getProviderDetails());
//        setCorrespondenceAddress(result, applicationDetails);
//
//        result.setClient(convertClient(applicationDetails.getClient()));
//
//        // category of law needs to be set before converting proceedings
//        if (applicationDetails.getCategoryOfLaw() != null) {
//          result.setCategoryOfLaw(applicationDetails.getCategoryOfLaw().getCategoryOfLawCode());
//          result.setCategoryOfLawDisplayValue(
//              applicationDetails.getCategoryOfLaw().getCategoryOfLawDescription());
//          // need to map costs to case cost structure
//          convertCosts(applicationDetails.getCategoryOfLaw(), result);
//        }
//
//        if (applicationDetails.getProceedings() != null) {
//          convertProceedings(applicationDetails.getProceedings(), result);
//        }
//
//        if (applicationDetails.getMeansAssesments() != null) {
//          result.setMeansAssessment(
//              convertAssessment(applicationDetails.getMeansAssesments().getAssesmentResults(),
//                  result));
//        }
//        if (applicationDetails.getMeritsAssesments() != null) {
//          result.setMeritsAssessment(
//              convertAssessment(applicationDetails.getMeritsAssesments().getAssesmentResults(),
//                  result));
//        }
//        if (applicationDetails.getOtherParties() != null) {
//          result.setOpponents(convertOpponents(applicationDetails.getOtherParties(), result));
//        }
//        if (applicationDetails.getApplicationAmendmentType() != null) {
//          result.setApplicationType(applicationDetails.getApplicationAmendmentType());
//          result.setApplicationTypeDisplayValue(getValueFromOptions(applicationTypeOptions,
//              applicationDetails.getApplicationAmendmentType()));
//          if ((applicationDetails.getApplicationAmendmentType().equalsIgnoreCase(
//              CcmsConstants.APP_TYPE_EMERGENCY_DEVOLVED_POWERS)) || (applicationDetails
//              .getApplicationAmendmentType().equalsIgnoreCase(
//                  CcmsConstants.APP_TYPE_SUBSTANTIVE_DEVOLVED_POWERS))) {
//            result.setDevolvedPowersUsed(CcmsConstants.OPTION_VALUE_YES);
//            result.setDateDevolvedPowersUsed(
//                DateUtils.convertXMLGregorianCalendarToDate(applicationDetails
//                    .getDevolvedPowersDate()));
//          } else {
//            result.setDevolvedPowersUsed(CcmsConstants.OPTION_VALUE_NO);
//          }
//        } else if (applicationDetails.getApplicationAmendmentType() == null) {
//          if (caseDetails.getCertificateType() != null) {
//            result.setApplicationType(caseDetails.getCertificateType());
//            result.setApplicationTypeDisplayValue(
//                getValueFromOptions(applicationTypeOptions, caseDetails.getCertificateType()));
//
//          }
//        }
//        if (applicationDetails.getLARDetails() != null) {
//          if (applicationDetails.getLARDetails().isLARScopeFlag() != null) {
//            if (applicationDetails.getLARDetails().isLARScopeFlag()) {
//              result.setLarScopeFlag("Y");
//            } else {
//              result.setLarScopeFlag("N");
//            }
//          } else {
//            //setting the lar scope flag to N for pre lar cases if not received from ebs
//            result.setLarScopeFlag("N");
//          }
//        } else {
//          //setting the lar scope flag to N for pre lar cases if not received from ebs
//          result.setLarScopeFlag("N");
//        }
//      }
//
//      if (caseDetails.getLinkedCases() != null) {
//        result.setLinkedCases(convertLinkedCases(caseDetails.getLinkedCases(), result));
//      }
//
//      if (caseDetails.getCaseStatus() != null) {
//        result.setDisplayStatus(caseDetails.getCaseStatus().getDisplayCaseStatus());
//        result.setActualStatus(caseDetails.getCaseStatus().getActualCaseStatus());
//      }
//      if (caseDetails.getAvailableFunctions() != null) {
//        result.setAvailableFunctions(caseDetails.getAvailableFunctions().getFunction());
//      }
//      if (caseDetails.getPriorAuthorities() != null) {
//        result.setPriorAuthorities(
//            convertPriorAuthorities(caseDetails.getPriorAuthorities(), result));
//      }
//
//      result.setCaseOutcome(convertCaseOutcome(caseDetails, result));
//
//    }
//    return result;
//  }




}
