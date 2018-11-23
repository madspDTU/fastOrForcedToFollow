package fastOrForcedToFollow;

abstract class LinkTransmissionModel {
	
	
	/**
	 * Method for returning the length of a bicycle.
	 * 
	 * @return The length [m] of a bicycle.
	 */
	abstract double getBicycleLength();

	/**
	 * Method for determining the maximum allowed speed at a pseudolane at a given time.
	 * 
	 * @param pseudoLane The PseudoLane for which the maximum speed is determined.
	 * @param time The time at which the speed determination is based.
	 * 
	 * @return The maximum allowed speed [m/s] the <code>Pseudolane</code> when entering at <code>time</code>.
	 */
	abstract double getLaneVMax(final PseudoLane pseudoLane, final double time);
	
	/**
	 * Method for determining the safety buffer distance given a speed.
	 * 
	 * @param speed The speed [m/s] that the safety distance will be based on.
	 * 
	 * @return safety distance [m] including the length of its own bicycle.
	 */
	abstract double getSafetyBufferDistance(final double speed);
	
	/**
	 * Method for determining the most appropriate pseudolane of a link given a desired speed and an entrance time.
	 * 
	 * @param receivingLink The Link that the Cyclist is entering.
	 * @param desiredSpeed The desired speed [m/s] of the Cyclist.
	 * @param time The time at which the lane selection takes place.
	 * 
	 * @return The selected pseudolane, i.e. the right-most pseudolane to accomodate the desired speed,
	 *         alternatively the pseudolane with the highest speed.
	 */
	abstract PseudoLane selectPseudoLane(final Link receivingLink, final double desiredSpeed, final double time);
	
	
	
	/**
	 * Method for determining the safety buffer time given corresponding methods for safety buffer distance and bicycle lengths.
	 * 
	 * @param speed The speed [m/s] at which the safety buffer time is calculated.
	 * 
	 * @return The safety buffer time [s], i.e. the time from the backwheel of the cyclist in front to the frontwheel of the cyclist.
	 */
	/*package*/ double getSafetyBufferTime(final double speed) {
		return (getSafetyBufferDistance(speed) - getBicycleLength())   /   speed;
	}
}
