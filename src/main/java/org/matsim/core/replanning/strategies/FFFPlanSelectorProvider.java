/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.core.replanning.strategies;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.replanning.FFFPlanStrategyImpl;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.PlanStrategyImpl.Builder;
import org.matsim.core.replanning.selectors.ExpBetaPlanChanger;
import org.matsim.core.replanning.selectors.FFFBestBoundedPlanSelector;
import org.matsim.core.replanning.selectors.FFFBoundedLogitPlanSelector;
import org.matsim.core.replanning.selectors.FFFGradualBoundedLogitPlanSelector;
import org.matsim.core.replanning.selectors.FFFPlanSelector;

import fastOrForcedToFollow.configgroups.FFFScoringConfigGroup;

import javax.inject.Inject;
import javax.inject.Provider;

public class FFFPlanSelectorProvider implements Provider<PlanStrategy> {
	
	public static final String NAME = "FFFBoundedSelector";

    @Inject private FFFScoringConfigGroup config;

	@SuppressWarnings("unchecked")
	@Override
	public PlanStrategy get() {

 
    	FFFPlanSelector planSelector;
    	
    	switch(config.getPlanSelectorType()) {

    	case BoundedLogit:
    		planSelector =  new FFFBoundedLogitPlanSelector(config.getPlanBeta(),
            		config.getThreshold(), config.getMaximumMemory());
    		break;
    	case GradualBoundedLogit: 
    		planSelector = new FFFGradualBoundedLogitPlanSelector(config.getPlanBeta(), 
            		config.getThreshold(), config.getMaximumMemory());
    		break;
    	case BestBounded:
    		planSelector = new FFFBestBoundedPlanSelector(config.getPlanBeta(),
            		config.getThreshold(), config.getMaximumMemory());
    		break;
    	default:
    		System.err.println("No valid planSelectorType set for FFFPlanSelector");
    		return null;
    	}
    	
    	Builder builder = new PlanStrategyImpl.Builder(planSelector);
    	
    	return(builder.build());
	}
    
    public static String getName() {
    	return NAME;
    }

}
