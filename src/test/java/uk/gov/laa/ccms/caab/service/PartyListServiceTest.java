package uk.gov.laa.ccms.caab.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_CONTACT_TITLE;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.Errors;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.bean.validators.opponent.IndividualOpponentOtherDetailsValidator;
import uk.gov.laa.ccms.caab.bean.validators.opponent.IndividualOpponentValidator;
import uk.gov.laa.ccms.caab.bean.validators.opponent.OrganisationOpponentValidator;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.mapper.OpponentMapperImpl;
import uk.gov.laa.ccms.caab.model.AddressDetail;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.OpponentDetail;
import uk.gov.laa.ccms.caab.model.sections.PartyListRow;
import uk.gov.laa.ccms.caab.service.PartyListService.PartyListCandidate;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;
import uk.gov.laa.ccms.data.model.RelationshipToCaseLookupDetail;
import uk.gov.laa.ccms.data.model.RelationshipToCaseLookupValueDetail;

@ExtendWith(MockitoExtension.class)
class PartyListServiceTest {

  private static final String CASE_REF = "300000001";

  @Mock private ApplicationService applicationService;
  @Mock private CaseOutcomeService caseOutcomeService;
  @Mock private LookupService lookupService;
  @Mock private IndividualOpponentFormService individualOpponentFormService;
  @Mock private IndividualOpponentValidator individualOpponentValidator;
  @Mock private OrganisationOpponentValidator organisationOpponentValidator;
  @Mock private IndividualOpponentOtherDetailsValidator individualOpponentOtherDetailsValidator;

  private PartyListService service;
  private ApplicationDetail ebsCase;

  @BeforeEach
  void setUp() {
    final LocalValidatorFactoryBean beanValidator = new LocalValidatorFactoryBean();
    beanValidator.afterPropertiesSet();
    service =
        new PartyListService(
            applicationService,
            caseOutcomeService,
            lookupService,
            individualOpponentFormService,
            new OpponentMapperImpl(),
            individualOpponentValidator,
            organisationOpponentValidator,
            individualOpponentOtherDetailsValidator,
            beanValidator);
    ebsCase = new ApplicationDetail().caseReferenceNumber(CASE_REF);
    lenient()
        .when(caseOutcomeService.getAwardLiableParties(CASE_REF, 123, "land", 7))
        .thenReturn(List.of());
  }

  private List<PartyListCandidate> candidates(
      final ApplicationDetail currentApplication, final Set<String> createdEbsIds) {
    return service.getCandidates(ebsCase, currentApplication, 123, "land", 7, createdEbsIds);
  }

  @Test
  void listsEveryIdentifiableOpponentWithStableKeys() {
    ebsCase.setOpponents(
        Arrays.asList(
            new OpponentDetail().id(1).ebsId("E1"),
            null,
            new OpponentDetail().id(1).ebsId("E1"),
            new OpponentDetail().ebsId("E2"),
            new OpponentDetail().ebsId(" "),
            new OpponentDetail()));

    assertThat(candidates(null, Set.of()))
        .extracting(PartyListCandidate::key, PartyListCandidate::alreadyAdded)
        .containsExactly(tuple("id-1", false), tuple("ebs-E2", false));
  }

  @Test
  void marksOpponentsLinkedToTheAwardOrCreatedInThisSessionAsAdded() {
    ebsCase.setOpponents(
        List.of(
            new OpponentDetail().id(1),
            new OpponentDetail().ebsId("E2"),
            new OpponentDetail().ebsId("E3"),
            new OpponentDetail().ebsId("E4")));
    when(caseOutcomeService.getAwardLiableParties(CASE_REF, 123, "land", 7))
        .thenReturn(List.of(new OpponentDetail().id(1), new OpponentDetail().id(9).ebsId("E2")));

    assertThat(candidates(null, Set.of("E3")))
        .extracting(PartyListCandidate::alreadyAdded)
        .containsExactly(true, true, true, false);
  }

  @Test
  void missingIdentifiersDoNotMatchEachOther() {
    ebsCase.setOpponents(
        List.of(
            new OpponentDetail().id(1),
            new OpponentDetail().id(2).ebsId(" "),
            new OpponentDetail().ebsId("E3")));
    when(caseOutcomeService.getAwardLiableParties(CASE_REF, 123, "land", 7))
        .thenReturn(
            List.of(
                new OpponentDetail().id(9), new OpponentDetail().ebsId(" "), new OpponentDetail()));

    assertThat(candidates(null, Set.of("", " ")))
        .extracting(PartyListCandidate::alreadyAdded)
        .containsExactly(false, false, false);
  }

  @Test
  void reloadsThePersistedDraftForTheSameCase() {
    final ApplicationDetail draft = new ApplicationDetail().caseReferenceNumber(CASE_REF);
    draft.setId(5);
    ebsCase.setOpponents(List.of(new OpponentDetail().ebsId("STALE")));
    final ApplicationDetail reloaded = new ApplicationDetail();
    reloaded.setOpponents(List.of(new OpponentDetail().id(1)));
    when(applicationService.getApplication("5")).thenReturn(Mono.just(reloaded));

    assertThat(candidates(draft, Set.of()))
        .extracting(PartyListCandidate::key)
        .containsExactly("id-1");
  }

