package fastOrForcedToFollow;

public class AdvancedSpatialLTM extends SpatialLTM {
	
	@Override
	public double getSafetyBufferTime(double speed) {
		return (getSafetyBufferDistance(speed) - Runner.lambda_c)   /   speed;
	//	return Runner.l / speed + Runner.t_safetySqrt / Math.sqrt(speed) + Runner.t_safety + Runner.t_safety2Poly*speed; // From Andresen et al. (2014), Basic Driving Dynamics of Cyclists, In: Simulation of Urban Mobility;
	}
	
	@Override
	public double getSafetyBufferDistance(double speed) {
		return Runner.l  + Runner.t_safetySqrt * Math.sqrt(speed) +  speed*Runner.t_safety + Runner.t_safety2Poly*speed*speed; // From Andresen et al. (2014), Basic Driving Dynamics of Cyclists, In: Simulation of Urban Mobility;
	}
	
	@Override
	public PseudoLane selectUnsatisfactoryPseudoLane(Link nextLink, int maxLaneIndex){
		return nextLink.psi[maxLaneIndex];
	}
}
