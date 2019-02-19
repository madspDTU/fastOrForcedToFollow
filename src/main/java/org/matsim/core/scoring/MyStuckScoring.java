package org.matsim.core.scoring;

import org.matsim.core.scoring.SumScoringFunction.AgentStuckScoring;

public class MyStuckScoring implements AgentStuckScoring{

	private double stuckUtility;
	private double score = 0;

	public MyStuckScoring(MyScoringParameters params) {
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
