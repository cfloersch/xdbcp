package xpertss.ds.base;

import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import xpertss.ds.utils.ThreadUtils;


import junit.framework.TestCase;


public class BlockingPoolTest extends TestCase {

   






   private enum ErrorType {
      Timeout, IllegalState, Interrupted, None
   }

   public void testSize() throws Exception {
      BlockingPool<String> pool = new BlockingPool<String>();
      assertTrue("Failed to offer resource", pool.offer(new PooledResource<String>(null, 0, "message")));
      assertTrue("Failed to offer resource", pool.offer(new PooledResource<String>(null, 0, "message")));
      assertEquals("Expected two items in pool", 2, pool.size());
   }

   public void testWrap() throws Exception {
      BlockingPool<String> pool = new BlockingPool<String>();
      PooledResource<String> res = pool.wrap(null, "message");
      assertNotNull("Wrap should have returned resource", res);
      assertEquals("Pool should still be empty", 0, pool.size());
      assertTrue("Pool should have accepted wraped resource", pool.offer(res));
      assertEquals("Pool should contain recently added resource", 1, pool.size());
      assertEquals("Pool should have drained one resource", 1, pool.drain().size());
      assertEquals("Pool should now be empty", 0, pool.size());
      assertFalse("Pool should reject previously wrapped resource", pool.offer(res));
      assertEquals("Pool should still be empty", 0, pool.size());
      assertTrue("Pool should accept newly wrapped resource", pool.offer(pool.wrap(null, "message")));
      assertEquals("Pool should contain recently added resource", 1, pool.size());
   }

   public void testSimpleBlockingTimeout() throws Exception {
      BlockingPool<String> pool = new BlockingPool<String>();
      CountDownLatch latch = new CountDownLatch(2);
      SimpleThreadTest[] threads = new SimpleThreadTest[(int)latch.getCount()];
      for(int i = 0; i < threads.length; i++) {
         threads[i] = new SimpleThreadTest(pool, latch, 1);
         threads[i].start();
      }
      ThreadUtils.sleep(500);
      assertEquals("There should be two threads waiting for a resource", 2, pool.getWaitQueueSize());
      latch.await();
      assertEquals("There should not be any threads waiting for a resource", 0, pool.getWaitQueueSize());
      for(int i = 0; i < threads.length; i++) {
         assertTrue(threads[i].getErrorType() == ErrorType.Timeout);
      }
   }
   
   public void testSimpleBlockingInterrupt() throws Exception {
      BlockingPool<String> pool = new BlockingPool<String>();
      CountDownLatch latch = new CountDownLatch(2);
      SimpleThreadTest[] threads = new SimpleThreadTest[(int)latch.getCount()];
      for(int i = 0; i < threads.length; i++) {
         threads[i] = new SimpleThreadTest(pool, latch, 2);
         threads[i].start();
      }
      ThreadUtils.sleep(500);
      assertEquals("There should be two threads waiting for a resource", 2, pool.getWaitQueueSize());
      for(int i = 0; i < threads.length; i++) {
         threads[i].interrupt();
      }      
      latch.await();
      assertEquals("There should not be any threads waiting for a resource", 0, pool.getWaitQueueSize());
      for(int i = 0; i < threads.length; i++) {
         assertTrue(threads[i].getErrorType() == ErrorType.Interrupted);
      }
   }

   public void testSimpleBlockingShutdown() throws Exception {
      BlockingPool<String> pool = new BlockingPool<String>();
      CountDownLatch latch = new CountDownLatch(2);
      SimpleThreadTest[] threads = new SimpleThreadTest[(int)latch.getCount()];
      for(int i = 0; i < threads.length; i++) {
         threads[i] = new SimpleThreadTest(pool, latch, 10);
         threads[i].start();
      }
      ThreadUtils.sleep(500);
      assertEquals("There should be two threads waiting for a resource", 2, pool.getWaitQueueSize());
      pool.shutdown();
      latch.await();
      assertEquals("There should not be any threads waiting for a resource", 0, pool.getWaitQueueSize());
      for(int i = 0; i < threads.length; i++) {
         assertTrue(threads[i].getErrorType() == ErrorType.IllegalState);
      }
   }

   public void testSimpleBlockingSuccess() throws Exception {
      BlockingPool<String> pool = new BlockingPool<String>();
      CountDownLatch latch = new CountDownLatch(2);
      SimpleThreadTest[] threads = new SimpleThreadTest[(int)latch.getCount()];
      for(int i = 0; i < threads.length; i++) {
         threads[i] = new SimpleThreadTest(pool, latch, 10);
         threads[i].start();
      }
      ThreadUtils.sleep(500);
      assertEquals("There should be two threads waiting for a resource", 2, pool.getWaitQueueSize());
      assertTrue("Failed to offer resource", pool.offer(new PooledResource<String>(null, 0, "message")));
      
      ThreadUtils.sleep(500);
      assertEquals("There should not be any threads waiting for a resource", 1, pool.getWaitQueueSize());
      assertTrue("Failed to offer resource", pool.offer(new PooledResource<String>(null, 0, "message")));
      
      latch.await();
      assertEquals("There should not be any threads waiting for a resource", 0, pool.getWaitQueueSize());
      for(int i = 0; i < threads.length; i++) {
         assertTrue(threads[i].getErrorType() == ErrorType.None);
      }
   }

