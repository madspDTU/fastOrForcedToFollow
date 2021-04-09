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


import java.util.Collections;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.BasicPlan;
import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.FFFPlan;

/**
 * Changes to another plan if that plan is better.  Probability to change depends on score difference.
 *
 * @author kn based on mrieser
 */
public abstract class FFFPlanSelector<T extends BasicPlan, I> implements PlanSelector<T, I> {
	private static final Logger log = Logger.getLogger(FFFPlanSelector.class);

	protected final double beta;
	// protected final double inertia;
	protected final double threshold;
	protected final int maximumMemory;

	public FFFPlanSelector(double beta, double threshold, int maximumMemory 
		//	, double inertia
			) {
		this.beta = beta;
		// this.inertia = inertia;
		this.threshold = threshold;
		this.maximumMemory = maximumMemory;
	}

	@Override
	public T selectPlan(final HasPlansAndId<T, I> person) {
		Gbl.assertIf(!person.getPlans().isEmpty());

		//We always make sure, that the latest carried out plan sits at the end of the list!
		int currentIteration = ((FFFPlan) person.getPlans().get(person.getPlans().size()-1)).getLatestUsedIteration() + 1;

		if(person.getPlans().size() == 1) {
			return returnPlan(0, person, currentIteration);
		} 

		//Determining maximumScore, and remove plans that have not been carried out in a long time.
		int i = 0;
		int iterationThreshold = currentIteration - this.maximumMemory;
		double maxScore = Double.NEGATIVE_INFINITY;
		int maxIndex = -1;
		while(i < person.getPlans().size()) {
			FFFPlan plan = (FFFPlan) person.getPlans().get(i);
			if(plan.getLatestUsedIteration() <  iterationThreshold) {
				person.getPlans().remove(i);
			} else {
				if(plan.getScore()> maxScore) {
					maxScore = plan.getScore();
					maxIndex = i;
				}
				i++;
			}
		}	
		if(person.getPlans().size() == 1) {
			return returnPlan(0, person, currentIteration);
		} 
		Gbl.assertIf(person.getPlans().size() > 1);

		// Removing plans that exceeds the threshold...
		double thresholdScore = maxScore * this.threshold;
		i = 0;
		while(i < person.getPlans().size()) {
			T plan = person.getPlans().get(i);
			if(plan.getScore() < thresholdScore) {
				person.getPlans().remove(i);
				if(i<maxIndex) {
					maxIndex--;
				}
			} else {
				i++;
			}
		}
		if(person.getPlans().size() == 1) {
			return returnPlan(0, person, currentIteration);
		} 
		if(person.getPlans().isEmpty()) {
			System.err.println("No plans left after threshold. MaxScore: " + maxScore + ". ThresholdScore: " + thresholdScore);
		}
		Gbl.assertIf(person.getPlans().size() > 1);


		// Calculating auxiliary expoential utilities...
		double iterationSpecificBeta = getIterationSpecificBeta(currentIteration);
		if(iterationSpecificBeta == Double.POSITIVE_INFINITY) {
			return returnPlan(maxIndex, person, currentIteration);
		}
		double[] cumulativeUnnormalisedP = new double[person.getPlans().size()];
		double limitUnnormalisedP = Math.exp(iterationSpecificBeta * (thresholdScore - maxScore));

		i = 0;
		while(i < person.getPlans().size()) {
			T plan = person.getPlans().get(i);
			//possibly insert inertia here!!!
			double unnormalisedP = Math.exp(iterationSpecificBeta * (plan.getScore() - maxScore)) - limitUnnormalisedP;
			if(i == 0) {
				cumulativeUnnormalisedP[i] = unnormalisedP;
			} else {
				cumulativeUnnormalisedP[i] = cumulativeUnnormalisedP[i-1] + unnormalisedP;
			}
			i++;
		}

		double draw = MatsimRandom.getRandom().nextDouble() * cumulativeUnnormalisedP[person.getPlans().size()-1];
		i = 0;
		while(draw > cumulativeUnnormalisedP[i]) {
			i++;
		}

		return returnPlan(i, person, currentIteration);

	}

	abstract double getIterationSpecificBeta(int currentIteration);

	protected T returnPlan(int i, HasPlansAndId<T, I> personish, int currentIteration) {
		Person person = (Person) personish;
		Plan plan = ((Person) person).getPlans().get(i);
		((FFFPlan) plan).setLatestUsedIteration(currentIteration);

		//Making sure that the latest carried out plan is always at the end
		int n = person.getPlans().size()-1;
		if(i < n) {
			Collections.swap(person.getPlans(), i, n);
		}
		person.setSelectedPlan(plan);
		return (T) plan;
	}

}
