package uk.gov.laa.ccms.caab.util;

/**
 * Immutable pagination state resolved from request parameters and stored criteria.
 *
 * @param page The resolved page number.
 * @param size The resolved page size.
 * @param sort The resolved sort field and direction.
 * @param isNewPageRequest Whether the request included any pagination parameters.
 * @param isNewSort Whether the resolved sort differs from the original sort.
 * @param isNewPage Whether the resolved page differs from the original page.
 * @param isNewSize Whether the resolved size differs from the original size.
 */
public record PaginationRequest(
    int page,
    int size,
    String sort,
    boolean isNewPageRequest,
    boolean isNewSort,
    boolean isNewPage,
    boolean isNewSize) {}
