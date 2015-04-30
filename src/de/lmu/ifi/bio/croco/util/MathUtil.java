package de.lmu.ifi.bio.croco.util;

import java.util.Collections;
import java.util.List;

public class MathUtil {
	public static float variance(List<Float> values) {
		float mean = mean(values);
		
		float variance = 0;
		for(float value : values){
			variance += Math.pow(value-mean, 2);
		}
		variance = variance/((float)values.size());
		return variance;
	}

	public static float mean(List<Float> values){
		float mean = 0;
		for(float value : values){
			mean+=value;
		}
		mean = mean/(float)values.size();
		return mean;
	}
	
	/**
	 * see @link{http://en.wikipedia.org/wiki/Spearman%27s_rank_correlation_coefficient}
	 * and  @link{http://chip-seq.googlecode.com/svn/ChIPSeqSoftware/Useq/util/gen/SpearmanCorrelation.java} and
	 * @param exp1
	 * @param exp2
	 * @return
	 */
	public static<E extends Number>  double spearman (List<Pair<E,E>> pairedData){
	
	
		//calculate sum of square differences
		double sumSqrDiffs = 0;
		double n = pairedData.size();
		for (int i=0; i< n; i++) {
			double d1 = pairedData.get(i).getFirst().doubleValue();
			double d2 = pairedData.get(i).getSecond().doubleValue();
			double diff = d1-d2;
			sumSqrDiffs += Math.pow(diff, 2);
		}
		//numerator 6(sumSqrDiffs)
		double numer = 6* sumSqrDiffs;
		
		double denom = n * (Math.pow(n, 2)-1.0);
		//final rho
		return 1-(numer/denom);
	}

	public static<E extends Number> double pearson(List<Pair<E,E>> pairedData ) {
	
		double n = pairedData.size();
		double sum1 = 0;
		double sum2 = 0;
		double sumpr = 0;
		double sumsq1 = 0;
		double sumsq2 = 0;
		
		for (int i = 0; i < n; i++) {
			double d1 = pairedData.get(i).getFirst().doubleValue();
			double d2 = pairedData.get(i).getSecond().doubleValue();
			
			sum1 +=d1;
			sum2 += d2;
			sumpr += d1*d2;
			sumsq1 += Math.pow(d1,2);
			sumsq2 += Math.pow(d2,2);

		}		
	
		double correlation = (sumpr-(sum1*sum2/n))/(Math.sqrt((sumsq1-(Math.pow(sum1, 2)/n))*(sumsq2-(Math.pow(sum2, 2)/n))));
		
		return correlation;
	}

	public static float mean(float[] values){
		float mean = 0;
		for(float value : values){
			mean+=value;
		}
		mean = mean/(float)values.length;
		return mean;
	}
	public static double sd(List<Float> values){
		return Math.sqrt(variance(values));
	}
	public static float variance(float[] values) {
		float mean = mean(values);
		
		float variance = 0;
		for(float value : values){
			variance += Math.pow(value-mean, 2);
		}
		variance = variance/((float)values.length-1);
		return variance;
	}
	public static double sd(float[] values) {
		return Math.sqrt(variance(values));
	}

	public static float median(List<Float> values) {
		Collections.sort(values);
		
		int index = values.size()/2;
		
		return values.get(index);
	}

	public static float min(List<Float> values) {
		float min = values.get(0);
		for(int i=  1 ; i < values.size(); i++){
			min = Math.min(min, values.get(i));
		}
		return min;
	}


}
