package uk.gov.laa.ccms.caab.util;

import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.AWARD_TYPE_COST;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.AWARD_TYPE_FINANCIAL;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.AWARD_TYPE_LAND;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.AWARD_TYPE_OTHER_ASSET;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.STATUS_DRAFT;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;
import uk.gov.laa.ccms.data.model.AwardTypeLookupDetail;
import uk.gov.laa.ccms.data.model.AwardTypeLookupValueDetail;
import uk.gov.laa.ccms.soa.gateway.model.AddressDetail;
import uk.gov.laa.ccms.soa.gateway.model.ApplicationDetails;
import uk.gov.laa.ccms.soa.gateway.model.AssessmentScreen;
import uk.gov.laa.ccms.soa.gateway.model.Award;
import uk.gov.laa.ccms.soa.gateway.model.BaseClient;
import uk.gov.laa.ccms.soa.gateway.model.CaseDetail;
import uk.gov.laa.ccms.soa.gateway.model.CaseDoc;
import uk.gov.laa.ccms.soa.gateway.model.CaseReferenceSummary;
import uk.gov.laa.ccms.soa.gateway.model.CaseStatus;
import uk.gov.laa.ccms.soa.gateway.model.CaseSummary;
import uk.gov.laa.ccms.soa.gateway.model.CategoryOfLaw;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetail;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetailDetails;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetailDetailsDisabilityMonitoring;
import uk.gov.laa.ccms.soa.gateway.model.ClientPersonalDetail;
import uk.gov.laa.ccms.soa.gateway.model.ContactDetail;
import uk.gov.laa.ccms.soa.gateway.model.ContractDetail;
import uk.gov.laa.ccms.soa.gateway.model.CostLimitation;
import uk.gov.laa.ccms.soa.gateway.model.Discharge;
import uk.gov.laa.ccms.soa.gateway.model.ExternalResource;
import uk.gov.laa.ccms.soa.gateway.model.LandAward;
import uk.gov.laa.ccms.soa.gateway.model.LarDetails;
import uk.gov.laa.ccms.soa.gateway.model.NameDetail;
import uk.gov.laa.ccms.soa.gateway.model.OfferedAmount;
import uk.gov.laa.ccms.soa.gateway.model.OpaAttribute;
import uk.gov.laa.ccms.soa.gateway.model.OpaEntity;
import uk.gov.laa.ccms.soa.gateway.model.OpaGoal;
import uk.gov.laa.ccms.soa.gateway.model.OpaInstance;
import uk.gov.laa.ccms.soa.gateway.model.OtherAsset;
import uk.gov.laa.ccms.soa.gateway.model.OtherParty;
import uk.gov.laa.ccms.soa.gateway.model.OtherPartyOrganisation;
import uk.gov.laa.ccms.soa.gateway.model.OtherPartyPerson;
import uk.gov.laa.ccms.soa.gateway.model.OutcomeDetail;
import uk.gov.laa.ccms.soa.gateway.model.PriorAuthorityAttribute;
import uk.gov.laa.ccms.soa.gateway.model.ProceedingDetail;
import uk.gov.laa.ccms.soa.gateway.model.ProviderDetail;
import uk.gov.laa.ccms.soa.gateway.model.RecordHistory;
import uk.gov.laa.ccms.soa.gateway.model.RecoveredAmount;
import uk.gov.laa.ccms.soa.gateway.model.Recovery;
import uk.gov.laa.ccms.soa.gateway.model.RecoveryAmount;
import uk.gov.laa.ccms.soa.gateway.model.ScopeLimitation;
import uk.gov.laa.ccms.soa.gateway.model.ServiceAddress;
import uk.gov.laa.ccms.soa.gateway.model.TimeRelatedAward;
import uk.gov.laa.ccms.soa.gateway.model.UserDetail;
import uk.gov.laa.ccms.soa.gateway.model.Valuation;

public class SoaModelUtils {
  public static uk.gov.laa.ccms.soa.gateway.model.PriorAuthority buildPriorAuthority() {
    return new uk.gov.laa.ccms.soa.gateway.model.PriorAuthority()
        .assessedAmount(BigDecimal.TEN)
        .decisionStatus("dstat")
        .description("descr")
        .addDetailsItem(
            new PriorAuthorityAttribute()
                .name("thecode")
                .value("attval"))
        .priorAuthorityType("prauthtype")
        .reasonForRequest("requestreason")
        .requestAmount(BigDecimal.ONE);
  }

