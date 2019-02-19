/* *********************************************************************** *
 * project: org.matsim.*
 * QueueNode.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.PassengerAgent;
import org.matsim.core.mobsim.qsim.qnetsimengine.DefaultTurnAcceptanceLogic;
import org.matsim.core.mobsim.qsim.qnetsimengine.NetsimEngineContext;
import org.matsim.core.mobsim.qsim.qnetsimengine.QLaneI;
import org.matsim.core.mobsim.qsim.qnetsimengine.QLinkI;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngine.NetsimInternalInterface;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetwork;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNodeI;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.core.mobsim.qsim.qnetsimengine.TurnAcceptanceLogic;
import org.matsim.core.mobsim.qsim.qnetsimengine.TurnAcceptanceLogic.AcceptTurn;

import fastOrForcedToFollow.Cyclist;
import fastOrForcedToFollow.configgroups.FFFNodeConfigGroup;

/**
 * Represents a node in the QSimulation.
 */
final class QFFFNode implements QNodeI {
	public static class Builder {
		private final NetsimInternalInterface netsimEngine;
		private final NetsimEngineContext context;
		private TurnAcceptanceLogic turnAcceptanceLogic = new DefaultTurnAcceptanceLogic() ;
		private final FFFNodeConfigGroup fffNodeConfig;
		public Builder( NetsimInternalInterface netsimEngine2, NetsimEngineContext context,
				FFFNodeConfigGroup fffNodeConfig) {
			this.netsimEngine = netsimEngine2;
			this.context = context;
			this.fffNodeConfig = fffNodeConfig;
		}
		public QFFFNode build( Node n ) {
			return new QFFFNode( n, context, netsimEngine, turnAcceptanceLogic, fffNodeConfig) ;
		}
		public final void setTurnAcceptanceLogic( TurnAcceptanceLogic turnAcceptanceLogic ) {
			this.turnAcceptanceLogic = turnAcceptanceLogic ;
		}
	}
	private static final Logger log = Logger.getLogger(QNodeImpl.class);

	private static int wrnCnt = 0 ;

	/*
	 * This needs to be atomic since this allows us to ensure that an node which is
	 * already active is not activated again. This could happen if multiple thread call
	 * activateNode() concurrently.
	 * cdobler, sep'14
	 */
	final AtomicBoolean active = new AtomicBoolean(false);

	private final Node node;

	// necessary if Nodes are (de)activated
	private NetElementActivationRegistry activator = null;

	// for Customizable
	private final Map<String, Object> customAttributes = new HashMap<>();
	private final NetsimEngineContext context;

	private final NetsimInternalInterface netsimEngine; 
	private QFFFAbstractNode nodeType;

	private final TurnAcceptanceLogic turnAcceptanceLogic ;

	private final FFFNodeConfigGroup fffNodeConfig;

	protected QFFFNode(final Node n, NetsimEngineContext context, NetsimInternalInterface netsimEngine2,
			TurnAcceptanceLogic turnAcceptanceLogic, FFFNodeConfigGroup fffNodeConfig) {
		this.node = n;
		this.netsimEngine = netsimEngine2 ;
		this.context = context ;
		this.turnAcceptanceLogic = turnAcceptanceLogic;
		this.fffNodeConfig = fffNodeConfig;
	}

	/**
	 * This method is called from QueueWithBuffer.addToBuffer(...) which is triggered at 
	 * some placed, but always initially by a QLink's doSomStep(...) method. I.e. QNodes
	 * are only activated while moveNodes(...) is performed. However, multiple threads
	 * could try to activate the same node at a time, therefore this has to be thread-safe.
	 * cdobler, sep'14 
	 */
	/*package*/ final void activateNode() {
		// yyyy I cannot say if this needs to be in QNodeI or not.  The mechanics of this are tricky to implement, so it would 
		// not be a stable/robust API.  kai, jul'17

		/*
		 * this.active.compareAndSet(boolean expected, boolean update)
		 * We expect the value to be false, i.e. the node is de-activated. If this is
		 * true, the value is changed to true and the activator is informed.
		 */

		if (this.active.compareAndSet(false, true)) {
			this.activator.registerNodeAsActive(this);
		}
	}


