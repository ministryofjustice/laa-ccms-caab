package uk.gov.laa.ccms.caab.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetails;
import uk.gov.laa.ccms.data.model.CommonLookupValueListDetails;
import uk.gov.laa.ccms.data.model.UserDetails;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DataService {
    private final WebClient dataWebClient;

    private final DataServiceErrorHandler dataServiceErrorHandler;

    public static final List<String> EXCLUDED_APPLICATION_TYPE_CODES = Arrays.asList("DP", "ECF", "SUBDP");
    public static final String COMMON_VALUE_APPLICATION_TYPE = "XXCCMS_APP_AMEND_TYPES";
    public static final String COMMON_VALUE_CATEGORY_OF_LAW = "XXCCMS_CATEGORY_OF_LAW";

    public static final String COMMON_VALUE_GENDER = "HZ_GENDER";
    public static final String COMMON_VALUE_UNIQUE_IDENTIFIER_TYPE = "XXCCMS_UNIQUE_ID_TYPE";


    public DataService(@Qualifier("dataWebClient") WebClient dataWebClient,
        DataServiceErrorHandler dataServiceErrorHandler) {
        this.dataWebClient = dataWebClient;
        this.dataServiceErrorHandler = dataServiceErrorHandler;
    }

    public Mono<UserDetails> getUser(String loginId){

        return dataWebClient
                .get()
                .uri("/users/{loginId}", loginId)
                .retrieve()
                .bodyToMono(UserDetails.class)
                .onErrorResume(e -> dataServiceErrorHandler.handleUserError(loginId, e));
    }

    public Mono<CommonLookupValueListDetails> getCommonValues(String type, String code, String sort) {

        return dataWebClient
                .get()
                .uri(builder -> builder.path("/common-lookup-values")
                        .queryParamIfPresent("type", Optional.ofNullable(type))
                        .queryParamIfPresent("code", Optional.ofNullable(code))
                        .queryParamIfPresent("sort", Optional.ofNullable(sort))
                        .build())
                .retrieve()
                .bodyToMono(CommonLookupValueListDetails.class)
                .onErrorResume(e -> dataServiceErrorHandler.handleCommonValuesError(type, code, sort, e));
    }

    public List<CommonLookupValueDetails> getApplicationTypes() {
        CommonLookupValueListDetails allApplicationTypes = getCommonValues(COMMON_VALUE_APPLICATION_TYPE, null, null).block();

        List<CommonLookupValueDetails> applicationTypes = allApplicationTypes.getContent().stream()
                .filter(applicationType -> {
                    String code = applicationType.getCode().toUpperCase();
                    return !EXCLUDED_APPLICATION_TYPE_CODES.contains(code);
                })
                .collect(Collectors.toList());

        return applicationTypes;
    }

    public List<CommonLookupValueDetails> getGenders() {
        CommonLookupValueListDetails commonLookupValues = getCommonValues(COMMON_VALUE_GENDER, null, null).block();
        List<CommonLookupValueDetails> genders = commonLookupValues.getContent().stream()
                .collect(Collectors.toList());

        return genders;
    }

    public List<CommonLookupValueDetails> getUniqueIdentifierTypes() {
        CommonLookupValueListDetails commonLookupValues = getCommonValues(COMMON_VALUE_UNIQUE_IDENTIFIER_TYPE, null, null).block();
        List<CommonLookupValueDetails> uniqueIdentifierTypes = commonLookupValues.getContent().stream()
                .collect(Collectors.toList());

        return uniqueIdentifierTypes;
    }



    public List<CommonLookupValueDetails> getCategoriesOfLaw(List<String> codes) {
        CommonLookupValueListDetails allCategoriesOfLaw = getCommonValues(COMMON_VALUE_CATEGORY_OF_LAW, null, null).block();
        List<CommonLookupValueDetails> categoriesOfLaw = allCategoriesOfLaw.getContent().stream()
                    .filter(category -> codes.contains(category.getCode()))
                    .collect(Collectors.toList());

        return categoriesOfLaw;
    }

    public List<CommonLookupValueDetails> getAllCategoriesOfLaw() {
        CommonLookupValueListDetails allCategoriesOfLaw = getCommonValues(COMMON_VALUE_CATEGORY_OF_LAW, null, null).block();
        List<CommonLookupValueDetails> categoriesOfLaw = allCategoriesOfLaw.getContent().stream()
                .collect(Collectors.toList());

        return categoriesOfLaw;
    }

}

