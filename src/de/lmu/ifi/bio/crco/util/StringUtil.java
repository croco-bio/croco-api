package de.lmu.ifi.bio.crco.util;

import java.util.Collection;
import java.util.Iterator;

/**
 * Various static methods to format strings
 * @author pesch
 *
 */
public class StringUtil {
	/**
	 * Returns a user specified string representation of a collection 
	 * @param data -- the collection
	 * @param seperator -- the separator
	 * @return the collection represented as string separated by separator.
	 */
	public static String getAsString(Collection<?> data, char seperator){
		Iterator<?> it = data.iterator();
		boolean first = true;
		StringBuffer ret = new StringBuffer();
		while(it.hasNext()){
			if ( first == false){
				ret.append(seperator);
			}else{
				first = false;
			}
			ret.append(it.next().toString());
		}
		return ret.toString();
	}
}
