package uk.gov.laa.ccms.caab.mapper;

import org.mapstruct.Mapper;
import org.springframework.data.domain.Page;
import uk.gov.laa.ccms.data.model.CounselLookupDetail;
import uk.gov.laa.ccms.data.model.CounselLookupValueDetail;

/**
 * Mapper interface for converting between various lookup entities and their corresponding DTO
 * representations.
 *
 * @see Mapper
 * @see CounselLookupDetail
 * @see CounselLookupValueDetail
 */
@Mapper(componentModel = "spring")
public interface CounselLookupMapper {

  CounselLookupDetail toCounselLookupDetail(Page<CounselLookupValueDetail> counselLookupValues);
}
