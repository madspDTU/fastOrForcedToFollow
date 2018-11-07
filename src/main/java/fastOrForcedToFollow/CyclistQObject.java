package fastOrForcedToFollow;

/**
 * @author mpaulsen
 * 
 * A CyclistQObject is essentially a cyclist, but has a modififed compareTo method, allowing it to be sorted
 * by earliest possible exit time in the priority queue when inserted.
 *
 */
public class CyclistQObject implements Comparable<CyclistQObject>{

	
	private Cyclist cyclist; // The cyclist.

	CyclistQObject(Cyclist cyclist){
		this.cyclist = cyclist;
	}

	/**
	 * @return The cyclist associated with this CQO.
	 */
	public Cyclist getCyclist(){
		return cyclist;
	}
	
	/**
	 * @return The time at which the cyclist entered the Q.
	 */
	
	public double getTime(){
		return this.cyclist.getTEarliestExit();
	}
	
	public int compareTo(CyclistQObject other){
			return Double.compare(this.cyclist.getTEarliestExit(), other.cyclist.getTEarliestExit());
	}

	public int compare(CyclistQObject cqo1, CyclistQObject cqo2){
			return Double.compare(cqo1.cyclist.getTEarliestExit(), cqo2.cyclist.getTEarliestExit());
	}

}
