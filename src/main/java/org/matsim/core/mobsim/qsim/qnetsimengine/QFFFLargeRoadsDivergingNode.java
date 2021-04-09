package org.matsim.core.mobsim.qsim.qnetsimengine;

import java.util.LinkedList;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;

public class QFFFLargeRoadsDivergingNode extends QFFFNodeWithLeftBuffer{

	private static final Logger log = Logger.getLogger(QFFFLargeRoadsDivergingNode.class);
	private final int leftOutLinkDirection; //(doesn't matter which is left and which is right)
	private int onlyInDirection;

	QFFFLargeRoadsDivergingNode(final QFFFNode qNode, final TreeMap<Double, LinkedList<Link>> thetaMap, QNetwork qNetwork,
			Id<Link> idOfLargestOutLink, Scenario scenario){
		super(qNode, thetaMap, qNetwork, scenario);
		this.leftOutLinkDirection = carOutTransformations.get(idOfLargestOutLink);
		for(int i = 0; i < carInLinks.length; i++) {
			if(carInLinks[i] != null) {
				this.onlyInDirection = i;
			}
		}
	}


	protected boolean doSimStep(final double now){
		QLinkI qLink = carInLinks[this.onlyInDirection];
		boolean someKindOfMove = false;
		for(QLaneI qLane : qLink.getOfferingQLanes()){
			if(!((QueueWithBufferForRoW) qLane).isNotOfferingVehicle()) {
				someKindOfMove = true;
				break;
			}
		}


		if(!someKindOfMove){
			this.qNode.setActive( false ) ;
			return false;
		}

		move(now);

		return true;
	}



	private void move(double now){
		QLinkI inLink = carInLinks[this.onlyInDirection];
		for(QLaneI laneI : inLink.getOfferingQLanes()){
			QueueWithBufferForRoW lane = (QueueWithBufferForRoW) laneI;
			while(! lane.isNotOfferingGeneralVehicle() ){
				QVehicleAndMoveType veh = (QVehicleAndMoveType) lane.getFirstGeneralVehicle();
				if (! this.qNode.moveVehicleOverNode(veh, inLink, lane, now, veh.getMoveType(), QFFFAbstractNode.defaultStuckReturnValue)) {
					break;
				}
			}
			while(! lane.isNotOfferingLeftVehicle() ){
				QVehicleAndMoveType veh = (QVehicleAndMoveType)  lane.getFirstLeftVehicle();
				if (! this.qNode.moveVehicleOverNode(veh, inLink, lane, now, veh.getMoveType(), QFFFAbstractNode.defaultStuckReturnValue)) {
					break;
				}
			}
		}
	}

	@Override
	public boolean isCarLeftTurn(Id<Link> fromLinkId, int outDirection) {
		return outDirection == this.leftOutLinkDirection;
	}


	@Override
	int[][] createBicycleRankMatrix(int n) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	int[][] createCarRankMatrix(int n) {
		// TODO Auto-generated method stub
		return null;
	}

}