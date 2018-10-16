package fastOrForcedToFollow;

import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.PriorityQueue;

public class Link{

	public int id;
	public double width;
	public double length;
	public int Psi;
	public PseudoLane[] psi;
	public PriorityQueue<CyclistQObject> outQ;
	public Link prevLink;
	public Link nextLink;
	public LinkedList<Double>[] speedReports;
	public LinkedList<Double> densityReport = new LinkedList<Double>();
	public int inFlowCounter = 0;
	public int outFlowCounter = 0;
	public int storageCapacity;
	public double occupiedSpace = 0;
	public double totalLaneLength;
	public LinkedList<Double[]> speedTimeReport = new LinkedList<Double[]>();
	public LinkedList<Double[]> outputTimeReport = new LinkedList<Double[]>(); 
	public double tWakeUp = 0;  // The earliest possible time that the link can be used again (used when storage capacity is met).



	@SuppressWarnings("unchecked")
	Link(int id, double width, double length) throws InstantiationException, IllegalAccessException{
		this.id = id;
		this.width = width;
		this.length = length;
		this.Psi = 1 + (int) Math.floor((width-Runner.deadSpace)/Runner.omega);
		psi = Runner.ltm.createPseudoLanes(this, Psi, length);
		speedReports = new LinkedList[Psi];
		for(int i = 0; i < Psi; i++){
			speedReports[i] = new LinkedList<Double>();
		}
		outQ = (PriorityQueue<CyclistQObject>) Runner.priorityQueueClassForLinks.newInstance();
		this.totalLaneLength = this.length * this.Psi * Runner.capacityMultiplier;
	}

	public double getLength(){
		return length;
	}

	public void setPrevLink(Link prevLink){
		this.prevLink = prevLink;
		prevLink.nextLink = this;
	}
	public void setNextLink(Link nextLink){
		this.nextLink = nextLink;
		nextLink.prevLink = this;
	}

	/*
	public void handleQ() throws IOException{
		int counter = 1;
		while(isRelevant()){
			CyclistQObject cyclist = outQ.poll();
			cyclist.enterCyclist();
			if(counter == Runner.maxCyclistsPerTau){
				break;
			} else {
				counter++;
			}
		}
	}
	 */

	public void handleQOnNotification(LinkQObject lqo) throws IOException{
		CyclistQObject cqo = outQ.peek();
		if(cqo.cyclist.route.isEmpty()){	// The cyclist has reached his/her destination.
			this.outQ.remove();
			this.outFlowCounter++;
			cqo.cyclist.terminateCyclist(lqo);
			if(!Runner.waitingTimeCountsOnNextLink){
				cqo.cyclist.reportSpeed(this.length, lqo.time);
			}
			reportOutputTime(lqo.time);
			reportSpeedTime(lqo.time, cqo.cyclist.speedReport.getLast()[2]);
			sendNotificationForNextInQ(cqo.cyclist);
		}  else {	
			Link nextLink = cqo.cyclist.route.peek();
			if(cqo.cyclist.advanceCyclist(lqo)){
				this.outQ.remove();
				this.outFlowCounter++;
				if(!Runner.waitingTimeCountsOnNextLink){
					if(this.id >= 0){
						cqo.cyclist.reportSpeed(length, lqo.time);
						reportOutputTime(lqo.time);
						reportSpeedTime(lqo.time, cqo.cyclist.speedReport.getLast()[2]);
					}
					cqo.cyclist.initialiseNewSpeedReportElement(nextLink.id, lqo.time);
				}
				sendNotificationForNextInQ(cqo.cyclist);
			} else { //It was not possible to advance the cyclist due to congestion.
				sendNotificationDueToDelay(cqo.cyclist, nextLink);
			}
		}
	}

	private void sendNotificationForNextInQ(Cyclist cyclist){
		if(!outQ.isEmpty()){
			double notificationTime = Math.max(this.outQ.peek().time, this.tWakeUp);
			cyclist.sendNotification(this.id, notificationTime);
			this.tWakeUp = notificationTime;
		} 
	}

	private void sendNotificationDueToDelay(Cyclist cyclist, Link nextLink){
		if(!outQ.isEmpty()){
			double notificationTime = Math.max(nextLink.outQ.peek().time, nextLink.tWakeUp);
			cyclist.sendNotification(this.id, notificationTime);
			this.tWakeUp = notificationTime;
		} 
	}

	public void reportSpeedTime(double t, double speed){
		speedTimeReport.add(new Double[]{t, speed});
	}

