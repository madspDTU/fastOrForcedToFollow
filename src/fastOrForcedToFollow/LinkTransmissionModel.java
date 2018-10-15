package fastOrForcedToFollow;

import java.util.LinkedList;

public abstract class LinkTransmissionModel {


	public abstract double getSafetyBufferTime(double speed);

	public abstract PseudoLane selectUnsatisfactoryPseudoLane(Link nextLink, int maxLaneIndex);

	public Cyclist createCyclist(int id, double cruiseSpeed, LinkedList<Link> route, LinkTransmissionModel ltm) throws InstantiationException, IllegalAccessException {
		return new Cyclist(id, cruiseSpeed, route, ltm);
	}

	public PseudoLane[] createPseudoLanes(Link link, int Psi, double length){
		PseudoLane[] psi = new PseudoLane[Psi];
		for(int i = 0; i < Psi; i++){
			psi[i] = new PseudoLane(length, link);
		}
		return psi;
	}

	public double getVMax(PseudoLane pseudoLane, double time){

		double speed = pseudoLane.length / (pseudoLane.tEnd - pseudoLane.tStart);
		if ( time - getSafetyBufferTime(speed) <= pseudoLane.tStart){
			return speed;
		}
		double timeDif = pseudoLane.tEnd - time;
		double constants = Runner.l - Runner.lambda_c - pseudoLane.length;
		speed = (Math.pow(Runner.t_safetySqrt, 2) - 2 * timeDif * constants - 
				Runner.t_safetySqrt * Math.sqrt(Math.pow(Runner.t_safetySqrt,2) - 4 * timeDif * constants))     /     (2*Math.pow(timeDif,2));
		if ( time - getSafetyBufferTime(speed) < pseudoLane.tEnd){
			return speed;
		}
		return Double.MAX_VALUE;

		/*
		double tStartTemp = Double.max(pseudoLane.tStart, time);
		double timeDif = (pseudoLane.tEnd - tStartTemp);
		//  double spatiallyAllowedMaximumSpeed = (pseudoLane.link.totalLaneLength - Runner.l - pseudoLane.link.occupiedSpace) / Runner.t_safety;
		if( timeDif <= 0){
		  return Double.MAX_VALUE;
		 // return Math.min(Double.MAX_VALUE, spatiallyAllowedMaximumSpeed);
		}
		 return pseudoLane.length  / timeDif; 
		 */
	}

	public PseudoLane selectPseudoLaneAndAdaptSpeed(Link nextLink, Cyclist cyclist, double time){
		double maxSpeed = 0;
		int maxLane = 0;
		for(int i = 0; i < nextLink.Psi; i++){
			double laneMaxSpeed = getVMax(nextLink.psi[i], time);
			if(laneMaxSpeed >= cyclist.desiredSpeed ){
				cyclist.setSpeed(cyclist.desiredSpeed);
				return nextLink.psi[i];
			}
			if(laneMaxSpeed > maxSpeed){
				maxLane = i;
				maxSpeed = laneMaxSpeed;
			}
		}
		// If no sufficient link was found
		PseudoLane selectedPseudoLane =  selectUnsatisfactoryPseudoLane(nextLink, maxLane);
		cyclist.setSpeed(getVMax(selectedPseudoLane, time));
		return selectedPseudoLane;
	}

	public void updatePseudoLane(PseudoLane pseudoLane, double speed, double time) {
		double tStartBasis = Double.max(time, pseudoLane.tStart);
		pseudoLane.tStart = tStartBasis + getSafetyBufferTime(speed);
		pseudoLane.tEnd = pseudoLane.tStart + pseudoLane.length / speed;
	}

	public double getSafetyBufferDistance(double speed){
		return 0;
	}

	public void reduceOccupiedSpace(int linkId, double speed){
		Link link = Runner.linksMap.get(linkId);
		link.occupiedSpace -= getSafetyBufferDistance(speed);
		Runner.linksMap.replace(linkId, link);
	}

	public void increaseOccupiedSpace(Link link, double speed){
		link.occupiedSpace += getSafetyBufferDistance(speed);
	}
}
