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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.VehicleAbortsEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.LaneEnterEvent;
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
import org.matsim.core.mobsim.qsim.qnetsimengine.QFFFNode.MoveType;
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
abstract class QueueWithBufferForRoW implements QLaneI, SignalizeableItem, HasLeftBufferTime {
	private static final Logger log = Logger.getLogger( QueueWithBuffer.class ) ;
	public static final boolean COUNTLANETYPES = false;
//	private static final QueueWithBuffer delegate = null;

	static final class Builder implements LaneFactory {
		private VehicleQ<QVehicle> vehicleQueue = new FIFOVehicleQ() ;
		private Id<Lane> id = null ;
		private Double length = null ;
		private Double effectiveNumberOfLanes = null ;
		private Double flowCapacity_s = null ;
		private final NetsimEngineContext context;
		private FlowEfficiencyCalculator flowEfficiencyCalculator;
		private Double leftBufferCapacity = null;

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
		void setMaximumLeftBufferLength(double leftBufferCapacity) { this.leftBufferCapacity = leftBufferCapacity; }
		void setEffectiveNumberOfLanes(Double effectiveNumberOfLanes) { this.effectiveNumberOfLanes = effectiveNumberOfLanes; }
		void setFlowCapacity_s(Double flowCapacity_s) { this.flowCapacity_s = flowCapacity_s; }
		void setFlowEfficiencyCalculator(FlowEfficiencyCalculator flowEfficiencyCalculator) { this.flowEfficiencyCalculator = flowEfficiencyCalculator; }