  public static CaseDetail buildCaseDetail(String appType) {
    return new CaseDetail()
        .applicationDetails(
            new ApplicationDetails()
                .applicationAmendmentType(appType)
                .categoryOfLaw(
                    new CategoryOfLaw()
                        .categoryOfLawCode("cat1")
                        .categoryOfLawDescription("cat 1")
                        .addCostLimitationsItem(buildCostLimitation("cat1"))
                        .addCostLimitationsItem(buildCostLimitation("cat2"))
                        .grantedAmount(BigDecimal.ZERO)
                        .requestedAmount(BigDecimal.ONE)
                        .totalPaidToDate(BigDecimal.TEN))
                .certificateType("certtype")
                .client(buildBaseClient())
                .correspondenceAddress(buildAddressDetail("corr"))
                .dateOfFirstAttendance(new java.util.Date())
                .dateOfHearing(new java.util.Date())
                .devolvedPowersDate(new java.util.Date())
                .addExternalResourcesItem(
                    new ExternalResource()
                        .costCeiling(Arrays.asList(
                            buildCostLimitation("ext1"),
                            buildCostLimitation("ext2"))))
                .fixedHearingDateInd(Boolean.TRUE)
                .highProfileCaseInd(Boolean.TRUE)
                .larDetails(
                    new LarDetails()
                        .larScopeFlag(Boolean.TRUE)
                        .legalHelpUfn("ufn")
                        .legalHelpOfficeCode("off1"))
                .addMeansAssesmentsItem(buildAssessmentResult("means"))
                .addMeritsAssesmentsItem(buildAssessmentResult("merits"))
                .otherParties(Arrays.asList(buildOtherPartyPerson(), buildOtherPartyOrganisation()))
                .preferredAddress("prefadd")
                .addProceedingsItem(buildProceedingDetail(STATUS_DRAFT))
                .addProceedingsItem(buildProceedingDetail("otherstatus"))
                .providerDetails(
                    new ProviderDetail()
                        .contactDetails(buildContactDetail("prov"))
                        .providerOfficeId("11111")
                        .contactUserId(buildUserDetail("contact"))
                        .providerFirmId("12345") // Defined as String, but data is numeric in db!
                        .providerCaseReferenceNumber("provcaseref123")
                        .feeEarnerContactId("22222")
                        .supervisorContactId("33333"))
                .purposeOfApplication("purposeA")
                .purposeOfHearing("purposeH"))
        .availableFunctions(Arrays.asList("func1", "func2"))
        .addAwardsItem(buildCostAward())
        .addAwardsItem(buildFinancialAward())
        .addAwardsItem(buildLandAward())
        .addAwardsItem(buildOtherAssetAward())
        .addCaseDocsItem(
            new CaseDoc()
                .ccmsDocumentId("docId")
                .documentSubject("thesub"))
        .caseReferenceNumber("caseref")
        .caseStatus(new CaseStatus()
            .actualCaseStatus("actualstat")
            .displayCaseStatus("displaystat")
            .statusUpdateInd(Boolean.TRUE))
        .certificateDate(new java.util.Date())
        .certificateType("certtype")
        .dischargeStatus(
            new Discharge()
                .clientContinuePvtInd(Boolean.TRUE)
                .otherDetails("dotherdets")
                .reason("dreason"))
        .legalHelpCosts(BigDecimal.TEN)
        .addLinkedCasesItem(buildLinkedCase())
        .preCertificateCosts(BigDecimal.ONE)
        .addPriorAuthoritiesItem(buildPriorAuthority())
        .recordHistory(
            new RecordHistory()
                .createdBy(buildUserDetail("creator"))
                .dateCreated(new java.util.Date())
                .dateLastUpdated(new java.util.Date())
                .lastUpdatedBy(buildUserDetail("lastUpd")))
        .undertakingAmount(BigDecimal.TEN);

  }

