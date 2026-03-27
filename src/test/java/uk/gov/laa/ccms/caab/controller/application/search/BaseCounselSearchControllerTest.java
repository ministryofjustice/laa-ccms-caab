package uk.gov.laa.ccms.caab.controller.application.search;

import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.laa.ccms.caab.bean.validators.application.CounselSearchValidator;
import uk.gov.laa.ccms.caab.client.EbsApiClientErrorHandler;
import uk.gov.laa.ccms.caab.mapper.CounselLookupMapper;
import uk.gov.laa.ccms.caab.mapper.CounselLookupMapperImpl;
import uk.gov.laa.ccms.caab.service.CounselService;
import uk.gov.laa.ccms.data.model.CounselLookupDetail;
import uk.gov.laa.ccms.data.model.CounselLookupValueDetail;

@ExtendWith(MockitoExtension.class)
public class BaseCounselSearchControllerTest {

  @Mock protected CounselService service;

  protected CounselSearchValidator validator = new CounselSearchValidator();

  protected CounselLookupMapper mapper = new CounselLookupMapperImpl();

  protected EbsApiClientErrorHandler errorHandler = new EbsApiClientErrorHandler();

  protected MockMvc mockMvc;

  @BeforeEach
  public void setup() {
    mockMvc =
        standaloneSetup(new CounselSearchController(validator, service, mapper, errorHandler))
            .build();
  }

  protected CounselLookupDetail getCounselLookupDetail(List<CounselLookupValueDetail> values) {

    Page<CounselLookupValueDetail> counselLookupValueDetailsPage =
        new PageImpl<>(values, Pageable.ofSize(10).withPage(0), values.size());

    return mapper.toCounselLookupDetail(counselLookupValueDetailsPage);
  }

  protected List<CounselLookupValueDetail> getCounselLookupValueDetail() {
    return List.of(
        new CounselLookupValueDetail()
            .name("SHAUN S DODDS")
            .company("SHAUN S DODDS")
            .legalAidSupplierNumber("1099V")
            .category("Junior")
            .county(null));
  }
}
