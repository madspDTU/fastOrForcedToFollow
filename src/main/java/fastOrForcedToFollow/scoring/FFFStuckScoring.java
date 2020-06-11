package fastOrForcedToFollow.scoring;

import org.apache.log4j.Logger;
import org.matsim.core.scoring.SumScoringFunction.AgentStuckScoring;

public class FFFStuckScoring implements AgentStuckScoring{

	private double stuckUtility;
	private double score = 0;
	private double simulationStartTime;
	private double simulationDuration;
	
	private static final Logger log = Logger.getLogger(FFFStuckScoring.class);


	public FFFStuckScoring(FFFScoringParameters params, double simulationStartTime, double simulationEndTime) {
		this.stuckUtility = params.abortedPlanUtility * 1.01;
		this.simulationStartTime = simulationStartTime;
		this.simulationDuration = simulationEndTime - simulationStartTime;
	}

	@Override
	public void finish() {
		
	}

	@Override
	public double getScore() {
		return score;
	}

	@Override
	public void agentStuck(double time) {
			score += stuckUtility * simulationDuration * (2 - (time-simulationStartTime) / (simulationDuration));
	}

}
