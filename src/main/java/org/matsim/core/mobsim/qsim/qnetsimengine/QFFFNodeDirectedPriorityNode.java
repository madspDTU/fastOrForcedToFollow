package org.matsim.core.mobsim.qsim.qnetsimengine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.qsim.qnetsimengine.QCycleLaneWithSublinks;

import fastOrForcedToFollow.timeoutmodifiers.PriorityLeftTurnCarTimeoutModifier;
import fastOrForcedToFollow.timeoutmodifiers.RightPriorityBicycleTimeoutModifier;
import fastOrForcedToFollow.timeoutmodifiers.RightPriorityCarTimeoutModifier;
import fastOrForcedToFollow.timeoutmodifiers.SecondaryBicycleTimeoutModifier;
import fastOrForcedToFollow.timeoutmodifiers.TimeoutModifier;

public class QFFFNodeDirectedPriorityNode extends QFFFAbstractNode{


	private int inPriority = -1;
	private int outPriority = -1;
	private int[][] bicycleTurns;
	private boolean[][] carLeftTurns;
	private boolean[] isSecondary;
	final HashMap<Id<Link>, Integer> carInTransformations;



	QFFFNodeDirectedPriorityNode(final QFFFNode qNode, final TreeMap<Double, LinkedList<Link>> thetaMap, QNetwork qNetwork,
			final TreeMap<Double,LinkedList<Integer>> capacities){
		super(qNode, thetaMap, qNetwork);
		if(capacities == null) {
			Gbl.assertIf(carInLinks.length == 2); // This constructor must only be used, when node has exactly 2 bundles
			this.inPriority = 0;
			this.outPriority = 1;
		} else {
			determinePriority(capacities);
		}
		this.isSecondary = new boolean[carInLinks.length];
		this.carInTransformations = new HashMap<Id<Link>, Integer>();
		for(int i = 0; i < carInLinks.length; i++){
			isSecondary[i] = i != this.inPriority  && i != this.outPriority;
			if(carInLinks[i] != null){
				this.carInTransformations.put(carInLinks[i].getLink().getId(), i);
			}
		}
		this.carLeftTurns = createCarLeftTurns();
		this.bicycleTurns = createBicycleTurns();
	}

	QFFFNodeDirectedPriorityNode(final QFFFNode qNode, final TreeMap<Double, LinkedList<Link>> thetaMap, QNetwork qNetwork){
		this(qNode, thetaMap, qNetwork, null);
	}

	protected void bicycleMove(final int inDirection, final double now,
			final double nowish, TimeoutModifier timeoutModifier) {
		QLinkI inLink = bicycleInLinks[inDirection];
		if(inLink != null) { //This can easily be null - but that's okay.
			for(QLaneI lane : inLink.getOfferingQLanes()){
				while(! lane.isNotOfferingVehicle()){	
					QVehicle veh = lane.getFirstVehicle();
					Id<Link> nextLinkId = veh.getDriver().chooseNextLinkId();
					int outDirection = bicycleOutTransformations.get(nextLinkId);
					int t = bicycleTurns[inDirection][outDirection];
					if(bicycleTimeouts[inDirection][t] <= now){
						if(t == outDirection){ // Not a partial turn
							if (! this.qNode.moveVehicleOverNode(veh, inLink, lane, now, true )) {
								break;
							}
						} else { // a partial turn
							t = decreaseInt(t);
							moveLeftTurningBicyclePartiallyOverNode(veh, lane, t);
						}
						timeoutModifier.updateTimeouts(bicycleTimeouts, carTimeouts, 
								inDirection, t, isSecondary, nowish);
					} else {
						break;
					}
				}
			}
		}
	}




