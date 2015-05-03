package xpertss.ds.base;

import xpertss.ds.DataSource;

import javax.naming.Reference;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * A base implementation of the {@link xpertss.ds.DataSource} interface
 * which provides base property support.
 * 
 * @author cfloersch
 */
public abstract class BaseDataSource<T> implements DataSource<T> {
   
   private Map<String,String> props = Collections.synchronizedMap(new LinkedHashMap<String,String>());
   
   private Type type;
   
   protected BaseDataSource(Type type)
   {
      this.type = type;
   }
   
   /**
    * A data source may be either an origin type that simply creates
    * connections like a factory or a pool type that shares connections
    * from an underlying pool.
    * 
    * @return The type of this data source
    */
   public Type getType()
   {
      return type;
   }
   
   /**
    * MBean implementation which translates our enum into a String
    * which can be handled across the network.
    */
   public String getDataSourceType()
   {
      return type.toString();
   }
   
   
   /**
    * Get a named property from this managed data source.
    * 
    * @param key The key identifying the property to retrieve
    * @return The property value
    * @throws NullPointerException if the key is {@code null}
    * @throws IllegalArgumentException if the key is empty
    */
   public String getProperty(String key)
   {
      checkKey(key);
      return props.get(key);
   }


   /**
    * Most APIs use specific setters and getters to define the properties
    * the class define. This, however, uses an abstraction so that it may
    * define different supported properties based on the implementation.
    * <p>
    * For example an origin data source might require things like a url,
    * a username, a password, etc while a pooling implementation might
    * require properties like min and max pool size, etc.
    * 
    * @param key The property's key
    * @param value The property's value
    * @return the key's previously mapped value or {@code null} if the property
    *          was previously unset.
    * @throws NullPointerException if the key is {@code null}
    * @throws IllegalArgumentException if the key is empty
    */
   public String setProperty(String key, String value)
   {
      checkKey(key);
      return props.put(key, value);
   }
   
   /**
    * Unset a previously set property value.
    * 
    * @param key The property's key
    * @return the key's previously mapped value or {@code null} if the property
    *          was previously unset.
    * @throws NullPointerException if the key is {@code null}
    * @throws IllegalArgumentException if the key is empty
    */
   public String clearProperty(String key)
   {
      checkKey(key);
      return props.remove(key);
   }

   
   /**
    * Return the properties defined for this {@code DataSource} object as
    * a read-only set.
    * 
    * @return The property set
    */
   public Set<Map.Entry<String,String>> getPropertySet()
   {
      return Collections.unmodifiableSet(props.entrySet());
   }

   /**
    * Compare this data source with another data source for equality.
    * For two data sources to be equal they must both be of the same
    * type and contain the same set properties.
    * 
    * @param o the object to be compared for equality with this data source
    * @return {@code true} if the specified object is equal to this data source
    */
   public boolean equals(Object o)
   {
      if (o == this) return true;
      if (o.getClass() == getClass()) {
         BaseDataSource<?> bds = (BaseDataSource<?>)o;
         return props.entrySet().equals(bds.getPropertySet());
      }
      return false;
   }
   
   
   /**
    * Returns the hash code value for this data source.
    * <p>
    * This implementation computes the hash code as a function of the
    * properties specified and the class implementation itself.
    *
    * @return the hash code value for this data source
    */
   public int hashCode() 
   {
      return props.hashCode() ^ getClass().getName().hashCode();
   }





   protected Reference createReference(Class clazz, Class factory)
   {
      return new Reference(clazz.getName(), factory.getName(), null);
   }


   protected boolean getBoolean(String key, boolean def)
   {
      try {
         String str = getProperty(key);
         if("false".equalsIgnoreCase(str) || "true".equalsIgnoreCase(str)) {
            // we only evaluate it if it is a valid choice.
            return "true".equalsIgnoreCase(str);
         }
      } catch(Exception e) { }
      return def;
   }



   protected int getInt(String key, int def)
   {
      try {
         return Integer.parseInt(getProperty(key));
      } catch(Exception e) {
         return def;
      }
   }

   protected int getPositiveInt(String key, int def)
   {
      try {
         int value = Integer.parseInt(getProperty(key));
         return (value >= 0) ? value : def;
      } catch(Exception e) {
         return def;
      }
   }

   protected int getIndefiniteInt(String key)
   {
      try {
         int value = Integer.parseInt(getProperty(key));
         return (value > 0) ? value : Integer.MAX_VALUE;
      } catch(Exception e) {
         return Integer.MAX_VALUE;
      }
   }


   protected long getLong(String key, long def)
   {
      try {
         return Long.parseLong(getProperty(key));
      } catch(Exception e) {
         return def;
      }
   }

   protected long getPositiveLong(String key, long def)
   {
      try {
         long value = Long.parseLong(getProperty(key));
         return (value >= 0) ? value : def;
      } catch(Exception e) {
         return def;
      }
   }

   protected long getIndefiniteLong(String key)
   {
      try {
         long value = Long.parseLong(getProperty(key));
         return (value > 0) ? value : Long.MAX_VALUE;
      } catch(Exception e) {
         return Long.MAX_VALUE;
      }
   }


   
   private void checkKey(String key)
   {
      if(key == null) throw new NullPointerException("null key");
      if(key.trim().length() == 0) throw new IllegalArgumentException("empty key");
   }
}
