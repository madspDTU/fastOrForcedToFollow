package org.matsim.core.mobsim.qsim.qnetsimengine;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.jfree.util.Log;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.qsim.qnetsimengine.QFFFNode.MoveType;
import org.matsim.core.trafficmonitoring.FFFTravelTimeCalculator;

public class QFFFLargeRoadsMergingNode extends QFFFAbstractNode{

	private static final Logger log = Logger.getLogger(QFFFLargeRoadsMergingNode.class);


	private int majorInLink = -1;
	private int minorInLink = -1;
	private double minorProb = 0;
	final HashMap<Id<Link>, Integer> carInTransformations;
	

	QFFFLargeRoadsMergingNode(final QFFFNode qNode, final TreeMap<Double, LinkedList<Link>> thetaMap, QNetwork qNetwork){
		super(qNode, thetaMap, qNetwork);
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

		QLaneI majorQLane = carInLinks[majorInLink].getAcceptingQLane();
		if(minorInLinkMoves){ // Can be nullified if other lane is fully packed.
			minorInLinkMoves = majorQLane.getStorageCapacity() > majorQLane.getLoadIndicator();
//			if(minorInLinkMoves) {
//				log.info("minorInLinkMove accepted     " + majorQLane.getStorageCapacity() + " > " +  majorQLane.getLoadIndicator());
//			} else {
//				log.info("minorInLinkMove prohibited   " + majorQLane.getStorageCapacity() + " <=  " +  majorQLane.getLoadIndicator());
//			}
		}

		if(majorInLinkMoves && minorInLinkMoves){
			if(random.nextDouble() < minorProb){
				highwayMove(now, minorInLink);
				highwayMove(now, majorInLink);
			} else {
				highwayMove(now, majorInLink);	
				highwayMove(now, minorInLink);
			}
		} else if(majorInLinkMoves){
			highwayMove(now, majorInLink);
		} else if(minorInLinkMoves){
			highwayMove(now, minorInLink);		
		} 

		return true;
	}



	private void highwayMove(double now, int inDirection){
		QLinkI inLink = carInLinks[inDirection];
		if(inLink != null){
			for(QLaneI lane : inLink.getOfferingQLanes()){
				while(! lane.isNotOfferingVehicle()){
					QVehicle veh = lane.getFirstVehicle();
					if (! this.qNode.moveVehicleOverNode(veh, inLink, lane, now, MoveType.GENERAL, false)) {
						break;
					}
				}
			}
		}
	}

}
