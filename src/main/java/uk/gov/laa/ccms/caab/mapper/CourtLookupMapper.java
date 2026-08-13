package uk.gov.laa.ccms.caab.mapper;

import org.mapstruct.Mapper;

/**
 * Mapper class for converting between various court lookup entities and their corresponding DTO
 * representations.
 *
 * @see Mapper
 */
@Mapper(componentModel = "spring")
public interface CourtLookupMapper {

  CourtLookupDetail toCourtLookupDetail(Page<CourtLookupValueDetail> courtLookupValues);
}
