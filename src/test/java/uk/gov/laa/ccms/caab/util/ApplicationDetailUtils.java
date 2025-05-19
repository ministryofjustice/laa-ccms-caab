package uk.gov.laa.ccms.caab.util;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import uk.gov.laa.ccms.caab.model.AddressDetail;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.ApplicationProviderDetails;
import uk.gov.laa.ccms.caab.model.ApplicationType;
import uk.gov.laa.ccms.caab.model.ClientDetail;
import uk.gov.laa.ccms.caab.model.CostStructureDetail;
import uk.gov.laa.ccms.caab.model.IntDisplayValue;
import uk.gov.laa.ccms.caab.model.LinkedCaseDetail;
import uk.gov.laa.ccms.caab.model.OpponentDetail;
import uk.gov.laa.ccms.caab.model.ProceedingDetail;
import uk.gov.laa.ccms.caab.model.StringDisplayValue;
import uk.gov.laa.ccms.caab.model.sections.ApplicationSectionDisplay;
import uk.gov.laa.ccms.caab.model.sections.ApplicationSectionStatusDisplay;
import uk.gov.laa.ccms.caab.model.sections.ApplicationTypeSectionDisplay;
import uk.gov.laa.ccms.caab.model.sections.ClientSectionDisplay;
import uk.gov.laa.ccms.caab.model.sections.GeneralDetailsSectionDisplay;
import uk.gov.laa.ccms.caab.model.sections.LinkedCaseDisplay;
import uk.gov.laa.ccms.caab.model.sections.LinkedCasesDisplaySection;
import uk.gov.laa.ccms.caab.model.sections.OpponentSectionDisplay;
import uk.gov.laa.ccms.caab.model.sections.OpponentsSectionDisplay;
import uk.gov.laa.ccms.caab.model.sections.ProceedingSectionDisplay;
import uk.gov.laa.ccms.caab.model.sections.ProceedingsAndCostsSectionDisplay;
import uk.gov.laa.ccms.caab.model.sections.ProviderSectionDisplay;

public final class ApplicationDetailUtils {

  public static ApplicationDetail buildFullApplicationDetail() {
    ApplicationDetail applicationDetail = new ApplicationDetail();

    // Basic fields
    applicationDetail.setId(123);
    applicationDetail.setCaseReferenceNumber("CASE-456");

    // Provider Details
    ApplicationProviderDetails providerDetails = new ApplicationProviderDetails();
    providerDetails.setProvider(new IntDisplayValue().id(1).displayValue("Provider Name"));
    providerDetails.setOffice(new IntDisplayValue().id(2).displayValue("Office Name"));
    providerDetails.setFeeEarner(new StringDisplayValue().id("3").displayValue("Fee Earner"));
    providerDetails.setSupervisor(new StringDisplayValue().id("4").displayValue("Supervisor"));
    providerDetails.setProviderCaseReference("PROV-REF-789");
    applicationDetail.setProviderDetails(providerDetails);

    // Client
    ClientDetail client = new ClientDetail();
    client.setFirstName("Jane");
    client.setSurname("Doe");
    applicationDetail.setClient(client);

    // Application Type
    ApplicationType applicationType = new ApplicationType();
    applicationType.setId("APP-TYPE-1");
    applicationType.setDisplayValue("Substantive");
    applicationDetail.setApplicationType(applicationType);

    // Address
    AddressDetail address = new AddressDetail();
    address.setAddressLine1("123 Test Street");
    address.setAddressLine2("Apt 4");
    address.setCity("Testville");
    address.setPostcode("TE5 7ST");
    address.setPreferredAddress("HOME");
    applicationDetail.setCorrespondenceAddress(address);

    // Costs
    CostStructureDetail costs = new CostStructureDetail();
    costs.setRequestedCostLimitation(new BigDecimal("1500.00"));
    costs.setDefaultCostLimitation(new BigDecimal("2000.00"));
    applicationDetail.setCosts(costs);

    // Proceedings
    ProceedingDetail proceeding = new ProceedingDetail();
    proceeding.setId(1);
    proceeding.setLeadProceedingInd(true);
    proceeding.setCostLimitation(new BigDecimal("1000.00"));
    proceeding.setMatterType(new StringDisplayValue().id("MAT-TYPE-1").displayValue("FAMILY"));
    proceeding.setStatus(new StringDisplayValue().displayValue("SUBMITTED"));
    applicationDetail.setProceedings(List.of(proceeding));

    // Opponents
    OpponentDetail opponent = new OpponentDetail();
    opponent.setId(1);
    opponent.setType("INDIVIDUAL");
    opponent.setFirstName("John");
    opponent.setSurname("Smith");
    opponent.setTitle("Mr");
    opponent.setOrganisationName("Opponent Org");
    opponent.setRelationshipToCase("REL1");
    opponent.setRelationshipToClient("REL2");
    applicationDetail.setOpponents(List.of(opponent));

    // Prior Authorities
    applicationDetail.setPriorAuthorities(new ArrayList<>());

    // Linked Cases
    LinkedCaseDetail linkedCase = new LinkedCaseDetail();
    linkedCase.setLscCaseReference("LINK-CASE-1");
    linkedCase.setRelationToCase("LEGAL");
    applicationDetail.setLinkedCases(List.of(linkedCase));

    return applicationDetail;
  }

