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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
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
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.qnetsimengine.*;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleTypeImpl;

import fastOrForcedToFollow.Runner;
import fastOrForcedToFollow.ToolBox;

/**
 * @author nagel
 *
 */
public class RunMatsim {
	private static final Logger log = Logger.getLogger( RunMatsim.class ) ;

	public static final String DESIRED_SPEED = "v_0";
	public static final String HEADWAY_DISTANCE_PREFERENCE = "z_c";
	public static final String BICYCLE_LENGTH = "lambda_c";
	public static final long RANDOM_SEED = 5355633;

	public static void main(String[] args) {

		String scenarioExample = "berlin";
		int lanesPerLink = 4;
		boolean useRandomActivityLocations = false;

		Config config = createConfigFromExampleName(scenarioExample);
		Scenario scenario = createScenario(config, lanesPerLink, useRandomActivityLocations);
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
		config.controler().setLastIteration( 0 );
		config.controler().setWriteEventsInterval(1);
		config.controler().setWritePlansInterval(1);

		config.qsim().setEndTime( 100.*3600. );

		PlanCalcScoreConfigGroup.ModeParams params = new PlanCalcScoreConfigGroup.ModeParams(TransportMode.bike) ;
		config.planCalcScore().addModeParams( params );
		final List<String> networkModes = Arrays.asList( new String[]{TransportMode.car, TransportMode.bike} );
		config.qsim().setMainModes( networkModes );
		config.plansCalcRoute().removeModeRoutingParams( TransportMode.bike );
		config.plansCalcRoute().setNetworkModes( networkModes );
		config.travelTimeCalculator().setAnalyzedModes( TransportMode.car + "," + TransportMode.bike);

		config.qsim().setVehiclesSource( QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData );

		return config;		
	}

