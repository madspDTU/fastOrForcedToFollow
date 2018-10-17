package fastOrForcedToFollow;


/**
 * @author mpaulsen
 *
 */
public class PseudoLane {
	
	public double length;
	public Link link;
	public double tEnd;		// The time at which the previous cyclist will have exited the link with his/her entire bicycle.
	public double tReady; 	// The time at which the previous cyclist will have entered the link with his/her entire bicycle.
	
	PseudoLane(double length, Link link){
		this.length = length;
		this.link = link;
		this.tReady = -1;
		this.tEnd = tReady + Double.MIN_VALUE;
	}

	public void update(double newTStart, double newTEnd){
		this.tReady = newTStart;
		this.tEnd = newTEnd;
	}
	
	public void updateTStart(double newTStart){
		this.tReady = newTStart;
	}
	
	public void updateTEnd(double newTEnd){
		this.tReady = newTEnd;
	}
	
	public void updateTs(double speed, double time) {
		double tStartBasis = Double.max(time, this.tReady);
		this.tReady = tStartBasis + Runner.lambda_c / speed;
		this.tEnd = this.tReady + this.length / speed;
	}
}
