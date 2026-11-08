package uk.gov.laa.ccms.caab.util;

import java.util.Objects;
import org.springframework.web.util.UriComponentsBuilder;
import uk.gov.laa.ccms.caab.bean.AddressFlowOwner;
import uk.gov.laa.ccms.caab.bean.AddressLookupFlowData;

/** Shared identity checks; expected ownership must always be resolved by the server. */
public final class AddressFlowIdentity {

  private AddressFlowIdentity() {}

  public static boolean belongsTo(AddressLookupFlowData<?> flow, AddressFlowOwner owner) {
    return flow != null && owner != null && owner.equals(flow.getOwner());
  }

  public static boolean matches(
      AddressLookupFlowData<?> flow, AddressFlowOwner owner, String flowId) {
    return belongsTo(flow, owner) && Objects.equals(flow.getFlowId(), flowId);
  }

  public static boolean matchesRevision(AddressLookupFlowData<?> flow, String revision) {
    if (flow == null || revision == null) {
      return false;
    }
    try {
      long expected = Long.parseLong(revision);
      return expected >= 0 && expected == flow.getRevision();
    } catch (NumberFormatException invalid) {
      return false;
    }
  }

  public static String url(String path, AddressLookupFlowData<?> flow) {
    return UriComponentsBuilder.fromUriString(path)
        .queryParam("flowId", flow.getFlowId())
        .queryParam("revision", flow.getRevision())
        .build()
        .encode()
        .toUriString();
  }
}
