package fastOrForcedToFollow;

public class CyclistQObject implements Comparable<CyclistQObject>{

	public double time; // First possible time at which the cyclist will be taken into consideration of moving onto next link.
						// If rejected, this time does not change. It is kept in order to keep track of the order of the outQ of the link.
	public Cyclist cyclist; // The cyclist.
	public long tieBreaker; // A tie breaker that we hopefully can do without (in the long run (at least)).

	CyclistQObject(double time, Cyclist cyclist){
		this.time = time;
		this.cyclist = cyclist;
		this.tieBreaker = System.currentTimeMillis();
	}

	public int compareTo(CyclistQObject other){
		if(this.time != other.time){
			return Double.compare(this.time, other.time);
		} else {
			return Long.compare(this.tieBreaker, other.tieBreaker);
		}
	}

	public int compare(CyclistQObject cqo1, CyclistQObject cqo2){
		if(cqo1.time != cqo2.time){
			return Double.compare(cqo1.time, cqo2.time);
		} else {
			return Long.compare(cqo1.tieBreaker, cqo2.tieBreaker);
		}
	}
/*
	public void enterCyclist(){
		if(cyclist.route.isEmpty()){
			cyclist.terminateCyclist(time);
		} else {
			cyclist.advanceCyclist(time);
		}
	}
	*/

}
