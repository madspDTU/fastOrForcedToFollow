package org.matsim.core.mobsim.qsim.qnetsimengine;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.gbl.MatsimRandom;

public class QFFFLargeRoadsMergingNode extends QFFFAbstractNode{


	private int majorInLink = -1;
	private int minorInLink = -1;
	private double minorProb = 0;
	final HashMap<Id<Link>, Integer> carInTransformations;


	QFFFLargeRoadsMergingNode(final QFFFNode qNode, final TreeMap<Double, LinkedList<Link>> thetaMap, QNetwork qNetwork){
		super(qNode, thetaMap, qNetwork);
		this.carInTransformations = new HashMap<Id<Link>, Integer>();
		for(int i = 0; i < carInLinks.length; i++){
			if(carInLinks[i] != null){
				this.carInTransformations.put(carInLinks[i].getLink().getId(), i);
			}
		}
		determineLinks();
	}

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
		this.minorProb = minorCap / (minorCap + majorCap);
		//Gbl.assertIf(this.inPriority != this.outPriority);
	}

	protected boolean doSimStep(final double now){

		boolean highwayInLinkMoves = false;
		QLinkI qLink = carInLinks[majorInLink];
		if(qLink != null){
			for(QLaneI qLane : qLink.getOfferingQLanes()){
				if(!qLane.isNotOfferingVehicle()){
					highwayInLinkMoves = true;
					break;
				}
			}
		}

		boolean rampInLinkMoves = false;
		qLink = carInLinks[minorInLink];
		if(qLink != null){
			for(QLaneI qLane : qLink.getOfferingQLanes()){
				if(!qLane.isNotOfferingVehicle()){
					rampInLinkMoves = true;
					break;
				}
			}
		}

		if(!highwayInLinkMoves && !rampInLinkMoves){
			this.qNode.setActive( false ) ;
			return false;
		}

		QLaneI qLane = carInLinks[majorInLink].getAcceptingQLane();
		if(rampInLinkMoves){ // Can be nullified if other lane is fully packed.
		 rampInLinkMoves = qLane.getStorageCapacity() > qLane.getLoadIndicator();
		}

		if(highwayInLinkMoves && rampInLinkMoves){
			if(random.nextDouble() < minorProb){
				highwayMove(now, minorInLink);
				highwayMove(now, majorInLink);
			} else {
				highwayMove(now, majorInLink);	
				highwayMove(now, minorInLink);
			}
		} else if(highwayInLinkMoves){
			highwayMove(now, majorInLink);
		} else if(rampInLinkMoves){
			highwayMove(now, minorInLink);		
		}

		return true;
	}

	
	
	private void highwayMove(double now, int inDirection){
		QLinkI inLink = carInLinks[inDirection];
		if(inLink != null){
			for(QLaneI laneI : inLink.getOfferingQLanes()){
				QueueWithBufferForRoW lane = (QueueWithBufferForRoW) laneI;
				while(! lane.isNotOfferingVehicle()){
					QVehicle veh = lane.getFirstVehicle();
					if (! this.qNode.moveVehicleOverNode(veh, inLink, lane, now, false)) {
						break;
					}
					lane.removeFirstGeneralVehicle();
				}
			}
		}
	}

}
