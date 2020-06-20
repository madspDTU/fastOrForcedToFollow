package org.matsim.core.mobsim.qsim.qnetsimengine;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.qsim.qnetsimengine.QFFFNode.MoveType;
import org.matsim.core.mobsim.qsim.qnetsimengine.QLaneI;
import org.matsim.core.mobsim.qsim.qnetsimengine.QLinkI;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetwork;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;

import fastOrForcedToFollow.timeoutmodifiers.TimeoutModifier;

public abstract class QFFFAbstractNode { //Interface or abstract

	static final boolean defaultStuckReturnValue = true;
	protected static final boolean stuckChangesOrderAtRightPriority = true;

	//static final boolean allowFreeingGeneralVehiclesStuckBehindLeftBuffer = false;
	static final boolean allowStuckedVehiclesToMoveDespieConflictingMoves = false;
	protected static final boolean timeoutChangesAtRightPriorityWhenStuckedVehiclesMoveDespiteConflictingMoves = false;
	static final boolean defaultTimeoutBehaviourWhenStuckedVehiclesMoveDespiteConflictingMoves = false;

	//	static final boolean letVehiclesFreedFromBehindLeftVehiclesAlterTimeouts = false;
	public static final int smallRoadLeftBufferCapacity = 2;
	
	


	final Random random;
	final QFFFNode qNode;
	final QLinkI[] carInLinks;
	final HashMap<Id<Link>, Integer> carOutTransformations;
	final QLinkI[] bicycleInLinks;
	final HashMap<Id<Link>, Integer> bicycleOutTransformations;
	double[][] carTimeouts;
	double[][] bicycleTimeouts;
	protected int[][] bicycleTurns;



	protected QFFFAbstractNode(final QFFFNode qNode, final TreeMap<Double, LinkedList<Link>> bundleMap, final QNetwork qNetwork){		
		final int n = bundleMap.size();
		this.qNode = qNode;
		this.carInLinks = new QLinkI[n];
		this.carOutTransformations = new HashMap<Id<Link>, Integer>();
		this.bicycleInLinks = new QLinkI[n];
		this.bicycleOutTransformations = new HashMap<Id<Link>, Integer>();
		for(int i = 0; bundleMap.size()>0; i++){
			LinkedList<Link> list = bundleMap.pollFirstEntry().getValue();
			for(Link link : list){
				QLinkI qLink = qNetwork.getNetsimLink(link.getId());
				Id<Node> id = qNode.getNode().getId();
				if(link.getToNode().getId().equals(id)){
					if( link.getAllowedModes().contains(TransportMode.car)){
						this.carInLinks[i] = qLink;
					} else if (link.getAllowedModes().contains(TransportMode.bike)){
						this.bicycleInLinks[i] = qLink;
					}
				}
				if(link.getFromNode().getId().equals(id)){
					if( link.getAllowedModes().contains(TransportMode.car)){
						carOutTransformations.put(qLink.getLink().getId(), i);
					} else if (link.getAllowedModes().contains(TransportMode.bike)){
						bicycleOutTransformations.put(qLink.getLink().getId(), i);
					}
				}
			}
		}
		this.carTimeouts = new double[n][n];
		this.bicycleTimeouts = new double[n][n];
		this.random = MatsimRandom.getLocalInstance();
	}


	abstract boolean doSimStep(final double now);


	protected double getNowPlusDelayBicycle(final double now){
		return now + qNode.getFFFNodeConfig().getBicycleDelay();
	}

	protected double getNowPlusDelayCar(final double now){
		return now + qNode.getFFFNodeConfig().getCarDelay();
	}

