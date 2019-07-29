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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Random;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.FacilitiesConfigGroup.FacilitiesSource;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleTypeImpl;

import fastOrForcedToFollow.configgroups.FFFConfigGroup;

/**
 * @author nagel
 *
 */
public class RunMatsimWithRoW {
	
	public static boolean omitEastLinks = false;
	public static boolean useEqualCapacities = false;

	private static double bicycleMarketShare = 0.5;
	private static int numberOfAgents = 499;

	private static boolean useToyNetwork = true;

	public static void main(String[] args) {

		Config config = RunMatsim.createConfigFromExampleName(
				Arrays.asList(TransportMode.bike, TransportMode.car));	
		
		//Possible changes to config
		//	FFFConfigGroup fffConfig = ConfigUtils.addOrGetModule(config, FFFConfigGroup.class);
		// fffConfig.setLMax(Double.MAX_VALUE); // To disable sublinks (faster computation, but lower realism)
	
		config.controler().setLastIteration(0);

		config.controler().setOutputDirectory("./output/RoW/");


		config.facilities().setFacilitiesSource(FacilitiesSource.onePerActivityLocationInPlansFile);


		Scenario scenario;
		if(useToyNetwork){

			scenario = ScenarioUtils.createScenario(config);

			createNetwork(scenario);

			createPopulation(scenario, numberOfAgents);
			scenario = RunMatsim.addCyclistAttributes(config, scenario);
			scenario.getVehicles().addVehicleType(  new VehicleTypeImpl( Id.create( TransportMode.car, VehicleType.class  ) ) );

		} else {
			scenario = RunMatsim.createScenario(config, 2, false, bicycleMarketShare);
		}
		
		Controler controler = RunMatsim.createControlerWithRoW(scenario); 
		//Controler controler = RunMatsim.createControler(scenario); 

		config.planCalcScore().addModeParams( new PlanCalcScoreConfigGroup.ModeParams(TransportMode.ride) );
		config.planCalcScore().addModeParams( new PlanCalcScoreConfigGroup.ModeParams(TransportMode.walk) );
		
		



		try{
			controler.run();
		}
		catch(NullPointerException e){
			NetworkWriter nw = new NetworkWriter(scenario.getNetwork());
			nw.write("./output/RoW/inputNetwork.xml");
			e.printStackTrace();
		}

	}


	private static void createPopulation(Scenario scenario, int numberOfAgents){
		Population population = scenario.getPopulation();
		PopulationFactory pf = population.getFactory();

		double timeA = 7*3600;
		double timeB = timeA + 5*60;
		double timeC = timeB + 15*60;
		double timeD = timeC + 5*60;

		Random random = new Random(RunMatsim.RANDOM_SEED);
		for(int i = 0; i < 200; i ++){
			random.nextDouble();
		}

		for(int i = 0; i < numberOfAgents; i++){
			double startTime1 = timeA + (timeB-timeA) * random.nextDouble();
			double startTime2 = timeC + (timeD-timeC) * random.nextDouble();
			Link startLink;
			Link endLink;
			double d = random.nextDouble();
			if(i < numberOfAgents/4){
				startLink = scenario.getNetwork().getLinks().get(Id.createLinkId("SS_S"));
				if(d < 0.3){
					endLink = scenario.getNetwork().getLinks().get(Id.createLinkId("W_WW"));
				} else if( d < 0.7){
					endLink = scenario.getNetwork().getLinks().get(Id.createLinkId("N_NN"));
				} else {
					endLink = scenario.getNetwork().getLinks().get(Id.createLinkId("E_EE"));
				}
			} else if (i < numberOfAgents/2){
				startLink = scenario.getNetwork().getLinks().get(Id.createLinkId("EE_E"));
				if(d < 0.3){
					endLink = scenario.getNetwork().getLinks().get(Id.createLinkId("W_WW"));
				} else if( d < 0.7){
					endLink = scenario.getNetwork().getLinks().get(Id.createLinkId("N_NN"));
				} else {
					endLink = scenario.getNetwork().getLinks().get(Id.createLinkId("S_SS"));
				}
			} else if (i < numberOfAgents/3*4){
				startLink = scenario.getNetwork().getLinks().get(Id.createLinkId("NN_N"));
				if(d < 0.3){
					endLink = scenario.getNetwork().getLinks().get(Id.createLinkId("W_WW"));
				} else if( d < 0.7){
					endLink = scenario.getNetwork().getLinks().get(Id.createLinkId("S_SS"));
				} else {
					endLink = scenario.getNetwork().getLinks().get(Id.createLinkId("E_EE"));
				}
			} else {
				startLink = scenario.getNetwork().getLinks().get(Id.createLinkId("WW_W"));
				if(d < 0.3){
					endLink = scenario.getNetwork().getLinks().get(Id.createLinkId("S_SS"));
				} else if( d < 0.7){
					endLink = scenario.getNetwork().getLinks().get(Id.createLinkId("N_NN"));
				} else {
					endLink = scenario.getNetwork().getLinks().get(Id.createLinkId("E_EE"));
				}
			}

			if(String.valueOf(endLink).equals("null")){
				endLink = scenario.getNetwork().getLinks().get(Id.createLinkId("W_WW"));
				startLink =  scenario.getNetwork().getLinks().get(Id.createLinkId("NN_N"));
			}
			if(String.valueOf(startLink).equals("null")){
				endLink = scenario.getNetwork().getLinks().get(Id.createLinkId("S_SS"));
				startLink =  scenario.getNetwork().getLinks().get(Id.createLinkId("WW_W"));
			}

			
			startLink = scenario.getNetwork().getLinks().get(Id.createLinkId("SS_S"));
			endLink =  scenario.getNetwork().getLinks().get(Id.createLinkId("WW_W"));
		

			String mode;
			if( random.nextDouble() < bicycleMarketShare ){
				mode = TransportMode.bike;
			} else {
				mode = TransportMode.car;
			}
			Person person = pf.createPerson(Id.createPersonId(i));
			Plan plan = pf.createPlan();
			plan.setPerson(person);
			Activity firstActivity = pf.createActivityFromLinkId("h", startLink.getId());
			firstActivity.setLinkId(startLink.getId());
			firstActivity.setEndTime(startTime1);
			firstActivity.setCoord(startLink.getToNode().getCoord());
			plan.addActivity(firstActivity);
			plan.addLeg(pf.createLeg(mode));
			Activity secondActivity = pf.createActivityFromLinkId("w", endLink.getId());
			secondActivity.setLinkId(endLink.getId());
			secondActivity.setEndTime(startTime2);
			secondActivity.setCoord(endLink.getToNode().getCoord());
			plan.addActivity(secondActivity);
			plan.addLeg(pf.createLeg(mode));
			Activity lastActivity = pf.createActivityFromLinkId("h", startLink.getId());
			lastActivity.setLinkId(startLink.getId());
			lastActivity.setEndTime(startTime2 + 60*3600);
			lastActivity.setCoord(startLink.getToNode().getCoord());
			plan.addActivity(lastActivity);
			person.addPlan(plan);
			person.setSelectedPlan(plan);
			population.addPerson(person);	
		}
	}