		@Override public QueueWithBufferForRoW createLane( AbstractQLink qLink ) {
			// a number of things I cannot configure before I have the qlink:
			if ( id==null ) { id = Id.create( qLink.getLink().getId() , Lane.class ) ; }
			if ( length==null ) { length = qLink.getLink().getLength() ; }
			if ( effectiveNumberOfLanes==null ) { effectiveNumberOfLanes = qLink.getLink().getNumberOfLanes() ; }
			if ( flowCapacity_s==null ) { flowCapacity_s = ((Link)qLink.getLink()).getFlowCapacityPerSec() ; }
			if (flowEfficiencyCalculator == null) { flowEfficiencyCalculator = new DefaultFlowEfficiencyCalculator(); }
			if ( leftBufferCapacity == null) { leftBufferCapacity = Double.MAX_VALUE; }

			if(leftBufferCapacity <= 0) {
				if(COUNTLANETYPES) {

					int cnt = Counters.countSingleLanes.incrementAndGet();
					if(cnt % 1000 == 0) {
						log.info(cnt + " singleLanes");
					}
				}
				return new QueueWithSingleBufferForRoW( qLink.getInternalInterface(), vehicleQueue, id, length, effectiveNumberOfLanes, flowCapacity_s, context, flowEfficiencyCalculator) ;
			} else if (leftBufferCapacity < Integer.MAX_VALUE) {
				if(COUNTLANETYPES) {

					int cnt = Counters.countInteractingLanes.incrementAndGet();
					if(cnt % 1000 == 0) {
						log.info(cnt + " interactingLanes");
					}
				}
				return new QueueWithTwoInteractingBuffersForRoW( qLink.getInternalInterface(), vehicleQueue, id, length, effectiveNumberOfLanes, flowCapacity_s, context, flowEfficiencyCalculator, leftBufferCapacity ) ;
			} else {
				if(COUNTLANETYPES) {
					int cnt = Counters.countSeparatedLanes.incrementAndGet();
					if(cnt % 1000 == 0) {
						log.info(cnt + " separatedLanes");
					}
				}
				return new QueueWithTwoSeparatedBuffersForRoW( qLink.getInternalInterface(), vehicleQueue, id, length, effectiveNumberOfLanes, flowCapacity_s, context, flowEfficiencyCalculator) ;
			} 
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

	/**
	 * true, i.e. green, if the link is not signalized
	 */
	private boolean thisTimeStepGreen = true ;
	private double inverseFlowCapacityPerTimeStep;
	/**
	 * The number of vehicles able to leave the buffer in one time step (usually 1s).
	 */
	private double flowCapacityPerTimeStep;
	private double remainingHolesStorageCapacity = 0.0 ;

	private final Queue<QueueWithBuffer.Hole> holes = new LinkedList<>();

	/** the last time-step the front-most vehicle in the buffer was moved. Used for detecting dead-locks. */
	protected double generalBufferLastMovedTime = Double.NEGATIVE_INFINITY;

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
	protected final Queue<QVehicleAndMoveType> newGeneralBuffer = new ConcurrentLinkedQueue<>() ;

	/**
	 * null if the link is not signalized
	 */
	private DefaultSignalizeableItem qSignalizedItem = null ;
	/**
	 * I think that it would be good to get rid of the qLink backpointer.  Instead keep a reduced QLinkInternalInterface back
	 * pointer, and give access only to reduced number of methods (in particular not the full Link information). kai, feb'18
	 * This is now done with the {@link AbstractQLink.QLinkInternalInterface}.  kai, feb'18
	 */
	protected final AbstractQLink.QLinkInternalInterface qLink;
	private final Id<Lane> id;
	private static int spaceCapWarningCount = 0;
	final static double HOLE_SPEED_KM_H = 15.0;

	private final double length ;
	private double unscaledFlowCapacity_s = Double.NaN ;
	private double effectiveNumberOfLanes = Double.NaN ;

	private final VisData visData = new VisDataImpl() ;
	protected final NetsimEngineContext context;

	private double maxFlowFromFdiag = Double.POSITIVE_INFINITY ;

	private double accumulatedInflowCap = 1. ;

	private final FlowEfficiencyCalculator flowEfficiencyCalculator;



	protected QueueWithBufferForRoW(AbstractQLink.QLinkInternalInterface qlink, final VehicleQ<QVehicle> vehicleQueue, Id<Lane> laneId,
			double length, double effectiveNumberOfLanes, double flowCapacity_s, final NetsimEngineContext context, FlowEfficiencyCalculator flowEfficiencyCalculator) {
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

		double now = context.getSimTimer().getTimeOfDay() ;
		flowcap_accumulate.addValue(-getFlowCapacityConsumptionInEquivalents(veh, null, null), now);

		Id<Link> nextLinkId = veh.getDriver().chooseNextLinkId();
		QFFFAbstractNode toQNode = ((QFFFNode) this.qLink.getToNodeQ()).getQFFFAbstractNode();
		int outDirection = toQNode.carOutTransformations.get(nextLinkId);
		MoveType mt = MoveType.GENERAL;
		if(toQNode instanceof QFFFNodeWithLeftBuffer && ((QFFFNodeWithLeftBuffer) toQNode).isCarLeftTurn(this.qLink.getId(), outDirection)) {
			mt = MoveType.LEFT_TURN;
		} 
		QVehicleImpl qVeh;
		if(veh instanceof QVehicleAndMoveType) {
			qVeh = ((QVehicleAndMoveType) veh).getQVehicle();
		} else {
			qVeh = (QVehicleImpl) veh;
		}
		QVehicleAndMoveType qVehAndMt = new QVehicleAndMoveType(qVeh, mt, outDirection);
		actuallyAddToBuffer(qVehAndMt, now);


		// Activation
		final QNodeI toNode = this.qLink.getToNodeQ();
		if(toNode instanceof AbstractQNode){
			((AbstractQNode) toNode).activateNode();
		}
	}

	protected abstract void actuallyAddToBuffer(QVehicleAndMoveType qVehAndMt, double now);

	@Override
	public final boolean isAcceptingFromWait(QVehicle veh) {
		return this.hasFlowCapacityLeft(veh) ;
	}

	private boolean hasFlowCapacityLeft(VisVehicle veh) {
		if(context.qsimConfig.isUsingFastCapacityUpdate() ){
			updateFastFlowAccumulation();
		}

		return flowcap_accumulate.getValue() > 0.0 || veh.getVehicle().getType()
				.getPcuEquivalents() <= context.qsimConfig.getPcuThresholdForFlowCapacityEasing();
	}

	private void updateFastFlowAccumulation(){
		double now = context.getSimTimer().getTimeOfDay() ;

		if( this.flowcap_accumulate.getTimeStep() < now
				&& this.flowcap_accumulate.getValue() < flowCapacityPerTimeStep
				&& isNotOfferingVehicle() ){

			double timeSteps = (now - flowcap_accumulate.getTimeStep()) / context.qsimConfig.getTimeStepSize();
			double accumulateFlowCap = timeSteps * flowCapacityPerTimeStep;
			double newFlowCap = Math.min(flowcap_accumulate.getValue() + accumulateFlowCap,
					flowCapacityPerTimeStep);

			flowcap_accumulate.setValue(newFlowCap);
			flowcap_accumulate.setTimeStep( now );
		}
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
	private static int wrnCnt=0 ;
	private void calculateFlowCapacity() {
		// the following is not looking at time because it simply assumes that the lookups are "now". kai, feb'18
		// I am currently not sure if this statement is correct. kai, feb'18

		double now = context.getSimTimer().getTimeOfDay() ;

		flowCapacityPerTimeStep = this.unscaledFlowCapacity_s ;
		//		flowCapacityPerTimeStep = this.qLink.getLink().getFlowCapacityPerSec(now) ;
		// we need the flow capacity per sim-tick and multiplied with flowCapFactor
		flowCapacityPerTimeStep = flowCapacityPerTimeStep * context.qsimConfig.getTimeStepSize() * context.qsimConfig.getFlowCapFactor() ;
		inverseFlowCapacityPerTimeStep = 1.0 / flowCapacityPerTimeStep;

		switch (context.qsimConfig.getTrafficDynamics()) {
		case queue:
		case withHoles:
			break;
		case kinematicWaves:
			// uncongested branch: q = vmax * rho
			// congested branch: q = vhole * (rhojam - rho)
			// equal: rho * (vmax + vhole) = vhole * rhojam
			// rho(qmax) = vhole * rhojam / (vmax + vhole)
			// qmax = vmax * rho(qmax) = rhojam / (1/vhole + 1/vmax) ;
			this.maxFlowFromFdiag = (1/context.effectiveCellSize) / ( 1./(HOLE_SPEED_KM_H/3.6) + 1/this.qLink.getFreespeed() ) ;
			// yyyyyy this should possibly be getFreespeed(now). But if that's the case, then maxFlowFromFdiag would
			// also have to be re-computed with each freespeed change. kai, feb'18
			if ( this.maxFlowFromFdiag < flowCapacityPerTimeStep && wrnCnt<10 ) {
				wrnCnt++ ;
				log.warn( "max flow from fdiag < requested flow cap; linkId=" + qLink.getId() +
						"; req flow cap/h=" + 3600.*flowCapacityPerTimeStep/context.qsimConfig.getTimeStepSize() +
						"; max flow from fdiag/h=" + 3600*maxFlowFromFdiag/context.qsimConfig.getTimeStepSize() ) ;
				if ( wrnCnt==10 ) {
					log.warn( Gbl.FUTURE_SUPPRESSED ) ;
				}
			}
			break;
		default: throw new RuntimeException("The traffic dynamics "+context.qsimConfig.getTrafficDynamics()+" is not implemented yet.");
		}
		//		log.debug( "linkId=" + this.qLink.getLink().getId() + "; flowCapPerTimeStep=" + flowCapacityPerTimeStep +
		//						   "; invFlowCapPerTimeStep=" + inverseFlowCapacityPerTimeStep + "; maxFlowFromFdiag=" + maxFlowFromFdiag ) ;

	}

	private void calculateStorageCapacity() {
		// The following is not adjusted for time-dependence!! kai, apr'16
		// No, I think that it simply assumes that the lookups are "now". kai, feb'18
		//		double now = context.getSimTimer().getTimeOfDay() ;

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

		/* About minStorCapForHoles:
		 * () uncongested branch is q(rho) = rho * v_max
		 * () congested branch is q(rho) = (rho - rho_jam) * v_holes
		 * () rho_maxflow is where these two meet, resulting in rho_maxflow = v_holes * rho_jam / ( v_holes + v_max )
		 * () max flow is q(rho_maxflow), resulting in v_max * v_holes * rho_jam / ( v_holes + v_max ) 
		 * () Since everything else is given, rho_jam needs to be large enough so that q(rho_maxflow) can reach capacity, resulting in
		 *    rho_jam >= capacity * (v_holes + v_max) / (v_max * v_holes) ;
		 * () In consequence, storage capacity needs to be larger than curved_length * rho_jam .
		 *
		 */

		switch (context.qsimConfig.getTrafficDynamics()) {
		case queue:
			break;
		case withHoles:
		case kinematicWaves:
			//			final double minStorCapForHoles = 2. * flowCapacityPerTimeStep * context.getSimTimer().getSimTimestepSize();
			final double freeSpeed = qLink.getFreespeed() ; // yyyyyy not clear why this is not time-dep. kai, feb'18
			final double holeSpeed = HOLE_SPEED_KM_H/3.6;
			final double minStorCapForHoles = length * flowCapacityPerTimeStep * (freeSpeed + holeSpeed) / freeSpeed / holeSpeed ;
			//			final double minStorCapForHoles = 2.* length * flowCapacityPerTimeStep * (freeSpeed + holeSpeed) / freeSpeed / holeSpeed ;
			// I have no idea why the factor 2 needs to be there?!?! kai, apr'16
			// I just removed the factor of 2 ... seems to work now without.  kai, may'16
			// yyyyyy (not thought through for TS != 1sec!  (should use flow cap per second) kai, apr'16)
			if ( storageCapacity < minStorCapForHoles ) {
				if ( spaceCapWarningCount <= 10 ) {
					log.warn("storage capacity not sufficient for holes; increasing from " + storageCapacity + " to " + minStorCapForHoles ) ;
					spaceCapWarningCount++;
				}
				storageCapacity = minStorCapForHoles ;
			}

			remainingHolesStorageCapacity = this.storageCapacity;
			// yyyy how is this.storageCapacity supposed to have a value here?  (It might just be zero, and
			// maybe this is the correct value, but the code is not very expressive.)  kai, mar'17
			// i think, at this location, this explains everything. amit mar'17
			break;
		default: throw new RuntimeException("The traffic dynmics "+context.qsimConfig.getTrafficDynamics()+" is not implemented yet.");
		}
	}

	private double getBufferStorageCapacity() {
		return flowCapacityPerTimeStep;//this assumes that vehicles have the flowEfficiencyFactor of 1.0 
	}

	@Override
	public final boolean doSimStep( ) {
		switch (context.qsimConfig.getTrafficDynamics()) {
		case queue:
			break;
		case withHoles:
			this.processArrivalOfHoles( ) ;
			break;
		case kinematicWaves:
			this.accumulatedInflowCap = Math.min(accumulatedInflowCap + maxFlowFromFdiag, maxFlowFromFdiag);
			this.processArrivalOfHoles( ) ;
			break;
		default: throw new RuntimeException("The traffic dynmics "+context.qsimConfig.getTrafficDynamics()+" is not implemented yet.");
		}
		this.moveQueueToBuffer();
		return true ;
	}

	private void processArrivalOfHoles() {
		double now = context.getSimTimer().getTimeOfDay() ;
		while ( this.holes.size()>0 && this.holes.peek().getEarliestLinkExitTime() < now ) {
			org.matsim.core.mobsim.qsim.qnetsimengine.QueueWithBuffer.Hole hole = this.holes.poll() ; // ???
			this.remainingHolesStorageCapacity += hole.getSizeInEquivalents() ;
		}
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
				if(qLink.letVehicleArrive( veh )) {
					// remove _after_ processing the arrival to keep link active:
					removeVehicleFromQueue( veh ) ;
					continue;
				} else {
					return;
				}


			}

			/* is there still any flow capacity left? */
			if (!hasFlowCapacityLeft(veh) ) {
				return;
			}

			addToBuffer(veh);
			removeVehicleFromQueue(veh);
			if(context.qsimConfig.isRestrictingSeepage()
					&& context.qsimConfig.getLinkDynamics()==LinkDynamics.SeepageQ
					&& context.qsimConfig.getSeepModes().contains(veh.getDriver().getMode()) ) {
				noOfSeepModeBringFwd++;
			}
		} // end while
	}

