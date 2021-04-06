package org.matsim.core.mobsim.qsim.qnetsimengine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.qsim.qnetsimengine.QFFFNode.MoveType;

import fastOrForcedToFollow.timeoutmodifiers.ConflictingMovesData;
import fastOrForcedToFollow.timeoutmodifiers.ConflictingMovesData.Element;
import fastOrForcedToFollow.timeoutmodifiers.ConflictingMovesData.ModeIdentifier;
import fastOrForcedToFollow.timeoutmodifiers.TimeoutModifier;

public class QFFFNodeDirectedPriorityNode extends QFFFNodeWithLeftBuffer {


	private int inPriority = -1;
	private int outPriority = -1;
	private boolean[][] carLeftTurns;
	private boolean[] isSecondary;



	QFFFNodeDirectedPriorityNode(final QFFFNode qNode, final TreeMap<Double, LinkedList<Link>> thetaMap, QNetwork qNetwork,
			final TreeMap<HierarchyInformation, LinkedList<Integer>> hierarchyInformations, Scenario scenario){
		super(qNode, thetaMap, qNetwork, scenario);
		if(hierarchyInformations == null) {
			Gbl.assertIf(carInLinks.length == 2); // This constructor must only be used, when node has exactly 2 bundles
			this.inPriority = 0;
			this.outPriority = 1;
		} else {
			determinePriority(hierarchyInformations);
		}
		this.isSecondary = new boolean[carInLinks.length];
		Arrays.fill(this.isSecondary, true);
		for(int i = 0; i < carInLinks.length; i++){
			if(i == this.inPriority || i == this.outPriority) {
				isSecondary[i] = false;
			}
		}
		this.carLeftTurns = createCarLeftTurns();
		this.bicycleTurns = createBicycleTurnsForDirectedPriorityNodes(carInLinks.length);
		this.conflictingMovesData = createConflictingMovesData(carInLinks.length);
	}
	
	QFFFNodeDirectedPriorityNode(final QFFFNode qNode, final TreeMap<Double, LinkedList<Link>> thetaMap, QNetwork qNetwork, Scenario scenario){
		this(qNode, thetaMap, qNetwork, null, scenario);
	}

	

	int[][] createBicycleRankMatrix(int n) {

		// All moves from priority: 0
		// All moves from non-priority -3.

		int[][] matrix = new int[n][n];

		for(int i = 0; i < n; i++) {
			if(isSecondary[i]) {
				for(int j = 0; j < n; j++) {
					matrix[i][j] = -3;
				}
			}
		}

		return matrix;
	}

	int[][] createCarRankMatrix(int n) {

		// All general moves from priorities: -1
		// All left turning moves from priorities: -2
		// All general moves from non-priorities: -4
		// All left turning moves from non-priorities: -5

		int[][] matrix = new int[n][n];

		for(int i = 0; i < n; i++) {
			for(int j = 0; j < n; j++) {
				if(!carLeftTurns[i][j]) {
					matrix[i][j] = isSecondary[i] ? -4 : -1;
				} else {
					matrix[i][j] = isSecondary[i] ? -5 : -2;
				}
			}
		}

		return matrix;
	}


