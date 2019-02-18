package org.matsim.run;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.MultiModalBicycleDoorToDoorHandler;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;

public class ConstructSpeedFlowsFromCopenhagen {


	public static void run(String outDir, String type, int it, 
			List<String> ignoredModes, List<String> analysedModes) throws IOException {
		String inputBaseDir = RunBicycleCopenhagen.inputBaseDir;



		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		PopulationReader pr = new PopulationReader(scenario);

		String networkString = outDir +"/output_network.xml.gz";
		if(!new File(networkString).exists() ){
			System.out.println(networkString + " does not exist.");
			if(type.contains("OneLane")){
				networkString =	inputBaseDir + "MATSimCopenhagenNetwork_BicyclesOnly_1Lane.xml.gz";
			} else {
				networkString = inputBaseDir + "MATSimCopenhagenNetwork_BicyclesOnly.xml.gz";
			}
		}
		Network network = NetworkUtils.readNetwork(networkString);

		double totalTravelTime = 0;


		MultiModalBicycleDoorToDoorHandler eventsHandler = new 
				MultiModalBicycleDoorToDoorHandler();
		eventsHandler.setNetwork(network);
		eventsHandler.setIgnoredModes(ignoredModes);
		eventsHandler.setAnalysedModes(analysedModes);

		EventsManager eventsManager = EventsUtils.createEventsManager();
		eventsManager.addHandler(eventsHandler);

		MatsimEventsReader eventsReader = new MatsimEventsReader(eventsManager);
		if(it >= 0){
			eventsReader.readFile(outDir + "/ITERS/it." + it + "/" + it + ".events.xml.gz");
		} else {
			eventsReader.readFile(outDir + "/output_events.xml.gz");
		}
		if(it <0){
			eventsHandler.writeFlowSpeeds(outDir + "/speedFlows.csv");
			eventsHandler.writeFlows(outDir + "/", 8);
		} else {
			eventsHandler.writeFlowSpeeds(outDir + "/ITERS/it." + it + "/speedFlows.csv");
			eventsHandler.writeFlows(outDir + "/ITERS/it." + it + "/", 8);
		}

		totalTravelTime = eventsHandler.totalTravelTime;

		eventsHandler = null;
		eventsManager = null;
		if(it < 0){
			pr.readFile(outDir + "/output_plans.xml.gz");
		} else {
			pr.readFile(outDir + "/ITERS/it." + it + "/" + it + ".plans.xml.gz");	
		}

		double[] measures = calculateCongestedTravelTimeAndTotalDistance(scenario.getPopulation(),
				network, totalTravelTime, new HashSet<String>(analysedModes));
		int nPop = 0;
		for(Person person : scenario.getPopulation().getPersons().values()){
			boolean analysed = false;
			for(PlanElement planElement : person.getSelectedPlan().getPlanElements()){
				if(planElement instanceof Leg && analysedModes.contains(((Leg) planElement).getMode())){
					analysed = true;
				}
			}
			if(analysed){
				nPop++;
			}
		}
		System.out.println(nPop + " cyclists in this study");
		String s = "Total travel time is: " + measures[0]/60. + " minutes\n";
		s += "That is " + (measures[0]/nPop/60.) + " minutes per person\n";
		s += "Total congested travel time is: " + measures[1]/60. + " minutes\n";
		s += "That is " + (measures[1]/nPop/60.) + " minutes per person\n";
		s += "Total distance is: " + measures[2]/1000. + " kilometres\n";
		s += "That is " + (measures[2]/nPop/1000.) + " kilometres per person";
		System.out.println(s);
		FileWriter writer;
		if(it <0){
			writer = new FileWriter(outDir + "/variousMeasures.txt");
		} else {
			writer = new FileWriter(outDir + "/ITERS/it." + it + "/variousMeasures.txt");
		}
		writer.append(s);
		writer.flush();
		writer.close();

	}

	private static double[] calculateCongestedTravelTimeAndTotalDistance(Population pop, Network network, 
			double totalTravelTime, HashSet<String> relevantModes){
		double totalFreeFlowTime= 0.;
		double totalDistance = 0.;


		for(Person person : pop.getPersons().values()){
			HashSet<Integer> relevantLegs = new HashSet<Integer>();
			int i = 0;
			for(PlanElement pe : person.getSelectedPlan().getPlanElements()){
				if(pe instanceof Leg && relevantModes.contains(((Leg) pe).getMode()) ){
					relevantLegs.add(i-2); // access
					relevantLegs.add(i);
					relevantLegs.add(i+2); // egress
				}
				i++;
			}
			i = 0;

			for(PlanElement pe : person.getSelectedPlan().getPlanElements()){
				if(relevantLegs.contains(i)){
					Leg leg = (Leg) pe;
					double travelTime = leg.getTravelTime();
					double distance = leg.getRoute().getDistance();
					double freeFlowTravelTime = travelTime;

					// The router bases distance of entire trip, but travel time without the last link!
					// This means, that when comparing leg by leg, the numbers differ. But since we have
					// taken the actual travel time from the events file, the aggregated numbers are in
					// fact correct without subtract the distance of the last link.. 'Mads.

					//For cars the distance does NOT include the last link...

					// So it makes no sense to compare the free flow travel time with the (route-)travel times,
					// only with the travel times extracted from the events file.

					if(relevantModes.contains(leg.getMode()) && distance > 0){
						if(leg.getMode().equals(TransportMode.bike)){
							double freeSpeed = (double) person.getAttributes().getAttribute("v_0");
							freeFlowTravelTime = Math.ceil(distance / freeSpeed) ;
							Link link = network.getLinks().get(leg.getRoute().getEndLinkId());
						//	System.out.println("Bicycle: "+(Math.ceil(travelTime)-
						//			Math.ceil((distance - link.getLength())/freeSpeed) +1));
						} else if(leg.getMode().equals(TransportMode.car)) {
							NetworkRoute route = (NetworkRoute) leg.getRoute();
							freeFlowTravelTime = 0;
							for(Id<Link> id : route.getLinkIds()){
								Link link = network.getLinks().get(id);
								freeFlowTravelTime += Math.ceil(link.getLength() / link.getFreespeed());
							}
							Link link = network.getLinks().get(leg.getRoute().getEndLinkId());
							freeFlowTravelTime += Math.ceil(link.getLength() / link.getFreespeed());
						}
						//	double surplusDist = network.getLinks().get(leg.getRoute().getEndLinkId()).getLength();			
						//	System.out.println("Actual congested time (excl. last link) for " + 
						//		leg.getMode()+": " + 
						//		(travelTime - (Math.ceil((distance - surplusDist)/freeSpeed) - 1) ) );
						// System.out.println(travelTime - freeFlowTravelTime);
					}
					totalFreeFlowTime += freeFlowTravelTime;
					totalDistance += distance;
				}
				i++;
			}
		}
		double totalCongestedTravelTime = totalTravelTime - totalFreeFlowTime;
		return new double[]{totalTravelTime, totalCongestedTravelTime, totalDistance};
	}

}