	private double bundleEntryIfPossible(TreeMap<Double, LinkedList<Link>> bundleMap,
			TreeMap<Double, Link> thisThetaMap,	LinkedList<TreeMap<Double, Link>> otherThetaMaps,
			Entry<Double, Link> baseEntry, double thetaRef) {

		if(!thisThetaMap.isEmpty()){

			Entry<Double, Link> thisEntry = thisThetaMap.ceilingEntry(thetaRef);
			double thisTheta = Double.NaN;
			if(thisEntry == null){
				thisEntry = thisThetaMap.firstEntry();
				if(thisEntry != null){
					thisTheta = thisEntry.getKey() + 2*Math.PI;
				}
			} else {	
				thisTheta = thisEntry.getKey();
			}
			if(thisEntry != null){
				if(thisTheta == thetaRef){
					bundleMap.get(baseEntry.getKey()).addFirst(thisEntry.getValue());
					thisThetaMap.remove(thisEntry.getKey());		
				} else if( QFFFNodeUtils.calculatePositiveThetaDif(thisTheta, thetaRef) < 
						fffNodeConfig.getBundleTol()){
					for(TreeMap<Double, Link> map : otherThetaMaps){
						if( numberOfKeysFromInclToExcl(map, thetaRef, thisTheta) > 0){
							return thetaRef;
						}
					}
					bundleMap.get(baseEntry.getKey()).addFirst(thisEntry.getValue());
					thisThetaMap.remove(thisEntry.getKey());
					return thisTheta;
				}
			} 
		}
		return thetaRef;
	}


	private void calculateCapacities(TreeMap<Double, LinkedList<Link>> thetaMap,
			TreeMap<Double, LinkedList<Integer>> carCapacities, TreeMap<Double, LinkedList<Integer>> bicycleCapacities) {
		int i = 0;
		for(LinkedList<Link> list : thetaMap.values()){

			double highestCarCapacity = 0;
			double highestBicycleCapacity = 0;

			for(Link link : list){
				if(link.getAllowedModes().contains(TransportMode.car)){
					double capacity = link.getCapacity();
					if(capacity > highestCarCapacity){
						highestCarCapacity = capacity;
					}
				} else if(link.getAllowedModes().contains(TransportMode.bike)){
					double capacity = link.getNumberOfLanes();
					if(capacity > highestBicycleCapacity){
						highestBicycleCapacity = capacity;
					}
				}
			}

			if(!carCapacities.containsKey(highestCarCapacity)){
				carCapacities.put(highestCarCapacity, new LinkedList<Integer>());
			}
			carCapacities.get(highestCarCapacity).addLast(i);
			if(!bicycleCapacities.containsKey(highestBicycleCapacity)){
				bicycleCapacities.put(highestBicycleCapacity, new LinkedList<Integer>());
			}
			bicycleCapacities.get(highestBicycleCapacity).addLast(i);
			i++;
		}
	}


	private TreeMap<Double, LinkedList<Link>> createBundleMap(TreeMap<Double, Link> bicycleOutThetaMap,
			TreeMap<Double, Link> carOutThetaMap, TreeMap<Double, Link> carInThetaMap,
			TreeMap<Double, Link> bicycleInThetaMap) {

		LinkedList<TreeMap<Double, Link>> thetaMaps = new LinkedList<TreeMap<Double, Link>>();
		thetaMaps.addFirst(bicycleOutThetaMap); 
		thetaMaps.addLast(carOutThetaMap); 
		thetaMaps.addLast(carInThetaMap);
		thetaMaps.addLast(bicycleInThetaMap);


		TreeMap<Double, LinkedList<Link>> bundleMap = new TreeMap<Double, LinkedList<Link>>();

		while(!thetaMaps.isEmpty()){
			TreeMap<Double, Link> baseThetaMap = thetaMaps.pollFirst();
			while(! baseThetaMap.isEmpty()){
				Entry<Double,Link> baseEntry = baseThetaMap.pollFirstEntry();

				double thetaRef = baseEntry.getKey();
				bundleMap.put(thetaRef, new LinkedList<Link>());
				bundleMap.get(thetaRef).add(baseEntry.getValue());

				for(int i = 0; i < thetaMaps.size(); i++){
					TreeMap<Double, Link> thisThetaMap = thetaMaps.pollFirst();
					thetaRef = bundleEntryIfPossible(bundleMap, thisThetaMap, thetaMaps, baseEntry, thetaRef);
					thetaMaps.addLast(thisThetaMap);
				}
			}
		}

		return bundleMap;
	}


	// thetaMap is now a compressed map of all the (up to 8) "directions" of this node



	@Override
	public boolean doSimStep(double now) {
		return nodeType.doSimStep(now);
	}
	@Override
	public Map<String, Object> getCustomAttributes() {
		return customAttributes;
	}






	@Override
	public Node getNode() {
		return this.node;
	}

