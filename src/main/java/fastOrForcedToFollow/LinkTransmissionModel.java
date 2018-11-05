package fastOrForcedToFollow;

import java.util.LinkedList;

public abstract class LinkTransmissionModel {


	public abstract double getSafetyBufferTime(double speed);

	public abstract PseudoLane selectUnsatisfactoryPseudoLane(Link nextLink, int maxLaneIndex);

	Cyclist createCyclist( int id, double cruiseSpeed, LinkedList<Link> route, LinkTransmissionModel ltm ) throws InstantiationException, IllegalAccessException {
		return new Cyclist(id, cruiseSpeed, route, ltm);
	}

	PseudoLane[] createPseudoLanes( Link link, int Psi, double length ){
		PseudoLane[] psi = new PseudoLane[Psi];
		for(int i = 0; i < Psi; i++){
			psi[i] = new PseudoLane(length, link);
		}
		return psi;
	}

	private double getVMax( PseudoLane pseudoLane, double time ){
		double constants = Runner.lambda_c + pseudoLane.length - Runner.theta_0;
		if(time >= pseudoLane.tEnd - Runner.theta_1*Runner.theta_1/4./constants){ 
			return 4*Math.pow((constants/Runner.theta_1),2); //Case 4 from paper
		}
		double speed = pseudoLane.length / (pseudoLane.tEnd - pseudoLane.tReady);
		if ( time <= pseudoLane.tReady + getSafetyBufferTime(speed) ){ 
			return speed; // Case 1 from paper
		}
		double timeDif = pseudoLane.tEnd - time;
		if(timeDif == 0){ 
			return Math.pow(constants/Runner.theta_1, 2); // Case 3 from paper
		}	
		return (Runner.theta_1 * Runner.theta_1  + 2 * timeDif * constants - 
				Runner.theta_1 * Math.sqrt( Runner.theta_1 * Runner.theta_1  + 4 * timeDif * constants))     /   
				(2 * timeDif * timeDif); // Case 2 from paper;
	}

	PseudoLane selectPseudoLaneAndAdaptSpeed( Link nextLink, Cyclist cyclist, double time ){
		double maxSpeed = 0;
		int maxLane = 0;
		for(int i = 0; i < nextLink.getNumberOfPseudoLanes(); i++){
			double laneMaxSpeed = getVMax(nextLink.getPseudoLane(i), time);
			
			if(laneMaxSpeed >= cyclist.getDesiredSpeed() ){
				cyclist.setSpeed(cyclist.getDesiredSpeed());
				return nextLink.getPseudoLane(i);
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

	
	
	/**
	 * @param speed given in m/s that the safety distance will be based on.
	 * 
	 * @return safety distance (including the length of its own bicycle) given in metres.
	 */
	public double getSafetyBufferDistance(double speed){
		return 0;
	}

	/**
	 * Reduces the occupied space of link <code>linkId</code> by the safety distance corresponding to <code>speed</code>
	 * 
	 * @param linkId of the link that will have its space reduced.
	 * 
	 * @param speed on which the safety distance will be based.
	 */
	void reduceOccupiedSpace( int linkId, double speed ){
		Runner.linksMap.get(linkId).supplementOccupiedSpace(-getSafetyBufferDistance(speed));
	}

	/**
	 * Increases the occupied space of link <code>linkId</code> by the safety distance corresponding to <code>speed</code>
	 * 
	 * @param linkId of the link that will have its space increased.
	 * 
	 * @param speed on which the safety distance will be based.
	 */
	void increaseOccupiedSpace( int linkId, double speed ){
		Runner.linksMap.get(linkId).supplementOccupiedSpace(getSafetyBufferDistance(speed));
	}
}