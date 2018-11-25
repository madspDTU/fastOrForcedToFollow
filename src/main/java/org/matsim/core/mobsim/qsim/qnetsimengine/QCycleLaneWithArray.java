package org.matsim.core.mobsim.qsim.qnetsimengine;

import fastOrForcedToFollow.Cyclist;
import fastOrForcedToFollow.Link;
import fastOrForcedToFollow.PseudoLane;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.lanes.Lane;
import org.matsim.vehicles.Vehicle;

import java.util.ArrayList;
import java.util.Collection;

class QCycleLaneWithArray implements QLaneI{
	private static final Logger log = Logger.getLogger( QCycleLaneWithArray.class ) ;

	private final fastOrForcedToFollow.Link[] fffLinkArray;
	private final AbstractQLink qLinkImpl;
	private final NetsimEngineContext context;

	public QCycleLaneWithArray( Link[] fffLinkArray, AbstractQLink qLinkImpl, NetsimEngineContext context ){
		this.fffLinkArray = fffLinkArray; 
		this.qLinkImpl = qLinkImpl;
		this.context = context;
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
		qLinkImpl.getInternalInterface().activateLink();

		veh.setCurrentLink( qLinkImpl.getLink() );

		// upcast:
		QCycle qCyc = (QCycle) veh;

		// get the Cyclist out of it:
		Cyclist cyclist = qCyc.getCyclist();

		// internal fff logic:

		// Selecting the appropriate pseudoLane:
		PseudoLane pseudoLane = cyclist.selectPseudoLane( fffLinkArray[0] );

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
		fffLinkArray[0].increaseOccupiedSpace(cyclist, vTilde );

		// Updating tReady and tExit of the link:
		double tOneBicycleLength = cyclist.getBicycleLength() / vTilde;
		pseudoLane.setTReady(tStart + tOneBicycleLength);
		pseudoLane.setTEnd(cyclist.getTEarliestExit() + tOneBicycleLength);


		// Add qCycle to the downstream queue of the next link.
		fffLinkArray[0].getOutQ().add(qCyc ); 

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


		for(int i = 0; i < fffLinkArray.length -1; i++){
			Link fffLink = fffLinkArray[i];

			while((cqo = fffLink.getOutQ().peek()) != null){


				double tEarliestExit = cqo.getEarliestLinkExitTime();
				if( tEarliestExit > context.getSimTimer().getTimeOfDay()){
					break;
				}

				fffLink.getOutQ().remove();
				fffLink.reduceOccupiedSpace(cqo.getCyclist(), cqo.getCyclist().getSpeed() );


				Cyclist cyclist = cqo.getCyclist();
				// internal fff logic:

				// Selecting the appropriate pseudoLane:
				Link receivingFFFLink = fffLinkArray[i+1];
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
				pseudoLane.setTReady(tStart + tOneBicycleLength);
				pseudoLane.setTEnd(cyclist.getTEarliestExit() + tOneBicycleLength);


				// Add qCycle to the downstream queue of the next link.

				receivingFFFLink.getOutQ().add(cqo ); 

			}
		}

		//Finally handling last link (pushing on to external "MATSim territory");
		Link lastFFFLink = fffLinkArray[fffLinkArray.length -1];
		while((cqo = lastFFFLink.getOutQ().peek()) != null){

			//								log.debug( "now=" + context.getSimTimer().getTimeOfDay() + "; linkId=" + fffLink.getId() + outqAsString( fffLink.getOutQ() ) );

			final double tEarliestExit = cqo.getEarliestLinkExitTime();
			if( tEarliestExit > context.getSimTimer().getTimeOfDay()){
				break;
			}
			lastFFFLink.getOutQ().remove();
			lastFFFLink.reduceOccupiedSpace(cqo.getCyclist(), cqo.getCyclist().getSpeed() );

			if(cqo.getDriver().isWantingToArriveOnCurrentLink()){
				qLinkImpl.letVehicleArrive(cqo );
				continue;
			}

			//Auxiliary buffer created to fit the piece into MATSim.
			lastFFFLink.addVehicleToLeavingVehicles(cqo );
			lastFFFLink.setLastTimeMoved(tEarliestExit);


			final QNodeI toNode = qLinkImpl.getToNode();
			if ( toNode instanceof QNodeImpl ) {
				((QNodeImpl) toNode).activateNode();
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
		((QCycle) veh).getCyclist().setTEarliestExit( now );

		// just inserting them upstream.  For the time being, but might also be ok in the long run.
		this.addFromUpstream( veh );
	}

	@Override public boolean isActive() {
		for(Link fffLink : fffLinkArray){
			if(!fffLink.getOutQ().isEmpty()){
				return true;
			}
		}
		return false;
	}

	@Override public double getSimulatedFlowCapacityPerTimeStep() {
		throw new RuntimeException( "not implemented" );
	}

	@Override public void recalcTimeVariantAttributes() {
		throw new RuntimeException( "not implemented" );
	}

	@Override public QVehicle getVehicle( final Id<Vehicle> vehicleId ) {
		for(Link fffLink : fffLinkArray){
			for(QCycle cqo : fffLink.getOutQ()){
				if( cqo.getVehicle().getId().equals( vehicleId.toString() ) ){
					return cqo;
				}
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
		for(Link fffLink : fffLinkArray){
			fffLink.getOutQ().clear();
		}
	}

	@Override public Collection<MobsimVehicle> getAllVehicles() {
		ArrayList<MobsimVehicle> qCycs = new ArrayList<MobsimVehicle>();
		for(Link fffLink : fffLinkArray){
			for(QCycle cqo : fffLink.getOutQ()){
				qCycs.add(cqo);
			}
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
}
