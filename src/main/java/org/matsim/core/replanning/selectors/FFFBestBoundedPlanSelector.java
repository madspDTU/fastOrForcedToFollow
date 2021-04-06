package org.matsim.core.replanning.selectors;

public class FFFBestBoundedPlanSelector extends FFFPlanSelector {

	public FFFBestBoundedPlanSelector(double beta, double inertia, double threshold, int maximumMemory) {
		super(beta, inertia, threshold, maximumMemory);
	}
	
	@Override
	double getIterationSpecificBeta(int currentIteration) {
			return Double.POSITIVE_INFINITY;
	}

}