  public static ApplicationSectionDisplay expectedApplicationSectionDisplay() {
    return ApplicationSectionDisplay.builder()
        .caseReferenceNumber("CASE-456")
        .applicationType(
            ApplicationTypeSectionDisplay.builder()
                .description("Substantive")
                .devolvedPowersUsed(null)
                .enabled(true)
                .devolvedPowersDate(null)
                .build()
        )
        .provider(
            ProviderSectionDisplay.builder()
                .providerName("Provider Name")
                .providerCaseReferenceNumber("PROV-REF-789")
                .providerContactName("")
                .officeName("Office Name")
                .feeEarner("Fee Earner")
                .supervisorName("Supervisor")
                .status("Started")
                .build()
        )
        .generalDetails(
            GeneralDetailsSectionDisplay.builder()
                .applicationStatus("")
                .categoryOfLaw("")
                .status("Complete")
                .correspondenceMethod("correspondence method1")
                .build()
        )
        .client(
            ClientSectionDisplay.builder()
                .clientFullName("Jane Doe")
                .clientReferenceNumber(null)
                .build()
        )
        .proceedingsAndCosts(
            ProceedingsAndCostsSectionDisplay.builder()
                .requestedCostLimitation(new BigDecimal("1500.00"))
                .grantedCostLimitation(null)
                .proceedings(List.of(
                    ProceedingSectionDisplay.builder()
                        .proceedingType("")
                        .matterType("FAMILY")
                        .levelOfService("")
                        .clientInvolvement("")
                        .status("SUBMITTED")
                        .scopeLimitations(null)
                        .build()
                )).status("Not started")
                .build()
        )
        .priorAuthorities(Collections.emptyList())
        .opponentsAndOtherParties(
            OpponentsSectionDisplay.builder()
                .opponents(List.of(
                    OpponentSectionDisplay.builder()
                        .partyName("Mr John Smith")
                        .partyType("INDIVIDUAL")
                        .relationshipToCase("REL1")
                        .relationshipToClient("REL2")
                        .build()
                )).status("Started")
                .build()
        )
        .meansAssessment(
            ApplicationSectionStatusDisplay.builder()
                .status(null)
                .lastSaved(null)
                .lastSavedBy(null)
                .enabled(false)
                .build()
        )
        .meritsAssessment(
            ApplicationSectionStatusDisplay.builder()
                .status(null)
                .lastSaved(null)
                .lastSavedBy(null)
                .enabled(false)
                .build()
        )
        .documentUpload(
            ApplicationSectionStatusDisplay.builder()
                .status(null)
                .lastSaved(null)
                .lastSavedBy(null)
                .enabled(false)
                .build()
        )
        .linkedCasesDisplaySection(
            LinkedCasesDisplaySection.builder()
                .linkedCases(List.of(
                    new LinkedCaseDisplay("LINK-CASE-1", "Linked Legal Issue")
                ))
                .build()
        )
        .build();
  }
}
