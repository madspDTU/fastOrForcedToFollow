package org.matsim.core.mobsim.qsim.qnetsimengine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;

import fastOrForcedToFollow.timeoutmodifiers.ConflictingMovesData;

// Intersection type with "anti priority" links. Corresponding to some links with full stop, and all other links handled with right priority.

public class QFFFAntiPriorityNode extends QFFFNodeWithLeftBuffer { //implements HasLeftBuffer {

	private boolean[] isSecondary;
	private int lastPriorityDirection = 0;
	private boolean lastPriorityWasSecondary = false;
	





	protected QFFFAntiPriorityNode(final QFFFNode qNode, final TreeMap<Double, LinkedList<Link>> bundleMap, QNetwork qNetwork,
			final TreeMap<HierarchyInformation, LinkedList<Integer>> hierarchyInformations, Scenario scenario) {
		super(qNode, bundleMap, qNetwork, scenario);
		int n = carInLinks.length;
		this.isSecondary = new boolean[n];
		Arrays.fill(this.isSecondary, true);
		List<Integer> highestEntry = hierarchyInformations.pollLastEntry().getValue();
		for(int i : highestEntry){
			isSecondary[i] = false;
		}
		if(highestEntry.size() == 1) {
			highestEntry = hierarchyInformations.pollLastEntry().getValue();
			if(highestEntry.size() <  n - 1) {
				for(int i : highestEntry){
					isSecondary[i] = false;
				}
			}
		}
		this.bicycleTurns = createBicycleTurnsForNonPrioritisedNodes(n);
		this.conflictingMovesData = createConflictingMovesData(n);
	}


	public ConflictingMovesData[][] getConflictingMovesData() {
		return this.conflictingMovesData;
	}

	
	
	int[][] createBicycleRankMatrix(int n) {

		// All moves from priority: 0
		// All moves from non-priority -1.

		int[][] matrix = new int[n][n];

		for(int i = 0; i < n; i++) {
			if(isSecondary[i]) {
				for(int j = 0; j < n; j++) {
					matrix[i][j] = -1;
				}
			}
		}

		return matrix;
	}

	int[][] createCarRankMatrix(int n) {
		return createBicycleRankMatrix(n);
	}
	
	
	
	
	
	
	
	
	
	


	@Override
	boolean doSimStep(double now) {

		ArrayList<Integer> primaryInLinkOrder = new ArrayList<Integer>(carInLinks.length); 
		ArrayList<Integer> secondaryInLinkOrder = new ArrayList<Integer>(carInLinks.length);

		int priorityDirectionNow = -1;
		outerLoop: 
			for(int i = 0; i < carInLinks.length; i++){
				if(simulateCars) {
					QLinkI qLink = carInLinks[i];
					if(qLink != null){
						for(QLaneI qLane : qLink.getOfferingQLanes()){
							if(!qLane.isNotOfferingVehicle()){
								if(isSecondary[i]){
									if(lastPriorityWasSecondary && i == lastPriorityDirection) {
										priorityDirectionNow = secondaryInLinkOrder.size(); //Since it is index, not link directly.
									}
									secondaryInLinkOrder.add(i);
								} else {
									if(!lastPriorityWasSecondary && i == lastPriorityDirection) {
										priorityDirectionNow = primaryInLinkOrder.size();  //Since it is index, not link directly.
									}
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
									if(lastPriorityWasSecondary && i == lastPriorityDirection) {
										priorityDirectionNow = secondaryInLinkOrder.size(); //Since it is index, not link directly.
									}
									secondaryInLinkOrder.add(i);
								} else {
									if(!lastPriorityWasSecondary && i == lastPriorityDirection) {
										priorityDirectionNow = primaryInLinkOrder.size(); //Since it is index, not link directly.
									}
									primaryInLinkOrder.add(i);
								}
								continue outerLoop;
							}
						}
					}
				}
			}
		lastPriorityDirection = -1;

		if(primaryInLinkOrder.isEmpty() && secondaryInLinkOrder.isEmpty()){
			this.qNode.setActive(false);
			return false;
		}


		double nowishBicycle = getNowPlusDelayBicycle(now);
		double nowishCar = getNowPlusDelayCar(now);
		double nowishBicycle2 = getNowPlusTwoTimesDelayBicycle(now);
		double nowishCar2 = getNowPlusTwoTimesDelayCar(now);


		int i;
		if(!primaryInLinkOrder.isEmpty()){
			int n = primaryInLinkOrder.size();
			if(n == 1) {
				i = 0;
			} else if(!lastPriorityWasSecondary && priorityDirectionNow >= 0) {
				i = priorityDirectionNow;
			} else {
				i = random.nextInt(n);
			}
			for(int count = 0; count < n; count++){
				int direction = primaryInLinkOrder.get(i);
				boolean bicycleBool = false;
				boolean carBool = false;
				if(simulateBicycles) {
					bicycleBool = bicycleMoveDirectedPriority(direction, now, nowishBicycle, nowishBicycle2, conflictingMovesData);
				}
				if(simulateCars) {
					carBool = carMovesRightPriority(direction, now, nowishCar, nowishCar2, conflictingMovesData);
				}
				if(lastPriorityDirection == -1 && (bicycleBool || carBool)) {
					lastPriorityDirection = direction;
				}
				i = QFFFNodeUtils.increaseInt(i, n);
			}
		}



		if(!secondaryInLinkOrder.isEmpty()){
			int n = secondaryInLinkOrder.size();
			if(n == 1) {
				i = 0;
			} else if(lastPriorityDirection >=0) {
				i = 0;
				while(i < n && secondaryInLinkOrder.get(i) <= lastPriorityDirection ) {
					i++;
				}
				if(i == n) {
					i = 0;
				}
			} else if(lastPriorityWasSecondary && priorityDirectionNow >= 0) {
				i = priorityDirectionNow;
			} else {
				i = random.nextInt(n);
			}

			for(int count = 0; count < n; count++){
				int direction = secondaryInLinkOrder.get(i);
				boolean bicycleBool = bicycleMoveDirectedPriority(direction, now, nowishBicycle, nowishBicycle2, conflictingMovesData);
				boolean carBool = carMovesRightPriority(direction, now, nowishCar, nowishCar2, conflictingMovesData);
				if(lastPriorityDirection == -1 && (bicycleBool || carBool)) {
					lastPriorityDirection = direction;
				}
				i = QFFFNodeUtils.increaseInt(i, n);
			}
		}

		return true;
	}




	//	@Override
	//	public boolean isCarLeftTurn(Id<Link> fromLink, Id<Link> toLink) {
	//		int inDirection = carInTransformations.get(fromLink) + 1;
	//		int outDirection = carOutTransformations.get(toLink);
	//
	//		if(inDirection == carInLinks.length) {
	//			return outDirection != 0;
	//		} else {
	//			return inDirection != outDirection;
	//		}
	//	}
}