	/**
	 * Loads the inLinks-array with the corresponding links.
	 * Cannot be called in constructor, as the queueNetwork does not yet know
	 * the queueLinks. Should be called by QueueNetwork, after creating all
	 * QueueNodes and QueueLinks.
	 */
	@Override
	public void init() {

		// Determining the nodeType.
		QNetwork network = netsimEngine.getNetsimNetwork();

		TreeMap<Double, Link> carInThetaMap = new TreeMap<Double, Link>();
		TreeMap<Double, Link> bicycleInThetaMap = new TreeMap<Double, Link>();
		TreeMap<Double, Link> carOutThetaMap = new TreeMap<Double, Link>();
		TreeMap<Double, Link> bicycleOutThetaMap = new TreeMap<Double, Link>();

		for(Link link : this.getNode().getInLinks().values()){
			double theta = QFFFNodeUtils.calculateTheta(link);
			if(link.getAllowedModes().contains(TransportMode.car)){
				Gbl.assertIf(!carInThetaMap.containsKey(theta));
				carInThetaMap.put(theta, link);
			} else if(link.getAllowedModes().contains(TransportMode.bike)){
				if(bicycleInThetaMap.containsKey(theta)){
					System.out.println(bicycleInThetaMap.get(theta).getFromNode().getId());
					System.out.println(link.getFromNode().getId());
				}
				Gbl.assertIf(!bicycleInThetaMap.containsKey(theta));
				bicycleInThetaMap.put(theta, link);
			}
		}
		for(Link link : this.getNode().getOutLinks().values()){
			double theta = QFFFNodeUtils.calculateInverseTheta(link);
			if(link.getAllowedModes().contains(TransportMode.car)){
				Gbl.assertIf(!carOutThetaMap.containsKey(theta));
				carOutThetaMap.put(theta, link);
			} else if(link.getAllowedModes().contains(TransportMode.bike)){
				Gbl.assertIf(!bicycleOutThetaMap.containsKey(theta));
				bicycleOutThetaMap.put(theta, link);
			}
		}

		TreeMap<Double, LinkedList<Link>> bundleMap = createBundleMap(bicycleOutThetaMap, carOutThetaMap, carInThetaMap,
				bicycleInThetaMap);

		if(bundleMap.size() >= 3){ // Determine if capacities are different: We now allow larger intersection types.

			TreeMap<Double, LinkedList<Integer>> carCapacities = new TreeMap<Double, LinkedList<Integer>>();
			TreeMap<Double, LinkedList<Integer>> bicycleCapacities = new TreeMap<Double, LinkedList<Integer>>();
			calculateCapacities(bundleMap, carCapacities, bicycleCapacities);


			boolean areCarCapacitiesEqual = carCapacities.tailMap(Double.MIN_VALUE).size() <= 1 &&
					!(carCapacities.tailMap(Double.MIN_VALUE).size() == 1 &&  
					carCapacities.ceilingEntry(Double.MIN_VALUE).getValue().size() <= 1);
			boolean areBicycleCapacitiesEqual =	 bicycleCapacities.tailMap(Double.MIN_VALUE).size() <= 1 &&
					!(bicycleCapacities.tailMap(Double.MIN_VALUE).size() == 1 &&  
					bicycleCapacities.ceilingEntry(Double.MIN_VALUE).getValue().size() <= 1);





			if(areBicycleCapacitiesEqual && areCarCapacitiesEqual){
				if(carCapacities.lastEntry().getValue().size() == 1 ){
					// too small...
					System.out.println("This is a problem, that I haven't thought about...");
					System.exit(-1);
				} else {
					this.nodeType = new QFFFRightPriorityNode(this, bundleMap, network);
				} 
			} else {
				TreeMap<Double, LinkedList<Integer>> capacities = carCapacities;
				if(areCarCapacitiesEqual){
					capacities = bicycleCapacities;
				}
				int largestCapacities = capacities.lastEntry().getValue().size();
				if(largestCapacities == 1){
					SortedMap<Double, LinkedList<Integer>> headMap = capacities.headMap(capacities.lastKey());
					largestCapacities += headMap.get(headMap.lastKey()).size();
				}
				if(largestCapacities == 2 || (largestCapacities == 3 && bundleMap.size() == 4) ){
					this.nodeType = new QFFFNodeDirectedPriorityNode(this, bundleMap, network, capacities);
				} else {
					this.nodeType = new QFFFAntiPriorityNode(this, bundleMap, network, capacities);		
				}
			}
		} else {
			this.nodeType = new QFFFRightPriorityNode(this, bundleMap, network);
		}
	}

	final boolean isActive() {
		// yyyy I cannot say if this needs to be in QNodeI or not.  The mechanics of this are tricky to implement, so it would 
		// not be a stable/robust API.  kai, jul'17
		return this.active.get();
	}

