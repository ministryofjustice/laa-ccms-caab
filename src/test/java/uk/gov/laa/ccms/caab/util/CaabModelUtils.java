package uk.gov.laa.ccms.caab.util;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import uk.gov.laa.ccms.caab.model.AddressDetail;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.ApplicationProviderDetails;
import uk.gov.laa.ccms.caab.model.ApplicationType;
import uk.gov.laa.ccms.caab.model.AssessmentResult;
import uk.gov.laa.ccms.caab.model.AuditDetail;
import uk.gov.laa.ccms.caab.model.BaseApplicationDetail;
import uk.gov.laa.ccms.caab.model.BooleanDisplayValue;
import uk.gov.laa.ccms.caab.model.CaseOutcomeDetail;
import uk.gov.laa.ccms.caab.model.ClientDetail;
import uk.gov.laa.ccms.caab.model.CostEntryDetail;
import uk.gov.laa.ccms.caab.model.CostLimitDetail;
import uk.gov.laa.ccms.caab.model.CostStructureDetail;
import uk.gov.laa.ccms.caab.model.DevolvedPowersDetail;
import uk.gov.laa.ccms.caab.model.IntDisplayValue;
import uk.gov.laa.ccms.caab.model.LinkedCaseDetail;
import uk.gov.laa.ccms.caab.model.OpponentDetail;
import uk.gov.laa.ccms.caab.model.PriorAuthorityDetail;
import uk.gov.laa.ccms.caab.model.ProceedingDetail;
import uk.gov.laa.ccms.caab.model.ProceedingOutcomeDetail;
import uk.gov.laa.ccms.caab.model.ScopeLimitationDetail;
import uk.gov.laa.ccms.caab.model.StringDisplayValue;

public final class CaabModelUtils {

  public static BaseApplicationDetail buildBaseApplication(Integer id) {
    return new BaseApplicationDetail()
        .caseReferenceNumber(id + "")
        .categoryOfLaw(new StringDisplayValue().id(id + "cat1").displayValue(id + "catoflaw1"))
        .client(new ClientDetail().firstName(id + "firstname").reference(id + "ref"))
        .providerDetails(buildApplicationProviderDetails(id))
        .status(new StringDisplayValue().id("st" + id).displayValue("status " + id));
  }

  public static ApplicationDetail buildApplicationDetail(Integer id, Boolean flag, Date date) {
    return new ApplicationDetail()
        .caseReferenceNumber(id + "")
        .categoryOfLaw(new StringDisplayValue().id(id + "cat1").displayValue(id + "catoflaw1"))
        .certificate(new StringDisplayValue().id(id + "cert").displayValue(id + " certificate"))
        .client(new ClientDetail().firstName(id + "firstname").reference(id + "ref"))
        .providerDetails(buildApplicationProviderDetails(id))
        .allSectionsComplete(flag)
        .amendment(flag)
        .addAmendmentProceedingsInEbsItem(buildProceeding(date, BigDecimal.ONE))
        .applicationType(
            new ApplicationType()
                .id("type" + id)
                .displayValue("the type " + id)
                .devolvedPowers(
                    new DevolvedPowersDetail()
                        .used(flag)
                        .dateUsed(flag ? date : null)
                        .contractFlag("flag" + id)))
        .appMode(flag)
        .auditTrail(new AuditDetail().createdBy("person" + id))
        .availableFunctions(new ArrayList<>(Arrays.asList("a" + id, "b" + id, "c" + id)))
        .award(flag)
        .caseOutcome(new CaseOutcomeDetail().caseReferenceNumber(id + ""))
        .certificate(new StringDisplayValue().id("cert" + id).displayValue("certificate " + id))
        .correspondenceAddress(new AddressDetail().addressLine1("addressline1, " + id))
        .costLimit(new CostLimitDetail().limitAtTimeOfMerits(BigDecimal.ONE).changed(Boolean.TRUE))
        .costs(
            new CostStructureDetail()
                .auditTrail(new AuditDetail().createdBy("user" + id))
                .addCostEntriesItem(
                    new CostEntryDetail()
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
        .linkedCases(new ArrayList<>(Collections.singletonList(new LinkedCaseDetail())))
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
        .addPriorAuthoritiesItem(new PriorAuthorityDetail())
        .addProceedingsItem(buildProceeding(date, BigDecimal.ONE))
        .addProceedingsItem(buildProceeding(date, BigDecimal.TEN))
        .quickEditType("quicktype" + id)
        .relationToLinkedCase("rel" + id)
        .status(new StringDisplayValue().id("st" + id).displayValue("status " + id))
        .submitted(flag);
  }

  public static ApplicationProviderDetails buildApplicationProviderDetails(Integer id) {
    return new ApplicationProviderDetails()
        .provider(new IntDisplayValue().id(id).displayValue("ClientDetail " + id))
        .feeEarner(new StringDisplayValue().id("fee" + id).displayValue("Fee " + id))
        .supervisor(new StringDisplayValue().id("sup" + id).displayValue("super " + id))
        .office(new IntDisplayValue().id(id).displayValue("Office " + id))
        .providerCaseReference("provcaseref" + id)
        .providerContact(
            new StringDisplayValue().id("prov" + id).displayValue("provcontact " + id));
  }

  public static ProceedingDetail buildProceeding(Date date, BigDecimal costLimitation) {
    return new ProceedingDetail()
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

  public static ProceedingOutcomeDetail buildProceedingOutcome(Date date) {
    return new ProceedingOutcomeDetail()
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

  public static ScopeLimitationDetail buildScopeLimitation() {
    return new ScopeLimitationDetail()
        .ebsId("ebsid")
        .delegatedFuncApplyInd(new BooleanDisplayValue().flag(Boolean.TRUE).displayValue("delfunc"))
        .scopeLimitation(new StringDisplayValue().id("scopelim").displayValue("scope lim"))
        .scopeLimitationWording("wording")
        .stage("stage")
        .defaultInd(Boolean.TRUE)
        .nonDefaultWordingReqd(Boolean.TRUE);
  }

  public static OpponentDetail buildOpponent(Date date) {
    return new OpponentDetail()
        .address(buildAddressDetail())
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
        .id(123)
        .legalAided(Boolean.TRUE)
        .middleNames("midnames")
        .nationalInsuranceNumber("nino")
        .organisationName("orgName")
        .organisationType("orgid")
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

  public static AddressDetail buildAddressDetail() {
    return new AddressDetail()
        .addressLine1("add1")
        .addressLine2("add2")
        .auditTrail(new AuditDetail())
        .careOf("careOf")
        .city("thecity")
        .country("thecountry")
        .county("thecounty")
        .houseNameOrNumber("name")
        .id(789)
        .noFixedAbode(Boolean.TRUE)
        .postcode("post")
        .preferredAddress("prefAdd");
  }

  private CaabModelUtils() {}
}
