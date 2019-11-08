package org.matsim.core.mobsim.qsim.qnetsimengine;

import fastOrForcedToFollow.Cyclist;
import fastOrForcedToFollow.Sublink;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.lanes.Lane;
import org.matsim.vehicles.Vehicle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

class QCycleLaneWithSublinks implements QLaneI{
	private static final Logger log = Logger.getLogger( QCycleLaneWithSublinks.class ) ;

	private final fastOrForcedToFollow.Sublink[] fffLinkArray;
	private final AbstractQLink qLinkImpl;
	private final NetsimEngineContext context;
	private final double correctionFactor;

	private final int lastIndex; // To make some (often queried) methods faster.

	private AtomicInteger numberOfCyclistsOnLink;

	public QCycleLaneWithSublinks( Sublink[] fffLinkArray, AbstractQLink qLinkImpl, NetsimEngineContext context, double correctionFactor ){
		this.fffLinkArray = fffLinkArray; 
		this.qLinkImpl = qLinkImpl;
		this.context = context;

		this.correctionFactor = correctionFactor;
		this.lastIndex = fffLinkArray.length -1;
		this.numberOfCyclistsOnLink = new AtomicInteger(0);

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
		numberOfCyclistsOnLink.incrementAndGet();
		veh.setCurrentLink( qLinkImpl.getLink() );

		// upcast:
		QCycle qCyc = (QCycle) veh;

		// get the Cyclist out of it:
		Cyclist cyclist = qCyc.getCyclist();

		// internal fff logic:
		Sublink fffLink = fffLinkArray[0];


		doFFFStuff(cyclist, fffLink);

		// Add qCycle to the downstream queue of the next link.
		fffLinkArray[0].addToQ(qCyc);

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

		for(int i = this.lastIndex; i>=0; i--){
			Sublink currentFFFLink = fffLinkArray[i];
			QCycle qCyc;
			while((qCyc = currentFFFLink.getQ().peek()) != null){

				if( qCyc.getEarliestLinkExitTime() > context.getSimTimer().getTimeOfDay()){
					break;
				}

				Cyclist cyclist = qCyc.getCyclist();


				if(i < this.lastIndex){
					// internal fff logic:

					// Selecting the appropriate pseudoLane:
					Sublink receivingFFFLink = fffLinkArray[i+1];
					if(receivingFFFLink.isLinkFull()){
						break;
					}
					// qCyc can in fact leave current sublink
					currentFFFLink.getQ().remove();
					currentFFFLink.reduceOccupiedSpace(cyclist, cyclist.getSpeed() );			

					doFFFStuff(cyclist, receivingFFFLink);

					receivingFFFLink.addToQ(qCyc);

				} else { ///fffLink is last subLink

					currentFFFLink.getQ().remove();
					currentFFFLink.reduceOccupiedSpace(cyclist, cyclist.getSpeed() );

					if(qCyc.getDriver().isWantingToArriveOnCurrentLink()){
						qLinkImpl.letVehicleArrive(qCyc );
						numberOfCyclistsOnLink.decrementAndGet();
						continue;
					}


					//Auxiliary buffer created to fit the piece into MATSim.
					currentFFFLink.addVehicleToLeavingVehicles(qCyc );
					currentFFFLink.increaseOccupiedSpaceByBicycleLength(cyclist);
					currentFFFLink.setLastTimeMoved(qCyc.getEarliestLinkExitTime());


					final QNodeI toNode = qLinkImpl.getToNode();
					if ( toNode instanceof AbstractQNode ) {
						((AbstractQNode) toNode).activateNode();
					} 
				}
			}
		}
		return true;
	}

	private void doFFFStuff(Cyclist cyclist, Sublink fffLink) {
		cyclist.selectPseudoLaneAndUpdateSpeed( fffLink);

		//Only relevant when considering downscaled population
		//		double surplus = pseudoLane.getLength() / vTilde * (correctionFactor-1);
		//		pseudoLane.setTReady(tStart + tOneBicycleLength + surplus);
		//		pseudoLane.setTEnd(cyclist.getTEarliestExit() + tOneBicycleLength + surplus);
	}

	@Override public boolean isNotOfferingVehicle() {
		return fffLinkArray[lastIndex].hasNoLeavingVehicles();

	}

	@Override public QVehicle popFirstVehicle() {
		QVehicle qVeh = fffLinkArray[lastIndex].pollFirstLeavingVehicle();
		Cyclist cyclist = ((QCycle) qVeh).getCyclist();
		fffLinkArray[lastIndex].reduceOccupiedSpaceByBicycleLength(cyclist);
		numberOfCyclistsOnLink.decrementAndGet();
		return qVeh;
	}

