package org.matsim.core.mobsim.qsim.qnetsimengine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.qsim.qnetsimengine.QLaneI;
import org.matsim.core.mobsim.qsim.qnetsimengine.QLinkI;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetwork;

import fastOrForcedToFollow.timeoutmodifiers.RightPriorityBicycleTimeoutModifier;
import fastOrForcedToFollow.timeoutmodifiers.RightPriorityCarTimeoutModifier;

// Intersection type with "anti priority" links. Corresponding to links with full stop, and all other links handled with right priority.

public class QFFFAntiPriorityNode extends QFFFAbstractNode {

	private boolean[] isSecondary;
	private int lastPriorityDirection = 0;
	private boolean lastPriorityWasSecondary = false;




	protected QFFFAntiPriorityNode(final QFFFNode qNode, final TreeMap<Double, LinkedList<Link>> bundleMap, QNetwork qNetwork,
			final TreeMap<HierarchyInformation, LinkedList<Integer>> hierarchyInformations) {
		super(qNode, bundleMap, qNetwork);
		this.isSecondary = new boolean[carInLinks.length];
		for(int i : hierarchyInformations.lastEntry().getValue()){
			isSecondary[i] = false;
		}
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


		double nowishBicycle = getNowPlusDelayBicycle(now) + 1;
		double nowishCar = getNowPlusDelayCar(now) + 1;

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
				boolean bicycleBool = bicycleMoveWithFullLeftTurns(direction, now, nowishBicycle,
						new RightPriorityBicycleTimeoutModifier());
				boolean carBool = carMoveAllowingLeftTurns(direction, now, nowishCar, 
						new RightPriorityCarTimeoutModifier());
				if(lastPriorityDirection == -1 && (bicycleBool || carBool)) {
					lastPriorityDirection = direction;
				}
				i = QFFFNodeUtils.increaseInt(i, n);
			}
		}



		if(!secondaryInLinkOrder.isEmpty()){
			nowishBicycle--;
			nowishCar--;	
			int n = secondaryInLinkOrder.size();
			if(n == 1) {
				i = 0;
			} else if(lastPriorityDirection >=0) {
				 i = n-1;
				while(i == 0 || secondaryInLinkOrder.get(i) > lastPriorityDirection) {
					i--;
				}
			} else if(lastPriorityWasSecondary && priorityDirectionNow >= 0) {
				i = priorityDirectionNow;
			} else {
				i = random.nextInt(n);
			}

			for(int count = 0; count < n; count++){
				int direction = secondaryInLinkOrder.get(i);
				boolean bicycleBool = bicycleMoveWithFullLeftTurns(direction, now, nowishBicycle,
						new RightPriorityBicycleTimeoutModifier());
				boolean carBool = carMoveAllowingLeftTurns(direction, now, nowishCar,
						new RightPriorityCarTimeoutModifier());
				if(lastPriorityDirection == -1 && (bicycleBool || carBool)) {
					lastPriorityDirection = direction;
				}
				i = QFFFNodeUtils.increaseInt(i, n);
			}
		}

		return true;
	}

}
