package org.matsim.core.mobsim.qsim.qnetsimengine;

import fastOrForcedToFollow.Cyclist;
import fastOrForcedToFollow.Sublink;
import fastOrForcedToFollow.PseudoLane;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.lanes.Lane;
import org.matsim.vehicles.Vehicle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.PriorityQueue;

class QCycleLaneWithSublinks implements QLaneI{
	private static final Logger log = Logger.getLogger( QCycleLaneWithSublinks.class ) ;

	private final fastOrForcedToFollow.Sublink[] fffLinkArray;
	private final AbstractQLink qLinkImpl;
	private final NetsimEngineContext context;
	private final PriorityQueue<QCycle> globalQ;
	private final double correctionFactor;
	
	public QCycleLaneWithSublinks( Sublink[] fffLinkArray, AbstractQLink qLinkImpl, NetsimEngineContext context, double correctionFactor ){
		this.fffLinkArray = fffLinkArray; 
		this.qLinkImpl = qLinkImpl;
		this.context = context;
		this.globalQ = new PriorityQueue<>( new Comparator<QCycle>(){
			@Override
			public int compare( QCycle qc1, QCycle qc2 ){
				return Double.compare(qc1.getCyclist().getTEarliestExit(), qc2.getCyclist().getTEarliestExit());
			}
		} ) ;
		
		this.correctionFactor = correctionFactor;
		
	}

	public void activateLink(){
		qLinkImpl.getInternalInterface().activateLink();
	}
 
	@Override public Id<Lane> getId() { //Done!?
		//TODO There must be a better way... But it is even necessary. 

		return Id.create(fffLinkArray[0].getId().substring(0, fffLinkArray[0].getId().toString().length() - 6), Lane.class);
	}

	@Override public boolean isAcceptingFromUpstream() { //Done!
		return !fffLinkArray[0].isLinkFull();
	}

	@Override public void addFromUpstream( final QVehicle veh ) {  
		// activate link since there is now action on it:
		activateLink();
			
		veh.setCurrentLink( qLinkImpl.getLink() );

		// upcast:
		QCycle qCyc = (QCycle) veh;

		// get the Cyclist out of it:
		Cyclist cyclist = qCyc.getCyclist();

		// internal fff logic:
		Sublink fffLink = fffLinkArray[0];

		// Selecting the appropriate pseudoLane:
		PseudoLane pseudoLane = cyclist.selectPseudoLane( fffLink);

		// Assigning a provisional, maximum speed for this link:
		double vTilde = cyclist.getVMax(pseudoLane);
		vTilde = Math.min(cyclist.getDesiredSpeed(), vTilde);
		cyclist.setSpeed(vTilde);

		//					 printDelay(cyclist);

		// The time at which the tip of the cyclist enters the beginning of the link:
		double tStart = Double.max(pseudoLane.getTReady(), cyclist.getTEarliestExit()) ;

		// Calculating earliest possible exit of the link:
		final double tEarliestExit = tStart + pseudoLane.getLength() / vTilde;
		cyclist.setTEarliestExit( tEarliestExit );

		// Increasing the occupied space on link:
		fffLink.increaseOccupiedSpace(cyclist, vTilde );

		// Updating tReady and tExit of the link:
		double tOneBicycleLength = cyclist.getBicycleLength() / vTilde;
		double surplus = pseudoLane.getLength() / vTilde * (correctionFactor-1);
		pseudoLane.setTReady(tStart + tOneBicycleLength + surplus);
		pseudoLane.setTEnd(cyclist.getTEarliestExit() + tOneBicycleLength + surplus);
		

		// Add qCycle to the downstream queue of the next link.
		//	fffLinkArray[0].getOutQ().add(qCyc ); 
		qCyc.getCyclist().resetCurrentLinkIndex();
		globalQ.add(qCyc);

	}


