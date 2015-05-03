package xpertss.ds.concurrent;


public class ConditionFactory {

   public static Condition equal(final long value)
   {
      return new Condition() {
         public boolean evaluate(long current)
         {
            return current == value;
         }
      };
   }

   public static Condition notEqual(final long value)
   {
      return new Condition() {
         public boolean evaluate(long current)
         {
            return current != value;
         }
      };
   }

   public static Condition lessThan(final long value)
   {
      return new Condition() {
         public boolean evaluate(long current)
         {
            return current < value;
         }
      };
   }
   
   public static Condition lessThanEqual(final long value)
   {
      return new Condition() {
         public boolean evaluate(long current)
         {
            return current <= value;
         }
      };
   }

   public static Condition greaterThan(final long value)
   {
      return new Condition() {
         public boolean evaluate(long current)
         {
            return current > value;
         }
      };
   }
   
   public static Condition greaterThanEqual(final long value)
   {
      return new Condition() {
         public boolean evaluate(long current)
         {
            return current >= value;
         }
      };
   }

}
