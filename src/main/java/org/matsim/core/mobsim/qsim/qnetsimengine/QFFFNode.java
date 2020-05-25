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

import java.util.Arrays;
import java.util.Collection;
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
//import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.PassengerAgent;

import fastOrForcedToFollow.Cyclist;
import fastOrForcedToFollow.configgroups.FFFNodeConfigGroup;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngineI.NetsimInternalInterface;

/**
 * Represents a node supporting right of way at intersections.
 */
public final class QFFFNode extends AbstractQNode {

	public static enum MoveType {GENERAL, LEFT_TURN, PASSING_IMPATIENTLY_ON_THE_RIGHT};

	public static class Builder {
		private final NetsimInternalInterface netsimEngine;
		private final NetsimEngineContext context;
		private final FFFNodeConfigGroup fffNodeConfig;
		public Builder( NetsimInternalInterface netsimEngine2, NetsimEngineContext context,
				FFFNodeConfigGroup fffNodeConfig) {
			this.netsimEngine = netsimEngine2;
			this.context = context;
			this.fffNodeConfig = fffNodeConfig;
		}
		public QFFFNode build( Node n ) {
			return new QFFFNode( n, context, netsimEngine, fffNodeConfig) ;
		}
	}
	private static final Logger log = Logger.getLogger(QNodeImpl.class);

	private static int wrnCnt = 0 ;

	private final NetsimEngineContext context;

	private final NetsimInternalInterface netsimEngine;
	private QFFFAbstractNode nodeType;

	private final FFFNodeConfigGroup fffNodeConfig;

