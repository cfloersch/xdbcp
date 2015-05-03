package xpertss.ds.concurrent;


public class Stats {

   private long min = Long.MAX_VALUE;
   private long max;
   private long total;
   private int count;

   
   public synchronized long getMinimum()
   {
      return min;
   }
   
   public synchronized long getAverage()
   {
      return (count == 0) ? 0 : total / count;
   }
   
   public synchronized long getMaximum()
   {
      return max;
   }
   
   public synchronized void record(long value)
   {
      min = Math.min(min, value);
      max = Math.max(max, value);
      total += value;
      count++;
   }
   
   public String toString()
   {
      StringBuilder buf = new StringBuilder();
      synchronized(this) {
         buf.append("min=").append(Long.toString(min));
         buf.append(", avg=").append((count > 0) ? Long.toString(total/count) : Long.toString(0));
         buf.append(", max=").append(Long.toString(max));
         buf.append(", total=").append(Long.toString(total));
         buf.append(", count=").append(Integer.toString(count));
      }
      return buf.toString();
   }
   
   public synchronized void reset()
   {
      total = min = max = 0;
      count = 0;
   }
}
