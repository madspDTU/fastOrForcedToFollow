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

import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.network.NetworkUtils;
import org.matsim.run.CreateFDs;



public class FDEventsHandler implements BasicEventHandler {

	static Network network;


	final static double endTime = CreateFDs.endTime;
	final static int numberOfSlots = (int) Math.ceil(endTime/ CreateFDs.timeStepSize) +1 ;

	HashMap<Integer, int[]> flows = new HashMap<Integer, int[]>();
	HashMap<Integer, LinkedList<Double>[]> speeds = new HashMap<Integer, LinkedList<Double>[]>();
	HashMap<Integer, HashMap<Integer, Double>> entryTimes = new HashMap<Integer, HashMap<Integer,Double>>();



	public void reset(final int iteration) {

	}

	public void readNetwork(String networkString){
		network = NetworkUtils.readNetwork(networkString);
	}

	@Override
	public void handleEvent(Event event) {
		if(event instanceof LinkEnterEvent){
			LinkEnterEvent e = (LinkEnterEvent) event;
			double now = event.getTime();
			int linkId = Integer.valueOf(e.getLinkId().toString().split("_")[0]);
			if(linkId == 2 || linkId == 3){
				int personId = Integer.valueOf(e.getVehicleId().toString().split("_")[0]);
				if(!flows.containsKey(linkId)){
					flows.put(linkId, new int[numberOfSlots]);
					LinkedList<Double>[] newLinkedListArray = new LinkedList[numberOfSlots];
					for(int i = 0; i < newLinkedListArray.length; i++){
						newLinkedListArray[i] = new LinkedList<Double>();
					}
					speeds.put(linkId, newLinkedListArray);
					entryTimes.put(linkId, new HashMap<Integer, Double>());
				}
				entryTimes.get(linkId).put(personId, now);
			}
		} else if(event instanceof LinkLeaveEvent){

			LinkLeaveEvent e = (LinkLeaveEvent) event;
			double now = event.getTime();
			int slot = timeToSlot(now);
			int linkId = Integer.valueOf(e.getLinkId().toString().split("_")[0]);
			if(linkId == 2 || linkId == 3){
				int personId = Integer.valueOf(e.getVehicleId().toString().split("_")[0]);
				if(entryTimes.containsKey(linkId) && entryTimes.get(linkId).containsKey(personId)){
					flows.get(linkId)[slot]++;
					double speed = network.getLinks().get(e.getLinkId()).getLength() / (now - entryTimes.get(linkId).get(personId));
					speeds.get(linkId)[slot].add(speed);

					//To safe memory
					entryTimes.get(linkId).remove(personId);
				}
			}
		}

	}

	private int timeToSlot(double now){
		return (int) Math.floor(now / CreateFDs.timeStepSize);
	}

	public void writeFlowSpeeds(String outFile) throws IOException{
		FileWriter writer = new FileWriter(outFile);

		writer.append("Link;Flow;Speed;TimeSlot\n");
		for(int linkId : flows.keySet()){
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
}