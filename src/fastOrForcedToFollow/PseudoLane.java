package fastOrForcedToFollow;


public class PseudoLane {
	
	public double length;
	public Link link;
	public double tEnd;		// The time at which the previous cyclist will have exited the link with his/her entire bicycle.
	public double tStart; 	// The time at which the previous cyclist will have entered the link with his/her entire bicycle.
	
	PseudoLane(double length, Link link){
		this.length = length;
		this.link = link;
		this.tStart = -1;
		this.tEnd = tStart + Double.MIN_VALUE;
	}

	public void update(double newTStart, double newTEnd){
		this.tStart = newTStart;
		this.tEnd = newTEnd;
	}
	
	public void updateTStart(double newTStart){
		this.tStart = newTStart;
	}
	
	public void updateTEnd(double newTEnd){
		this.tStart = newTEnd;
	}
}
