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

public class QFFFRightPriorityNode extends QFFFAbstractNode{


	QFFFRightPriorityNode(final QFFFNode qNode, final TreeMap<Double, LinkedList<Link>> bundleMap, QNetwork qNetwork){
		super(qNode, bundleMap, qNetwork);
	}


	protected boolean doSimStep(final double now){
		double nowish = getNowPlusDelay(now);
		List<Integer> inLinkOrder = new LinkedList<Integer>();

		outerLoop: 
			for(int i = 0; i < carInLinks.length; i++){
				QLinkI qLink = carInLinks[i];
				if(qLink != null){
					for(QLaneI qLane : qLink.getOfferingQLanes()){
						if(!qLane.isNotOfferingVehicle()){
							inLinkOrder.add(i);
							continue outerLoop;
						}
					}
				}
				qLink = bicycleInLinks[i];
				if(qLink != null){
					for(QLaneI qLane : qLink.getOfferingQLanes()){
						if(!qLane.isNotOfferingVehicle()){
							inLinkOrder.add(i);
							continue outerLoop;
						}
					}
				}
			}

		if(inLinkOrder.isEmpty()){
			this.qNode.active.set(false);
			return false;
		}

		Collections.shuffle(inLinkOrder, random);

		for(int i : inLinkOrder){
			bicycleMoveWithFullLeftTurns(i, now, nowish, new RightPriorityBicycleTimeoutModifier());
			carMoveAllowingLeftTurns(i, now, nowish, new RightPriorityCarTimeoutModifier());
		}
		return true;
	}

	

	


}
