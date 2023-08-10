package uk.gov.laa.ccms.caab.util;

import uk.gov.laa.ccms.caab.bean.ApplicationDetails;
import uk.gov.laa.ccms.caab.model.*;
import uk.gov.laa.ccms.data.model.*;
import uk.gov.laa.ccms.soa.gateway.model.CaseReferenceSummary;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetail;
import uk.gov.laa.ccms.soa.gateway.model.ContractDetail;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.*;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.APP_TYPE_EXCEPTIONAL_CASE_FUNDING_DISPLAY;

public class ApplicationBuilder {

    private final ApplicationDetail application;

    public ApplicationBuilder() {
        this.application = new ApplicationDetail(null, null, null,null);
    }

    public ApplicationBuilder(ApplicationDetail application) {
        this.application = application;
    }

    public ApplicationBuilder applicationType(String applicationTypeCategory, boolean isDelegatedFunctions) {
        StringDisplayValue applicationType = new StringDisplayValue();
        if (APP_TYPE_SUBSTANTIVE.equals(applicationTypeCategory)) {
            applicationType.setId(isDelegatedFunctions ? APP_TYPE_SUBSTANTIVE_DEVOLVED_POWERS : APP_TYPE_SUBSTANTIVE);
            applicationType.setDisplayValue(isDelegatedFunctions ? APP_TYPE_SUBSTANTIVE_DEVOLVED_POWERS_DISPLAY : APP_TYPE_SUBSTANTIVE_DISPLAY);
        } else if (APP_TYPE_EMERGENCY.equals(applicationTypeCategory)){
            applicationType.setId(isDelegatedFunctions ? APP_TYPE_EMERGENCY_DEVOLVED_POWERS : APP_TYPE_EMERGENCY);
            applicationType.setDisplayValue(isDelegatedFunctions ? APP_TYPE_EMERGENCY_DEVOLVED_POWERS_DISPLAY : APP_TYPE_EMERGENCY_DISPLAY);
        } else {
            applicationType.setId(APP_TYPE_EXCEPTIONAL_CASE_FUNDING);
            applicationType.setDisplayValue(APP_TYPE_EXCEPTIONAL_CASE_FUNDING_DISPLAY);
        }
        application.setApplicationType(applicationType);
        return this;
    }

    public ApplicationBuilder caseReference(CaseReferenceSummary caseReferenceSummary) {
        String caseReference = Optional.ofNullable(caseReferenceSummary.getCaseReferenceNumber())
                .orElseThrow(() -> new RuntimeException("No case reference number was created, unable to continue"));
        application.setCaseReferenceNumber(caseReference);
        return this;
    }

    public ApplicationBuilder provider(UserDetail user) {
        ApplicationDetailProvider provider = new ApplicationDetailProvider(user.getProvider().getId())
                .displayValue(user.getProvider().getName());
        application.setProvider(provider);
        return this;
    }

    public ApplicationBuilder client(ClientDetail clientInformation) {
        ApplicationDetailClient client = new ApplicationDetailClient()
                .firstName(clientInformation.getDetails().getName().getFirstName())
                .surname(clientInformation.getDetails().getName().getSurname())
                .reference(clientInformation.getClientReferenceNumber());
        application.setClient(client);
        return this;
    }

    public ApplicationBuilder categoryOfLaw(String categoryOfLawId, CommonLookupDetail categoryOfLawLookupDetail) {
        String categoryOfLawDisplayValue = Optional.of(categoryOfLawLookupDetail)
                .map(CommonLookupDetail::getContent)
                .orElse(Collections.emptyList())
                .stream()
                .filter(category -> categoryOfLawId.equals(category.getCode()))
                .map(CommonLookupValueDetail::getDescription) // Assuming getDisplayValue() gets the display value
                .findFirst()
                .orElse(null);

        StringDisplayValue categoryOfLaw = new StringDisplayValue()
                .id(categoryOfLawId)
                .displayValue(categoryOfLawDisplayValue);
        application.setCategoryOfLaw(categoryOfLaw);
        return this;
    }

    public ApplicationBuilder office(Integer officeId, List<OfficeDetail> offices) {
        String officeDisplayValue = offices.stream()
                .filter(office -> officeId.equals(office.getId()))
                .map(OfficeDetail::getName)
                .findFirst()
                .orElse(null);

        IntDisplayValue office = new IntDisplayValue()
                .id(officeId)
                .displayValue(officeDisplayValue);
        application.setOffice(office);
        return this;
    }

    public ApplicationBuilder devolvedPowers(List<ContractDetail> contractDetails, ApplicationDetails applicationDetails) throws ParseException {
        ApplicationDetailDevolvedPowers devolvedPowers = new ApplicationDetailDevolvedPowers();

        String contractualDevolvedPower = contractDetails != null ? contractDetails.stream()
                .filter(contract -> applicationDetails.getCategoryOfLawId().equals(contract.getCategoryofLaw()))
                .map(ContractDetail::getContractualDevolvedPowers)
                .findFirst()
                .orElse(null)
                : null;

        devolvedPowers.setContractFlag(contractualDevolvedPower);
        devolvedPowers.setUsed(applicationDetails.isDelegatedFunctions());

        if (applicationDetails.isDelegatedFunctions()){
            String dateString = applicationDetails.getDelegatedFunctionUsedDay() + "-" +
                    applicationDetails.getDelegatedFunctionUsedMonth() + "-" +
                    applicationDetails.getDelegatedFunctionUsedYear();
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
            devolvedPowers.setDateUsed(sdf.parse(dateString));
        }
        application.setDevolvedPowers(devolvedPowers);
        return this;
    }

    public ApplicationBuilder larScopeFlag(AmendmentTypeLookupDetail amendmentTypes) {
        String defaultLarScopeFlag = Optional.ofNullable(amendmentTypes.getContent())
                .filter(content -> !content.isEmpty())
                .map(content -> content.get(0))
                .map(AmendmentTypeLookupValueDetail::getDefaultLarScopeFlag)
                .orElseThrow(() -> new RuntimeException("No amendment type available, unable to continue"));
        application.setLarScopeFlag(defaultLarScopeFlag);
        return this;
    }

    public ApplicationBuilder status() {
        StringDisplayValue status = new StringDisplayValue()
                .id(STATUS_UNSUBMITTED_ACTUAL_VALUE)
                .displayValue(STATUS_UNSUBMITTED_ACTUAL_VALUE_DISPLAY);
        application.setStatus(status);
        return this;
    }


    public ApplicationDetail build() {
        return application;
    }

}
