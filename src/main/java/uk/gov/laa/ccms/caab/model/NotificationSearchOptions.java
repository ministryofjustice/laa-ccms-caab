package uk.gov.laa.ccms.caab.model;

import java.util.List;
import uk.gov.laa.ccms.data.model.BaseUser;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;
import uk.gov.laa.ccms.data.model.ContactDetail;

/** Options required to render the notification search form. */
public record NotificationSearchOptions(
    Integer providerId,
    List<ContactDetail> feeEarners,
    List<CommonLookupValueDetail> notificationTypes,
    List<BaseUser> users,
    boolean fullyAvailable) {

  public NotificationSearchOptions {
    feeEarners = List.copyOf(feeEarners);
    notificationTypes = List.copyOf(notificationTypes);
    users = List.copyOf(users);
  }
}