  public static ProceedingDetail buildProceedingDetail(String status) {
    return new ProceedingDetail()
        .availableFunctions(Arrays.asList("pavfuncs1", "pavfuncs2"))
        .clientInvolvementType("citype")
        .dateApplied(new java.util.Date())
        .dateCostsValid(new java.util.Date())
        .dateDevolvedPowersUsed(new java.util.Date())
        .dateGranted(new java.util.Date())
        .devolvedPowersInd(Boolean.TRUE)
        .leadProceedingIndicator(Boolean.TRUE)
        .levelOfService("levofsvc")
        .matterType("pmattype")
        .orderType("ordtype")
        .outcome(buildOutcomeDetail())
        .outcomeCourtCaseNumber("occn")
        .proceedingCaseId("pcid")
        .proceedingDescription("procdescr")
        .proceedingType("proctype")
        .scopeLimitationApplied("scopelimapp")
        .addScopeLimitationsItem(buildScopeLimitation("1"))
        .addScopeLimitationsItem(buildScopeLimitation("2"))
        .addScopeLimitationsItem(buildScopeLimitation("3"))
        .stage("stg")
        .status(status);
  }

  public static ScopeLimitation buildScopeLimitation(String prefix) {
    return new ScopeLimitation()
        .delegatedFunctionsApply(Boolean.TRUE)
        .scopeLimitation(prefix + "scopelim")
        .scopeLimitationId(prefix +"scopelimid")
        .scopeLimitationWording(prefix + "slwording");
  }

  public static OutcomeDetail buildOutcomeDetail() {
    return new OutcomeDetail()
        .additionalResultInfo("addresinfo")
        .altAcceptanceReason("altaccreason")
        .altDisputeResolution("altdispres")
        .courtCode("courtcode")
        .finalWorkDate(new java.util.Date())
        .issueDate(new java.util.Date())
        .outcomeCourtCaseNumber("outccn")
        .resolutionMethod("resmeth")
        .result("res")
        .stageEnd("se")
        .widerBenefits("widerbens");
  }

  public static OtherParty buildOtherPartyPerson() {
    return new OtherParty()
        .otherPartyId("opid")
        .person(
            new OtherPartyPerson()
                .address(buildAddressDetail("opp"))
                .assessedAssets(BigDecimal.TEN)
                .assessedIncome(BigDecimal.ONE)
                .assessedIncomeFrequency("often")
                .assessmentDate(new java.util.Date())
                .certificateNumber("certnum")
                .contactDetails(buildContactDetail("opp"))
                .contactName("conname")
                .courtOrderedMeansAssesment(Boolean.TRUE)
                .dateOfBirth(new java.util.Date())
                .employersName("employer")
                .employmentStatus("empstat")
                .name(buildNameDetail())
                .niNumber("ni123456")
                .organizationAddress("orgaddr")
                .organizationName("orgname")
                .otherInformation("otherinf")
                .partyLegalAidedInd(Boolean.TRUE)
                .publicFundingAppliedInd(Boolean.TRUE)
                .relationToCase("reltocase")
                .relationToClient("reltoclient"))
        .sharedInd(Boolean.TRUE);
  }

  public static OtherParty buildOtherPartyOrganisation() {
    return new OtherParty()
        .otherPartyId("opid")
        .organisation(
            new OtherPartyOrganisation()
                .address(buildAddressDetail("opo"))
                .contactDetails(buildContactDetail("op"))
                .contactName("name")
                .currentlyTrading("curtrad")
                .relationToCase("reltocase")
                .organizationName("orgname")
                .organizationType("orgtype")
                .otherInformation("otherinf")
                .relationToClient("relclient"))
        .sharedInd(Boolean.TRUE);
  }

  public static NameDetail buildNameDetail() {
    return new NameDetail()
        .firstName("firstname")
        .surname("thesurname")
        .fullName("thefullname")
        .middleName("mid")
        .surnameAtBirth("surbirth")
        .title("mr");
  }

