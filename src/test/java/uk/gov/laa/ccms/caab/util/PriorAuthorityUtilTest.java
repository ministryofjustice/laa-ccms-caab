package uk.gov.laa.ccms.caab.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import uk.gov.laa.ccms.caab.constants.PriorAuthorityGroup;
import uk.gov.laa.ccms.caab.model.PriorAuthorityDetail;
import uk.gov.laa.ccms.caab.model.ReferenceDataItemDetail;
import uk.gov.laa.ccms.caab.model.StringDisplayValue;

public class PriorAuthorityUtilTest {

  @Test
  void returnsEmptyMapWhenPriorAuthorityDetailIsNull() {
    Map<PriorAuthorityGroup, List<ReferenceDataItemDetail>> result =
        PriorAuthorityUtils.groupItems(null);

    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  @Test
  void unknownTypePutsAllItemsInOtherGroup() {
    PriorAuthorityDetail detail =
        createPriorAuthority(
            "UNKNOWN_TYPE",
            createItem("ANY_CODE", "Value"),
            createItem("ANOTHER_CODE", "Another Value"));

    Map<PriorAuthorityGroup, List<ReferenceDataItemDetail>> result =
        PriorAuthorityUtils.groupItems(detail);

    assertEquals(1, result.size());
    assertTrue(result.containsKey(PriorAuthorityGroup.OTHER));

    List<ReferenceDataItemDetail> otherGroup = result.get(PriorAuthorityGroup.OTHER);
    assertEquals(2, otherGroup.size());
  }

  @Test
  void counselTypeGroupsCorrectly() {
    PriorAuthorityDetail detail =
        createPriorAuthority(
            "COUNSEL",
            createItem("C02_COUNSEL_BRIEF_TYPE", "TYPE"),
            createItem("C01_AUTHORTY_REQD", "Auth Reqd"));

    Map<PriorAuthorityGroup, List<ReferenceDataItemDetail>> result =
        PriorAuthorityUtils.groupItems(detail);

    assertEquals(1, result.size());
    assertTrue(result.containsKey(PriorAuthorityGroup.COUNSEL_DETAILS));

    List<ReferenceDataItemDetail> counselDetails = result.get(PriorAuthorityGroup.COUNSEL_DETAILS);
    assertEquals(2, counselDetails.size());
  }

  @Test
  void expertTypeGroupsCorrectly() {
    PriorAuthorityDetail detail =
        createPriorAuthority(
            "EXPERT", createItem("E01_EXPERT_TYPE", "TYPE"), createItem("E02_EXPERT_NAME", "Name"));

    Map<PriorAuthorityGroup, List<ReferenceDataItemDetail>> result =
        PriorAuthorityUtils.groupItems(detail);

    assertEquals(1, result.size());
    assertTrue(result.containsKey(PriorAuthorityGroup.EXPERT_DETAILS));

    List<ReferenceDataItemDetail> expertDetails = result.get(PriorAuthorityGroup.EXPERT_DETAILS);
    assertEquals(2, expertDetails.size());
  }

  @Test
  void otherTypeGroupsCorrectly() {
    PriorAuthorityDetail detail =
        createPriorAuthority(
            "OTHER",
            createItem("O01_EXPENSE_TYPE", "TYPE"),
            createItem("O02_EXPENSE_DETAILS", "Details"));

    Map<PriorAuthorityGroup, List<ReferenceDataItemDetail>> result =
        PriorAuthorityUtils.groupItems(detail);

    assertEquals(1, result.size());
    assertTrue(result.containsKey(PriorAuthorityGroup.EXPENSE_DETAILS));

    List<ReferenceDataItemDetail> expenseDetails = result.get(PriorAuthorityGroup.EXPENSE_DETAILS);
    assertEquals(2, expenseDetails.size());
  }

  private PriorAuthorityDetail createPriorAuthority(
      String typeId, ReferenceDataItemDetail... items) {
    PriorAuthorityDetail priorAuthorityDetail = new PriorAuthorityDetail();

    StringDisplayValue typeValue = new StringDisplayValue();
    typeValue.setId(typeId);
    priorAuthorityDetail.setType(typeValue);

    if (items != null && items.length > 0) {
      priorAuthorityDetail.setItems(List.of(items));
    }

    return priorAuthorityDetail;
  }

  private ReferenceDataItemDetail createItem(String codeId, String displayValue) {
    ReferenceDataItemDetail item = new ReferenceDataItemDetail();

    StringDisplayValue code = new StringDisplayValue().id(codeId);
    item.setCode(code);

    StringDisplayValue value = new StringDisplayValue().displayValue(displayValue);
    item.setValue(value);

    return item;
  }
}