	/**
	 * Moves vehicles from the inlinks' buffer to the outlinks where possible.<br>
	 * The inLinks are randomly chosen, and for each link all vehicles in the
	 * buffer are moved to their desired outLink as long as there is space. If the
	 * front most vehicle in a buffer cannot move across the node because there is
	 * no free space on its destination link, the work on this inLink is finished
	 * and the next inLink's buffer is handled (this means, that at the node, all
	 * links have only like one lane, and there are no separate lanes for the
	 * different outLinks. Thus if the front most vehicle cannot drive further,
	 * all other vehicles behind must wait, too, even if their links would be
	 * free).
	 *
	 * @param now
	 *          The current time in seconds from midnight.
	 * @return
	 * 		Whether the QNode stays active or not.
	 */





	private void moveVehicleFromInlinkToAbort(final QVehicle veh, final QLaneI fromLane, final double now, Id<Link> currentLinkId) {
		fromLane.popFirstVehicle();
		// -->
		this.context.getEventsManager().processEvent(new LinkLeaveEvent(now, veh.getId(), currentLinkId));
		// <--

		// first treat the passengers:
		for ( PassengerAgent pp : veh.getPassengers() ) {
			if ( pp instanceof MobsimAgent ) {
				((MobsimAgent)pp).setStateToAbort(now);
				netsimEngine.arrangeNextAgentState((MobsimAgent)pp) ;
			} else if ( wrnCnt < 1 ) {
				wrnCnt++ ;
				log.warn("encountering PassengerAgent that cannot be cast into a MobsimAgent; cannot say if this is a problem" ) ;
				log.warn(Gbl.ONLYONCE) ;
			}
		}

		// now treat the driver:
		veh.getDriver().setStateToAbort(now) ;
		netsimEngine.arrangeNextAgentState(veh.getDriver()) ;

	}


	private void moveVehicleFromInlinkToOutlink(final QVehicle veh, Id<Link> currentLinkId, final QLaneI fromLane, Id<Link> nextLinkId, QLaneI nextQueueLane) {

		double now = this.context.getSimTimer().getTimeOfDay();

		if(veh instanceof QCycle){
			Cyclist cyclist = ((QCycle) veh).getCyclist();
			double tEarliestExit = cyclist.getTEarliestExit();

			// Delays might occur at intersections... These are not captured otherwise (e.g. by tReady).
			double stepSize = context.getSimTimer().getSimTimestepSize();
			if(now > tEarliestExit + 2 * stepSize){
				double delayInStepSizes = now - (Math.ceil(tEarliestExit/stepSize) + 1) * stepSize;
				cyclist.setTEarliestExit(tEarliestExit + delayInStepSizes);
			}
		}

		fromLane.popFirstVehicle();
		// -->
		//		network.simEngine.getMobsim().getEventsManager().processEvent(new LaneLeaveEvent(now, veh.getId(), currentLinkId, fromLane.getId()));

		this.context.getEventsManager().processEvent(new LinkLeaveEvent(now, veh.getId(), currentLinkId));
		// <--

		veh.getDriver().notifyMoveOverNode( nextLinkId );

		// -->
		this.context.getEventsManager().processEvent(new LinkEnterEvent(now, veh.getId(), nextLinkId ));

		// <--
		nextQueueLane.addFromUpstream(veh);
	}
	// ////////////////////////////////////////////////////////////////////
	// Queue related movement code
	// ////////////////////////////////////////////////////////////////////
	/**
	 * @return <code>true</code> if the vehicle was successfully moved over the node, <code>false</code>
	 * otherwise (e.g. in case where the next link is jammed)
	 */
	protected boolean moveVehicleOverNode( final QVehicle veh, QLinkI fromLink, final QLaneI fromLane, final double now ) {
		Id<Link> nextLinkId = veh.getDriver().chooseNextLinkId();
		Link currentLink = veh.getCurrentLink();   // Takes it from QVehicle, so temporary link does not enter here...

//		AcceptTurn turn = turnAcceptanceLogic.isAcceptingTurn(currentLink, fromLane, nextLinkId, veh, this.netsimEngine.getNetsimNetwork(), now);
//		if ( turn.equals(AcceptTurn.ABORT) ) {
//			moveVehicleFromInlinkToAbort( veh, fromLane, now, currentLink.getId() ) ;
//			return true ;
//		} else if ( turn.equals(AcceptTurn.WAIT) ) {
//			return false;
//		}

		QLinkI nextQueueLink = this.netsimEngine.getNetsimNetwork().getNetsimLinks().get(nextLinkId);
		QLaneI nextQueueLane = nextQueueLink.getAcceptingQLane() ;
		if (nextQueueLane.isAcceptingFromUpstream()) {
			moveVehicleFromInlinkToOutlink(veh, currentLink.getId(), fromLane, nextLinkId, nextQueueLane);
			return true;
		}

		if (vehicleIsStuck(fromLane, now)) {
			/* We just push the vehicle further after stucktime is over, regardless
			 * of if there is space on the next link or not.. optionally we let them
			 * die here, we have a config setting for that!
			 */
			if (this.context.qsimConfig.isRemoveStuckVehicles()) {
				moveVehicleFromInlinkToAbort(veh, fromLane, now, currentLink.getId());
				return false ;
			} else {
				moveVehicleFromInlinkToOutlink(veh, currentLink.getId(), fromLane, nextLinkId, nextQueueLane);
				return true;
				// (yyyy why is this returning `true'?  Since this is a fix to avoid gridlock, this should proceed in small steps. 
				// kai, feb'12) 
			}
		}

		return false;

	}
	
