package fastOrForcedToFollow;


/**
 * @author mpaulsen
 */
public final class ToolBox {
	
	// Suppress default constructor for noninstantiability
	private ToolBox(){
		throw new AssertionError();
	}

	
	/**
	 * Method for computing the inverse cumulative distribution function of the standard normal distribution.
	 * Taken directly from https://stackedboxes.org/2017/05/01/acklams-normal-quantile-function/
	 * 
	 * @param u The quantile whose corresponding value, z, is to be found.
	 * 
	 * @return The value that would yield u when inserted into the
	 * cumulative distribution function of the standard normal distribution 
	 */
	public static double qNorm(final double u){
		double a1 = -3.969683028665376e+01;
		double a2 =  2.209460984245205e+02;
		double a3 = -2.759285104469687e+02;
		double a4 =  1.383577518672690e+02;
		double a5 = -3.066479806614716e+01;
		double a6 =  2.506628277459239e+00;

		double b1 = -5.447609879822406e+01;
		double b2 =  1.615858368580409e+02;
		double b3 = -1.556989798598866e+02;
		double b4 =  6.680131188771972e+01;
		double b5 = -1.328068155288572e+01;

		double c1 = -7.784894002430293e-03;
		double c2 = -3.223964580411365e-01;
		double c3 = -2.400758277161838e+00;
		double c4 = -2.549732539343734e+00;
		double c5 =  4.374664141464968e+00;
		double c6 =  2.938163982698783e+00;

		double d1 =  7.784695709041462e-03;
		double d2 =  3.224671290700398e-01;
		double d3 =  2.445134137142996e+00;
		double d4 =  3.754408661907416e+00;

		double p_low  = 0.02425;
		double p_high = 1 - p_low;

		if(u < p_low){
			double q = Math.sqrt(-2*Math.log(u));
			return (((((c1*q+c2)*q+c3)*q+c4)*q+c5)*q+c6) /
					((((d1*q+d2)*q+d3)*q+d4)*q+1);
		}

		if(u > p_high){
			double q = Math.sqrt(-2*Math.log(1-u));
			return -(((((c1*q+c2)*q+c3)*q+c4)*q+c5)*q+c6) /
					((((d1*q+d2)*q+d3)*q+d4)*q+1);
		}

		double q = u - 0.5;
		double r = q * q;
		return (((((a1*r+a2)*r+a3)*r+a4)*r+a5)*r+a6)*q /
				(((((b1*r+b2)*r+b3)*r+b4)*r+b5)*r+1);


	}
	
	
	public static double uniformToJohnson(final double u){
		return Runner.JOHNSON_LAMBDA * Math.sinh( (qNorm(u) - Runner.JOHNSON_GAMMA) / Runner.JOHNSON_DELTA) + Runner.JOHNSON_XSI;
	}
	
	
}