	@Override public QVehicle getFirstVehicle() {
		return fffLinkArray[lastIndex].getFirstLeavingVehicle();
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
		Sublink lastSubLink = fffLinkArray[this.lastIndex];

		// Essentially just skipping this first link (in order to be consistent with scorin mechanism)
		numberOfCyclistsOnLink.incrementAndGet();
		lastSubLink.addVehicleToLeavingVehicles(qCyc );
		lastSubLink.increaseOccupiedSpaceByBicycleLength(qCyc.getCyclist());
		lastSubLink.setLastTimeMoved(now);
		qCyc.setEarliestLinkExitTime(now);
		final QNodeI toNode = qLinkImpl.getToNode();
		if ( toNode instanceof QNodeImpl ) {
			((QNodeImpl) toNode).activateNode();
		} else if( toNode instanceof QFFFNode ){
			((QFFFNode) toNode).activateNode();
		}
	}

	@Override public boolean isActive() {
		if(numberOfCyclistsOnLink.intValue() == 0){
			return false;
		} else {
			return true;
		}
	}

	public void addVehicleToFrontOfLeavingVehicles(final QVehicle veh){
		this.fffLinkArray[lastIndex].getLeavingVehicles().addFirst(veh);
	}


	@Override public double getSimulatedFlowCapacityPerTimeStep() {
		throw new RuntimeException( "not implemented" );
	}

	@Override public void recalcTimeVariantAttributes() {
		throw new RuntimeException( "not implemented" );
	}

	@Override public QVehicle getVehicle( final Id<Vehicle> vehicleId ) {
		for(Sublink sublink : fffLinkArray){
			for(QCycle cqo : sublink.getQ()){
				if( cqo.getVehicle().getId().equals( vehicleId) ){
					return cqo;
				}
			}
		}
		return null;
	}


	@Override public double getStorageCapacity() {
		throw new RuntimeException( "not implemented" );
	}

	@Override public VisData getVisData() {
		throw new RuntimeException( "not implemented" );
	}

	@Override public void addTransitSlightlyUpstreamOfStop( final QVehicle veh ) {
		//This is used instead of addFromWait, when the cyclist only has to move 1 link. 
		// Because of this, it doesn't get the setEarliestLinkExitTime (that it normally
		// gets from addFromWait), why it has to get it here.
		//	((QCycle)veh).setEarliestLinkExitTime(context.getSimTimer().getTimeOfDay()); // fulltransit
		//	this.addFromUpstream(veh);
		this.addFromWait(veh); // fulltransit2
	}

	@Override public void changeUnscaledFlowCapacityPerSecond( final double val ) {
		throw new RuntimeException( "not implemented" );
	}

	@Override public void changeEffectiveNumberOfLanes( final double val ) {
		throw new RuntimeException( "not implemented" );
	}


	@Override public void clearVehicles() {
		numberOfCyclistsOnLink.set(0);
		for(Sublink sublink : fffLinkArray){
			sublink.getQ().clear();
		}
		//			for(int i = 0; i < fffLinkArray.length; i++){
		//				Sublink sublink = fffLinkArray[i];
		//				while(!sublink.getQ().isEmpty()){
		//					QVehicle veh = sublink.getQ().poll();
		//					log.debug("When ending the simulation vehicle " + veh.getId() + 
		//							" was found at sublink " + i + " of link " +
		//							qLinkImpl.getLink().getId() + " with tEarliestExit =" +
		//							((QCycle) veh).getCyclist().getTEarliestExit() +
		//							". Length is " + qLinkImpl.getLink().getLength() +
		//							". Speed is " + ((QCycle) veh).getCyclist().getSpeed() +
		//							". Destination is " + veh.getDriver().getDestinationLinkId() +
		//							". Next link is + " + veh.getDriver().chooseNextLinkId() +
		//							". Current number of cyclists is " + numberOfCyclistsOnLink);
		//			}
		//		}
	}

	@Override public Collection<MobsimVehicle> getAllVehicles() {
		ArrayList<MobsimVehicle> qCycs = new ArrayList<MobsimVehicle>();
		for(Sublink sublink : fffLinkArray){
			for(QCycle cqo : sublink.getQ()){
				qCycs.add(cqo);
			}
		}
		return qCycs;
	}

	@Override public double getLastMovementTimeOfFirstVehicle() {
		return fffLinkArray[this.lastIndex].getLastTimeMoved();
	}

	@Override public double getLoadIndicator() {
		throw new RuntimeException( "not implemented" );
	}

	@Override public void initBeforeSimStep() {
		//Intentionally empty
	}


	/**
	 * Used by left turning cyclists who will be skipping the queue when making a stepwise left turn.
	 */
	public void placeVehicleAtFront(QVehicle veh){
		activateLink(); // Activates the new link
		QCycle qCyc = (QCycle) veh; // Upcast
		numberOfCyclistsOnLink.incrementAndGet();
		this.fffLinkArray[this.lastIndex].addVehicleToFrontOfLeavingVehicles(qCyc);
		this.fffLinkArray[this.lastIndex].increaseOccupiedSpaceByBicycleLength(qCyc.getCyclist());
	}

}
