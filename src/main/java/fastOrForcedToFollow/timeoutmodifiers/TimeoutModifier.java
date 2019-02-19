package fastOrForcedToFollow.timeoutmodifiers;

public interface TimeoutModifier {
	
	void updateTimeouts(double[][] bicycleTimeouts, double[][] carTimeouts, int inDirection, 
			int outDirection, boolean[] isSecondary, final  double nowish);
	
}
