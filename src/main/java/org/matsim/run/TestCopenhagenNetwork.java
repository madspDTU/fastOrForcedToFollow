package org.matsim.run;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

import fastOrForcedToFollow.FFFUtils;
import fastOrForcedToFollow.configgroups.FFFConfigGroup;

public class TestCopenhagenNetwork {

	public static void main(String[] args) {

		
		Config combinedConfig = ConfigUtils.createConfig();
		Scenario combinedScenario = ScenarioUtils.createScenario(combinedConfig);
		Population combinedPop = combinedScenario.getPopulation();

		Config bicycleConfig = ConfigUtils.createConfig();
		bicycleConfig.global().setRandomSeed(60190);
		Scenario bicycleScenario = ScenarioUtils.createScenario(bicycleConfig);

		Config carConfig = ConfigUtils.createConfig();
		Scenario carScenario = ScenarioUtils.createScenario(carConfig);

		/*

		
		boolean bool1 = true;
		PopulationReader pr2 = new PopulationReader(bicycleScenario);
		pr2.readFile("C:/Users/madsp/DTA/ResumableInput/ResumablePlans_Bicycles.xml.gz");
		for(Person person : bicycleScenario.getPopulation().getPersons().values()){
			double bestScore = Double.NEGATIVE_INFINITY;
			int bestPlan = -1;
			if(bool1) {
				System.out.println("Agent has " + person.getPlans().size() + " plans before");
			}
			int i = 0;
			for(Plan plan : person.getPlans()){
				double score = plan.getScore();
				if(score > bestScore) {
					bestScore = score;
					bestPlan = i;
				}
				i++;
			}
			LinkedList<Plan> plansToRemove = new LinkedList<Plan>();
			i = 0;
			for(Plan plan : person.getPlans()){
				if(i != bestPlan) {
					plansToRemove.add(plan);
				}
				i++;
			}
			for(Plan plan: plansToRemove) {
				person.removePlan(plan);
			}
			if(bool1) {
				System.out.println("Agent has " + person.getPlans().size() + " plans afterwards");
				bool1 = false;
			}
		}
		PopulationWriter pw2 = new PopulationWriter(bicycleScenario.getPopulation());
		pw2.write("C:/Users/madsp/DTA/ResumableInput/ResumablePlans_Bicycles_selectedOnly.xml.gz");
		bicycleScenario = null;
		pw2 = null;
		pr2 = null;

		PopulationReader pr3 = new PopulationReader(carScenario);
		pr3.readFile("C:/Users/madsp/DTA/ResumableInput/ResumablePlans_Cars.xml.gz");
		for(Person person : carScenario.getPopulation().getPersons().values()){
			double bestScore = Double.NEGATIVE_INFINITY;
			int bestPlan = -1;
			if(bool1) {
				System.out.println("Agent has " + person.getPlans().size() + " plans before");
			}
			int i = 0;
			for(Plan plan : person.getPlans()){
				double score = plan.getScore();
				if(score > bestScore) {
					bestScore = score;
					bestPlan = i;
				}
				i++;
			}
			LinkedList<Plan> plansToRemove = new LinkedList<Plan>();
			i = 0;
			for(Plan plan : person.getPlans()){
				if(i != bestPlan) {
					plansToRemove.add(plan);
				}
				i++;
			}
			for(Plan plan: plansToRemove) {
				person.removePlan(plan);
			}
			if(bool1) {
				System.out.println("Agent has " + person.getPlans().size() + " plans afterwards");
				bool1 = false;
			}
		}
		PopulationWriter pw3 = new PopulationWriter(carScenario.getPopulation());
		pw3.write("C:/Users/madsp/DTA/ResumableInput/ResumablePlans_Cars_selectedOnly.xml.gz");
		carScenario = null;
		pw3 = null;
		pr3 = null;

		PopulationReader pr4 = new PopulationReader(combinedScenario);
		pr4.readFile("C:/Users/madsp/DTA/ResumableInput/ResumablePlans_Both.xml.gz");
		for(Person person : combinedScenario.getPopulation().getPersons().values()){
			double bestScore = Double.NEGATIVE_INFINITY;
			int bestPlan = -1;
			if(bool1) {
				System.out.println("Agent has " + person.getPlans().size() + " plans before");
			}
			int i = 0;
			for(Plan plan : person.getPlans()){
				double score = plan.getScore();
				if(score > bestScore) {
					bestScore = score;
					bestPlan = i;
				}
				i++;
			}
			LinkedList<Plan> plansToRemove = new LinkedList<Plan>();
			i = 0;
			for(Plan plan : person.getPlans()){
				if(i != bestPlan) {
					plansToRemove.add(plan);
				}
				i++;
			}
			for(Plan plan: plansToRemove) {
				person.removePlan(plan);
			}
			if(bool1) {
				System.out.println("Agent has " + person.getPlans().size() + " plans afterwards");
				bool1 = false;
			}
}
		PopulationWriter pw4 = new PopulationWriter(combinedScenario.getPopulation());
		pw4.write("C:/Users/madsp/DTA/ResumableInput/ResumablePlans_Both_selectedOnly.xml.gz");
		combinedScenario = null;
		pw4 = null;
		pr4 = null;
		System.exit(-1);
*/
		
		String dir = "/zhome/81/e/64390/MATSim/DTA2020/input/";
		PopulationReader bicycleReader = new PopulationReader(bicycleScenario);
		bicycleReader.readFile(dir + "BicyclePlans_CPH_Full.xml.gz");
		FFFUtils.addCyclistAttributes(bicycleScenario);
		PopulationWriter bicycleWriter = new PopulationWriter(bicycleScenario.getPopulation());
		bicycleWriter.write(dir + "BicyclePlans_CPH_Full.xml.gz");
		Population bicyclePop = bicycleScenario.getPopulation();


		PopulationReader carReader = new PopulationReader(carScenario);
		carReader.readFile(dir + "CarPopulation2020_OTM_full.xml.gz");
		Population carPop = carScenario.getPopulation();

		for(Population pop : Arrays.asList(bicyclePop,carPop)){
			for(Person person : pop.getPersons().values()){
				combinedPop.addPerson(person);
			}
		}	

		PopulationWriter combinedWriter = new PopulationWriter(combinedPop);
		combinedWriter.write(dir + "COMPASBicycle100_COMPASSCarOTM100.xml.gz");




		System.exit(-1);

		/*
		NetworkCleaner nc = new NetworkCleaner();

		Network network = combinedScenario.getNetwork();


		MatsimNetworkReader nr =  new MatsimNetworkReader(network);
		nr.readFile("/zhome/81/e/64390/MATSim/ABMTRANS2019/input/MATSimCopenhagenNetwork_WithBicycleInfrastructure_old.xml.gz");


		double tol = 7; // in metres. full2 running with 7. you could try 6.	
		TreeMap<Double,Link> treeMap2 = new TreeMap<Double,Link>();
		for(Link link : network.getLinks().values()){
			if(link.getLength() < tol){
				treeMap2.put(link.getLength(),link);
			}
		}
		LinkedList<Link> linkedList = new LinkedList<Link>();
		while(!treeMap2.isEmpty()){
			linkedList.addLast(treeMap2.pollFirstEntry().getValue());
		}
		System.out.println(linkedList.size() + " of " + network.getLinks().size() + " links are less than " + tol + "m long...");
		System.out.println("Network had " + network.getNodes().size() + " nodes and " + network.getLinks().size() + " links.");


		// TODO! Instead of always eliminating the to-node, eliminate the one that has the most short links 

		while(!linkedList.isEmpty()){
			Link link = linkedList.pollFirst();
			double length = link.getLength();
			if(network.getLinks().containsKey(link.getId()) && length < tol){
				Coord newCoord = new Coord(link.getFromNode().getCoord().getX()/2. + link.getToNode().getCoord().getX()/2.,
						link.getFromNode().getCoord().getY()/2. + link.getToNode().getCoord().getY()/2.);
				link.getFromNode().setCoord(newCoord);
				for(Link l : link.getFromNode().getInLinks().values()){
					if(l.getFromNode() == link.getToNode()){
						network.removeLink(l.getId());
					} else {
						l.setLength(l.getLength() + length/2.);
					}
				}
				for(Link l : link.getFromNode().getOutLinks().values()){
					if(l.getToNode() == link.getToNode() && !l.getId().equals(link.getId())){
						network.removeLink(l.getId());
					} else {
						l.setLength(l.getLength() + length/2.);
					}
				}

				for(Link l : link.getToNode().getInLinks().values()){
					if(!l.getId().equals(link.getId())){
						l.setLength(l.getLength() + length/2.);
						l.setToNode(link.getFromNode());
						link.getFromNode().addInLink(l);
						link.getToNode().removeInLink(l.getId());
					}
				}
				for(Link l : link.getToNode().getOutLinks().values()){
					l.setLength(l.getLength() + length/2.);
					l.setFromNode(link.getFromNode());
					link.getFromNode().addOutLink(l);
					link.getToNode().removeOutLink(l.getId());
				}
				Gbl.assertIf(link.getToNode().getOutLinks().isEmpty());
				Gbl.assertIf(link.getToNode().getInLinks().size() == 1);
				network.removeNode(link.getToNode().getId());
			}
		}
		System.out.println("Network now has " + network.getNodes().size() + " nodes and " + network.getLinks().size() + " links.");

		NetworkWriter writer = new NetworkWriter(network);
		writer.write("/zhome/81/e/64390/MATSim/ABMTRANS2019/input/MATSimCopenhagenNetwork_WithBicycleInfrastructure.xml.gz");
		nc.run("/zhome/81/e/64390/MATSim/ABMTRANS2019/input/MATSimCopenhagenNetwork_WithBicycleInfrastructure.xml.gz",
				"/zhome/81/e/64390/MATSim/ABMTRANS2019/input/MATSimCopenhagenNetwork_WithBicycleInfrastructure.xml.gz");

		Network bicycleNetwork = extractModeSpecificNetwork(network, TransportMode.bike);
		Network carNetwork = extractModeSpecificNetwork(network, TransportMode.car);
		writer = new NetworkWriter(bicycleNetwork);
		writer.write("/zhome/81/e/64390/MATSim/ABMTRANS2019/input/MATSimCopenhagenNetwork_BicyclesOnly.xml.gz");
		nc.run("/zhome/81/e/64390/MATSim/ABMTRANS2019/input/MATSimCopenhagenNetwork_BicyclesOnly.xml.gz",
				"/zhome/81/e/64390/MATSim/ABMTRANS2019/input/MATSimCopenhagenNetwork_BicyclesOnly.xml.gz");
		writer = new NetworkWriter(carNetwork);
		writer.write("/zhome/81/e/64390/MATSim/ABMTRANS2019/input/MATSimCopenhagenNetwork_CarsOnly.xml.gz");
		nc.run("/zhome/81/e/64390/MATSim/ABMTRANS2019/input/MATSimCopenhagenNetwork_CarsOnly.xml.gz",
				"/zhome/81/e/64390/MATSim/ABMTRANS2019/input/MATSimCopenhagenNetwork_CarsOnly.xml.gz");
		System.exit(-1);

/*

		/*	
		for(Link link : network.getLinks().values()){

			if(link.getAllowedModes().contains(TransportMode.car)){		
				if(link.getNumberOfLanes()> 4){
					System.out.println(link.getCapacity() + "  " + link.getCapacity() / link.getNumberOfLanes() +
							" " + link.getNumberOfLanes());
				}
			}
		}

		for(Node node : network.getNodes().values()){
			int inCar = 0;
			int inBicycle = 0;
			int outCar = 0;
			int outBicycle = 0;
			for(Link link : node.getInLinks().values()){
				if(link.getAllowedModes().contains(TransportMode.car)){
					inCar++;
				}
				if(link.getAllowedModes().contains(TransportMode.bike)){
					inBicycle++;
				}
			}
			for(Link link : node.getOutLinks().values()){
				if(link.getAllowedModes().contains(TransportMode.car)){
					outCar++;
				}
				if(link.getAllowedModes().contains(TransportMode.bike)){
					outBicycle++;
				}
			}
			if(inCar == 2 && outCar == 1){
				System.out.println("In: " + inBicycle + "   out: " + outBicycle);
			}
		}
		 */
	}


