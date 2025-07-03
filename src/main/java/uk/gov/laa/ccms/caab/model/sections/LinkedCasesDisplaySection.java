package uk.gov.laa.ccms.caab.model.sections;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/** List of linked cases to display. */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class LinkedCasesDisplaySection {

  List<LinkedCaseDisplay> linkedCases;
}
