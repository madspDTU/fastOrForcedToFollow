package fastOrForcedToFollow;

abstract class LinkTransmissionModel {
	
	abstract double getBicycleLength();

	abstract double getLaneVMax(final PseudoLane pseudoLane, final double time);
	
	abstract double getSafetyBufferDistance(final double speed);
	
	
	abstract PseudoLane selectPseudoLane(final Link receivingLink, final double desiredSpeed, final double time);
	
	/*package*/ double getSafetyBufferTime(final double speed) {
		return (getSafetyBufferDistance(speed) - getBicycleLength())   /   speed;
	}
}
