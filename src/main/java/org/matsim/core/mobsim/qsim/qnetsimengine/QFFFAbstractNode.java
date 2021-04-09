package org.matsim.core.mobsim.qsim.qnetsimengine;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.qsim.qnetsimengine.QFFFNode.MoveType;

import fastOrForcedToFollow.timeoutmodifiers.ConflictingMovesData;
import fastOrForcedToFollow.timeoutmodifiers.ConflictingMovesData.Element;
import fastOrForcedToFollow.timeoutmodifiers.ConflictingMovesData.ModeIdentifier;
import fastOrForcedToFollow.timeoutmodifiers.TimeoutModifier;

public abstract class QFFFAbstractNode { //Interface or abstract

	private static final Logger log = Logger.getLogger( QFFFAbstractNode.class ) ;

	static final boolean defaultStuckReturnValue = true;
	protected static final boolean stuckChangesOrderAtRightPriority = true;

	//static final boolean allowFreeingGeneralVehiclesStuckBehindLeftBuffer = false;
	static final boolean allowStuckedVehiclesToMoveDespieConflictingMoves = false;
	protected static final boolean timeoutChangesAtRightPriorityWhenStuckedVehiclesMoveDespiteConflictingMoves = false;
	static final boolean defaultTimeoutBehaviourWhenStuckedVehiclesMoveDespiteConflictingMoves = false;

	//	static final boolean letVehiclesFreedFromBehindLeftVehiclesAlterTimeouts = false;
	protected static int smallRoadLeftBufferCapacity;




	final Random random;
	final QFFFNode qNode;
	final QLinkI[] carInLinks;
	final HashMap<Id<Link>, Integer> carOutTransformations;
	final QLinkI[] bicycleInLinks;
	final HashMap<Id<Link>, Integer> bicycleOutTransformations;
	double[][] carTimeouts;
	double[][] bicycleTimeouts;
	protected int[][] bicycleTurns;
	protected boolean simulateBicycles;
	protected boolean simulateCars;
	protected ConflictingMovesData[][] conflictingMovesData;




	protected QFFFAbstractNode(final QFFFNode qNode, final TreeMap<Double, LinkedList<Link>> bundleMap, final QNetwork qNetwork, final Scenario scenario){		
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
		for(double[] row : this.carTimeouts) {
			Arrays.fill(row, -1.0);
		}
		this.bicycleTimeouts = new double[n][n];
		for(double[] row : this.bicycleTimeouts) {
			Arrays.fill(row, -1.0);
		}
		this.random = MatsimRandom.getLocalInstance();
		this.simulateBicycles = scenario.getConfig().qsim().getMainModes().contains(TransportMode.bike);
		this.simulateCars = scenario.getConfig().qsim().getMainModes().contains(TransportMode.car);
		this.conflictingMovesData = null;

	}


	abstract boolean doSimStep(final double now);


	protected double getNowPlusDelayBicycle(final double now){
		return now + qNode.getFFFNodeConfig().getBicycleDelay();
	}

	protected double getNowPlusDelayCar(final double now){
		return now + qNode.getFFFNodeConfig().getCarDelay();
	}

	protected double getNowPlusTwoTimesDelayBicycle(final double now){
		return now + qNode.getFFFNodeConfig().getBicycleDelay();
	}

