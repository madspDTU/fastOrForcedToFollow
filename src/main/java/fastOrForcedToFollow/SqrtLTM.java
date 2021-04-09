package fastOrForcedToFollow;

import org.apache.log4j.Logger;

/**
 * 
 * @author madsp
 *
 * An implementation of LinkTransmissionModel that is based on individualised square root modelled safety distances.
 */

public final class SqrtLTM extends LinkTransmissionModel {
	
	private static final Logger log = Logger.getLogger( SqrtLTM.class ) ;


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

	/* package */ double getLaneVMax(final PseudoLane pseudoLane, final double tStart){
		double lambda_l = Double.max(pseudoLane.getLength(), 10.); //TODO Very heuristic, but prevents very small links from going crazy....
		double constants = lambda_c + lambda_l - this.theta_0;
		
		if(tStart >= pseudoLane.getTEnd() + this.theta_1*this.theta_1/4./constants){ 
			return 4*Math.pow((constants/this.theta_1),2); //Case 3 from paper
		}
		double timeDif = pseudoLane.getTEnd() - tStart;
		if(timeDif == 0){ 
			return Math.pow(constants/this.theta_1, 2); // Case 2 from paper
		}	
		return (this.theta_1 * this.theta_1  + 2 * timeDif * constants - 
				this.theta_1 * Math.sqrt( this.theta_1 * this.theta_1  + 4 * timeDif * constants))     /   
				(2 * timeDif * timeDif); // Case 1 from paper;
	}


	/* package */ double getSafetyBufferDistance(final double speed) {
		return this.theta_0  + this.theta_1 * Math.sqrt(speed);// 
	}


	/* package */ void selectPseudoLaneAndUpdateSpeed(final Sublink receivingLink, final Cyclist cyclist){
		final double tStart = cyclist.getTEarliestExit();	
		double maxSpeed = Double.NEGATIVE_INFINITY; 
		int maxLane = 0;
		for(int i = 0; i < receivingLink.getNumberOfPseudoLanes(); i++){
			double laneMaxSpeed = getLaneVMax(receivingLink.getPseudoLane(i), tStart);
			if(laneMaxSpeed > maxSpeed){
				maxLane = i;
				maxSpeed = laneMaxSpeed;
				if(maxSpeed >= cyclist.getDesiredSpeed()){
					break;
				}
			}
		}
	
		
		double vTilde = Math.min(cyclist.getDesiredSpeed(), maxSpeed);
		
		PseudoLane pseudoLane = receivingLink.getPseudoLane(maxLane);
	
		//		if(cyclist.getTEarliestExit() > pseudoLane.getTEnd() && pseudoLane.getLength() / vTilde > 60) {
//			System.out.println("ShortLinkProblem: " + vTilde + "  " + pseudoLane.getLength());
//		} else if(pseudoLane.getLength() / vTilde > 60) {
//			System.out.println(vTilde + "\tt_s: " + cyclist.getTEarliestExit() + "\tt_e: " + pseudoLane.getTEnd() + "\ttheta_0: " + this.theta_0 + "\ttheta_1: " +
//					this.theta_1 + "\tlambda_psi" + pseudoLane.getLength() + "\tv_0: " + cyclist.getDesiredSpeed());
//		}

		// Updating cyclist
		cyclist.setSpeed(vTilde);
		cyclist.setTEarliestExit( tStart + pseudoLane.getLength() / vTilde);

		// Updating pseudoLane
		double tOneBicycleLength = cyclist.getBicycleLength() / vTilde;
		pseudoLane.setTEnd(cyclist.getTEarliestExit() + tOneBicycleLength);

		// Updating FFF(Sub)Link
		receivingLink.increaseOccupiedSpace(cyclist, vTilde);

	}

}
