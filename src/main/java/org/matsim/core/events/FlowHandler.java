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

package org.matsim.core.events;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.events.handler.BasicEventHandler;


public class FlowHandler implements BasicEventHandler {

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



	public void reset(final int iteration) {

	}

	public void setNetwork(Network network){
		this.network = network;
	}

	@Override
	public void handleEvent(Event event) {
		if(event instanceof LinkEnterEvent){
			LinkEnterEvent e = (LinkEnterEvent) event;
			double now = event.getTime();
			String linkId = e.getLinkId().toString();
			String personId = e.getVehicleId().toString();
			if(!flows.containsKey(linkId)){
				flows.put(linkId, new int[numberOfSlots]);
				LinkedList<Double>[] newLinkedListArray = new LinkedList[numberOfSlots];
				for(int i = 0; i < newLinkedListArray.length; i++){
					newLinkedListArray[i] = new LinkedList<Double>();
				}
				speeds.put(linkId, newLinkedListArray);
				entryTimes.put(linkId, new HashMap<String, Double>());
			}
			entryTimes.get(linkId).put(personId, now);
			print(now);
		} else if(event instanceof LinkLeaveEvent){

			LinkLeaveEvent e = (LinkLeaveEvent) event;
			double now = event.getTime();
			int slot = timeToSlot(now);
			String linkId = e.getLinkId().toString();
			String personId = e.getVehicleId().toString();
			if(entryTimes.containsKey(linkId) && entryTimes.get(linkId).containsKey(personId)){
				flows.get(linkId)[slot]++;
				double speed = network.getLinks().get(linkId).getLength() / (now - entryTimes.get(linkId).get(personId));
				speeds.get(linkId)[slot].add(speed);

				//To safe memory
				entryTimes.get(linkId).remove(personId);
			}
		} else if(event instanceof PersonDepartureEvent){
			PersonDepartureEvent e = (PersonDepartureEvent) event;
			if(e.getLegMode().equals(TransportMode.access_walk)){
				double now = event.getTime();
				personLegStarts.put(e.getPersonId().toString(), now);
			}
		} else if(event instanceof PersonArrivalEvent){
			PersonArrivalEvent e = (PersonArrivalEvent) event;
			if(e.getLegMode().equals(TransportMode.egress_walk)){
				double now = event.getTime();
				String personId = e.getPersonId().toString();
				double then = personLegStarts.get(personId);
				if(now > then){
					totalTravelTime += now - then -1;
				}
							//To safe memory
				personLegStarts.remove(personId);
			}
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