	/*
	protected boolean bicycleMoveWithFullLeftTurns(final int inDirection, final double now, 
			final double nowish, TimeoutModifier timeoutModifier) {
		boolean returnValue = false;
		QLinkI inLink = bicycleInLinks[inDirection];
		if(inLink != null) {
			for(QLaneI lane : inLink.getOfferingQLanes()){
				while(! lane.isNotOfferingVehicle()){
					QVehicle veh = lane.getFirstVehicle();
					Id<Link> nextLinkId = veh.getDriver().chooseNextLinkId();	
					int outDirection = bicycleOutTransformations.get(nextLinkId);
					if(bicycleTimeouts[inDirection][outDirection] <= now){
						//Ignoring left turns when using right priority.
						if (! this.qNode.moveVehicleOverNode(veh, inLink, lane, now,  MoveType.GENERAL, defaultStuckReturnValue )) {
							break;
						}
						timeoutModifier.updateTimeouts(bicycleTimeouts, carTimeouts, 
								inDirection, outDirection, null, nowish);
						returnValue = true;
					} else {
						if(allowStuckedVehiclesToMoveDespieConflictingMoves && this.qNode.vehicleIsStuck(lane, now, MoveType.GENERAL)) {
							this.qNode.moveVehicleFromInlinkToOutlink(veh, inLink, lane, now, MoveType.GENERAL);
							if(defaultTimeoutBehaviourWhenStuckedVehiclesMoveDespiteConflictingMoves) {
								timeoutModifier.updateTimeouts(bicycleTimeouts, carTimeouts, 
										inDirection, outDirection, null, nowish);
								continue;
							}
						}
						break;
					}
				}
			}
		}
		return returnValue;
	}
	*/

	protected boolean carMoveAllowingLeftTurns(final int inDirection, final double now, 
			final double nowish, TimeoutModifier timeoutModifier) {
		boolean returnValue = false;
		QLinkI inLink = carInLinks[inDirection];
		if(inLink != null){
			for(QLaneI lane : inLink.getOfferingQLanes()){
				while(! lane.isNotOfferingVehicle()){
					QVehicle veh = lane.getFirstVehicle();
					Id<Link> nextLinkId = veh.getDriver().chooseNextLinkId();
					int outDirection = carOutTransformations.get(nextLinkId);
					if(carTimeouts[inDirection][outDirection] <= now){
						//Ignoring left turns when using right priority.
						if (! this.qNode.moveVehicleOverNode(veh, inLink, lane, now, MoveType.GENERAL, stuckChangesOrderAtRightPriority)) {
							break;
						}
						timeoutModifier.updateTimeouts(bicycleTimeouts, carTimeouts, 
								inDirection, outDirection, null, nowish);
						returnValue = true;
						continue;
					} else {
						if(allowStuckedVehiclesToMoveDespieConflictingMoves && this.qNode.vehicleIsStuck(lane, now, MoveType.GENERAL)) {
							this.qNode.moveVehicleFromInlinkToOutlink(veh, inLink, lane, now, MoveType.GENERAL);
							if(timeoutChangesAtRightPriorityWhenStuckedVehiclesMoveDespiteConflictingMoves) {
								timeoutModifier.updateTimeouts(bicycleTimeouts, carTimeouts, 
										inDirection, outDirection, null, nowish);
								continue;
							}
						}
						break;
					}
				}
			}
		}
		return returnValue;
	}




	protected int getBicycleOutDirection(Id<Link> nextLinkId, QVehicle veh) {
		if(bicycleOutTransformations.containsKey(nextLinkId)) {
			return bicycleOutTransformations.get(nextLinkId);
		} else {
			return crashReport(nextLinkId, veh);
		}
	}



	protected int decreaseInt(int i){
		return QFFFNodeUtils.decreaseInt(i, carInLinks.length);
	}
	protected int increaseInt(int i){
		return QFFFNodeUtils.increaseInt(i, carInLinks.length);
	}




	protected boolean carMovesRightPriority(final int inDirection, final double now, 
			final double nowish, TimeoutModifier timeoutModifier) {
		boolean returnValue = false;
		QLinkI inLink = carInLinks[inDirection];
		if(inLink != null){
			for(QLaneI qLaneI : inLink.getOfferingQLanes()){
				QueueWithBufferForRoW lane = (QueueWithBufferForRoW) qLaneI;
				//Contra intuitive, but left have to be processed first. This is because left continues deep into the general buffer
				boolean leftMoved;
				do {
					leftMoved = true;
					while(! lane.isNotOfferingLeftVehicle()){
						QVehicleAndMoveType veh = lane.getFirstLeftVehicle();
						leftMoved = actualCarMoveRightPriority(lane, inLink, inDirection, now, nowish, timeoutModifier, veh);
						if(leftMoved) {
							returnValue = true;
						} else {
							break;
						}
					}
					while(! lane.isNotOfferingGeneralVehicle()){
						QVehicleAndMoveType veh = lane.getFirstGeneralVehicle();
						boolean moved = actualCarMoveRightPriority(lane, inLink, inDirection, now, nowish, timeoutModifier,  veh);
						if(moved) {
							returnValue = true;
						} else {
							break;
						}
					}
				}
				while( lane instanceof QueueWithTwoInteractingBuffersForRoW &&  leftMoved  && !lane.isNotOfferingLeftVehicle() ) ;
			}
		}
		return returnValue;
	}

