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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.events.handler.BasicEventHandler;


public class MultiModalBicycleDoorToDoorHandler implements BasicEventHandler {

	Network network;

	final static double timeStepSize = 3600;
	final static double endTime = 32*3600;
	final static int numberOfSlots = (int) Math.ceil(endTime/ timeStepSize) +1 ;
	static double currentTime = 0;
	static int printCounter = 0;
	final static int printHowOften = 60*15;
	public double totalTravelTime = 0;

	HashMap<String, int[]> flows = new HashMap<String, int[]>();
	HashMap<String, LinkedList<Double>[]> speeds = new HashMap<String, LinkedList<Double>[]>();
	HashMap<String, HashMap<String, Double>> entryTimes = new HashMap<String, HashMap<String,Double>>();
	HashMap<String, Double> personLegStarts = new HashMap<String, Double>();

	HashSet<String> ignoredModes;
	List<String> analysedModes;
	HashMap<String,String> vehicleToPerson = new HashMap<String,String>();



	public void reset(final int iteration) {

	}

	public void setNetwork(Network network){
		this.network = network;
	}

	public void setIgnoredModes(List<String> ignoredModes){
		this.ignoredModes = new HashSet<String>();
		for(String s : ignoredModes){
			this.ignoredModes.add(s + " interaction");
		}
	}
	public void setAnalysedModes(List<String> analysedModes){
		this.analysedModes = analysedModes;
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
			LinkEnterEvent e = (LinkEnterEvent) event;
			String vehicleId = bestGuessVehicleId(e.getVehicleId().toString());
			if(personLegStarts.containsKey(vehicleId)){
				double now = e.getTime();
				String linkId = e.getLinkId().toString();
				if(!flows.containsKey(linkId)){
					flows.put(linkId, new int[numberOfSlots]);
					LinkedList<Double>[] newLinkedListArray = new LinkedList[numberOfSlots];
					for(int i = 0; i < newLinkedListArray.length; i++){
						newLinkedListArray[i] = new LinkedList<Double>();
					}
					speeds.put(linkId, newLinkedListArray);
					entryTimes.put(linkId, new HashMap<String, Double>());
				}
				entryTimes.get(linkId).put(vehicleId, now);
				print(now);
			} 
		} else if(event instanceof LinkLeaveEvent){
			LinkLeaveEvent e = (LinkLeaveEvent) event;
			String vehicleId = bestGuessVehicleId(e.getVehicleId().toString());
			if(personLegStarts.containsKey(vehicleId)){
				double now = e.getTime();
				int slot = timeToSlot(now);
				String linkId = e.getLinkId().toString();
				if(entryTimes.containsKey(linkId) && entryTimes.get(linkId).containsKey(vehicleId)){
					flows.get(linkId)[slot]++;
					double speed = network.getLinks().get(Id.createLinkId(linkId)).getLength() / 
							(now - entryTimes.get(linkId).get(vehicleId));
					speeds.get(linkId)[slot].add(speed);

					//To safe memory
					entryTimes.get(linkId).remove(vehicleId);
				}
			} 
		} else if(event instanceof PersonDepartureEvent){
			PersonDepartureEvent e = (PersonDepartureEvent) event;
			if(e.getLegMode().equals(TransportMode.access_walk)){
				double now = e.getTime();
				personLegStarts.put(e.getPersonId().toString(), now);
				createVehicleKeys(e.getPersonId().toString());
			}
		} else if(event instanceof ActivityStartEvent){
			ActivityStartEvent e = (ActivityStartEvent) event;
			String vehicleId = bestGuessVehicleId(e.getPersonId().toString());
			if(personLegStarts.containsKey(vehicleId) && ignoredModes.contains(e.getActType())){
				personLegStarts.remove(vehicleId);
			}
		}	else if(event instanceof PersonArrivalEvent){
			PersonArrivalEvent e = (PersonArrivalEvent) event;
			String vehicleId = bestGuessVehicleId(e.getPersonId().toString());
			if(personLegStarts.containsKey(vehicleId)){
				if(e.getLegMode().equals(TransportMode.egress_walk)){
					double now = e.getTime();
					double then = personLegStarts.get(vehicleId);
					if(now > then){
						totalTravelTime += now - then -1;
					}
				}
			}
		}
	}

	private void createVehicleKeys(String personId) {
		for(String mode : analysedModes){
			vehicleToPerson.put(personId + "_" + mode, personId);
		}
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
