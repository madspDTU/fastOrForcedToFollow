package fastOrForcedToFollow;


import java.util.LinkedList;
import java.util.PriorityQueue;

import org.matsim.core.mobsim.qsim.qnetsimengine.QCycleAsVehicle;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;

/**
 * 
 * @author mpaulsen
 */
public class Link{


	/**
	 * The id of the link.
	 */
	private String id;

	/**
	 * The length of the link.
	 */
	private double length;

	/**
	 * The total pseudolane distance (buffer distance) occupied of the cyclists currently on the link.
	 */
	private double occupiedSpace = 0;


	/**
	 * The Q containing the CQO's for the cyclists that have entered the link but not yet left it.
	 */
	private PriorityQueue<CyclistQObject> outQ;

	/**
	 * The array containing all <code>Psi</code> pseudolanes of the link.
	 */
	private PseudoLane[] psi;

	/**
	 * The number of <code>pseudolanes</code> the link has.
	 */
	private int Psi;

	/**
	 * The total pseudolane distance, i.e. the product between the length and number of pseudolanes.
	 */
	private final double totalLaneLength; 

	/**
	 * The earliest possible time that the link can potentially handle traffic (entering or leaving).
	 */
	private double tWakeUp = 0;
	
	
	//mads: Buffer created in order to fit the piece into MATSim. Would be nice to do without.
	private LinkedList<QVehicle> vehiclesMovedDownstream = new LinkedList<QVehicle>();
	public void addVehicleToMovedDownstreamVehicles(QVehicle veh){
		this.vehiclesMovedDownstream.addLast(veh);
	}
	public QVehicle getFirstVehicleMovedDownstream(){
		return vehiclesMovedDownstream.isEmpty() ? null : vehiclesMovedDownstream.peekFirst();
	}
	public QVehicle pollFirstVehicleMovedDownstream(){
		return vehiclesMovedDownstream.isEmpty() ? null : vehiclesMovedDownstream.pollFirst();
	}
	public boolean isVehiclesMovedDownstreamEmpty(){
		return vehiclesMovedDownstream.isEmpty();
	}
	

	public Link(String id, double width, double length) throws InstantiationException, IllegalAccessException{
		this(id, 1 + (int) Math.floor((width-Runner.deadSpace)/Runner.omega), length );
	}
	
	//Constructor using the number of pseudoLanes directly.
	public Link(String id, int Psi, double length) throws InstantiationException, IllegalAccessException{
		this.id = id;
		this.length = length;
		this.Psi = Psi;
		psi = createPseudoLanes();
		outQ = new PriorityQueue<CyclistQObject>();
		this.totalLaneLength = this.length * this.Psi * Runner.capacityMultiplier;
	}

	/**
	 * @return The array of PseudoLanes to be created
	 */
	private PseudoLane[] createPseudoLanes(){
		PseudoLane[] psi = new PseudoLane[Psi];
		for(int i = 0; i < Psi; i++){
			psi[i] = new PseudoLane(length, this);
		}
		return psi;
	}

	
	
	/**
	 * @return The integer id of the link.
	 */
	public String getId(){
		return id;
	}

	public double getLength(){
		return length;
	}

	/**
	 * @return The number of <code>pseudolanes</code>.
	 */
	public int getNumberOfPseudoLanes(){
		return Psi;
	}

	/**
	 * @return The occupied space (sum of safetydistances of <code>cyclists</code> currently on the <code>link</code>.
	 */
	public double getOccupiedSpace(){
		return occupiedSpace;
	}

	/** 
	 * @return The outQ belonging to the link.
	 */
	public PriorityQueue<CyclistQObject> getOutQ(){
		return outQ;
	}

	/**
	 * Gets a <code>pseudolane</code> from the <code>link</code>.
	 * 
	 * @param i the index where 0 is the rightmost <code>pseudolane</code>, and <code>Psi</code> - 1 is the leftmost.
	 * 
	 * @return The i'th <code>pseudolane</code> from the right (0 being the rightmost).
	 */
	public PseudoLane getPseudoLane(int i){
		return psi[i];
	}
	/**
	 * @return The total lane length, defined as the product of the length and the number of pseudolanes.
	 */
	public double getTotalLaneLength(){
		return totalLaneLength;
	}

