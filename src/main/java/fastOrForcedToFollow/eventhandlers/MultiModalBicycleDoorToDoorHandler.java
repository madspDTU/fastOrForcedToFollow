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
import java.util.concurrent.atomic.AtomicInteger;

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
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
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
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.run.ConstructSpeedFlowsFromCopenhagen;
import org.matsim.vehicles.Vehicle;

import com.google.common.util.concurrent.AtomicDouble;


public class MultiModalBicycleDoorToDoorHandler implements BasicEventHandler {

	private  Network network;

	final static double timeStepSize = 3600;
	final static double endTime = 27*3600;  // From 0-1 it is necessary to add 1 and 25 probably.
	final static int numberOfSlots = (int) Math.ceil(endTime/ timeStepSize);
	static double currentTime = 0;
	static int printCounter = 0;
	final static int printHowOften = 60*15;
	public HashMap<String,AtomicDouble> totalTravelTimes = new HashMap<String,AtomicDouble>();
	public HashMap<String,AtomicDouble> totalHighlightTravelTimes = new HashMap<String,AtomicDouble>();
	public HashMap<String,AtomicInteger> highlightTrips = new HashMap<String,AtomicInteger>();
	public HashMap<Id<Person>,HashMap<String,LinkedList<Double>>> everyTravelTime = 
			new HashMap<Id<Person>,HashMap<String,LinkedList<Double>>>();

	HashMap<Id<Link>, int[]> flows;
	HashMap<Id<Link>, int[]> stucks;
	HashMap<Id<Link>, long[]> travelTimes;

	HashMap<Id<Link>, LinkedList<Double>[]> speeds = new HashMap<Id<Link>, LinkedList<Double>[]>();
	HashMap<Id<Vehicle>, LinkEnterEvent> entries = new HashMap<Id<Vehicle>, LinkEnterEvent>();
	HashMap<Id<Person>, Double> personLegStarts = new HashMap<Id<Person>, Double>();
	HashMap<Id<Person>, String> personLegModes = new HashMap<Id<Person>, String>();
	HashMap<Id<Person>, Double > latestTimes = new HashMap<Id<Person>, Double>();

	HashSet<String> ignoredActivities;
	HashMap<String,String> vehicleToPerson = new HashMap<String,String>();
	LinkedList<String> detailedOutput = new LinkedList<String>();

	private final String TELEPORTATION_MODE = "teleportation";

	private Population population;

	private HashMap<Id<Person>, AtomicInteger> populationCurrentLeg;

	private boolean fetchHourlyLinkFlows = false; //default is false - can be set to true.
	private boolean fetchHourlyLinkStucks = false;


	private HashSet<Id<Person>> highlightPersonOriginTime = new HashSet<Id<Person>>();
	private HashSet<Id<Person>> highlightPersonOriginGeography = new HashSet<Id<Person>>();

	private boolean roW = false;

	private boolean fetchHourlyLinkSpeeds;






	public void reset(final int iteration) {

	}

	public void initiateFlowsMap(){
		flows = new HashMap<Id<Link>, int[]>();
		for(Id<Link> linkId : network.getLinks().keySet()){
			flows.put(linkId, new int[numberOfSlots]);
		}
	}

	public void initiateTravelTimeMap(){
		travelTimes = new HashMap<Id<Link>, long[]>();
		for(Id<Link> linkId : network.getLinks().keySet()){
			travelTimes.put(linkId, new long[numberOfSlots]);
		}
	}

	public void initiateStucksMap(){
		stucks = new HashMap<Id<Link>, int[]>();
		for(Id<Link> linkId : network.getLinks().keySet()){
			stucks.put(linkId, new int[numberOfSlots]);
		}
	}

	public void setNetwork(Network network){
		this.network = network;
		if(fetchHourlyLinkFlows){
			initiateFlowsMap();
		}
	}
	public Network getNetwork() {
		return this.network;
	}

	public void setFetchHourlyLinkFlows(boolean fetchHourlyLinkFlows){
		this.fetchHourlyLinkFlows = fetchHourlyLinkFlows; 
		if(this.fetchHourlyLinkFlows && network != null){
			initiateFlowsMap();
		}
	}

	public void setFetchHourlyLinkStucks(boolean fetchHourlyLinkStucks){
		this.fetchHourlyLinkStucks = fetchHourlyLinkStucks; 
		if(this.fetchHourlyLinkStucks && network != null){
			initiateStucksMap();
		}
	}

	public void setFetchHourlyLinkSpeeds(boolean b) {
		this.fetchHourlyLinkSpeeds = true;
		if(this.fetchHourlyLinkStucks && network != null){
			initiateTravelTimeMap();
		}
	}
	public void setPopulation(Population population){
		this.population = population;
		populationCurrentLeg = new HashMap<Id<Person>, AtomicInteger>();
		for(Person person : this.population.getPersons().values()){
			populationCurrentLeg.put(person.getId(), new AtomicInteger()); 
		}
	}

