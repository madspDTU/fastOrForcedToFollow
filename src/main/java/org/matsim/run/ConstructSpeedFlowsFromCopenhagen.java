package org.matsim.run;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.FlowHandler;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;

public class ConstructSpeedFlowsFromCopenhagen {

	public static String type = "small";
	public static int it = -1; //Use a negative number to use the final events file.

	public static void main(String[] args) throws IOException {
		String inputBaseDir = RunBicycleCopenhagen.inputBaseDir;
		String outputBaseDir = RunBicycleCopenhagen.outputBaseDir;
		String outDir = null;

		for(String s : args){
			System.out.print(s + " ");
		} System.out.print("\n");

		if(args.length > 0){
			outDir = args[0];
			type = args[1];	
			it = Integer.valueOf(args[2]);
		} else {
			outDir = outputBaseDir + type;
		}
	

		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		PopulationReader pr = new PopulationReader(scenario);

		String networkString = outDir +"/output_network.xml.gz";
		if(!new File(networkString).exists() ){
			System.out.println(networkString + " does not exist.");
			if(type.contains("OneLane")){
				networkString =
						inputBaseDir + "MATSimCopenhagenNetwork_BicyclesOnly_1Lane.xml.gz";

			} else {
				networkString = inputBaseDir + "MATSimCopenhagenNetwork_BicyclesOnly.xml.gz";
			}
		}
		Network network = NetworkUtils.readNetwork(networkString);

		double totalTravelTime = 0;
		
		
			FlowHandler eventsHandler = new FlowHandler();
			eventsHandler.setNetwork(network);

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

			double[] measures = calculateCongestedTravelTimeAndTotalDistance(scenario.getPopulation(), network,
					totalTravelTime);
			int nPop = scenario.getPopulation().getPersons().size();

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
			double totalTravelTime){
		double totalFreeFlowTime= 0.;
		double totalDistance = 0.;

		for(Person person : pop.getPersons().values()){
			for(PlanElement pe : person.getSelectedPlan().getPlanElements()){
				if(pe instanceof Leg){
					Leg leg = (Leg) pe;
					double travelTime = leg.getTravelTime();
					double distance = leg.getRoute().getDistance();
					double freeSpeed = (double) person.getAttributes().getAttribute("v_0");
					double freeFlowTravelTime = travelTime;
					if( leg.getMode().equals(TransportMode.bike) ){
						freeFlowTravelTime= Math.ceil(distance/freeSpeed);
					}
					/*
					System.err.println("\n\nMode: " + leg.getMode());
					System.err.println(network.getLinks().get(leg.getRoute().getEndLinkId()).getLength() / freeSpeed);
					System.err.println("Travel time: " + travelTime);
					System.err.println("Distance: " + distance);
					System.err.println("Congested travel time:" + congestedTravelTime);
					 */
					totalFreeFlowTime += freeFlowTravelTime;
					totalDistance += distance;
				}
			}
		}
		double totalCongestedTravelTime = totalTravelTime - totalFreeFlowTime;
		return new double[]{totalTravelTime, totalCongestedTravelTime, totalDistance};
	}

}
