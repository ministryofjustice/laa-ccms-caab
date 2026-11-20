package uk.gov.laa.ccms.caab.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.laa.ccms.caab.bean.metric.PuiMetricService;
import uk.gov.laa.ccms.caab.security.CcmsUserIdentityResolver;
import uk.gov.laa.ccms.caab.service.UserService;

/** Independent, exhaustive expectations exercised through the production security filter chain. */
@SpringJUnitWebConfig(SecurityEventsEndToEndTest.Config.class)
@TestPropertySource(properties = "portal.logoutUrl=https://idp.example.test/logout")
class RouteAuthorizationTest {

  private static final String MANIFEST = "/security/route-authorisation.txt";
  @Autowired private WebApplicationContext context;
  @MockitoBean private UserService userService;
  @MockitoBean private PuiMetricService puiMetricService;
  @MockitoBean private CcmsUserIdentityResolver ccmsUserIdentityResolver;
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  private record Route(UnrestrictedRoutesTest.Route mapping, String path) {
    String key() {
      return mapping.key(path);
    }
  }

  private static Map<String, List<Route>> routes() throws ClassNotFoundException {
    Map<String, List<Route>> routes = new LinkedHashMap<>();
    for (var mapping : UnrestrictedRoutesTest.controllerRoutes()) {
      for (String path : UnrestrictedRoutesTest.expand(mapping.path())) {
        Route route = new Route(mapping, path);
        routes.computeIfAbsent(route.key(), unused -> new ArrayList<>()).add(route);
      }
    }
    return routes;
  }

  private static Map<String, String> expectations() throws IOException {
    try (var stream = RouteAuthorizationTest.class.getResourceAsStream(MANIFEST)) {
      assertThat(stream).as(MANIFEST).isNotNull();
      Map<String, String> expectations = new LinkedHashMap<>();
      for (String line :
          new String(stream.readAllBytes(), StandardCharsets.UTF_8).lines().toList()) {
        if (line.isBlank() || line.startsWith("#")) {
          continue;
        }
        String[] entry = line.split(" => ", -1);
        assertThat(entry).as(line).hasSize(2);
        assertThat(expectations.put(entry[0], entry[1]))
            .as("Duplicate route: " + entry[0])
            .isNull();
        if (entry[0].startsWith("GET ")) {
          assertThat(expectations.put("HEAD " + entry[0].substring(4), entry[1]))
              .as("Duplicate HEAD route: " + entry[0])
              .isNull();
        }
      }
      return expectations;
    }
  }

  private static void assertInventory(Set<String> actual, Set<String> expected) {
    assertThat(new TreeSet<>(actual))
        .as(
            "Every mapping, method, context and parameter condition needs an explicit policy in %s",
            MANIFEST)
        .containsExactlyInAnyOrderElementsOf(expected);
  }

  @Test
  void everyMappingHasAnIndependentExpectation() throws Exception {
    assertInventory(routes().keySet(), expectations().keySet());
  }

  @Test
  void aNewRouteUnderAnAlreadyProtectedPrefixStillFailsTheInventory() throws Exception {
    Set<String> actual = new TreeSet<>(routes().keySet());
    actual.add("GET /case/new-sensitive-action");
    assertThrows(AssertionError.class, () -> assertInventory(actual, expectations().keySet()));
  }

  static Stream<Arguments> routePolicies() throws Exception {
    Map<String, List<Route>> routes = routes();
    Map<String, String> expected = expectations();
    assertInventory(routes.keySet(), expected.keySet());
    return expected.entrySet().stream()
        .flatMap(
            entry ->
                routes.get(entry.getKey()).stream()
                    .map(route -> Arguments.of(Named.of(route.key(), route), entry.getValue())));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("routePolicies")
  void everyRouteChecksEveryFunctionAndItsCompositePrerequisites(Route route, String policy)
      throws Exception {
    Set<String> allFunctions = new TreeSet<>();
    Arrays.stream(UserRole.values()).map(UserRole::getCode).forEach(allFunctions::add);
    List<Set<String>> grants = new ArrayList<>();
    grants.add(Set.of());
    grants.add(Set.of("UNRELATED"));
    allFunctions.forEach(function -> grants.add(Set.of(function)));
    grants.add(allFunctions);
    List<Set<String>> alternatives = alternatives(policy);
    for (Set<String> required : alternatives) {
      assertThat(allFunctions).as("Known functions in %s", route.key()).containsAll(required);
      grants.add(required);
      for (String omitted : required) {
        Set<String> withoutOne = new TreeSet<>(required);
        withoutOne.remove(omitted);
        grants.add(withoutOne);
      }
    }
    for (Set<String> functions : grants.stream().distinct().toList()) {
      boolean allowed =
          "PUBLIC".equals(policy)
              || "AUTHENTICATED".equals(policy)
              || alternatives.stream().anyMatch(functions::containsAll);
      int actual =
          mockMvc
              .perform(
                  requestFor(route)
                      .with(
                          user("user")
                              .authorities(
                                  functions.stream().map(SimpleGrantedAuthority::new).toList())))
              .andReturn()
              .getResponse()
              .getStatus();
      assertThat(actual).as("%s with %s", route.key(), functions).isEqualTo(allowed ? 404 : 403);
    }
    int unauthenticated = mockMvc.perform(requestFor(route)).andReturn().getResponse().getStatus();
    assertThat(unauthenticated)
        .as("Unauthenticated: %s", route.key())
        .isEqualTo("PUBLIC".equals(policy) ? 404 : 302);
  }

  private static List<Set<String>> alternatives(String policy) {
    if ("PUBLIC".equals(policy) || "AUTHENTICATED".equals(policy)) {
      return List.of();
    }
    return Arrays.stream(policy.split("\\|"))
        .map(alternative -> Set.of(alternative.split("\\+")))
        .toList();
  }

  private static MockHttpServletRequestBuilder requestFor(Route route) {
    var builder =
        request(route.mapping().method(), route.path())
            .header("Sec-Fetch-Mode", "cors")
            .with(csrf());
    for (String parameter : route.mapping().parameters()) {
      if (!parameter.startsWith("!")) {
        String[] value = parameter.split("=", 2);
        builder.param(value[0], value.length == 2 ? value[1] : "1");
      }
    }
    for (String header : route.mapping().headers()) {
      if (!header.startsWith("!")) {
        String[] value = header.split("=", 2);
        builder.header(value[0], value.length == 2 ? value[1] : "1");
      }
    }
    if (!route.mapping().consumes().isEmpty()) {
      builder.contentType(route.mapping().consumes().getFirst());
    }
    if (!route.mapping().produces().isEmpty()) {
      builder.accept(route.mapping().produces().getFirst());
    }
    return builder;
  }
}
