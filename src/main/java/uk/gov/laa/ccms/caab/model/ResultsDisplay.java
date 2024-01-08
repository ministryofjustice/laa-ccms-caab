package uk.gov.laa.ccms.caab.model;

import java.util.List;
import lombok.Data;

/**
 * Generic class for encapsulating paginated results display.
 *
 * @param <T> the type of elements in the results
 */
@Data
public class ResultsDisplay<T> {

  private List<T> content;
  private Integer totalPages;
  private Integer totalElements;
  private Integer number;
  private Integer size;

}
