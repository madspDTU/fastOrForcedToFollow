package fastOrForcedToFollow;


public class LinkTransmissionModel {

	final double theta_0;
	final double theta_1;
	
	LinkTransmissionModel(double theta_0, double theta_1){
		this.theta_0 = theta_0;
		this.theta_1 = theta_1;
	}
	
	public double getSafetyBufferTime(double speed) {
		return (getSafetyBufferDistance(speed) - Runner.lambda_c)   /   speed;
	}
	
	public PseudoLane selectUnsatisfactoryPseudoLane(Link nextLink, int maxLaneIndex){
		return nextLink.getPseudoLane(maxLaneIndex);
	}
	
	public PseudoLane[] createPseudoLanes(Link link, int Psi, double length){
		PseudoLane[] psi = new PseudoLane[Psi];
		for(int i = 0; i < Psi; i++){
			psi[i] = new PseudoLane(length, link);
		}
		return psi;
	}

	double getVMax(PseudoLane pseudoLane, double time){
		double constants = Runner.lambda_c + pseudoLane.length - this.theta_0;
		if(time >= pseudoLane.tEnd - this.theta_1*this.theta_1/4./constants){ 
			return 4*Math.pow((constants/this.theta_1),2); //Case 4 from paper
		}
		double speed = pseudoLane.length / (pseudoLane.tEnd - pseudoLane.tReady);
		if ( time <= pseudoLane.tReady + getSafetyBufferTime(speed) ){ 
			return speed; // Case 1 from paper
		}
		double timeDif = pseudoLane.tEnd - time;
		if(timeDif == 0){ 
			return Math.pow(constants/this.theta_1, 2); // Case 3 from paper
		}	
		return (this.theta_1 * this.theta_1  + 2 * timeDif * constants - 
				this.theta_1 * Math.sqrt( this.theta_1 * this.theta_1  + 4 * timeDif * constants))     /   
				(2 * timeDif * timeDif); // Case 2 from paper;
	}

	public PseudoLane selectPseudoLane(Link nextLink, double desiredSpeed, double time){
		double maxSpeed = 0;
		int maxLane = 0;
		for(int i = 0; i < nextLink.getNumberOfPseudoLanes(); i++){
			double laneMaxSpeed = getVMax(nextLink.getPseudoLane(i), time);
			
			if(laneMaxSpeed >= desiredSpeed ){
				return nextLink.getPseudoLane(i);
			}
			if(laneMaxSpeed > maxSpeed){
				maxLane = i;
				maxSpeed = laneMaxSpeed;
			}
		}
		// If no sufficient link was found
		PseudoLane selectedPseudoLane =  selectUnsatisfactoryPseudoLane(nextLink, maxLane);
		return selectedPseudoLane;
	}

	
	
	/**
	 * @param speed given in m/s that the safety distance will be based on.
	 * 
	 * @return safety distance (including the length of its own bicycle) given in metres.
	 */
	double getSafetyBufferDistance(double speed) {
		return this.theta_0  + this.theta_1 * Math.sqrt(speed);// 
	}


}
