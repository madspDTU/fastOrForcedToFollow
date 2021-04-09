package org.matsim.core.replanning.selectors;

public class FFFBoundedLogitPlanSelector extends FFFPlanSelector {

	/**
	 * Plan selector that always chooses the best available plans according to a logit model, and ensures that plans
	 * exceeding some threshold are automatically removed in each iteration.
	 * Plans that remains unused for several (maximumMemory) consecutive iterations are also removed.
	 * 
	 * @param beta Scale parameter used in the logit model.
	 * @param threshold In each iteration, plans that are worse than bestScore * threshold are removed 
	 * @param maximumMemory If a plan remains unused for maximumMemory iterations, it is removed.
	 */
	
	public FFFBoundedLogitPlanSelector(double beta,double threshold, int maximumMemory) {
		super(beta,threshold, maximumMemory);
	}
	
	@Override
	double getIterationSpecificBeta(int currentIteration) {
		return this.beta;
	}

}
