package uk.gov.laa.ccms.caab.util;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;

/**
 * Utility class that provides helper functions for pagination.
 *
 * <p>This class offers a set of methods to help in the pagination of data, especially when working
 * with lists and {@link Page} objects.
 */
public final class PaginationUtil {

  private PaginationUtil() {}

  /**
   * Paginates a given list based on the provided {@link Pageable} object.
   *
   * <p>This method takes a list and a {@link Pageable} object to return a paginated subset of the
   * list wrapped in a {@link Page} object.
   *
   * @param <T> The type of the elements within the list.
   * @param pageable The {@link Pageable} object containing pagination instructions.
   * @param list The list of items to be paginated.
   * @return A {@link Page} object containing a paginated and possibly sorted subset of the list.
   */
  public static <T> Page<T> paginateList(final Pageable pageable, List<T> list) {
    int start = (int) pageable.getOffset();
    int end = Math.min(start + pageable.getPageSize(), list.size());

    /*
     if there's no sort in the request, just return the default page
    */
    if (!pageable.getSort().isSorted() || list.isEmpty()) {
      return new PageImpl<>(list.subList(start, end), pageable, list.size());
    }

    List<Sort.Order> nestedOrders = new ArrayList<>();
    List<Sort.Order> topLevelOrders = new ArrayList<>();

    for (Sort.Order order : pageable.getSort()) {
      String property = order.getProperty();
      boolean isNested = property.contains(".");

      if (isNested) {
        nestedOrders.add(order);
      } else {
        topLevelOrders.add(order);
      }
    }

    if (!topLevelOrders.isEmpty()) {
      // Sort the list based on top-level properties
      list.sort(comparatorForTopLevelSort(list.getFirst().getClass(), topLevelOrders));
    }

    if (!nestedOrders.isEmpty()) {
      // Sort the list based on nested properties
      list.sort(comparatorForNestedSort(nestedOrders));
    }

    List<T> sublist = new ArrayList<>(list.subList(start, end));
    return new PageImpl<>(sublist, pageable, list.size());
  }

  private static <T> Comparator<T> comparatorForTopLevelSort(
      Class<?> clazz, List<Sort.Order> topLevelOrders) {
    return (o1, o2) -> {
      for (Sort.Order order : topLevelOrders) {
        String property = order.getProperty();
        String methodName = "get" + property.substring(0, 1).toUpperCase() + property.substring(1);

        try {
          Method method = clazz.getDeclaredMethod(methodName);
          Object val1 = method.invoke(o1);
          Object val2 = method.invoke(o2);

          if (val1 == null && val2 == null) {
            continue;
          } else if (val1 == null) {
            return order.isAscending() ? -1 : 1;
          } else if (val2 == null) {
            return order.isAscending() ? 1 : -1;
          }

          if (val1 instanceof Comparable && val2 instanceof Comparable) {
            @SuppressWarnings("unchecked")
            Comparable<Object> comparableVal1 = (Comparable<Object>) val1;
            return order.isAscending()
                ? comparableVal1.compareTo(val2)
                : comparableVal1.compareTo(val2) * -1;
          }
        } catch (Exception e) {
          throw new CaabApplicationException("Error sorting comparator for top-level properties");
        }
      }
      return 0;
    };
  }

  private static <T> Comparator<T> comparatorForNestedSort(List<Sort.Order> nestedOrders) {
    return (o1, o2) -> {
      for (Sort.Order order : nestedOrders) {
        String nestedProperty = order.getProperty();
        String[] nestedPropertyParts = nestedProperty.split("\\.");
        Object val1 = o1;
        Object val2 = o2;

        for (String propertyPart : nestedPropertyParts) {
          try {
            String methodName =
                "get" + propertyPart.substring(0, 1).toUpperCase() + propertyPart.substring(1);
            Method method = val1.getClass().getDeclaredMethod(methodName);
            val1 = method.invoke(val1);
            val2 = method.invoke(val2);

            if (val1 == null) {
              return order.isAscending() ? -1 : 1;
            } else if (val2 == null) {
              return order.isAscending() ? 1 : -1;
            }
          } catch (Exception e) {
            throw new CaabApplicationException("Error sorting comparator for nested properties");
          }
        }

        if (val1 instanceof Comparable && val2 instanceof Comparable) {
          @SuppressWarnings("unchecked")
          Comparable<Object> comparableVal1 = (Comparable<Object>) val1;
          return order.isAscending()
              ? comparableVal1.compareTo(val2)
              : comparableVal1.compareTo(val2) * -1;
        }
      }
      return 0;
    };
  }
}
