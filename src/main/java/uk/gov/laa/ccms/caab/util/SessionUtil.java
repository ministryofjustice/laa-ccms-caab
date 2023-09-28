package uk.gov.laa.ccms.caab.util;

import jakarta.servlet.http.HttpSession;
import java.util.Enumeration;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SessionUtil {

  public static void printAllSessionAttributes(HttpSession session) {
    Enumeration<String> attributeNames = session.getAttributeNames();
    while (attributeNames.hasMoreElements()) {
      String attributeName = attributeNames.nextElement();
      Object attributeValue = session.getAttribute(attributeName);
      log.debug("Session Attribute Name: {}, Value: {}", attributeName, attributeValue);
    }
  }

  public static void printAllSessionAttributeNames(HttpSession session) {
    Enumeration<String> attributeNames = session.getAttributeNames();
    while (attributeNames.hasMoreElements()) {
      String attributeName = attributeNames.nextElement();
      log.debug("Session Attribute Name: {}", attributeName);
    }
  }
}
