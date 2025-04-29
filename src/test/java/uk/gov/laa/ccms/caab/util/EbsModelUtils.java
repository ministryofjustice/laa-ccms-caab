package uk.gov.laa.ccms.caab.util;

import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.AWARD_TYPE_COST;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.AWARD_TYPE_FINANCIAL;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.AWARD_TYPE_LAND;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.AWARD_TYPE_OTHER_ASSET;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.STATUS_DRAFT;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.UUID;
import uk.gov.laa.ccms.data.model.AddressDetail;
import uk.gov.laa.ccms.data.model.AssessmentResult;
import uk.gov.laa.ccms.data.model.AssessmentScreen;
import uk.gov.laa.ccms.data.model.Award;
import uk.gov.laa.ccms.data.model.AwardTypeLookupDetail;
import uk.gov.laa.ccms.data.model.AwardTypeLookupValueDetail;
import uk.gov.laa.ccms.data.model.BaseClient;
import uk.gov.laa.ccms.data.model.BaseOffice;
import uk.gov.laa.ccms.data.model.BaseProvider;
import uk.gov.laa.ccms.data.model.CaseDetail;
import uk.gov.laa.ccms.data.model.CaseDoc;
import uk.gov.laa.ccms.data.model.CaseReferenceSummary;
import uk.gov.laa.ccms.data.model.CaseStatus;
import uk.gov.laa.ccms.data.model.CaseSummary;
import uk.gov.laa.ccms.data.model.CategoryOfLaw;
import uk.gov.laa.ccms.data.model.CategoryOfLawLookupValueDetail;
import uk.gov.laa.ccms.data.model.ContactDetail;
import uk.gov.laa.ccms.data.model.ContactDetails;
import uk.gov.laa.ccms.data.model.CostLimitation;
import uk.gov.laa.ccms.data.model.Discharge;
import uk.gov.laa.ccms.data.model.ExternalResource;
import uk.gov.laa.ccms.data.model.LandAward;
import uk.gov.laa.ccms.data.model.LarDetails;
import uk.gov.laa.ccms.data.model.LinkedCase;
import uk.gov.laa.ccms.data.model.NameDetail;
import uk.gov.laa.ccms.data.model.OfferedAmount;
import uk.gov.laa.ccms.data.model.OfficeDetail;
import uk.gov.laa.ccms.data.model.OpaAttribute;
import uk.gov.laa.ccms.data.model.OpaEntity;
import uk.gov.laa.ccms.data.model.OpaGoal;
import uk.gov.laa.ccms.data.model.OpaInstance;
import uk.gov.laa.ccms.data.model.OtherAsset;
import uk.gov.laa.ccms.data.model.OtherParty;
import uk.gov.laa.ccms.data.model.OtherPartyOrganisation;
import uk.gov.laa.ccms.data.model.OtherPartyPerson;
import uk.gov.laa.ccms.data.model.OutcomeDetail;
import uk.gov.laa.ccms.data.model.PriorAuthority;
import uk.gov.laa.ccms.data.model.PriorAuthorityAttribute;
import uk.gov.laa.ccms.data.model.PriorAuthorityDetail;
import uk.gov.laa.ccms.data.model.PriorAuthorityTypeDetail;
import uk.gov.laa.ccms.data.model.PriorAuthorityTypeDetails;
import uk.gov.laa.ccms.data.model.Proceeding;
import uk.gov.laa.ccms.data.model.ProviderDetail;
import uk.gov.laa.ccms.data.model.ProviderDetails;
import uk.gov.laa.ccms.data.model.RecordHistory;
import uk.gov.laa.ccms.data.model.RecoveredAmount;
import uk.gov.laa.ccms.data.model.Recovery;
import uk.gov.laa.ccms.data.model.RecoveryAmount;
import uk.gov.laa.ccms.data.model.RelationshipToCaseLookupDetail;
import uk.gov.laa.ccms.data.model.RelationshipToCaseLookupValueDetail;
import uk.gov.laa.ccms.data.model.ScopeLimitation;
import uk.gov.laa.ccms.data.model.ServiceAddress;
import uk.gov.laa.ccms.data.model.SubmittedApplicationDetails;
import uk.gov.laa.ccms.data.model.TimeRelatedAward;
import uk.gov.laa.ccms.data.model.UserDetail;
import uk.gov.laa.ccms.data.model.Valuation;

