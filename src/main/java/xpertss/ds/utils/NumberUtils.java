package xpertss.ds.utils;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * A series of utility functions for parsing string representations
 * of numbers into actual numbers.
 *  
 * @author cfloersch
 */
public class NumberUtils {

   /**
    * Parse the given string into a short if possible otherwise return
    * the specified default.
    */
   public static short getShort( String number, short def ) {
      try {
         return Short.parseShort(number);
      } catch ( Exception e ) {
         return def;
      }
   }
   
   
   /**
    * Parse the given string into an int if possible otherwise return
    * the specified default.
    */
   public static int getInt( String number, int def ) {
      try {
         return Integer.parseInt(number);
      } catch ( Exception e ) {
         return def;
      }
   }
   
   /**
    * Parse the given string into a long if possible otherwise return
    * the specified default.
    */
   public static long getLong( String number, long def ) {
      try {
         return Long.parseLong(number);
      } catch( Exception e ) {
         return def;
      }
   }

   /**
    * Parse the given string into a double if possible otherwise return
    * the specified default.
    */
   public static double getDouble( String number, double def ) {
      try {
         return Double.parseDouble(number);
      } catch ( Exception e ) {
         return def;
      }
   }

   /**
    * Parse the given string into a float if possible otherwise return
    * the specified default.
    */
   public static float getFloat( String number, float def ) {
      try {
         return Float.parseFloat(number);
      } catch( Exception e ) {
         return def;
      }
   }
   
   /**
    * Parse the given string into a big integer if possible otherwise return
    * the specified default.
    */
   public static BigInteger getIntegral( String number, BigInteger def ) {
      try {
         return new BigInteger(number);
      } catch( Exception e ) {
         return def;
      }
   }

   /**
    * Parse the given string into a big decimal if possible otherwise return
    * the specified default.
    */
   public static BigDecimal getDecimal( String number, BigDecimal def ) {
      try {
         return new BigDecimal(number);
      } catch( Exception e ) {
         return def;
      }
   }

}