	private static void createNetwork(Scenario scenario){
		Network network = scenario.getNetwork();
		NetworkFactory nf = network.getFactory();
		Node nodeSS = nf.createNode(Id.createNodeId("SS"), new Coord(0, -200));
		Node nodeS = nf.createNode(Id.createNodeId("S"), new Coord(0, -100));
		Node nodeEE = nf.createNode(Id.createNodeId("EE"), new Coord(200, 0));
		Node nodeE = nf.createNode(Id.createNodeId("E"), new Coord(100, 0));
		Node nodeNN = nf.createNode(Id.createNodeId("NN"), new Coord(0, 200));
		Node nodeN = nf.createNode(Id.createNodeId("N"), new Coord(0, 100));
		Node nodeWW = nf.createNode(Id.createNodeId("WW"), new Coord(-200, 0));
		Node nodeW = nf.createNode(Id.createNodeId("W"), new Coord(-100, 0));
		Node nodeC = nf.createNode(Id.createNodeId("C"), new Coord(0, 0));
		for(Node node : Arrays.asList(nodeSS, nodeS, nodeEE, nodeE, nodeNN, nodeN, nodeWW, nodeW, nodeC)){
			network.addNode(node);
		}
		if(omitEastLinks){
			network.removeNode(nodeE.getId());
			network.removeNode(nodeEE.getId());
		}
		network.addLink(nf.createLink(Id.createLinkId("SS_S"), nodeSS, nodeS));
		network.addLink(nf.createLink(Id.createLinkId("S_SS"), nodeS, nodeSS));
		network.addLink(nf.createLink(Id.createLinkId("S_C"), nodeS, nodeC));
		network.addLink(nf.createLink(Id.createLinkId("C_S"), nodeC, nodeS));
		if(!omitEastLinks){
			network.addLink(nf.createLink(Id.createLinkId("EE_E"), nodeEE, nodeE));
			network.addLink(nf.createLink(Id.createLinkId("E_EE"), nodeE, nodeEE));
			network.addLink(nf.createLink(Id.createLinkId("E_C"), nodeE, nodeC));
			network.addLink(nf.createLink(Id.createLinkId("C_E"), nodeC, nodeE));
		}
		network.addLink(nf.createLink(Id.createLinkId("NN_N"), nodeNN, nodeN));
		network.addLink(nf.createLink(Id.createLinkId("N_NN"), nodeN, nodeNN));
		network.addLink(nf.createLink(Id.createLinkId("N_C"), nodeN, nodeC));
		network.addLink(nf.createLink(Id.createLinkId("C_N"), nodeC, nodeN));
		network.addLink(nf.createLink(Id.createLinkId("WW_W"), nodeWW, nodeW));
		network.addLink(nf.createLink(Id.createLinkId("W_WW"), nodeW, nodeWW));
		network.addLink(nf.createLink(Id.createLinkId("W_C"), nodeW, nodeC));
		network.addLink(nf.createLink(Id.createLinkId("C_W"), nodeC, nodeW));
		HashSet<String> modesSet = new HashSet<String>();
		modesSet.add(TransportMode.car);
		modesSet.add(TransportMode.bike);
		for(Link link : network.getLinks().values()){
			if(link.getId().toString().contains("N") || link.getId().toString().contains("S")){
				link.setCapacity(3600);
				link.setAllowedModes(modesSet);
			} else {
				if(!useEqualCapacities){
					link.setCapacity(3500);
				} else {
					link.setCapacity(3600);
				}
				link.setAllowedModes(modesSet);
			}
			link.setFreespeed(50/3.6);
			link.setLength(1000);
		}


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
			bikeLink.setNumberOfLanes( 2 );
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

	}


}



