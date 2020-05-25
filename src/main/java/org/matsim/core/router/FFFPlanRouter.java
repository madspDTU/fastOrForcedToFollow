package org.matsim.core.router;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.FFFPlan;
import org.matsim.facilities.ActivityFacilities;

public class FFFPlanRouter extends PlanRouter {

	public FFFPlanRouter(TripRouter tripRouter, ActivityFacilities facilities) {
		super(tripRouter, facilities);
	}
	
	@Override
	public void run(Plan plan) {
		FFFPlan fffPlan = new FFFPlan(plan);
		plan.getPerson().setSelectedPlan(fffPlan);
		super.run(fffPlan);
	}

}