	private void removeVehicleFromQueue(final QVehicle veh2Remove) {
		double now = context.getSimTimer().getTimeOfDay() ;


		//		QVehicle veh = vehQueue.poll();
		//		usedStorageCapacity -= veh.getSizeInEquivalents();

		QVehicle veh = pollFromVehQueue(veh2Remove);

		if(context.qsimConfig.getLinkDynamics()==LinkDynamics.SeepageQ
				&& context.qsimConfig.isSeepModeStorageFree()
				&& context.qsimConfig.getSeepModes().contains(veh.getVehicle().getType().getId().toString()) ){
			// do nothing
		} else {
			usedStorageCapacity -= veh.getSizeInEquivalents();
		}

		switch (context.qsimConfig.getTrafficDynamics()) {
		case queue:
			break;
		case withHoles:
		case kinematicWaves:
			QueueWithBuffer.Hole hole = new QueueWithBuffer.Hole() ;
			double ttimeOfHoles = length*3600./HOLE_SPEED_KM_H/1000. ;

			//			double offset = this.storageCapacity/this.flowCapacityPerTimeStep ;
			/* NOTE: Start with completely full link, i.e. N_storageCap cells filled.  Now make light at end of link green, discharge with
			 * flowCapPerTS.  After N_storageCap/flowCapPerTS, the link is empty.  Which also means that the holes must have reached
			 * the upstream end of the link.  I.e. speed_holes = length / (N_storageCap/flowCap) and
			 * ttime_holes = lenth/speed = N_storCap/flowCap.
			 * Say length=75m, storCap=10, flowCap=1/2sec.  offset = 20sec.  75m/20sec = 225m/1min = 13.5km/h so this is normal.
			 * Say length=75m, storCap=20, flowCap=1/2sec.  offset = 40sec.  ... = 6.75km/h ... to low.  Reason: unphysical parameters.
			 * (Parameters assume 2-lane road, which should have discharge of 1/sec.  Or we have lots of  tuk tuks, which have only half a vehicle
			 * length.  Thus we incur the reaction time twice as often --> half speed of holes.
			 */

			//			double nLanes = 2. * flowCapacityPerTimeStep ; // pseudo-lanes
			//			double ttimeOfHoles = 0.1 * this.storageCapacity/this.flowCapacityPerTimeStep/nLanes ;

			hole.setEarliestLinkExitTime( now + 1.0*ttimeOfHoles + 0.0*MatsimRandom.getRandom().nextDouble()*ttimeOfHoles ) ;
			hole.setSizeInEquivalents(veh2Remove.getSizeInEquivalents());
			holes.add( hole ) ;
			break;
		default: throw new RuntimeException("The traffic dynmics "+context.qsimConfig.getTrafficDynamics()+" is not implemented yet.");
		}
	}

