package de.lmu.ifi.bio.croco.util;

import java.util.HashMap;
import java.util.HashSet;

public class MapBuilder {
    public static<E,V> void update(HashMap<E,HashSet<V>> map, E key, V value)
    {
        if (! map.containsKey(key))
        {
            map.put(key, new HashSet<V>());
        }
        map.get(key).add(value);
    }
    
    public static<E> void increment(HashMap<E,Integer> map, E key, Integer delta)
    {
        if (! map.containsKey(key))
        {
            map.put(key,delta);
        }
        map.put(key,map.get(key)+delta);
    }
    

}
