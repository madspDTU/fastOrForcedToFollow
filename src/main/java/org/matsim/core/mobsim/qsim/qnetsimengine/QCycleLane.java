package org.matsim.core.mobsim.qsim.qnetsimengine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.VehicleAbortsEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.qnetsimengine.AbstractQLink.QLinkInternalInterface;
import org.matsim.core.mobsim.qsim.qnetsimengine.QFFFNode.MoveType;
import org.matsim.core.mobsim.qsim.qnetsimengine.QLaneI.VisData;
import org.matsim.core.mobsim.qsim.qnetsimengine.QLinkImpl.LaneFactory;
import org.matsim.lanes.Lane;
import org.matsim.vehicles.Vehicle;

import fastOrForcedToFollow.Cyclist;
import fastOrForcedToFollow.Sublink;
import fastOrForcedToFollow.SublinkWithTwoInteractingBuffers;
import fastOrForcedToFollow.configgroups.FFFConfigGroup;

public abstract class QCycleLane implements QLaneI {

	private static final Logger log = Logger.getLogger( QCycleLane.class ) ;

	protected final fastOrForcedToFollow.Sublink[] fffLinkArray;
	protected final AbstractQLink qLinkImpl;
	protected final NetsimEngineContext context;
	protected final int lastIndex; // To make some (often queried) methods faster.
	protected final boolean usingRoW;

	protected AtomicInteger numberOfCyclistsOnLink;

	
	private QFFFNodeWithLeftBuffer getToNodeQ() {
		return (QFFFNodeWithLeftBuffer) ((QFFFNode) qLinkImpl.getInternalInterface().getToNodeQ()).getQFFFAbstractNode(); 
	}
	
