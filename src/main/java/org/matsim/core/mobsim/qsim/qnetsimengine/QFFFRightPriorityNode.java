package org.matsim.core.mobsim.qsim.qnetsimengine;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.TreeMap;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;


public class QFFFRightPriorityNode extends QFFFNodeWithLeftBuffer {


	private int piorityDirectionInPreviousTimestep = -1;


	QFFFRightPriorityNode(final QFFFNode qNode, final TreeMap<Double, LinkedList<Link>> bundleMap, QNetwork qNetwork, Scenario scenario){
		super(qNode, bundleMap, qNetwork, scenario);
		this.bicycleTurns = createBicycleTurnsForNonPrioritisedNodes(carInLinks.length);
		this.conflictingMovesData = createConflictingMovesData(carInLinks.length);
	}


	protected boolean doSimStep(final double now){	
		ArrayList<Integer> inLinkOrder = new ArrayList<Integer>(carInLinks.length);

		// The index in inLinkOrder corresponding to the current priority direction. 
		int priorityDirectionNow = -1;
		outerLoop: 
			for(int i = 0; i < carInLinks.length; i++){
				if(simulateCars) {
					QLinkI qLink = carInLinks[i];
					if(qLink != null){
						for(QLaneI qLane : qLink.getOfferingQLanes()){
							if(!qLane.isNotOfferingVehicle()){
								if(i == piorityDirectionInPreviousTimestep) {
									priorityDirectionNow = inLinkOrder.size(); //Since it is index, not link directly.
								}
								inLinkOrder.add(i);
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
								if(i == piorityDirectionInPreviousTimestep) {
									priorityDirectionNow = inLinkOrder.size(); //Since it is index, not link directly.
								}
								inLinkOrder.add(i);
								continue outerLoop;
							}
						}
					}
				}
			}

		piorityDirectionInPreviousTimestep = -1;
		if(inLinkOrder.isEmpty()){
			this.qNode.setActive(false);
			return false;
		}

		double nowishBicycle = getNowPlusDelayBicycle(now);
		double nowishCar = getNowPlusDelayCar(now);
		double nowishBicycle2 = getNowPlusTwoTimesDelayBicycle(now);
		double nowishCar2 = getNowPlusTwoTimesDelayCar(now);
	

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
			boolean bicycleReturn = false;
			boolean carReturn = false;
			if(simulateBicycles) {
				bicycleReturn = bicycleMoveDirectedPriority(direction, now, nowishBicycle, nowishBicycle2, conflictingMovesData);
			}
			if(simulateCars) {
				carReturn = carMovesRightPriority(direction, now, nowishCar, nowishCar2, conflictingMovesData);
			}
			// If any moves happended: Flagging that this was priority direction in this timestep (used in next timestep)
			if(piorityDirectionInPreviousTimestep == -1 && (bicycleReturn || carReturn)) {
				piorityDirectionInPreviousTimestep = direction;
			}
			i = QFFFNodeUtils.increaseInt(i, n);
		}

		return true;
	}


	@Override
	int[][] createBicycleRankMatrix(int n) {
		return new int[n][n];
	}


	@Override
	int[][] createCarRankMatrix(int n) {
		return createBicycleRankMatrix(n);
	}

}
