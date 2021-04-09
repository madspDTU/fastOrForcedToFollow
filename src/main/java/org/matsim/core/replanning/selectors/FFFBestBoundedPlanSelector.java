package org.matsim.core.replanning.selectors;

public class FFFBestBoundedPlanSelector extends FFFPlanSelector {

	/**
	 * Plan selector that always chooses the best available plan, and ensures that plans
	 * exceeding some threshold are automatically remoed in each iteration.
	 * Plans that remains unused for several (maximumMemory) consecutive iterations are also removed.
	 * 
	 * @param beta Irrelevant for this FFFPlanSelector
	 * @param threshold In each iteration, plans that are worse than bestScore * threshold are removed 
	 * @param maximumMemory If a plan remains unused for maximumMemory iterations, it is removed.
	 */
	
	public FFFBestBoundedPlanSelector(double beta, double threshold, int maximumMemory) {
		super(beta, threshold, maximumMemory);
	}
	
	@Override
	double getIterationSpecificBeta(int currentIteration) { // Causes best plan to be chosen
			return Double.POSITIVE_INFINITY;
	}

}
