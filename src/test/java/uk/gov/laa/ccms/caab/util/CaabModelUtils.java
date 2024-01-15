package uk.gov.laa.ccms.caab.util;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import uk.gov.laa.ccms.caab.model.Address;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.ApplicationProviderDetails;
import uk.gov.laa.ccms.caab.model.ApplicationType;
import uk.gov.laa.ccms.caab.model.AssessmentResult;
import uk.gov.laa.ccms.caab.model.AuditDetail;
import uk.gov.laa.ccms.caab.model.BaseApplication;
import uk.gov.laa.ccms.caab.model.BooleanDisplayValue;
import uk.gov.laa.ccms.caab.model.CaseOutcome;
import uk.gov.laa.ccms.caab.model.Client;
import uk.gov.laa.ccms.caab.model.CostEntry;
import uk.gov.laa.ccms.caab.model.CostLimit;
import uk.gov.laa.ccms.caab.model.CostStructure;
import uk.gov.laa.ccms.caab.model.DevolvedPowers;
import uk.gov.laa.ccms.caab.model.IntDisplayValue;
import uk.gov.laa.ccms.caab.model.LinkedCase;
import uk.gov.laa.ccms.caab.model.Opponent;
import uk.gov.laa.ccms.caab.model.PriorAuthority;
import uk.gov.laa.ccms.caab.model.Proceeding;
import uk.gov.laa.ccms.caab.model.ProceedingOutcome;
import uk.gov.laa.ccms.caab.model.ScopeLimitation;
import uk.gov.laa.ccms.caab.model.StringDisplayValue;

public class CaabModelUtils {

  public static BaseApplication buildBaseApplication(Integer id) {
    return new BaseApplication()
        .caseReferenceNumber(id + "")
        .categoryOfLaw(new StringDisplayValue().id(id + "cat1").displayValue(id + "catoflaw1"))
        .client(new Client().firstName(id + "firstname"))
        .providerDetails(new ApplicationProviderDetails()
            .provider(new IntDisplayValue().id(id).displayValue("Client " + id))
            .feeEarner(new StringDisplayValue().id("fee" + id).displayValue("Fee " + id))
            .supervisor(new StringDisplayValue().id("sup" + id).displayValue("super " + id))
            .office(new IntDisplayValue().id(id).displayValue("Office " + id))
            .providerCaseReference("provcaseref" + id)
            .providerContact(new StringDisplayValue().id("prov" + id).displayValue("provcontact " + id)))
        .status(new StringDisplayValue().id("st" + id).displayValue("status " + id));
  }

  public static ApplicationDetail buildApplicationDetail(Integer id, Boolean flag, java.util.Date date) {
    return new ApplicationDetail()
        .caseReferenceNumber(id + "")
        .categoryOfLaw(new StringDisplayValue().id(id + "cat1").displayValue(id + "catoflaw1"))
        .client(new Client().firstName(id + "firstname"))
        .providerDetails(new ApplicationProviderDetails()
            .provider(new IntDisplayValue().id(id).displayValue("Client " + id))
            .feeEarner(new StringDisplayValue().id("fee" + id).displayValue("Fee " + id))
            .supervisor(new StringDisplayValue().id("sup" + id).displayValue("super " + id))
            .office(new IntDisplayValue().id(id).displayValue("Office " + id))
            .providerCaseReference("provcaseref" + id)
            .providerContact(new StringDisplayValue().id("prov" + id).displayValue("provcontact " + id)))
        .allSectionsComplete(flag)
        .amendment(flag)
        .addAmendmentProceedingsInEbsItem(buildProceeding(date, BigDecimal.ONE))
        .applicationType(new ApplicationType()
            .id("type" + id)
            .displayValue("the type " + id)
            .devolvedPowers(new DevolvedPowers()
                .used(flag)
                .dateUsed(flag ? date : null)
                .contractFlag("flag" + id)))
        .appMode(flag)
        .auditTrail(new AuditDetail().createdBy("person" + id))
        .availableFunctions(new ArrayList<>(Arrays.asList("a" + id, "b" + id, "c" + id)))
        .award(flag)
        .caseOutcome(new CaseOutcome()
            .caseReferenceNumber(id + ""))
        .certificate(new StringDisplayValue().id("cert" + id).displayValue("certificate " + id))
        .correspondenceAddress(new Address().addressLine1("addressline1, " + id))
        .costLimit(new CostLimit().limitAtTimeOfMerits(BigDecimal.ONE).changed(Boolean.TRUE))
        .costs(new CostStructure()
            .auditTrail(new AuditDetail().createdBy("user" + id))
            .addCostEntriesItem(new CostEntry()
                .amountBilled(BigDecimal.TEN)
                .costCategory("costcat")
                .ebsId("ebsId")
                .lscResourceId("lscResource")
                .newEntry(Boolean.TRUE)
                .requestedCosts(BigDecimal.ONE)
                .resourceName("resname")
                .submitted(Boolean.TRUE))
            .currentProviderBilledAmount(BigDecimal.ONE)
            .grantedCostLimitation(BigDecimal.ZERO)
            .defaultCostLimitation(BigDecimal.ONE)
            .requestedCostLimitation(BigDecimal.TEN))
        .dateCreated(date)
        .editProceedingsAndCostsAllowed(flag)
        .larScopeFlag(flag)
        .leadProceedingChanged(flag)
        .linkedCases(new ArrayList<>(Collections.singletonList(new LinkedCase())))
        .meansAssessment(new AssessmentResult())
        .meansAssessmentAmended(flag)
        .meansAssessmentStatus("stat" + id)
        .meritsAssessment(new AssessmentResult())
        .meritsAssessmentAmended(flag)
        .meritsAssessmentStatus("merStat" + id)
        .meritsReassessmentRequired(flag)
        .opponentAppliedForFunding(flag)
        .opponentMode("mode" + id)
        .addOpponentsItem(buildOpponent(date))
        .addPriorAuthoritiesItem(new PriorAuthority())
        .addProceedingsItem(buildProceeding(date, BigDecimal.ONE))
        .addProceedingsItem(buildProceeding(date, BigDecimal.TEN))
        .quickEditType("quicktype" + id)
        .relationToLinkedCase("rel" + id)
        .status(new StringDisplayValue().id("st" + id).displayValue("status " + id))
        .submitted(flag);
  }

