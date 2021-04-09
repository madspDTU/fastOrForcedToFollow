/* *********************************************************************** *
 * project: org.matsim.*												   *
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
package org.matsim.run;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.ControlerConfigGroup.RoutingAlgorithmType;
import org.matsim.core.config.groups.QSimConfigGroup.VehicleBehavior;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.replanning.selectors.PlanSelector;
import org.matsim.core.replanning.strategies.FFFPlanSelectorProvider;
import org.matsim.core.replanning.strategies.FFFReRouteProvider;
import org.matsim.core.scenario.ScenarioUtils;
import fastOrForcedToFollow.FFFUtils;
import fastOrForcedToFollow.configgroups.FFFConfigGroup;
import fastOrForcedToFollow.configgroups.FFFScoringConfigGroup;
import fastOrForcedToFollow.configgroups.FFFScoringConfigGroup.PlanSelectorType;
import fastOrForcedToFollow.scoring.FFFModeUtilityParameters;

/**
 * @author nagel
 *
 */
public class RunMatsimWithFFF {
	private static final Logger log = Logger.getLogger( RunMatsimWithFFF.class ) ;

	public static final long RANDOM_SEED = 5355633;

	public static void main(String[] args) {
		
		boolean modelBicycleCongestion = true;
		boolean modelRoW = false;

		// This could be any config - this has configurations suited for - but not necessarily required for - FFF and RoW.
		Config config = createConfigWithSuitableSettings(Arrays.asList(TransportMode.bike), 0.2, 5, PlanSelectorType.BestBounded);		

		// This could be any scenario
		Scenario scenario = ScenarioUtils.loadScenario( config ) ;

		// Preparing scenario for fff
		FFFUtils.prepareScenarioForFFF(scenario);

		// Creating controler
		Controler controler = new Controler( scenario ) ;
		// Preparing controler for fff

		if( !modelBicycleCongestion) {
			if(modelRoW) {
				System.err.println("Not possible to model RoW without modelling bicycle congestion at the moment");
			}
			FFFUtils.prepareControlerForFFFWithoutCongestion(controler);
		} else if( !modelRoW) {
			FFFUtils.prepareControlerForFFF(controler);
		} else {
			FFFUtils.prepareControlerForFFFWithRoW(controler);
		}

		controler.run();
	}




	public static Config createConfigWithSuitableSettings(Collection<String> networkModes, double reRoutingProportion, int choiceSetSize, 
																						PlanSelectorType planSelectorType){
		Config config = ConfigUtils.createConfig( ) ;

		config.global().setRandomSeed(RANDOM_SEED);
		config.controler().setRoutingAlgorithmType(RoutingAlgorithmType.FastAStarLandmarks);

		config.qsim().setEndTime(RunBicycleCopenhagen.qSimEndTime);
		config.qsim().setVehiclesSource( QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData );

		config.strategy().clearStrategySettings();
		StrategySettings reRoute = new StrategySettings();
		reRoute.setStrategyName(FFFReRouteProvider.NAME);
		reRoute.setWeight(reRoutingProportion);
		//StrategySettings bestScore = new StrategySettings();
		//bestScore.setStrategyName(DefaultSelector.BestScore);
		//bestScore.setWeight(0.001);
		StrategySettings logit = new StrategySettings();
		logit.setStrategyName(FFFPlanSelectorProvider.getName());
		logit.setWeight(1-reRoutingProportion);

		config.strategy().addStrategySettings(logit);
		config.strategy().addStrategySettings(reRoute);
		//config.strategy().addStrategySettings(bestScore);


		config.strategy().setFractionOfIterationsToDisableInnovation(0.8);
		config.strategy().setMaxAgentPlanMemorySize(choiceSetSize);


		config.qsim().setVehiclesSource( QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData);
		config.qsim().setVehicleBehavior(VehicleBehavior.teleport);

		for(String mode : networkModes){
			config.plansCalcRoute().removeModeRoutingParams(mode);
			config.planCalcScore().addModeParams(new ModeParams(mode)); //Not used, but checked for...
		}
		config.qsim().setMainModes(networkModes);
		config.plansCalcRoute().setNetworkModes(networkModes);
		config.plansCalcRoute().setInsertingAccessEgressWalk(true);
		//	config.plansCalcRoute().setInsertingAccessEgressWalk(false); // TODO MIGHT HAVE TO DELETE THIS.
		config.travelTimeCalculator().setAnalyzedModes(new HashSet<String>(networkModes)); //To avoid warning


		//Possible changes to config
		FFFConfigGroup fffConfig = ConfigUtils.addOrGetModule(config, FFFConfigGroup.class);
		//fffConfig.setLMax(Double.MAX_VALUE); //  To disable sublinks of long links (faster computation, but lower realism)

		FFFScoringConfigGroup fffScoringConfig = ConfigUtils.addOrGetModule(config, FFFScoringConfigGroup.class);
		fffScoringConfig.setPlanBeta(1.);
		fffScoringConfig.setPlanSelectorType(planSelectorType);

		HashMap<String, FFFModeUtilityParameters> modeParams = new HashMap<String, FFFModeUtilityParameters>();
		double ttUtility = -1./60.;
		double congTTUtility = -1./120.;
		modeParams.put(TransportMode.walk, new FFFModeUtilityParameters(ttUtility, 0., 0., 0.));
		modeParams.put(TransportMode.car, new FFFModeUtilityParameters(ttUtility, congTTUtility, 0., 0.));
		modeParams.put(TransportMode.truck, new FFFModeUtilityParameters(ttUtility, congTTUtility, 0., 0.)); 
		modeParams.put(TransportMode.bike, new FFFModeUtilityParameters(ttUtility, congTTUtility, 0., 0.));
		fffScoringConfig.setScoringParameters(modeParams);

		return config;		

	}

}



