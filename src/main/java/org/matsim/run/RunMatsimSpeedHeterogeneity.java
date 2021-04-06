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

import java.awt.JobAttributes.DefaultSelectionType;
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
import org.matsim.core.config.groups.ControlerConfigGroup.RoutingAlgorithmType;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.config.groups.FacilitiesConfigGroup.FacilitiesSource;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultSelector;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultStrategy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.VehicleType;

import fastOrForcedToFollow.configgroups.FFFConfigGroup;

/**
 * @author nagel
 *
 */
public class RunMatsimSpeedHeterogeneity {

	final static double freeSpeedSlow = 7;
	final static double freeSpeedFast = 100;
	final static int numberOfAgents = 10000;
	
	final static int lastIteration = 1;

	public static void main(String[] args) {
		Config config = RunMatsim.createConfigFromExampleName(Arrays.asList(TransportMode.bike), 0.2, 5);	
		config.controler().setLastIteration(lastIteration);
		config.controler().setOutputDirectory("./output/SpeedHeterogeneity/");
	
		Scenario scenario = ScenarioUtils.createScenario(config);

		createNetwork(scenario);

		createPopulation(scenario);
		scenario = RunMatsim.addCyclistAttributes(config, scenario);			
	
		
		Controler controler = RunMatsim.createControlerWithRoW(scenario); 


	
		try{
			controler.run();
		}
		catch(NullPointerException e){
			NetworkWriter nw = new NetworkWriter(scenario.getNetwork());
			nw.write("./output/RoW/inputNetwork.xml");
			e.printStackTrace();
		}

	}


	private static void createPopulation(Scenario scenario){
		Population population = scenario.getPopulation();
		PopulationFactory pf = population.getFactory();

		double timeA = 7*3600;
		double timeC = 7.25*3600;

		
		for(int i = 0; i < numberOfAgents; i++){
			double startTime1 = timeA;
			double startTime2 = timeC;
			Link startLink;
			Link endLink;
			startLink = scenario.getNetwork().getLinks().get(Id.createLinkId("SS_S"));
			endLink = scenario.getNetwork().getLinks().get(Id.createLinkId("N_NN"));
			String mode = TransportMode.bike;
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
		Node nodeSS = nf.createNode(Id.createNodeId("SS"), new Coord(0, -1200));
		Node nodeS = nf.createNode(Id.createNodeId("S"), new Coord(0, -1000));
		Node nodeW = nf.createNode(Id.createNodeId("W"), new Coord(0,0));
		Node nodeE = nf.createNode(Id.createNodeId("E"), new Coord(200,0));
		Node nodeN = nf.createNode(Id.createNodeId("N"), new Coord(0, 1000));
		Node nodeNN = nf.createNode(Id.createNodeId("NN"), new Coord(0, 1200));
		for(Node node : Arrays.asList(nodeSS, nodeS, nodeE, nodeNN, nodeN, nodeW)){
			network.addNode(node);
		}
		network.addLink(nf.createLink(Id.createLinkId("SS_S"), nodeSS, nodeS));
		network.addLink(nf.createLink(Id.createLinkId("S_SS"), nodeS, nodeSS));

		network.addLink(nf.createLink(Id.createLinkId("S_W"), nodeS, nodeW));
		network.addLink(nf.createLink(Id.createLinkId("W_S"), nodeW, nodeS));
		network.addLink(nf.createLink(Id.createLinkId("N_W"), nodeN, nodeW));
		network.addLink(nf.createLink(Id.createLinkId("W_N"), nodeW, nodeN));

		network.addLink(nf.createLink(Id.createLinkId("N_E"), nodeN, nodeE));
		network.addLink(nf.createLink(Id.createLinkId("E_N"), nodeE, nodeN));
		network.addLink(nf.createLink(Id.createLinkId("E_S"), nodeE, nodeS));
		network.addLink(nf.createLink(Id.createLinkId("S_E"), nodeS, nodeE));

		network.addLink(nf.createLink(Id.createLinkId("NN_N"), nodeNN, nodeN));
		network.addLink(nf.createLink(Id.createLinkId("N_NN"), nodeN, nodeNN));
		HashSet<String> modesSet = new HashSet<String>();
		modesSet.add(TransportMode.bike);
		for(Link link : network.getLinks().values()){
			if(link.getId().toString().contains("E")){
				link.setNumberOfLanes(10000);
				link.setFreespeed(freeSpeedFast);
			} else {
				link.setNumberOfLanes(1);
				link.setFreespeed(freeSpeedSlow);
			}
			link.setAllowedModes(modesSet);
		}


	}


}



