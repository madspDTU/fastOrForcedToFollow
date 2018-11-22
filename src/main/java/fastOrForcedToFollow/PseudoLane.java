package fastOrForcedToFollow;


/**
 * @author mpaulsen
 *
 */
public final class PseudoLane {

	private final double length;   //
	private double tEnd;		// The time at which the previous cyclist will have exited the link with his/her entire bicycle.
	private double tReady; 	// The time at which the previous cyclist will have entered the link with his/her entire bicycle.

	PseudoLane(final double length){
		this.length = length;
	}

	public double getLength(){
		return this.length;
	}
	
	public double getTEnd(){
		return this.tEnd;
	}
	
	public double getTReady(){
		return this.tReady;
	}

	public void setTReady(final double newTReady){
		this.tReady = newTReady;
	}

	public void setTEnd(final double newTEnd){
		this.tEnd = newTEnd;
	}

}