	/**
	 * Auxiliary method that can be used for logging/printing cyclist delays on individual links.
	 * @param vTilde
	 * @param cyclist
	 */
	//    private void printDelay(Cyclist cyclist) {
	//    	double vTilde = cyclist.getSpeed();
	//        if(vTilde + 0.00001 < cyclist.getDesiredSpeed()){
	//            log.info("Cyclist "+ cyclist.getId() + " riding " + String.format("%.1f",
	//                     (cyclist.getDesiredSpeed() - vTilde)/cyclist.getDesiredSpeed()*100d )
	//                     + "% slower on link " + fffLink.getId() );
	//        }
	//    }

	@Override public boolean doSimStep() {
		QCycle cqo;

		while((cqo = globalQ.peek()) != null){

			double tEarliestExit = cqo.getEarliestLinkExitTime();
			if( tEarliestExit > context.getSimTimer().getTimeOfDay()){
				break;
			}


			Sublink fffLink = fffLinkArray[cqo.getCyclist().getCurrentLinkIndex()];

			//	fffLink.getOutQ().remove();
			globalQ.remove();
			fffLink.reduceOccupiedSpace(cqo.getCyclist(), cqo.getCyclist().getSpeed() );
			
			//Anything but the last subLink
			if(cqo.getCyclist().getCurrentLinkIndex() < fffLinkArray.length -1){

				Cyclist cyclist = cqo.getCyclist();
				// internal fff logic:

				// Selecting the appropriate pseudoLane:
				//Make sure that this cannot happen for cet currentLinkIdex = maxIndex.
				Sublink receivingFFFLink = fffLinkArray[cqo.getCyclist().getCurrentLinkIndex() + 1];
				PseudoLane pseudoLane = cyclist.selectPseudoLane( receivingFFFLink );

				// Assigning a provisional, maximum speed for this link:
				double vTilde = cyclist.getVMax(pseudoLane);
				vTilde = Math.min(cyclist.getDesiredSpeed(), vTilde);
				cyclist.setSpeed(vTilde);

				// The time at which the tip of the cyclist enters the beginning of the link:
				double tStart = Double.max(pseudoLane.getTReady(), cyclist.getTEarliestExit()) ;

				// Calculating earliest possible exit of the link:
				tEarliestExit = tStart + pseudoLane.getLength() / vTilde;
				cyclist.setTEarliestExit( tEarliestExit );

				// Increasing the occupied space on link:
				receivingFFFLink.increaseOccupiedSpace(cyclist, vTilde );

				// Updating tReady and tExit of the link:
				double tOneBicycleLength = cyclist.getBicycleLength() / vTilde;
				double surplus = pseudoLane.getLength() / vTilde * (correctionFactor-1);
				pseudoLane.setTReady(tStart + tOneBicycleLength + surplus);
				pseudoLane.setTEnd(cyclist.getTEarliestExit() + tOneBicycleLength + surplus);
			
				// Add qCycle to the downstream queue of the next link.
				//	receivingFFFLink.getOutQ().add(cqo ); 
				cqo.getCyclist().incrementCurrentLinkIndex();
				globalQ.add(cqo);


			} else { ///fffLink is last subLink

				if(cqo.getDriver().isWantingToArriveOnCurrentLink()){
					qLinkImpl.letVehicleArrive(cqo );
					continue;
				}


				//Auxiliary buffer created to fit the piece into MATSim.
				fffLink.addVehicleToLeavingVehicles(cqo );
				fffLink.setLastTimeMoved(tEarliestExit);


				final QNodeI toNode = qLinkImpl.getToNode();
				if ( toNode instanceof QNodeImpl ) {
					((QNodeImpl) toNode).activateNode();
				} else if ( toNode instanceof QFFFNode){ //mads: Added QFFFNode here...
					((QFFFNode) toNode).activateNode();
				}
			}
		}
		return true;
	}

	@Override public boolean isNotOfferingVehicle() {
		return fffLinkArray[fffLinkArray.length-1].hasNoLeavingVehicles();

	}

	@Override public QVehicle popFirstVehicle() {
		return fffLinkArray[fffLinkArray.length-1].pollFirstLeavingVehicle();
	}

