package fastOrForcedToFollow.timeoutmodifiers.deprecated;

import fastOrForcedToFollow.timeoutmodifiers.TimeoutModifier;

public class CarTimeoutModifierRightPriorityMove extends TimeoutModifier{

	@Override
	public void updateTimeouts(double[][] bicycleTimeouts, double[][] carTimeouts, int inDirection, int outDirection,
			double nowish, double nowish2, boolean[] isSecondary) {
		updateTimeoutsCarPrimaryGeneralOrRightPriority(bicycleTimeouts, carTimeouts, inDirection, outDirection, nowish);
	}
	
}