	protected QFFFNode(final Node n, NetsimEngineContext context, NetsimInternalInterface netsimEngine2, FFFNodeConfigGroup fffNodeConfig) {
		super(n);
		this.netsimEngine = netsimEngine2 ;
		this.context = context ;
		this.fffNodeConfig = fffNodeConfig;
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


	private void calculateHierarchyInformations(TreeMap<Double, LinkedList<Link>> thetaMap, 
			TreeMap<HierarchyInformation, LinkedList<Integer>> carHIs, TreeMap<HierarchyInformation, LinkedList<Integer>> bicycleHIs, 
			HashMap<String,Integer> roadValueMap) {

		int i = 0;
		for(LinkedList<Link> list : thetaMap.values()){
			HierarchyInformation highestCarHI = new HierarchyInformation(Integer.MIN_VALUE, 0.);
			HierarchyInformation highestBicycleHI = new HierarchyInformation(Integer.MIN_VALUE, 0.);

			for(Link link : list){
				String roadType = (String) link.getAttributes().getAttribute("type");
				int roadValue = roadValueMap.get( roadType );
				double capacity = link.getCapacity();
				HierarchyInformation hi = new HierarchyInformation(roadValue, capacity);
				if(link.getAllowedModes().contains(TransportMode.car)){
					if(hi.compareTo(highestCarHI) > 0){
						highestCarHI = hi;
					}
				} else if(link.getAllowedModes().contains(TransportMode.bike)){
					if(hi.compareTo(highestBicycleHI) > 0){
						highestBicycleHI = hi;
					}
				}
			}

			if(!carHIs.containsKey(highestCarHI)){
				carHIs.put(highestCarHI, new LinkedList<Integer>());
			}
			carHIs.get(highestCarHI).addLast(i);
			if(!bicycleHIs.containsKey(highestBicycleHI)){
				bicycleHIs.put(highestBicycleHI, new LinkedList<Integer>());
			}
			bicycleHIs.get(highestBicycleHI).addLast(i);
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
	//	@Override
	//	public Map<String, Object> getCustomAttributes() {
	//		return customAttributes;
	//	}



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

		//Cannot be tested later since the thetamaps are emptied during the creation of bundleMap.
		boolean isLargeRoadsMerging = false;
		Id<Link> largestOutLinkOfLargeRoadsDiverging = null;

		if(carOutThetaMap.size() == 1 && carInThetaMap.size() == 2
				&& bicycleOutThetaMap.size() == 0 && bicycleInThetaMap.size() == 0){

			// Determining the lowest valued link...
			int lowestRoadValue = Integer.MAX_VALUE;
			for(Collection<Link> links : Arrays.asList(carInThetaMap.values(), carOutThetaMap.values())) {
				for(Link link : links) {
					int value = fffNodeConfig.getRoadTypeToValueMap().get(link.getAttributes().getAttribute("type"));
					if(value < lowestRoadValue) {
						lowestRoadValue = value;
					}
				} 
			}
			//Only allow node type if smallest link is at least trunk_link.
			//	if(lowestRoadValue >= fffNodeConfig.getRoadTypeToValueMap().get("trunk_link")) {
			isLargeRoadsMerging = true;
			//	}
		} else if(carOutThetaMap.size() == 2 && carInThetaMap.size() == 1
				&& bicycleOutThetaMap.size() == 0 && bicycleInThetaMap.size() == 0) {
			double largestCapacity = -1;
			for(Link link : carInThetaMap.values()) {
				if(link.getCapacity() > largestCapacity) {
					largestCapacity = link.getCapacity();
					largestOutLinkOfLargeRoadsDiverging = link.getId();
				}
			}
		}


		TreeMap<Double, LinkedList<Link>> bundleMap = createBundleMap(bicycleOutThetaMap, carOutThetaMap, carInThetaMap,
				bicycleInThetaMap);


		if(isLargeRoadsMerging){
			this.nodeType = new QFFFLargeRoadsMergingNode(this, bundleMap, network);
			return;
		} else if(largestOutLinkOfLargeRoadsDiverging != null) {
			this.nodeType = new QFFFLargeRoadsDivergingNode(this, bundleMap, network, largestOutLinkOfLargeRoadsDiverging);
			return;
		}




		if(bundleMap.size() == 1){ // Determine if capacities are different: We now allow larger intersection types.
			this.nodeType = new QFFFRightPriorityNode(this, bundleMap, network);
			return;
		} else if(bundleMap.size() == 2){
			//Obviously fine when both bundles are proper bundles...
			this.nodeType = new QFFFNodeDirectedPriorityNode(this, bundleMap, network);
			return;
		} else {
			TreeMap<HierarchyInformation, LinkedList<Integer>> carHIs = new TreeMap<HierarchyInformation, LinkedList<Integer>>();
			TreeMap<HierarchyInformation, LinkedList<Integer>> bicycleHIs = new TreeMap<HierarchyInformation, LinkedList<Integer>>();

			calculateHierarchyInformations(bundleMap, carHIs, bicycleHIs, fffNodeConfig.getRoadTypeToValueMap());
			HierarchyInformation minimumHI = new HierarchyInformation(Integer.MIN_VALUE, 0.);

			boolean areCarHIsEqual = carHIs.tailMap(minimumHI).size() <= 1 &&
					!(carHIs.tailMap(minimumHI).size() == 1 &&  
					carHIs.ceilingEntry(minimumHI).getValue().size() <= 1);
			boolean areBicycleHIsEqual = bicycleHIs.tailMap(minimumHI).size() <= 1 &&
					!(bicycleHIs.tailMap(minimumHI).size() == 1 &&  
					bicycleHIs.ceilingEntry(minimumHI).getValue().size() <= 1);


			if(areBicycleHIsEqual && areCarHIsEqual){
				this.nodeType = new QFFFRightPriorityNode(this, bundleMap, network);
				return;
			} else {
				TreeMap<HierarchyInformation, LinkedList<Integer>> his = carHIs;
				if(areCarHIsEqual){
					his = bicycleHIs;
				}
				int largestHIs = his.lastEntry().getValue().size();
				if(largestHIs == 1){
					SortedMap<HierarchyInformation, LinkedList<Integer>> headMap = his.headMap(his.lastKey());
					largestHIs += headMap.get(headMap.lastKey()).size();
				}
				if(largestHIs == 2 || (largestHIs == 3 && bundleMap.size() == 4) ){
					this.nodeType = new QFFFNodeDirectedPriorityNode(this, bundleMap, network, his);
					return;
				} else {
					this.nodeType = new QFFFAntiPriorityNode(this, bundleMap, network, his);		
					return;
				}
			}
		} 
	}

	//	// TOBEDELETED
	//	final boolean isActive() {
	//		// yyyy I cannot say if this needs to be in QNodeI or not.  The mechanics of this are tricky to implement, so it would
	//		// not be a stable/robust API.  kai, jul'17
	//		return this.active.get();
	//	}

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

		// Because we only care about linkentries in this RoW setup, we do not need a linkLeaveEvent. Mads
		if(!fffNodeConfig.getOmitLinkLeaveEvents()) {
			this.context.getEventsManager().processEvent(new LinkLeaveEvent(now, veh.getId(), currentLinkId));
		}
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


	private void moveVehicleFromInlinkToOutlink(final QVehicle veh, Id<Link> currentLinkId, final QLaneI fromLane, 
			Id<Link> nextLinkId, QLaneI nextQueueLane, MoveType moveType) {

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

		if(moveType == MoveType.GENERAL) {
			fromLane.popFirstVehicle();
		} else if(moveType == MoveType.LEFT_TURN) {
			((QueueWithBufferForRoW) fromLane).popFirstLeftVehicle();
		} // else if( moveType == MoeveType.PASSING_IMPATIENTLY_ON_THE_RIGHT { //Passing on the right pops beforehand! }

		// -->
		//		network.simEngine.getMobsim().getEventsManager().processEvent(new LaneLeaveEvent(now, veh.getId(), currentLinkId, fromLane.getId()));

		// Because we only care about linkentries in this RoW setup, we do not need a linkLeaveEvent. Mads
		if(!fffNodeConfig.getOmitLinkLeaveEvents()) {
			this.context.getEventsManager().processEvent(new LinkLeaveEvent(now, veh.getId(), currentLinkId));
		}

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



	void moveVehicleFromInlinkToOutlink( final QVehicle veh, QLinkI fromLink, final QLaneI fromLane,
			final double now, MoveType moveType) {
		Id<Link> nextLinkId = veh.getDriver().chooseNextLinkId();
		Link currentLink = veh.getCurrentLink();   // Takes it from QVehicle, so temporary link does not enter here...
		QLinkI nextQueueLink = this.netsimEngine.getNetsimNetwork().getNetsimLinks().get(nextLinkId);
		QLaneI	nextQueueLane = nextQueueLink.getAcceptingQLane() ;
		moveVehicleFromInlinkToOutlink(veh, currentLink.getId(), fromLane, nextLinkId, nextQueueLane, moveType);
	}




	protected boolean moveVehicleOverNode( final QVehicle veh, QLinkI fromLink, final QLaneI fromLane,
			final double now, MoveType moveType, boolean stuckReturnValue) {

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
		QLaneI	nextQueueLane = nextQueueLink.getAcceptingQLane() ;

		if (nextQueueLane.isAcceptingFromUpstream()) {
			moveVehicleFromInlinkToOutlink(veh, currentLink.getId(), fromLane, nextLinkId, nextQueueLane, moveType);
			return true;
		}


		boolean vehicleIsStuck = vehicleIsStuck(fromLane, now, moveType); 
		if (vehicleIsStuck) {
			/* We just push the vehicle further after stucktime is over, regardless
			 * of if there is space on the next link or not.. optionally we let them
			 * die here, we have a config setting for that!
			 */
				moveVehicleFromInlinkToOutlink(veh, currentLink.getId(), fromLane, nextLinkId, nextQueueLane, moveType);
				return stuckReturnValue;
				// (yyyy why is this returning `true'?  Since this is a fix to avoid gridlock, this should proceed in small steps. 
				// kai, feb'12) 
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

	//	/**
	//	 * The ParallelQSim replaces the activator with the QSimEngineRunner
	//	 * that handles this node.
	//	 */
	//	public void setNetElementActivationRegistry(NetElementActivationRegistry activator) {
	//		// yyyy I cannot say if this needs to be in QNodeI or not.  The mechanics of this are tricky to implement, so it would
	//		// not be a stable/robust API.  kai, jul'17
	//
	//		this.activator = activator;
	//	}

	boolean vehicleIsStuck(final QLaneI fromLaneBuffer, final double now, MoveType moveType) {
		if(moveType == MoveType.LEFT_TURN) {
			return leftVehicleIsStuck( (QueueWithBufferForRoW) fromLaneBuffer, now);
		} else {
			return generalVehicleIsStuck(fromLaneBuffer, now);
		}
	}

	private boolean leftVehicleIsStuck(final QueueWithBufferForRoW fromLaneBuffer, final double now) {
		final double stuckTime = this.context.qsimConfig.getStuckTime() ;
		return (now - fromLaneBuffer.getLastMovementTimeOfFirstLeftVehicle()) > stuckTime;
	}

	private boolean generalVehicleIsStuck(final QLaneI fromLaneBuffer, final double now) {
		final double stuckTime = this.context.qsimConfig.getStuckTime() ;
		return (now - fromLaneBuffer.getLastMovementTimeOfFirstVehicle()) > stuckTime;
	}



	FFFNodeConfigGroup getFFFNodeConfig(){
		return this.fffNodeConfig;
	}






	public QFFFAbstractNode getQFFFAbstractNode(){
		return nodeType;
	}

}