	public void reportOutputTime(double t){
		outputTimeReport.add(new Double[]{t, (double) this.outFlowCounter});
	}

	public boolean isRelevant(){
		return !outQ.isEmpty() && Runner.t >= outQ.peek().time;
	}


	public void killCyclist(String baseDir) throws IOException{
		Cyclist cyclist = outQ.poll().cyclist;
		cyclist.exportSpeeds(baseDir);
	}

	/*public void setSpeedToZeroForStuckCyclists(){
		//This is not easily done when using a priorityQueue - thus using setSpeedToZeroForStuckCyclists is
		// discouraged using this implementation -- which is otherwise the better one.
		PriorityQueue<CyclistQObject> tempQ = new PriorityQueue<CyclistQObject>();
		while(!outQ.isEmpty()){
			double qT = outQ.peek().time;
			if( qT <= Runner.t && qT > 0){
				CyclistQObject cqo = outQ.poll();
				cqo.cyclist.speed = 0;
				tempQ.add(cqo);
			} else {
				break;
			}
		}
		while(!tempQ.isEmpty())	{
			outQ.add(tempQ.poll());
		}
	}*/



	/* Not necessary when having vMax as a function.

	public void updatePseudoLaneSpeeds(){
		for(int i = 0; i < Psi; i++){
			PseudoLane pseudoLane = psi[i];
			double D = pseudoLane.length/pseudoLane.vSet - Runner.tau;
			if(D <= 0){
				pseudoLane.vSet = vMax;
			} else {
				pseudoLane.vSet = pseudoLane.length / D;
			}
			speedReports[i].add(pseudoLane.vSet);
		}
	}
	 */

	public void exportFlows(String baseDir) throws IOException{
		FileWriter writer = new FileWriter(baseDir + "/flowsLink_" + Runner.ltm.getClass().getName() + "_" +
				id + "_" + Runner.N + "Persons_" + Runner.circuitString + ".csv");
		writer.append("InFlow;OutFlow\n");
		writer.append(inFlowCounter + ";" + outFlowCounter + "\n");
		writer.flush();
		writer.close();
	}

	public void exportSpeedTimes(String baseDir) throws IOException{
		FileWriter writer = new FileWriter(baseDir + "/speedTimesLink_" + Runner.ltm.getClass().getName() + "_" +
				id + "_" + Runner.N + "Persons_" + Runner.circuitString + ".csv");
		writer.append("Time;Speed\n");
		while(!this.speedTimeReport.isEmpty()){
			Double[] element = speedTimeReport.pollFirst();
			writer.append(element[0] + ";" + element[1] + "\n");
		}
		writer.flush();
		writer.close();
	}

	public void exportOutputTimes(String baseDir) throws IOException{
		FileWriter writer = new FileWriter(baseDir + "/outputTimesLink_" + Runner.ltm.getClass().getName() + "_" +
				id + "_" + Runner.N + "Persons_" + Runner.circuitString + ".csv");
		writer.append("Time;Output\n");
		while(!this.outputTimeReport.isEmpty()){
			Double[] element = outputTimeReport.pollFirst();
			writer.append(element[0] + ";" + element[1] + "\n");
		}
		writer.flush();
		writer.close();
	}

	public void exportDensities(String baseDir) throws IOException{
		FileWriter writer = new FileWriter(baseDir + "/densitiesLink_" + Runner.ltm.getClass().getName() + "_" +
				id + "_" + Runner.N + "Persons_" + Runner.circuitString + ".csv");
		writer.append("Density\n");
		while(!densityReport.isEmpty()){
			writer.append(String.valueOf(densityReport.pollFirst()) + "\n");
		}
		writer.flush();
		writer.close();
	}

	public void exportSpeeds(String baseDir) throws IOException{
		Runner.createFolderIfNeeded(baseDir);
		FileWriter writer = new FileWriter(baseDir + "/speedsOfLinks_" + Runner.ltm.getClass().getName() + "_" +
				id + "_" + Runner.N + "Persons_" + Runner.circuitString + ".csv");
		for(int i = 0; i < Psi; i++){
			writer.append("Speed" + i + ";");
		}
		writer.append("\n");
		while(!speedReports[Psi-1].isEmpty()){
			for(int i = 0; i < Psi; i++){
				writer.append(speedReports[i].pollFirst() + ";");
			}
			writer.append("\n");
		}
		writer.flush();
		writer.close();
	}

	public int calculateStorageCapacity(){
		return (int) Math.floor(length * Psi / Runner.l);
	}


}