	@Override
	public final boolean isActive() {
		if( context.qsimConfig.isUsingFastCapacityUpdate() ){
			return (!this.vehQueue.isEmpty())
					|| (!this.isNotOfferingVehicle() && context.qsimConfig.isUseLanes()) // if lanes, the buffer needs to be active in order to move vehicles over an internal node
					|| ( !this.holes.isEmpty() ) ;
		} else {
			return (this.flowcap_accumulate.getValue() < flowCapacityPerTimeStep) // still accumulating, thus active
					|| (!this.vehQueue.isEmpty()) // vehicles are on link, thus active
					|| (!this.isNotOfferingVehicle() && context.qsimConfig.isUseLanes()) // if lanes, the buffer needs to be active in order to move vehicles over an internal node
					|| ( !this.holes.isEmpty() ); // need to process arrival of holes
		}
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

		if ( context.qsimConfig.getTrafficDynamics()==TrafficDynamics.queue )  {
			return storageOk ;
		}
		// (continue only if HOLES and/or inflow constraint)

		if ( context.qsimConfig.getTrafficDynamics() != TrafficDynamics.queue
				&& remainingHolesStorageCapacity <= 0. ) {
			// check the holes storage capacity if using holes only (amit, Aug 2016)
			return false ;
		}
		// remainingHolesStorageCapacity is:
		// * initialized at linkStorageCapacity
		// * reduced by entering vehicles
		// * increased by holes arriving at upstream end of link

		if ( context.qsimConfig.getTrafficDynamics() != TrafficDynamics.kinematicWaves) {
			return true ;
		}

		return this.accumulatedInflowCap > 0;

	}

