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
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.events.handler.BasicEventHandler;


public class ExtractTotalTravelTimes implements BasicEventHandler {

	Network network;

	HashSet<String> interestingLegs = new HashSet<String>();
	int interestingLegsMaxSize = 400;
	double currentTime = 0;
	int printCounter = 0;
	final static int printHowOften = 60*15;
	LinkedHashMap<String, AtomicLong> totalTravelTimes = new LinkedHashMap<String, AtomicLong>();
	HashMap<String, Double> personLegStarts = new HashMap<String, Double>();
	HashMap<String, String> personToMode = new HashMap<String, String>();

	private double interestingBufferTime = 5 *3600;



	public void reset(final int iteration) {
		this.currentTime = 0;
		this.printCounter = 0;
		this.totalTravelTimes = new LinkedHashMap<String, AtomicLong>();
		this.personLegStarts = new HashMap<String, Double>();
		this.personToMode = new HashMap<String, String>();
	}

	public void setNetwork(Network network){
		this.network = network;
	}

	public void setAnalysedModes(List<String> analysedModes){
		for(String mode : analysedModes){
			totalTravelTimes.put(mode, new AtomicLong(0));
		}
	}

	private void incrementModeTravelTime(String mode, int inc){
		totalTravelTimes.get(mode).addAndGet(inc);
	}


	@Override
	public void handleEvent(Event event) {
		if(interestingLegs.size() < interestingLegsMaxSize){
			if(event instanceof PersonDepartureEvent){
				PersonDepartureEvent e = (PersonDepartureEvent) event;
				String personId = e.getPersonId().toString();
				if(e.getLegMode().equals(TransportMode.access_walk)){
					double now = e.getTime();
					personLegStarts.put(personId, now);
				}
			} else if(event instanceof ActivityStartEvent){
				ActivityStartEvent e = (ActivityStartEvent) event;
				String personId = e.getPersonId().toString();
				String actType = e.getActType();
				if(actType.contains("interaction")){
					String mode = actType.split(" ")[0];
					personToMode.put(personId, mode);
				}
			}	else if(event instanceof PersonArrivalEvent){
				PersonArrivalEvent e = (PersonArrivalEvent) event;
				String personId = e.getPersonId().toString();
				if(personLegStarts.containsKey(personId)){
					if(e.getLegMode().equals(TransportMode.egress_walk)){
						double now = e.getTime();
						double then = personLegStarts.get(personId);
						String mode = personToMode.get(personId);
						if(now > then){
							incrementModeTravelTime(mode, (int) (now - then - 1) );
							if(now > then + interestingBufferTime ){
								interestingLegs.add(personId);
							}
						}
						print(now);
					}
				}
			}
		}
	}

	public HashSet<String> getInterestingLegs(){
		return interestingLegs;
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


	public void writeTotalTravelTimes(String outDir) throws IOException{
		String outFile = outDir + "/TotalTravelTimes.csv";
		FileWriter writer = new FileWriter(outFile);
		int i = 1;
		for(String mode : totalTravelTimes.keySet()){
			writer.append(mode);
			if(i < totalTravelTimes.size()){
				writer.append(";");	
			}
			i++;
		}
		writer.append("\n");
		i = 1;
		for(AtomicLong d : totalTravelTimes.values()){
			writer.append(String.valueOf(d.get()));
			i++;
			if(i > totalTravelTimes.size()){
				writer.append(";");	
			}
		}
		writer.flush();
		writer.close();
	}

	public LinkedHashMap<String, Double> getTotalTravelTimes(){
		LinkedHashMap<String, Double> output = new LinkedHashMap<String, Double>();
		for(String mode : totalTravelTimes.keySet()){
			output.put(mode, totalTravelTimes.get(mode).doubleValue());
		}
		return output;
	}

}