  @Test
  void usesTheCaseWhenTheDraftBelongsToAnotherCase() {
    final ApplicationDetail otherDraft = new ApplicationDetail().caseReferenceNumber("OTHER");
    otherDraft.setId(5);
    ebsCase.setOpponents(List.of(new OpponentDetail().ebsId("E1")));

    assertThat(candidates(otherDraft, Set.of()))
        .extracting(PartyListCandidate::key)
        .containsExactly("ebs-E1");
    verifyNoInteractions(applicationService);
  }

  @Test
  void failsWhenThePersistedDraftCannotBeReloaded() {
    final ApplicationDetail draft = new ApplicationDetail().caseReferenceNumber(CASE_REF);
    draft.setId(5);
    when(applicationService.getApplication("5")).thenReturn(Mono.empty());

    assertThatThrownBy(() -> candidates(draft, Set.of()))
        .isInstanceOf(CaabApplicationException.class)
        .hasMessageContaining("5");
  }

  @Test
  void buildsRowsWithOrganisationNamesTitledIndividualsAndLookupDescriptions() {
    when(lookupService.getCommonValues(COMMON_VALUE_CONTACT_TITLE))
        .thenReturn(
            Mono.just(
                new CommonLookupDetail()
                    .addContentItem(new CommonLookupValueDetail().code("MS").description("Ms"))));
    when(lookupService.getOrganisationToCaseRelationships())
        .thenReturn(
            Mono.just(
                new RelationshipToCaseLookupDetail()
                    .addContentItem(
                        new RelationshipToCaseLookupValueDetail()
                            .code("OPP")
                            .description("Opponent"))));
    when(lookupService.getPersonToCaseRelationships()).thenReturn(Mono.empty());
    final OpponentDetail organisation =
        new OpponentDetail()
            .type("Organisation")
            .organisationName("Acme Ltd")
            .contactNameRole("Contact")
            .relationshipToCase("OPP")
            .address(new AddressDetail().addressLine1("1 High Street").postcode("AB1 2CD"));
    final OpponentDetail individual =
        new OpponentDetail()
            .type("Individual")
            .title("MS")
            .firstName("Jane")
            .surname("Smith")
            .relationshipToCase("RAW");

    assertThat(
            service.toRows(
                List.of(
                    new PartyListCandidate("ebs-E1", organisation, false),
                    new PartyListCandidate("id-2", individual, true))))
        .containsExactly(
            new PartyListRow(
                "ebs-E1", "Acme Ltd", "Organisation", "Opponent", "1 High Street AB1 2CD", false),
            new PartyListRow("id-2", "Ms Jane Smith", "Individual", "RAW", "", true));
  }

  @Test
  void namesUnnamedOrganisationsByTheirContact() {
    when(lookupService.getCommonValues(COMMON_VALUE_CONTACT_TITLE)).thenReturn(Mono.empty());
    when(lookupService.getOrganisationToCaseRelationships()).thenReturn(Mono.empty());
    when(lookupService.getPersonToCaseRelationships()).thenReturn(Mono.empty());
    final OpponentDetail organisation =
        new OpponentDetail().type("Organisation").contactNameRole("Jo Bloggs, Manager");

    assertThat(service.toRows(List.of(new PartyListCandidate("ebs-E1", organisation, false))))
        .extracting(PartyListRow::name)
        .containsExactly("Jo Bloggs, Manager");
  }

  @Test
  void validatesOnlyOpponentsThatMustBeCreated() {
    final OpponentDetail existing = new OpponentDetail().id(1).type("Unknown");

    assertThat(service.findInvalid(List.of(new PartyListCandidate("id-1", existing, false))))
        .isEmpty();
    verifyNoInteractions(
        individualOpponentFormService, individualOpponentValidator, organisationOpponentValidator);
  }

  @Test
  void reportsSelectedOpponentsThatFailValidation() {
    final Errors[] individualErrors = new Errors[1];
    doAnswer(
            invocation -> {
              final Errors errors = invocation.getArgument(1);
              errors.reject("invalid");
              return null;
            })
        .when(organisationOpponentValidator)
        .validate(any(), any());
    doAnswer(
            invocation -> {
              individualErrors[0] = invocation.getArgument(1);
              return null;
            })
        .when(individualOpponentValidator)
        .validateLiableParty(any(), any());

    final var selected =
        List.of(
            new PartyListCandidate(
                "ebs-ORG", new OpponentDetail().ebsId("ORG").type("Organisation"), false),
            new PartyListCandidate(
                "ebs-IND", new OpponentDetail().ebsId("IND").type("Individual"), false),
            new PartyListCandidate(
                "ebs-UNKNOWN", new OpponentDetail().ebsId("UNKNOWN").type("Unknown"), false));

    assertThat(service.findInvalid(selected)).containsExactlyInAnyOrder("ebs-ORG", "ebs-UNKNOWN");
    verify(individualOpponentFormService).prepareForValidation(any());
    verify(individualOpponentOtherDetailsValidator).validate(any(), any());
    assertThat(individualErrors[0].hasErrors()).isFalse();
  }
}
