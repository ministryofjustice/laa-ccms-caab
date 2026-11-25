package uk.gov.laa.ccms.caab.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Controller;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Fails when a controller route can be reached without any user function and is not listed in
 * {@code security/unrestricted-routes.txt}, so that a new route for a restricted action cannot be
 * added without a rule in {@link SecurityConfiguration#authoriseUserFunctions}.
 */
@SpringJUnitWebConfig(UserFunctionAuthorisationTest.Config.class)
@DisplayName("Routes reachable without a user function")
class UnrestrictedRoutesTest {

  private static final String CONTROLLER_PACKAGE = "uk.gov.laa.ccms.caab";
  private static final String ALLOWLIST = "/security/unrestricted-routes.txt";
  private static final Pattern PATH_VARIABLE = Pattern.compile("\\{([^}:]+)(?::([^}]+))?}");

  /** Values a path variable can take, where the rules treat them differently. */
  private static final Map<String, List<String>> VARIABLE_VALUES =
      Map.of(
          "caseContext", List.of("application", "amendments"),
          "billingContext", List.of("bill", "poa"),
          "action", List.of("add", "edit"),
          "submissionContext", List.of("undertaking"),
          "submissionType",
              List.of(
                  "client",
                  "client-create",
                  "client-update",
                  "notification-attachments",
                  "submit-case",
                  "submit-case-provider-request",
                  "submit-general-provider-request"));

  @Autowired private WebApplicationContext context;

  @Test
  @DisplayName("Every route reachable without a user function is listed as deliberately open")
  void unrestrictedRoutesMatchAllowlist() throws Exception {
    MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();

    TreeSet<String> unrestricted = new TreeSet<>();
    for (Route route : controllerRoutes()) {
      for (String path : expand(route.path())) {
        String listed = displayPath(route.path(), path);
        boolean reached = reachesHandler(mockMvc, route.method(), path);
        if (reached) {
          unrestricted.add(route.method() + " " + listed);
        }
        // Spring MVC also serves HEAD through every GET mapping.
        if (route.method() == HttpMethod.GET
            && !reached
            && reachesHandler(mockMvc, HttpMethod.HEAD, path)) {
          unrestricted.add(HttpMethod.HEAD + " " + listed);
        }
      }
    }

    assertThat(unrestricted)
        .as(
            "Routes reachable without a user function. Add a rule to"
                + " SecurityConfiguration.authoriseUserFunctions, or list the route in %s if it"
                + " is meant to be open",
            ALLOWLIST)
        .containsExactlyInAnyOrderElementsOf(allowlist());
  }

  /** No controllers are registered, so a request the rules let through ends in a 404. */
  private static boolean reachesHandler(MockMvc mockMvc, HttpMethod method, String path)
      throws Exception {
    return mockMvc
            .perform(
                request(method, path).with(user("user").authorities(() -> "NONE")).with(csrf()))
            .andReturn()
            .getResponse()
            .getStatus()
        == 404;
  }

  record Route(
      HttpMethod method,
      String path,
      List<String> parameters,
      List<String> headers,
      List<String> consumes,
      List<String> produces) {
    String key(String expandedPath) {
      return method
          + " "
          + displayPath(path, expandedPath)
          + (parameters.isEmpty() ? "" : " [" + String.join(",", parameters) + "]")
          + (headers.isEmpty() ? "" : " [headers:" + String.join(",", headers) + "]")
          + (consumes.isEmpty() ? "" : " [consumes:" + String.join(",", consumes) + "]")
          + (produces.isEmpty() ? "" : " [produces:" + String.join(",", produces) + "]");
    }
  }

  static List<Route> controllerRoutes() throws ClassNotFoundException {
    ClassPathScanningCandidateComponentProvider scanner =
        new ClassPathScanningCandidateComponentProvider(false);
    scanner.addIncludeFilter(new AnnotationTypeFilter(Controller.class));

    List<Route> routes = new ArrayList<>();
    for (BeanDefinition candidate : scanner.findCandidateComponents(CONTROLLER_PACKAGE)) {
      Class<?> controller =
          ((AbstractBeanDefinition) candidate)
              .resolveBeanClass(UnrestrictedRoutesTest.class.getClassLoader());
      if (!isProductionClass(controller)) {
        continue;
      }
      MethodIntrospector.MetadataLookup<RequestMappingInfo> lookup =
          method -> new MappingInspector().mapping(method, controller);
      for (RequestMappingInfo mapping :
          MethodIntrospector.selectMethods(controller, lookup).values()) {
        var methods = mapping.getMethodsCondition().getMethods();
        if (methods.isEmpty()) {
          methods = java.util.Set.of(RequestMethod.values());
        }
        List<String> parameters =
            mapping.getParamsCondition().getExpressions().stream()
                .map(Object::toString)
                .sorted()
                .toList();
        List<String> headers =
            mapping.getHeadersCondition().getExpressions().stream()
                .map(Object::toString)
                .sorted()
                .toList();
        List<String> consumes =
            mapping.getConsumesCondition().getExpressions().stream()
                .map(Object::toString)
                .sorted()
                .toList();
        List<String> produces =
            mapping.getProducesCondition().getExpressions().stream()
                .map(Object::toString)
                .sorted()
                .toList();
        for (String path : mapping.getPatternValues()) {
          for (RequestMethod requestMethod : methods) {
            routes.add(
                new Route(
                    requestMethod.asHttpMethod(), path, parameters, headers, consumes, produces));
            if (requestMethod == RequestMethod.GET && !methods.contains(RequestMethod.HEAD)) {
              routes.add(new Route(HttpMethod.HEAD, path, parameters, headers, consumes, produces));
            }
          }
        }
      }
    }
    return routes;
  }

  private static class MappingInspector extends RequestMappingHandlerMapping {
    RequestMappingInfo mapping(Method method, Class<?> controller) {
      return getMappingForMethod(method, controller);
    }
  }

  /** Test sources declare their own controllers in the same packages; only the app's count. */
  private static boolean isProductionClass(Class<?> type) {
    return type.getProtectionDomain()
        .getCodeSource()
        .getLocation()
        .equals(
            uk.gov.laa.ccms.caab.CaabApplication.class
                .getProtectionDomain()
                .getCodeSource()
                .getLocation());
  }

  /** Concrete request paths for a route, one per combination of significant variable values. */
  static List<String> expand(String template) {
    Matcher matcher = PATH_VARIABLE.matcher(template);
    if (!matcher.find()) {
      return List.of(template);
    }
    if (matcher.group(2) != null
        && !matcher.group(2).matches("[A-Za-z0-9_-]+(?:\\|[A-Za-z0-9_-]+)*")) {
      throw new AssertionError("Add explicit request samples for constrained route " + template);
    }
    List<String> values =
        VARIABLE_VALUES.getOrDefault(
            matcher.group(1),
            matcher.group(2) == null ? List.of("1") : List.of(matcher.group(2).split("\\|")));
    List<String> expanded = new ArrayList<>();
    for (String value : values) {
      String next =
          template.substring(0, matcher.start()) + value + template.substring(matcher.end());
      expanded.addAll(expand(next));
    }
    return expanded;
  }

  /** The route as listed: variables the rules distinguish written out, others as placeholders. */
  static String displayPath(String template, String path) {
    if (template.isEmpty()) {
      return "(context-root)";
    }
    String[] templateSegments = template.split("/", -1);
    String[] pathSegments = path.split("/", -1);
    for (int i = 0; i < templateSegments.length; i++) {
      Matcher matcher = PATH_VARIABLE.matcher(templateSegments[i]);
      if (matcher.matches() && VARIABLE_VALUES.containsKey(matcher.group(1))) {
        templateSegments[i] = pathSegments[i];
      }
    }
    return String.join("/", templateSegments);
  }

  private static List<String> allowlist() throws IOException {
    try (InputStream stream = UnrestrictedRoutesTest.class.getResourceAsStream(ALLOWLIST)) {
      assertThat(stream).as(ALLOWLIST).isNotNull();
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8)
          .lines()
          .map(String::strip)
          .filter(line -> !line.isEmpty() && !line.startsWith("#"))
          .flatMap(
              line ->
                  line.startsWith("GET ")
                      ? java.util.stream.Stream.of(line, "HEAD " + line.substring(4))
                      : java.util.stream.Stream.of(line))
          .toList();
    }
  }
}
