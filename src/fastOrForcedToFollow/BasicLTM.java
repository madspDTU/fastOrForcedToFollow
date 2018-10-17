package fastOrForcedToFollow;

public class BasicLTM extends LinkTransmissionModel{

	@Override	
	public PseudoLane selectUnsatisfactoryPseudoLane(Link nextLink, int maxLaneIndex){
		return nextLink.getPseudoLane(nextLink.getNumberOfPseudoLanes() - 1);
	}

	@Override
	public double getSafetyBufferTime(double speed) {
		return 0;
	}

}