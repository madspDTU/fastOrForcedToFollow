/* *********************************************************************** *
 * project: org.matsim.*
 * EventsReadersTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package fastOrForcedToFollow.eventhandlers;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.TeleportationArrivalEvent;
import org.matsim.core.api.internal.HasPersonId;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.run.ConstructSpeedFlowsFromCopenhagen;


public class MultiModalBicycleDoorToDoorHandler implements BasicEventHandler {

	static Network network;

	final static double timeStepSize = 3600;
	final static double endTime = 25*3600;  // From 0-1 it is necessary to add 1 and 25 probably.
	final static int numberOfSlots = (int) Math.ceil(endTime/ timeStepSize);
	static double currentTime = 0;
	static int printCounter = 0;
	final static int printHowOften = 60*15;
	public HashMap<String,Double> totalTravelTimes = new HashMap<String,Double>();
	public HashMap<String,Double> totalHighlightTravelTimes = new HashMap<String,Double>();
	public HashMap<String,Integer> highlightTrips = new HashMap<String,Integer>();
	public HashMap<String,HashMap<String,LinkedList<Double>>> everyTravelTime = 
			new HashMap<String,HashMap<String,LinkedList<Double>>>();

	HashMap<String, int[]> flows;
	HashMap<String, LinkedList<Double>[]> speeds = new HashMap<String, LinkedList<Double>[]>();
	HashMap<String, HashMap<String, Double>> entryTimes = new HashMap<String, HashMap<String,Double>>();
	HashMap<String, Double> personLegStarts = new HashMap<String, Double>();
	HashMap<String, String> personLegModes = new HashMap<String, String>();
	
	HashSet<String> ignoredActivities;
	HashMap<String,String> vehicleToPerson = new HashMap<String,String>();
	LinkedList<String> detailedOutput = new LinkedList<String>();

	private final String TELEPORTATION_MODE = "teleportation";

	private Population population;

	private HashMap<String, Integer> populationCurrentLeg;

	private boolean fetchHourlyLinkFlows = false; //default is false - can be set to true.

	private HashSet<String> highlightPersonOriginTime = new HashSet<String>();
	private HashSet<String> highlightPersonOriginGeography = new HashSet<String>();




	public void reset(final int iteration) {

	}

	public void initiateFlowsMap(){
		flows = new HashMap<String, int[]>();
		for(Id<Link> linkId : network.getLinks().keySet()){
			flows.put(linkId.toString(), new int[numberOfSlots]);
		}
	}

	public void setNetwork(Network network){
		this.network = network;
		if(fetchHourlyLinkFlows){
			initiateFlowsMap();
		}
	}

	public void setFetchHourlyLinkFlows(boolean fetchHourlyLinkFlows){
		this.fetchHourlyLinkFlows = fetchHourlyLinkFlows; 
		if(this.fetchHourlyLinkFlows && network != null){
			initiateFlowsMap();
		}
	}

	public void setPopulation(Population population){
		this.population = population;
		populationCurrentLeg = new HashMap<String, Integer>();
		for(Person person : this.population.getPersons().values()){
			populationCurrentLeg.put(person.getId().toString(), 0); 
		}
	}

	public void setAnalysedModes(List<String> analysedModes){
		this.ignoredActivities = new HashSet<String>();
		for(String mode : analysedModes){
			totalTravelTimes.put(mode, 0.);
			totalHighlightTravelTimes.put(mode, 0.);
			highlightTrips.put(mode, 0);
			this.ignoredActivities.add(mode + " interaction");
		}
	}

	public void writeDetailedOutput(String fileName){
		try {
			FileWriter writer = new FileWriter(fileName);
			writer.append("Person;Trip;Mode;StartTime;TravelTime;PlannedDuration\n");
			for(String s : detailedOutput){
				writer.append(s + "\n");
			}
			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private String bestGuessVehicleId(String vehicleId){
		if(personLegStarts.containsKey(vehicleId)){
			return vehicleId;
		} else if(personLegStarts.containsKey(vehicleToPerson.get(vehicleId))){
			return vehicleToPerson.get(vehicleId);
		}
		return ":(";
	}

	@Override
	public void handleEvent(Event event) {
		if(event instanceof LinkEnterEvent){
			// These are ignored for now. Can be used to collect speeds and flows.
			//		LinkEnterEvent e = (LinkEnterEvent) event;
			//		String vehicleId = bestGuessVehicleId(e.getVehicleId().toString());
			//		if(personLegStarts.containsKey(vehicleId)){
			//			double now = e.getTime();
			//			String linkId = e.getLinkId().toString();
			//			if(!flows.containsKey(linkId)){
			//				flows.put(linkId, new int[numberOfSlots]);
			//				LinkedList<Double>[] newLinkedListArray = new LinkedList[numberOfSlots];
			//				for(int i = 0; i < newLinkedListArray.length; i++){
			//					newLinkedListArray[i] = new LinkedList<Double>();
			//				}
			//				speeds.put(linkId, newLinkedListArray);
			//				entryTimes.put(linkId, new HashMap<String, Double>());
			//			}
			//			entryTimes.get(linkId).put(vehicleId, now);
			//			print(now);
			//		} 
		} else if(event instanceof LinkLeaveEvent && fetchHourlyLinkFlows){
			LinkLeaveEvent e = (LinkLeaveEvent) event;
			double now = e.getTime();
			if(now < endTime){
				int slot = timeToSlot(now);
				String linkId = e.getLinkId().toString();
				flows.get(linkId)[slot]++;
			}
		} else if(event instanceof ActivityStartEvent){
			ActivityStartEvent e = (ActivityStartEvent) event;
			String actType = e.getActType();

			//If the acttype is non-trivial (i.e. not an interaction).
			if(!ignoredActivities.contains(actType)){
				String personId = e.getPersonId().toString();

				//If we have not picked up a mode yet, it is because no vehicle has ever entered traffic.
				// Thus, it must be a teleportation. 
				String mode = personLegModes.containsKey(personId) ? personLegModes.get(personId) : TransportMode.walk;
				// It doesn't contain the TELEPORTATION_MODE and the TransportMode.walk - as such, these are ignored.
				if(totalTravelTimes.containsKey(mode)){
					double now = e.getTime();
					double then = personLegStarts.get(personId);
					if(now > then){
						double TT = now - then - 1;

						double plannedDuration = getPlannedDuration(personId, mode);
						if(totalTravelTimes.containsKey(mode)){
							//Not interesting if the mode was teleported.
							detailedOutput.add(personId + ";" + populationCurrentLeg.get(personId) + ";" +
									mode + ";" + then + ";" + TT + ";" + plannedDuration);
						}

						double before = totalTravelTimes.get(mode);
						totalTravelTimes.put(mode, before + TT);
						
						boolean originWithinHighlightGeography = highlightPersonOriginGeography.contains(personId);
						boolean originWithinHighlightTime = highlightPersonOriginTime.contains(personId);
						highlightPersonOriginGeography.remove(personId);
						highlightPersonOriginTime.remove(personId);
						Coord coord = network.getLinks().get(e.getLinkId()).getFromNode().getCoord();
						
						boolean destinationWithinHighlightGeography = isInHighlightArea(coord);
						boolean destinationWithinHighlightTime = isInHighlightTime(now);
							
						boolean isHighlightTrip = isHighlightTrip(
								originWithinHighlightTime,
								destinationWithinHighlightTime,
								originWithinHighlightGeography,
								destinationWithinHighlightGeography);
						
						if(isHighlightTrip){
							before = totalHighlightTravelTimes.get(mode);
							int beforeTrips = highlightTrips.get(mode);
							totalHighlightTravelTimes.put(mode, before + TT);
							highlightTrips.put(mode, beforeTrips + 1);
						}
						if(!everyTravelTime.containsKey(personId)){
							everyTravelTime.put(personId, new HashMap<String,LinkedList<Double>>());
						}
						if(!everyTravelTime.get(personId).containsKey(mode)){
							everyTravelTime.get(personId).put(mode, new LinkedList<Double>());
						}
						everyTravelTime.get(personId).get(mode).addLast(TT);
					}
					print(now);
				}
				personLegStarts.remove(personId);
				personLegModes.remove(personId);
			}
		} else if(event instanceof ActivityEndEvent){
			ActivityEndEvent e = (ActivityEndEvent) event;
			String actType = e.getActType();

			//If non-trivial activity (i.e. not an interaction).
			if(!ignoredActivities.contains(actType)){
				String personId = e.getPersonId().toString();
				double now = e.getTime();
				personLegStarts.put(personId, now);
				Coord coord = getCoord(e.getLinkId());
				if(isInHighlightArea(coord)){
					highlightPersonOriginGeography.add(personId);
				}
				if(isInHighlightTime(now)){
					highlightPersonOriginTime.add(personId);
				}
				print(now);
			}
		} else if(event instanceof VehicleEntersTrafficEvent){
			// This is only used for determining the (network) mode of the leg.

			VehicleEntersTrafficEvent e = (VehicleEntersTrafficEvent) event;
			String personId = e.getPersonId().toString();
			if(personLegStarts.containsKey(personId)){ //Have to check, could be PT-driver.
				personLegModes.put(personId,e.getNetworkMode());
			} else {
				System.out.println("This shouldn't be able to happen with pt disabled."
						+ " Vehicle entered but no leg has started");
			}
			//} else if (event instanceof TeleportationArrivalEvent){
			//TeleportationArrivalEvent e = (TeleportationArrivalEvent) event;
			//String personId = e.getPersonId().toString();
			//personLegModes.put(personId, TELEPORTATION_MODE );
			// It turned out that actual teleporting is done in a different way,
			// and does not produce an event. The TeleportationArrivalEvent is 
			// for non-network travel (e.g. non_network_walk.
			// The teleportation caused by removing parts of the day, can
			// be fully ignored when processing the events file.
		} 
	}

	private Coord getCoord(Id<Link> linkId) {
		return network.getLinks().get(linkId).getCoord();
	}

	public static boolean isInHighlightTime(double now) {
		return now >= ConstructSpeedFlowsFromCopenhagen.highlightStartTime &&
				now <= ConstructSpeedFlowsFromCopenhagen.highlightEndTime;
	}

	public static boolean isHighlightTrip(Leg leg){
		double originTime = leg.getDepartureTime();
		double destinationTime = originTime + leg.getTravelTime();
		Coord originCoord = network.getLinks().get(leg.getRoute().getStartLinkId()).getCoord();
		Coord destinationCoord = network.getLinks().get(leg.getRoute().getEndLinkId()).getCoord();
		boolean originTimeBool = isInHighlightTime(originTime);
		boolean destinationTimeBool = isInHighlightTime(destinationTime);
		boolean originGeo = isInHighlightArea(originCoord);
		boolean destinationGeo = isInHighlightArea(destinationCoord);
		return isHighlightTrip(originTimeBool, destinationTimeBool, originGeo, destinationGeo);
	}
	
	public static boolean isHighlightTrip(boolean originTime, boolean destinationTime,
			boolean originGeography, boolean destinationGeography) {
		return destinationGeography && originTime  && originGeography;
	}

	private double getPlannedDuration(String personId, String mode) {
		Person person = this.population.getPersons().get(Id.create(personId, Person.class));
		Plan plan = person.getSelectedPlan();
		int index = populationCurrentLeg.get(personId) + 1;
		PlanElement pe = plan.getPlanElements().get(index);
		while(!(pe instanceof Leg) || !((Leg) pe).getMode().equals(mode)){
			index++;
			if(index >= plan.getPlanElements().size()){
				System.err.println("This is an error. No index satisfied the mode");
				System.exit(-1);
			}
			pe = plan.getPlanElements().get(index);
		}
		populationCurrentLeg.put(personId, index);

		if(!totalTravelTimes.containsKey(mode)){
			//Teleported, and thus not interesting.
			return ((Leg) plan.getPlanElements().get(index)).getTravelTime();
		} // else

		double plannedDuration = 0;
		Leg leg;
		for(int i = index - 2; i <= index + 2; i += 4){
			leg = ((Leg) plan.getPlanElements().get(i));
			plannedDuration += leg.getTravelTime();
		}

		leg = ((Leg) plan.getPlanElements().get(index));
		double freeFlowTravelTime = 0;
		if(mode.equals(TransportMode.bike)){
			double distance = ((Leg) plan.getPlanElements().get(index)).getRoute().getDistance();
			double freeSpeed = (double) person.getAttributes().getAttribute("v_0");
			freeFlowTravelTime = Math.ceil(distance / freeSpeed) ;
		} else if (mode.equals(TransportMode.car)){
			NetworkRoute route = (NetworkRoute) leg.getRoute();
			Link link;
			for(Id<Link> id : route.getLinkIds()){
				link = network.getLinks().get(id);
				freeFlowTravelTime += Math.ceil(link.getLength() / link.getFreespeed());
			}
			link = network.getLinks().get(leg.getRoute().getEndLinkId());
			freeFlowTravelTime += Math.ceil(link.getLength() / link.getFreespeed());
		}
		plannedDuration += freeFlowTravelTime;

		return plannedDuration;
	}

	public static boolean isInHighlightArea(Coord c){
		Coord[] v = getVertices();
		int j  = v.length -1;
		boolean oddNodes = false;
		for(int i = 0; i< v.length; i++){
			if((v[i].getY() < c.getY() && v[j].getY() >= c.getY()) ||
					v[j].getY() < c.getY() && v[i].getY() >= c.getY() ){
				if(v[i].getX() + (c.getY() - v[i].getY()) / (v[j].getY() - v[i].getY()) * (v[j].getX() - v[i].getX()) < c.getX() ){
					oddNodes = !oddNodes;
				}
			}
			j = i;
		}
		return oddNodes;
	}



	public static Coord[] getVertices() {

		LinkedList<Coord> coords = new LinkedList<Coord>();
		// All sites from https://epsg.io/map#srs=32632

		coords.addLast(new Coord(725704.792574,	6169796.538738)); // Ørestad Syd - øst
		coords.addLast(new Coord(724402.912291,6168994.395954)); // Ørestad Syd - vest
		coords.addLast(new Coord(720752.041046, 6170926.423185)); // Frihedenish
		coords.addLast(new Coord(718709.685939,	6173653.269510)); // Syd for Damhussøen
		coords.addLast(new Coord(716708.184015,	6178214.948262)); // Motorring 3 v. Islev
		coords.addLast(new Coord(718856.372724,	6181061.096926)); // Tingbjerg
		coords.addLast(new Coord(728852.561565, 6181618.840032)); // Nordhavn
		coords.addLast(new Coord(729605.453470, 6172658.247045)); // Amager Strandpark
		coords.addLast(new Coord(725956.710891, 6171625.034203)); // Bygrænsen

		Coord[] output = new Coord[coords.size()];
		for(int i = 0; i < output.length; i++){
			output[i] = coords.pollFirst();
		}
		return output;
	}




	private void print(double now){
		if(now > currentTime){
			currentTime = now + 0.01;

			if(printCounter % printHowOften == 0){
				int h = (int) Math.floor(now/3600);
				if(h< 10){
					System.out.print(0);
				}
				System.out.print(h + ":");
				now -= h*3600;
				int m = (int) Math.floor(now/60);
				if(m< 10){
					System.out.print(0);
				}
				System.out.print(m + ":");
				now -= m*60;
				int s = (int) Math.floor(now);
				if(s< 10){
					System.out.print(0);
				}
				System.out.println(s);
			}
			printCounter++;
		}
	}

	private int timeToSlot(double now){
		return (int) Math.floor(now / timeStepSize);
	}

	public void writeFlowSpeeds(String outFile) throws IOException{
		FileWriter writer = new FileWriter(outFile);

		writer.append("Link;Flow;Speed;TimeSlot\n");
		for(String linkId : flows.keySet()){
			for(int i = 0; i < numberOfSlots; i++){
				int timeSlotId = i +1;
				int flow = flows.get(linkId)[i];
				double avgSpeed = 0;
				double n = 0;
				for(double speed : speeds.get(linkId)[i]){
					avgSpeed += 1/speed;
					n++;
				}
				avgSpeed = 1/(avgSpeed/n);
				writer.append(linkId + ";" + flow + ";" + avgSpeed + ";" + timeSlotId + "\n");
			}
		}
		writer.flush();
		writer.close();
	}

	public void writeAllFlows(String outDir) throws IOException{
		String outFile = outDir + "AllFlows.csv";
		String outFile2 = outDir + "LinkGeometry.csv";
		FileWriter writer = new FileWriter(outFile);
		FileWriter writer2 = new FileWriter(outFile2);
		writer.append("LinkId;ToHour;Flow\n");
		writer2.append("LinkId;FromX;FromY;ToX;ToY\n");
		for(String linkId : flows.keySet()){
			Link link = network.getLinks().get(Id.create(linkId,Link.class));
			Coord fromCoord = link.getFromNode().getCoord(); 
			Coord toCoord = link.getToNode().getCoord();
			double fromX = fromCoord.getX();
			double fromY = fromCoord.getY();
			double toX = toCoord.getX();
			double toY = toCoord.getY();
			writer2.append(linkId + ";" + fromX + ";" + fromY + ";" + toX + ";" + toY + "\n");
			int toHour = 1;
			for(int flow : flows.get(linkId)){
				writer.append(linkId + ";" + toHour + ";" + flow + "\n");
				toHour++;
			}
		}
		writer.flush();
		writer.close();
		writer2.flush();
		writer2.close();
	}


	public void writeFlows(String outDir, int timeSlotId) throws IOException{
		String outFile = outDir + "flows" + (timeSlotId - 1) + "to" + timeSlotId + ".csv";
		FileWriter writer = new FileWriter(outFile);
		writer.append("Link;Flow\n");
		for(String linkId : flows.keySet()){
			int flow = flows.get(linkId)[timeSlotId-1];
			writer.append(linkId + ";" + flow + "\n");
		}
		writer.flush();
		writer.close();
	}

	/*
	public void writeTotalTravelTime(String outDir) throws IOException{
		int numberOfAgents = 547085;
		String outFile = outDir + "totalTravelTime.txt";
		FileWriter writer = new FileWriter(outFile);
		String printString1 = "Total travel time is: " + totalTravelTime + " seconds";
		String printString2 = "That is " + (totalTravelTime/numberOfAgents/60.) + " minutes per person.";
		writer.append(printString1);
		writer.append(printString2);
		writer.flush();
		writer.close();
		System.out.println(printString1);
		System.out.println(printString2);
	}
	 */


}
