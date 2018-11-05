package fastOrForcedToFollow;

import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.PriorityQueue;

/**
 * 
 * @author mpaulsen
 */
public class Link{

	/**
	 * Density report containing the densities that have occured during the simulation.
	 */
	private LinkedList<Double> densityReport = new LinkedList<Double>();

	/**
	 * The id of the link.
	 */
	private int id;

	/**
	 * Counts the total number of cyclists that have entered the link.
	 */
	private int inFlowCounter = 0;

	/**
	 * The length of the link.
	 */
	private double length;

	/**
	 * The total pseudolane distance (buffer distance) occupied of the cyclists currently on the link.
	 */
	private double occupiedSpace = 0;

	/**
	 * Counts the total number of cyclists that have left the link.
	 */
	private int outFlowCounter = 0;

	/**
	 * The outflow time report containing the speed and time of every cyclist leaving the link.
	 */
	private LinkedList<Double[]> outflowTimeReport = new LinkedList<Double[]>();

	/**
	 * The Q containing the CQO's for the cyclists that have entered the link but not yet left it.
	 */
	private PriorityQueue<CyclistQObject> outQ;

	/**
	 * The array containing all <code>Psi</code> pseudolanes of the link.
	 */
	private PseudoLane[] psi;

	/**
	 * The number of <code>pseudolanes</code> the link has.
	 */
	private int Psi;

	/**
	 * Speed report containing the speeds of the cyclists that have traversed the link.
	 */
	private LinkedList<Double>[] speedReports;

	/**
	 * The speed time report containing the speed and time of every cyclist leaving the link.
	 */
	private LinkedList<Double[]> speedTimeReport = new LinkedList<Double[]>();

	/**
	 * The total pseudolane distance, i.e. the product between the length and number of pseudolanes.
	 */
	private final double totalLaneLength; 

	/**
	 * The earliest possible time that the link can potentially handle traffic (entering or leaving).
	 */
	private double tWakeUp = 0;


	@SuppressWarnings("unchecked")
	Link(int id, double width, double length) throws InstantiationException, IllegalAccessException{
		this.id = id;
		this.length = length;
		this.Psi = 1 + (int) Math.floor((width-Runner.deadSpace)/Runner.omega);
		psi = createPseudoLanes();
		speedReports = new LinkedList[Psi];
		for(int i = 0; i < Psi; i++){
			speedReports[i] = new LinkedList<Double>();
		}
		outQ = (PriorityQueue<CyclistQObject>) Runner.priorityQueueClassForLinks.newInstance();
		this.totalLaneLength = this.length * this.Psi * Runner.capacityMultiplier;
	}

	/**
	 * @return The array of PseudoLanes to be created
	 */
	private PseudoLane[] createPseudoLanes(){
		PseudoLane[] psi = new PseudoLane[Psi];
		for(int i = 0; i < Psi; i++){
			psi[i] = new PseudoLane(length, this);
		}
		return psi;
	}

	public void exportDensities(String baseDir) throws IOException{
		FileWriter writer = new FileWriter(baseDir + "/densitiesLink_" + id + "_" + Runner.N + "Persons.csv");
		writer.append("Density\n");
		while(!densityReport.isEmpty()){
			writer.append(String.valueOf(densityReport.pollFirst()) + "\n");
		}
		writer.flush();
		writer.close();
	}

	public void exportFlows(String baseDir) throws IOException{
		FileWriter writer = new FileWriter(baseDir + "/flowsLink_" + id + "_" + Runner.N + "Persons.csv");
		writer.append("InFlow;OutFlow\n");
		writer.append(inFlowCounter + ";" + outFlowCounter + "\n");
		writer.flush();
		writer.close();
	}

	public void exportOutputTimes(String baseDir) throws IOException{
		FileWriter writer = new FileWriter(baseDir + "/outputTimesLink_" +	id + "_" + Runner.N + "Persons.csv");
		writer.append("Time;Output\n");
		while(!this.outflowTimeReport.isEmpty()){
			Double[] element = outflowTimeReport.pollFirst();
			writer.append(element[0] + ";" + element[1] + "\n");
		}
		writer.flush();
		writer.close();
	}

