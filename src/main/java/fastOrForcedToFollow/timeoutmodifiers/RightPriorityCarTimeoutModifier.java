package fastOrForcedToFollow.timeoutmodifiers;

import org.matsim.core.mobsim.qsim.qnetsimengine.QFFFNodeUtils;

public class RightPriorityCarTimeoutModifier implements TimeoutModifier{

	public void updateTimeouts(double[][] bicycleTimeouts, double[][] carTimeouts, int inDirection,
			int outDirection, boolean[] isSecondary, double nowish) {
		int n = bicycleTimeouts[0].length;
		
		for(int i = 0; i < n; i++){ // All car movements to out direction except from indirection
			if(i != inDirection){
				updateEntry(carTimeouts,i,outDirection,nowish);
			}
		}

		int r = inDirection;
		while(r != outDirection){
			int l = inDirection;
			while(l != outDirection){
				updateEntry(bicycleTimeouts,r,l,nowish);	// All bicycle movements from right to left.
				if(l != inDirection){
					updateEntry(bicycleTimeouts,l,r,nowish); // All bicycle movements from left to right (except from inDirection).
				}
				l = QFFFNodeUtils.decreaseInt(l,n);
			}
			updateEntry(bicycleTimeouts,l,r,nowish); // The final one (from outDirection)
			r = QFFFNodeUtils.increaseInt(r,n);
		}

		if(inDirection == QFFFNodeUtils.decreaseInt(outDirection,n)){
			return;
		} 

		r = inDirection;
		while(r != outDirection){
			for(int i = 0; i < n; i++){
				updateEntry(carTimeouts,r,i,nowish); // all car movements from the right 	
				if(i != inDirection){	
					updateEntry(carTimeouts,i,r,nowish); // all car movements to the right
				}
			}
			r = QFFFNodeUtils.increaseInt(r,n);
		}
	}	
	
}
