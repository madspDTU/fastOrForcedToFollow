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
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;

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
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.qnetsimengine.MadsQNetworkFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.MadsQNetworkFactoryWithQFFFNodes;
import org.matsim.core.mobsim.qsim.qnetsimengine.MadsQNetworkFactoryWithoutCongestion;
import org.matsim.core.mobsim.qsim.qnetsimengine.MadsQVehicleFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetworkFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicleFactory;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultSelector;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultStrategy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleTypeImpl;

/**
 * @author nagel
 *
 */
public class RunMatsim {
	private static final Logger log = Logger.getLogger( RunMatsim.class ) ;

	public static final String DESIRED_SPEED = "v_0";
	public static final String HEADWAY_DISTANCE_INTERCEPT = "theta_0";
	public static final String HEADWAY_DISTANCE_SLOPE= "theta_1";
	public static final String BICYCLE_LENGTH = "lambda_c";
	public static final long RANDOM_SEED = 5355633;

	public static void main(String[] args) {

		String scenarioExample = "equil";
		int lanesPerLink = 2;
		double marketShareOfBicycles = 1.;
		boolean useRandomActivityLocations = false;

		Config config = createConfigFromExampleName(scenarioExample);		

		Scenario scenario = createScenario(config, lanesPerLink, useRandomActivityLocations, marketShareOfBicycles);
		Controler controler = createControler(scenario);

		controler.run();

	}




	/**
	 * @param exampleName The example name that a config will be created from
	 * 
	 * @return A config based on a given example name.
	 */
	public static Config createConfigFromExampleName(String exampleName){
		URL url = ExamplesUtils.getTestScenarioURL( exampleName);;
		URL configUrl = IOUtils.newUrl( url, "config.xml" ) ;
		Config config = ConfigUtils.loadConfig( configUrl ) ;

		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists );
		config.controler().setLastIteration( 25 );
		config.controler().setWriteEventsInterval(25);
		config.controler().setWritePlansInterval(25);

		config.qsim().setEndTime( 100.*3600. );

		PlanCalcScoreConfigGroup.ModeParams params = new PlanCalcScoreConfigGroup.ModeParams(TransportMode.bike) ;
		config.planCalcScore().addModeParams( params );
		final List<String> networkModes = Arrays.asList( new String[]{TransportMode.car, TransportMode.bike} );
		config.qsim().setMainModes( networkModes );
		config.plansCalcRoute().removeModeRoutingParams( TransportMode.bike );
		config.plansCalcRoute().setNetworkModes( networkModes );
		config.plansCalcRoute().setInsertingAccessEgressWalk(true);
		config.planCalcScore().getOrCreateModeParams(TransportMode.access_walk);
		config.planCalcScore().getOrCreateModeParams(TransportMode.egress_walk);


		config.strategy().clearStrategySettings();
		StrategySettings reRoute = new StrategySettings();
		reRoute.setStrategyName(DefaultStrategy.ReRoute);
		reRoute.setWeight(0.2);
		StrategySettings bestScore = new StrategySettings();
		bestScore.setStrategyName(DefaultSelector.BestScore);
		bestScore.setWeight(0.8);
		config.strategy().addStrategySettings(reRoute);
		config.strategy().addStrategySettings(bestScore);

		config.travelTimeCalculator().setSeparateModes(true); //To get separate travel times for different modes on the same link..

		config.qsim().setVehiclesSource( QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData );

		config.global().setRandomSeed(RANDOM_SEED);
		
		//Possible changes to config
		FFFConfigGroup fffConfig = ConfigUtils.addOrGetModule(config, FFFConfigGroup.class);
		// fffConfig.setLMax(Double.MAX_VALUE); // To disable sublinks (faster computation, but lower realism)


