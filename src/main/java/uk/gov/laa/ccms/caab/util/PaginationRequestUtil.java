package uk.gov.laa.ccms.caab.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

/** Utility class for resolving pagination and sorting request parameters. */
public final class PaginationRequestUtil {

  public static final int DEFAULT_PAGE = 0;
  public static final int DEFAULT_SIZE = 10;

  public static final String PAGE_PARAM = "page";
  public static final String SIZE_PARAM = "size";
  public static final String SORT_PARAM = "pageSort";

  private PaginationRequestUtil() {}

  /**
   * Resolves pagination and sorting parameters from the current request.
   *
   * @param request The current {@link HttpServletRequest}.
   * @param page The requested page number.
   * @param size The requested page size.
   * @param pageSort The requested sorting parameter.
   * @param originalPage The original page number before resolution.
   * @param originalSize The original page size before resolution.
   * @param originalSort The original sorting parameter before resolution.
   * @param defaultSort The default sorting to use if no other sort is found.
   * @return A {@link PaginationRequest} containing resolved pagination parameters.
   */
  public static PaginationRequest resolve(
      HttpServletRequest request,
      Integer page,
      Integer size,
      String pageSort,
      Integer originalPage,
      Integer originalSize,
      String originalSort,
      String defaultSort) {
    return resolve(
        request,
        page,
        size,
        pageSort,
        originalPage,
        originalSize,
        originalSort,
        DEFAULT_PAGE,
        DEFAULT_SIZE,
        defaultSort);
  }

  /**
   * Resolves pagination and sorting parameters from the current request with custom default bases.
   *
   * @param request The current {@link HttpServletRequest}.
   * @param page The requested page number.
   * @param size The requested page size.
   * @param pageSort The requested sorting parameter.
   * @param originalPage The original page number before resolution.
   * @param originalSize The original page size before resolution.
   * @param originalSort The original sorting parameter before resolution.
   * @param defaultPageBase The base default page number.
   * @param defaultSizeBase The base default page size.
   * @param defaultSort The default sorting to use if no other sort is found.
   * @return A {@link PaginationRequest} containing resolved pagination parameters.
   */
  public static PaginationRequest resolve(
      HttpServletRequest request,
      Integer page,
      Integer size,
      String pageSort,
      Integer originalPage,
      Integer originalSize,
      String originalSort,
      int defaultPageBase,
      int defaultSizeBase,
      String defaultSort) {
    boolean hasPageParam = hasParam(request, PAGE_PARAM);
    boolean hasSizeParam = hasParam(request, SIZE_PARAM);
    boolean hasSortParam = hasParam(request, SORT_PARAM);
    boolean isNewPageRequest = hasPageParam || hasSizeParam || hasSortParam;

    int defaultPage = originalPage != null ? originalPage : defaultPageBase;
    int defaultSize = originalSize != null ? originalSize : defaultSizeBase;
    int finalPage = page != null ? page : defaultPage;
    int finalSize = size != null ? size : defaultSize;

    String finalSort = resolveSort(pageSort, hasSortParam, originalSort, defaultSort);

    boolean isNewSort = !finalSort.equals(originalSort);
    boolean isNewPage = originalPage == null || finalPage != originalPage.intValue();
    boolean isNewSize = originalSize == null || finalSize != originalSize.intValue();

    if (isNewSort) {
      finalPage = defaultPageBase;
      isNewPage = originalPage == null || finalPage != originalPage.intValue();
    }

    return new PaginationRequest(
        finalPage, finalSize, finalSort, isNewPageRequest, isNewSort, isNewPage, isNewSize);
  }

  private static boolean hasParam(HttpServletRequest request, String name) {
    return request != null && request.getParameterMap().containsKey(name);
  }

  private static String resolveSort(
      String pageSort, boolean hasSortParam, String originalSort, String defaultSort) {
    if (StringUtils.hasText(pageSort) && pageSort.contains(",")) {
      return pageSort;
    }
    if (hasSortParam) {
      return defaultSort;
    }
    if (StringUtils.hasText(originalSort) && originalSort.contains(",")) {
      return originalSort;
    }
    return defaultSort;
  }
}
