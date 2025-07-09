package uk.gov.laa.ccms.caab.controller.application;

import java.util.ArrayList;
import java.util.List;
import uk.gov.laa.ccms.data.model.BaseOffice;
import uk.gov.laa.ccms.data.model.BaseProvider;
import uk.gov.laa.ccms.data.model.ContactDetail;
import uk.gov.laa.ccms.data.model.UserDetail;

public class ApplicationTestUtils {

  public static UserDetail buildUser() {
    return new UserDetail()
        .userId(1)
        .userType("testUserType")
        .loginId("testLoginId")
        .provider(buildBaseProvider());
  }

  public static BaseProvider buildBaseProvider() {
    return new BaseProvider()
        .id(123)
        .name("provider1")
        .addOfficesItem(new BaseOffice().id(10).name("Office 1"))
        .addOfficesItem(new BaseOffice().id(11).name("Office 2"));
  }

  public static List<ContactDetail> buildFeeEarners() {
    List<ContactDetail> feeEarners = new ArrayList<>();
    feeEarners.add(new ContactDetail().id(1).name("FeeEarner1"));
    feeEarners.add(new ContactDetail().id(2).name("FeeEarner2"));
    return feeEarners;
  }
}