	private static Network extractModeSpecificNetwork(Network network, String mode){
		Config newConfig = ConfigUtils.createConfig();
		Scenario newScenario = ScenarioUtils.createScenario(newConfig);
		Network newNetwork = newScenario.getNetwork();

		for(Node node : network.getNodes().values()){
			Node newNode = NetworkUtils.createNode(node.getId(), node.getCoord());
			newNetwork.addNode(newNode);
		}
		for(Link link : network.getLinks().values()){
			if(link.getAllowedModes().contains(mode)){
				newNetwork.addLink(link);
			} 
		}
		LinkedList<Node> nodesToBeRemoved = new LinkedList<Node>();
		for(Node node : network.getNodes().values()){
			for(Link link : node.getInLinks().values()) {
				if(!link.getAllowedModes().contains(mode)) {
					node.removeInLink(link.getId());
				}
			}
			for(Link link : node.getOutLinks().values()) {
				if(!link.getAllowedModes().contains(mode)) {
					node.removeOutLink(link.getId());
				}
			}
			if( node.getInLinks().isEmpty() && node.getOutLinks().isEmpty()){
				nodesToBeRemoved.add(node);
			}
		}
		for(Node node : nodesToBeRemoved){
			network.removeNode(node.getId());
		}
		return newNetwork;
	}
}