	public void setAnalysedModes(List<String> analysedModes){
		this.ignoredActivities = new HashSet<String>();
		for(String mode : analysedModes){
			totalTravelTimes.put(mode, new AtomicDouble());
			totalHighlightTravelTimes.put(mode, new AtomicDouble());
			highlightTrips.put(mode, new AtomicInteger());
			this.ignoredActivities.add(mode + " interaction");
		}
	}

	public void writeDetailedOutput(String fileName){
		try {
			FileWriter writer = new FileWriter(fileName);
			writer.append("Person;Trip;Mode;StartTime;TravelTime;PlannedDuration;IsHighlightTrip\n");
			for(String s : detailedOutput){
				writer.append(s + "\n");
			}
			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	//	private String bestGuessVehicleId(String vehicleId){
	//		if(personLegStarts.containsKey(vehicleId)){
	//			return vehicleId;
	//		} else if(personLegStarts.containsKey(vehicleToPerson.get(vehicleId))){
	//			return vehicleToPerson.get(vehicleId);
	//		}
	//		return ":(";
	//	}

	@Override
	public void handleEvent(Event event) {
		if(event instanceof LinkEnterEvent){

			LinkEnterEvent e = (LinkEnterEvent) event;
			double now = e.getTime();
			if(now < endTime){
				int slot = timeToSlot(now);
				Id<Link> linkId = e.getLinkId();
				flows.get(linkId)[slot]++;
			}
			LinkEnterEvent prevEvent = entries.remove(e.getVehicleId());
			if(prevEvent != null) {
				processPrevEvent(prevEvent, e.getTime());
			}
			entries.put(e.getVehicleId(), e);

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
		} else if(event instanceof LinkLeaveEvent){
			// do nothing
		} else if(event instanceof ActivityStartEvent){
			// do nothing
		} else if(event instanceof ActivityEndEvent){
			// Do nothing
		} else if(event instanceof PersonStuckEvent) {
			PersonStuckEvent e = (PersonStuckEvent) event;
			double now = e.getTime();
			if(now < endTime){
				int slot = timeToSlot(now);
				Id<Link> linkId = e.getLinkId();
				stucks.get(linkId)[slot]++;
			}
		} else if(event instanceof VehicleEntersTrafficEvent){
			VehicleEntersTrafficEvent e = (VehicleEntersTrafficEvent) event;
			if(totalTravelTimes.containsKey(e.getNetworkMode())) {
				personLegStarts.put(e.getPersonId(), e.getTime());
				personLegModes.put(e.getPersonId(), e.getNetworkMode());
				if(isInHighlightArea(getCoord(e.getLinkId()))){
					highlightPersonOriginGeography.add(e.getPersonId());
				}
				if(isInHighlightTime(e.getTime()-1)){
					highlightPersonOriginTime.add(e.getPersonId());
				}
				print(e.getTime());
			}
		} else if(event instanceof VehicleLeavesTrafficEvent) {
			VehicleLeavesTrafficEvent e = (VehicleLeavesTrafficEvent) event;
			String mode = e.getNetworkMode();
			// It doesn't contain the TELEPORTATION_MODE and the TransportMode.walk - as such, these are ignored.
			if(totalTravelTimes.containsKey(mode)){
				double now = e.getTime();
				double then = personLegStarts.get(e.getPersonId());
				double TT = now - then;
				//	if(roW) {
				//		TT--;
				//	}
				double plannedDuration = getPlannedDuration(e.getPersonId(), mode);

				totalTravelTimes.get(mode).addAndGet(TT);

				boolean originWithinHighlightGeography = highlightPersonOriginGeography.contains(e.getPersonId());
				boolean originWithinHighlightTime = highlightPersonOriginTime.contains(e.getPersonId());
				highlightPersonOriginGeography.remove(e.getPersonId());
				highlightPersonOriginTime.remove(e.getPersonId());
				Coord coord = getCoord(e.getLinkId());

				boolean destinationWithinHighlightGeography = isInHighlightArea(coord);
				boolean destinationWithinHighlightTime = isInHighlightTime(now);

				boolean isHighlightTrip = isHighlightTrip(
						originWithinHighlightTime,
						destinationWithinHighlightTime,
						originWithinHighlightGeography,
						destinationWithinHighlightGeography);

				if(totalTravelTimes.containsKey(mode)){
					detailedOutput.add(e.getPersonId() + ";" + populationCurrentLeg.get(e.getPersonId()) + ";" +
							mode + ";" + then + ";" + TT + ";" + plannedDuration + ";" + (isHighlightTrip ? "1" : "0"));
				}


				if(isHighlightTrip){
					totalHighlightTravelTimes.get(mode).addAndGet(TT);
					highlightTrips.get(mode).incrementAndGet();
				}
				if(!everyTravelTime.containsKey(e.getPersonId())){
					everyTravelTime.put(e.getPersonId(), new HashMap<String,LinkedList<Double>>());
				}
				if(!everyTravelTime.get(e.getPersonId()).containsKey(mode)){
					everyTravelTime.get(e.getPersonId()).put(mode, new LinkedList<Double>());
				}
				everyTravelTime.get(e.getPersonId()).get(mode).addLast(TT);

				personLegStarts.remove(e.getPersonId());
				personLegModes.remove(e.getPersonId());


				LinkEnterEvent prevEvent = entries.remove(e.getVehicleId());
				if(prevEvent != null) {
					processPrevEvent(prevEvent, e.getTime());
				}

			}
			print(e.getTime());	
		}
	}


	private void processPrevEvent(LinkEnterEvent prevEvent, double time) {
		int timeSlot = timeToSlot(time);
		travelTimes.get(prevEvent.getLinkId())[timeSlot] += time - prevEvent.getTime();	
	}

	private Coord getCoord(Id<Link> linkId) {
		return network.getLinks().get(linkId).getCoord();
	}

	public static boolean isInHighlightTime(double now) {
		return now >= ConstructSpeedFlowsFromCopenhagen.highlightStartTime &&
				now <= ConstructSpeedFlowsFromCopenhagen.highlightEndTime;
	}

	public boolean isHighlightTrip(Leg leg){
		double originTime = leg.getDepartureTime().seconds();
		double destinationTime = originTime + leg.getTravelTime().seconds();
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

	private double getPlannedDuration(Id<Person> personId, String mode) {
		Person person = this.population.getPersons().get(personId);
		Plan plan = person.getSelectedPlan();
		AtomicInteger atomicIndex = populationCurrentLeg.get(personId);
		PlanElement pe = plan.getPlanElements().get(atomicIndex.incrementAndGet());
		while(!(pe instanceof Leg) || !((Leg) pe).getMode().equals(mode)){
			pe = plan.getPlanElements().get(atomicIndex.incrementAndGet());
		}
		Leg leg = (Leg) pe;
		if(!totalTravelTimes.containsKey(mode)){
			//Teleported, and thus not interesting.
			return leg.getTravelTime().seconds();
		} // else

		double freeFlowTravelTime = 0;
		if(mode.equals(TransportMode.bike)){
			double distance = leg.getRoute().getDistance();
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
		return freeFlowTravelTime;
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
		int timeSlot = (int) Math.floor(now / timeStepSize);
		if(timeSlot < numberOfSlots) {
			return timeSlot;
		} else {
			return numberOfSlots - 1;
		}
	}

	public void writeFlowSpeeds(String outFile) throws IOException{
		FileWriter writer = new FileWriter(outFile);
		System.out.println("Writing Speed flows...");
		writer.append("Link;Flow;Speed;TimeSlot\n");
		for(Id<Link> linkId : flows.keySet()){
			for(int i = 0; i < numberOfSlots; i++){
				int timeSlotId = i +1;
				int flow = flows.get(linkId)[i];
				double avgSpeed = -1;
				if(flow > 0) {
					long travelTime = travelTimes.get(linkId)[i];
					avgSpeed = network.getLinks().get(linkId).getLength() * flow /  travelTime;
					writer.append(linkId + ";" + flow + ";" + avgSpeed + ";" + timeSlotId + "\n");
				}
			}
		}
		writer.flush();
		writer.close();
		System.out.println("Done!");
	}

	public void writeAllFlows(String outDir) throws IOException{
		String outFile = outDir + "AllFlows.csv";
		String outFile2 = outDir + "LinkGeometry.csv";
		FileWriter writer = new FileWriter(outFile);
		FileWriter writer2 = new FileWriter(outFile2);
		writer.append("LinkId;ToHour;Flow\n");
		writer2.append("LinkId;FromX;FromY;ToX;ToY\n");
		for(Id<Link> linkId : flows.keySet()){
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
				if(flow > 0) {
					writer.append(linkId + ";" + toHour + ";" + flow + "\n");
				}
				toHour++;
			}
		}
		writer.flush();
		writer.close();
		writer2.flush();
		writer2.close();
	}

	public void writeAllStucks(String outDir) throws IOException{
		String outFile = outDir + "AllStucks.csv";
		FileWriter writer = new FileWriter(outFile);
		writer.append("LinkId;ToHour;Stuck\n");
		for(Id<Link> linkId : stucks.keySet()){
			int toHour = 1;
			for(int stuck : stucks.get(linkId)){
				if(stuck > 0) {
					writer.append(linkId + ";" + toHour + ";" + stuck + "\n");
				}
				toHour++;

			}
		}
		writer.flush();
		writer.close();
	}


	public void writeFlows(String outDir, int timeSlotId) throws IOException{
		String outFile = outDir + "flows" + (timeSlotId - 1) + "to" + timeSlotId + ".csv";
		FileWriter writer = new FileWriter(outFile);
		writer.append("Link;Flow\n");
		for(Id<Link> linkId : flows.keySet()){
			int flow = flows.get(linkId)[timeSlotId-1];
			writer.append(linkId + ";" + flow + "\n");
		}
		writer.flush();
		writer.close();
	}

	public boolean isRoW() {
		return roW;
	}

	public void setRoW(boolean roW) {
		this.roW = roW;
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
