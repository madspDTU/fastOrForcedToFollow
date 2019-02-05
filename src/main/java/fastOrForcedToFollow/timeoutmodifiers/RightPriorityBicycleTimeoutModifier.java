package fastOrForcedToFollow.timeoutmodifiers;

import org.matsim.core.mobsim.qsim.qnetsimengine.QFFFNodeUtils;

public class RightPriorityBicycleTimeoutModifier implements TimeoutModifierI {

	public void updateTimeouts(double[][] bicycleTimeouts, double[][] carTimeouts, int inDirection, int outDirection,
			boolean[] isSecondary, double nowish) {
		int n = bicycleTimeouts[0].length;
		for(int i = 0; i < n; i++){ // All bicycle movements to out direction except from indirection
			if(i != inDirection){
				bicycleTimeouts[i][outDirection] = nowish;
			}
		}

		if(inDirection == QFFFNodeUtils.decreaseInt(outDirection, n)){
			return;
		} 


		int j = QFFFNodeUtils.increaseInt(inDirection, n);
		while(j != outDirection){

			for(int i = 0; i < n; i++){
				carTimeouts[i][j] = nowish; // all car movements to a road to the right of the movement
				carTimeouts[j][i] = nowish; // all car movements from the right of the movement 
				bicycleTimeouts[j][i] = nowish; // all bicycle movements from the right of the movement
				if(i != inDirection){	
					bicycleTimeouts[i][j] = nowish; // all bicycle movements to the right of the movement (except from indirection)
				}
			}

			j = QFFFNodeUtils.increaseInt(j, n);
		}

	}
}