  public static uk.gov.laa.ccms.soa.gateway.model.LinkedCase buildLinkedCase() {
    return new uk.gov.laa.ccms.soa.gateway.model.LinkedCase()
        .caseReferenceNumber("lcaseref")
        .caseStatus("lcasestat")
        .categoryOfLawCode("lcat1")
        .categoryOfLawDesc("linked cat 1")
        .client(
            new BaseClient()
                .firstName("lfirstname")
                .surname("lsurname")
                .clientReferenceNumber("lclientref"))
        .feeEarnerId("lfeeearner")
        .feeEarnerName("lname")
        .linkType("ltype")
        .providerReferenceNumber("lprovrefnum")
        .publicFundingAppliedInd(Boolean.TRUE);
  }

  public static AddressDetail buildAddressDetail(String prefix) {
    return new AddressDetail()
        .addressId(prefix + "address1")
        .addressLine1(prefix + "addline1")
        .addressLine2(prefix + "addline2")
        .addressLine3(prefix + "addline3")
        .addressLine4(prefix + "addline4")
        .careOfName(prefix + "cofname")
        .city(prefix + "thecity")
        .country(prefix + "thecountry")
        .county(prefix + "thecounty")
        .house(prefix + "thehouse")
        .postalCode(prefix + "pc")
        .province(prefix + "prov")
        .state(prefix + "st");
  }

  public static ContactDetail buildContactDetail(String prefix) {
    return new ContactDetail()
        .correspondenceLanguage(prefix + "lang")
        .emailAddress(prefix + "email")
        .fax(prefix + "123765")
        .correspondenceMethod(prefix + "method")
        .password(prefix + "pass")
        .mobileNumber(prefix + "mobil123")
        .passwordReminder(prefix + "remember")
        .telephoneHome(prefix + "tel123")
        .telephoneWork(prefix + "telwork123");
  }

  public static CostLimitation buildCostLimitation(String prefix) {
    return new CostLimitation()
        .costLimitId(prefix + "costid")
        .costCategory(prefix + "costcat1")
        .amount(BigDecimal.TEN)
        .paidToDate(BigDecimal.ONE)
        .billingProviderId(prefix + "billprovid")
        .billingProviderName(prefix + "billprovname");
  }

  public static uk.gov.laa.ccms.soa.gateway.model.AssessmentResult buildAssessmentResult(
      String prefix) {
    return new uk.gov.laa.ccms.soa.gateway.model.AssessmentResult()
        .defaultInd(Boolean.TRUE)
        .date(new java.util.Date())
        .assessmentId(prefix + "assessid")
        .addResultsItem(
            new OpaGoal()
                .attributeValue(prefix + "val")
                .attribute(prefix + "att"))
        .addAssessmentDetailsItem(
            new AssessmentScreen()
                .caption(prefix + "cap")
                .screenName(prefix + "name")
                .addEntityItem(
                    new OpaEntity()
                        .sequenceNumber(1)
                        .entityName(prefix + "thentity")
                        .caption(prefix + "capt")
                        .addInstancesItem(
                            new OpaInstance()
                                .caption(prefix + "capti")
                                .instanceLabel(prefix + "label")
                                .addAttributesItem(
                                    new OpaAttribute()
                                        .userDefinedInd(Boolean.TRUE)
                                        .attribute(prefix + "attt")
                                        .responseText(prefix + "response")
                                        .responseType(prefix + "restype")
                                        .responseValue(prefix + "resval")
                                        .caption(prefix + "caption")))));
  }

  public static Award buildCostAward() {
    return new Award()
        .deleteAllowed(Boolean.TRUE)
        .updateAllowed(Boolean.TRUE)
        .awardType(AWARD_TYPE_COST)
        .awardId("costAwardId")
        .awardCategory("costCat")
        .costAward(
            new uk.gov.laa.ccms.soa.gateway.model.CostAward()
                .serviceAddress(
                    new ServiceAddress()
                        .addressLine1("add1")
                        .addressLine2("add2")
                        .addressLine3("add3"))
                .recovery(buildRecovery())
                .awardedBy("me")
                .liableParties(Arrays.asList("a", "b", "c"))
                .orderDate(new java.util.Date())
                .orderDateServed(new java.util.Date())
                .otherDetails("otherDets")
                .certificateCostRateLsc(BigDecimal.ONE)
                .certificateCostRateMarket(BigDecimal.ZERO)
                .preCertificateAwardLsc(BigDecimal.TEN)
                .preCertificateAwardOth(BigDecimal.ONE)
                .courtAssessmentStatus("assessstate")
                .interestAwardedRate(BigDecimal.TEN)
                .interestAwardedStartDate(new java.util.Date()));
  }