	/**
	 * @return The earliest possible time that the link can potentially handle traffic (entering or leaving).
	 */
	public double getWakeUpTime(){
		return tWakeUp;
	}



	public void finishCyclist(QCycleAsVehicle qCyc){
		Cyclist cyclist = qCyc.getCyclist();
		Link previousLink = cyclist.getCurrentLink();
		previousLink.outQ.remove();
		previousLink.reduceOccupiedSpace(cyclist, cyclist.getSpeed());
		cyclist.setCurrentLink(null);
	}

	public CyclistQObject advanceCyclist(QCycleAsVehicle qCyc){
		Cyclist cyclist = qCyc.getCyclist();
		PseudoLane pseudoLane = cyclist.selectPseudoLane(this); 
		double vTilde = cyclist.getVMax(pseudoLane);
		
		// The cyclist may not fit, but it is alright that 1 cyclist exceeds the capacity.
		
		//if(cyclist.speedFitsOnLink(vTilde, this)){
			
			double tLeave = Double.max(pseudoLane.tReady, cyclist.getTEarliestExit());

			Link previousLink = cyclist.getCurrentLink();
			if(previousLink != null){
				
				previousLink.getOutQ().remove();
				previousLink.reduceOccupiedSpace( cyclist, cyclist.getSpeed());
				previousLink.setWakeUpTime(tLeave);
			}


			cyclist.setSpeed(vTilde);
			cyclist.setTStart(tLeave);
			cyclist.setTEarliestExit(tLeave + this.length/vTilde);
			this.increaseOccupiedSpace(cyclist, vTilde);
			pseudoLane.updateTs(vTilde, tLeave);
			cyclist.setCurrentLink(this);	

			return new CyclistQObject(qCyc);
	}



	/**
	 * Reduces the occupied space of link <code>linkId</code> by the safety distance corresponding to <code>speed</code>
	 * 
	 * @param linkId of the link that will have its space reduced.
	 * 
	 * @param speed on which the safety distance will be based.
	 */
	public void reduceOccupiedSpace(Cyclist cyclist, double speed){
		this.supplementOccupiedSpace(-cyclist.getSafetyBufferDistance(speed));
	}

	public void increaseOccupiedSpace(Cyclist cyclist, double speed){
		this.supplementOccupiedSpace(cyclist.getSafetyBufferDistance(speed));
	}




	public boolean isRelevant(){
		return !outQ.isEmpty() && Runner.t >= outQ.peek().getCyclist().getTEarliestExit();
	}



	/**
	 * @param tWakeUp The earliest possible time that the link can potentially handle traffic (entering or leaving).
	 */
	public void setWakeUpTime(double tWakeUp){
		this.tWakeUp = tWakeUp;
	}

	/**
	 * @param length in metres by which the occupied space will be supplemented.
	 */
	public void supplementOccupiedSpace(double length){
		occupiedSpace += length;
	}

	
	//No need to keep the non-MATSim functions anymore... 
/*
	public void processLink(double now){
		boolean linkFullyProcessed = false;
		while(!linkFullyProcessed){
			if(this.getOutQ().isEmpty()){
				linkFullyProcessed = true;
			} else {
				QCycleAsVehicle cqo = this.getOutQ().peek().getQCycle();
				Cyclist cyclist = cqo.getCyclist();
				if(cyclist.getTEarliestExit() > now){
					linkFullyProcessed = true;
				} else {
					if(cyclist.getRoute().isEmpty()){
						this.finishCyclist(cqo);
					} else {
						Link nextLink = cyclist.getRoute().peek();
						if(nextLink.cyclistFitsOnLink(cyclist)){
							this.advanceCyclist(cqo);	
						} else {
							cyclist.setTEarliestExit(nextLink.getOutQ().peek().getCyclist().getTEarliestExit());
							linkFullyProcessed = true;
						}
					}
				} 
			} 
		}
	}
	*/


	public boolean cyclistFitsOnLink(Cyclist cyclist){
		PseudoLane pseudoLane = cyclist.selectPseudoLane(this);
		double vTilde = cyclist.getVMax(pseudoLane);
		return cyclist.speedFitsOnLink(vTilde, this);
	}


}
