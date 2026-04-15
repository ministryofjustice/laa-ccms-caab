package uk.gov.laa.ccms.caab.model;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import lombok.Data;

/** Represents the category. */
@JsonTypeName("categoryDetail")
@Data
public class CategoryDetail {

  private List<CategoryDetail> content;

  private String code;

  private String description;

  public CategoryDetail code(String code) {
    this.code = code;
    return this;
  }

  public CategoryDetail description(String description) {
    this.description = description;
    return this;
  }
}
