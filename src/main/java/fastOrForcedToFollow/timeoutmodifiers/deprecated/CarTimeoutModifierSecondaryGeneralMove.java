package fastOrForcedToFollow.timeoutmodifiers.deprecated;

import org.matsim.core.mobsim.qsim.qnetsimengine.QFFFNodeUtils;

import fastOrForcedToFollow.timeoutmodifiers.TimeoutModifier;

public class CarTimeoutModifierSecondaryGeneralMove extends TimeoutModifier{

	@Override
	public void updateTimeouts(double[][] bicycleTimeouts, double[][] carTimeouts, int inDirection, int outDirection,
			double nowish, double nowish2, boolean[] isSecondary) {

		int n = bicycleTimeouts[0].length;

		//Out direction and the two priority directions are directions of interest. 
		//Since our direction is either before or at the first priority direction met,
		//all other moves are left turns if the counter is positive (after three visits it is back to 0.
		int directionsOfInterestReached = 0;  
		int r = QFFFNodeUtils.increaseInt(inDirection,n);
		while(r != inDirection) {
			if(r == outDirection) {
				directionsOfInterestReached = QFFFNodeUtils.increaseInt(directionsOfInterestReached, 3);
			}
			if(isSecondary[r]){
				updateEntry(carTimeouts,r,outDirection,directionsOfInterestReached > 0  ? nowish2 : nowish);
			}  else {
				QFFFNodeUtils.increaseInt(directionsOfInterestReached, 3);
			}
			r = QFFFNodeUtils.increaseInt(r, n);
		}

		if(inDirection == outDirection) {
			return;
		}

		
		///TODO HAVENT FIXED BEYOND THIS POINT

		//Conflicting car movements
		r = QFFFNodeUtils.increaseInt(inDirection, n); //new
		while(r != outDirection){
			for(int i = 0; i < n; i++){
				if(isSecondary[r]) {
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
				if(isSecondary[r]) {
					updateEntry(bicycleTimeouts,r,l,nowish2);	// All bicycle movements from right to left.
				}
				if(isSecondary[l] && l != inDirection){
					updateEntry(bicycleTimeouts,l,r,nowish2); // All bicycle movements from left to right (except from inDirection).
				}
				l = QFFFNodeUtils.decreaseInt(l,n);
			}
			if(isSecondary[l] && r != inDirection) { // new
				updateEntry(bicycleTimeouts,l,r,nowish2); // The final one (from outDirection)	
			} 
			r = QFFFNodeUtils.increaseInt(r,n);
		}
	}

}
