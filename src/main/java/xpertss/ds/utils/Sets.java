/**
 * Created By: cfloersch
 * Date: 6/21/13
 * Copyright 2013 XpertSoftware
 */
package xpertss.ds.utils;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class Sets {

   public static <T> Set<T> of(T ... items)
   {
      Set<T> result = new LinkedHashSet<T>(items.length);
      Collections.addAll(result, items);
      return Collections.unmodifiableSet(result);
   }

}
