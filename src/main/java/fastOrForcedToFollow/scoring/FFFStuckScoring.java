package fastOrForcedToFollow.scoring;

import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.SumScoringFunction.AgentStuckScoring;

public class FFFStuckScoring implements AgentStuckScoring{

	private double stuckUtility;
	private double score = 0;

	public FFFStuckScoring(FFFScoringParameters params) {
		this.stuckUtility = params.abortedPlanScore;
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
		score += (stuckUtility*1.01) * (100*3600 - time);
	}

}
