package org.matsim.core.mobsim.qsim.qnetsimengine;

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

		if(!primaryInLinkOrder.isEmpty()){
			Collections.shuffle(primaryInLinkOrder, random);
			for(int i : primaryInLinkOrder){
				bicycleMoveWithFullLeftTurns(i, now, nowish, new RightPriorityBicycleTimeoutModifier());
				carMoveAllowingLeftTurns(i, now, nowish, new RightPriorityCarTimeoutModifier());
			}
		}
		if(!secondaryInLinkOrder.isEmpty()){
			Collections.shuffle(primaryInLinkOrder, random);
			for(int i : secondaryInLinkOrder){
				bicycleMoveWithFullLeftTurns(i, now, nowish, new RightPriorityBicycleTimeoutModifier());
				carMoveAllowingLeftTurns(i, now, nowish, new RightPriorityCarTimeoutModifier());
			}
		}

		return true;
	}

}