	@Override
	public void recalcTimeVariantAttributes() {
		// not speed, since that is looked up anyways.
		// yy might also make flow and storage self-detecting changes (not really that
		// much more expensive). kai, feb'18

		//		log.debug("just entered recalcTimeVariantAttributes; now=" + this.context.getSimTimer().getTimeOfDay() ) ;

		calculateFlowCapacity();
		calculateStorageCapacity();
		flowcap_accumulate.setValue(flowCapacityPerTimeStep);
	}

	//	@Override
	//	public final void changeSpeedMetersPerSecond( final double val ) {
	//		this.freespeedTravelTime = this.length / val ;
	//		if (Double.isNaN(freespeedTravelTime)) {
	//			throw new IllegalStateException("Double.NaN is not a valid freespeed travel time for a link. Please check the attributes length and freespeed!");
	//		}
	//	}

	@Override
	public final QVehicle getVehicle(final Id<Vehicle> vehicleId) {
		for (QVehicle veh : this.vehQueue) {
			if (veh.getId().equals(vehicleId))
				return veh;
		}
		for (QVehicleAndMoveType veh : this.newGeneralBuffer) {
			if (veh.getQVehicle().getId().equals(vehicleId))
				return veh.getQVehicle();
		}
		return getVehicleInLeftBuffers(vehicleId);
	}

