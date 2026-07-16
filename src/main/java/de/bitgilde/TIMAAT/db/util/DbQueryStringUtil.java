package de.bitgilde.TIMAAT.db.util;

import java.util.Collections;
import java.util.List;

/**
 * This class contains some utility methods helping to create query string
 *
 * @author Nico Kotlenga (nico@nko-dev.studio) 
 * @since 16.07.26
 */
public class DbQueryStringUtil {

  private DbQueryStringUtil() {
  }

  public static String createInPlaceHolderValue(int numberOfItems) {
    List<String> placeholders = Collections.nCopies(numberOfItems, "?");
    return "(" + String.join(",", placeholders) + ")";
  }
}