	private boolean actualCarMoveRightPriority(QueueWithBufferForRoW lane, QLinkI inLink,
			int inDirection, double now, double nowish, TimeoutModifier timeoutModifier, QVehicleAndMoveType veh) {
		int outDirection = veh.getOutDirection();
		MoveType mt = veh.getMoveType();
		//Id<Link> nextLinkId = veh.getDriver().chooseNextLinkId();
		//int outDirection = carOutTransformations.get(nextLinkId);
		if(carTimeouts[inDirection][outDirection] <= now){
			//Ignoring left turns when using right priority.
			if (! this.qNode.moveVehicleOverNode(veh, inLink, lane, now, mt, stuckChangesOrderAtRightPriority)) {
				return false;
			}
			timeoutModifier.updateTimeouts(bicycleTimeouts, carTimeouts, inDirection, outDirection, null, nowish);
			return true;
		} else {
			if(allowStuckedVehiclesToMoveDespieConflictingMoves && this.qNode.vehicleIsStuck(lane, now, MoveType.GENERAL)) {
				this.qNode.moveVehicleFromInlinkToOutlink(veh, inLink, lane, now, MoveType.GENERAL);
				if(timeoutChangesAtRightPriorityWhenStuckedVehiclesMoveDespiteConflictingMoves) {
					timeoutModifier.updateTimeouts(bicycleTimeouts, carTimeouts, inDirection, outDirection, null, nowish);
					return true;
				}
			}
			return false;
		}
	}



	private int crashReport(Id<Link> nextLinkId, QVehicle veh) {
		System.out.println(nextLinkId + " does not exist... Existing outlinks are: ");
		for(Id<Link> linkId : bicycleOutTransformations.keySet()) {
			System.out.println(linkId);
		}
		System.out.println(nextLinkId + " does not exist... Existing inlinks are: ");
		for( QLinkI link : bicycleInLinks) {
			System.out.println(link.getLink().getId());
		}
		System.out.println(veh.getDriver().getState());
		System.out.println(veh.getDriver().getMode());
		System.out.println(veh.getDriver().chooseNextLinkId());
		System.out.println(veh.getDriver().isWantingToArriveOnCurrentLink());
		System.out.println(veh.getDriver().getDestinationLinkId());
		System.out.println(veh.getDriver().getCurrentLinkId());
		System.out.println(veh.getDriver().getId());
		System.exit(-1);
		return -1;
	}



