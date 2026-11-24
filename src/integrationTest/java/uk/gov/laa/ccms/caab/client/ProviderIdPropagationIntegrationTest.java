package uk.gov.laa.ccms.caab.client;

import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import uk.gov.laa.ccms.caab.AbstractIntegrationTest;
import uk.gov.laa.ccms.caab.security.ProviderContext;

class ProviderIdPropagationIntegrationTest extends AbstractIntegrationTest {

  @RegisterExtension
  static WireMockExtension wiremock =
      WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

  @DynamicPropertySource
  static void properties(final DynamicPropertyRegistry registry) {
    registry.add("laa.ccms.ebs-api.url", wiremock::baseUrl);
    registry.add("laa.ccms.soa-api.url", wiremock::baseUrl);
    registry.add("laa.ccms.caab-api.url", wiremock::baseUrl);
    registry.add("laa.ccms.assessment-api.url", wiremock::baseUrl);
    registry.add("os.api.url", wiremock::baseUrl);
    registry.add("laa.ccms.user-details-api.url", wiremock::baseUrl);
  }

  @Autowired
  @Qualifier("ebsApiWebClient")
  private WebClient ebsApiWebClient;

  @Autowired
  @Qualifier("soaApiWebClient")
  private WebClient soaApiWebClient;

  @Autowired
  @Qualifier("caabApiWebClient")
  private WebClient caabApiWebClient;

  @Autowired
  @Qualifier("assessmentApiWebClient")
  private WebClient assessmentApiWebClient;

  @Autowired
  @Qualifier("osApiWebClient")
  private WebClient osApiWebClient;

  @Autowired
  @Qualifier("userDetailsApiWebClient")
  private WebClient userDetailsApiWebClient;

  @BeforeEach
  void setUp() {
    wiremock.resetAll();
    ProviderContext.clear();
  }

  @AfterEach
  void tearDown() {
    ProviderContext.clear();
  }

  @Test
  void propagatesCaabProviderIdAcrossChainedFlatMapHopsForAllInternalApiWebClients() {
    wiremock.stubFor(get(urlEqualTo("/ebs-probe")).willReturn(okJson("{\"step\":1}")));
    wiremock.stubFor(get(urlEqualTo("/soa-probe")).willReturn(okJson("{\"step\":2}")));
    wiremock.stubFor(get(urlEqualTo("/caab-probe")).willReturn(okJson("{\"step\":3}")));
    wiremock.stubFor(get(urlEqualTo("/assessment-probe")).willReturn(okJson("{\"step\":4}")));
    wiremock.stubFor(get(urlEqualTo("/os-probe")).willReturn(okJson("{\"external\":true}")));
    wiremock.stubFor(
        get(urlEqualTo("/user-details-probe")).willReturn(okJson("{\"external\":true}")));

    final String callerThread = Thread.currentThread().getName();
    final List<String> hopThreads = new CopyOnWriteArrayList<>();

    ProviderContext.setProviderId("12345");

    final String finalResponse =
        Mono.fromCallable(() -> "seed")
            .subscribeOn(Schedulers.parallel())
            .flatMap(
                ignored -> {
                  hopThreads.add(Thread.currentThread().getName());
                  return ebsApiWebClient
                      .get()
                      .uri("/ebs-probe")
                      .retrieve()
                      .bodyToMono(String.class);
                })
            .publishOn(Schedulers.boundedElastic())
            .flatMap(
                ignored -> {
                  hopThreads.add(Thread.currentThread().getName());
                  return soaApiWebClient
                      .get()
                      .uri("/soa-probe")
                      .retrieve()
                      .bodyToMono(String.class);
                })
            .publishOn(Schedulers.parallel())
            .flatMap(
                ignored -> {
                  hopThreads.add(Thread.currentThread().getName());
                  return caabApiWebClient
                      .get()
                      .uri("/caab-probe")
                      .retrieve()
                      .bodyToMono(String.class);
                })
            .flatMap(
                ignored -> {
                  hopThreads.add(Thread.currentThread().getName());
                  return assessmentApiWebClient
                      .get()
                      .uri("/assessment-probe")
                      .retrieve()
                      .bodyToMono(String.class);
                })
            .flatMap(
                result ->
                    osApiWebClient
                        .get()
                        .uri("/os-probe")
                        .retrieve()
                        .bodyToMono(String.class)
                        .then(
                            userDetailsApiWebClient
                                .get()
                                .uri("/user-details-probe")
                                .retrieve()
                                .bodyToMono(String.class))
                        .thenReturn(result))
            .block();

    assertThat(finalResponse).contains("\"step\":4");
    assertThat(hopThreads).hasSize(4).noneMatch(callerThread::equals);

    wiremock.verify(
        getRequestedFor(urlEqualTo("/ebs-probe"))
            .withHeader(ProviderContext.HEADER_NAME, equalTo("12345")));
    wiremock.verify(
        getRequestedFor(urlEqualTo("/soa-probe"))
            .withHeader(ProviderContext.HEADER_NAME, equalTo("12345")));
    wiremock.verify(
        getRequestedFor(urlEqualTo("/caab-probe"))
            .withHeader(ProviderContext.HEADER_NAME, equalTo("12345")));
    wiremock.verify(
        getRequestedFor(urlEqualTo("/assessment-probe"))
            .withHeader(ProviderContext.HEADER_NAME, equalTo("12345")));

    wiremock.verify(
        getRequestedFor(urlEqualTo("/os-probe")).withHeader(ProviderContext.HEADER_NAME, absent()));
    wiremock.verify(
        getRequestedFor(urlEqualTo("/user-details-probe"))
            .withHeader(ProviderContext.HEADER_NAME, absent()));
  }

  @Test
  void omitsCaabProviderIdWhenNoProviderIsInContext() {
    wiremock.stubFor(get(urlEqualTo("/users/entra/user@example.com")).willReturn(okJson("{}")));
    wiremock.stubFor(get(urlEqualTo("/soa-unscoped")).willReturn(okJson("{}")));
    wiremock.stubFor(get(urlEqualTo("/caab-unscoped")).willReturn(okJson("{}")));
    wiremock.stubFor(get(urlEqualTo("/assessment-unscoped")).willReturn(okJson("{}")));

    Mono.just("unscoped")
        .publishOn(Schedulers.parallel())
        .flatMap(
            ignored ->
                ebsApiWebClient
                    .get()
                    .uri("/users/entra/user@example.com")
                    .retrieve()
                    .bodyToMono(String.class))
        .flatMap(
            ignored ->
                soaApiWebClient.get().uri("/soa-unscoped").retrieve().bodyToMono(String.class))
        .flatMap(
            ignored ->
                caabApiWebClient.get().uri("/caab-unscoped").retrieve().bodyToMono(String.class))
        .flatMap(
            ignored ->
                assessmentApiWebClient
                    .get()
                    .uri("/assessment-unscoped")
                    .retrieve()
                    .bodyToMono(String.class))
        .block();

    wiremock.verify(
        getRequestedFor(urlEqualTo("/users/entra/user@example.com"))
            .withHeader(ProviderContext.HEADER_NAME, absent()));
    wiremock.verify(
        getRequestedFor(urlEqualTo("/soa-unscoped"))
            .withHeader(ProviderContext.HEADER_NAME, absent()));
    wiremock.verify(
        getRequestedFor(urlEqualTo("/caab-unscoped"))
            .withHeader(ProviderContext.HEADER_NAME, absent()));
    wiremock.verify(
        getRequestedFor(urlEqualTo("/assessment-unscoped"))
            .withHeader(ProviderContext.HEADER_NAME, absent()));
  }
}
