package org.matsim.core.mobsim.qsim.qnetsimengine;

import java.util.Collections;
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
import fastOrForcedToFollow.timeoutmodifiers.TimeoutModifierI;

public class QFFFNodeDirectedPriorityNode extends QFFFAbstractNode{


	private int inPriority = -1;
	private int outPriority = -1;
	private int[][] bicycleTurns;
	private boolean[][] carLeftTurns;
	private boolean[] isSecondary;

	QFFFNodeDirectedPriorityNode(final QFFFNode qNode, final TreeMap<Double, LinkedList<Link>> thetaMap, QNetwork qNetwork,
			final TreeMap<Double,LinkedList<Integer>> capacities){
		super(qNode, thetaMap, qNetwork);
		determinePriority(capacities);
		this.carLeftTurns = createCarLeftTurns();
		this.bicycleTurns = createBicycleTurns();
		this.isSecondary = new boolean[carInLinks.length];
		for(int i = 0; i < carInLinks.length; i++){
			isSecondary[i] = i != this.inPriority  && i != this.outPriority;
		}
	}

	protected void bicycleMove(final int inDirection, final double now,
			final double nowish, TimeoutModifierI timeoutModifier) {
		QLinkI inLink = bicycleInLinks[inDirection];
		if(inLink != null){
			for(QLaneI lane : inLink.getOfferingQLanes()){
				while(! lane.isNotOfferingVehicle()){	
					QVehicle veh = lane.getFirstVehicle();
					Id<Link> nextLinkId = veh.getDriver().chooseNextLinkId();

					//debugging
					if(!bicycleOutTransformations.containsKey(nextLinkId)){
						System.out.println(qNode.getNode().getId() + " N L " + nextLinkId + " " + inLink.getLink().getId() + " From");
						for(Id<Link> id : bicycleOutTransformations.keySet()){
							System.out.println(bicycleOutTransformations.get(id) + " is " + id);
						}
						for(Link link : this.qNode.getNode().getOutLinks().values()){
							System.out.println("Outlink: " + link.getId());
						}
						for(Link link : this.qNode.getNode().getInLinks().values()){
							System.out.println("Inlink: " + link.getId());
						}


						System.out.println("Simulation endds prematurely");
						System.exit(-1);
					}

					int outDirection = bicycleOutTransformations.get(nextLinkId);
					int t = bicycleTurns[inDirection][outDirection];
					if(t != outDirection){
						t = increaseInt(t); // It corresponds to going to the next link.
					}

					if(bicycleTimeouts[inDirection][t] <= now){
						if(t == outDirection){ // Not a partial turn
							if (! this.qNode.moveVehicleOverNode(veh, inLink, lane, now )) {
								break;
							}
						} else { // a partial turn
							moveLeftTurningBicyclePartiallyOverNode(veh, lane, nowish, decreaseInt(t));
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

		// Still something about the left turns from non-priority links that does not work.
		// (Currently, straightahead is considered a left turn, which it should not). 


		int[][] turns = new int[carInLinks.length][carInLinks.length];
		for(int inDirection = 0; inDirection < carInLinks.length; inDirection++){
			for(int outDirection = 0; outDirection < carInLinks.length; outDirection++){
				int t = outDirection;
				if( carLeftTurns[inDirection][outDirection]) { // Is a left turn
					int otherPriority = -1;
					if( inDirection == this.inPriority){
						otherPriority = this.outPriority;
					} else if( inDirection == this.outPriority){
						otherPriority = this.inPriority;
					}
					if(otherPriority >= 0){
						t = decreaseInt(otherPriority); 
						while(bicycleInLinks[t] == null && t != inDirection ){
							t = decreaseInt(t);
						}
						if(t == inDirection){
							t = increaseInt(otherPriority);
							while(bicycleInLinks[t] == null && t != outDirection ){
								t = increaseInt(t);
							}
						}
					}  else {// not priority, choose first link to the right...
						t = increaseInt(inDirection);
						while(bicycleInLinks[t] == null && t != outDirection ){
							t = increaseInt(t);
						}
					}
				}
				turns[inDirection][outDirection] = t;
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


	private int decreaseInt(int i){
		if(i == 0){
			return carInLinks.length - 1;
		} else {
			return i - 1;
		}
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
				//					
				//					//Seems to be the optimal way to do it.
				//					int guess = (this.inPriority + 1) % 3;
				//					int other = (this.inPriority + 2) % 3;
				//					boolean guessInCar = carInLinks[guess] != null;
				//					boolean guessInBicycle = bicycleInLinks[guess] != null;
				//
				//					boolean guessOutCar = carOutTransformations.containsValue(guess);
				//					boolean guessOutBicycle = bicycleOutTransformations.containsValue(guess);
				//
				//					boolean otherInCar = carInLinks[other] != null;
				//					boolean otherInBicycle = bicycleInLinks[other] != null;
				//
				//					boolean otherOutCar = carOutTransformations.containsValue(other);
				//					boolean otherOutBicycle = bicycleOutTransformations.containsValue(other);
				//
				//					if(!(guessInCar || guessOutCar || otherInCar || otherOutCar) || 
				//							(guessInCar && guessOutCar && otherInCar && otherOutCar) ){
				//						if(!(guessInBicycle || guessOutBicycle || otherInBicycle || otherOutBicycle) || 
				//								(guessInBicycle && guessOutBicycle && otherInBicycle && otherOutBicycle) ){
				//							System.err.println("Probably an anti-priority node");
				//						}
				//					}
				//					
				//					if(priorityInCar || priorityOutCar){
				//						if(priorityInCar  && priorityOutCar){
				//							if(guessOutCar){
				//								if(!guessInCar && otherOutCar && otherInCar){
				//									this.outPriority = other;
				//								} else {
				//									this.outPriority = guess;
				//								}
				//							} else {
				//								this.outPriority = other;
				//							}
				//						} else if(!priorityInCar  && priorityOutCar){
				//							if(guessOutCar){
				//								this.outPriority = guess;
				//							} else {
				//								this.outPriority = other;
				//							}
				//						} else if (priorityInCar  && !priorityOutCar){
				//							if(otherInCar){
				//								this.outPriority = other;
				//							} else {
				//								this.outPriority = guess;
				//							}
				//						} 
				//					} else if(priorityInBicycle || priorityOutBicycle) {
				//						if(priorityInBicycle  && priorityOutBicycle){
				//							if(guessOutBicycle){
				//								if(!guessInBicycle && otherOutBicycle && otherInBicycle){
				//									this.outPriority = other;
				//								} else {
				//									this.outPriority = guess;
				//								}
				//							} else {
				//								this.outPriority = other;
				//							}
				//						} else if(!priorityInBicycle  && priorityOutBicycle){
				//							if(guessOutCar){
				//								this.outPriority = guess;
				//							} else {
				//								this.outPriority = other;
				//							}
				//						} else if (priorityInBicycle  && !priorityOutBicycle){
				//							if(otherInCar){
				//								this.outPriority = other;
				//							} else {
				//								this.outPriority = guess;
				//							}
				//						} 
				//
				//					} else {
				//						System.err.println("Fatal error, large capacity has no valid links...");
				//					}
				//
				//				} else if(carInLinks.length == 6){
				//					System.out.println("6: Not an optimal way to do it.");
				//					int guess = (this.inPriority + 3) % 6;
				//					if(entry.getValue().contains(guess)){
				//						this.outPriority = guess;
				//					} else {
				//						System.out.println("Rethink this 6");
				//						System.exit(-1);
				//					}
				//				} else if(carInLinks.length == 5){
				//					int guess = (this.inPriority + 2) % 5;
				//					if(entry.getValue().contains(guess)){
				//						//5: Not an optimal way to do it...
				//						this.outPriority = guess;
				//					} else {
				//						guess = (this.inPriority + 3) % 5;
				//						if(entry.getValue().contains(guess)){
				//							//"5: Even less optimal way to do it.
				//							this.outPriority = guess;
				//						} else {
				//							System.out.println("Rethink this 5");
				//							System.exit(-1);
				//						}
				//					}
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
		double nowish = getNowPlusDelay(now);

		List<Integer> primaryInLinkOrder = new LinkedList<Integer>();
		List<Integer> secondaryInLinkOrder = new LinkedList<Integer>();

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
			this.qNode.active.set(false);
			return false;
		}


		// A)
		if(!primaryInLinkOrder.isEmpty()){
			Collections.shuffle(primaryInLinkOrder, random);

			//1 ) priorityMovesForBicycles
			for(int i : primaryInLinkOrder){
				bicycleMove(i, now, nowish, new RightPriorityBicycleTimeoutModifier()); // includes step-wise left turns
			}
			//2 ) straight or right moves from priority for cars
			for(int i : primaryInLinkOrder){
				straightOrRightTurnCarMove(i, now, nowish, new RightPriorityCarTimeoutModifier()); // left turns handled below
			}
			//3 ) left turn moves for cars from priority
			for(int i : primaryInLinkOrder){
				leftTurnCarMove(i, now, nowish, new PriorityLeftTurnCarTimeoutModifier()); // includes step-wise left turns
			}
		}


		// B) secondary inlinks.
		if(!secondaryInLinkOrder.isEmpty()){
			Collections.shuffle(secondaryInLinkOrder, random);

			//4 ) secondary MovesForBicycles 
			for(int i : secondaryInLinkOrder){
				bicycleMove(i, now, nowish, new SecondaryBicycleTimeoutModifier()); // includes step-wise left turns

			}
			//5 ) straight or right moves from secondary for cars
			for(int i : secondaryInLinkOrder){
				//TODO: Is this the correct timeoutModifier to use here???
				straightOrRightTurnCarMove(i, now, nowish, new PriorityLeftTurnCarTimeoutModifier()); // left turns handled below
			}
			//6 ) left turn moves for cars from secondary
			for(int i : secondaryInLinkOrder){
				//TODO: Is this the correct timeoutModifier to use here???
				leftTurnCarMove(i, now, nowish, new PriorityLeftTurnCarTimeoutModifier()); // includes step-wise left turns
			}
		}

		return true;
	}


	private int increaseInt(int i){
		if(i == carInLinks.length -1){
			return 0;
		} else {
			return i + 1;
		}
	}


	protected void leftTurnCarMove(final int inDirection, 
			final double now, final double nowish, TimeoutModifierI timeoutModifier) {

		QLinkI inLink = carInLinks[inDirection];
		if(inLink != null){
			for(QLaneI lane : inLink.getOfferingQLanes()){
				while(! lane.isNotOfferingVehicle()){
					QVehicle veh = lane.getFirstVehicle();
					Id<Link> nextLinkId = veh.getDriver().chooseNextLinkId();
					int outDirection = carOutTransformations.get(nextLinkId);
					if(carTimeouts[inDirection][outDirection] <= now && carLeftTurns[inDirection][outDirection]){
						if (! this.qNode.moveVehicleOverNode(veh, inLink, lane, now )) {
							break;
						}
						timeoutModifier.updateTimeouts(bicycleTimeouts, carTimeouts, inDirection, outDirection,
								isSecondary, nowish);
					} else {
						break;
					}
				}
			}
		}
	}



	private void moveLeftTurningBicyclePartiallyOverNode( final QVehicle veh, final QLaneI fromLane, final double now, final int tempBundle ) {
		QLinkImpl qLink =  (QLinkImpl) bicycleInLinks[tempBundle];
		QCycleLaneWithSublinks qCycleLink = (QCycleLaneWithSublinks) qLink.getAcceptingQLane();
		qCycleLink.placeVehicleAtFront(veh); // Place vehicle in front of temporary queue
		fromLane.popFirstVehicle(); // Remove veh from previous queue
		qCycleLink.activateLink();  // Activate the temporary link
	}




	protected void straightOrRightTurnCarMove(final int inDirection, 
			final double now, final double nowish, TimeoutModifierI timeoutModifier) {

		LinkedList<QVehicle> temporarilyRemoved;

		QLinkI inLink = carInLinks[inDirection];
		if(inLink != null){
			for(QLaneI lane : inLink.getOfferingQLanes()){
				temporarilyRemoved = new LinkedList<QVehicle>();
				while(! lane.isNotOfferingVehicle()){
					QVehicle veh = lane.getFirstVehicle();
					Id<Link> nextLinkId = veh.getDriver().chooseNextLinkId();
					int outDirection = carOutTransformations.get(nextLinkId);
					if(carTimeouts[inDirection][outDirection] <= now){
						if(carLeftTurns[inDirection][outDirection]){
							temporarilyRemoved.addFirst(lane.popFirstVehicle());
						} else {
							if (! this.qNode.moveVehicleOverNode(veh, inLink, lane, now )) {
								break;
							}
							timeoutModifier.updateTimeouts(bicycleTimeouts, carTimeouts, 
									inDirection, outDirection, isSecondary, nowish);
						}
					} else {
						break;
					}
				}

				//Inserting left turning vehicles again....
				if(lane instanceof QCycleLaneWithSublinks){
					QCycleLaneWithSublinks qCycleLane = (QCycleLaneWithSublinks) lane;
					for(QVehicle veh : temporarilyRemoved){
						qCycleLane.addVehicleToFrontOfLeavingVehicles(veh);
					}
				} else if (lane instanceof QueueWithBuffer) {
					QueueWithBuffer qCycleLane = (QueueWithBuffer) lane;
					for(QVehicle veh : temporarilyRemoved){
						qCycleLane.addToFrontOfBuffer(veh);
					}
				}
			}
		}
	}


}
