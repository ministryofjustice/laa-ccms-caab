package uk.gov.laa.ccms.caab.builders;

import java.util.LinkedHashMap;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
import org.springframework.ui.Model;
import java.util.Map;
import uk.gov.laa.ccms.caab.service.CommonLookupService;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;

public class DropdownBuilder {

  private final Model model;

  private final Map<String, Mono<CommonLookupDetail>> monoMap;

  public DropdownBuilder(Model model) {
    this.model = model;
    this.monoMap = new LinkedHashMap<>();
  }

  public DropdownBuilder addDropdown(String attributeName, Mono<CommonLookupDetail> mono) {
    monoMap.put(attributeName, mono);
    return this;
  }

  public void build() {
    Flux.fromIterable(monoMap.entrySet())
        .flatMap(entry -> entry.getValue()
            .map(detail -> Map.entry(entry.getKey(), detail.getContent())))
        .collectMap(Map.Entry::getKey, Map.Entry::getValue)
        .doOnNext(model::addAllAttributes)
        .block();
  }
}