	protected int[][] createBicycleTurnsForNonPrioritisedNodes(){
		// The outputs either the outdirection, or the bundle FOLLOWING the bundle with the appropriate inlink
		// The FOLLOWING bundle is used, because the conflicts have to be based on this bundle.
		// One link is "subtracted" before using it in a stepwise left turn move. 

		int[][] turns = new int[bicycleInLinks.length][bicycleInLinks.length];

		for(int inDirection = 0; inDirection < carInLinks.length; inDirection++){
			if(bicycleInLinks[inDirection] != null) {
				for(int outDirection = 0; outDirection < carInLinks.length; outDirection++){
					if(bicycleOutTransformations.values().contains(outDirection)) {
						int t = increaseInt(inDirection);
						//For non-prioritised nodes, ALL bundles are treated as primary links in this regard
						//	while(t != outDirection && isSecondary[t]) {
						//		t = increaseInt(t);
						//	}
						if(t != outDirection) {
							//Find the waiting point closes to the priority link
							int s = decreaseInt(t);
							while( s != inDirection && bicycleInLinks[s] == null) {
								s = decreaseInt(s);
							}
							if(s != inDirection) {
								t = s; // We managed to find a waiting point closer to the priority link.
							} else {
								// We are already waiting as close to the priority as possible.
								// So we need to pass the priority links.

								// Do we go to outdirection, or to next priority?
								s = increaseInt(t);
								
								//Again, all bundles need to be considered for this unprioritised node
								//while(s != outDirection && isSecondary[s]) {
								//	s = increaseInt(s);
								//}
								if(s == outDirection) {
									t = s; // We go directly to outDirection.
								} else {
									//We found the other priority link. Looking for nearest waiting point.
									s = decreaseInt(s);

									//This can at most continue until the inDirection is found, as this is not null.
									while(bicycleInLinks[s] == null) {
										s = decreaseInt(s);
									}

									//If all our strategies leads us to our origin,
									// we have to go directly to outDirection.
									if(s == inDirection) {
										t = outDirection;
									} else {
										t = s;
									}
								}
							} 
						}
						if(t != outDirection) {
							t = increaseInt(t); //This might cause t to be outDirection. But that's okay,
							// since that final part would be a trivial, short right-hand move anyway.
						}
						turns[inDirection][outDirection] = t;
					}
				}
			}
		}
		return turns;
	}

	
	
	
	protected boolean bicycleMoveDirectedPriority(final int inDirection, final double now, final double nowish, TimeoutModifier timeoutModifier) {
		QLinkI inLink = bicycleInLinks[inDirection];
		boolean returnValue = false;
		if(inLink != null) { //This can easily be null - but that's okay.
			for(QLaneI qLaneI : inLink.getOfferingQLanes()){
				QCycleLane lane = ((QCycleLane) qLaneI);
				boolean leftMoved;
				do {
					leftMoved = true;
					while( !lane.isNotOfferingLeftVehicle()){
						QCycleAndMoveType veh = lane.getFirstLeftLeavingVehicle();
						leftMoved = actualBicycleMoveDirectedPriority(inDirection, now, nowish, timeoutModifier, inLink, lane, veh);
						if(!leftMoved) {
							break;
						} else {
							returnValue = true;
						}
					}
					while( !lane.isNotOfferingGeneralVehicle()) {
						QCycleAndMoveType veh = lane.getFirstGeneralLeavingVehicle();
						boolean moved = actualBicycleMoveDirectedPriority(inDirection, now, nowish, timeoutModifier, inLink, lane, veh);
						if(!moved) {
							break;
						} else {
							returnValue = true;
						}
					}
				}
				while(lane.hasInteractingBuffers() && leftMoved && !lane.isNotOfferingLeftVehicle() );
			}
		}
		return returnValue;
	}

	private boolean actualBicycleMoveDirectedPriority(final int inDirection, final double now, final double nowish,
			TimeoutModifier timeoutModifier, QLinkI inLink, QCycleLane lane, QCycleAndMoveType veh) {
		int t = bicycleTurns[inDirection][veh.getOutDirection()];
		int outDirection = veh.getOutDirection();
		if(bicycleTimeouts[inDirection][t] <= now){
			if(t == veh.getOutDirection()){ // Not a partial turn
				if (! this.qNode.moveVehicleOverNode(veh, inLink, lane, now,  MoveType.GENERAL, QFFFAbstractNode.defaultStuckReturnValue)) {
					return false;
				}
			} else { // a partial turn
				t = decreaseInt(t);
				moveLeftTurningBicyclePartiallyOverNode(veh, lane, t);
			}
			timeoutModifier.updateTimeouts(bicycleTimeouts, carTimeouts, 
					inDirection, t, null, nowish);
			return true;
		} else {
			if(QFFFAbstractNode.allowStuckedVehiclesToMoveDespieConflictingMoves && this.qNode.vehicleIsStuck(lane, now, MoveType.GENERAL)){
				if(t != outDirection){
					t = decreaseInt(t);
					moveLeftTurningBicyclePartiallyOverNode(veh, lane, t);
				} else {
					this.qNode.moveVehicleFromInlinkToOutlink(veh, inLink, lane, now, MoveType.GENERAL);	
				}
				if(QFFFAbstractNode.defaultTimeoutBehaviourWhenStuckedVehiclesMoveDespiteConflictingMoves) {
					timeoutModifier.updateTimeouts(bicycleTimeouts, carTimeouts, 
							inDirection, t, null, nowish);
					return true;
				}
			}
			return false;
		}
	}
	
	protected void moveLeftTurningBicyclePartiallyOverNode( final QVehicle veh, final QCycleLane fromLane, final int tempBundle) {
		QLinkI toQLink =  (QLinkI) bicycleInLinks[tempBundle];
		QCycleLane toLane = (QCycleLane) toQLink.getAcceptingQLane();
		fromLane.popFirstLeftVehicle();
		toLane.placeVehicleAtFront(veh);
	}

}
