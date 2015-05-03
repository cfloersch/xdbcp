package xpertss.ds.concurrent;

/**
 * @author cfloersch
 */
public class Ratio {

   private int total;
   private int hits;
   
   public synchronized void record(boolean hit)
   {
      total++;
      if(hit) hits++;
   }
   
   public synchronized int hits()
   {
      return hits;
   }
   
   public synchronized int total()
   {
      return total;
   }
   
   public synchronized int ratio()
   {
      return (total == 0) ? 0 : hits * 100 / total;
   }
   
   public synchronized void reset()
   {
      total = hits = 0;
   }

}