   public void testSimpleNonBlockingSuccess() throws Exception {
      BlockingPool<String> pool = new BlockingPool<String>();
      assertTrue("Failed to offer resource", pool.offer(new PooledResource<String>(null, 0, "message")));
      assertTrue("Failed to offer resource", pool.offer(new PooledResource<String>(null, 0, "message")));
      
      CountDownLatch latch = new CountDownLatch(2);
      SimpleThreadTest[] threads = new SimpleThreadTest[(int)latch.getCount()];
      for(int i = 0; i < threads.length; i++) {
         threads[i] = new SimpleThreadTest(pool, latch, 10);
         threads[i].start();
      }

      latch.await(5, TimeUnit.MILLISECONDS);
      assertEquals("There should not be any threads waiting for a resource", 0, pool.getWaitQueueSize());
      for(int i = 0; i < threads.length; i++) {
         assertTrue(threads[i].getErrorType() == ErrorType.None);
      }
   }

   public void testDrain() throws Exception {
      BlockingPool<String> pool = new BlockingPool<String>();
      assertTrue("Failed to offer resource", pool.offer(new PooledResource<String>(null, 0, "message")));
      assertTrue("Failed to offer resource", pool.offer(new PooledResource<String>(null, 0, "message")));
      assertEquals("Expected two items in pool", 2, pool.size());
      
      assertEquals("Expected two items drained", 2, pool.drain().size());
      assertFalse("Should not have accepted old object", pool.offer(new PooledResource<String>(null, 0, "message")));
      assertTrue("Failed to offer resource", pool.offer(new PooledResource<String>(null, 1, "message")));
      assertEquals("Expected one item in the pool", 1, pool.size());
   }

   public void testCopyContainsAndRemove() throws Exception {
      BlockingPool<String> pool = new BlockingPool<String>();
      assertTrue("Failed to offer resource", pool.offer(new PooledResource<String>(null, 0, "message")));
      assertTrue("Failed to offer resource", pool.offer(new PooledResource<String>(null, 0, "message")));
      assertEquals("Expected two items in pool", 2, pool.size());
      Collection<PooledResource<String>> copy = pool.copy();
      assertEquals("Expected two items in copy", 2, copy.size());
      for(PooledResource<String> res : copy) {
         assertTrue("Failed to find copied object in pool", pool.contains(res));
         assertTrue("Failed to remove copied object from pool", pool.remove(res));
      }
      assertEquals("Expected no items in the pool", 0, pool.size());
   }
   
   public void testPurge() throws Exception {
      BlockingPool<String> pool = new BlockingPool<String>();
      assertTrue("Failed to offer resource", pool.offer(new PooledResource<String>(null, 0, "message")));
      assertTrue("Failed to offer resource", pool.offer(new PooledResource<String>(null, 0, "message")));
      assertEquals("Expected two items in pool", 2, pool.size());
      
      ThreadUtils.sleep(1100);  // wait slightly longer than our 1 second idle time
      
      assertEquals("Expected two items purged", 2, pool.purge(1).size());
      assertTrue("Should still acept version 0 items", pool.offer(new PooledResource<String>(null, 0, "message")));
      assertEquals("Expected one item in the pool", 1, pool.size());
   }
   
   public void testRemainingCapacity() throws Exception {
      BlockingPool<String> pool = new BlockingPool<String>();
      pool.setMaxIdle(10);
      assertEquals("Remaining capacity did not return expected result", 10, pool.remainingCapacity());
      assertTrue("Failed to offer resource", pool.offer(new PooledResource<String>(null, 0, "message")));
      assertTrue("Failed to offer resource", pool.offer(new PooledResource<String>(null, 0, "message")));
      assertEquals("Expected two items in pool", 2, pool.size());
      assertEquals("Remaining capacity did not return expected result", 8, pool.remainingCapacity());
      assertTrue("Failed to offer resource", pool.offer(new PooledResource<String>(null, 0, "message")));
      assertTrue("Failed to offer resource", pool.offer(new PooledResource<String>(null, 0, "message")));
      assertEquals("Remaining capacity did not return expected result", 6, pool.remainingCapacity());
      assertTrue("Failed to offer resource", pool.offer(new PooledResource<String>(null, 0, "message")));
      assertTrue("Failed to offer resource", pool.offer(new PooledResource<String>(null, 0, "message")));
      assertEquals("Remaining capacity did not return expected result", 4, pool.remainingCapacity());
      assertTrue("Failed to offer resource", pool.offer(new PooledResource<String>(null, 0, "message")));
      assertTrue("Failed to offer resource", pool.offer(new PooledResource<String>(null, 0, "message")));
      assertEquals("Remaining capacity did not return expected result", 2, pool.remainingCapacity());
      assertTrue("Failed to offer resource", pool.offer(new PooledResource<String>(null, 0, "message")));
      assertTrue("Failed to offer resource", pool.offer(new PooledResource<String>(null, 0, "message")));
      assertEquals("Remaining capacity did not return expected result", 0, pool.remainingCapacity());
      assertFalse("Failed to reject resource offering", pool.offer(new PooledResource<String>(null, 0, "message")));
      assertNotNull("Pool returned null element", pool.poll());
      assertEquals("Remaining capacity did not return expected result", 1, pool.remainingCapacity());
   }
   
