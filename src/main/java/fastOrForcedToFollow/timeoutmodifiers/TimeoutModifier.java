package fastOrForcedToFollow.timeoutmodifiers;

public interface TimeoutModifier {
	
	//TODO In near future, this should be an _absract class_ instead of an _interface_.
	
	void updateTimeouts(double[][] bicycleTimeouts, double[][] carTimeouts, int inDirection, 
			int outDirection, boolean[] isSecondary, final  double nowish);
	
	
	/*
	 * This method is unavoidable when using unequal population samples:
	 * 
	 * If a car at time 10 sets the time t = 20,
	 * then it is possible that a bicycle at time 12 sets time t = 13. This is obviously wrong.
	 */
	default void updateEntry(double[][] matrix, int dim1, int dim2, double newTime){
	//	matrix[dim1][dim2] = Double.max(matrix[dim1][dim2], newTime);
		double oldTime = matrix[dim1][dim2];
		if(oldTime < newTime){
			matrix[dim1][dim2] = newTime;
		}
	}
	
}
