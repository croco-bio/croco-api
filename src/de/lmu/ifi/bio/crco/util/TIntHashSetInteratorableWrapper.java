package de.lmu.ifi.bio.crco.util;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.set.hash.TIntHashSet;

import java.util.Iterator;



public class TIntHashSetInteratorableWrapper  implements Iterable<Integer>{
	private TIntHashSet set;
	public TIntHashSetInteratorableWrapper(TIntHashSet set){
		this.set = set;
	}
	
	@Override
	public Iterator<Integer> iterator() {
		return new TIntHashSetInterator(set.iterator());
	}
	class TIntHashSetInterator implements Iterator<Integer>{
		TIntIterator tIntIterator;
		public TIntHashSetInterator(TIntIterator tIntIterator){
			this.tIntIterator = tIntIterator;
		}
		
		@Override
		public boolean hasNext() {
			return tIntIterator.hasNext();
		}

		@Override
		public Integer next() {
			return tIntIterator.next();
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
			
		}
		
	}

}
