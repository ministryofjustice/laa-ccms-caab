package uk.gov.laa.ccms.caab.builders;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.ui.Model;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;


/**
 * A builder used to populate controllers dropdown options, performing them asynchronously.
 */
public class DropdownBuilder {

  private final Model model;

  private final Map<String, Mono<CommonLookupDetail>> monoMap;

  public DropdownBuilder(final Model model) {
    this.model = model;
    this.monoMap = new LinkedHashMap<>();
  }

  public DropdownBuilder addDropdown(
      final String attributeName,
      final Mono<CommonLookupDetail> mono) {
    monoMap.put(attributeName, mono);
    return this;
  }

  /**
   * Build method to collect the added monos added to the map and perform them asynchronously.
   */
  public void build() {
    Flux.fromIterable(monoMap.entrySet())
        .flatMap(entry -> entry.getValue()
            .map(detail -> Map.entry(entry.getKey(), detail.getContent())))
        .collectMap(Map.Entry::getKey, Map.Entry::getValue)
        .doOnNext(model::addAllAttributes)
        .block();
  }
}
