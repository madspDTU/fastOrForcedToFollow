package fastOrForcedToFollow.timeoutmodifiers.deprecated;

import org.matsim.core.mobsim.qsim.qnetsimengine.QFFFNodeUtils;

import fastOrForcedToFollow.timeoutmodifiers.TimeoutModifier;

public class CarTimeoutModifierSecondaryLeftMove extends TimeoutModifier{

	@Override
	public void updateTimeouts(double[][] bicycleTimeouts, double[][] carTimeouts, int inDirection, int outDirection,
			double nowish, double nowish2, boolean[] isSecondary) {
		updateTimeouts(bicycleTimeouts, carTimeouts, inDirection, outDirection, nowish2, isSecondary);
		
	}
	
	private void updateTimeouts(double[][] bicycleTimeouts, double[][] carTimeouts, int inDirection,
			int outDirection,  double nowish, boolean[] isSecondary) {

		int n = bicycleTimeouts[0].length;

		if(isSecondary[outDirection]) {
			for(int i = 0; i < n; i++){ // All car movements to out direction except from indirection
				if(isSecondary[i] && i != inDirection){
					updateEntry(carTimeouts,i,outDirection,nowish);
				}
			}
		}

		if(inDirection == outDirection) {
			return;
		}


		//Conflicting car movements
		int r = QFFFNodeUtils.increaseInt(inDirection, n); //new
		while(r != outDirection){
			if(isSecondary[r]) {
				for(int i = 0; i < n; i++){
					if(isSecondary[i]) {
						updateEntry(carTimeouts,r,i,nowish); // all car movements from the right 	
						if(i != inDirection){	
							updateEntry(carTimeouts,i,r,nowish); // all car movements to the right
						}
					}
				}
			}
			r = QFFFNodeUtils.increaseInt(r,n);
		}

		// Conflicting bicycle movements
		r = inDirection;
		while(r != outDirection){
			if(isSecondary[r]) {
				int l = inDirection;
				while(l != outDirection){
					if(isSecondary[l]) {
						if(isSecondary[r]) {
							updateEntry(bicycleTimeouts,r,l,nowish);	// All bicycle movements from right to left.
						}
						if(isSecondary[l] && l != inDirection){
							updateEntry(bicycleTimeouts,l,r,nowish); // All bicycle movements from left to right (except from inDirection).
						}
					}
					l = QFFFNodeUtils.decreaseInt(l,n);
				}
				if(isSecondary[l] && r != inDirection) { // new
					updateEntry(bicycleTimeouts,l,r,nowish); // The final one (from outDirection)	
				} 
			}
			r = QFFFNodeUtils.increaseInt(r,n);
		}
	}


}
