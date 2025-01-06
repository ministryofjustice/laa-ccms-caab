package uk.gov.laa.ccms.caab.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.laa.ccms.caab.bean.NotificationSearchCriteria;

@DisplayName("Notification search util test")
class NotificationSearchUtilTest {

  String toMojStringDate(LocalDate localDate) {
    return localDate.format(java.time.format.DateTimeFormatter.ofPattern("d/M/yyyy"));
  }

  String toResultStringDate(LocalDate localDate) {
    return localDate.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
  }

  @Test
  @DisplayName("Should not touch dates if dates are already set")
  void shouldNotTouchDatesIfDatesAreAlreadySet() {
    // Given
    LocalDate fromDate = LocalDate.of(2020, 1, 1);
    LocalDate toDate = LocalDate.of(2022, 1, 1);
    String from = toMojStringDate(fromDate);
    String to = toMojStringDate(toDate);
    NotificationSearchCriteria input = new NotificationSearchCriteria();
    input.setNotificationFromDate(from);
    input.setNotificationToDate(to);
    // When
    NotificationSearchCriteria result =
        NotificationSearchUtil.prepareNotificationSearchCriteria(input);
    // Then
    assertEquals(toResultStringDate(fromDate), result.getNotificationFromDate());
    assertEquals(toResultStringDate(toDate), result.getNotificationToDate());
  }

  @Test
  @DisplayName("Should set from date if to date is set")
  void shouldSetFromDateIfToDateIsSet() {
    // Given
    LocalDate toDate = LocalDate.of(2022, 1, 1);
    String to = toMojStringDate(toDate);
    NotificationSearchCriteria input = new NotificationSearchCriteria();
    input.setNotificationToDate(to);
    // When
    NotificationSearchCriteria result =
        NotificationSearchUtil.prepareNotificationSearchCriteria(input);
    // Then
    assertEquals(toResultStringDate(LocalDate.of(2019, 1, 1)), result.getNotificationFromDate());
    assertEquals(toResultStringDate(toDate), result.getNotificationToDate());
  }


  @Test
  @DisplayName("Should set to date if from date is set")
  void shouldSetToDateIfFromDateIsSet() {
    // Given
    LocalDate fromDate = LocalDate.of(2018, 5, 1);
    String fromInput = toMojStringDate(fromDate);
    NotificationSearchCriteria input = new NotificationSearchCriteria();
    input.setNotificationFromDate(fromInput);
    // When
    NotificationSearchCriteria result =
        NotificationSearchUtil.prepareNotificationSearchCriteria(input);
    // Then
    assertEquals(toResultStringDate(fromDate), result.getNotificationFromDate());
    assertEquals(toResultStringDate(LocalDate.of(2021, 5, 1)), result.getNotificationToDate());
  }

  @Test
  @DisplayName("Should set to date to today if from date is set and within last 3 years")
  void shouldSetToDateToTodayIfFromDateIsSetAndWithinLast3Years() {
    // Given
    LocalDate fromDate = LocalDate.now().minusYears(2).minusMonths(5);
    String fromInput = toMojStringDate(fromDate);
    NotificationSearchCriteria input = new NotificationSearchCriteria();
    input.setNotificationFromDate(fromInput);
    // When
    NotificationSearchCriteria result =
        NotificationSearchUtil.prepareNotificationSearchCriteria(input);
    // Then
    assertEquals(toResultStringDate(fromDate), result.getNotificationFromDate());
    assertEquals(toResultStringDate(LocalDate.now()), result.getNotificationToDate());
  }

  @Test
  @DisplayName("Should preset to last 3 years if to and from not set")
  void shouldPresetToLast3YearsIfToAndFromNotSet() {
    // Given
    NotificationSearchCriteria input = new NotificationSearchCriteria();
    // When
    NotificationSearchCriteria result =
        NotificationSearchUtil.prepareNotificationSearchCriteria(input);
    // Then
    assertEquals(toResultStringDate(LocalDate.now()), result.getNotificationToDate());
    assertEquals(toResultStringDate(LocalDate.now().minusYears(3)),
        result.getNotificationFromDate());
  }

  @Test
  @DisplayName("Should format properly")
  void shouldFormatProperly() {
    // Given
    LocalDate fromDate = LocalDate.of(2020, 5, 1);
    LocalDate toDate = LocalDate.of(2022, 5, 1);
    NotificationSearchCriteria input = new NotificationSearchCriteria();
    input.setNotificationFromDate(toMojStringDate(fromDate));
    input.setNotificationToDate(toMojStringDate(toDate));
    // When
    NotificationSearchCriteria result =
        NotificationSearchUtil.prepareNotificationSearchCriteria(input);
    // Then
    assertEquals(toResultStringDate(fromDate), result.getNotificationFromDate());
    assertEquals(toResultStringDate(toDate), result.getNotificationToDate());
  }


}