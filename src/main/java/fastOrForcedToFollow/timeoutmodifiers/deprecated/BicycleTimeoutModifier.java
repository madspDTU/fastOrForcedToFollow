package fastOrForcedToFollow.timeoutmodifiers.deprecated;

import org.matsim.core.mobsim.qsim.qnetsimengine.QFFFNodeUtils;

import fastOrForcedToFollow.timeoutmodifiers.TimeoutModifier;

public class BicycleTimeoutModifier extends TimeoutModifier {

	public void updateTimeouts(double[][] bicycleTimeouts, double[][] carTimeouts, int inDirection, int outDirection,
			double nowish, boolean isSecondary[]) {

		boolean isFromSecondary = isSecondary != null && isSecondary[inDirection];
		int n = bicycleTimeouts[0].length;

		if(!isFromSecondary) {
			for(int i = 0; i < n; i++){ // All bicycle movements to out direction except from indirection
				if(i != inDirection){
					updateEntry(bicycleTimeouts, i, outDirection, nowish);
				}
			}
		} else {
			for(int i = 0; i < n; i++){ // All bicycle movements to out direction except from indirection and priority indirections
				if(isSecondary[i] && i != inDirection){
					updateEntry(bicycleTimeouts, i, outDirection, nowish);
				}
			}
		}

		// If indirection = "outdirection + 1". _Rightest_ possible turn.

		int j = QFFFNodeUtils.increaseInt(inDirection, n);
		
		if(!isFromSecondary) { // Primary move
			while(j != outDirection){
				for(int i = 0; i < n; i++){
					updateEntry(carTimeouts,i,j,nowish); // all car movements to a road to the right of the movement
					updateEntry(carTimeouts,j,i,nowish); // all car movements from the right of the movement 
					updateEntry(bicycleTimeouts,j,i, nowish); // all bicycle movements from the right of the movement
					if(i != inDirection){	
						updateEntry(bicycleTimeouts,i,j,nowish); // all bicycle movements to the right of the movement (except from indirection)
					}
				}
				j = QFFFNodeUtils.increaseInt(j, n);
			}
		} else { // Secondary move
			while(j != outDirection){
				for(int i = 0; i < n; i++){
					if(isSecondary[i]) {
						updateEntry(carTimeouts,i,j,nowish); // all car movements to a road to the right of the movement
						if(i != inDirection){	
							updateEntry(bicycleTimeouts,i,j,nowish); // all bicycle movements to the right of the movement (except from indirection)
						}
					}
					if(isSecondary[j]) {
						updateEntry(carTimeouts,j,i,nowish); // all car movements from the right of the movement 
						updateEntry(bicycleTimeouts,j,i, nowish); // all bicycle movements from the right of the movement
					}
				}
				j = QFFFNodeUtils.increaseInt(j, n);
			}
		}
	}

	@Override
	public void updateTimeouts(double[][] bicycleTimeouts, double[][] carTimeouts, int inDirection, int outDirection,
			double nowish, double nowish2, boolean[] isSecondary) {
		// TODO Auto-generated method stub
		
	}
}