	public static Scenario createScenario(Config config, int lanesPerLink, boolean useRandomActivityLocations){
		
		double l_max = 100.;

		Scenario scenario = ScenarioUtils.loadScenario( config ) ;

		final int L = scenario.getNetwork().getLinks().size();
		BigInteger aux = BigInteger.valueOf((long) Math.ceil(L / 4.));
		int linkStepSize =  Integer.parseInt(String.valueOf(aux.nextProbablePrime()));

		Random speedRandom = new Random(RANDOM_SEED);
		Random headwayRandom = new Random(RANDOM_SEED + 341);
		for(int i = 0; i <200; i++){
			speedRandom.nextDouble();
			headwayRandom.nextDouble();
		}

		Population population= scenario.getPopulation() ;
		int linkInt = 0;
		for ( Person person : population.getPersons().values() ) {
			if ( true ) {  // Forcing all legs in scenario to be made by bicycle...
				{
					double v_0 = ToolBox.uniformToJohnson(speedRandom.nextDouble());
					v_0 = Math.max(v_0, Runner.MINIMUM_ALLOWED_DESIRED_SPEED);  //Minimum value is 2 m/s. 
					double z_c = headwayRandom.nextDouble(); 
					person.getAttributes().putAttribute(DESIRED_SPEED, v_0);
					person.getAttributes().putAttribute(HEADWAY_DISTANCE_PREFERENCE, z_c);
					person.getAttributes().putAttribute(BICYCLE_LENGTH, Runner.LAMBDA_C);

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
							act.setLinkId(Id.createLinkId(act.getLinkId().toString() + "_" + TransportMode.bike));
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

		
	//	scenario = chopNetwork(scenario, l_max);
	//	scenario = moveActivitiesToChoppedLinks(scenario);


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


	private static Scenario chopNetwork(final Scenario scenario, final double l_max){

		ArrayList<Double> linkLengths = new ArrayList<Double>();
		int totalNumberOfPieces = 0;
		LinkedList<Link> oldLinks = new LinkedList<Link>();
		for(Link link : scenario.getNetwork().getLinks().values()){
			oldLinks.add(link);
		}
		for(Link link : oldLinks){
			if(link.getAllowedModes().contains(TransportMode.bike)){
				Gbl.assertIf(link.getAllowedModes().size() == 1);  // Only do so if the link has been separated.
				if(link.getLength() > l_max){
					int noOfPieces = (int) Math.ceil(link.getLength() / l_max);
					double newLength = link.getLength() / noOfPieces;
					totalNumberOfPieces += noOfPieces;
					for(int i = 1; i <= noOfPieces; i++){
						linkLengths.add(newLength);
					}


					if(noOfPieces == 1){
						// do nothing
					} else {
						//Create nodes
						Node fromNode = link.getFromNode();
						double fromX = fromNode.getCoord().getX();
						double fromY = fromNode.getCoord().getY();
						Node toNode = link.getToNode();
						double toX = toNode.getCoord().getX();
						double toY = toNode.getCoord().getY();
						String baseName = fromNode.getId() + "_to_" + toNode.getId();
						for(int i = 1; i < noOfPieces; i++){
							Node newNode = NetworkUtils.createNode(Id.createNodeId(baseName + "_part_" + i)); 
							double rho = (double) i / noOfPieces;
							double newX = fromX + rho*(toX - fromX);
							double newY = fromY + rho*(toY - fromY);
							newNode.setCoord(new Coord(newX, newY));
							scenario.getNetwork().addNode(newNode);
						}

						//Create links using new nodes, (and original nodes for outermost links)
						for(int i = 1; i <= noOfPieces; i++){
							Node newFromNode = i == 1 ? fromNode : scenario.getNetwork().getNodes().get(Id.createNodeId(baseName + "_part_" + (i-1)));
							Node newToNode = i == noOfPieces ? toNode : scenario.getNetwork().getNodes().get(Id.createNodeId(baseName + "_part_" + i));
							Link newLink = NetworkUtils.createLink(Id.createLinkId(link.getId() + "_part_" + i),
									newFromNode, newToNode, scenario.getNetwork(), newLength,	link.getFreespeed(), link.getCapacity(), link.getNumberOfLanes());
							newLink.setAllowedModes(link.getAllowedModes());
							
							scenario.getNetwork().addLink(newLink);
						}
						
						//Remove original non-splitted link.
						scenario.getNetwork().removeLink(link.getId());
					}
				}
			}
		}

		
		/*
		linkLengths.sort( new Comparator<Double>(){
			@Override
			public int compare(Double o1,Double o2){
				return Double.compare(o1,o2);
			}
		});

		System.out.println("Minimum value is   " + linkLengths.get(0) + "m");

		System.out.println("0.1% quantile is   " + linkLengths.get(totalNumberOfPieces/1000) + "m");
		System.out.println("0.5% quantile is   " + linkLengths.get(totalNumberOfPieces/200) + "m");
		System.out.println("1th percentile is  " + linkLengths.get(totalNumberOfPieces/100) + "m");
		System.out.println("2.5% quantile is   " + linkLengths.get(totalNumberOfPieces/40) + "m");
		System.out.println("5th percentile is  " + linkLengths.get(totalNumberOfPieces/20) + "m");
		System.out.println("10th percentile is " + linkLengths.get(totalNumberOfPieces/10) + "m");
		System.out.println("25th percentile is " + linkLengths.get(totalNumberOfPieces/4) + "m");
		System.out.println("Median is          " + linkLengths.get(totalNumberOfPieces/2) + "m");


		System.out.println((double) totalNumberOfPieces / scenario.getNetwork().getLinks().values().size() + " times as many links now"); 
		*/

		return scenario;
	}

	public static Scenario moveActivitiesToChoppedLinks(final Scenario scenario){
		
		for(Person person : scenario.getPopulation().getPersons().values()){
			for(PlanElement pe : person.getSelectedPlan().getPlanElements()){
				if(pe instanceof Activity){
					Activity activity = (Activity) pe;
					if(!scenario.getNetwork().getLinks().containsKey(activity.getLinkId())){
						activity.setLinkId(Id.createLinkId(activity.getLinkId() + "_part_2"));
					}
				}
			}
		}
		return scenario;
	}
}



