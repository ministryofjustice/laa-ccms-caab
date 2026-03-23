package uk.gov.laa.ccms.caab.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import uk.gov.laa.ccms.caab.model.PriorAuthorityDetail;
import uk.gov.laa.ccms.caab.model.ReferenceDataItemDetail;

/** Utility class that provides helper functions for prior authorities. */
@Service
public class PriorAuthorityUtils {

  /**
   * Groups the items in a prior authority.
   *
   * @param priorAuthorityDetail details of the prior authority being reviewed
   * @return returns a mapping of the groupings for each item.
   */
  public Map<String, List<ReferenceDataItemDetail>> groupPriorAuthorityItems(
      PriorAuthorityDetail priorAuthorityDetail) {
    Map<String, List<ReferenceDataItemDetail>> groupedItems = new HashMap<>();

    switch (priorAuthorityDetail.getType().getId()) {
      case "COUNSEL":
        groupedItems.put("COUNSEL_DETAILS", new ArrayList<>());
        break;

      case "EXPERT":
        groupedItems.put("EXPERT_DETAILS", new ArrayList<>());
        groupedItems.put("TIME_SPENT", new ArrayList<>());
        groupedItems.put("RATES", new ArrayList<>());
        groupedItems.put("COSTS", new ArrayList<>());
        groupedItems.put("REASONING", new ArrayList<>());
        break;

      case "OTHER":
        groupedItems.put("EXPENSE_DETAILS", new ArrayList<>());
        break;

      default:
        throw new IllegalStateException(
            "Unhandled prior authority type: " + priorAuthorityDetail.getType().getId());
    }

    for (ReferenceDataItemDetail item : priorAuthorityDetail.getItems()) {
      String group =
          getGroupForCode(item.getCode().getId(), priorAuthorityDetail.getType().getId());
      if (groupedItems.containsKey(group)) {
        groupedItems.get(group).add(item);
      }
    }

    return groupedItems;
  }

  private String getGroupForCode(String codeId, String priorAuthorityType) {

    switch (priorAuthorityType) {
      case "COUNSEL":
        switch (codeId) {
          case "C01_AUTHORTY_REQD":
          case "C02_COUNSEL_BRIEF_TYPE":
            return "COUNSEL_DETAILS";
          default:
            return null;
        }

      case "EXPERT":
        switch (codeId) {
          case "E01_EXPERT_TYPE":
          case "E02_EXPERT_NAME":
          case "E03_COMPANY":
          case "E04_ADDRESS":
          case "E05_POST_CODE":
          case "E06_EXPERT_REGION":
            return "EXPERT_DETAILS";

          case "E18_EXPERT_TOTAL_HOURS":
          case "E19_PREPARATION_HOURS":
          case "E20_EXPERT_COURT_HRS":
          case "E21_TRAVEL_TIME":
          case "E25_EXPERT_MILEAGE":
          case "E26_EXPERT_MILEAGE_NUMBER":
          case "E33_EXPERT_TOTAL_MIN":
          case "E34_PREPARATION_MINS":
          case "E35_EXPERT_COURT_MINS":
          case "E36_TRAVEL_TIME_MINS":
            return "TIME_SPENT";

          case "E17_EXPERT_HOURLY_RATE":
          case "E22_TRAVEL_HOURLY_RATE":
          case "E28_EXPERT_VAT_RATE":
            return "RATES";

          case "E11_EXP_BEFORE_APP":
          case "E23_TRAVEL_FARES":
          case "E24_TRAVEL_FARES_DESCRIPTION":
          case "E27_EXPERT_NET":
          case "E29_EXPERT_VAT":
          case "E30_EXPERT_TOTAL":
            return "COSTS";

          case "E07_JOINTLY_INSTRUCTED":
          case "E08_REASON_NOT_JOINTLY_INSTR":
          case "E09_CHILDREN_ACT_COURT_PERMI":
          case "E10_REASON_LA_NOT_BEARING_COST":
          case "E12_BASIS_OF_APPORTIONMENT":
          case "E13_EXPERT_APPORTION":
          case "E14_NUMBER_ALTERNATIVE_QUOTES":
          case "E15_OTHER_QUOTES":
          case "E16_REASON_FOR_SELECTED_QUOTE":
          case "E31_EXPERT_COD":
          case "E32_EXPERT_COD_EXPLAIN":
            return "REASONING";
          default:
            return null;
        }

      case "OTHER":
        switch (codeId) {
          case "O01_EXPENSE_TYPE":
          case "O02_EXPENSE_DETAILS":
          case "O03_EXPENSE_REGION":
            return "EXPENSE_DETAILS";
          default:
            return null;
        }
      default:
        return null;
    }
  }
}