	private int numberOfKeysFromInclToExcl(TreeMap<Double,Link> thetaMap, double lowerInclBound, double upperExclBound){
		Entry<Double, Link> entry = thetaMap.ceilingEntry(lowerInclBound);
		if(entry == null){
			return 0;
		}
		int count = 0;
		while(entry.getKey() < upperExclBound){
			count++;
			entry = thetaMap.higherEntry(entry.getKey());
			if(entry == null){
				return count;
			}
		}
		return count; 
	}

	/**
	 * The ParallelQSim replaces the activator with the QSimEngineRunner 
	 * that handles this node.
	 */
	/*package*/ void setNetElementActivationRegistry(NetElementActivationRegistry activator) {
		// yyyy I cannot say if this needs to be in QNodeI or not.  The mechanics of this are tricky to implement, so it would 
		// not be a stable/robust API.  kai, jul'17

		this.activator = activator;
	}

	private boolean vehicleIsStuck(final QLaneI fromLaneBuffer, final double now) {
		//		final double stuckTime = network.simEngine.getStuckTime();
		final double stuckTime = this.context.qsimConfig.getStuckTime() ;
		return (now - fromLaneBuffer.getLastMovementTimeOfFirstVehicle()) > stuckTime;
	}



	FFFNodeConfigGroup getFFFNodeConfig(){
		return this.fffNodeConfig;
	}

	
	protected boolean moveCarPassingOnTheRightOverNode( final QVehicle veh, QLinkI fromLink, final QLaneI fromLane, final double now ) {
		Id<Link> nextLinkId = veh.getDriver().chooseNextLinkId();
		Link currentLink = veh.getCurrentLink();   // Takes it from QVehicle, so temporary link does not enter here...

		QLinkI nextQueueLink = this.netsimEngine.getNetsimNetwork().getNetsimLinks().get(nextLinkId);
		QLaneI nextQueueLane = nextQueueLink.getAcceptingQLane() ;
		if (nextQueueLane.isAcceptingFromUpstream()) {
			moveCarPassingOnTheRightFromInlinkToOutlink(veh, currentLink.getId(), fromLane, nextLinkId, nextQueueLane);
			return true;
		}

		if (vehicleIsStuck(fromLane, now)) {
			if (this.context.qsimConfig.isRemoveStuckVehicles()) {
				moveVehicleFromInlinkToAbort(veh, fromLane, now, currentLink.getId());
				return false ;
			} else {
				moveCarPassingOnTheRightFromInlinkToOutlink(veh, currentLink.getId(), fromLane, nextLinkId, nextQueueLane);
				return true;
			}
		}

		return false;
	}
	
	private void moveCarPassingOnTheRightFromInlinkToOutlink(final QVehicle veh, Id<Link> currentLinkId, final QLaneI fromLane, Id<Link> nextLinkId, QLaneI nextQueueLane) {

		double now = this.context.getSimTimer().getTimeOfDay();

		// The vehicle is popped at a later stage...
		// -->
		//		network.simEngine.getMobsim().getEventsManager().processEvent(new LaneLeaveEvent(now, veh.getId(), currentLinkId, fromLane.getId()));

		this.context.getEventsManager().processEvent(new LinkLeaveEvent(now, veh.getId(), currentLinkId));
		// <--

		veh.getDriver().notifyMoveOverNode( nextLinkId );

		// -->
		this.context.getEventsManager().processEvent(new LinkEnterEvent(now, veh.getId(), nextLinkId ));

		// <--
		nextQueueLane.addFromUpstream(veh);
	}


}
