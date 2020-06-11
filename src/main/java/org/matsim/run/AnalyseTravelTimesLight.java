package org.matsim.run;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;

import com.google.common.util.concurrent.AtomicDouble;

import fastOrForcedToFollow.eventhandlers.DetailedMultiModalBicycleDoorToDoorHandler;
import fastOrForcedToFollow.eventhandlers.ExtractTotalTravelTimes;

public class AnalyseTravelTimesLight {

	public static void main(String[] args){
		int it;
		String outDir;
		List<String> analysedModes = new LinkedList<String>();
		if(args.length>0){

			outDir = args[0];
			it = Integer.valueOf(args[1]);
			for(String mode : args[2].split(",")){
				analysedModes.add(mode);
			}
		} else {
			outDir = "/work1/s103232/ABMTRANS2019/withNodeModelling/fullRoWUneven100A";
			it = -1;
			analysedModes.add(TransportMode.car);
			analysedModes.add(TransportMode.truck);
			analysedModes.add(TransportMode.bike);
		}

		try {
			run(outDir, "", it, analysedModes );
		} catch (IOException e) {
			System.err.println("Something went wrong...");
			System.exit(-1);
		}
	}


	public static void run(String outDir, String type, int it, List<String> analysedModes) throws IOException {
		String inputBaseDir = RunBicycleCopenhagen.inputBaseDir;



		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		PopulationReader pr = new PopulationReader(scenario);

		String networkString = outDir +"/output_network.xml.gz";
		if(!new File(networkString).exists() ){
			System.out.println(networkString + " does not exist.");
			if(type.contains("OneLane")){
				networkString =	inputBaseDir + "MATSimCopenhagenNetwork_BicyclesOnly_1Lane.xml.gz";
			} else if(outDir.contains("Mixed") || outDir.contains("Uneven") || outDir.contains("Cars") ){
				networkString = inputBaseDir + "MATSimCopenhagenNetwork_WithBicycleInfrastructure.xml.gz";
			} else {
				networkString = inputBaseDir + "MATSimCopenhagenNetwork_BicyclesOnly.xml.gz";
			}
		}
		Network network = NetworkUtils.readNetwork(networkString);


		ExtractTotalTravelTimes eventsHandler = new ExtractTotalTravelTimes();
		eventsHandler.reset(-1);
		eventsHandler.setNetwork(network);
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
			eventsHandler.writeTotalTravelTimes(outDir);
		} else {
			eventsHandler.writeTotalTravelTimes(outDir + "/ITERS/it." + it + "/");
		}


		//TODO;
		HashSet<String> interestingPersons = eventsHandler.getInterestingLegs();
	
		//TODO;
		// This has to be modified (the class, i.e. MyEventWriterXML ), so that a filter can be added....
		MyEventWriterXML eventWriter = new MyEventWriterXML(outDir + "/filteredEvents.xml.gz");
		eventWriter.setInterestingPersons(interestingPersons);
		eventsManager = EventsUtils.createEventsManager();
		eventsManager.addHandler(eventWriter);
		eventsReader = new MatsimEventsReader(eventsManager);
		if(it < 0){
			eventsReader.readFile(outDir + "/output_events.xml.gz");
		} else {
			eventsReader.readFile(outDir + "/ITERS/it." + it + "/" + it + ".events.xml.gz");
		}
	
		eventWriter.closeFile();


		System.exit(-1);

		LinkedHashMap<String, Double> totalTravelTimes = eventsHandler.getTotalTravelTimes();

		eventsHandler = null;
		eventsManager = null;
		if(it < 0){
			pr.readFile(outDir + "/output_plans.xml.gz");
		} else {
			pr.readFile(outDir + "/ITERS/it." + it + "/" + it + ".plans.xml.gz");	
		}

