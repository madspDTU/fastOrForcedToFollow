package fastOrForcedToFollow;


import org.matsim.core.mobsim.qsim.qnetsimengine.QCycle;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.PriorityQueue;

/**
 * 
 * @author mpaulsen
 */
public final class Link{

	
	/**
	 * The id of the link.
	 */
	private final String id;

	
	/**
	 * The total pseudolane distance (buffer distance) occupied of the cyclists currently on the link.
	 */
	private double occupiedSpace = 0;


	/**
	 * The Q containing the CQO's for the cyclists that have entered the link but not yet left it.
	 */
	private final PriorityQueue<QCycle> outQ;

	/**
	 * The array containing all pseudolanes of the link.
	 */
	private final PseudoLane[] psi;


	/**
	 * The total pseudolane distance, i.e. the product between the length and number of pseudolanes.
	 */
	private final double totalLaneLength; 

	private double lastTimeMoved;
	
	
	//mads: Buffer created in order to fit the piece into MATSim. Would be nice to do without.
	private final LinkedList<QVehicle> leavingVehicles;
	
	public void addVehicleToLeavingVehicles(final QVehicle veh){
		this.leavingVehicles.addLast(veh);
	}
	public QVehicle getFirstLeavingVehicle(){
		return leavingVehicles.isEmpty() ? null : leavingVehicles.peekFirst();
	}
	public QVehicle pollFirstLeavingVehicle(){
		return leavingVehicles.isEmpty() ? null : leavingVehicles.pollFirst();
	}
	public boolean hasNoLeavingVehicles(){
		return leavingVehicles.isEmpty();
	}
	

	public Link(final String id, final double width, final double length) throws InstantiationException, IllegalAccessException{
		this(id, 1 + (int) Math.floor((width-Runner.deadSpace)/Runner.omega), length );
	}
	
	//Constructor using the number of pseudoLanes directly.
	public Link(final String id, final int Psi, final double length) throws InstantiationException, IllegalAccessException{
		this.id = id;
		this.psi = createPseudoLanes(Psi, length);

		this.outQ = new PriorityQueue<>( new Comparator<QCycle>(){
			@Override
			public int compare( QCycle cqo1, QCycle cqo2 ){
				return Double.compare(cqo1.getCyclist().getTEarliestExit(), cqo2.getCyclist().getTEarliestExit());
			}
		} ) ;
		
		this.leavingVehicles = new LinkedList<QVehicle>();

		double totalLaneLength = 0.;
		for(PseudoLane pseudoLane : psi){
			totalLaneLength += pseudoLane.getLength();
		}
		this.totalLaneLength = totalLaneLength;
		
	}

	/**
	 * @return The array of PseudoLanes to be created
	 */
	private PseudoLane[] createPseudoLanes(final int Psi, final double length){
		PseudoLane[] psi = new PseudoLane[Psi];
		for(int i = 0; i < Psi; i++){
			psi[i] = new PseudoLane(length);
		}
		return psi;
	}

	
	
	/**
	 * @return The integer id of the link.
	 */
	public String getId(){
		return id;
	}


	/**
	 * @return The number of <code>pseudolanes</code>.
	 */
	public int getNumberOfPseudoLanes(){
		return psi.length;
	}


	/** 
	 * @return The outQ belonging to the link.
	 */
	public PriorityQueue<QCycle> getOutQ(){
		return outQ;
	}

	/**
	 * Gets a <code>pseudolane</code> from the <code>link</code>.
	 * 
	 * @param i the index where 0 is the rightmost <code>pseudolane</code>, and <code>Psi</code> - 1 is the leftmost.
	 * 
	 * @return The i'th <code>pseudolane</code> from the right (0 being the rightmost).
	 */
	public PseudoLane getPseudoLane(final int i){
		return psi[i];
	}

	
	/**
	 * @return <code>true</code> iff the link is full, i.e. the occupied space is at least as large as the total lane length.
	 */
	public boolean isLinkFull(){
		return occupiedSpace >= totalLaneLength;
	}

	/**
	 * Reduces the occupied space of link <code>linkId</code> by the safety distance corresponding to <code>speed</code>
	 * 
	 * @param speed on which the safety distance will be based.
	 */
	public void reduceOccupiedSpace(final Cyclist cyclist, final double speed){
		this.supplementOccupiedSpace(-cyclist.getSafetyBufferDistance(speed));
	}

	public void increaseOccupiedSpace(final Cyclist cyclist, final double speed){
		this.supplementOccupiedSpace(cyclist.getSafetyBufferDistance(speed));
	}


	/**
	 * @param length in metres by which the occupied space will be supplemented.
	 */
	public void supplementOccupiedSpace(final double length){
		occupiedSpace += length;
	}

	
	
	public double getLastTimeMoved(){
		return this.lastTimeMoved;
	}
	
	public void setLastTimeMoved(final double lastTimeMoved){
		this.lastTimeMoved = lastTimeMoved;
	}

}
