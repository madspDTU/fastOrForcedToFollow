package fastOrForcedToFollow;

/**
 * @author mpaulsen
 *
 */
public class LinkQObject implements Comparable<LinkQObject>{

	private double time;
	private int linkId;
	private long tieBreaker; 

	LinkQObject(double time, int linkId){
		this.time = time;
		this.linkId = linkId;
		Runner.tieBreaker++;
		this.tieBreaker = Runner.tieBreaker;
	}
	
	LinkQObject(double time, int linkId, long tieBreaker){
		this.time = time;
		this.linkId = linkId;
		this.tieBreaker = tieBreaker;
	}
	
	/**
	 * @return The time associated with this LQO.
	 */
	public double getTime(){
		return time;
	}
	
	/**
	 * @return The id of the link associated with this LQO.
	 */
	public int getId(){
		return linkId;
	}

	public int compareTo(LinkQObject other){
		if(this.time != other.time){
			return Double.compare(this.time, other.time);
		} else {
			return Long.compare(this.tieBreaker, other.tieBreaker);
		}
	}

	public int compare(LinkQObject lqo1, LinkQObject lqo2){
		if(lqo1.time != lqo2.time){
			return Double.compare(lqo1.time, lqo2.time);
		} else {
			return Long.compare(lqo1.tieBreaker, lqo2.tieBreaker);
		}
	}

}
