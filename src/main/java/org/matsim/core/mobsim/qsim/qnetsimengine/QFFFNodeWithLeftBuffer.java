package org.matsim.core.mobsim.qsim.qnetsimengine;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;

public abstract class QFFFNodeWithLeftBuffer extends QFFFAbstractNode {
	
		HashMap<Id<Link>, Integer> carInTransformations;
		HashMap<Id<Link>, Integer> bicycleInTransformations;

	
		public QFFFNodeWithLeftBuffer(final QFFFNode qNode, final TreeMap<Double, LinkedList<Link>> bundleMap, QNetwork qNetwork, Scenario scenario) {
			super(qNode, bundleMap, qNetwork, scenario);
			this.carInTransformations = new HashMap<Id<Link>, Integer>();
			for(int i = 0; i < carInLinks.length; i++){
				if(carInLinks[i] != null){
					this.carInTransformations.put(carInLinks[i].getLink().getId(), i);
				}
			}
			this.bicycleInTransformations = new HashMap<Id<Link>, Integer>();
			for(int i = 0; i < bicycleInLinks.length; i++){
				if(bicycleInLinks[i] != null){
					this.bicycleInTransformations.put(bicycleInLinks[i].getLink().getId(), i);
				}
			}
		}
	
		public boolean isCarLeftTurn(Id<Link> fromLink, int outDirection) {
			int inDirection = carInTransformations.get(fromLink) + 1;
			//int outDirection = carOutTransformations.get(toLink);
	
			if(inDirection == carInLinks.length) {
				return outDirection != 0;
			} else {
				return inDirection != outDirection;
			}
		}
		
		
		//Something special here... about overshooting the outdirection.
		public boolean isBicycleLeftTurn(Id<Link> fromLink, int outDirection) {
			int inDirection = bicycleInTransformations.get(fromLink);
			return increaseInt(inDirection) != outDirection;
		}
		
		//public boolean isBicycleLeftTurn(Id<Link> fromLink, int outDirection) {
		//	int inDirection = increaseInt(bicycleInTransformations.get(fromLink));
		//	return inDirection != outDirection;
			//But override this for directed priority, as straight in priority should also be consideres as a non-left move.
		//}

}
