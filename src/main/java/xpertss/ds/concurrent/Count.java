package xpertss.ds.concurrent;



public class Count {

   private int total;
   private int peek;
   private int current;
   
   public Count increment()
   {
      increment(new Condition() {
         public boolean evaluate(long current)
         {
            return true;
         }
      });
      return this;
   }
   
   public synchronized boolean increment(Condition condition)
   {
      if(condition.evaluate(current)) {
         total++;
         current++;
         peek = Math.max(peek, current);
         return true;
      }
      return false;
   }

   public Count decrement()
   {
      decrement(new Condition() {
         public boolean evaluate(long current)
         {
            return true;
         }
      });
      return this;
   }
   
   public synchronized boolean decrement(Condition condition)
   {
      if(condition.evaluate(current)) {
         current--;
         return true;
      }
      return false;
   }
   
   

   public synchronized int current()
   {
      return current;
   }
   
   public synchronized int currentMinus(int value)
   {
      return current - value;
   }

   public synchronized int currentPlus(int value)
   {
      return current + value;
   }

   
   
   public synchronized int peek()
   {
      return peek;
   }

   public synchronized int total()
   {
      return total;
   }
   
   
   public synchronized void reset()
   {
      total = peek = current = 0;
   }
}
