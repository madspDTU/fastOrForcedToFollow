package org.matsim.core.replanning.selectors;

public class FFFGradualBoundedLogitPlanSelector extends FFFPlanSelector {

	/**
	 * Plan selector that always chooses the best available plans according to a logit model, and ensures 
	 * that plans exceeding some threshold are automatically removed in each iteration.
	 * The beta increses throughout the iterations.
	 * Plans that remains unused for several (maximumMemory) consecutive iterations are also removed.
	 * 
	 * @param beta Initial scale parameter used in the logit model.
	 * @param threshold In each iteration, plans that are worse than bestScore * threshold are removed 
	 * @param maximumMemory If a plan remains unused for maximumMemory iterations, it is removed.
	 */
	
	public FFFGradualBoundedLogitPlanSelector(double beta,  double threshold, int maximumMemory) {
		super(beta, threshold, maximumMemory);
	}
	
	@Override
	double getIterationSpecificBeta(int currentIteration) {
		if(currentIteration <= 25) {
			return this.beta;
		} else if(currentIteration <= 40) {
			return this.beta * 2;
		}else if(currentIteration <= 55) {
			return this.beta * 4;
		}else if(currentIteration <= 70) {
			return this.beta * 8;
		}else if(currentIteration <= 85) {
			return this.beta * 16;
		} else if(currentIteration <= 100) {
			return this.beta * 32;
		} else {
			return Double.POSITIVE_INFINITY;
		}
	}

}
