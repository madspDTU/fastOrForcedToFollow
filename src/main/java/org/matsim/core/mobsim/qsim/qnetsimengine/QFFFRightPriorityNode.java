package org.matsim.core.mobsim.qsim.qnetsimengine;

import java.util.ArrayList;
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

public class QFFFRightPriorityNode extends QFFFAbstractNode{ // implements HasLeftBuffer {


	private HashMap<Id<Link>, Integer> carInTransformations;
	private int lastPriorityDirection = -1;


	QFFFRightPriorityNode(final QFFFNode qNode, final TreeMap<Double, LinkedList<Link>> bundleMap, QNetwork qNetwork){
		super(qNode, bundleMap, qNetwork);
		this.carInTransformations = new HashMap<Id<Link>, Integer>();
		for(int i = 0; i < carInLinks.length; i++){
			if(carInLinks[i] != null){
				this.carInTransformations.put(carInLinks[i].getLink().getId(), i);
			}
		}
	}


	protected boolean doSimStep(final double now){	
		ArrayList<Integer> inLinkOrder = new ArrayList<Integer>(carInLinks.length);

		int priorityDirectionNow = -1;
		outerLoop: 
			for(int i = 0; i < carInLinks.length; i++){
				QLinkI qLink = carInLinks[i];
				if(qLink != null){
					for(QLaneI qLane : qLink.getOfferingQLanes()){
						if(!qLane.isNotOfferingVehicle()){
							if(i == lastPriorityDirection) {
								priorityDirectionNow = inLinkOrder.size();
							}
							inLinkOrder.add(i);
							continue outerLoop;
						}
					}
				}
				qLink = bicycleInLinks[i];
				if(qLink != null){
					for(QLaneI qLane : qLink.getOfferingQLanes()){
						if(!qLane.isNotOfferingVehicle()){
							if(i == lastPriorityDirection) {
								priorityDirectionNow = inLinkOrder.size();
							}
							inLinkOrder.add(i);
							continue outerLoop;
						}
					}
				}
			}

		lastPriorityDirection = -1;
		if(inLinkOrder.isEmpty()){
			this.qNode.setActive(false);
			return false;
		}

		double nowishBicycle = getNowPlusDelayBicycle(now) + 1;
		double nowishCar = getNowPlusDelayCar(now) + 1;

		int n = inLinkOrder.size();
		int i;
		if (n == 1) {
			i = 0;
		} else 	if(priorityDirectionNow  >= 0) {
			i = priorityDirectionNow;
		} else {
			i = random.nextInt(n);
		}
		for(int count = 0; count < n; count++){
			int direction = inLinkOrder.get(i);
			boolean bicycleReturn = bicycleMoveWithFullLeftTurns(direction, now, nowishBicycle,
					new RightPriorityBicycleTimeoutModifier());
			boolean carReturn = carMoveAllowingLeftTurns(direction, now, nowishCar,
					new RightPriorityCarTimeoutModifier());
			if(lastPriorityDirection == -1 && (bicycleReturn || carReturn)) {
				lastPriorityDirection = direction;
			}
			i = QFFFNodeUtils.increaseInt(i, n);
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
