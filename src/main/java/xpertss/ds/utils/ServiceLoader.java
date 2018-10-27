package xpertss.ds.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ServiceLoader<S> implements Iterable<S> {

   private static final String PREFIX = "META-INF/services/";

   // The class or interface representing the service being loaded
   private Class<S> service;

   // The class loader used to locate, load, and instantiate providers
   private ClassLoader loader;

   // Cached providers, in instantiation order
   private Set<S> providers;


   private ServiceLoader(Class<S> svc, ClassLoader cl)
   {
      service = svc;
      loader = cl;
      reload();
   }


   /**
    * Clear this loader's provider cache so that all providers will be reloaded.
    * 
    * <p>
    * After invoking this method, subsequent invocations of the {@link #iterator() iterator} method will lazily look up
    * and instantiate providers from scratch, just as is done by a newly-created loader.
    * 
    * <p>
    * This method is intended for use in situations in which new providers can be installed into a running Java virtual
    * machine.
    */
   public void reload()
   {
      Set<S> list = new LinkedHashSet<S>();
      String fullName = PREFIX + service.getName();
      try {
         Enumeration<URL> configs = loader.getResources(fullName);
         Set<Class<?>> classes = new HashSet<Class<?>>();
         while(configs.hasMoreElements()) {
            try {
               InputStream in = configs.nextElement().openStream();
               try {
                  for(String clsName : parse(in)) {
                     try {
                        Class<?> clazz = loader.loadClass(clsName);
                        if(!classes.contains(clazz) && service.isAssignableFrom(clazz)) {
                           list.add(service.cast(clazz.newInstance()));
                           classes.add(clazz);
                        }
                     } catch(Exception e) { }
                  }
               } finally {
                  if(in != null) in.close();
               }
            } catch(Exception iex) { }
         }
      } catch(Exception ex) { }
      providers = Collections.unmodifiableSet(list);
   }
   
   
   private List<String> parse(InputStream in)
      throws IOException
   {
      List<String> classes = new ArrayList<String>();
      BufferedReader br = new BufferedReader(new InputStreamReader(in, "utf-8"));
      String line;
      while((line = br.readLine()) != null) {
         if(line.contains("#")) {
            line = line.substring(0, line.indexOf("#")).trim();
         }
         if(line.length() > 0) classes.add(line);
      }
      return classes;
   }
   

   /**
    * Iterates the available providers of this loader's service.
    * <p>
    * The iterator returned by this method first yields all of the elements of the provider cache, in instantiation
    * order. It then lazily loads and instantiates any remaining providers, adding each one to the cache in turn.
    * <p>
    * <p>
    * The iterator returned by this method does not support removal. Invoking its {@link java.util.Iterator#remove()
    * remove} method will cause an {@link UnsupportedOperationException} to be thrown.
    * 
    * @return An iterator that lazily loads providers for this loader's service
    */
   public Iterator<S> iterator()
   {
      return providers.iterator();
   }

   /**
    * Returns a string describing this service.
    * 
    * @return A descriptive string
    */
   public String toString()
   {
      return "ServiceLoader[" + service.getName() + "]";
   }

   
   
   
   
   /**
    * Creates a new service loader for the given service type and class loader.
    * 
    * @param service
    *           The interface or abstract class representing the service
    * 
    * @param loader
    *           The class loader to be used to load provider-configuration files and provider classes, or <tt>null</tt>
    *           if the system class loader (or, failing that, the bootstrap class loader) is to be used
    * 
    * @return A new service loader
    */
   public static <S> ServiceLoader<S> load(Class<S> service, ClassLoader loader)
   {
      if(loader == null) loader = ClassLoader.getSystemClassLoader();
      return new ServiceLoader<S>(service, loader);
   }

   /**
    * Creates a new service loader for the given service type, using the current thread's
    * {@linkplain java.lang.Thread#getContextClassLoader context class loader}.
    * 
    * <p>
    * An invocation of this convenience method of the form
    * 
    * <blockquote>
    * 
    * <pre>
    * ServiceLoader.load(<i>service</i>)
    * </pre>
    * 
    * </blockquote>
    * 
    * is equivalent to
    * 
    * <blockquote>
    * 
    * <pre>
    * ServiceLoader.load(<i>service</i>,
    *                    Thread.currentThread().getContextClassLoader())
    * </pre>
    * 
    * </blockquote>
    * 
    * @param service
    *           The interface or abstract class representing the service
    * 
    * @return A new service loader
    */
   public static <S> ServiceLoader<S> load(Class<S> service)
   {
      ClassLoader cl = Thread.currentThread().getContextClassLoader();
      return ServiceLoader.load(service, cl);
   }



}
