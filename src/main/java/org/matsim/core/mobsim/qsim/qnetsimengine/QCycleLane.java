package org.matsim.core.mobsim.qsim.qnetsimengine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.qnetsimengine.QLaneI.VisData;
import org.matsim.core.mobsim.qsim.qnetsimengine.QLinkImpl.LaneFactory;
import org.matsim.lanes.Lane;
import org.matsim.vehicles.Vehicle;

import fastOrForcedToFollow.Cyclist;
import fastOrForcedToFollow.Sublink;
import fastOrForcedToFollow.configgroups.FFFConfigGroup;

public abstract class QCycleLane implements QLaneI {

	private static final Logger log = Logger.getLogger( QCycleLane.class ) ;

	protected final fastOrForcedToFollow.Sublink[] fffLinkArray;
	protected final AbstractQLink qLinkImpl;
	protected final NetsimEngineContext context;
	protected final int lastIndex; // To make some (often queried) methods faster.

	protected AtomicInteger numberOfCyclistsOnLink;

	protected QCycleLane( Sublink[] fffLinkArray, AbstractQLink qLinkImpl, NetsimEngineContext context){
		this.fffLinkArray = fffLinkArray; 
		this.qLinkImpl = qLinkImpl;
		this.context = context;
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
	
	protected abstract void doFFFStuff(Cyclist cyclist, Sublink fffLink);

	
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
	public abstract void placeVehicleAtFront(QVehicle veh);
		
}
