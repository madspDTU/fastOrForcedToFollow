package fastOrForcedToFollow;


public final class SqrtLTM extends LinkTransmissionModel {

	final double theta_0;
	final double theta_1;
	final double lambda_c;
	
	/* package */ SqrtLTM(final double theta_0, final double theta_1, final double lambda_c){
		this.theta_0 = theta_0;
		this.theta_1 = theta_1;
		this.lambda_c = lambda_c;
	}
	
	/* package */ double getBicycleLength(){
		return this.lambda_c;
	}
	
	/* package */ double getLaneVMax(final PseudoLane pseudoLane, final double time){
		double constants = this.lambda_c + pseudoLane.getLength() - this.theta_0;
		if(time >= pseudoLane.getTEnd() - this.theta_1*this.theta_1/4./constants){ 
			return 4*Math.pow((constants/this.theta_1),2); //Case 4 from paper
		}
		double speed = pseudoLane.getLength() / (pseudoLane.getTEnd() - pseudoLane.getTReady());
		if ( time <= pseudoLane.getTReady() + getSafetyBufferTime(speed) ){ 
			return speed; // Case 1 from paper
		}
		double timeDif = pseudoLane.getTEnd() - time;
		if(timeDif == 0){ 
			return Math.pow(constants/this.theta_1, 2); // Case 3 from paper
		}	
		return (this.theta_1 * this.theta_1  + 2 * timeDif * constants - 
				this.theta_1 * Math.sqrt( this.theta_1 * this.theta_1  + 4 * timeDif * constants))     /   
				(2 * timeDif * timeDif); // Case 2 from paper;
	}

	
	/* package */ double getSafetyBufferDistance(final double speed) {
		return this.theta_0  + this.theta_1 * Math.sqrt(speed);// 
	}

	
	/* package */ PseudoLane selectPseudoLane(final Link receivingLink, final double desiredSpeed, final double time){
		double maxSpeed = 0;
		int maxLane = 0;
		for(int i = 0; i < receivingLink.getNumberOfPseudoLanes(); i++){
			double laneMaxSpeed = getLaneVMax(receivingLink.getPseudoLane(i), time);
			
			if(laneMaxSpeed >= desiredSpeed ){
				return receivingLink.getPseudoLane(i);
			}
			if(laneMaxSpeed > maxSpeed){
				maxLane = i;
				maxSpeed = laneMaxSpeed;
			}
		}
		// If no sufficient link was found, choose the fastest.
		return receivingLink.getPseudoLane(maxLane);
	}
	
}
