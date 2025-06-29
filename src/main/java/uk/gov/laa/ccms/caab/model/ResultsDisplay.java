package uk.gov.laa.ccms.caab.model;

import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generic class for encapsulating paginated results display.
 *
 * @param <T> the type of elements in the results
 */
@Data
@NoArgsConstructor
public class ResultsDisplay<T> {

  /** List of elements in the current results display. */
  private List<T> content;

  /** Total number of pages available. */
  private Integer totalPages;

  /** Total number of elements across all pages. */
  private Integer totalElements;

  /** Current page number. */
  private Integer number;

  /** Number of elements per page. */
  private Integer size;

  public ResultsDisplay(List<T> content) {
    this.content = content;
  }
}
