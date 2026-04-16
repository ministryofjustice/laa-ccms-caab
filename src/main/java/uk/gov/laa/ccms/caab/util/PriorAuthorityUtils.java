package uk.gov.laa.ccms.caab.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import uk.gov.laa.ccms.caab.bean.common.DynamicOptionFormData;
import uk.gov.laa.ccms.caab.constants.PriorAuthorityGroup;
import uk.gov.laa.ccms.caab.model.PriorAuthorityDetail;
import uk.gov.laa.ccms.caab.model.ReferenceDataItemDetail;
import uk.gov.laa.ccms.caab.model.StringDisplayValue;

/** Utility class that provides helper functions for prior authorities. */
@SuppressWarnings("checkstyle:Indentation")
public class PriorAuthorityUtils {

  private PriorAuthorityUtils() {}

  /**
   * Groups the items in a prior authority for the review page.
   *
   * @param priorAuthorityDetail details of the prior authority being reviewed
   * @return returns a mapping of the groupings for each item.
   */
  public static Map<PriorAuthorityGroup, List<ReferenceDataItemDetail>> groupItems(
      PriorAuthorityDetail priorAuthorityDetail) {

    if (priorAuthorityDetail == null || priorAuthorityDetail.getType() == null) {
      return new LinkedHashMap<>();
    }

    String typeId = priorAuthorityDetail.getType().getId();

    Map<PriorAuthorityGroup, List<ReferenceDataItemDetail>> grouped = new LinkedHashMap<>();

    for (PriorAuthorityGroup group : PriorAuthorityGroup.getGroupsForType(typeId)) {
      grouped.put(group, new ArrayList<>());
    }

    for (ReferenceDataItemDetail item : priorAuthorityDetail.getItems()) {
      if (hasDisplayValue(item)) {

        PriorAuthorityGroup groupKey = getGroupForCode(item.getCode().getId(), typeId);

        if (groupKey == null || !grouped.containsKey(groupKey)) {
          groupKey = PriorAuthorityGroup.OTHER;
        }

        grouped.get(groupKey).add(item);
      }
    }

    Map<PriorAuthorityGroup, List<String>> config =
        TYPE_GROUPS.getOrDefault(typeId.toUpperCase(), new LinkedHashMap<>());

    grouped.forEach(
        (group, list) -> {
          List<String> order = config.getOrDefault(group, List.of());

          list.sort(
              Comparator.comparingInt(
                  item -> {
                    String code =
                        (item.getCode() != null && item.getCode().getId() != null)
                            ? item.getCode().getId()
                            : null;

                    if (code == null) {
                      return Integer.MAX_VALUE;
                    }

                    int index = order.indexOf(code);
                    return index >= 0 ? index : Integer.MAX_VALUE;
                  }));
        });

    grouped.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    return grouped;
  }

  private static boolean hasDisplayValue(ReferenceDataItemDetail item) {
    if (item == null || item.getValue() == null) {
      return false;
    }

    StringDisplayValue value = item.getValue();
    if (value.getDisplayValue() != null && !value.getDisplayValue().isBlank()) {
      return true;
    }

    if (value.getId() != null) {
      String idStr = value.getId().toString().trim();
      if (!idStr.isBlank() && !"null".equalsIgnoreCase(idStr)) {
        return true;
      }
    }

    return false;
  }

  private static PriorAuthorityGroup getGroupForCode(String codeId, String priorAuthorityType) {

    if (codeId == null || priorAuthorityType == null) {
      return PriorAuthorityGroup.OTHER;
    }

    Map<PriorAuthorityGroup, List<String>> config = TYPE_GROUPS.get(priorAuthorityType);

    if (config == null) {
      return PriorAuthorityGroup.OTHER;
    }

    return config.entrySet().stream()
        .filter(entry -> entry.getValue().contains(codeId))
        .map(Map.Entry::getKey)
        .findFirst()
        .orElse(PriorAuthorityGroup.OTHER);
  }

