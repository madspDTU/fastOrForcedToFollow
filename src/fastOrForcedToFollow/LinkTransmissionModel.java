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
		double constants = Runner.lambda_c + pseudoLane.length - Runner.l;
		if(time >= pseudoLane.tEnd - Runner.t_safetySqrt*Runner.t_safetySqrt/4./constants){ 
			return 4*Math.pow((constants/Runner.t_safetySqrt),2); //Case 4 from paper
		}
		double speed = pseudoLane.length / (pseudoLane.tEnd - pseudoLane.tReady);
		if ( time <= pseudoLane.tReady + getSafetyBufferTime(speed) ){ 
			return speed; // Case 1 from paper
		}
		double timeDif = pseudoLane.tEnd - time;
		if(timeDif == 0){ 
			return Math.pow(constants/Runner.t_safetySqrt, 2); // Case 3 from paper
		}	
		return (Runner.t_safetySqrt * Runner.t_safetySqrt  - 2 * timeDif * constants - 
				Runner.t_safetySqrt * Math.sqrt( Runner.t_safetySqrt * Runner.t_safetySqrt  + 4 * timeDif * constants))     /   
				(2 * timeDif * timeDif); // Case 2 from paper;
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
		double tStartBasis = Double.max(time, pseudoLane.tReady);
		pseudoLane.tReady = tStartBasis + getSafetyBufferTime(speed);
		pseudoLane.tEnd = pseudoLane.tReady + pseudoLane.length / speed;
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
