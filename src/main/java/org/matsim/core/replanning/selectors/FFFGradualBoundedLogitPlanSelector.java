package org.matsim.core.replanning.selectors;

public class FFFGradualBoundedLogitPlanSelector extends FFFPlanSelector {

	public FFFGradualBoundedLogitPlanSelector(double beta, double inertia, double threshold, int maximumMemory) {
		super(beta, inertia, threshold, maximumMemory);
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
