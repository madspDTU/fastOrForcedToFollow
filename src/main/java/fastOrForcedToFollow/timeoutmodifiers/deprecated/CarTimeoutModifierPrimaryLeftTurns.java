package fastOrForcedToFollow.timeoutmodifiers.deprecated;

import org.matsim.core.mobsim.qsim.qnetsimengine.QFFFNodeUtils;

import fastOrForcedToFollow.timeoutmodifiers.TimeoutModifier;

public class CarTimeoutModifierPrimaryLeftTurns extends TimeoutModifier{

	@Override
	public void updateTimeouts(double[][] bicycleTimeouts, double[][] carTimeouts, int inDirection, int outDirection,
			double nowish, double nowish2, boolean[] isSecondary) {
		updateTimeouts(bicycleTimeouts, carTimeouts, inDirection, outDirection, nowish2, isSecondary);
		
	}
	
	private void updateTimeouts(double[][] bicycleTimeouts, double[][] carTimeouts, int inDirection,
			int outDirection,  double nowish2, boolean[] isSecondary) {

		int n = bicycleTimeouts[0].length;

		// Car movements to out direction
		for(int i = 0; i < n; i++){ // All car movements to out direction except from indirection
			if(i != inDirection){
				updateEntry(carTimeouts,i,outDirection,nowish2);
			}
		}

		if(inDirection == outDirection) {
			return;
		}


		//Conflicting car movements
		int r = QFFFNodeUtils.increaseInt(inDirection, n); //new
		while(r != outDirection){
			for(int i = 0; i < n; i++){
				if(isSecondary[r]) { // Cannot block a general move from a priority link
					updateEntry(carTimeouts,r,i,nowish2); // all car movements from the right 	
				}
				if(i != inDirection){	
					updateEntry(carTimeouts,i,r,nowish2); // all car movements to the right
				}
			}
			r = QFFFNodeUtils.increaseInt(r,n);
		}
		
		// Conflicting bicycle movements
		r = inDirection;
		while(r != outDirection){
			int l = inDirection;
			while(l != outDirection){
				if(isSecondary[r]) { // Cannot block a general move from a priority link
					updateEntry(bicycleTimeouts,r,l,nowish2);	// All bicycle movements from right to left.
				}
				if(l != inDirection){
					updateEntry(bicycleTimeouts,l,r,nowish2); // All bicycle movements from left to right (except from inDirection).
				}
				l = QFFFNodeUtils.decreaseInt(l,n);
			}
			if(r != inDirection) { // new
				updateEntry(bicycleTimeouts,l,r,nowish2); // The final one (from outDirection)	
			} 
			r = QFFFNodeUtils.increaseInt(r,n);
		}
	}	

}