	protected abstract QVehicle getVehicleInLeftBuffers(Id<Vehicle> vehicleId);

	@Override
	public final Collection<MobsimVehicle> getAllVehicles() {
		/* since it is an instance of arrayList, insertion order is maintained. Thus, correcting the order or insertion.
		 * It will be more complicated for passingQueue. amit feb'16
		 */
		Collection<MobsimVehicle> vehicles = new ArrayList<>();
		for(QVehicleAndMoveType veh : newGeneralBuffer) {
			vehicles.add(veh.getQVehicle());
		}
		addVehiclesFromLeftBuffer(vehicles);
		vehicles.addAll(vehQueue);
		return vehicles ;
	}

	public abstract double getLeftBufferLastTimeMoved();

	protected abstract void addVehiclesFromLeftBuffer(Collection<MobsimVehicle> vehicles);

	public final QVehicle popFirstLeftVehicle() {
		//		double now = context.getSimTimer().getTimeOfDay() ;
		return removeFirstLeftVehicle();
		//		if (this.context.qsimConfig.isUseLanes() ) {
		//			if (  hasMoreThanOneLane() ) {
		//				this.context.getEventsManager().processEvent(new LaneLeaveEvent( now, veh.getId(), this.qLink.getId(), this.getId() ));
		//			}
		//		}
	}

	@Override
	public final QVehicle popFirstVehicle() {
		return removeFirstGeneralVehicle();	
	}


	private final QVehicle removeFirstGeneralVehicle(){
		double now = context.getSimTimer().getTimeOfDay() ;

		QVehicle veh = actuallyRemoveFirstGeneralVehicle(now);

		if( context.qsimConfig.isUsingFastCapacityUpdate() ) {
			flowcap_accumulate.setTimeStep(now - 1);
		}
		return veh;
	}

	protected abstract QVehicle actuallyRemoveFirstGeneralVehicle(double now);

	protected final QVehicle removeFirstLeftVehicle() {
		double now = context.getSimTimer().getTimeOfDay() ;
		QVehicle veh = actuallyRemoveFirstLeftVehicle(now);
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

	protected abstract QVehicle actuallyRemoveFirstLeftVehicle(double now2);

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
	public abstract boolean isNotOfferingVehicle() ;

	//	public final boolean isOfferingNeitherGeneralNorLeftVehicle() {
	//		return generalBuffer.isEmpty() && leftBuffer.isEmpty();
	//	}

	public abstract boolean isNotOfferingGeneralVehicle() ;

	public abstract boolean isNotOfferingLeftVehicle();



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

		for (QVehicleAndMoveType veh : newGeneralBuffer) {
			context.getEventsManager().processEvent( new VehicleAbortsEvent(now, veh.getId(), veh.getCurrentLink().getId()));
			context.getEventsManager().processEvent( new PersonStuckEvent(now, veh.getDriver().getId(), veh.getCurrentLink().getId(), veh.getDriver().getMode()));

			context.getAgentCounter().incLost();
			context.getAgentCounter().decLiving();
		}
		newGeneralBuffer.clear();
		clearLeftBuffer(now);

		holes.clear();
		this.remainingHolesStorageCapacity = this.storageCapacity;
	}