   public void testNonBlockingEmptyPool() throws Exception {
      BlockingPool<String> pool = new BlockingPool<String>();
      long start = System.nanoTime();
      assertNull("Pool should have been empty", pool.poll());
      long time = System.nanoTime() - start;
      assertEquals("Poll should not have blocked", 0, TimeUnit.MILLISECONDS.convert(time, TimeUnit.NANOSECONDS));
   }
   
   public void testMaxLifeTime() throws Exception {
      BlockingPool<String> pool = new BlockingPool<String>();
      pool.setMaxLife(1); // 1 second
      assertTrue("Failed to offer resource", pool.offer(new PooledResource<String>(null, 0, "message")));
      PooledResource<String> res = pool.poll();
      assertNotNull("Pool should have returned a resource", res);
      ThreadUtils.sleep(1100);
      assertFalse("Pool should have rejected resource", pool.offer(res));
   }
   
   public void testShutdown() throws Exception {
      BlockingPool<String> pool = new BlockingPool<String>();
      pool.shutdown();
      assertTrue("Pool should have indicated it was shutdown", pool.isShutdown());
      assertFalse("Pool should not accept resources when shutdown", pool.offer(new PooledResource<String>(null, 0, "message")));
      try {
         pool.wrap(null, "message");
         throw new Exception("Calling wrap should result in illegal state exception when closed");
      } catch(IllegalStateException e) { /* Test passes */ }
      try {
         pool.poll();
         throw new Exception("Calling poll should result in illegal state exception when closed");
      } catch(IllegalStateException e) { /* Test passes */ }
      try {
         pool.poll(5, TimeUnit.SECONDS);
         throw new Exception("Calling poll(TimeUnit) should result in illegal state exception when closed");
      } catch(IllegalStateException e) { /* Test passes */ }
   }

   
   public void testPoolOrdering() throws Exception {
      String messageOne = "message 1";
      String messageTwo = "message 2";
      
      BlockingPool<String> pool = new BlockingPool<String>();
      assertTrue("Message 1 was rejected", pool.offer(pool.wrap(null, messageOne)));
      
      ThreadUtils.sleep(50);  // Java system clock has resolution of 20ms
      assertTrue("Message 2 was rejected", pool.offer(pool.wrap(null, messageTwo)));
      
      PooledResource<String> res = pool.poll();
      assertNotNull("Pool failed to return item", res);
      assertEquals("Failed to return least recently used resource", messageOne, res.resource);   // don't reset last access time
      
      ThreadUtils.sleep(50);  // Java system clock has resolution of 20ms

      assertTrue("Reoffer of Message 1 was rejected", pool.offer(res));
      res = pool.poll();
      assertNotNull("Pool failed to return item", res);
      assertEquals("Should still give us message 1", messageOne, res.getResource());   // do reset last access time

      ThreadUtils.sleep(50);  // Java system clock has resolution of 20ms

      assertTrue("Reoffer of Message 1 was rejected", pool.offer(res));
      res = pool.poll();
      assertNotNull("Pool failed to return item", res);
      assertEquals("Should now give us message 2", messageTwo, res.getResource());   // do reset last access time
      
   }
   
   
   
   
   
   
   private class SimpleThreadTest extends Thread {
      
      private BlockingPool<String> pool;
      private CountDownLatch latch;
      private int seconds;
      
      private ErrorType type;
      
      public SimpleThreadTest(BlockingPool<String> pool, CountDownLatch latch, int seconds)
      {
         this.pool = pool;
         this.latch = latch;
         this.seconds = seconds;
      }
      
      public void run()
      {
         try {
            pool.poll(seconds, TimeUnit.SECONDS);
            type = ErrorType.None;
         } catch(TimeoutException e) {
            type = ErrorType.Timeout;
         } catch (IllegalStateException e) {
            type = ErrorType.IllegalState;
         } catch (InterruptedException e) {
            type = ErrorType.Interrupted;
         } finally {
            latch.countDown();
         }
      }
      
      public ErrorType getErrorType()
      {
         return type;
      }
      
   }
   
}