  public static Award buildFinancialAward() {
    return new Award()
        .deleteAllowed(Boolean.TRUE)
        .updateAllowed(Boolean.TRUE)
        .awardType(AWARD_TYPE_FINANCIAL)
        .awardId("finAwardId")
        .awardCategory("finCat")
        .financialAward(
            new uk.gov.laa.ccms.soa.gateway.model.FinancialAward()
                .serviceAddress(
                    new ServiceAddress()
                        .addressLine1("add1")
                        .addressLine2("add2")
                        .addressLine3("add3"))
                .recovery(buildRecovery())
                .awardedBy("me")
                .interimAward(BigDecimal.TEN)
                .amount(BigDecimal.ONE)
                .awardJustifications("justified")
                .liableParties(Arrays.asList("a", "b", "c"))
                .orderDate(new java.util.Date())
                .orderDateServed(new java.util.Date())
                .otherDetails("otherDets")
                .statutoryChangeReason("statreason"));
  }

  public static Award buildLandAward() {
    return new Award()
        .deleteAllowed(Boolean.TRUE)
        .updateAllowed(Boolean.TRUE)
        .awardType(AWARD_TYPE_LAND)
        .awardId("landAwardId")
        .awardCategory("landCat")
        .landAward(
            new LandAward()
                .orderDate(new java.util.Date())
                .description("descr")
                .titleNo("title")
                .propertyAddress(
                    new ServiceAddress()
                        .addressLine1("add1")
                        .addressLine2("add2")
                        .addressLine3("add3"))
                .valuation(
                    new Valuation()
                        .amount(BigDecimal.TEN)
                        .criteria("crit")
                        .date(new java.util.Date()))
                .disputedPercentage(BigDecimal.ZERO)
                .awardedPercentage(BigDecimal.TEN)
                .mortgageAmountDue(BigDecimal.ONE)
                .equity("shouldn't be used")
                .awardedBy("me")
                .recovery("recov")
                .noRecoveryDetails("none")
                .statChargeExemptReason("exempt")
                .landChargeRegistration("landChargeReg")
                .registrationRef("regRef")
                .otherProprietors(Arrays.asList("a", "b", "c"))
                .timeRelatedAward(buildTimeRelatedAward()));
  }

  public static Award buildOtherAssetAward() {
    return new Award()
        .deleteAllowed(Boolean.TRUE)
        .updateAllowed(Boolean.TRUE)
        .awardType(AWARD_TYPE_OTHER_ASSET)
        .awardId("otherAwardId")
        .awardCategory("otherCat")
        .otherAsset(
            new OtherAsset()
                .awardedBy("me")
                .description("descr")
                .awardedAmount(BigDecimal.ONE)
                .recoveredAmount(BigDecimal.ZERO)
                .disputedAmount(BigDecimal.TEN)
                .heldBy(Arrays.asList("A", "B", "C"))
                .awardedPercentage(BigDecimal.TEN)
                .recoveredPercentage(BigDecimal.ONE)
                .disputedPercentage(BigDecimal.ZERO)
                .noRecoveryDetails("none")
                .orderDate(new java.util.Date())
                .recovery("recov")
                .statChargeExemptReason("exempt")
                .timeRelatedAward(buildTimeRelatedAward())
                .valuation(
                    new Valuation()
                        .amount(BigDecimal.TEN)
                        .criteria("crit")
                        .date(new java.util.Date())));
  }

  public static TimeRelatedAward buildTimeRelatedAward() {
    return new TimeRelatedAward()
        .awardDate(new java.util.Date())
        .amount(BigDecimal.TEN)
        .awardType("type")
        .description("desc1")
        .awardTriggeringEvent("theevent")
        .otherDetails("otherdets");
  }

