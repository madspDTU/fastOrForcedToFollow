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

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import com.google.inject.Inject;

import fastOrForcedToFollow.Cyclist;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.ControlerConfigGroup.RoutingAlgorithmType;
import org.matsim.core.config.groups.QSimConfigGroup.VehicleBehavior;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.framework.HasPerson;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.qnetsimengine.DefaultMadsQNetworkFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.MadsQNetworkFactoryWithQFFFNodes;
import org.matsim.core.mobsim.qsim.qnetsimengine.MadsQNetworkFactoryWithoutCongestion;
import org.matsim.core.mobsim.qsim.qnetsimengine.MadsQVehicleFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.NetsimEngine;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetworkFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicleFactory;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultSelector;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultStrategy;
import org.matsim.core.router.DesiredSpeedBicycleDijkstra;
import org.matsim.core.router.DesiredSpeedBicycleDijkstraFactory;
import org.matsim.core.router.DesiredSpeedBicycleFastAStarLandmarks;
import org.matsim.core.router.DesiredSpeedBicycleFastAStarLandmarksFactory;
import org.matsim.core.router.DesiredSpeedBicycleFastDijkstraFactory;
import org.matsim.core.router.NetworkRoutingProviderWithCleaning;
import org.matsim.core.router.SingleModeNetworksCache;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

import fastOrForcedToFollow.configgroups.FFFConfigGroup;
import fastOrForcedToFollow.configgroups.FFFNodeConfigGroup;
import fastOrForcedToFollow.configgroups.FFFScoringConfigGroup;
import fastOrForcedToFollow.scoring.FFFModeUtilityParameters;
import fastOrForcedToFollow.scoring.FFFScoringFactory;

/**
 * @author nagel
 *
 */
public class RunMatsim {
	private static final Logger log = Logger.getLogger( RunMatsim.class ) ;

	public static final long RANDOM_SEED = 5355633;

	public static void main(String[] args) {

		String scenarioExample = "equil";
		int lanesPerLink = 2;
		double marketShareOfBicycles = 1.;
		boolean useRandomActivityLocations = false;

		Config config = createConfigFromExampleName(Arrays.asList(TransportMode.bike));		

		Scenario scenario = createScenario(config, lanesPerLink, useRandomActivityLocations, marketShareOfBicycles);
		Controler controler = createControler(scenario);

		controler.run();

	}




	/**
	 * @param exampleName The example name that a config will be created from
	 * @param networkModes 
	 * 
	 * @return A config based on a given example name.
	 */
	public static Config createConfigFromExampleName(Collection<String> networkModes){
		Config config = ConfigUtils.createConfig( ) ;

		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists );
		config.controler().setLastIteration( 25 );
		config.controler().setWriteEventsInterval(26);
		config.controler().setWritePlansInterval(26);
		config.counts().setWriteCountsInterval(-1);
		config.linkStats().setWriteLinkStatsInterval(-1);

		config.global().setRandomSeed(RANDOM_SEED);
		config.controler().setRoutingAlgorithmType(RoutingAlgorithmType.FastAStarLandmarks);

		config.qsim().setEndTime( 30.*3600. );
		config.qsim().setVehiclesSource( QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData );

		config.strategy().clearStrategySettings();
		StrategySettings reRoute = new StrategySettings();
		reRoute.setStrategyName(DefaultStrategy.ReRoute);
		reRoute.setWeight(0.2);
		//StrategySettings bestScore = new StrategySettings();
		//bestScore.setStrategyName(DefaultSelector.BestScore);
		//bestScore.setWeight(0.001);
		StrategySettings logit = new StrategySettings();
		logit.setStrategyName(DefaultSelector.SelectExpBeta);
		logit.setWeight(0.8);

		config.strategy().addStrategySettings(reRoute);
		//config.strategy().addStrategySettings(bestScore);
		config.strategy().addStrategySettings(logit);
		config.strategy().setFractionOfIterationsToDisableInnovation(0.8);


