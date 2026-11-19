package uk.gov.laa.ccms.caab.config;

import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_FORM_DATA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CLIENT_FLOW_FORM_DATA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CREATE_CLIENT_ADDRESS_FLOW;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.UriComponentsBuilder;
import uk.gov.laa.ccms.caab.bean.AddressFlowOwner;
import uk.gov.laa.ccms.caab.bean.AddressLookupFlowData;
import uk.gov.laa.ccms.caab.bean.ApplicationFormData;
import uk.gov.laa.ccms.caab.bean.ClientFlowFormData;
import uk.gov.laa.ccms.caab.util.AddressFlowIdentity;

/** Create-client identity checks before MVC binds session-owned form objects. */
public class CreateClientRegistrationInterceptor implements HandlerInterceptor {

  public static final String ADDRESS_PURPOSE = "create-client-address";
  private static final String CLIENT_SNAPSHOT =
      CreateClientRegistrationInterceptor.class.getName() + ".client";
  private static final String DETAILS = "/application/client/details/";
  private static final String ADDRESS = DETAILS + "address";

  public static AddressFlowOwner owner(ClientFlowFormData client) {
    return new AddressFlowOwner(client.getRegistrationId(), ADDRESS_PURPOSE);
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {
    String path = mappedPath(request);
    HttpSession session = request.getSession(false);
    ClientFlowFormData client =
        session != null
                && session.getAttribute(CLIENT_FLOW_FORM_DATA) instanceof ClientFlowFormData data
            ? data
            : null;
    boolean register =
        path.equals("/application/client/results") && request.getMethod().equals("POST");
    boolean agreement = path.equals("/application/agreement");
    boolean newClientAgreement =
        agreement
            && (request.getParameterValues("registrationId") != null
                || (session != null
                    && session.getAttribute(APPLICATION_FORM_DATA)
                        instanceof ApplicationFormData data
                    && !data.isApplicationCreated()));
    if (path.startsWith(DETAILS) || newClientAgreement || (register && client != null)) {
      if (client == null
          || !client.getRegistrationId().equals(singleParameter(request, "registrationId"))) {
        return expired(response);
      }
    }
    if (register
        && client == null
        && request.getParameterValues("registrationId") != null
        && !"".equals(singleParameter(request, "registrationId"))) {
      return expired(response);
    }
    if (client != null) {
      request.setAttribute(CLIENT_SNAPSHOT, client);
    }
    if (path.equals(ADDRESS) || path.startsWith(ADDRESS + "/")) {
      AddressLookupFlowData<?> flow =
          session.getAttribute(CREATE_CLIENT_ADDRESS_FLOW) instanceof AddressLookupFlowData<?> data
              ? data
              : null;
      boolean entry = path.equals(ADDRESS) && request.getMethod().equals("GET");
      if (entry
          && !AddressFlowIdentity.belongsTo(flow, owner(client))
          && request.getParameterValues("flowId") == null) {
        return true;
      }
      if (!AddressFlowIdentity.matches(flow, owner(client), singleParameter(request, "flowId"))) {
        return expired(response);
      }
      // The main GET can refresh its own flow; mutations and result pages must use their revision.
      if (!entry
          && !AddressFlowIdentity.matchesRevision(flow, singleParameter(request, "revision"))) {
        return expired(response);
      }
    }
    return true;
  }

  @Override
  public void postHandle(
      HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView view) {
    if (view == null) {
      return;
    }
    HttpSession session = request.getSession(false);
    ClientFlowFormData client =
        view.getModel().get(CLIENT_FLOW_FORM_DATA) instanceof ClientFlowFormData data
            ? data
            : (ClientFlowFormData) request.getAttribute(CLIENT_SNAPSHOT);
    String path = mappedPath(request);
    // The entry controller replaces the registration; redirect models may omit session attributes.
    if (path.equals("/application/client/results")
        && request.getMethod().equals("POST")
        && session != null
        && session.getAttribute(CLIENT_FLOW_FORM_DATA) instanceof ClientFlowFormData data) {
      client = data;
    }
    if (client == null) {
      return;
    }
    AddressLookupFlowData<?> flow =
        session != null
                && session.getAttribute(CREATE_CLIENT_ADDRESS_FLOW)
                    instanceof AddressLookupFlowData<?> data
                && AddressFlowIdentity.belongsTo(data, owner(client))
            ? data
            : null;
    String name = view.getViewName();
    if (name != null && name.startsWith("redirect:")) {
      String target = name.substring("redirect:".length());
      if (target.startsWith(DETAILS) || target.equals("/application/agreement")) {
        view.setViewName("redirect:" + identityUrl(target, client, flow));
      }
      return;
    }
    view.addObject("registrationId", client.getRegistrationId());
    view.addObject("addressFlowId", flow == null ? null : flow.getFlowId());
    for (String key : List.of("changeAddressUrl", "manualEntryUrl", "searchAgainUrl")) {
      if (view.getModel().get(key) instanceof String url) {
        view.addObject(key, identityUrl(url, client, flow));
      }
    }
  }

  private static String identityUrl(
      String url, ClientFlowFormData client, AddressLookupFlowData<?> flow) {
    UriComponentsBuilder builder =
        UriComponentsBuilder.fromUriString(url)
            .replaceQueryParam("registrationId", client.getRegistrationId());
    if (flow != null && url.startsWith(ADDRESS)) {
      builder
          .replaceQueryParam("flowId", flow.getFlowId())
          .replaceQueryParam("revision", flow.getRevision());
    }
    return builder.build().encode().toUriString();
  }

  private static String singleParameter(HttpServletRequest request, String name) {
    String[] values = request.getParameterValues(name);
    return values != null && values.length == 1 ? values[0] : null;
  }

  private static String mappedPath(HttpServletRequest request) {
    Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
    return pattern != null
        ? pattern.toString()
        : request.getRequestURI().substring(request.getContextPath().length());
  }

  private static boolean expired(HttpServletResponse response) throws Exception {
    response.sendError(
        HttpServletResponse.SC_CONFLICT,
        "This client registration or address flow has expired. Return to client search to continue.");
    return false;
  }
}
