package fastOrForcedToFollow;


import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.qsim.qnetsimengine.HasLeftBufferTime;
import org.matsim.core.mobsim.qsim.qnetsimengine.QCycle;
import org.matsim.core.mobsim.qsim.qnetsimengine.QCycleAndMoveType;
import org.matsim.core.mobsim.qsim.qnetsimengine.QFFFNode.MoveType;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicleAndMoveType;

import fastOrForcedToFollow.configgroups.FFFConfigGroup;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.PriorityQueue;

/**
 * 
 * @author mpaulsen
 */
public abstract class Sublink implements HasLeftBufferTime {


	private final PriorityQueue<QCycle> queue;


	/**
	 * The id of the link.
	 */
	private final String id;


	/**
	 * The total pseudolane distance (buffer distance) occupied of the cyclists currently on the link.
	 */
	private double occupiedSpace = 0;


	/**
	 * The array containing all pseudolanes of the link.
	 */
	private final PseudoLane[] psi;


	/**
	 * The total pseudolane distance, i.e. the product between the length and number of pseudolanes.
	 */
	private final double totalLaneLength; 

	/**
	 * The last time a vehicle was moved through the downstream end of the link.
	 */
	protected double generalLastTimeMoved;


	/**
	 * A LinkedList containing the vehicles which will be leaving the link. 
	 */
	protected final LinkedList<QVehicle> generalLeavingVehicles;


	
	
	public void addVehicleToGeneralLeavingVehiclesWithoutMovetype(final QVehicle veh){
		this.generalLeavingVehicles.addLast( veh); 
	}
	public abstract void addToLeavingVehicles(final QCycleAndMoveType veh,  double now);
	
	public void addVehicleToFrontOfLeavingVehiclesWithoutMovetype(final QVehicle veh, double now) {
		Gbl.assertIf(!(veh instanceof QCycleAndMoveType));
		if(this.generalLeavingVehicles.isEmpty()) {
			this.generalLastTimeMoved = now;
		}
		this.generalLeavingVehicles.addFirst(veh); 
	}
	
	public abstract void addVehicleToFrontOfLeavingVehicles(final QCycleAndMoveType veh, double now);
	
	public abstract double getLeftBufferLastTimeMoved();

	public QCycleAndMoveType getFirstGeneralLeavingVehicle() {
		return (QCycleAndMoveType) this.generalLeavingVehicles.getFirst();
	}
	
	public QVehicle getFirstGeneralLeavingVehicleWithoutMovetype() {
		return this.generalLeavingVehicles.getFirst();
	}
	public QVehicle pollFirstGeneralLeavingVehicle(double now) {
		this.generalLastTimeMoved = now;
		return  this.generalLeavingVehicles.pollFirst();
	}
	
	public abstract void setLastTimeMovedLeft(double lastMoved) ;
	
	public abstract QCycleAndMoveType getFirstLeftLeavingVehicle() ;
	
	public abstract QCycleAndMoveType pollFirstLeftLeavingVehicle(double now);
	
	public abstract boolean hasNoLeavingVehicles();
	public abstract boolean hasNoGeneralLeavingVehicles(double now);
	public abstract boolean hasNoLeftLeavingVehicles(double now);
	
	
	public abstract LinkedList<QVehicle> getAllLeavingVehicles();
	
	public PriorityQueue<QCycle> getQ(){
		return queue;
	}
	public void addToQ(QCycle qCyc){
		queue.add(qCyc);
	}


	/**
	 * Static factory method creating a link based on the width of the link. See also the {@link #Link(String, int, double) constructor}.
	 */
	//public static Sublink createLinkFromWidth(final String id, final double width, final double length, final FFFConfigGroup fffConfig){
	//	return new Sublink(id, 1 + (int) Math.floor((width-fffConfig.getUnusedWidth())/fffConfig.getEfficientLaneWidth()), length );
	//}

	/**
	 * Static factory method creating a link based directly on the number of pseudolanes of the link. See also the {@link #Link(String, int, double) constructor}.
	 */
	//public static Sublink createLinkFromNumberOfPseudoLanes(final String id, final int Psi, final double length){
	//	return new Sublink(id, Psi, length);
	//}