		return config;		
	}

	public static Scenario addCyclistAttributes(Config config, Scenario scenario){
	final long RANDOM_SEED = config.global().getRandomSeed();
		final FFFConfigGroup fffConfig = ConfigUtils.addOrGetModule(config, FFFConfigGroup.class);
		return addCyclistAttributes(scenario, fffConfig, RANDOM_SEED);
	}

	private static Scenario addCyclistAttributes(Scenario scenario, FFFConfigGroup fffConfig, long seed){


		final Random speedRandom = new Random(seed);
		final Random headwayRandom = new Random(seed + 341);
		for(int i = 0; i <200; i++){
			speedRandom.nextDouble();
			headwayRandom.nextDouble();
		}

		final Population population= scenario.getPopulation() ;

		for ( Person person : population.getPersons().values() ) {
			double v_0 = uniformToJohnson(speedRandom.nextDouble(), fffConfig);
			v_0 = Math.max(v_0, fffConfig.getMinimumAllowedDesiredSpeed());
			double z_c = headwayRandom.nextDouble(); 
			double theta_0 = fffConfig.getTheta_0() + z_c * fffConfig.getZeta_0();
			double theta_1 = fffConfig.getTheta_1() + z_c * fffConfig.getZeta_1();

			person.getAttributes().putAttribute(DESIRED_SPEED, v_0);
			person.getAttributes().putAttribute(HEADWAY_DISTANCE_INTERCEPT, theta_0);
			person.getAttributes().putAttribute(HEADWAY_DISTANCE_SLOPE, theta_1);
			person.getAttributes().putAttribute(BICYCLE_LENGTH, fffConfig.getLambda_c());

		}

		VehicleType type = new VehicleTypeImpl( Id.create( TransportMode.bike, VehicleType.class  ) ) ;

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

		final FFFConfigGroup fffConfig = ConfigUtils.addOrGetModule(config, FFFConfigGroup.class);
		final Population population= scenario.getPopulation() ;

		int linkInt = 0;
		for ( Person person : population.getPersons().values() ) {
			if (modeRandom.nextDouble() < marketShare){
				{
					double v_0 = uniformToJohnson(speedRandom.nextDouble(), fffConfig);
					v_0 = Math.max(v_0, fffConfig.getMinimumAllowedDesiredSpeed());
					double z_c = headwayRandom.nextDouble(); 
					double theta_0 = fffConfig.getTheta_0() + z_c * fffConfig.getZeta_0();
					double theta_1 = fffConfig.getTheta_1() + z_c * fffConfig.getZeta_1();
					person.getAttributes().putAttribute(DESIRED_SPEED, v_0);
					person.getAttributes().putAttribute(HEADWAY_DISTANCE_INTERCEPT, theta_0);
					person.getAttributes().putAttribute(HEADWAY_DISTANCE_SLOPE, theta_1);
					person.getAttributes().putAttribute(BICYCLE_LENGTH, fffConfig.getLambda_c());

				}

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
			VehicleType type = new VehicleTypeImpl( Id.create( TransportMode.car, VehicleType.class  ) ) ;
			scenario.getVehicles().addVehicleType( type );
		}
		{
			VehicleType type = new VehicleTypeImpl( Id.create( TransportMode.bike, VehicleType.class  ) ) ;
			scenario.getVehicles().addVehicleType( type );
		}


		return scenario;

	}


	public static Controler createControler(Scenario scenario){
		Controler controler = new Controler( scenario ) ;


		controler.addOverridingQSimModule(new AbstractQSimModule() {
			@Override
			protected void configureQSim() {
				this.bind( QNetworkFactory.class ).to( MadsQNetworkFactory.class );
				this.bind( QVehicleFactory.class ).to( MadsQVehicleFactory.class ) ;
			}

		});

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

	static void cleanBicycleNetwork(Network network){
		//removeRedundantNodes(network);
		removeDuplicateLinks(network);
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
				if(inLink.getFromNode() != outLink.getToNode() &&
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
			outNode.addInLink(inLink);
			outNode.removeInLink(outLink.getId());
			network.removeLink(outLink.getId());

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
				if(carInLink != null && carOutLink != null && bicycleInLink != null && bicycleOutLink != null){
					//Check of they have the same attributes per mode!!!;

					if(carInLink.getCapacity() == carOutLink.getCapacity() &&
							carInLink.getFreespeed() == carOutLink.getFreespeed() &&
							carInLink.getNumberOfLanes() == carOutLink.getNumberOfLanes() &&
							bicycleInLink.getCapacity() == bicycleOutLink.getCapacity() &&
							bicycleInLink.getFreespeed() == bicycleOutLink.getFreespeed() &&
							bicycleInLink.getNumberOfLanes() == bicycleOutLink.getNumberOfLanes()){
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

			Node outNode = carOutLink.getToNode();
			double carLength = carInLink.getLength() + carOutLink.getLength();
			double bicycleLength = bicycleInLink.getLength() + bicycleOutLink.getLength();

			network.removeLink(carOutLink.getId());
			network.removeLink(bicycleOutLink.getId());

			carInLink.setToNode(outNode);
			carInLink.setLength(carLength);
			bicycleInLink.setToNode(outNode);
			bicycleInLink.setLength(bicycleLength);

			node.removeInLink(carInLink.getId());
			node.removeInLink(bicycleInLink.getId());
			outNode.addInLink(carInLink);
			outNode.addInLink(bicycleInLink);
			network.removeNode(node.getId());
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