	private int[][] createBicycleTurnsForDirectedPriorityNodes(int n){
		// The outputs either the outdirection, or the bundle FOLLOWING the bundle with the appropriate inlink
		// The FOLLOWING bundle is used, because the conflicts have to be based on this bundle.
		// One link is "subtracted" before using it in a stepwise left turn move. 

		int[][] turns = new int[n][n];

		for(int inDirection = 0; inDirection < n; inDirection++){
			if(bicycleInLinks[inDirection] != null) {
				for(int outDirection = 0; outDirection < n; outDirection++){
					if(bicycleOutTransformations.values().contains(outDirection)) {
						int t = increaseInt(inDirection);
						while(t != outDirection && isSecondary[t]) {
							t = increaseInt(t);
						}
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
								while(s != outDirection && isSecondary[s]) {
									s = increaseInt(s);
								}
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

	private boolean[][] createCarLeftTurns() {

		boolean[] bundleContainsCarLinks = new boolean[carInLinks.length];
		for(int k = 0; k < carInLinks.length; k++){
			if(carInLinks[k] != null){
				bundleContainsCarLinks[k] = true;
			}
		}
		for(int k : carOutTransformations.values()){
			bundleContainsCarLinks[k] = true;
		}
		int numberOfBundlesContainingCarLinks = 0;
		for(boolean b : bundleContainsCarLinks){
			if(b){
				numberOfBundlesContainingCarLinks++;
			}
		}
		int maximumNumberOfLeftTurns = (int) Math.ceil(numberOfBundlesContainingCarLinks / 2.);


		boolean[][] leftTurns = new boolean[carInLinks.length][carInLinks.length];
		for(int inDirection = 0; inDirection < carInLinks.length; inDirection++){
			for(int outDirection = 0; outDirection < carInLinks.length; outDirection++){
				boolean isLeftTurn = false;
				if( inDirection == this.inPriority){
					if( outDirection > this.outPriority || outDirection <= this.inPriority) { // left turn
						isLeftTurn = true;
					} 
				} else if( inDirection == this.outPriority){
					if( outDirection > this.inPriority  && outDirection <= this.outPriority ){ 
						isLeftTurn = true;
					}
				} else {

					int counter = 0;
					int t = inDirection;	
					while(t != outDirection){
						if(bundleContainsCarLinks[t]){
							counter++;
						} else {
							counter += 2;
						}
						t = decreaseInt(t);
					} // one last time
					if(bundleContainsCarLinks[t]){
						counter++;
					} else {
						counter += 2;
					}

					if(counter <= maximumNumberOfLeftTurns){ //is a left turn
						isLeftTurn = true;
					} 
				}
				leftTurns[inDirection][outDirection] = isLeftTurn;
			}
		}
		return leftTurns;

	}


	protected void determinePriority(TreeMap<HierarchyInformation, LinkedList<Integer>> hierarchyInformations){

		Entry<HierarchyInformation, LinkedList<Integer>> entry = hierarchyInformations.pollLastEntry(); //highest entry
		int numberOfBundlesInLargestCapacity = entry.getValue().size();
		if(numberOfBundlesInLargestCapacity == 2){
			this.inPriority = entry.getValue().getFirst();
			this.outPriority = entry.getValue().getLast();
		} else if (numberOfBundlesInLargestCapacity == 1){
			this.inPriority = entry.getValue().getFirst();	

			entry = hierarchyInformations.lastEntry();

			if(entry.getValue().size() == 1){
				this.outPriority = entry.getValue().getFirst();
			} else { // This is now carInLinks == 4 and 2 bundles in second largest capacity.

				Gbl.assertIf(entry.getValue().size() == 2 && carInLinks.length == 4);

				int guess = ((this.inPriority + 2) % 4);
				if(entry.getValue().contains(guess)){
					this.outPriority = guess;
				} else {
					//mads: May not be obvious, but seems as better than ((this.inPriority + 3) % 4),
					//   since secondary to tertiary violates more with this implementation;
					this.outPriority = ((this.inPriority + 1) % 4);
				}
			}
		} else { //cumCount > 2
			if(carInLinks.length == 4){ //cumCount must be 3 then - we thus determine capacity based on the lowest
				Gbl.assertIf(numberOfBundlesInLargestCapacity == 3);
				int lowestDirection = hierarchyInformations.pollFirstEntry().getValue().getFirst();
				this.inPriority = (lowestDirection + 3) % 4;
				this.outPriority = (lowestDirection + 1) % 4;
			} else if(carInLinks.length >= 5){
				System.err.println("This should not be possible (At least 5 bundles, but more than 2 with maximum capacity");
				System.exit(-1);
			}
		}

		if(this.inPriority > this.outPriority){
			int temp = this.inPriority;
			this.inPriority = this.outPriority;
			this.outPriority = temp;
		}
		if(this.inPriority < 0 ||  this.outPriority < 0 || this.inPriority == this.outPriority  ){
			System.out.println("Debug: (" + this.inPriority + "," + this.outPriority + ")  ");
			System.out.println(carInLinks.length + " " + numberOfBundlesInLargestCapacity);
			System.out.println("Interesting... seems as if links with only 1 capacity have errors.");
			System.exit(-1);
		}
		//Gbl.assertIf(this.inPriority != this.outPriority);

	}

	protected boolean doSimStep(final double now){


		ArrayList<Integer> primaryInLinkOrder = new ArrayList<Integer>(carInLinks.length); 
		ArrayList<Integer> secondaryInLinkOrder = new ArrayList<Integer>(carInLinks.length);

		outerLoop: 
			for(int i = 0; i < carInLinks.length; i++){
				if(simulateCars) {
					QLinkI qLink = carInLinks[i];
					if(qLink != null){
						for(QLaneI qLaneI : qLink.getOfferingQLanes()){
							QueueWithBufferForRoW qLane = (QueueWithBufferForRoW) qLaneI;
							if(!qLane.isNotOfferingVehicle()){
								if(isSecondary[i]){
									secondaryInLinkOrder.add(i);
								} else {
									primaryInLinkOrder.add(i);
								}
								continue outerLoop;
							}
						}
					}
				}
				if(simulateBicycles) {
					QLinkI qLink = bicycleInLinks[i];
					if(qLink != null){
						for(QLaneI qLane : qLink.getOfferingQLanes()){
							if(!qLane.isNotOfferingVehicle()){
								if(isSecondary[i]){
									secondaryInLinkOrder.add(i);
								} else {
									primaryInLinkOrder.add(i);
								}
								continue outerLoop;
							}
						}
					}
				}
			}

		if(primaryInLinkOrder.isEmpty() && secondaryInLinkOrder.isEmpty()){
			this.qNode.setActive( false ) ;
			return false;
		}

		double nowishBicycle = getNowPlusDelayBicycle(now);
		double nowishCar = getNowPlusDelayCar(now);
		double nowishBicycle2 = getNowPlusTwoTimesDelayBicycle(now);
		double nowishCar2 = getNowPlusTwoTimesDelayCar(now);
		

		// A)
		if(!primaryInLinkOrder.isEmpty()){

			int n = primaryInLinkOrder.size();
			int i = (n == 1) ? 0 : random.nextInt(n);

			if(simulateBicycles) {
				//1 ) priorityMovesForBicycles
				for(int count = 0; count < n; count++){
					int direction = primaryInLinkOrder.get(i);
					bicycleMoveDirectedPriority(direction, now, nowishBicycle, nowishBicycle2, conflictingMovesData); // includes step-wise left turns
					i = QFFFNodeUtils.increaseInt(i, n);
				}
			}
			//i is now back to the original i.

			if(simulateCars) {
				//2 ) straight or right moves from priority for cars
				for(int count = 0; count < n; count++){
					int direction = primaryInLinkOrder.get(i);
					straightOrRightTurnCarMove(direction, now, nowishCar, nowishCar2, conflictingMovesData); // left turns handled below
					i = QFFFNodeUtils.increaseInt(i, n);

				}
				//3 ) left turn moves for cars from priority
				for(int count = 0; count < n; count++){
					int direction = primaryInLinkOrder.get(i);
					leftTurnCarMove(direction, now, nowishCar, nowishCar2, conflictingMovesData); 
					i = QFFFNodeUtils.increaseInt(i, n);
				}
			}
		}


		// B) secondary inlinks.
		if(!secondaryInLinkOrder.isEmpty()){

			int n = secondaryInLinkOrder.size();
			int i = (n == 0) ? 1 : random.nextInt(n);

			if(simulateBicycles) {
				//4 ) secondary MovesForBicycles 
				for(int count = 0; count < n; count++){
					int direction = secondaryInLinkOrder.get(i);
					bicycleMoveDirectedPriority(direction, now, nowishBicycle, nowishBicycle2, conflictingMovesData); // includes step-wise left turns
					i = QFFFNodeUtils.increaseInt(i, n);
				}
			}
			if(simulateCars) {
				//5 ) straight or right moves from secondary for cars
				for(int count = 0; count < n; count++){
					//TODO: Is this the correct timeoutModifier to use here???
					int direction = secondaryInLinkOrder.get(i);
					straightOrRightTurnCarMove(direction, now, nowishCar, nowishCar2, conflictingMovesData); // left turns handled below
					i = QFFFNodeUtils.increaseInt(i, n);
				}
				//6 ) left turn moves for cars from secondary
				for(int count = 0; count < n; count++){
					//TODO: Is this the correct timeoutModifier to use here???
					int direction = secondaryInLinkOrder.get(i);
					leftTurnCarMove(direction, now, nowishCar, nowishCar2, conflictingMovesData); // includes step-wise left turns
					i = QFFFNodeUtils.increaseInt(i, n);
				}
			}
		}
		return true;

	}


	protected void leftTurnCarMove(final int inDirection, 
			final double now, final double nowish, final double nowish2, ConflictingMovesData[][] conflictingMovesData) {

		QLinkI inLink = carInLinks[inDirection];
		if(inLink != null){
			for(QLaneI laneI : inLink.getOfferingQLanes()){
				QueueWithBufferForRoW lane = (QueueWithBufferForRoW) laneI;
				while(! lane.isNotOfferingLeftVehicle()){
					QVehicleAndMoveType veh = (QVehicleAndMoveType) lane.getFirstLeftVehicle();
					Id<Link> nextLinkId = veh.getDriver().chooseNextLinkId();
					int outDirection = carOutTransformations.get(nextLinkId);
					if(TimeoutModifier.moveNotHinderedByConflicts(now, carTimeouts[inDirection][outDirection])){
						if (! this.qNode.moveVehicleOverNode(veh, inLink, lane, now,  veh.getMoveType(), QFFFAbstractNode.defaultStuckReturnValue)) {
							break;
						}
						TimeoutModifier.updateTimeoutsWithConflictingMovesCarData(bicycleTimeouts, carTimeouts,
								nowish, nowish2, conflictingMovesData[inDirection][outDirection]);
					} else {
						if(QFFFAbstractNode.allowStuckedVehiclesToMoveDespieConflictingMoves && this.qNode.vehicleIsStuck(lane, now, veh.getMoveType())){
							this.qNode.moveVehicleFromInlinkToOutlink(veh, inLink, lane, now, veh.getMoveType());
							if(QFFFAbstractNode.defaultTimeoutBehaviourWhenStuckedVehiclesMoveDespiteConflictingMoves) {
								TimeoutModifier.updateTimeoutsWithConflictingMovesCarData(bicycleTimeouts, carTimeouts,
										nowish, nowish2, conflictingMovesData[inDirection][outDirection]);
						continue;
							}
						}
						break;
					}
				}
			}
		}
	}








	protected void straightOrRightTurnCarMove(final int inDirection, 
			final double now, final double nowish, final double nowish2, ConflictingMovesData[][] conflictingData) {

		QLinkI inLink = carInLinks[inDirection];
		if(inLink != null){
			for(QLaneI laneI : inLink.getOfferingQLanes()){
				QueueWithBufferForRoW lane = (QueueWithBufferForRoW) laneI;
				while(! lane.isNotOfferingGeneralVehicle()){
					QVehicleAndMoveType veh = (QVehicleAndMoveType) lane.getFirstGeneralVehicle();
					Id<Link> nextLinkId = veh.getDriver().chooseNextLinkId();
					int outDirection = carOutTransformations.get(nextLinkId);
					if(TimeoutModifier.moveNotHinderedByConflicts(now, carTimeouts[inDirection][outDirection])){
						// No conflicting move is making the move impossible.
						if (! this.qNode.moveVehicleOverNode(veh, inLink, lane, now, veh.getMoveType(), QFFFAbstractNode.defaultStuckReturnValue)) {
							//vehicle wasn't moved.
							break;
						}
						TimeoutModifier.updateTimeoutsWithConflictingMovesCarData(bicycleTimeouts, carTimeouts, 
								nowish, nowish2, conflictingData[inDirection][outDirection]);
					} else {
						if(QFFFAbstractNode.allowStuckedVehiclesToMoveDespieConflictingMoves && this.qNode.vehicleIsStuck(lane, now, veh.getMoveType())){
							this.qNode.moveVehicleFromInlinkToOutlink(veh, inLink, lane, now, veh.getMoveType());
							if(QFFFAbstractNode.defaultTimeoutBehaviourWhenStuckedVehiclesMoveDespiteConflictingMoves) {
								TimeoutModifier.updateTimeoutsWithConflictingMovesCarData(bicycleTimeouts, carTimeouts, 
										nowish, nowish2, conflictingData[inDirection][outDirection]);
								continue;
							} 
						}
						break;
					}
				}
			}
		}
	}


	@Override
	public boolean isCarLeftTurn(Id<Link> id, int outDirection) {
		//	if(id == nextLinkId || nextLinkId == null){
		//		return false;
		//	}
		return carLeftTurns[carInTransformations.get(id)][outDirection];
	}





	/*
	protected void bicycleLeftishMove(final int inDirection, final double now,
			final double nowish, TimeoutModifier timeoutModifier) {
		QLinkI inLink = bicycleInLinks[inDirection];
		if(inLink != null) { //This can easily be null - but that's okay.
			for(QLaneI lane : inLink.getOfferingQLanes()){
				while(! lane.isNotOfferingVehicle()){	
					QVehicle veh = lane.getFirstVehicle();
					Id<Link> nextLinkId = veh.getDriver().chooseNextLinkId();
					int outDirection = getBicycleOutDirection(nextLinkId, veh);
					int t = bicycleTurns[inDirection][outDirection];
					if(TimeoutModifier.notHinderedByConflicts(now,bicycleTimeouts[inDirection][t])){
						if(t == outDirection){ // Not a partial turn
							if (! this.qNode.moveVehicleOverNode(veh, inLink, lane, now,  MoveType.GENERAL, QFFFAbstractNode.defaultStuckReturnValue)) {
								break;
							}
						} else { // a partial turn
							t = decreaseInt(t);
							moveLeftTurningBicyclePartiallyOverNode(veh, lane, t);
						}
						timeoutModifier.updateTimeouts(bicycleTimeouts, carTimeouts, 
								inDirection, t, isSecondary, nowish);
						continue;
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
										inDirection, t, isSecondary, nowish);
								continue;
							}
						}
						break;
					}
				}
			}
		}
	}
	 */



}