	protected double getNowPlusTwoTimesDelayCar(final double now){
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
					if(TimeoutModifier.notHinderedByConflicts(now, bicycleTimeouts[inDirection][outDirection])){
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
			final double nowish, final double nowish2, ConflictingMovesData[][] conflictData) {
		boolean returnValue = false;
		QLinkI inLink = carInLinks[inDirection];
		if(inLink != null){
			for(QLaneI lane : inLink.getOfferingQLanes()){
				while(! lane.isNotOfferingVehicle()){
					QVehicleAndMoveType veh = (QVehicleAndMoveType) lane.getFirstVehicle();
					Id<Link> nextLinkId = veh.getDriver().chooseNextLinkId();
					int outDirection = carOutTransformations.get(nextLinkId);
					if(TimeoutModifier.moveNotHinderedByConflicts(now, carTimeouts[inDirection][outDirection])){
						//Ignoring left turns when using right priority.
						if (! this.qNode.moveVehicleOverNode(veh, inLink, lane, now, veh.getMoveType(), stuckChangesOrderAtRightPriority)) {
							break;
						}
						TimeoutModifier.updateTimeoutsWithConflictingMovesCarData(bicycleTimeouts, carTimeouts, 
								nowish, nowish2, conflictData[inDirection][outDirection]);
						returnValue = true;
						continue;
					} else {
						if(allowStuckedVehiclesToMoveDespieConflictingMoves && this.qNode.vehicleIsStuck(lane, now, MoveType.GENERAL)) {
							this.qNode.moveVehicleFromInlinkToOutlink(veh, inLink, lane, now, veh.getMoveType());
							if(timeoutChangesAtRightPriorityWhenStuckedVehiclesMoveDespiteConflictingMoves) {
								TimeoutModifier.updateTimeoutsWithConflictingMovesCarData(bicycleTimeouts, carTimeouts, 
										nowish, nowish2, conflictData[inDirection][outDirection]);
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
			final double nowish, final double nowish2, ConflictingMovesData[][] conflictingMovesData) {
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
						leftMoved = actualCarMoveRightPriority(lane, inLink, inDirection, now, nowish, nowish2, conflictingMovesData, veh);
						if(leftMoved) {
							returnValue = true;
						} else {
							break;
						}
					}
					while(! lane.isNotOfferingGeneralVehicle()){
						QVehicleAndMoveType veh = lane.getFirstGeneralVehicle();
						boolean moved = actualCarMoveRightPriority(lane, inLink, inDirection, now, nowish, nowish2, conflictingMovesData, veh);
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
			int inDirection, double now, double nowish, double nowish2, ConflictingMovesData[][] conflictData,
			QVehicleAndMoveType veh) {
		int outDirection = veh.getOutDirection();
		MoveType mt = veh.getMoveType();
		//Id<Link> nextLinkId = veh.getDriver().chooseNextLinkId();
		//int outDirection = carOutTransformations.get(nextLinkId);
		if(TimeoutModifier.moveNotHinderedByConflicts(now, carTimeouts[inDirection][outDirection])){
			//Ignoring left turns when using right priority.
			if (! this.qNode.moveVehicleOverNode(veh, inLink, lane, now, mt, stuckChangesOrderAtRightPriority)) {
				return false;
			}
			TimeoutModifier.updateTimeoutsWithConflictingMovesCarData(bicycleTimeouts, carTimeouts, 
					nowish, nowish2, conflictData[inDirection][outDirection]);
			return true;
		} else {
			if(allowStuckedVehiclesToMoveDespieConflictingMoves && this.qNode.vehicleIsStuck(lane, now, veh.getMoveType())) {
				this.qNode.moveVehicleFromInlinkToOutlink(veh, inLink, lane, now, veh.getMoveType());
				if(timeoutChangesAtRightPriorityWhenStuckedVehiclesMoveDespiteConflictingMoves) {
					TimeoutModifier.updateTimeoutsWithConflictingMovesCarData(bicycleTimeouts, carTimeouts, 
							nowish, nowish2, conflictData[inDirection][outDirection]);
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



	protected int[][] createBicycleTurnsForNonPrioritisedNodes(int n){
		// The outputs either the outdirection, or the bundle FOLLOWING the bundle with the appropriate inlink
		// The FOLLOWING bundle is used, because the conflicts have to be based on this bundle.
		// One link is "subtracted" before using it in a stepwise left turn move. 

		int[][] turns = new int[n][n];

		for(int inDirection = 0; inDirection < n; inDirection++){
			if(bicycleInLinks[inDirection] != null) {
				for(int outDirection = 0; outDirection < n; outDirection++){
					if(bicycleOutTransformations.values().contains(outDirection)) {
						int t = QFFFNodeUtils.increaseInt(inDirection,n);
						//For non-prioritised nodes, ALL bundles are treated as primary links in this regard
						while(t != outDirection && bicycleInLinks[t] == null) {
							t = QFFFNodeUtils.increaseInt(t,n);
						}
						if(t != outDirection) {
							t = QFFFNodeUtils.increaseInt(t,n); //This might cause t to be outDirection. But that's okay,
							// since that final part would be a trivial, short right-hand move anyway.
						}
						turns[inDirection][outDirection] = t;
					}
				}
			}
		}
		return turns;
	}




	protected boolean bicycleMoveDirectedPriority(final int inDirection, final double now, final double nowish, final double nowish2,
			ConflictingMovesData[][] conflictingMovesData) {
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
						leftMoved = actualBicycleMoveDirectedPriority(inDirection, now, nowish, nowish2,  inLink, lane, veh, conflictingMovesData);
						if(!leftMoved) {
							break;
						} else {
							returnValue = true;
						}
					}
					while( !lane.isNotOfferingGeneralVehicle()) {
						QCycleAndMoveType veh = lane.getFirstGeneralLeavingVehicle();
						boolean moved = actualBicycleMoveDirectedPriority(inDirection, now, nowish, nowish2,  inLink, lane, veh, conflictingMovesData);
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

	private boolean actualBicycleMoveDirectedPriority(final int inDirection, final double now, final double nowish, final double nowish2,
			QLinkI inLink, QCycleLane lane, QCycleAndMoveType veh, ConflictingMovesData[][] conflictingMovesData) {
		int outDirection = veh.getOutDirection();
		int t = bicycleTurns[inDirection][outDirection];
		if(TimeoutModifier.moveNotHinderedByConflicts(now, bicycleTimeouts[inDirection][t])){
			if(t == outDirection){ // Not a partial turn
				if (! this.qNode.moveVehicleOverNode(veh, inLink, lane, now,  veh.getMoveType(), QFFFAbstractNode.defaultStuckReturnValue)) {
					return false;
				}
				TimeoutModifier.updateTimeoutsWithConflictingMovesBicycleData(bicycleTimeouts, carTimeouts, nowish, nowish2,
						conflictingMovesData[inDirection][outDirection]);

			} else { // a partial turn
				t = decreaseInt(t);
				moveLeftTurningBicyclePartiallyOverNode(veh, lane, t);
				TimeoutModifier.updateTimeoutsWithConflictingMovesBicycleData(bicycleTimeouts, carTimeouts, nowish, nowish2,
						conflictingMovesData[inDirection][outDirection]);

			}
			return true;
		} else {
			if(QFFFAbstractNode.allowStuckedVehiclesToMoveDespieConflictingMoves && this.qNode.vehicleIsStuck(lane, now, veh.getMoveType())){
				if(t != outDirection){
					int overshootBundle = t;
					t = decreaseInt(t);
					moveLeftTurningBicyclePartiallyOverNode(veh, lane, t);
					TimeoutModifier.updateTimeoutsWithConflictingMovesBicycleData(bicycleTimeouts, carTimeouts, nowish, nowish2,
							conflictingMovesData[inDirection][outDirection]);

				} else {
					this.qNode.moveVehicleFromInlinkToOutlink(veh, inLink, lane, now, veh.getMoveType());
					TimeoutModifier.updateTimeoutsWithConflictingMovesBicycleData(bicycleTimeouts, carTimeouts, nowish, nowish2,
							conflictingMovesData[inDirection][outDirection]);
				}
				if(QFFFAbstractNode.defaultTimeoutBehaviourWhenStuckedVehiclesMoveDespiteConflictingMoves) {
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






	protected LinkedList<ConflictingMovesData.Element> createBicycleConflictingData(int inDirection, int outDirection,
			int n, int[][] bicycleRankMatrix, int[][] carRankMatrix) {

		LinkedList<ConflictingMovesData.Element> ll = new LinkedList<ConflictingMovesData.Element>();

		int inMoveRank = bicycleRankMatrix[inDirection][outDirection];
		int t = bicycleTurns[inDirection][outDirection];
		boolean isLeftTurn = t != outDirection;

		if(isLeftTurn) {

			int outDirectionMinusOne =  QFFFNodeUtils.decreaseInt(t, n);
			int j = QFFFNodeUtils.increaseInt(inDirection, n);
			while(j != outDirection){
				for(int i = 0; i < n; i++){
					// all car movements to a road to the right of the movement
					addElementIfNeeded(ll, i, j, ModeIdentifier.Car, inMoveRank, carRankMatrix);
					// all car movements from the right of the movement 
					addElementIfNeeded(ll, j, i, ModeIdentifier.Car, inMoveRank, carRankMatrix);
					if(j != outDirectionMinusOne) {
						// all bicycle movements from the right of the movement
						addElementIfNeeded(ll, j, i, ModeIdentifier.Bicycle, inMoveRank, bicycleRankMatrix);
					}
					if(i != inDirection){	
						// all bicycle movements to the right of the movement (indirection)
						addElementIfNeeded(ll, i, j, ModeIdentifier.Bicycle, inMoveRank, bicycleRankMatrix);
					}
				}
				j = QFFFNodeUtils.increaseInt(j, n);
			}

		} else { // General move

			for(int i = 0; i < n; i++){ // All bicycle movements to out direction exceptindirection
				if(i != inDirection){
					t = bicycleTurns[i][outDirection];
					if(t == outDirection) { // Otherwise never used
						addElementIfNeeded(ll, i, outDirection, ModeIdentifier.Bicycle, inMoveRank, bicycleRankMatrix);
					}
				}
			}

			// If indirection = "outdirection + 1". _Rightest_ possible turn.

			int j = QFFFNodeUtils.increaseInt(inDirection, n);
			while(j != outDirection){
				for(int i = 0; i < n; i++){
					// all car movements to a road to the right of the movement
					addElementIfNeeded(ll, i, j, ModeIdentifier.Car, inMoveRank, carRankMatrix);
					// all car movements from the right of the movement 
					addElementIfNeeded(ll, j, i, ModeIdentifier.Car, inMoveRank, carRankMatrix);
					// all bicycle movements from the right of the movement
					addElementIfNeeded(ll, j, i, ModeIdentifier.Bicycle, inMoveRank, bicycleRankMatrix);
					if(i != inDirection){	
						// all bicycle movements to the right of the movement (indirection)
						addElementIfNeeded(ll, i, j, ModeIdentifier.Bicycle, inMoveRank, bicycleRankMatrix);
					}
				}
				j = QFFFNodeUtils.increaseInt(j, n);
			}
		}

		return ll;
	}


	protected LinkedList<Element> createCarConflictingData(int inDirection, int outDirection, int n, int[][] bicycleRankMatrix,
			int[][] carRankMatrix) {

		LinkedList<ConflictingMovesData.Element> ll = new LinkedList<ConflictingMovesData.Element>();
		int inMoveRank = carRankMatrix[inDirection][outDirection];

		// Car movements to out direction
		for(int i = 0; i < n; i++){ // All car movements to out direction except from indirection
			if(i != inDirection){
				addElementIfNeeded(ll, i, outDirection, ModeIdentifier.Car, inMoveRank, carRankMatrix);
			}
		}

		if(inDirection == outDirection) {
			return ll;
		}

		//Conflicting car movements
		int r = QFFFNodeUtils.increaseInt(inDirection, n); //new
		while(r != outDirection){
			for(int i = 0; i < n; i++){
				// all car movements from the right 
				addElementIfNeeded(ll, r, i, ModeIdentifier.Car, inMoveRank, carRankMatrix);
				// all car movements to the right (except from indirection)
				if(i != inDirection){	
					addElementIfNeeded(ll, i, r, ModeIdentifier.Car, inMoveRank, carRankMatrix);
				}
			}
			r = QFFFNodeUtils.increaseInt(r,n);
		}

		// Conflicting bicycle movements
		r = inDirection;
		while(r != outDirection){
			int l = inDirection;
			while(l != outDirection){
				// All bicycle movements from right to left.
				addElementIfNeeded(ll, r, l, ModeIdentifier.Bicycle, inMoveRank, bicycleRankMatrix);
				// All bicycle movements from left to right.
				if(l != inDirection){
					addElementIfNeeded(ll, l, r, ModeIdentifier.Bicycle, inMoveRank, bicycleRankMatrix);
				}
				l = QFFFNodeUtils.decreaseInt(l,n);
			}
			// The final one (from outDirection)
			if(r != inDirection) { // new
				addElementIfNeeded(ll, l, r, ModeIdentifier.Bicycle, inMoveRank, bicycleRankMatrix);
			} 
			r = QFFFNodeUtils.increaseInt(r,n);
		}

		return ll;
	}


	protected void addElementIfNeeded(LinkedList<Element> ll, int in, int out, ModeIdentifier modeIdentifier,
			int inRank, int[][] rankMatrix) {
		int confRank = rankMatrix[in][out];
		if(confRank > inRank) {
			return;
		}
		ll.add(new ConflictingMovesData.Element(in, out, modeIdentifier, confRank == inRank));
	}


	public ConflictingMovesData[][] getConflictingMovesData() {
		return this.conflictingMovesData;
	}

	abstract int[][] createBicycleRankMatrix(int n);
	abstract int[][] createCarRankMatrix(int n);

	protected ConflictingMovesData[][] createConflictingMovesData(int n) {

		int[][] bicycleRankMatrix = createBicycleRankMatrix(n);
		int[][] carRankMatrix = createCarRankMatrix(n);

		ConflictingMovesData[][] conflictingMovesData = new ConflictingMovesData[n][n];
		for(int i = 0; i < n; i++) {
			for(int j = 0; j <n; j++) {
				LinkedList<ConflictingMovesData.Element> bicycleData = createBicycleConflictingData(i,j,n, bicycleRankMatrix, carRankMatrix);
				LinkedList<ConflictingMovesData.Element> carData = createCarConflictingData(i,j,n, bicycleRankMatrix, carRankMatrix);
				conflictingMovesData[i][j] = new ConflictingMovesData(bicycleData, carData);
			}
		}
		return(conflictingMovesData);
	}


}