  public static Recovery buildRecovery() {
    return new Recovery()
        .awardValue(BigDecimal.ONE)
        .leaveOfCourtReqdInd(Boolean.TRUE)
        .offeredAmount(
            new OfferedAmount()
                .amount(BigDecimal.TEN)
                .conditionsOfOffer("cond1"))
        .recoveredAmount(
            new RecoveredAmount()
                .client(
                    new RecoveryAmount()
                        .amount(BigDecimal.ONE)
                        .dateReceived(new java.util.Date())
                        .paidToLsc(BigDecimal.ZERO))
                .court(
                    new RecoveryAmount()
                        .amount(BigDecimal.TEN)
                        .dateReceived(new java.util.Date())
                        .paidToLsc(BigDecimal.ONE))
                .solicitor(
                    new RecoveryAmount()
                        .amount(BigDecimal.ZERO)
                        .dateReceived(new java.util.Date())
                        .paidToLsc(BigDecimal.TEN)))
        .unRecoveredAmount(BigDecimal.ZERO);
  }

  public static UserDetail buildUserDetail(String prefix) {
    return new uk.gov.laa.ccms.soa.gateway.model.UserDetail()
        .userType(prefix + "type1")
        .userLoginId(prefix + "login1")
        .userName(prefix + "username");
  }

  public static BaseClient buildBaseClient() {
    return new BaseClient()
        .clientReferenceNumber("clientref")
        .firstName("firstname")
        .surname("surn");
  }

  public static ClientDetail buildClientDetail() {
    return new ClientDetail()
        .clientReferenceNumber("12345")
        .details(new ClientDetailDetails()
            .name(new NameDetail()
                .firstName("firstname")
                .surname("surname")
                .fullName("the full name")
                .title("mr")
                .middleName("middle")
                .surnameAtBirth("birth"))
            .address(buildAddressDetail("client"))
            .contacts(buildContactDetail("client"))
            .disabilityMonitoring(new ClientDetailDetailsDisabilityMonitoring()
                .addDisabilityTypeItem("item"))
            .ethnicMonitoring("ethnic")
            .noFixedAbode(Boolean.FALSE)
            .personalInformation(new ClientPersonalDetail()
                .countryOfOrigin("origin")
                .dateOfBirth(new Date())
                .gender("gender")
                .highProfileClient(Boolean.FALSE)
                .homeOfficeNumber("123")
                .maritalStatus("status")
                .mentalCapacityInd(Boolean.FALSE)
                .nationalInsuranceNumber("nino")
                .vexatiousLitigant(Boolean.FALSE)
                .vulnerableClient(Boolean.FALSE))
            .specialConsiderations("special"))
        .recordHistory(new RecordHistory());
  }

  public static AwardTypeLookupDetail buildAwardTypeLookupDetail(CaseDetail soaCase) {
    return new AwardTypeLookupDetail()
        .addContentItem(new AwardTypeLookupValueDetail()
            .awardType(AWARD_TYPE_COST)
            .code(soaCase.getAwards().get(0).getAwardType()))
        .addContentItem(new AwardTypeLookupValueDetail()
            .awardType(AWARD_TYPE_FINANCIAL)
            .code(soaCase.getAwards().get(1).getAwardType()))
        .addContentItem(new AwardTypeLookupValueDetail()
            .awardType(AWARD_TYPE_LAND)
            .code(soaCase.getAwards().get(2).getAwardType()))
        .addContentItem(new AwardTypeLookupValueDetail()
            .awardType(AWARD_TYPE_OTHER_ASSET)
            .code(soaCase.getAwards().get(3).getAwardType()));
  }

  public static ContractDetail createContractDetail(String cat, Boolean createNewMatters,
      Boolean remainderAuth) {
    return new ContractDetail()
        .categoryofLaw(cat)
        .subCategory("SUBCAT1")
        .createNewMatters(createNewMatters)
        .remainderAuthorisation(remainderAuth)
        .contractualDevolvedPowers("CATDEVPOW")
        .authorisationType("AUTHTYPE1");
  }

  public static CaseReferenceSummary buildCaseReferenceSummary() {
    return new CaseReferenceSummary()
        .caseReferenceNumber(UUID.randomUUID().toString());
  }

  public static CaseSummary buildCaseSummary() {
    return new CaseSummary()
        .caseReferenceNumber("caseref")
        .caseStatusDisplay("the status")
        .categoryOfLaw("CAT")
        .client(buildBaseClient())
        .feeEarnerName("feeearner")
        .providerCaseReferenceNumber("prov");
  }
}
