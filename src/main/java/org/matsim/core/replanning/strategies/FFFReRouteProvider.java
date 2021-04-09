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

import javax.inject.Inject;
import javax.inject.Provider;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.replanning.FFFPlanStrategyImpl;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.router.TripRouter;
import org.matsim.facilities.ActivityFacilities;

public class FFFReRouteProvider implements Provider<PlanStrategy> {

	@Inject private GlobalConfigGroup globalConfigGroup;
	@Inject private ActivityFacilities facilities;
	@Inject private Provider<TripRouter> tripRouterProvider;

	public static final String NAME = "FFFReRoute";
	
	@Override
	public PlanStrategy get() {
		org.matsim.core.replanning.FFFPlanStrategyImpl.Builder builder = new FFFPlanStrategyImpl.Builder(new RandomPlanSelector<Plan,Person>()) ;
		builder.addStrategyModule(new org.matsim.core.replanning.modules.ReRoute(facilities, tripRouterProvider, globalConfigGroup));
		return builder.build() ;
	}

	public static String getName() {
		return NAME;
	}

}

