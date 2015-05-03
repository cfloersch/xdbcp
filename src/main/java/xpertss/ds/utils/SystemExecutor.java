package xpertss.ds.utils;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * SystemExecutor is a system wide scheduled executor service which
 * may be used for various tasks. It would have been nice if Sun had 
 * included such a shared resource that all java programs could use.
 * <p>
 * The system executor defaults to a pool of 10 threads to perform
 * operations. However, that can be adjusted using the system property
 * <code>system.executor.thread.count</code>.
 * <p>
 * All threads used by the system executor are daemon threads and as
 * such will not prevent the normal shutdown of the JVM.
 *  
 * @author cfloersch
 */
public class SystemExecutor {

   
   private static ScheduledExecutorService executor = 
            Executors.newScheduledThreadPool(
                     NumberUtils.getInt(System.getProperty("system.executor.thread.count"), 10), 
                     new DaemonThreadFactory("system"));
   
   
   /**
    * Executes the given command at some time in the future.  The command
    * may execute in a new thread, in a pooled thread, or in the calling
    * thread, at the discretion of the <tt>Executor</tt> implementation.
    *
    * @param command the runnable task
    * @throws RejectedExecutionException if this task cannot be
    * accepted for execution.
    * @throws NullPointerException if command is null
    */
   public static void execute(Runnable command)
   {
      executor.execute(command);
   }
   
   
   
   /**
    * Submits a value-returning task for execution and returns a Future
    * representing the pending results of the task. 
    * <p>
    * If you would like to immediately block waiting
    * for a task, you can use constructions of the form
    * <tt>result = exec.submit(aCallable).get();</tt>
    *
    * <p> Note: The {@link Executors} class includes a set of methods
    * that can convert some other common closure-like objects,
    * for example, {@link java.security.PrivilegedAction} to
    * {@link Callable} form so they can be submitted.
    *
    * @param task the task to submit
    * @return a Future representing pending completion of the task
    * @throws RejectedExecutionException if task cannot be scheduled
    * for execution
    * @throws NullPointerException if task null
    */
   public static <T> Future<T> submit(Callable<T> task)
   {
      return executor.submit(task);
   }
   
   
   /**
    * Submits a Runnable task for execution and returns a Future 
    * representing that task.
    *
    * @param task the task to submit
    * @return a Future representing pending completion of the task,
    * and whose <tt>get()</tt> method will return <tt>null</tt>
    * upon completion.
    * @throws RejectedExecutionException if task cannot be scheduled
    * for execution
    * @throws NullPointerException if task null
    */
   public static Future<?> submit(Runnable task)
   {
      return executor.submit(task);
   }
   
   
   
   /**
    * Creates and executes a ScheduledFuture that becomes enabled after the
    * given delay.
    * @param callable the function to execute.
    * @param delay the time from now to delay execution.
    * @param unit the time unit of the delay parameter.
    * @return a ScheduledFuture that can be used to extract result or cancel.
    * @throws RejectedExecutionException if task cannot be scheduled
    * for execution.
    * @throws NullPointerException if callable is null
    */
   public static <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit)
   {
      return executor.schedule(callable, delay, unit);
   }
   
   
   
   /**
    * Creates and executes a one-shot action that becomes enabled
    * after the given delay.
    * @param command the task to execute.
    * @param delay the time from now to delay execution.
    * @param unit the time unit of the delay parameter.
    * @return a Future representing pending completion of the task,
    * and whose <tt>get()</tt> method will return <tt>null</tt>
    * upon completion.
    * @throws RejectedExecutionException if task cannot be scheduled
    * for execution.
    * @throws NullPointerException if command is null
    */
   public static ScheduledFuture<?> schedule(Runnable command, long delay,  TimeUnit unit)
   {
      return executor.schedule(command, delay, unit);
   }

   
   
   /**
    * Creates and executes a periodic action that becomes enabled first
    * after the given initial delay, and subsequently with the given
    * period; that is executions will commence after
    * <tt>initialDelay</tt> then <tt>initialDelay+period</tt>, then
    * <tt>initialDelay + 2 * period</tt>, and so on.  
    * If any execution of the task
    * encounters an exception, subsequent executions are suppressed.
    * Otherwise, the task will only terminate via cancellation or
    * termination of the executor.
    * @param command the task to execute.
    * @param initialDelay the time to delay first execution.
    * @param period the period between successive executions.
    * @param unit the time unit of the initialDelay and period parameters
    * @return a Future representing pending completion of the task,
    * and whose <tt>get()</tt> method will throw an exception upon
    * cancellation.
    * @throws RejectedExecutionException if task cannot be scheduled
    * for execution.
    * @throws NullPointerException if command is null
    * @throws IllegalArgumentException if period less than or equal to zero.
    */
   public static ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit)
   {
      return executor.scheduleAtFixedRate(command, initialDelay, period, unit);
   }
   
   /**
    * Creates and executes a periodic action that becomes enabled first
    * after the given initial delay, and subsequently with the
    * given delay between the termination of one execution and the
    * commencement of the next. If any execution of the task
    * encounters an exception, subsequent executions are suppressed.
    * Otherwise, the task will only terminate via cancellation or
    * termination of the executor.
    * @param command the task to execute.
    * @param initialDelay the time to delay first execution.
    * @param delay the delay between the termination of one
    * execution and the commencement of the next.
    * @param unit the time unit of the initialDelay and delay parameters
    * @return a Future representing pending completion of the task,
    * and whose <tt>get()</tt> method will throw an exception upon
    * cancellation.
    * @throws RejectedExecutionException if task cannot be scheduled
    * for execution.
    * @throws NullPointerException if command is null
    * @throws IllegalArgumentException if delay less than or equal to zero.
    */
   public static ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit)
   {
      return executor.scheduleWithFixedDelay(command, initialDelay, delay, unit);
   }
   
   
}
