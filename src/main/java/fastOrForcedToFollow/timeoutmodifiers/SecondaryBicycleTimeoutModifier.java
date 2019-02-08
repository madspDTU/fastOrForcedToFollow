package fastOrForcedToFollow.timeoutmodifiers;

import org.matsim.core.mobsim.qsim.qnetsimengine.QFFFNodeUtils;

public class SecondaryBicycleTimeoutModifier implements TimeoutModifierI {

	@Override
	public void updateTimeouts(double[][] bicycleTimeouts, double[][] carTimeouts, int inDirection, int outDirection,
			boolean[] isSecondary, double nowish) {

		int n = bicycleTimeouts[0].length;
		for(int i = 0; i < n; i++){ // All bicycle movements to out direction except from priorities and inDirection
			if(isSecondary[i] && i != inDirection){
				bicycleTimeouts[i][outDirection] = nowish;
			}
		}

		if(inDirection == outDirection - 1){
			return;
		} 


		int j = QFFFNodeUtils.increaseInt(inDirection, n);
		while(j != outDirection){

			for(int i = 0; i < n; i++){
				if(isSecondary[i]){
					carTimeouts[i][j] = nowish; // all car movements to a road to the right of the movement
					if(i != inDirection){	
						bicycleTimeouts[i][j] = nowish; // all bicycle movements to the right of the movement (except from indirection)
					}
				}
				if(isSecondary[j]){
					carTimeouts[j][i] = nowish; // all car movements from the right of the movement 
					bicycleTimeouts[j][i] = nowish; // all bicycle movements from the right of the movement
				}
			}

			j = QFFFNodeUtils.increaseInt(j, n);
		}

	}

}