	public void exportSpeeds(String baseDir) throws IOException{
		ToolBox.createFolderIfNeeded(baseDir);
		FileWriter writer = new FileWriter(baseDir + "/speedsOfLinks_" + id + "_" + Runner.N + "Persons.csv");
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

	public void exportSpeedTimes(String baseDir) throws IOException{
		FileWriter writer = new FileWriter(baseDir + "/speedTimesLink_" + id + "_" + Runner.N + "Persons.csv");
		writer.append("Time;Speed\n");
		while(!this.speedTimeReport.isEmpty()){
			Double[] element = speedTimeReport.pollFirst();
			writer.append(element[0] + ";" + element[1] + "\n");
		}
		writer.flush();
		writer.close();
	}

	/**
	 * @return The density report storing the density at every time of the simulation.
	 */
	public LinkedList<Double> getDensityReport(){
		return densityReport;
	}

	/**
	 * @return The integer id of the link.
	 */
	public int getId(){
		return id;
	}

	public double getLength(){
		return length;
	}

	/**
	 * @return The number of <code>pseudolanes</code>.
	 */
	public int getNumberOfPseudoLanes(){
		return Psi;
	}

	/**
	 * @return The occupied space (sum of safetydistances of <code>cyclists</code> currently on the <code>link</code>.
	 */
	public double getOccupiedSpace(){
		return occupiedSpace;
	}

	/** 
	 * @return The outQ belonging to the link.
	 */
	public PriorityQueue<CyclistQObject> getOutQ(){
		return outQ;
	}

	/**
	 * Gets a <code>pseudolane</code> from the <code>link</code>.
	 * 
	 * @param i the index where 0 is the rightmost <code>pseudolane</code>, and <code>Psi</code> - 1 is the leftmost.
	 * 
	 * @return The i'th <code>pseudolane</code> from the right (0 being the rightmost).
	 */
	public PseudoLane getPseudoLane(int i){
		return psi[i];
	}
	/**
	 * @return The total lane length, defined as the product of the length and the number of pseudolanes.
	 */
	public double getTotalLaneLength(){
		return totalLaneLength;
	}

	/**
	 * @return The earliest possible time that the link can potentially handle traffic (entering or leaving).
	 */
	public double getWakeUpTime(){
		return tWakeUp;
	}

	public void handleQOnNotification(double time) throws IOException{
		if(!this.outQ.isEmpty()){ //TODO This can happen if multiple shorttermnotifications happen for same link
			Cyclist cyclist = this.outQ.peek().getCyclist();
			if(cyclist.getRoute().isEmpty()){	//The cyclist has reached his/her destination.
				this.outQ.remove();
				this.outFlowCounter++;
				cyclist.terminateCyclist(this.id);
				cyclist.reportSpeed(this.length, time);
				reportOutputTime(time);
				reportSpeedTime(time, cyclist.getSpeedReport().getLast()[2]);
				sendNotificationBasedOnNextInQ();
			}  else {	
				Link nextLink = cyclist.getRoute().peek();
				if(cyclist.advanceCyclist(this.id, time)){ // Checking whether it is possible to advance the cyclist
					// ... and does so if possible.

					if(this.id != Runner.sourceLink.getId()){
						cyclist.reportSpeed(length, time);
						reportOutputTime(time);
						reportSpeedTime(time, cyclist.getSpeedReport().getLast()[2]);
					}
					cyclist.initialiseNewSpeedReportElement(nextLink.id, time);
					sendNotificationBasedOnNextInQ();
				} else { //It was not possible to advance the cyclist due to congestion.
					sendNotificationDueToDelay(nextLink);
				}
			}
		}
	}

	/**
	 * Increments the in-flow counter of the link by 1.
	 */
	public void incrementInFlowCounter(){
		inFlowCounter++;
	}

	/**
	 * Increments the out-flow counter of the link by 1.
	 */
	public void incrementOutFlowCounter(){
		outFlowCounter++;
	}

	public boolean isRelevant(){
		return !outQ.isEmpty() && Runner.t >= outQ.peek().getTime();
	}


	public void reportOutputTime(double t){
		outflowTimeReport.add(new Double[]{t, (double) this.outFlowCounter});
	}


	public void reportSpeedTime(double t, double speed){
		speedTimeReport.add(new Double[]{t, speed});
	}


	private void sendNotificationDueToDelay(Link nextLink){
		if(!outQ.isEmpty()){
			double notificationTime = Math.max(nextLink.outQ.peek().getTime(), nextLink.tWakeUp);
			sendNotification(notificationTime);
			this.tWakeUp = notificationTime;
		} 
	}

	private void sendNotificationBasedOnNextInQ(){
		if(!outQ.isEmpty()){
			double notificationTime = Math.max(this.outQ.peek().getTime(), this.tWakeUp);
			sendNotification(notificationTime);
			this.tWakeUp = notificationTime;
		} 
		
	}


	public void sendShortTermNotification(int linkId, double tEnd){
		Runner.shortTermPriorityQueue.add(new LinkQObject(tEnd, linkId));
	}

	public void sendNotification(double tEnd){
		if(tEnd < Runner.T){
			if(tEnd <= Runner.t){
				sendShortTermNotification(this.id, tEnd);
			} else {
				int timeSlot  = ((int) tEnd) / ((int) Runner.timeStep) + 1;
				if( !Runner.notificationArray[timeSlot].containsKey(this.id) || 
						tEnd < Runner.notificationArray[timeSlot].get(this.id).time ){
					Runner.tieBreaker++;
					Runner.notificationArray[timeSlot].put(this.id, new  NotificationArrayObject(tEnd, Runner.tieBreaker));	
				}
			}
		}
	}


	/**
	 * @param tWakeUp The earliest possible time that the link can potentially handle traffic (entering or leaving).
	 */
	public void setWakeUpTime(double tWakeUp){
		this.tWakeUp = tWakeUp;
	}

	/**
	 * @param length in metres by which the occupied space will be supplemented.
	 */
	public void supplementOccupiedSpace(double length){
		occupiedSpace += length;
	}


}
