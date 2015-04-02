package de.lmu.ifi.bio.croco.util;


public class OrderedPair<FIRST,SECOND> implements Tuple<FIRST,SECOND> {
	
	
	private static final long serialVersionUID = 1L;
	
	private FIRST first;
	private SECOND second;
	
	public OrderedPair(FIRST first, SECOND second){
		this.first = first;
		this.second  = second;
	}
	
	public FIRST getFirst(){
		return first;
	}
	
	public SECOND getSecond(){
		return second;
	}
	
	public void setFirst(FIRST first) {
		this.first = first;
	}

	public void setSecond(SECOND second) {
		this.second = second;
	}

	@Override
	public String toString(){
		return first.toString() + "-" + second.toString();
	}
   @Override
    public int hashCode() {
    	return this.toString().hashCode();
    }
	
	@Override
	public boolean equals(Object o){
		if ( o instanceof OrderedPair<?,?>){
			OrderedPair<FIRST,SECOND> toCheck = (OrderedPair<FIRST, SECOND>) o;
			if  (toCheck.getFirst().equals(this.getFirst()) && toCheck.getSecond().equals(this.getSecond() ) )
				return true;
			else
				return false;
				
		}else{
			return false;
		}
	}
}