public class EbsModelUtils {

  public static ProviderDetail buildProviderDetail() {
    return buildProviderDetail("1", "2", "3");
  }

  public static ProviderDetail buildProviderDetail(String providerOfficeId,
      String feeEarnerContactId, String supervisorContactId) {
    return new ProviderDetail()
        .addOfficesItem(new OfficeDetail()
            .id(100)
            .name("office 1"))
        .addOfficesItem(new OfficeDetail()
            .id(101)
            .name("office 2")
            .addFeeEarnersItem(
              new ContactDetail()
                .id(102)
                .name("b fee earner")))
        .addOfficesItem(new OfficeDetail()
            .id(Integer.parseInt(providerOfficeId))
            .name("office 3")
            .addFeeEarnersItem(
                new ContactDetail()
                    .id(Integer.parseInt(feeEarnerContactId))
                    .name("a fee earner"))
            .addFeeEarnersItem(
                new ContactDetail()
                    .id(Integer.parseInt(supervisorContactId))
                    .name("c supervisor")));
  }

  public static PriorAuthorityTypeDetails buildPriorAuthorityTypeDetails(String itemDataType) {
    return new PriorAuthorityTypeDetails()
        .addContentItem(buildPriorAuthorityTypeDetail(itemDataType));
  }

  public static PriorAuthorityTypeDetail buildPriorAuthorityTypeDetail(String itemDataType) {
    return new PriorAuthorityTypeDetail()
        .code("prauthtype")
        .description("prauthtypedesc")
        .valueRequired(Boolean.TRUE)
        .addPriorAuthoritiesItem(buildPriorAuthorityDetail(itemDataType));
  }

  public static PriorAuthorityDetail buildPriorAuthorityDetail(String itemDataType) {
    return new PriorAuthorityDetail()
        .code("thecode")
        .lovCode("lov")
        .dataType(itemDataType)
        .description("descr");
  }

  public static UserDetail buildUserDetail() {
    return new UserDetail()
        .username("testUser")
        .userId(1)
        .userType("testUserType")
        .loginId("testLoginId")
        .provider(buildBaseProvider());
  }

  public static UserDetail buildUserDetail(String prefix) {
    return new UserDetail()
        .userId(1)
        .userType(prefix + "type1")
        .loginId(prefix + "login1")
        .username(prefix + "username")
        .provider(buildBaseProvider());
  }

  public static BaseProvider buildBaseProvider() {
    return new BaseProvider()
        .id(123)
        .addOfficesItem(
            new BaseOffice()
                .id(1)
                .name("Office 1"));
  }

  public static CategoryOfLawLookupValueDetail buildCategoryOfLawLookupValueDetail(
      Boolean copyCostLimit) {
    return new CategoryOfLawLookupValueDetail()
        .code("cat1")
        .copyCostLimit(copyCostLimit)
        .matterTypeDescription("matter type");
  }

