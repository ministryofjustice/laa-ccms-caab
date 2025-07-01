package uk.gov.laa.ccms.caab.controller.client;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.ui.Model;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.bean.ClientFlowFormData;
import uk.gov.laa.ccms.caab.mapper.ClientDetailMapper;
import uk.gov.laa.ccms.caab.service.ClientService;
import uk.gov.laa.ccms.caab.service.LookupService;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;

/** Abstract controller for handling Client Summary Information. */
@RequiredArgsConstructor
public abstract class AbstractClientSummaryController {

  protected final LookupService lookupService;

  protected final ClientService clientService;

  protected final ClientDetailMapper clientDetailsMapper;

  protected Mono<Void> populateSummaryListLookups(
      final ClientFlowFormData clientFlowFormData, final Model model) {

    // Create a list of Mono calls and their respective attribute keys
    final List<Pair<String, Mono<Optional<CommonLookupValueDetail>>>> lookups =
        lookupService.getClientLookups(clientFlowFormData);

    // Fetch all Monos asynchronously
    return lookupService.addCommonLookupsToModel(lookups, model);
  }
}
