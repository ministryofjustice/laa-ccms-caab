package uk.gov.laa.ccms.caab.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import uk.gov.laa.ccms.caab.bean.common.DynamicOptionFormData;
import uk.gov.laa.ccms.caab.constants.PriorAuthorityGroup;
import uk.gov.laa.ccms.caab.model.PriorAuthorityDetail;
import uk.gov.laa.ccms.caab.model.ReferenceDataItemDetail;
import uk.gov.laa.ccms.data.model.PriorAuthorityTypeDetail;

/** Utility class that provides helper functions for prior authorities. */
public class PriorAuthorityUtils {

  private PriorAuthorityUtils() {}

  public static Map<String, List<ReferenceDataItemDetail>> groupItems(
      PriorAuthorityDetail priorAuthorityDetail
  ) {
    if (priorAuthorityDetail == null || priorAuthorityDetail.getType() == null) {
      return new HashMap<>();
    }

    String typeId = priorAuthorityDetail.getType().getId();
    Map<String, List<ReferenceDataItemDetail>> grouped = createEmptyStringMap(typeId);

    for (ReferenceDataItemDetail item : priorAuthorityDetail.getItems()) {
      if (hasDisplayValue(item)) {
        PriorAuthorityGroup groupKey = getGroupForCode(item.getCode().getId(), typeId);
        grouped.getOrDefault(groupKey, grouped.get(PriorAuthorityGroup.OTHER.name()))
            .add(item);
      }
    }

    removeEmptyGroups(grouped);
    return grouped;
  }

  public static Map<String, List<DynamicOptionFormData>> initialiseGroupsForForm(String typeId) {
    Map<String, List<DynamicOptionFormData>> grouped = new HashMap<>();

    for (PriorAuthorityGroup group : PriorAuthorityGroup.getGroupsForType(typeId)) {
      grouped.put(group.name(), new ArrayList<>());
    }

    return grouped;
  }

  private static Map<String, List<ReferenceDataItemDetail>> createEmptyStringMap(String typeId) {
    Map<String, List<ReferenceDataItemDetail>> map = new HashMap<>();
    for (PriorAuthorityGroup group : PriorAuthorityGroup.getGroupsForType(typeId)) {
      map.put(group.name(), new ArrayList<>());
    }
    return map;
  }

  private static <T> void removeEmptyGroups(Map<String, List<T>> grouped) {
    grouped.entrySet().removeIf(entry -> entry.getValue().isEmpty());
  }

  private static boolean hasDisplayValue(ReferenceDataItemDetail item) {
    return item.getValue() != null && item.getValue().getDisplayValue() != null
        && !item.getValue().getDisplayValue().isBlank();
  }

  private static PriorAuthorityGroup getGroupForCode(String codeId, String priorAuthorityType) {

    if (codeId == null || priorAuthorityType == null) {
      return PriorAuthorityGroup.OTHER;
    }

    switch (priorAuthorityType) {
      case "COUNSEL":
        return switch (codeId) {
          case "C01_AUTHORTY_REQD", "C02_COUNSEL_BRIEF_TYPE" ->
              PriorAuthorityGroup.COUNSEL_DETAILS;
          default -> PriorAuthorityGroup.OTHER;
        };

      case "EXPERT":
        return switch (codeId) {
          case "E01_EXPERT_TYPE", "E02_EXPERT_NAME", "E03_COMPANY", "E04_ADDRESS", "E05_POST_CODE",
               "E06_EXPERT_REGION" -> PriorAuthorityGroup.EXPERT_DETAILS;
          case "E18_EXPERT_TOTAL_HOURS", "E19_PREPARATION_HOURS", "E20_EXPERT_COURT_HRS",
               "E21_TRAVEL_TIME", "E25_EXPERT_MILEAGE", "E26_EXPERT_MILEAGE_NUMBER",
               "E33_EXPERT_TOTAL_MIN", "E34_PREPARATION_MINS", "E35_EXPERT_COURT_MINS",
               "E36_TRAVEL_TIME_MINS" -> PriorAuthorityGroup.TIME_SPENT;
          case "E17_EXPERT_HOURLY_RATE", "E22_TRAVEL_HOURLY_RATE", "E28_EXPERT_VAT_RATE" ->
              PriorAuthorityGroup.RATES;
          case "E11_EXP_BEFORE_APP", "E23_TRAVEL_FARES", "E24_TRAVEL_FARES_DESCRIPTION",
               "E27_EXPERT_NET", "E29_EXPERT_VAT", "E30_EXPERT_TOTAL" ->
              PriorAuthorityGroup.COSTS;
          case "E07_JOINTLY_INSTRUCTED", "E08_REASON_NOT_JOINTLY_INSTR",
               "E09_CHILDREN_ACT_COURT_PERMI", "E10_REASON_LA_NOT_BEARING_COST",
               "E12_BASIS_OF_APPORTIONMENT", "E13_EXPERT_APPORTION",
               "E14_NUMBER_ALTERNATIVE_QUOTES", "E15_OTHER_QUOTES", "E16_REASON_FOR_SELECTED_QUOTE",
               "E31_EXPERT_COD", "E32_EXPERT_COD_EXPLAIN" -> PriorAuthorityGroup.REASONING;
          default -> PriorAuthorityGroup.OTHER;
        };
      case "OTHER":
        return switch (codeId) {
          case "O01_EXPENSE_TYPE", "O02_EXPENSE_DETAILS", "O03_EXPENSE_REGION" ->
              PriorAuthorityGroup.EXPENSE_DETAILS;
          default -> null;
        };
      default:
        return PriorAuthorityGroup.OTHER;
    }
  }

  public static Map<PriorAuthorityGroup, List<DynamicOptionFormData>> groupDynamicOptions(
      Map<String, DynamicOptionFormData> optionMap, String typeId
  ) {

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

    grouped.values().forEach(list -> list.sort(
        Comparator.comparing(DynamicOptionFormData::getCode)
    ));

    grouped.entrySet().removeIf(entry -> entry.getValue().isEmpty());

    return grouped;

  }
}