	protected abstract void clearLeftBuffer(double now);

	@Override
	public final void addFromUpstream(final QVehicle veh) {
		double now = context.getSimTimer().getTimeOfDay() ;

		if (this.context.qsimConfig.isUseLanes() ) {
			if (  hasMoreThanOneLane() ) {
				this.context.getEventsManager().processEvent(new LaneEnterEvent( now, veh.getId(), this.qLink.getId(), this.getId() ));
			}
		}

		// activate link since there is now action on it:
		qLink.activateLink();

		if(context.qsimConfig.isSeepModeStorageFree() && context.qsimConfig.getSeepModes().contains( veh.getVehicle().getType().getId().toString() ) ){
			// do nothing
		} else {
			usedStorageCapacity += veh.getSizeInEquivalents();
		}

		// compute and set earliest link exit time:
		//		double linkTravelTime = this.length / this.linkSpeedCalculator.getMaximumVelocity(veh, qLink.getLink(), now);
		double linkTravelTime = this.length / this.qLink.getMaximumVelocityFromLinkSpeedCalculator( veh, now ) ;
		linkTravelTime = context.qsimConfig.getTimeStepSize() * Math.floor( linkTravelTime / context.qsimConfig.getTimeStepSize() );

		veh.setEarliestLinkExitTime(now + linkTravelTime);

		// In theory, one could do something like
		//		final double discretizedEarliestLinkExitTime = timeStepSize * Math.ceil(veh.getEarliestLinkExitTime()/timeStepSize);
		//		double effectiveEntryTime = now - ( discretizedEarliestLinkExitTime - veh.getEarliestLinkExitTime() ) ;
		//		double earliestExitTime = effectiveEntryTime + linkTravelTime;
		// We decided against this since this would effectively move the simulation to operating on true floating point time steps.  For example,
		// events could then have arbitrary floating point values (assuming one would use the "effectiveEntryTime" also for the event).  
		// Also, it could happen that vehicles with an earlier link exit time could be 
		// inserted and thus end up after vehicles with a later link exit time.  theresa & kai, jun'14

		//		veh.setCurrentLink(qLink.getLink());
		this.qLink.setCurrentLinkToVehicle( veh ) ;
		vehQueue.add(veh);

		switch (context.qsimConfig.getTrafficDynamics()) {
		case queue:
			break;
		case withHoles:
			this.remainingHolesStorageCapacity -= veh.getSizeInEquivalents();
			break;
		case kinematicWaves:
			this.remainingHolesStorageCapacity -= veh.getSizeInEquivalents();
			this.accumulatedInflowCap -= getFlowCapacityConsumptionInEquivalents(veh, null, null) ;
			break;
		default: throw new RuntimeException("The traffic dynamics "+context.qsimConfig.getTrafficDynamics()+" is not implemented yet.");
		}
	}

	private boolean hasMoreThanOneLane() {
		return this.qLink.getAcceptingQLane() != this.qLink.getOfferingQLanes().get(0);
		// this works independent from sorting since if there is only one lane, then it has to be the one to be returned by
		// getOfferingQLanes().get(0), and it is also the same as the accepting QLane.  If, however, "lanes" is used,
		// there are at least two lanes in sequence, so the accepting lane is never the same as any of the offering lanes, and
		// this will always return false independent from sorting.  kai/theresa, dec'16
	}

	@Override
	public final QLaneI.VisData getVisData() {
		return this.visData  ;
	}

	@Override
	public final QVehicle getFirstVehicle() {
		// Deliberately not implemented.
		log.error("getFirstVehicle() deliberately not implemented as it may be ambigous");
		log.warn("getFirstVehicle() deliberately not implemented as it may be ambigous");
		return null;
	}

