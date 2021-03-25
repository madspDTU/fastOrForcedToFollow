package fastOrForcedToFollow.timeoutmodifiers.deprecated;

import org.matsim.core.mobsim.qsim.qnetsimengine.QFFFNodeUtils;

import fastOrForcedToFollow.timeoutmodifiers.TimeoutModifier;

public class CarTimeoutModifierPrioritisedAntiPriorityMove extends TimeoutModifier{

	public void updateTimeouts(double[][] bicycleTimeouts, double[][] carTimeouts, int inDirection,
			int outDirection,  double nowish, double nowish2, boolean[] isSecondary) {

		int n = bicycleTimeouts[0].length;

		// Car movements to out direction
		for(int i = 0; i < n; i++){ // All car movements to out direction except from indirection
			if(i != inDirection){
				updateEntry(carTimeouts,i,outDirection,getNowish(isSecondary, i, nowish, nowish2));
			}
		}

		if(inDirection == outDirection) {
			return;
		}


		//Conflicting car movements
		int r = QFFFNodeUtils.increaseInt(inDirection, n); //new
		while(r != outDirection){
			double nowishR = isSecondary[r] ? nowish2 : nowish;
			for(int i = 0; i < n; i++){
				updateEntry(carTimeouts,r,i,nowishR); // all car movements from the right 	
				if(i != inDirection){	
					updateEntry(carTimeouts,i,r,getNowish(isSecondary, i, nowish, nowish2)); // all car movements to the right
				}
			}
			r = QFFFNodeUtils.increaseInt(r,n);
		}

		// Conflicting bicycle movements
		r = inDirection;
		while(r != outDirection){
			double nowishR = getNowish(isSecondary, r, nowish, nowish2);
			int l = inDirection;
			while(l != outDirection){
				updateEntry(bicycleTimeouts,r,l,nowishR);	// All bicycle movements from right to left.
				if(l != inDirection){
					updateEntry(bicycleTimeouts,l,r,getNowish(isSecondary, l, nowish, nowish2)); // All bicycle movements from left to right (except from inDirection).
				}
				l = QFFFNodeUtils.decreaseInt(l,n);
			}
			if(r != inDirection) { // new
				updateEntry(bicycleTimeouts,l,r,getNowish(isSecondary, l, nowish, nowish2)); // The final one (from outDirection)	
			} 
			r = QFFFNodeUtils.increaseInt(r,n);
		}
	}

	private double getNowish(boolean[] isSecondary, int inIndex, double nowish, double nowish2) {
		if(isSecondary[inIndex]) {
			return nowish2;
		} else {
			return nowish;
		} 
	}


}