		config.qsim().setVehiclesSource( QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData);
		config.qsim().setVehicleBehavior(VehicleBehavior.teleport);

		for(String mode : networkModes){
			config.plansCalcRoute().removeModeRoutingParams(mode);
			config.planCalcScore().addModeParams(new ModeParams(mode)); //Not used, but checked for...
		}
		config.qsim().setMainModes(networkModes);
		config.plansCalcRoute().setNetworkModes(networkModes);
		config.plansCalcRoute().setInsertingAccessEgressWalk(true);
		config.travelTimeCalculator().setAnalyzedModes(new HashSet<String>(networkModes)); //To avoid warning


		//Possible changes to config
		FFFConfigGroup fffConfig = ConfigUtils.addOrGetModule(config, FFFConfigGroup.class);
		// fffConfig.setLMax(Double.MAX_VALUE); // To disable sublinks (faster computation, but lower realism)

		FFFScoringConfigGroup fffScoringConfig = ConfigUtils.addOrGetModule(config, FFFScoringConfigGroup.class);
		HashMap<String, FFFModeUtilityParameters> modeParams = new HashMap<String, FFFModeUtilityParameters>();
		double ttUtility = -1./60.;
		double congTTUtility = -1./120.;
		modeParams.put(TransportMode.non_network_walk, new FFFModeUtilityParameters(ttUtility, 0., 0., 0.));
		modeParams.put(TransportMode.car, new FFFModeUtilityParameters(ttUtility, congTTUtility, 0., 0.));
		modeParams.put(TransportMode.bike, new FFFModeUtilityParameters(ttUtility, congTTUtility, 0., 0.));
		fffScoringConfig.setScoringParameters(modeParams);