	//	public final QVehicle getFirstNonLeftVehicle2(QFFFNodeWithLeftBuffer node) {
	//		Iterator<QVehicle> it = this.generalBuffer.iterator();
	//		while(it.hasNext()) {
	//			QVehicle veh = it.next();
	//			if(!node.isCarLeftTurn(this.qLink.getId(), veh.getDriver().chooseNextLinkId())){
	//				it.remove();
	//				this.generalBufferLastMovedTime = context.getSimTimer().getTimeOfDay();
	//				return veh;
	//			}
	//		}
	//		return null;
	//	}

	public final QVehicleAndMoveType getFirstGeneralVehicle() {
		return this.newGeneralBuffer.peek() ;
	}
	public abstract QVehicleAndMoveType getFirstLeftVehicle();

	@Override
	public final double getLastMovementTimeOfFirstVehicle() {
		return getLastMovementTimeOfFirstGeneralVehicle();
	}

	private final double getLastMovementTimeOfFirstGeneralVehicle() {
		return this.generalBufferLastMovedTime ;
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


	class VisDataImpl implements QLaneI.VisData {
		private Coord upstreamCoord;
		private Coord downstreamCoord;

		@Override
		public final Collection<AgentSnapshotInfo> addAgentSnapshotInfo(Collection<AgentSnapshotInfo> positions, double now) {
			if ( !isLeftBufferEmpty() || !newGeneralBuffer.isEmpty() || !vehQueue.isEmpty() ||
					!holes.isEmpty() ) {
				Gbl.assertNotNull(positions);
				Gbl.assertNotNull( context.snapshotInfoBuilder );
				if ( this.upstreamCoord==null ) {
					this.upstreamCoord = qLink.getFromNode().getCoord() ;
				}
				if ( this.downstreamCoord==null ) {
					this.downstreamCoord = qLink.getToNode().getCoord() ;
				}
				// vehicle positions are computed in snapshotInfoBuilder as a service:
				positions = context.snapshotInfoBuilder.positionVehiclesAlongLine(
						positions,
						now,
						getAllVehicles(),
						length,
						storageCapacity + getBufferStorageCapacity(),
						this.upstreamCoord,
						this.downstreamCoord,
						inverseFlowCapacityPerTimeStep,
						qLink.getFreespeed(now),
						//						NetworkUtils.getNumberOfLanesAsInt(now, qLink.getLink()),
						qLink.getNumberOfLanesAsInt(now) ,
						holes
						);
			}
			return positions ;
		}

		void setVisInfo(Coord upstreamCoord, Coord downstreamCoord) {
			this.upstreamCoord = upstreamCoord;
			this.downstreamCoord = downstreamCoord;
		}
	}

	private int noOfSeepModeBringFwd = 0;

	private QVehicle peekFromVehQueue(){
		double now = context.getSimTimer().getTimeOfDay() ;

		QVehicle returnVeh = vehQueue.peek();

		if( context.qsimConfig.getLinkDynamics()==LinkDynamics.SeepageQ ) {

			int maxSeepModeAllowed = 4;
			if( context.qsimConfig.isRestrictingSeepage() && noOfSeepModeBringFwd == maxSeepModeAllowed) {
				noOfSeepModeBringFwd = 0;
				return returnVeh;
			}

			VehicleQ<QVehicle> newVehQueue = new PassingVehicleQ();
			newVehQueue.addAll(vehQueue);

			Iterator<QVehicle> it = newVehQueue.iterator();

			while(it.hasNext()){
				QVehicle veh = newVehQueue.poll();
				if( veh.getEarliestLinkExitTime()<=now && context.qsimConfig.getSeepModes().contains(veh.getDriver().getMode()) ) {
					returnVeh = veh;
					break;
				}
			}
		}
		return returnVeh;
	}

	protected abstract boolean isLeftBufferEmpty();

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

	 private double getFlowCapacityConsumptionInEquivalents(QVehicle vehicle, QVehicle prevVehicle, Double timeDiff) {
	        double flowEfficiency = flowEfficiencyCalculator.calculateFlowEfficiency(vehicle, prevVehicle, timeDiff, qLink.getLink(), id);
	        return vehicle.getSizeInEquivalents() / flowEfficiency;
	}

	public boolean isGeneralBufferEmpty() {
		return newGeneralBuffer.isEmpty();
	}

}
