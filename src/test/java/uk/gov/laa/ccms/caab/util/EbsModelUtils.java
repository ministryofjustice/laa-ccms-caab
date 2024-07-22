package uk.gov.laa.ccms.caab.util;

import uk.gov.laa.ccms.data.model.BaseOffice;
import uk.gov.laa.ccms.data.model.BaseProvider;
import uk.gov.laa.ccms.data.model.CategoryOfLawLookupValueDetail;
import uk.gov.laa.ccms.data.model.ContactDetail;
import uk.gov.laa.ccms.data.model.OfficeDetail;
import uk.gov.laa.ccms.data.model.PriorAuthorityDetail;
import uk.gov.laa.ccms.data.model.PriorAuthorityTypeDetail;
import uk.gov.laa.ccms.data.model.PriorAuthorityTypeDetails;
import uk.gov.laa.ccms.data.model.ProviderDetail;
import uk.gov.laa.ccms.data.model.RelationshipToCaseLookupDetail;
import uk.gov.laa.ccms.data.model.RelationshipToCaseLookupValueDetail;
import uk.gov.laa.ccms.data.model.UserDetail;

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

}
