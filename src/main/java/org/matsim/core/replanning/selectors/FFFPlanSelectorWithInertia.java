/* *********************************************************************** *
 * project: org.matsim.*
 * ExpBetaPlanChanger.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.core.replanning.selectors;


import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.BasicPlan;
import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.core.gbl.MatsimRandom;

/**
 * Changes to another plan if that plan is better.  Probability to change depends on score difference.
 *
 * @author kn based on mrieser
 */
public final class FFFPlanSelectorWithInertia<T extends BasicPlan, I> implements PlanSelector<T, I> {
	private static final Logger log = Logger.getLogger(FFFPlanSelector.class);

	private final double beta;
	private final double inertia;

	public FFFPlanSelectorWithInertia(double beta, double inertia) {
		this.beta = beta;
		this.inertia = inertia;
	}

	@Override
	public T selectPlan(final HasPlansAndId<T, I> person) {
		// current plan and random plan:
		T currentPlan = person.getSelectedPlan();
		if(person.getPlans().size() == 1) {
			return currentPlan;
		} 
		
		double logSum = 0;
		double[] cumLogSum = new double[person.getPlans().size()];
		int i = 0;
		for(T plan : person.getPlans()) {
			double disutility = plan.getScore();
			if(plan == currentPlan) {
				disutility += this.inertia;
			}
			logSum += Math.exp(beta*disutility);
			cumLogSum[i] = logSum;
			i++;
		}

		double draw = MatsimRandom.getRandom().nextDouble() * logSum;
		i = 0;
		for(T plan : person.getPlans()) {
			if(draw <= cumLogSum[i]) {
				return plan;
			}
			i++;
		}

		// We should be out by now, but if this doesn't happen, something is wrong! Just choose existing path
		log.error( "We did not find a plan..... Returning existing one" ) ;
		return currentPlan;
	}

}
