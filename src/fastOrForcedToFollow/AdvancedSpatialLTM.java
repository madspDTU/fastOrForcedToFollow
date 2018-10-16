package fastOrForcedToFollow;

public class AdvancedSpatialLTM extends SpatialLTM {
	
	@Override
	public double getSafetyBufferTime(double speed) {
		return (getSafetyBufferDistance(speed) - Runner.lambda_c)   /   speed;
	//	return Runner.l / speed + Runner.t_safetySqrt / Math.sqrt(speed) + Runner.t_safety + Runner.t_safety2Poly*speed; 
	}
	
	@Override
	public double getSafetyBufferDistance(double speed) {
		return Runner.l  + Runner.t_safetySqrt * Math.sqrt(speed);// +  speed*Runner.t_safety + Runner.t_safety2Poly*speed*speed
	}
	
	@Override
	public PseudoLane selectUnsatisfactoryPseudoLane(Link nextLink, int maxLaneIndex){
		return nextLink.psi[maxLaneIndex];
	}
}