	/**
	 * @param id The id of the link.
	 * @param Psi The number of pseudolanes that the link has.
	 * @param length The length [m] of the link.
	 */
	protected Sublink(final String id, final int Psi, final double length){
		this.id = id;
		this.psi = createPseudoLanes(Psi, length);

		this.generalLeavingVehicles = new LinkedList<QVehicle>();

		double totalLaneLength = 0.;
		for(PseudoLane pseudoLane : psi){
			totalLaneLength += pseudoLane.getLength();
		}
		this.totalLaneLength = totalLaneLength;

		this.queue = new PriorityQueue<>( new Comparator<QCycle>(){
			@Override
			public int compare( QCycle qc1, QCycle qc2 ){
				return Double.compare(qc1.getEarliestLinkExitTime(), qc2.getEarliestLinkExitTime());
			}
		} ) ;


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


	public String getId(){
		return id;
	}


	public int getNumberOfPseudoLanes(){
		return psi.length;
	}


	/**
	 * Gets a specific <code>pseudolane</code> from the <code>link</code>.
	 * 
	 * @param i the index where 0 is the rightmost <code>pseudolane</code>, and <code>Psi</code> - 1 is the leftmost.
	 * 
	 * @return The i'th <code>pseudolane</code> from the right (0 being the rightmost).
	 */
	public PseudoLane getPseudoLane(final int i){
		return psi[i];
	}


	/**
	 * Configured such that the pseudoLane tReadys are automatically updated.
	 * 
	 * @return <code>true</code> iff the link is full, i.e. the occupied space is at least as large as the total lane length.
	 */
	public boolean isLinkFull(){
		if(occupiedSpace >= totalLaneLength){
			return true;
		} else {
			return false;
		}
	}


	/**
	 * Reduces the occupied space of link by the safety distance corresponding to <code>speed</code> of <code>cyclist</code>.
	 * 
	 * @param cyclist The cyclist which is removed from the link.
	 * @param speed The speed the cyclist was assigned to the link.
	 */
	public void reduceOccupiedSpace(final Cyclist cyclist, final double speed){
		this.supplementOccupiedSpace(-cyclist.getSafetyBufferDistance(speed));
	}

	public void reduceOccupiedSpaceByBicycleLength(final Cyclist cyclist){
		this.supplementOccupiedSpace(-cyclist.getBicycleLength());
	}

	/**
	 * Increases the occupied space of link by the safety distance corresponding to <code>speed</code> of <code>cyclist</code>.
	 * 
	 * @param cyclist The cyclist which enters the link.
	 * @param speed The speed the cyclist is assigned to the link.
	 */

	public void increaseOccupiedSpace(final Cyclist cyclist, final double speed){
		this.supplementOccupiedSpace(cyclist.getSafetyBufferDistance(speed));
	}
	public void increaseOccupiedSpaceByBicycleLength(final Cyclist cyclist){
		this.supplementOccupiedSpace(cyclist.getBicycleLength());
	}


	/**
	 * @param length The length [m] by which the occupied space will be supplemented.
	 */
	public void supplementOccupiedSpace(final double length){
		occupiedSpace += length;
	}



	public double getLastTimeMovedGeneral(){
		return this.generalLastTimeMoved;
	}

	public void setLastTimeMovedGeneral(final double lastTimeMoved){
		this.generalLastTimeMoved = lastTimeMoved;
	}


	/**
	 * Static factory method creating a link based directly on the number of pseudolanes of the link. See also the {@link #Link(String, int, double) constructor}.
	 */
	public static Sublink[] createLinkArrayFromNumberOfPseudoLanes(final String id, final int Psi, final double length, 
			final double L_MAX, int leftBufferCapacity){
		int N = (int) Math.ceil(length / L_MAX);
		Sublink[] linkArray = new Sublink[N];
		double subLinkLength = length/N;
		for(int i = 0; i < linkArray.length-1; i++){
			linkArray[i] = new SublinkWithSingleBuffer(id + "_part_" + (i+1) , Psi, subLinkLength);
		}
		if(leftBufferCapacity <= 0) {
			linkArray[linkArray.length-1] = new SublinkWithSingleBuffer(id + "_part_" + linkArray.length, Psi, subLinkLength);
		} else if(leftBufferCapacity < Integer.MAX_VALUE) {
			linkArray[linkArray.length-1] = new SublinkWithTwoInteractingBuffers(id + "_part_" + linkArray.length, Psi, subLinkLength, leftBufferCapacity);
		} else {
			linkArray[linkArray.length-1] = new SublinkWithTwoSeparatedBuffers(id + "_part_" + linkArray.length, Psi, subLinkLength);
		}
		return linkArray;
	}

	public double getMinimumNextMoveTime(){
		double minTime = psi[0].getTEnd();
		for(int i = 1; i < psi.length; i++){
			PseudoLane pseudoLane = psi[i];
			if( pseudoLane.getTEnd() < minTime){
				minTime = pseudoLane.getTEnd();
			}
		}
		return minTime;
	}
	
}