	private int[][] createBicycleTurns(){
		// The outputs either the outdirection, or the bundle FOLLOWING the bundle with the appropriate inlink
		// The FOLLOWING bundle is used, because the conflicts have to be based on this bundle.
		// One link is "subtracted" before using it in a stepwise left turn move. 

		int[][] turns = new int[bicycleInLinks.length][bicycleInLinks.length];

		for(int inDirection = 0; inDirection < carInLinks.length; inDirection++){
			if(bicycleInLinks[inDirection] != null) {
				for(int outDirection = 0; outDirection < carInLinks.length; outDirection++){
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


	protected void determinePriority(TreeMap<Double, LinkedList<Integer>> capacities){

		Entry<Double,LinkedList<Integer>> entry = capacities.pollLastEntry(); //highest entry
		int numberOfBundlesInLargestCapacity = entry.getValue().size();
		if(numberOfBundlesInLargestCapacity == 2){
			this.inPriority = entry.getValue().getFirst();
			this.outPriority = entry.getValue().getLast();
		} else if (numberOfBundlesInLargestCapacity == 1){
			this.inPriority = entry.getValue().getFirst();	

			entry = capacities.lastEntry();

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
				int lowestDirection = capacities.pollFirstEntry().getValue().getFirst();
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
				QLinkI qLink = carInLinks[i];
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
				qLink = bicycleInLinks[i];
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

		if(primaryInLinkOrder.isEmpty() && secondaryInLinkOrder.isEmpty()){
			this.qNode.setActive( false ) ;
			return false;
		}

		double nowishBicycle = getNowPlusDelayBicycle(now);
		double nowishCar = getNowPlusDelayCar(now);

		// A)
		if(!primaryInLinkOrder.isEmpty()){

			int n = primaryInLinkOrder.size();
			int i = (n == 1) ? 0 : random.nextInt(n);

			//1 ) priorityMovesForBicycles
			for(int count = 0; count < n; count++){
				int direction = primaryInLinkOrder.get(i);
				bicycleMove(direction, now, nowishBicycle, new RightPriorityBicycleTimeoutModifier()); // includes step-wise left turns
				i = QFFFNodeUtils.increaseInt(i, n);
			}
			//i is now back to the original i.

			//2 ) straight or right moves from priority for cars
			for(int count = 0; count < n; count++){
				int direction = primaryInLinkOrder.get(i);
				straightOrRightTurnCarMove(direction, now, nowishCar, new RightPriorityCarTimeoutModifier()); // left turns handled below
				i = QFFFNodeUtils.increaseInt(i, n);

			}
			//3 ) left turn moves for cars from priority
			for(int count = 0; count < n; count++){
				int direction = primaryInLinkOrder.get(i);
				leftTurnCarMove(direction, now, nowishCar, new PriorityLeftTurnCarTimeoutModifier()); // includes step-wise left turns
				i = QFFFNodeUtils.increaseInt(i, n);
			}
		}


		// B) secondary inlinks.
		if(!secondaryInLinkOrder.isEmpty()){

			int n = secondaryInLinkOrder.size();
			int i = (n == 0) ? 1 : random.nextInt(n);

			//4 ) secondary MovesForBicycles 
			for(int count = 0; count < n; count++){
				int direction = secondaryInLinkOrder.get(i);
				bicycleMove(direction, now, nowishBicycle, new SecondaryBicycleTimeoutModifier()); // includes step-wise left turns
				i = QFFFNodeUtils.increaseInt(i, n);
			}
			//5 ) straight or right moves from secondary for cars
			for(int count = 0; count < n; count++){
				//TODO: Is this the correct timeoutModifier to use here???
				int direction = secondaryInLinkOrder.get(i);
				straightOrRightTurnCarMove(direction, now, nowishCar, new PriorityLeftTurnCarTimeoutModifier()); // left turns handled below
				i = QFFFNodeUtils.increaseInt(i, n);
			}
			//6 ) left turn moves for cars from secondary
			for(int count = 0; count < n; count++){
				//TODO: Is this the correct timeoutModifier to use here???
				int direction = secondaryInLinkOrder.get(i);
				leftTurnCarMove(direction, now, nowishCar, new PriorityLeftTurnCarTimeoutModifier()); // includes step-wise left turns
				i = QFFFNodeUtils.increaseInt(i, n);
			}
		}
		return true;

	}


	protected void leftTurnCarMove(final int inDirection, 
			final double now, final double nowish, TimeoutModifier timeoutModifier) {

		QLinkI inLink = carInLinks[inDirection];
		if(inLink != null){
			for(QLaneI laneI : inLink.getOfferingQLanes()){
				QueueWithBufferForRoW lane = (QueueWithBufferForRoW) laneI;
				while(! lane.isNotOfferingLeftVehicle()){
					QVehicle veh = lane.getFirstLeftVehicle();
					Id<Link> nextLinkId = veh.getDriver().chooseNextLinkId();
					int outDirection = carOutTransformations.get(nextLinkId);
					if(carTimeouts[inDirection][outDirection] <= now && carLeftTurns[inDirection][outDirection]){
						if (! this.qNode.moveVehicleOverNode(veh, inLink, lane, now, false)) {
							break;
						}
						lane.removeFirstLeftVehicle();
						timeoutModifier.updateTimeouts(bicycleTimeouts, carTimeouts, inDirection, outDirection,
								isSecondary, nowish);
					} else {
						break;
					}
				}
			}
		}
	}



	private void moveLeftTurningBicyclePartiallyOverNode( final QVehicle veh, final QLaneI fromLane, final int tempBundle ) {
		QLinkI qLink =  (QLinkI) bicycleInLinks[tempBundle];
		QLaneI qLane = qLink.getAcceptingQLane();
		fromLane.popFirstVehicle(); // Remove veh from previous queue
		if(qLane instanceof QCycleLaneWithSublinks) { //TODO Make interface/abstract class QCycleLane
			QCycleLaneWithSublinks qCycleLane = (QCycleLaneWithSublinks) qLane;
			qCycleLane.placeVehicleAtFront(veh); // Place vehicle in front of temporary queue
		}	
	}




	protected void straightOrRightTurnCarMove(final int inDirection, 
			final double now, final double nowish, TimeoutModifier timeoutModifier) {

		QLinkI inLink = carInLinks[inDirection];
		if(inLink != null){
			for(QLaneI laneI : inLink.getOfferingQLanes()){
				QueueWithBufferForRoW lane = (QueueWithBufferForRoW) laneI;
				while(! lane.isNotOfferingGeneralVehicle()){
					QVehicle veh = lane.getFirstGeneralVehicle();
					Id<Link> nextLinkId = veh.getDriver().chooseNextLinkId();
					int outDirection = carOutTransformations.get(nextLinkId);
					if(carTimeouts[inDirection][outDirection] <= now){
						// No conflicting move is making the move impossible.
						if (! this.qNode.moveVehicleOverNode(veh, inLink, lane, now, false )) {
							//vehicle wasn't moved.
							break;
						}
						lane.removeFirstGeneralVehicle();
						timeoutModifier.updateTimeouts(bicycleTimeouts, carTimeouts, 
								inDirection, outDirection, isSecondary, nowish);
					} else {
						break;
					}
				}
			}
		}
	}

	private int decreaseInt(int i){
		return QFFFNodeUtils.decreaseInt(i, carInLinks.length);
	}
	private int increaseInt(int i){
		return QFFFNodeUtils.increaseInt(i, carInLinks.length);
	}

	public boolean isCarLeftTurn(Id<Link> id, Id<Link> nextLinkId) {
		if(id == nextLinkId || nextLinkId == null){
			return false;
		}
		return carLeftTurns[carInTransformations.get(id)][carOutTransformations.get(nextLinkId)];
	}

}