  public static RelationshipToCaseLookupDetail buildRelationshipToCaseLookupDetail() {
    return new RelationshipToCaseLookupDetail()
        .addContentItem(
            new RelationshipToCaseLookupValueDetail()
                .code("relToCase")
                .copyParty(Boolean.TRUE));
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

  public static BaseClient buildBaseClient() {
    return new BaseClient()
        .clientReferenceNumber("clientref")
        .firstName("firstname")
        .surname("surn");
  }
  
  // Case Detail stuff below this line
  public static CaseDetail buildCaseDetail(String appType) {
    return new CaseDetail()
        .applicationDetails(
            new SubmittedApplicationDetails()
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
                .dateOfFirstAttendance(LocalDate.now())
                .dateOfHearing(LocalDate.now())
                .devolvedPowersDate(LocalDate.now())
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
                .addMeansAssessmentsItem(buildAssessmentResult("means"))
                .addMeritsAssessmentsItem(buildAssessmentResult("merits"))
                .otherParties(Arrays.asList(buildOtherPartyPerson(), buildOtherPartyOrganisation()))
                .preferredAddress("prefadd")
                .addProceedingsItem(buildProceedingDetail(STATUS_DRAFT))
                .addProceedingsItem(buildProceedingDetail("otherstatus"))
                .providerDetails(
                    new ProviderDetails()
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
        .certificateDate(LocalDate.now())
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
                .dateCreated(LocalDateTime.now())
                .dateLastUpdated(LocalDateTime.now())
                .lastUpdatedBy(buildUserDetail("lastUpd")))
        .undertakingAmount(BigDecimal.TEN);

  }


  public static Proceeding buildProceedingDetail(String status) {
    return new Proceeding()
        .availableFunctions(Arrays.asList("pavfuncs1", "pavfuncs2"))
        .clientInvolvementType("citype")
        .dateApplied(LocalDate.now())
        .dateCostsValid(LocalDate.now())
        .dateDevolvedPowersUsed(LocalDate.now())
        .dateGranted(LocalDate.now())
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

  public static OutcomeDetail buildOutcomeDetail() {
    return new OutcomeDetail()
        .additionalResultInfo("addresinfo")
        .altAcceptanceReason("altaccreason")
        .altDisputeResolution("altdispres")
        .courtCode("courtcode")
        .finalWorkDate(LocalDate.now())
        .issueDate(LocalDate.now())
        .outcomeCourtCaseNumber("outccn")
        .resolutionMethod("resmeth")
        .result("res")
        .stageEnd("se")
        .widerBenefits("widerbens");
  }

  public static ScopeLimitation buildScopeLimitation(String prefix) {
    return new ScopeLimitation()
        .delegatedFunctionsApply(Boolean.TRUE)
        .scopeLimitation(prefix + "scopelim")
        .scopeLimitationId(prefix + "scopelimid")
        .scopeLimitationWording(prefix + "slwording");
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
                .assessmentDate(LocalDate.now())
                .certificateNumber("certnum")
                .contactDetails(buildContactDetail("opp"))
                .contactName("conname")
                .courtOrderedMeansAssessment(Boolean.TRUE)
                .dateOfBirth(LocalDate.now())
                .employersName("employer")
                .employmentStatus("empstat")
                .name(buildNameDetail())
                .niNumber("ni123456")
                .organisationAddress("orgaddr")
                .organisationName("orgname")
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
                .currentlyTrading(true)
                .relationToCase("reltocase")
                .organisationName("orgname")
                .organisationType("orgtype")
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



  public static LinkedCase buildLinkedCase() {
    return new LinkedCase()
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
  
  public static PriorAuthority buildPriorAuthority() {
    return new PriorAuthority()
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


  public static CostLimitation buildCostLimitation(String prefix) {
    return new CostLimitation()
        .costLimitId(prefix + "costid")
        .costCategory(prefix + "costcat1")
        .amount(BigDecimal.TEN)
        .paidToDate(BigDecimal.ONE)
        .billingProviderId(prefix + "billprovid")
        .billingProviderName(prefix + "billprovname");
  }

  public static AssessmentResult buildAssessmentResult(
      String prefix) {
    return new AssessmentResult()
        .defaultInd(Boolean.TRUE)
        .date(LocalDate.now())
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
                                    buildOpaAttribute(prefix)))));
  }

  public static OpaAttribute buildOpaAttribute(final String prefix) {
    return new OpaAttribute()
        .userDefinedInd(Boolean.TRUE)
        .attribute(prefix + "attt")
        .responseText(prefix + "response")
        .responseType(prefix + "restype")
        .responseValue(prefix + "resval")
        .caption(prefix + "caption");
  }




  public static Award buildCostAward() {
    return new Award()
        .deleteAllowed(Boolean.TRUE)
        .updateAllowed(Boolean.TRUE)
        .awardType(AWARD_TYPE_COST)
        .awardId("costAwardId")
        .awardCategory("costCat")
        .costAward(
            new uk.gov.laa.ccms.data.model.CostAward()
                .serviceAddress(
                    new ServiceAddress()
                        .addressLine1("add1")
                        .addressLine2("add2")
                        .addressLine3("add3"))
                .recovery(buildRecovery())
                .awardedBy("me")
                .liableParties(Arrays.asList("1", "2", "3"))
                .orderDate(LocalDate.now())
                .orderDateServed(LocalDate.now())
                .otherDetails("otherDets")
                .certificateCostRateLsc(BigDecimal.ONE)
                .certificateCostRateMarket(BigDecimal.ZERO)
                .preCertificateAwardLsc(BigDecimal.TEN)
                .preCertificateAwardOth(BigDecimal.ONE)
                .courtAssessmentStatus("assessstate")
                .interestAwardedRate(BigDecimal.TEN)
                .interestAwardedStartDate(LocalDate.now()));
  }

  public static Award buildFinancialAward() {
    return new Award()
        .deleteAllowed(Boolean.TRUE)
        .updateAllowed(Boolean.TRUE)
        .awardType(AWARD_TYPE_FINANCIAL)
        .awardId("finAwardId")
        .awardCategory("finCat")
        .financialAward(
            new uk.gov.laa.ccms.data.model.FinancialAward()
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
                .liableParties(Arrays.asList("1", "2", "3"))
                .orderDate(LocalDate.now())
                .orderDateServed(LocalDate.now())
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
                .orderDate(LocalDate.now())
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
                        .date(LocalDate.now()))
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
                .otherProprietors(Arrays.asList("1", "2", "3"))
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
                .heldBy(Arrays.asList("1", "2", "3"))
                .awardedPercentage(BigDecimal.TEN)
                .recoveredPercentage(BigDecimal.ONE)
                .disputedPercentage(BigDecimal.ZERO)
                .noRecoveryDetails("none")
                .orderDate(LocalDate.now())
                .recovery("recov")
                .statChargeExemptReason("exempt")
                .timeRelatedAward(buildTimeRelatedAward())
                .valuation(
                    new Valuation()
                        .amount(BigDecimal.TEN)
                        .criteria("crit")
                        .date(LocalDate.now())));
  }

  public static TimeRelatedAward buildTimeRelatedAward() {
    return new TimeRelatedAward()
        .awardDate(LocalDate.now())
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
                        .dateReceived(LocalDate.now())
                        .paidToLsc(BigDecimal.ZERO))
                .court(
                    new RecoveryAmount()
                        .amount(BigDecimal.TEN)
                        .dateReceived(LocalDate.now())
                        .paidToLsc(BigDecimal.ONE))
                .solicitor(
                    new RecoveryAmount()
                        .amount(BigDecimal.ZERO)
                        .dateReceived(LocalDate.now())
                        .paidToLsc(BigDecimal.TEN)))
        .unRecoveredAmount(BigDecimal.ZERO);
  }

  public static AwardTypeLookupDetail buildAwardTypeLookupDetail(CaseDetail soaCase) {
    return new AwardTypeLookupDetail()
        .addContentItem(new AwardTypeLookupValueDetail()
            .awardType(AWARD_TYPE_COST)
            .code(soaCase.getAwards().getFirst().getAwardType()))
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

  public static ContactDetails buildContactDetail(String prefix) {
    return new ContactDetails()
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


}