		return config;		
	}

	public static void addRoWModuleToConfig(Config config, boolean uneven){
		FFFNodeConfigGroup fffNodeConfig = ConfigUtils.addOrGetModule(config, FFFNodeConfigGroup.class);
		fffNodeConfig.setBicycleDelay(1.);
		fffNodeConfig.setCarDelay(1.);
		config.qsim().setStuckTime(30); // This has to be calibrated
		if(uneven){
			config.qsim().setFlowCapFactor(RunBicycleCopenhagen.capFactor); // This has to be calibrated
			config.qsim().setStorageCapFactor(RunBicycleCopenhagen.capFactor); // This has to be calibrated
			fffNodeConfig.setCarDelay(10.);
			config.qsim().setStuckTime(60); // This has to be calibrated
		} 
	}

	public static Scenario addCyclistAttributes(Config config, Scenario scenario){
		final long RANDOM_SEED = config.global().getRandomSeed();
		final FFFConfigGroup fffConfig = ConfigUtils.addOrGetModule(config, FFFConfigGroup.class);
		return addCyclistAttributes(scenario, fffConfig, RANDOM_SEED);
	}

	private static Scenario addCyclistAttributes(Scenario scenario, FFFConfigGroup fffConfig, long seed){


		final Random speedRandom = new Random(seed);
		final Random headwayRandom = new Random(seed + 341);
		final Random headwayRandom2 = new Random(seed + 732341);
		
		for(int i = 0; i <200; i++){
			speedRandom.nextDouble();
			headwayRandom.nextDouble();
			headwayRandom2.nextDouble();
		}

		final Population population= scenario.getPopulation() ;

		final List<String> attributeTypes = Arrays.asList(FFFConfigGroup.DESIRED_SPEED,
				FFFConfigGroup.HEADWAY_DISTANCE_INTERCEPT, FFFConfigGroup.HEADWAY_DISTANCE_SLOPE,
				FFFConfigGroup.BICYCLE_LENGTH);
		for ( Person person : population.getPersons().values() ) {
			if(!person.getAttributes().getAsMap().keySet().containsAll(attributeTypes)){
				double v_0 = 0;
				while(v_0 < fffConfig.getMinimumAllowedDesiredSpeed() ||
						v_0 > fffConfig.getMaximumAllowedDesiredSpeed() ){
					v_0 = uniformToJohnson(speedRandom.nextDouble(), fffConfig);
				}
				double z_beta;
				while(true){
					z_beta = headwayRandom.nextDouble();
					double p = Math.pow(4*z_beta*(1-z_beta), fffConfig.getBeta_alpha()- 1);
					double u = headwayRandom2.nextDouble();
					if(u < p){
						break;
					}
				}
				double theta_0 = fffConfig.getTheta_0() + (2*z_beta-1) * fffConfig.getZeta_0();
				double theta_1 = fffConfig.getTheta_1() + (2*z_beta-1) * fffConfig.getZeta_1();
				
				person.getAttributes().putAttribute(FFFConfigGroup.DESIRED_SPEED, v_0);
				person.getAttributes().putAttribute(FFFConfigGroup.HEADWAY_DISTANCE_INTERCEPT, theta_0);
				person.getAttributes().putAttribute(FFFConfigGroup.HEADWAY_DISTANCE_SLOPE, theta_1);
				person.getAttributes().putAttribute(FFFConfigGroup.BICYCLE_LENGTH, fffConfig.getLambda_c());
			}
		}

		VehicleType type = scenario.getVehicles().getFactory().createVehicleType(Id.create( TransportMode.bike, VehicleType.class  ) ) ;
		scenario.getVehicles().addVehicleType( type );
		return scenario;

	}

	public static Scenario createScenario(Config config, int lanesPerLink, boolean useRandomActivityLocations, double marketShare){

		final Scenario scenario = ScenarioUtils.loadScenario( config ) ;
		final long RANDOM_SEED = config.global().getRandomSeed();

		final int L = scenario.getNetwork().getLinks().size();
		final BigInteger aux = BigInteger.valueOf((long) Math.ceil(L / 4.));
		final int linkStepSize =  Integer.parseInt(String.valueOf(aux.nextProbablePrime()));

		final Random speedRandom = new Random(RANDOM_SEED);
		final Random headwayRandom = new Random(RANDOM_SEED + 341);
		final Random modeRandom = new Random(RANDOM_SEED + 513);

		for(int i = 0; i <200; i++){
			speedRandom.nextDouble();
			headwayRandom.nextDouble();
			modeRandom.nextDouble();
		}

		final Population population= scenario.getPopulation() ;

		int linkInt = 0;
		for ( Person person : population.getPersons().values() ) {
			if (modeRandom.nextDouble() < marketShare){
				for ( PlanElement pe : person.getSelectedPlan().getPlanElements() ) {
					if ( pe instanceof Leg ) {
						( (Leg) pe ).setMode( TransportMode.bike );
						( (Leg) pe ).setRoute( null  );  // invalidate route since it will be on a different network
					}
				}

				// Create activities on the bicycle links, ensuring that all links except the first and last
				// are different (except if only having two activities).
				int N = (person.getSelectedPlan().getPlanElements().size() +1) / 2;
				int n =1;
				int firstLinkInt = linkInt;

				for ( PlanElement pe : person.getSelectedPlan().getPlanElements()){
					if( pe instanceof Activity){
						Activity act =  (Activity) pe;
						if(!useRandomActivityLocations){
							//	act.setLinkId(Id.createLinkId(act.getLinkId().toString() + "_" + TransportMode.bike));
							//	act.setCoord(scenario.getNetwork().getLinks().get(act.getLinkId()).getCoord());
							//	act.setLinkId(null);
						} else {
							if(n < N  || N == 2){
								act.setLinkId(Id.createLinkId(((linkInt % L) +1) + "_" + TransportMode.bike));
							} else{
								act.setLinkId(Id.createLinkId((firstLinkInt % L) +1 + "_" + TransportMode.bike));
							}
							linkInt+=linkStepSize;
							n++;
						}
					}
				}
			}
		}
		
		// Add bicycle attributes if not already present.
		addCyclistAttributes(config, scenario);


		// adjust network to bicycle stuff:
		Network network = scenario.getNetwork() ;
		final NetworkFactory nf = network.getFactory();
		LinkedList<Link> bikeLinks = new LinkedList<Link>();
		for(Link link : network.getLinks().values()){
			{
				Set<String> set = new HashSet<>();
				for(String allowedMode : link.getAllowedModes()){
					if(!allowedMode.equals(TransportMode.bike)){
						set.add(allowedMode);
					}
				}
				link.setAllowedModes( set );
			}



			final Id<Link> id = Id.createLinkId(  link.getId().toString() + "_bike" ) ;
			final Node fromNode = link.getFromNode() ;
			final Node toNode = link.getToNode() ;
			final Link bikeLink = nf.createLink( id, fromNode, toNode );

			bikeLink.setLength( link.getLength() );
			bikeLink.setFreespeed( Double.MAX_VALUE);  //This is controlled by the desired speed of the individual.
			bikeLink.setCapacity( Double.MAX_VALUE); //The FFF-framework does not use such value.
			bikeLink.setNumberOfLanes( lanesPerLink ); //Ideally, this should be done by creating a
			// custom attribute to the link: width.
			{
				Set<String> set = new HashSet<>();
				set.add( TransportMode.bike );
				bikeLink.setAllowedModes( set );
			}

			bikeLinks.add(bikeLink);
		}
		for(Link link : bikeLinks){
			network.addLink(link);
		}

		{
			VehicleType type = scenario.getVehicles().getFactory().createVehicleType(Id.create( TransportMode.car, VehicleType.class  ) );
			scenario.getVehicles().addVehicleType( type );
		}
		{
			VehicleType type = scenario.getVehicles().getFactory().createVehicleType( Id.create( TransportMode.bike, VehicleType.class ) );
			scenario.getVehicles().addVehicleType( type );
		}


		return scenario;

	}


	public static Controler createControler(Scenario scenario){
		Controler controler = new Controler( scenario ) ;


		controler.addOverridingQSimModule(new AbstractQSimModule() {
			@Override
			protected void configureQSim() {
				this.bind( QNetworkFactory.class ).to( DefaultMadsQNetworkFactory.class );
				this.bind( QVehicleFactory.class ).to( MadsQVehicleFactory.class ) ;
			}

		});

		controler = addRoutingToControler(controler, scenario);

		return controler;
	}

	public static Controler addRoutingToControler(Controler controler, final Scenario scenario){
		Collection<String> networkModes = scenario.getConfig().plansCalcRoute().getNetworkModes();
		controler.addOverridingModule( new AbstractModule(){
			@Override public void install() {
				this.bindScoringFunctionFactory().to( FFFScoringFactory.class ) ;
				switch(scenario.getConfig().controler().getRoutingAlgorithmType()) {
				case FastAStarLandmarks: 
					this.bind(LeastCostPathCalculatorFactory.class).to(
							DesiredSpeedBicycleFastAStarLandmarksFactory.class );
					break;
				case FastDijkstra: 
					this.bind(LeastCostPathCalculatorFactory.class).to(
							DesiredSpeedBicycleFastDijkstraFactory.class);
					break;
				default:
					this.bind(LeastCostPathCalculatorFactory.class).to(
							DesiredSpeedBicycleDijkstraFactory.class);
					break;
				}
				
				for(String mode : networkModes){
					this.addRoutingModuleBinding(mode).toProvider(new NetworkRoutingProviderWithCleaning(mode));
				}
			}
		} );
		return controler;
	}

	public static Controler createControlerWithoutCongestion(Scenario scenario){
		Controler controler = new Controler( scenario ) ;


		controler.addOverridingQSimModule(new AbstractQSimModule() {
			@Override
			protected void configureQSim() {
				this.bind( QNetworkFactory.class ).to( MadsQNetworkFactoryWithoutCongestion.class );
				this.bind( QVehicleFactory.class ).to( MadsQVehicleFactory.class ) ;
			}

		});


		controler = addRoutingToControler(controler, scenario);

		return controler;
	}

	public static Controler createControlerWithRoW(Scenario scenario){
		Controler controler = new Controler( scenario ) ;


		controler.addOverridingQSimModule(new AbstractQSimModule() {
			@Override
			protected void configureQSim() {
				this.bind( QNetworkFactory.class ).to( MadsQNetworkFactoryWithQFFFNodes.class );
				this.bind( QVehicleFactory.class ).to( MadsQVehicleFactory.class ) ;
			}

		});
	

		controler = addRoutingToControler(controler, scenario);

		return controler;
	}


	/**
	 * Method for computing the inverse cumulative distribution function of the standard normal distribution.
	 * Taken directly from https://stackedboxes.org/2017/05/01/acklams-normal-quantile-function/
	 * 
	 * @param u The quantile whose corresponding value, z, is to be found.
	 * 
	 * @return The value that would yield u when inserted into the
	 * cumulative distribution function of the standard normal distribution 
	 */
	private static double qNorm(final double u){
		double a1 = -3.969683028665376e+01;
		double a2 =  2.209460984245205e+02;
		double a3 = -2.759285104469687e+02;
		double a4 =  1.383577518672690e+02;
		double a5 = -3.066479806614716e+01;
		double a6 =  2.506628277459239e+00;

		double b1 = -5.447609879822406e+01;
		double b2 =  1.615858368580409e+02;
		double b3 = -1.556989798598866e+02;
		double b4 =  6.680131188771972e+01;
		double b5 = -1.328068155288572e+01;

		double c1 = -7.784894002430293e-03;
		double c2 = -3.223964580411365e-01;
		double c3 = -2.400758277161838e+00;
		double c4 = -2.549732539343734e+00;
		double c5 =  4.374664141464968e+00;
		double c6 =  2.938163982698783e+00;

		double d1 =  7.784695709041462e-03;
		double d2 =  3.224671290700398e-01;
		double d3 =  2.445134137142996e+00;
		double d4 =  3.754408661907416e+00;

		double p_low  = 0.02425;
		double p_high = 1 - p_low;

		if(u < p_low){
			double q = Math.sqrt(-2*Math.log(u));
			return (((((c1*q+c2)*q+c3)*q+c4)*q+c5)*q+c6) /
					((((d1*q+d2)*q+d3)*q+d4)*q+1);
		}

		if(u > p_high){
			double q = Math.sqrt(-2*Math.log(1-u));
			return -(((((c1*q+c2)*q+c3)*q+c4)*q+c5)*q+c6) /
					((((d1*q+d2)*q+d3)*q+d4)*q+1);
		}

		double q = u - 0.5;
		double r = q * q;
		return (((((a1*r+a2)*r+a3)*r+a4)*r+a5)*r+a6)*q /
				(((((b1*r+b2)*r+b3)*r+b4)*r+b5)*r+1);


	}


	/**
	 * Converts a uniform number to a appropriately Johnson S_u distributed number.
	 * 
	 * @param u The uniform number to be transformed
	 * 
	 * @return A number from the appropriate Johnson S_u distribution.
	 */

	public static double uniformToJohnson(final double u, FFFConfigGroup fffConfig){
		return fffConfig.getJohnsonLambda() * Math.sinh( (qNorm(u) - fffConfig.getJohnsonGamma()) / fffConfig.getJohnsonDelta()) + fffConfig.getJohnsonXsi();
	}


	private static void removeDuplicateLinks(Network network){

		for(String mode : Arrays.asList(TransportMode.car, TransportMode.bike)){
			System.out.print("Starting to remove duplicate links network...");
			LinkedList<Link> linksToBeRemoved = new LinkedList<Link>();
			for(Node node : network.getNodes().values()){
				HashMap<Node, Link> outNodes = new HashMap<Node, Link>();
				for(Link link : node.getOutLinks().values()){
					if(link.getAllowedModes().contains(mode)){
						if(!outNodes.containsKey(link.getToNode())){
							outNodes.put(link.getToNode(),link);
						} else {
							if(link.getNumberOfLanes() > outNodes.get(link.getToNode()).getNumberOfLanes()){
								linksToBeRemoved.add(outNodes.get(link.getToNode()));
								outNodes.put(link.getToNode(), link);
							} else {
								linksToBeRemoved.add(link);
							}
						}
					}
				}
			}
			int counter = 0;
			for(Link link : linksToBeRemoved){
				network.removeLink(link.getId());
				counter++;
			}
			System.out.println(counter + " duplicate " + mode + " links removed from the network");
		}
	}

	static void cleanBicycleNetwork(Network network, Config config){
		//removeRedundantNodes(network); // This method does not work yet...
		removeDuplicateLinks(network);
		setFreespeed(network, config);
	}




	private static void setFreespeed(Network network, Config config) {
		FFFConfigGroup fffConfigGroup = ConfigUtils.addOrGetModule(config, FFFConfigGroup.class);
		System.out.print("Setting free speed of bicycle links to " + 
				fffConfigGroup.getMaximumAllowedDesiredSpeed() + "... ");
		for(Link link : network.getLinks().values()){
			if(link.getAllowedModes().contains(TransportMode.bike)){
				Gbl.assertIf(link.getAllowedModes().size() == 1); // Otherwise this is wrong
				link.setFreespeed(fffConfigGroup.getMaximumAllowedDesiredSpeed());
			}
		}	
		System.out.println(" Done!");
	}




	private static void removeRedundantNodes(Network network){
		System.out.print("Starting to remove redundant nodes...");

		//Unimodal nodes...
		LinkedList<Node> nodesToBeRemoved = new LinkedList<Node>();

		for(Node node : network.getNodes().values()){
			if(node.getInLinks().size() == 1 && node.getOutLinks().size() ==1){
				Link inLink = null;
				Link outLink = null;
				for(Link link : node.getInLinks().values()){
					inLink = link;
				}
				for(Link link : node.getOutLinks().values()){
					outLink = link;
				}

				int inBranches = inLink.getFromNode().getInLinks().size() +
						inLink.getFromNode().getOutLinks().size();
				int outBranches = outLink.getToNode().getInLinks().size() +
						outLink.getToNode().getOutLinks().size();

				if(inLink.getFromNode() != outLink.getToNode() &&
						inBranches <= 1 && outBranches <= 1 &&
						inLink.getCapacity() == outLink.getCapacity() &&
						inLink.getFreespeed() == outLink.getFreespeed() &&
						inLink.getNumberOfLanes() == outLink.getNumberOfLanes() &&
						inLink.getAllowedModes() == outLink.getAllowedModes()){
					nodesToBeRemoved.addLast(node);		
				}
			}
		}
		int counter = 0;
		for(Node node : nodesToBeRemoved){
			Link inLink = null;
			Link outLink = null;
			for(Link link : node.getInLinks().values()){
				inLink = link;
			}
			for(Link link : node.getOutLinks().values()){
				outLink = link;
			}

			Node outNode = outLink.getToNode();
			double length = inLink.getLength() + outLink.getLength();

			inLink.setToNode(outNode);
			inLink.setLength(length);

			node.removeInLink(inLink.getId());
			node.removeOutLink(outLink.getId());
			network.removeNode(node.getId());
			outNode.removeInLink(outLink.getId());
			network.removeLink(outLink.getId());
			outNode.addInLink(inLink);

			counter++;
		}

		System.out.println(counter + " unnecessary unimodal nodes removed from the network");

		//Bimodal nodes...
		nodesToBeRemoved = new LinkedList<Node>();


		for(Node node : network.getNodes().values()){
			if(node.getInLinks().size() == 2 && node.getOutLinks().size() == 2){
				Link carInLink = null;
				Link carOutLink = null;
				Link bicycleInLink = null;
				Link bicycleOutLink = null;
				for(Link link : node.getInLinks().values()){
					if(link.getAllowedModes().contains(TransportMode.car)){
						carInLink = link;
					} else if(link.getAllowedModes().contains(TransportMode.bike)){
						bicycleInLink = link;
					}
				}
				for(Link link : node.getOutLinks().values()){
					if(link.getAllowedModes().contains(TransportMode.car)){
						carOutLink = link;
					} else if(link.getAllowedModes().contains(TransportMode.bike)){
						bicycleOutLink = link;
					}
				}
				if(carInLink != null && carInLink.getToNode() != carInLink.getFromNode() &&
						carOutLink != null && carOutLink.getToNode() != carOutLink.getFromNode() &&
						bicycleInLink != null && bicycleInLink.getToNode() != bicycleInLink.getFromNode() &&
						bicycleOutLink != null && bicycleOutLink.getToNode() != bicycleOutLink.getFromNode() &&
						carInLink.getFromNode() == bicycleInLink.getFromNode() &&
						carOutLink.getToNode() == bicycleOutLink.getToNode()   &&
						carInLink.getFromNode() != carInLink.getToNode()){

					//Check if they have the same attributes per mode
					if( carInLink.getCapacity() == carOutLink.getCapacity() &&
							carInLink.getFreespeed() == carOutLink.getFreespeed() &&
							carInLink.getNumberOfLanes() == carOutLink.getNumberOfLanes() &&
							bicycleInLink.getCapacity() == bicycleOutLink.getCapacity() &&
							bicycleInLink.getFreespeed() == bicycleOutLink.getFreespeed() &&
							bicycleInLink.getNumberOfLanes() == bicycleOutLink.getNumberOfLanes() ){
						nodesToBeRemoved.addLast(node);
					}
				}
			}
		}
		counter = 0;
		for(Node node : nodesToBeRemoved){
			Link carInLink = null;
			Link carOutLink = null;
			Link bicycleInLink = null;
			Link bicycleOutLink = null;
			for(Link link : node.getInLinks().values()){
				if(link.getAllowedModes().contains(TransportMode.car)){
					carInLink = link;
				} else if(link.getAllowedModes().contains(TransportMode.bike)){
					bicycleInLink = link;
				}
			}
			for(Link link : node.getOutLinks().values()){
				if(link.getAllowedModes().contains(TransportMode.car)){
					carOutLink = link;
				} else if(link.getAllowedModes().contains(TransportMode.bike)){
					bicycleOutLink = link;
				}
			}

			Node toNode = carOutLink.getToNode();

			double carLength = carInLink.getLength() + carOutLink.getLength();
			double bicycleLength = bicycleInLink.getLength() + bicycleOutLink.getLength();

			toNode.removeInLink(carOutLink.getId());
			toNode.removeInLink(bicycleOutLink.getId());
			network.removeLink(carOutLink.getId());
			network.removeLink(bicycleOutLink.getId());


			node.removeInLink(carInLink.getId());
			node.removeInLink(bicycleInLink.getId());
			network.removeNode(node.getId());

			carInLink.setToNode(toNode);
			carInLink.setLength(carLength);
			bicycleInLink.setToNode(toNode);
			bicycleInLink.setLength(bicycleLength);

			toNode.addInLink(carInLink);
			toNode.addInLink(bicycleInLink);
			counter++;
		}
		System.out.println(counter + " redundant bimodal nodes removed from the network");
	}

	public static void reducePopulationToN(int n, Population population){
		LinkedList<Person> personsToRemove = new LinkedList<Person>();
		for(Person person : population.getPersons().values()){
			if(n >0){
				n--;
			} else {
				personsToRemove.add(person);
			}
		}
		for(Person person : personsToRemove){
			population.removePerson(person.getId());
		}
	}
}