  public static Proceeding buildProceeding(java.util.Date date, BigDecimal costLimitation) {
    return new Proceeding()
        .auditTrail(new AuditDetail())
        .availableFunctions(new ArrayList<>(Arrays.asList("a", "b", "c")))
        .clientInvolvement(new StringDisplayValue().id("clientInv").displayValue("client inv"))
        .costLimitation(costLimitation)
        .dateCostsValid(date)
        .dateDevolvedPowersUsed(date)
        .dateGranted(date)
        .defaultScopeLimitation("defScope")
        .deleteScopeLimitationFlag(Boolean.TRUE)
        .description("descr")
        .ebsId("theebsid")
        .edited(Boolean.TRUE)
        .grantedUsingDevolvedPowers("granted")
        .larScope("scope")
        .leadProceedingInd(Boolean.TRUE)
        .levelOfService(new StringDisplayValue().id("los").displayValue("levelofser"))
        .matterType(new StringDisplayValue().id("mat").displayValue("matter"))
        .orderTypeDisplayFlag(Boolean.TRUE)
        .orderTypeReqFlag(Boolean.TRUE)
        .outcome(buildProceedingOutcome(date))
        .proceedingCaseId("caseid")
        .proceedingType(new StringDisplayValue().id("type").displayValue("thetype"))
        .scopeLimitations(new ArrayList<>(Collections.singletonList(buildScopeLimitation())))
        .stage("thestage")
        .status(new StringDisplayValue().id("stat").displayValue("the status"))
        .typeOfOrder(new StringDisplayValue().id("too").displayValue("type of order"));
  }

  public static ProceedingOutcome buildProceedingOutcome(java.util.Date date) {
    return new ProceedingOutcome()
        .adrInfo("adr")
        .alternativeResolution("alt")
        .courtCode("court")
        .courtName("thecourt")
        .dateOfFinalWork(date)
        .dateOfIssue(date)
        .description("descr")
        .matterType(new StringDisplayValue().id("mat1").displayValue("matter one"))
        .outcomeCourtCaseNo("number")
        .proceedingCaseId("caseid")
        .proceedingType(new StringDisplayValue().id("procType").displayValue("proc type"))
        .resolutionMethod("resMeth")
        .result(new StringDisplayValue().id("res").displayValue("the result"))
        .resultInfo("resInf")
        .stageEnd(new StringDisplayValue().id("stg").displayValue("stage end"))
        .widerBenefits("widerBens");
  }

  public static ScopeLimitation buildScopeLimitation() {
    return new ScopeLimitation()
        .ebsId("ebsid")
        .delegatedFuncApplyInd(new BooleanDisplayValue()
            .flag(Boolean.TRUE)
            .displayValue("delfunc"))
        .scopeLimitation(new StringDisplayValue().id("scopelim").displayValue("scope lim"))
        .scopeLimitationWording("wording")
        .stage("stage")
        .nonDefaultWordingReqd(Boolean.TRUE);
  }

  public static Opponent buildOpponent(java.util.Date date) {
    return new Opponent()
        .address(new Address().addressLine1("add1"))
        .amendment(Boolean.TRUE)
        .appMode(Boolean.FALSE)
        .assessedAssets(BigDecimal.TEN)
        .assessedIncome(BigDecimal.ONE)
        .assessedIncomeFrequency("freq")
        .assessmentDate(date)
        .auditTrail(new AuditDetail())
        .award(Boolean.TRUE)
        .certificateNumber("cert")
        .confirmed(Boolean.FALSE)
        .contactNameRole("conNameRole")
        .courtOrderedMeansAssessment(Boolean.TRUE)
        .currentlyTrading(Boolean.TRUE)
        .dateOfBirth(date)
        .deleteInd(Boolean.FALSE)
        .displayAddress("address 1")
        .displayName("disp name")
        .ebsId("ebsid")
        .emailAddress("emailAdd")
        .employerName("empName")
        .employerAddress("empAddr")
        .employmentStatus("empSt")
        .faxNumber("fax")
        .firstName("firstname")
        .legalAided(Boolean.TRUE)
        .middleNames("midnames")
        .nationalInsuranceNumber("nino")
        .organisationName("orgName")
        .organisationType(new StringDisplayValue().id("orgid").displayValue("org"))
        .otherInformation("otherInf")
        .partyId("party")
        .publicFundingApplied(Boolean.TRUE)
        .relationshipToCase("relToCase")
        .relationshipToClient("relToClient")
        .sharedInd(Boolean.TRUE)
        .surname("surname")
        .telephoneHome("telHome")
        .telephoneMobile("telMob")
        .telephoneWork("telWork")
        .title("thetitle")
        .type("thetype");
  }



}
