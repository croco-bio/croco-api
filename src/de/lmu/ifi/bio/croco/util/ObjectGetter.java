package de.lmu.ifi.bio.croco.util;

import java.util.HashSet;
import java.util.Set;

public class ObjectGetter {
    public static<E> Set<E> toSet(E ... elements)
    {
        HashSet<E> set = new HashSet<E>();
        for(E element : elements)
        {
            set.add(element);
        }
        
        return set;
    }
}