	protected QCycleLane( Sublink[] fffLinkArray, AbstractQLink qLinkImpl, NetsimEngineContext context){
		this.fffLinkArray = fffLinkArray; 
		this.qLinkImpl = qLinkImpl;
		this.context = context;
		this.lastIndex = fffLinkArray.length -1;
		this.numberOfCyclistsOnLink = new AtomicInteger(0);
		if(qLinkImpl.getInternalInterface().getToNodeQ() instanceof QFFFNode) {
			this.usingRoW = true;
		} else {
			this.usingRoW = false;
		}
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
		QCycle qCyc;
		if(veh instanceof QCycleAndMoveType) {
			qCyc = (QCycle) ((QCycleAndMoveType) veh).getQCycle();
		} else {
			qCyc = (QCycle) veh;
		}
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
	
	public boolean isNotOfferingGeneralVehicle() {
		double now = context.getSimTimer().getTimeOfDay();
		return fffLinkArray[lastIndex].hasNoGeneralLeavingVehicles(now);
	}
	
	public boolean isNotOfferingLeftVehicle() {
		double now = context.getSimTimer().getTimeOfDay();
		return fffLinkArray[lastIndex].hasNoLeftLeavingVehicles(now);
	}

	public QVehicle popFirstGeneralVehicle() {
		return (QCycleAndMoveType) popFirstGeneralVehicle();
	}
	
	//Ensuring that it works when using the normal node
	@Override
	public QVehicle popFirstVehicle() {
		double now = this.context.getSimTimer().getTimeOfDay();
		Sublink lastSubLink = fffLinkArray[lastIndex];
		QVehicle qVeh = lastSubLink.pollFirstGeneralLeavingVehicle(now);
		Cyclist cyclist;
		if(qVeh instanceof QCycleAndMoveType) {
			cyclist = ((QCycleAndMoveType)  qVeh).getCyclist();
		} else {
			cyclist = ((QCycle) qVeh).getCyclist();
		}
		fffLinkArray[lastIndex].reduceOccupiedSpaceByBicycleLength(cyclist);
		numberOfCyclistsOnLink.decrementAndGet();
		return qVeh;
	}
	
	//Ensuring that it works when using the normal nodes
	@Override
	public double getLastMovementTimeOfFirstVehicle() {
		return this.getLastSublink().getLastTimeMovedGeneral();
	}

	

	public QVehicle popFirstLeftVehicle() {
		double now = this.context.getSimTimer().getTimeOfDay();
		Sublink lastSubLink = fffLinkArray[lastIndex];
		QVehicle qVeh = lastSubLink.pollFirstLeftLeavingVehicle(now);
		Cyclist cyclist;
		if(qVeh instanceof QCycleAndMoveType) {
			cyclist = ((QCycleAndMoveType) qVeh).getCyclist();
		} else {
			cyclist = ((QCycle) qVeh).getCyclist();
		}
		fffLinkArray[lastIndex].reduceOccupiedSpaceByBicycleLength(cyclist);
		numberOfCyclistsOnLink.decrementAndGet();
		return qVeh;
	}



	@Override public QVehicle getFirstVehicle() {
		if(usingRoW) {
			System.err.println("GetFirstVehicle() is ambiguous... Returning null");
			return null;
		} else {
			return fffLinkArray[lastIndex].getFirstGeneralLeavingVehicle();
		}
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
		double now = this.context.getSimTimer().getTimeOfDay();
		if(usingRoW) {
			QCycle qCyc = ((QCycleAndMoveType) veh).getQCycle();
			QLinkInternalInterface qLinkII = this.qLinkImpl.getInternalInterface();
			int outDirection = determineOutDirection(veh);
			MoveType mt = MoveType.GENERAL;
			if(isLeftTurn(qLinkII, outDirection)) {
				mt = MoveType.LEFT_TURN;
			}
			this.fffLinkArray[lastIndex].addVehicleToFrontOfLeavingVehicles(new QCycleAndMoveType(qCyc, mt, outDirection),  now);
		} else {
			this.fffLinkArray[lastIndex].addVehicleToFrontOfLeavingVehiclesWithoutMovetype(veh, now);
		}

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
		double now = context.getSimTimer().getTimeOfDay();
		for(Sublink sublink : fffLinkArray){
			for (QVehicle veh : sublink.getQ()) {
				context.getEventsManager().processEvent( new VehicleAbortsEvent(now, veh.getId(), veh.getCurrentLink().getId()));
				context.getEventsManager().processEvent( new PersonStuckEvent(now, veh.getDriver().getId(), veh.getCurrentLink().getId(), veh.getDriver().getMode()));

				context.getAgentCounter().incLost();
				context.getAgentCounter().decLiving();
			}
			sublink.getQ().clear();

			for(QVehicle veh : sublink.getAllLeavingVehicles()){
				context.getEventsManager().processEvent( new VehicleAbortsEvent(now, veh.getId(), veh.getCurrentLink().getId()));
				context.getEventsManager().processEvent( new PersonStuckEvent(now, veh.getDriver().getId(), veh.getCurrentLink().getId(), veh.getDriver().getMode()));

				context.getAgentCounter().incLost();
				context.getAgentCounter().decLiving();
			}
			sublink.getAllLeavingVehicles().clear();
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
		double now = this.context.getSimTimer().getTimeOfDay();
		QCycle qCyc;
		if(veh instanceof QCycleAndMoveType) {
			qCyc = (QCycle) ((QCycleAndMoveType) veh).getQCycle();
		} else {
			 qCyc = (QCycle) veh; // Upcast
		}
		numberOfCyclistsOnLink.incrementAndGet();
		if(usingRoW) {
			QLinkInternalInterface qLinkII = this.qLinkImpl.getInternalInterface();
			int outDirection = determineOutDirection(veh);
			MoveType mt = MoveType.GENERAL;
			if(isLeftTurn(qLinkII, outDirection)) {
				mt = MoveType.LEFT_TURN;
			}
			this.fffLinkArray[this.lastIndex].addVehicleToFrontOfLeavingVehicles(new QCycleAndMoveType(qCyc, mt, outDirection), now);
		} else{
			this.fffLinkArray[this.lastIndex].addVehicleToFrontOfLeavingVehiclesWithoutMovetype(qCyc, now);
		}
		this.fffLinkArray[this.lastIndex].increaseOccupiedSpaceByBicycleLength(qCyc.getCyclist());
	}

	private Sublink getLastSublink() {
		return this.fffLinkArray[this.lastIndex];
	}


	protected boolean isLeftTurn(QLinkInternalInterface qLinkII, int outDirection) {
		return usingRoW && getToNodeQ().isBicycleLeftTurn(qLinkII.getId(), outDirection);
	}


	protected int determineOutDirection(QVehicle veh) {
		Id<Link> nextLinkId = veh.getDriver().chooseNextLinkId();
		return getToNodeQ().bicycleOutTransformations.get(nextLinkId);
	}

	public QCycleAndMoveType getFirstLeftLeavingVehicle() {
		return getLastSublink().getFirstLeftLeavingVehicle();
	}

	public QCycleAndMoveType getFirstGeneralLeavingVehicle() {
		return getLastSublink().getFirstGeneralLeavingVehicle();
	}
	
	public boolean hasInteractingBuffers() {
		return getLastSublink() instanceof SublinkWithTwoInteractingBuffers;
	}

}
