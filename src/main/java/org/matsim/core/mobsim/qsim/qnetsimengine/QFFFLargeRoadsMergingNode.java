package org.matsim.core.mobsim.qsim.qnetsimengine;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;

public class QFFFLargeRoadsMergingNode extends QFFFAbstractNode{

	private static final Logger log = Logger.getLogger(QFFFLargeRoadsMergingNode.class);


	private int majorInLink = -1;
	private int minorInLink = -1;
	private double minorProb = 0;
	final HashMap<Id<Link>, Integer> carInTransformations;
	

	QFFFLargeRoadsMergingNode(final QFFFNode qNode, final TreeMap<Double, LinkedList<Link>> thetaMap, QNetwork qNetwork, Scenario scenario){
		super(qNode, thetaMap, qNetwork, scenario);
		this.carInTransformations = new HashMap<Id<Link>, Integer>();
		int outLink = -1;
		for(int i = 0; i < carInLinks.length; i++){
			if(carInLinks[i] != null){
				this.carInTransformations.put(carInLinks[i].getLink().getId(), i);
			} else {
				outLink = i;
			}
		}
		this.majorInLink = (outLink + 1)  % carInLinks.length;
		this.minorInLink = (this.majorInLink + 1) % carInLinks.length;
		this.minorProb = carInLinks[this.minorInLink].getLink().getCapacity() / 
				(carInLinks[this.minorInLink].getLink().getCapacity() + carInLinks[this.majorInLink].getLink().getCapacity() );
		if(minorProb > 0.5) {
			int temp = this.majorInLink;
			this.majorInLink = this.minorInLink;
			this.minorInLink = temp;
			this.minorProb = 1 - this.minorProb;
		}
	}

	/*
	protected void determineLinks(){
		double highestCapacity = -1;
		for( int k : carInTransformations.values()){
			QLinkI link = carInLinks[k];
			double cap = link.getLink().getCapacity();
			if(cap > highestCapacity){
				highestCapacity = cap;
				this.majorInLink = k;
			}	
		}
		for(int k : carInTransformations.values()){
			if(k != this.majorInLink){
				this.minorInLink = k;
				break;
			}
		}

		double minorCap = 	carInLinks[minorInLink].getLink().getCapacity();
		double majorCap = carInLinks[majorInLink].getLink().getCapacity();

		Gbl.assertIf(this.inPriority != this.outPriority);
	}
	 */

	protected boolean doSimStep(final double now){

		boolean majorInLinkMoves = false;
		QLinkI qLink = carInLinks[majorInLink];
		if(qLink != null){
			for(QLaneI qLane : qLink.getOfferingQLanes()){
				if(!qLane.isNotOfferingVehicle()){
					majorInLinkMoves = true;
					break;
				}
			}
		}

		boolean minorInLinkMoves = false;
		qLink = carInLinks[minorInLink];
		if(qLink != null){
			for(QLaneI qLane : qLink.getOfferingQLanes()){
				if(!qLane.isNotOfferingVehicle()){
					minorInLinkMoves = true;
					break;
				}
			}
		}

		if(!majorInLinkMoves && !minorInLinkMoves){
			this.qNode.setActive( false ) ;
			return false;
		}
		
		if(!majorInLinkMoves) {
			highwayMove(now, minorInLink);	
		} else if(!minorInLinkMoves) {
			highwayMove(now, majorInLink);
		} else {
			QLaneI majorQLane = carInLinks[majorInLink].getAcceptingQLane();
			if(majorQLane.getLoadIndicator() >= majorQLane.getStorageCapacity()  ) {
				highwayMove(now, majorInLink);
			} else if(random.nextDouble() < minorProb){
				highwayMove(now, minorInLink);
				highwayMove(now, majorInLink);
			} else {
				highwayMove(now, majorInLink);
				highwayMove(now, minorInLink);
			}
		}
		
		return true;
	}



	private void highwayMove(double now, int inDirection){
		QLinkI inLink = carInLinks[inDirection];
		if(inLink != null){
			for(QLaneI qLaneI : inLink.getOfferingQLanes()){
				QueueWithBufferForRoW lane = (QueueWithBufferForRoW) qLaneI;
				while(! lane.isNotOfferingGeneralVehicle()){
					QVehicleAndMoveType veh = (QVehicleAndMoveType) lane.getFirstGeneralVehicle();
					if (! this.qNode.moveVehicleOverNode(veh, inLink, lane, now, veh.getMoveType(), false)) {
						break;
					}
				}
			}
		}
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