		HashMap<String,double[]> measuresMap = 
				calculateCongestedTravelTimeAndTotalDistance(scenario.getPopulation(),
						network, totalTravelTimes, new HashSet<String>(analysedModes));
		for(String mode : measuresMap.keySet()){
			int nTrips = 0;
			int nPop = 0;
			for(Person person : scenario.getPopulation().getPersons().values()){
				boolean analysed = false;
				for(PlanElement planElement : person.getSelectedPlan().getPlanElements()){
					if(planElement instanceof Leg && ( ((Leg) planElement).getMode().equals(mode) ||
							(mode.equals("combined") && measuresMap.keySet().contains(
									((Leg) planElement).getMode()) )  )){
						analysed = true;
						nTrips++;
					}
				}
				if(analysed){
					nPop++;
				}
			}
			double[] measures = measuresMap.get(mode);
			String s = nTrips + " " + mode + " trips on " + nPop + " agents in this study\n";
			s += "Total travel time is: " + measures[0]/60. + " minutes\n";
			s += "That is " + (measures[0]/nTrips/60.) + " minutes per trip\n";
			s += "Total congested travel time is: " + measures[1]/60. + " minutes\n";
			s += "That is " + (measures[1]/nTrips/60.) + " minutes per trip\n";
			s += "Total distance is: " + measures[2]/1000. + " kilometres\n";
			s += "That is " + (measures[2]/nTrips/1000.) + " kilometres per trip\n";
			System.out.println(s);
			FileWriter writer;
			if(it <0){
				writer = new FileWriter(outDir + "/variousMeasures_" + mode + ".txt");
			} else {
				writer = new FileWriter(outDir + "/ITERS/it." + it + "/variousMeasures_" + mode + ".txt");
			}
			writer.append(s);
			writer.flush();
			writer.close();
		}

	}

	private static HashMap<String,double[]> calculateCongestedTravelTimeAndTotalDistance(
			Population pop, Network network, 
			LinkedHashMap<String, Double> totalTravelTimes, HashSet<String> relevantModes){

		LinkedHashMap<String, AtomicDouble> totalFreeFlowTimes = 
				new LinkedHashMap<String, AtomicDouble>();
		LinkedHashMap<String, AtomicDouble> totalDistances = 
				new LinkedHashMap<String, AtomicDouble>();

		for(String mode : relevantModes){
			totalFreeFlowTimes.put(mode, new AtomicDouble(0) );
			totalDistances.put(mode,  new AtomicDouble(0) );
		}


		for(Person person : pop.getPersons().values()){
			HashSet<Integer> relevantLegs = new HashSet<Integer>();
			int i = 0;
			String[] modes = new String[person.getSelectedPlan().getPlanElements().size()];
			for(PlanElement pe : person.getSelectedPlan().getPlanElements()){
				if(pe instanceof Leg && relevantModes.contains(((Leg) pe).getMode()) ){
					relevantLegs.add(i-2); // access
					relevantLegs.add(i);
					relevantLegs.add(i+2); // egress
					modes[i-2] = ((Leg) pe).getMode();
					modes[i] = ((Leg) pe).getMode().equals(TransportMode.truck) ? TransportMode.car :  ((Leg) pe).getMode();
					modes[i+2] = ((Leg) pe).getMode();		
				}
				i++;
			}
			i = 0;

			for(PlanElement pe : person.getSelectedPlan().getPlanElements()){
				if(relevantLegs.contains(i)){
					Leg leg = (Leg) pe;
					double travelTime = leg.getTravelTime().seconds();
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
						} else if(leg.getMode().equals(TransportMode.car)) {
							NetworkRoute route = (NetworkRoute) leg.getRoute();
							freeFlowTravelTime = 0;
							for(Id<Link> id : route.getLinkIds()){
								Link link = network.getLinks().get(id);
								freeFlowTravelTime += Math.ceil(link.getLength() / link.getFreespeed());
							}
							Link link = network.getLinks().get(leg.getRoute().getEndLinkId());
							freeFlowTravelTime += Math.ceil(link.getLength() / link.getFreespeed());
							System.out.println(travelTime / freeFlowTravelTime);
						} else if(leg.getMode().equals(TransportMode.truck)) {
							NetworkRoute route = (NetworkRoute) leg.getRoute();
							freeFlowTravelTime = 0;
							for(Id<Link> id : route.getLinkIds()){
								Link link = network.getLinks().get(id);
								double freeSpeed = Math.min(link.getFreespeed(), 80/3.6);
								freeFlowTravelTime += Math.ceil(link.getLength() / freeSpeed);
							}
							Link link = network.getLinks().get(leg.getRoute().getEndLinkId());
							double freeSpeed = Math.min(link.getFreespeed(), 80/3.6);
							freeFlowTravelTime += Math.ceil(link.getLength() / freeSpeed);
							System.out.println(travelTime / freeFlowTravelTime);
						} 
					}
					totalFreeFlowTimes.get(modes[i]).addAndGet(freeFlowTravelTime);
					totalDistances.get(modes[i]).addAndGet(distance);
				}
				i++;
			}
		}
		HashMap<String, Double> totalCongestedTravelTimes = new HashMap<String, Double>();
		for(String mode : relevantModes){
			totalCongestedTravelTimes.put(mode, totalTravelTimes.get(mode).doubleValue() 
					- totalFreeFlowTimes.get(mode).doubleValue());
		}
		HashMap<String, double[]> output = new HashMap<String, double[]>();
		double double1 = 0;
		double double2 = 0;
		double double3 = 0;

		for(String mode : relevantModes){
			output.put(mode, new double[]{
					totalTravelTimes.get(mode), 
					totalCongestedTravelTimes.get(mode), 
					totalDistances.get(mode).doubleValue()} );
			double1 += totalTravelTimes.get(mode);
			double2 += totalCongestedTravelTimes.get(mode);
			double3 += totalDistances.get(mode).doubleValue();
		}
		output.put("combined", new double[]{double1, double2, double3});
		return output;
	}

}
