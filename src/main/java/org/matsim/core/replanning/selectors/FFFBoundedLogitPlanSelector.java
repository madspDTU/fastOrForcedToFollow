package org.matsim.core.replanning.selectors;

public class FFFBoundedLogitPlanSelector extends FFFPlanSelector {

	public FFFBoundedLogitPlanSelector(double beta, double inertia, double threshold, int maximumMemory) {
		super(beta, inertia, threshold, maximumMemory);
	}
	
	@Override
	double getIterationSpecificBeta(int currentIteration) {
		return this.beta;
	}

}
