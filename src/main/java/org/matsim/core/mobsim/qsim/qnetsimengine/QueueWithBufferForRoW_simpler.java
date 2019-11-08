/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.core.mobsim.qsim.qnetsimengine;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.VehicleAbortsEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.LaneEnterEvent;
import org.matsim.core.api.experimental.events.LaneLeaveEvent;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup.LinkDynamics;
import org.matsim.core.config.groups.QSimConfigGroup.TrafficDynamics;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.interfaces.SignalGroupState;
import org.matsim.core.mobsim.qsim.interfaces.SignalizeableItem;
import org.matsim.core.mobsim.qsim.pt.TransitDriverAgent;
import org.matsim.core.mobsim.qsim.qnetsimengine.AbstractQLink.HandleTransitStopResult;
import org.matsim.core.mobsim.qsim.qnetsimengine.QLinkImpl.LaneFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.flow_efficiency.DefaultFlowEfficiencyCalculator;
import org.matsim.core.mobsim.qsim.qnetsimengine.flow_efficiency.FlowEfficiencyCalculator;
import org.matsim.core.mobsim.qsim.qnetsimengine.linkspeedcalculator.LinkSpeedCalculator;
import org.matsim.core.mobsim.qsim.qnetsimengine.vehicleq.FIFOVehicleQ;
import org.matsim.core.mobsim.qsim.qnetsimengine.vehicleq.PassingVehicleQ;
import org.matsim.core.mobsim.qsim.qnetsimengine.vehicleq.VehicleQ;
import org.matsim.core.utils.misc.Time;
import org.matsim.lanes.Lane;
import org.matsim.vehicles.Vehicle;
import org.matsim.vis.snapshotwriters.AgentSnapshotInfo;
import org.matsim.vis.snapshotwriters.VisVehicle;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Separating out the "lane" functionality from the "link" functionality.
 * <p></p>
 * Design thoughts:<ul>
 * <li> In fast capacity update, the flows are not accumulated in every time step, 
 * rather updated only if an agent wants to enter the link or an agent is added to buffer. 
 * Improvement of 15-20% in the computational performance is observed. amit feb'16
 * (I seem to recall that in the end that statement was not consistently correct.  kai, feb'18)</li>
 * <li>Currently (feb'18), the design is such that (possibly time-dep) flowCap and nEffectiveLanes are "pushed" into the
 * class, while freeSpeed is "pulled" from the class.  This is an attempt to bridge the diverging design requirements,
 * where flowCap and nEffectiveLanes may, in the multiple lanes implementation, vary by lane (for the same link), while
 * speed uses {@link LinkSpeedCalculator}, which does not have the link freeSpeed as a parameter and thus is a "pull"
 * method.</li>
 * </ul>
 *
 * @author nagel
 */
final class QueueWithBufferForRoW_simpler implements QLaneI, SignalizeableItem {
	private static final Logger log = Logger.getLogger( QueueWithBuffer.class ) ;

	static final class Builder implements LaneFactory {
		private VehicleQ<QVehicle> vehicleQueue = new FIFOVehicleQ() ;
		private Id<Lane> id = null ;
		private Double length = null ;
		private Double effectiveNumberOfLanes = null ;
		private Double flowCapacity_s = null ;
		private final NetsimEngineContext context;
		private FlowEfficiencyCalculator flowEfficiencyCalculator;
		Builder( final NetsimEngineContext context ) {
			this.context = context ;
			if (context.qsimConfig.getLinkDynamics() == QSimConfigGroup.LinkDynamics.PassingQ ||
					context.qsimConfig.getLinkDynamics() == QSimConfigGroup.LinkDynamics.SeepageQ) {
				this.vehicleQueue = new PassingVehicleQ() ;
			}
		}
		void setVehicleQueue(VehicleQ<QVehicle> vehicleQueue) { this.vehicleQueue = vehicleQueue; }
		void setLaneId(Id<Lane> id) { this.id = id; }
		void setLength(Double length) { this.length = length; }
		void setEffectiveNumberOfLanes(Double effectiveNumberOfLanes) { this.effectiveNumberOfLanes = effectiveNumberOfLanes; }
		void setFlowCapacity_s(Double flowCapacity_s) { this.flowCapacity_s = flowCapacity_s; }
		void setFlowEfficiencyCalculator(FlowEfficiencyCalculator flowEfficiencyCalculator) { this.flowEfficiencyCalculator = flowEfficiencyCalculator; }
		@Override public QueueWithBufferForRoW_simpler createLane( AbstractQLink qLink ) {
			// a number of things I cannot configure before I have the qlink:
			if ( id==null ) { id = Id.create( qLink.getLink().getId() , Lane.class ) ; }
			if ( length==null ) { length = qLink.getLink().getLength() ; }
			if ( effectiveNumberOfLanes==null ) { effectiveNumberOfLanes = qLink.getLink().getNumberOfLanes() ; }
			if ( flowCapacity_s==null ) { flowCapacity_s = ((Link)qLink.getLink()).getFlowCapacityPerSec() ; }
			if (flowEfficiencyCalculator == null) { flowEfficiencyCalculator = new DefaultFlowEfficiencyCalculator(); }
			QueueWithBuffer.Builder delegateBuilder = new QueueWithBuffer.Builder(context);
			QueueWithBuffer delegate = delegateBuilder.createLane(qLink);

			return new QueueWithBufferForRoW_simpler( qLink.getInternalInterface(), vehicleQueue, id, length, effectiveNumberOfLanes, flowCapacity_s, context, flowEfficiencyCalculator,
					delegate) ;
		}
	}

	/**
	 * Stores the accumulated fractional parts of the flow capacity. See also
	 * flowCapFraction.
	 * <p></p>
	 * I changed this into an internal class as a first step to look into acceleration (not having to keep this link active until
	 * this has accumulated to one).  There is no need to keep it this way; it just seems to make it easier to keep track of
	 * changes.  kai, sep'14
	 */
	private static class FlowcapAccumulate {
		private double timeStep = 0.;//Double.NEGATIVE_INFINITY ;
		private double value = 0. ;
		private double getTimeStep(){
			return this.timeStep;
		}
		private void setTimeStep(double now) {
			this.timeStep = now;
		}
		private double getValue() {
			return value;
		}
		private void setValue(double value ) {
			this.value = value;
		}
		private void addValue(double value1, double now) {
			this.value += value1;
			this.timeStep = now ;
		}
	}
	private final FlowcapAccumulate flowcap_accumulate = new FlowcapAccumulate() ;
	// might be changed back to standard double after all of this was figured out. kai, sep'14


	QueueWithBuffer delegate;
	/**
	 * true, i.e. green, if the link is not signalized
	 */
	private boolean thisTimeStepGreen = true ;
	private double inverseFlowCapacityPerTimeStep;
	/**
	 * The number of vehicles able to leave the buffer in one time step (usually 1s).
	 */
	private double flowCapacityPerTimeStep;


	/** the last time-step the front-most vehicle in the buffer was moved. Used for detecting dead-locks. */
	private double bufferLastMovedTime = Time.getUndefinedTime() ;
	/**
	 * The list of vehicles that have not yet reached the end of the link
	 * according to the free travel speed of the link
	 */
	private final VehicleQ<QVehicle> vehQueue;

	private double storageCapacity;
	private double usedStorageCapacity;
	/**
	 * Holds all vehicles that are ready to cross the outgoing intersection
	 */
	private final Queue<QVehicle> generalBuffer = new ConcurrentLinkedQueue<>() ;
	private final Queue<QVehicle> leftBuffer = new ConcurrentLinkedQueue<>() ;

	private final Queue<QueueWithBuffer.Hole> holes = new LinkedList<>();

	/**
	 * null if the link is not signalized
	 */
	private DefaultSignalizeableItem qSignalizedItem = null ;
	/**
	 * I think that it would be good to get rid of the qLink backpointer.  Instead keep a reduced QLinkInternalInterface back
	 * pointer, and give access only to reduced number of methods (in particular not the full Link information). kai, feb'18
	 * This is now done with the {@link AbstractQLink.QLinkInternalInterface}.  kai, feb'18
	 */
	private final AbstractQLink.QLinkInternalInterface qLink;
	private final Id<Lane> id;
	private static int spaceCapWarningCount = 0;

	private final double length ;
	private double unscaledFlowCapacity_s = Double.NaN ;
	private double effectiveNumberOfLanes = Double.NaN ;

	private final NetsimEngineContext context;

	private double accumulatedInflowCap = 1. ;

	private final FlowEfficiencyCalculator flowEfficiencyCalculator;

	private QueueWithBufferForRoW_simpler(AbstractQLink.QLinkInternalInterface qlink, final VehicleQ<QVehicle> vehicleQueue, Id<Lane> laneId,
			double length, double effectiveNumberOfLanes, double flowCapacity_s, final NetsimEngineContext context, FlowEfficiencyCalculator flowEfficiencyCalculator, QueueWithBuffer delegate) {
		// the general idea is to give this object no longer access to "everything".  Objects get back pointers (here qlink), but they
		// do not present the back pointer to the outside.  In consequence, this object can go up to qlink, but not any further. kai, mar'16
		// Now I am even trying to get rid of the full qLink back pointer (since it allows, e.g., going back to Link). kai, feb'18

		//		log.setLevel(Level.DEBUG);

		this.flowEfficiencyCalculator = flowEfficiencyCalculator;

		this.qLink = qlink;
		this.id = laneId ;
		this.context = context ;
		this.vehQueue = vehicleQueue ;
		this.length = length;
		this.unscaledFlowCapacity_s = flowCapacity_s ;
		this.effectiveNumberOfLanes = effectiveNumberOfLanes;
		this.delegate = delegate;

		//		freespeedTravelTime = this.length / qlink.getLink().getFreespeed();
		//		if (Double.isNaN(freespeedTravelTime)) {
		//			throw new IllegalStateException("Double.NaN is not a valid freespeed travel time for a link. Please check the attributes length and freespeed!");
		//		}
		this.calculateFlowCapacity();
		this.calculateStorageCapacity();

		flowcap_accumulate.setValue(flowCapacityPerTimeStep);

		if ( context.qsimConfig.getTimeStepSize() < 1. ) {
			throw new RuntimeException("yyyy This will produce weird results because in at least one place "
					+ "(addFromUpstream(...)) everything is pulled to integer values.  Aborting ... "
					+ "(This statement may no longer be correct; I think that the incriminating code was modified.  So please test and remove"
					+ " the warning if it works. kai, sep'14") ;
		}

	}

	@Override
	public final void addFromWait(final QVehicle veh) {
		//To protect against calling addToBuffer() without calling hasFlowCapacityLeft() first.
		//This only could happen for addFromWait(), because it can be called from outside QueueWithBuffer
		if (flowcap_accumulate.getValue() <= 0.0 && veh.getVehicle().getType().getPcuEquivalents() > context.qsimConfig
				.getPcuThresholdForFlowCapacityEasing()) {
			throw new IllegalStateException("Buffer of link " + this.id + " has no space left!");
		}

		addToBuffer(veh);
	}


	private void addToBuffer(final QVehicle veh) {
		// yy might make sense to just accumulate to "zero" and go into negative when something is used up.
		// kai/mz/amit, mar'12

		double now = context.getSimTimer().getTimeOfDay() ;
		flowcap_accumulate.addValue(-getFlowCapacityConsumptionInEquivalents(veh), now);


		Id<Link> nextLinkId = veh.getDriver().chooseNextLinkId();
		QFFFAbstractNode toNode = ((QFFFNode) this.qLink.getToNodeQ()).getQFFFAbstractNode();
		// Left buffer
		if(toNode instanceof QFFFNodeDirectedPriorityNode && 
				((QFFFNodeDirectedPriorityNode) toNode).isLeftTurn(this.qLink.getId(), nextLinkId) ){
			if(leftBuffer.isEmpty() & generalBuffer.isEmpty()){
				bufferLastMovedTime = now;
			}
			leftBuffer.add(veh);
		} else { 	// General buffer
			if(leftBuffer.isEmpty() & generalBuffer.isEmpty()){
				bufferLastMovedTime = now;
			}
			generalBuffer.add(veh);
		}
		((QFFFNode) qLink.getToNodeQ()).activateNode();
	}

	@Override
	public final boolean isAcceptingFromWait(QVehicle veh) {
		return this.hasFlowCapacityLeft(veh) ;
	}

	private boolean hasFlowCapacityLeft(VisVehicle veh) {
		return flowcap_accumulate.getValue() > 0.0 || veh.getVehicle().getType()
				.getPcuEquivalents() <= context.qsimConfig.getPcuThresholdForFlowCapacityEasing();
	}


	private void updateSlowFlowAccumulation(){
		if (this.thisTimeStepGreen
				&& this.flowcap_accumulate.getValue() < flowCapacityPerTimeStep
				&& isNotOfferingVehicle() ){
			double newFlowCap = Math.min(flowcap_accumulate.getValue() + flowCapacityPerTimeStep,
					flowCapacityPerTimeStep);
			flowcap_accumulate.setValue(newFlowCap);
		}
	}

	@Override
	public final void initBeforeSimStep() {
		if(!context.qsimConfig.isUsingFastCapacityUpdate() ){
			updateSlowFlowAccumulation();
		}
	}
	private void calculateFlowCapacity() {
		// the following is not looking at time because it simply assumes that the lookups are "now". kai, feb'18
		// I am currently not sure if this statement is correct. kai, feb'18
		flowCapacityPerTimeStep = this.unscaledFlowCapacity_s * context.qsimConfig.getTimeStepSize() * context.qsimConfig.getFlowCapFactor() ;
		inverseFlowCapacityPerTimeStep = 1.0 / flowCapacityPerTimeStep;
	}

	private void calculateStorageCapacity() {
		// first guess at storageCapacity:
		storageCapacity = this.length * this.effectiveNumberOfLanes / context.effectiveCellSize * context.qsimConfig.getStorageCapFactor() ;
		//		storageCapacity = this.length * this.qLink.getLink().getNumberOfLanes(now) / context.effectiveCellSize * context.qsimConfig.getStorageCapFactor() ;

		// storage capacity needs to be at least enough to handle the cap_per_time_step:
		storageCapacity = Math.max(storageCapacity, getBufferStorageCapacity());

		/*
		 * If speed on link is relatively slow, then we need MORE cells than the above spaceCap to handle the flowCap.
		 * Example: Assume freeSpeedTravelTime (aka freeTravelDuration) is 2 seconds. Than we need the spaceCap = TWO
		 * times the flowCap to handle the flowCap.
		 *
		 * Will base these computations (for the time being) on the standard free speed; i.e. reductions in free speed
		 * will also reduce the maximum flow.
		 */
		double freespeedTravelTime = this.length / qLink.getFreespeed();
		// yyyyyy this should possibly be getFreespeed(now). But if that's the case, then storageCap would
		// also have to be re-computed with each freespeed change. kai, feb'18
		if (Double.isNaN(freespeedTravelTime)) {
			throw new IllegalStateException("Double.NaN is not a valid freespeed travel time for a link. Please check the attributes length and freespeed!");
		}

		//this assumes that vehicles have the flowEfficiencyFactor of 1.0; the actual flow can be different
		double tempStorageCapacity = freespeedTravelTime * flowCapacityPerTimeStep; 
		// yy note: freespeedTravelTime may be Inf.  In this case, storageCapacity will also be set to Inf.  This can still be
		// interpreted, but it means that the link will act as an infinite sink.  kai, nov'10

		if (storageCapacity < tempStorageCapacity) {
			if (spaceCapWarningCount <= 10) {
				log.warn("Link " + this.id + " too small: enlarge storage capacity from: " + storageCapacity
						+ " Vehicles to: " + tempStorageCapacity + " Vehicles.  This is not fatal, but modifies the traffic flow dynamics.");
				if (spaceCapWarningCount == 10) {
					log.warn("Additional warnings of this type are suppressed.");
				}
				spaceCapWarningCount++;
			}
			storageCapacity = tempStorageCapacity;
		}

	}

	private double getBufferStorageCapacity() {
		return flowCapacityPerTimeStep;//this assumes that vehicles have the flowEfficiencyFactor of 1.0 
	}

	@Override
	public final boolean doSimStep( ) {
		this.moveQueueToBuffer();
		return true ;
	}


	/**
	 * Move vehicles from link to buffer, according to buffer capacity and
	 * departure time of vehicle. Also removes vehicles from lane if the vehicle
	 * arrived at its destination.
	 */
	private void moveQueueToBuffer() {
		double now = context.getSimTimer().getTimeOfDay() ;

		QVehicle veh;
		while((veh = peekFromVehQueue()) !=null){
			//we have an original QueueLink behaviour
			if (veh.getEarliestLinkExitTime() > now){
				return;
			}

			MobsimDriverAgent driver = veh.getDriver();

			if (driver instanceof TransitDriverAgent) {
				HandleTransitStopResult handleTransitStop = qLink.handleTransitStop(
						now, veh, (TransitDriverAgent) driver, this.qLink.getId()
						);
				if (handleTransitStop == HandleTransitStopResult.accepted) {
					// vehicle has been accepted into the transit vehicle queue of the link.
					removeVehicleFromQueue(veh) ;
					continue;
				} else if (handleTransitStop == HandleTransitStopResult.rehandle) {
					continue; // yy why "continue", and not "break" or "return"?  Seems to me that this
					// is currently only working because qLink.handleTransitStop(...) also increases the
					// earliestLinkExitTime for the present vehicle.  kai, oct'13
					// zz From my point of view it is exactly like described above. dg, mar'14
					//				} else if (handleTransitStop == HandleTransitStopResult.continue_driving) {
					// Do nothing, but go on..
				}
			}

			// Check if veh has reached destination:
			if ( driver.isWantingToArriveOnCurrentLink() ) {
				qLink.letVehicleArrive( veh );

				// remove _after_ processing the arrival to keep link active:
				removeVehicleFromQueue( veh ) ;

				continue;
			}

			/* is there still any flow capacity left? */
			if (!hasFlowCapacityLeft(veh) ) {
				return;
			}

			addToBuffer(veh);
			removeVehicleFromQueue(veh);
		} // end while
	}

	private void removeVehicleFromQueue(final QVehicle veh2Remove) {

		//		QVehicle veh = vehQueue.poll();
		//		usedStorageCapacity -= veh.getSizeInEquivalents();

		QVehicle veh = pollFromVehQueue(veh2Remove);
		usedStorageCapacity -= veh.getSizeInEquivalents();
	}

	@Override
	public final boolean isActive() {
				return (this.flowcap_accumulate.getValue() < flowCapacityPerTimeStep) // still accumulating, thus active
					|| (!this.vehQueue.isEmpty()) // vehicles are on link, thus active
					|| (!this.isNotOfferingVehicle() && context.qsimConfig.isUseLanes()) // if lanes, the buffer needs to be active in order to move vehicles over an internal node
					; // need to process arrival of holes
	}

	@Override
	public final void setSignalStateAllTurningMoves( final SignalGroupState state) {
		qSignalizedItem.setSignalStateAllTurningMoves(state);

		thisTimeStepGreen  = qSignalizedItem.hasGreenForAllToLinks();
		// (this is only for capacity accumulation)
	}

	@Override
	public final double getSimulatedFlowCapacityPerTimeStep() {
		return this.flowCapacityPerTimeStep;
	}

	@Override
	public final boolean isAcceptingFromUpstream() {
		boolean storageOk = usedStorageCapacity < storageCapacity ;
		return storageOk ;
	}

	@Override
	public void recalcTimeVariantAttributes() {
		calculateFlowCapacity();
		calculateStorageCapacity();
		flowcap_accumulate.setValue(flowCapacityPerTimeStep);
	}

	@Override
	public final QVehicle getVehicle(final Id<Vehicle> vehicleId) {
		for (QVehicle veh : this.vehQueue) {
			if (veh.getId().equals(vehicleId))
				return veh;
		}
		for (QVehicle veh : this.generalBuffer) {
			if (veh.getId().equals(vehicleId))
				return veh;
		}
		for (QVehicle veh : this.leftBuffer) {
			if (veh.getId().equals(vehicleId))
				return veh;
		}
		return null;
	}

	@Override
	public final Collection<MobsimVehicle> getAllVehicles() {
		/* since it is an instance of arrayList, insertion order is maintained. Thus, correcting the order or insertion.
		 * It will be more complicated for passingQueue. amit feb'16
		 */
		Collection<MobsimVehicle> vehicles = new ArrayList<>();
		vehicles.addAll(generalBuffer);
		vehicles.addAll(leftBuffer);
		vehicles.addAll(vehQueue);
		return vehicles ;
	}

	@Override
	public final QVehicle popFirstVehicle() {
		return removeFirstVehicle();
	}

	public void removeFirstLeftVehicle() {
		leftBuffer.remove();
	}
	public  void removeFirstGeneralVehicle() {
		generalBuffer.remove();
	}

	private final QVehicle removeFirstVehicle(){
		double now = context.getSimTimer().getTimeOfDay() ;

		QVehicle veh = generalBuffer.poll();
		bufferLastMovedTime = now; // just in case there is another vehicle in the buffer that is now the new front-most

		return veh;
	}

	@Override
	public final void setSignalStateForTurningMove( final SignalGroupState state, final Id<Link> toLinkId) {
		if (!qLink.getToNode().getOutLinks().containsKey(toLinkId)){
			throw new IllegalArgumentException("ToLink " + toLinkId + " is not reachable from QLink Id " +  this.id );
		}
		qSignalizedItem.setSignalStateForTurningMove(state, toLinkId);

		thisTimeStepGreen = qSignalizedItem.hasGreenForAllToLinks();
		// (this is only for capacity accumulation.  As soon as at least one turning relation is green, the "link" is considered
		// green).
	}

	@Override
	public final boolean hasGreenForToLink(final Id<Link> toLinkId) {
		if (qSignalizedItem != null){
			return qSignalizedItem.hasGreenForToLink(toLinkId);
		}
		return true; //the lane is not signalized and thus always green
	}

	@Override
	public boolean hasGreenForAllToLinks() {
		if (qSignalizedItem != null) {
			return qSignalizedItem.hasGreenForAllToLinks();
		}
		return true; //the lane is not signalized and thus always green
	}

	@Override
	public final double getStorageCapacity() {
		return storageCapacity;
	}

	@Override
	public final boolean isNotOfferingVehicle() {
		return generalBuffer.isEmpty() && leftBuffer.isEmpty();
	}

	public final boolean isNotOfferingGeneralVehicle() {
		return generalBuffer.isEmpty();
	}

	public final boolean isNotOfferingLeftVehicle() {
		return leftBuffer.isEmpty();
	}

	@Override
	public final void clearVehicles() {
		// yyyyyy right now it seems to me that one should rather just abort the agents and have the framework take care of the rest. kai, mar'16

		double now = context.getSimTimer().getTimeOfDay() ;

		for (QVehicle veh : vehQueue) {
			context.getEventsManager().processEvent( new VehicleAbortsEvent(now, veh.getId(), veh.getCurrentLink().getId()));
			context.getEventsManager().processEvent( new PersonStuckEvent(now, veh.getDriver().getId(), veh.getCurrentLink().getId(), veh.getDriver().getMode()));

			context.getAgentCounter().incLost();
			context.getAgentCounter().decLiving();
		}
		vehQueue.clear();

		for(Queue<QVehicle> buffer : Arrays.asList(generalBuffer, leftBuffer)){
			for (QVehicle veh : buffer) {
				context.getEventsManager().processEvent( new VehicleAbortsEvent(now, veh.getId(), veh.getCurrentLink().getId()));
				context.getEventsManager().processEvent( new PersonStuckEvent(now, veh.getDriver().getId(), veh.getCurrentLink().getId(), veh.getDriver().getMode()));

				context.getAgentCounter().incLost();
				context.getAgentCounter().decLiving();
			}
			buffer.clear();
		}

	}

	@Override
	public final void addFromUpstream(final QVehicle veh) {
		double now = context.getSimTimer().getTimeOfDay() ;

		qLink.activateLink();

		usedStorageCapacity += veh.getSizeInEquivalents();

		double linkTravelTime = this.length / this.qLink.getMaximumVelocityFromLinkSpeedCalculator( veh, now ) ;
		linkTravelTime = context.qsimConfig.getTimeStepSize() * Math.floor( linkTravelTime / context.qsimConfig.getTimeStepSize() );

		veh.setEarliestLinkExitTime(now + linkTravelTime);
		this.qLink.setCurrentLinkToVehicle( veh ) ;

		vehQueue.add(veh);
	}

	private boolean hasMoreThanOneLane() {
		return this.qLink.getAcceptingQLane() != this.qLink.getOfferingQLanes().get(0);
		// this works independent from sorting since if there is only one lane, then it has to be the one to be returned by
		// getOfferingQLanes().get(0), and it is also the same as the accepting QLane.  If, however, "lanes" is used,
		// there are at least two lanes in sequence, so the accepting lane is never the same as any of the offering lanes, and
		// this will always return false independent from sorting.  kai/theresa, dec'16
	}


	@Override
	public final QVehicle getFirstVehicle() {
		if (this.generalBuffer.isEmpty() && this.leftBuffer.isEmpty()) {
			return this.vehQueue.peek();
		}
		return this.generalBuffer.peek() ;
	}

	public final QVehicle getFirstGeneralVehicle() {
		return this.generalBuffer.peek() ;
	}
	public final QVehicle getFirstLeftVehicle() {
		return this.leftBuffer.peek() ;
	}

	@Override
	public final double getLastMovementTimeOfFirstVehicle() {
		return this.bufferLastMovedTime ;
	}

	/**
	 * Needs to be added _upstream_ of the regular stop location so that a possible second stop on the link can also be served.
	 */
	@Override
	public final void addTransitSlightlyUpstreamOfStop( final QVehicle veh) {
		this.vehQueue.addFirst(veh) ;
	}

	@Override
	public final void setSignalized( final boolean isSignalized) {
		qSignalizedItem  = new DefaultSignalizeableItem(qLink.getToNode().getOutLinks().keySet());
	}

	@Override
	public final void changeUnscaledFlowCapacityPerSecond( final double val ) {
		this.unscaledFlowCapacity_s = val ;
		// be defensive (might now be called twice):
		this.recalcTimeVariantAttributes();
	}

	@Override
	public final void changeEffectiveNumberOfLanes( final double val ) {
		this.effectiveNumberOfLanes = val ;
		// be defensive (might now be called twice):
		this.recalcTimeVariantAttributes();
	}

	@Override public Id<Lane> getId() {
		return this.id;
	}



	private QVehicle peekFromVehQueue(){
		double now = context.getSimTimer().getTimeOfDay() ;

		QVehicle returnVeh = vehQueue.peek();

		return returnVeh;
	}

	private QVehicle pollFromVehQueue(QVehicle veh2Remove){
		if(vehQueue.remove(veh2Remove)){
			return veh2Remove;
		} else {
			throw new RuntimeException("Desired vehicle is not removed from vehQueue. Aborting...");
		}
	}

	@Override
	public double getLoadIndicator() {
		return usedStorageCapacity;
	}

	private double getFlowCapacityConsumptionInEquivalents(QVehicle vehicle) {
		double flowEfficiency = flowEfficiencyCalculator.calculateFlowEfficiency(vehicle.getVehicle(), qLink.getLink());
		return vehicle.getSizeInEquivalents() / flowEfficiency;
	}

	@Override
	public VisData getVisData() {
		return delegate.getVisData();
	}

}
