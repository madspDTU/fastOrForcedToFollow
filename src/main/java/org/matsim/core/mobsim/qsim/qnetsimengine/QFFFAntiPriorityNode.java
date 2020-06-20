package org.matsim.core.mobsim.qsim.qnetsimengine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.qsim.qnetsimengine.QLaneI;
import org.matsim.core.mobsim.qsim.qnetsimengine.QLinkI;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetwork;

import fastOrForcedToFollow.timeoutmodifiers.RightPriorityBicycleTimeoutModifier;
import fastOrForcedToFollow.timeoutmodifiers.RightPriorityCarTimeoutModifier;

// Intersection type with "anti priority" links. Corresponding to links with full stop, and all other links handled with right priority.

public class QFFFAntiPriorityNode extends QFFFNodeWithLeftBuffer { //implements HasLeftBuffer {

	private boolean[] isSecondary;
	private int lastPriorityDirection = 0;
	private boolean lastPriorityWasSecondary = false;
	




	protected QFFFAntiPriorityNode(final QFFFNode qNode, final TreeMap<Double, LinkedList<Link>> bundleMap, QNetwork qNetwork,
			final TreeMap<HierarchyInformation, LinkedList<Integer>> hierarchyInformations) {
		super(qNode, bundleMap, qNetwork);
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
		this.bicycleTurns = createBicycleTurnsForNonPrioritisedNodes();
	}

	@Override
	boolean doSimStep(double now) {

		ArrayList<Integer> primaryInLinkOrder = new ArrayList<Integer>(carInLinks.length); 
		ArrayList<Integer> secondaryInLinkOrder = new ArrayList<Integer>(carInLinks.length);

		int priorityDirectionNow = -1;
		outerLoop: 
			for(int i = 0; i < carInLinks.length; i++){
				QLinkI qLink = carInLinks[i];
				if(qLink != null){
					for(QLaneI qLane : qLink.getOfferingQLanes()){
						if(!qLane.isNotOfferingVehicle()){
							if(isSecondary[i]){
								if(lastPriorityWasSecondary && i == lastPriorityDirection) {
									priorityDirectionNow = secondaryInLinkOrder.size();
								}
								secondaryInLinkOrder.add(i);
							} else {
								if(!lastPriorityWasSecondary && i == lastPriorityDirection) {
									priorityDirectionNow = primaryInLinkOrder.size();
								}
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
								if(lastPriorityWasSecondary && i == lastPriorityDirection) {
									priorityDirectionNow = secondaryInLinkOrder.size();
								}
								secondaryInLinkOrder.add(i);
							} else {
								if(!lastPriorityWasSecondary && i == lastPriorityDirection) {
									priorityDirectionNow = primaryInLinkOrder.size();
								}
								primaryInLinkOrder.add(i);
							}
							continue outerLoop;
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
				boolean bicycleBool = bicycleMoveDirectedPriority(direction, now, nowishBicycle, new RightPriorityBicycleTimeoutModifier());
				boolean carBool = carMovesRightPriority(direction, now, nowishCar, 
						new RightPriorityCarTimeoutModifier());
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
				boolean bicycleBool = bicycleMoveDirectedPriority(direction, now, nowishBicycle,
						new RightPriorityBicycleTimeoutModifier());
				boolean carBool = carMovesRightPriority(direction, now, nowishCar,
						new RightPriorityCarTimeoutModifier());
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