	@Override public QVehicle getFirstVehicle() {
		return fffLinkArray[fffLinkArray.length-1].getFirstLeavingVehicle();
	}

	@Override public boolean isAcceptingFromWait( final QVehicle veh ) {
		// use same logic as inserting from upstream:
		return this.isAcceptingFromUpstream() ;

	}

	@Override public void addFromWait( final QVehicle veh ) {

		// ensuring that the first provisional earliest link exit cannot be before now.
		double now = context.getSimTimer().getTimeOfDay() ;
		QCycle qCyc = (QCycle) veh;
		
		if(qCyc.getDriver().isWantingToArriveOnCurrentLink()){
			qLinkImpl.letVehicleArrive(qCyc);
			return;
		}
		Sublink lastSubLink = fffLinkArray[ fffLinkArray.length -1];
		
		// Essentially just skipping this first link (in order to be consistent with scorin mechanism)
		lastSubLink.addVehicleToLeavingVehicles(qCyc );
		lastSubLink.setLastTimeMoved(now);
		qCyc.getCyclist().setTEarliestExit( now );

		final QNodeI toNode = qLinkImpl.getToNode();
		if ( toNode instanceof QNodeImpl ) {
			((QNodeImpl) toNode).activateNode();
		} else if ( toNode instanceof QFFFNode){ //mads: Added QFFFNode here...			
			((QFFFNode) toNode).activateNode();		
		}
	}

	@Override public boolean isActive() {
		if(!globalQ.isEmpty()){
			return true;
		} else {
			return false;
		}
	}

	public void addVehicleToFrontOfLeavingVehicles(final QVehicle veh){
		this.fffLinkArray[this.fffLinkArray.length - 1].getLeavingVehicles().addFirst(veh);
	}

	
	@Override public double getSimulatedFlowCapacityPerTimeStep() {
		throw new RuntimeException( "not implemented" );
	}

	@Override public void recalcTimeVariantAttributes() {
		throw new RuntimeException( "not implemented" );
	}

	@Override public QVehicle getVehicle( final Id<Vehicle> vehicleId ) {
		for(QCycle cqo : globalQ){
			if( cqo.getVehicle().getId().equals( vehicleId.toString() ) ){
				return cqo;
			}
		}
		return null ;
	}

	@Override public double getStorageCapacity() {
		throw new RuntimeException( "not implemented" );
	}

	@Override public VisData getVisData() {
		throw new RuntimeException( "not implemented" );
	}

	@Override public void addTransitSlightlyUpstreamOfStop( final QVehicle veh ) {
		this.addFromUpstream(veh);
		//Not sure about that
	}

	@Override public void changeUnscaledFlowCapacityPerSecond( final double val ) {
		throw new RuntimeException( "not implemented" );
	}

	@Override public void changeEffectiveNumberOfLanes( final double val ) {
		throw new RuntimeException( "not implemented" );
	}


	@Override public void clearVehicles() {
		globalQ.clear();
	}

	@Override public Collection<MobsimVehicle> getAllVehicles() {
		ArrayList<MobsimVehicle> qCycs = new ArrayList<MobsimVehicle>();
		for(QCycle cqo : globalQ){
			qCycs.add(cqo);
		}
		return qCycs;
	}

	@Override public double getLastMovementTimeOfFirstVehicle() {
		return fffLinkArray[fffLinkArray.length-1].getLastTimeMoved();
	}

	@Override public double getLoadIndicator() {
		throw new RuntimeException( "not implemented" );
	}

	@Override public void initBeforeSimStep() {
		//Intentionally empty
	}
	
	
	/**
	 * Used by left turning cyclists who will be skipping the queue when making a two-phase left turn.
	 */
	public void placeVehicleAtFront(QVehicle veh){
		QCycle qCyc = (QCycle) veh;
		qCyc.getCyclist().setCurrentLinkIndex(fffLinkArray.length -1);
		globalQ.add(qCyc);
	}
}
