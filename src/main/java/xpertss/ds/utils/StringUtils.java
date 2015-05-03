package xpertss.ds.utils;

/**
 * Series of utility functions for working with strings.
 * 
 * @author cfloersch
 */
public class StringUtils {

   /**
    * A string is empty if it is either null or made up of nothing
    * but spaces.
    */
   public static boolean isEmpty(String str)
   {
      return (str == null || str.trim().equals(""));
   }
   
   /**
    * Compares to string for equality ensuring that null values do
    * not result in a null pointer exception. Two strings are equal
    * if they are both null or if they are both not null and the
    * equals method returns true.
    */
   public static boolean isEqual(String str, String comp)
   {
      return (str == comp || (str != null && str.equals(comp)));
   }
   
   
   public static String toUpper(String str)
   {
      return (str == null) ? null : str.toUpperCase();
   }
   
   public static String toLower(String str)
   {
      return (str == null) ? null : str.toLowerCase();
   }
   
   
}
