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
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Controller;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.WebApplicationContext;

/**
 * Fails when a controller route can be reached without any user function and is not listed in
 * {@code security/unrestricted-routes.txt}, so that a new route for a restricted action cannot be
 * added without a rule in {@link SecurityConfiguration#authorizeUserFunctions}.
 */
@SpringJUnitWebConfig(UserFunctionAuthorizationTest.Config.class)
@DisplayName("Routes reachable without a user function")
class UnrestrictedRoutesTest {

  private static final String CONTROLLER_PACKAGE = "uk.gov.laa.ccms.caab";
  private static final String ALLOWLIST = "/security/unrestricted-routes.txt";
  private static final Pattern PATH_VARIABLE = Pattern.compile("\\{([^}:]+)(?::[^}]*)?}");

  /** Values a path variable can take, where the rules treat them differently. */
  private static final Map<String, List<String>> VARIABLE_VALUES =
      Map.of(
          "caseContext", List.of("application", "amendments"),
          "billingContext", List.of("bill", "poa"),
          "action", List.of("add", "edit"),
          "submissionContext", List.of("undertaking"));

  @Autowired private WebApplicationContext context;

  @Test
  @DisplayName("Every route reachable without a user function is listed as deliberately open")
  void unrestrictedRoutesMatchAllowlist() throws Exception {
    MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();

    TreeSet<String> unrestricted = new TreeSet<>();
    for (Route route : controllerRoutes()) {
      for (String path : expand(route.path())) {
        int status =
            mockMvc
                .perform(
                    request(route.method(), path)
                        .with(user("user").authorities(() -> "NONE"))
                        .with(csrf()))
                .andReturn()
                .getResponse()
                .getStatus();
        if (status == 404) {
          unrestricted.add(route.method() + " " + displayPath(route.path(), path));
        }
      }
    }

    assertThat(unrestricted)
        .as(
            "Routes reachable without a user function. Add a rule to"
                + " SecurityConfiguration.authorizeUserFunctions, or list the route in %s if it"
                + " is meant to be open",
            ALLOWLIST)
        .containsExactlyInAnyOrderElementsOf(allowlist());
  }

  private record Route(HttpMethod method, String path) {}

  private static List<Route> controllerRoutes() throws ClassNotFoundException {
    ClassPathScanningCandidateComponentProvider scanner =
        new ClassPathScanningCandidateComponentProvider(false);
    scanner.addIncludeFilter(new AnnotationTypeFilter(Controller.class));

    List<Route> routes = new ArrayList<>();
    for (BeanDefinition candidate : scanner.findCandidateComponents(CONTROLLER_PACKAGE)) {
      Class<?> controller = Class.forName(candidate.getBeanClassName());
      if (!isProductionClass(controller)) {
        continue;
      }
      RequestMapping classMapping =
          AnnotatedElementUtils.findMergedAnnotation(controller, RequestMapping.class);
      String[] prefixes =
          classMapping == null || classMapping.path().length == 0
              ? new String[] {""}
              : classMapping.path();

      for (Method method : controller.getDeclaredMethods()) {
        RequestMapping mapping =
            AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
        if (mapping == null) {
          continue;
        }
        String[] paths = mapping.path().length == 0 ? new String[] {""} : mapping.path();
        RequestMethod[] methods =
            mapping.method().length == 0
                ? new RequestMethod[] {RequestMethod.GET}
                : mapping.method();
        for (String prefix : prefixes) {
          for (String path : paths) {
            for (RequestMethod requestMethod : methods) {
              routes.add(new Route(requestMethod.asHttpMethod(), prefix + path));
            }
          }
        }
      }
    }
    return routes;
  }

  /** Test sources declare their own controllers in the same packages; only the app's count. */
  private static boolean isProductionClass(Class<?> type) {
    return !type.getProtectionDomain().getCodeSource().getLocation().getPath().contains("/test/");
  }

  /** Concrete request paths for a route, one per combination of significant variable values. */
  private static List<String> expand(String template) {
    Matcher matcher = PATH_VARIABLE.matcher(template);
    if (!matcher.find()) {
      return List.of(template);
    }
    List<String> values = VARIABLE_VALUES.getOrDefault(matcher.group(1), List.of("1"));
    List<String> expanded = new ArrayList<>();
    for (String value : values) {
      String next =
          template.substring(0, matcher.start()) + value + template.substring(matcher.end());
      expanded.addAll(expand(next));
    }
    return expanded;
  }

  /** The route as listed: variables the rules distinguish written out, others as placeholders. */
  private static String displayPath(String template, String path) {
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
          .toList();
    }
  }
}
