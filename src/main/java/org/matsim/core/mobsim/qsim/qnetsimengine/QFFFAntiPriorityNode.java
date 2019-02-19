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

// Intersection type with "anti priority" links. Corresponding to links with full stop, and all links handled with right priority.

public class QFFFAntiPriorityNode extends QFFFAbstractNode {

	private boolean[] isSecondary;


	protected QFFFAntiPriorityNode(final QFFFNode qNode, final TreeMap<Double, LinkedList<Link>> bundleMap, QNetwork qNetwork,
			final TreeMap<Double,LinkedList<Integer>> capacities) {
		super(qNode, bundleMap, qNetwork);
		this.isSecondary = new boolean[carInLinks.length];
		for(int i : capacities.lastEntry().getValue()){
			isSecondary[i] = false;
		}
	}

	@Override
	boolean doSimStep(double now) {
			
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
			this.qNode.active.set(false);
			return false;
		}

		
		double nowishBicycle = getNowPlusDelayBicycle(now);
		double nowishCar = getNowPlusDelayCar(now);

		
		if(!primaryInLinkOrder.isEmpty()){

			int n = primaryInLinkOrder.size();
			int i = (n == 0) ? 1 : random.nextInt(n);
		
			for(int count = 0; count < n; count++){
				int direction = primaryInLinkOrder.get(i);
				bicycleMoveWithFullLeftTurns(direction, now, nowishBicycle,
						new RightPriorityBicycleTimeoutModifier());
				carMoveAllowingLeftTurns(direction, now, nowishCar, 
						new RightPriorityCarTimeoutModifier());
				i = QFFFNodeUtils.increaseInt(i, n);
			}
		}
		if(!secondaryInLinkOrder.isEmpty()){
			
			int n = secondaryInLinkOrder.size();
			int i = (n == 0) ? 1 : random.nextInt(n);
			
			for(int count = 0; count < n; count++){
				int direction = secondaryInLinkOrder.get(i);
				bicycleMoveWithFullLeftTurns(direction, now, nowishBicycle,
						new RightPriorityBicycleTimeoutModifier());
				carMoveAllowingLeftTurns(direction, now, nowishCar,
						new RightPriorityCarTimeoutModifier());
				i = QFFFNodeUtils.increaseInt(i, n);
			}
		}

		return true;
	}

}
