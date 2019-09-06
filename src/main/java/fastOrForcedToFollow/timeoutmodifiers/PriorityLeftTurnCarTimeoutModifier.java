package fastOrForcedToFollow.timeoutmodifiers;

import org.matsim.core.mobsim.qsim.qnetsimengine.QFFFNodeUtils;

public class PriorityLeftTurnCarTimeoutModifier implements TimeoutModifier {

	@Override
	public void updateTimeouts(double[][] bicycleTimeouts, double[][] carTimeouts, int inDirection, int outDirection,
			 boolean[] isSecondary, double nowish) {
		int n = bicycleTimeouts[0].length;
		for(int i = 0; i < n; i++){ // All car movements to out direction except from itself and the other (right turn) priority.
			if(isSecondary[i]){
				updateEntry(carTimeouts,i,outDirection,nowish);
			}
		}

		int r = inDirection;
		while(r != outDirection){
			int l = inDirection;
			while(l != outDirection){
				if( isSecondary[r] ){ // if not from Priority (also excludes left turns, but these
					// cannot be pulled through anyway (they are stepwise).
					updateEntry(bicycleTimeouts,r,l,nowish);
				}
				if( isSecondary[l] ){
					updateEntry(bicycleTimeouts,l,r,nowish); // All non-priority bicycle movements from left to right (except from inDirection).
				}
				l = QFFFNodeUtils.decreaseInt(l, n);
			}
			if(isSecondary[l]){
				updateEntry(bicycleTimeouts,l,r,nowish);  // The final one (from outDirection) if not a priority
			}
			r = QFFFNodeUtils.decreaseInt(r, n);
		}

		if(inDirection == QFFFNodeUtils.decreaseInt(outDirection, n)){
			return;
		} 

		r = inDirection;
		while(r != outDirection){
			for(int i = 0; i < n; i++){
				if(!isSecondary[r]){
					updateEntry(carTimeouts,r,i,nowish); // all car movements from the right 	
				}
				if(!isSecondary[i]){	
					updateEntry(carTimeouts,i,r,nowish);
				}
			}
			r = QFFFNodeUtils.decreaseInt(r, n);
		}

	}

}
