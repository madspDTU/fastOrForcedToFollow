package org.matsim.core.mobsim.qsim.qnetsimengine;

import java.util.LinkedList;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.qsim.qnetsimengine.QFFFNode.MoveType;

public class QFFFLargeRoadsDivergingNode extends QFFFAbstractNode implements HasLeftBuffer{

	private static final Logger log = Logger.getLogger(QFFFLargeRoadsDivergingNode.class);
	private final Id<Link> leftOutLink; //(doesn't matter which is left and which is right)
	private int onlyInDirection;

	QFFFLargeRoadsDivergingNode(final QFFFNode qNode, final TreeMap<Double, LinkedList<Link>> thetaMap, QNetwork qNetwork,
			Id<Link> idOfLargestOutLink){
		super(qNode, thetaMap, qNetwork);
		this.leftOutLink = idOfLargestOutLink;
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
			if(!((QueueWithBufferForRoW) qLane).isOfferingNeitherGeneralNorLeftVehicle()) {
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
			while(! lane.isNotOfferingGeneralVehicle(this) ){
				QVehicle veh = lane.getFirstGeneralVehicle();
				if (! this.qNode.moveVehicleOverNode(veh, inLink, lane, now, MoveType.GENERAL, QFFFAbstractNode.defaultStuckReturnValue)) {
					break;
				}
			}
			while(! lane.isNotOfferingLeftVehicle(this) ){
				QVehicle veh = lane.getFirstLeftVehicle();
				if (! this.qNode.moveVehicleOverNode(veh, inLink, lane, now, MoveType.LEFT_TURN, QFFFAbstractNode.defaultStuckReturnValue)) {
					break;
				}
			}
		}
	}

	public boolean isCarLeftTurn(Id<Link> fromLinkId, Id<Link> toLinkId) {
		return toLinkId.equals(this.leftOutLink);
	}

}