  /**
   * Groups the dynamic options for the prior authority form.
   *
   * @param optionMap map of dynamic options for a given type of prior authority
   * @param typeId the type of prior authority being created
   * @return returns a mapping of the groupings for each form item.
   */
  public static Map<PriorAuthorityGroup, List<DynamicOptionFormData>> groupDynamicOptions(
      Map<String, DynamicOptionFormData> optionMap, String typeId) {

    Map<PriorAuthorityGroup, List<DynamicOptionFormData>> grouped = new LinkedHashMap<>();

    for (PriorAuthorityGroup group : PriorAuthorityGroup.getGroupsForType(typeId)) {
      grouped.put(group, new ArrayList<>());
    }

    if (optionMap != null) {
      for (Map.Entry<String, DynamicOptionFormData> entry : optionMap.entrySet()) {
        String code = entry.getKey();
        DynamicOptionFormData option = entry.getValue();

        option.setCode(code);

        PriorAuthorityGroup groupKey = getGroupForCode(code, typeId);

        if (groupKey == null || !grouped.containsKey(groupKey)) {
          groupKey = PriorAuthorityGroup.OTHER;
        }

        grouped.get(groupKey).add(option);
      }
    }

    Map<PriorAuthorityGroup, List<String>> config =
        TYPE_GROUPS.getOrDefault(typeId.toUpperCase(), new LinkedHashMap<>());

    grouped.forEach(
        (group, list) -> {
          List<String> order = config.getOrDefault(group, List.of());

          list.sort(
              Comparator.comparingInt(
                  option -> {
                    int index = order.indexOf(option.getCode());
                    return index >= 0 ? index : Integer.MAX_VALUE;
                  }));
        });

    grouped.entrySet().removeIf(entry -> entry.getValue().isEmpty());

    return grouped;
  }

  private static final Map<String, LinkedHashMap<PriorAuthorityGroup, List<String>>> TYPE_GROUPS =
      Map.of(
          "COUNSEL",
              new LinkedHashMap<>() {
                {
                  put(
                      PriorAuthorityGroup.COUNSEL_DETAILS,
                      List.of("C01_AUTHORTY_REQD", "C02_COUNSEL_BRIEF_TYPE"));
                  put(PriorAuthorityGroup.OTHER, List.of());
                }
              },
          "EXPERT",
              new LinkedHashMap<>() {
                {
                  put(
                      PriorAuthorityGroup.EXPERT_DETAILS,
                      List.of(
                          "E01_EXPERT_TYPE",
                          "E02_EXPERT_NAME",
                          "E03_COMPANY",
                          "E04_ADDRESS",
                          "E05_POST_CODE",
                          "E06_EXPERT_REGION"));
                  put(
                      PriorAuthorityGroup.TIME_SPENT,
                      List.of(
                          "E18_EXPERT_TOTAL_HOURS",
                          "E33_EXPERT_TOTAL_MIN",
                          "E19_PREPARATION_HOURS",
                          "E34_PREPARATION_MINS",
                          "E20_EXPERT_COURT_HRS",
                          "E35_EXPERT_COURT_MINS",
                          "E21_TRAVEL_TIME",
                          "E36_TRAVEL_TIME_MINS",
                          "E26_EXPERT_MILEAGE_NUMBER"));
                  put(
                      PriorAuthorityGroup.RATES,
                      List.of(
                          "E17_EXPERT_HOURLY_RATE",
                          "E22_TRAVEL_HOURLY_RATE",
                          "E25_EXPERT_MILEAGE",
                          "E28_EXPERT_VAT_RATE"));
                  put(
                      PriorAuthorityGroup.COSTS,
                      List.of(
                          "E23_TRAVEL_FARES",
                          "E24_TRAVEL_FARES_DESCRIPTION",
                          "E11_EXP_BEFORE_APP",
                          "E30_EXPERT_TOTAL",
                          "E29_EXPERT_VAT",
                          "E27_EXPERT_NET"));
                  put(
                      PriorAuthorityGroup.REASONING,
                      List.of(
                          "E07_JOINTLY_INSTRUCTED",
                          "E08_REASON_NOT_JOINTLY_INSTR",
                          "E09_CHILDREN_ACT_COURT_PERMI",
                          "E10_REASON_LA_NOT_BEARING_COST",
                          "E12_BASIS_OF_APPORTIONMENT",
                          "E13_EXPERT_APPORTION",
                          "E14_NUMBER_ALTERNATIVE_QUOTES",
                          "E15_OTHER_QUOTES",
                          "E16_REASON_FOR_SELECTED_QUOTE",
                          "E31_EXPERT_COD",
                          "E32_EXPERT_COD_EXPLAIN"));
                  put(PriorAuthorityGroup.OTHER, List.of());
                }
              },
          "OTHER",
              new LinkedHashMap<>() {
                {
                  put(
                      PriorAuthorityGroup.EXPENSE_DETAILS,
                      List.of("O01_EXPENSE_TYPE", "O02_EXPENSE_DETAILS", "O03_EXPENSE_REGION"));
                  put(PriorAuthorityGroup.OTHER, List.of());
                }
              });
}
