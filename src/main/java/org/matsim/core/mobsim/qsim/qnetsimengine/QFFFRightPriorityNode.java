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

public class QFFFRightPriorityNode extends QFFFAbstractNode{


	QFFFRightPriorityNode(final QFFFNode qNode, final TreeMap<Double, LinkedList<Link>> bundleMap, QNetwork qNetwork){
		super(qNode, bundleMap, qNetwork);
	}


	protected boolean doSimStep(final double now){	
		ArrayList<Integer> inLinkOrder = new ArrayList<Integer>(carInLinks.length);

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
			this.qNode.setActive(false);
			return false;
		}

		double nowishBicycle = getNowPlusDelayBicycle(now);
		double nowishCar = getNowPlusDelayCar(now);

		int n = inLinkOrder.size();
		int i = (n == 1) ? 0 : random.nextInt(n);
		for(int count = 0; count < n; count++){
			int direction = inLinkOrder.get(i);
			bicycleMoveWithFullLeftTurns(direction, now, nowishBicycle,
					new RightPriorityBicycleTimeoutModifier());
			carMoveAllowingLeftTurns(direction, now, nowishCar,
					new RightPriorityCarTimeoutModifier());
			i = QFFFNodeUtils.increaseInt(i, n);
		}

		return true;
	}

}
