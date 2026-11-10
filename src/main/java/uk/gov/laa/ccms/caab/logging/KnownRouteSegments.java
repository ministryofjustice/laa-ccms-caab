package uk.gov.laa.ccms.caab.logging;

import java.util.HashSet;
import java.util.Set;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/** Tells {@link RequestRoute} which path segments are fixed parts of the application's routes. */
@Component
public class KnownRouteSegments {

  @EventListener
  void onRefresh(ContextRefreshedEvent event) {
    Set<String> segments = new HashSet<>();
    event
        .getApplicationContext()
        .getBeansOfType(RequestMappingHandlerMapping.class)
        .values()
        .forEach(
            mapping ->
                mapping.getHandlerMethods().keySet().forEach(info -> addLiterals(info, segments)));
    RequestRoute.registerKnownSegments(segments);
  }

  private static void addLiterals(RequestMappingInfo info, Set<String> segments) {
    if (info.getPathPatternsCondition() == null) {
      return;
    }
    for (String pattern : info.getPathPatternsCondition().getPatternValues()) {
      for (String segment : pattern.split("/")) {
        if (!segment.isEmpty() && !segment.contains("{") && !segment.contains("*")) {
          segments.add(segment);
        }
      }
    }
  }
}
