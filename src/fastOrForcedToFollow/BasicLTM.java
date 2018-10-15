package fastOrForcedToFollow;

public class BasicLTM extends LinkTransmissionModel{

	@Override	
	public PseudoLane selectUnsatisfactoryPseudoLane(Link nextLink, int maxLaneIndex){
		return nextLink.psi[nextLink.Psi - 1];
	}

	@Override
	public double getSafetyBufferTime(double speed) {
		return 0;
	}

}