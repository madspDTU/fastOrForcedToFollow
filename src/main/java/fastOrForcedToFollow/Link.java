package fastOrForcedToFollow;

import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.PriorityQueue;

import org.matsim.core.mobsim.qsim.qnetsimengine.QCycleAsVehicle;

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
	private String id;

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
	public Link(String id, double width, double length) throws InstantiationException, IllegalAccessException{
		this.id = id;
		this.length = length;
		this.Psi = 1 + (int) Math.floor((width-Runner.deadSpace)/Runner.omega);
		psi = createPseudoLanes();
		speedReports = new LinkedList[Psi];
		for(int i = 0; i < Psi; i++){
			speedReports[i] = new LinkedList<Double>();
		}
		outQ = new PriorityQueue<CyclistQObject>();
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
	public String getId(){
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



	public void finishCyclist(QCycleAsVehicle qCyc){
		Cyclist cyclist = qCyc.getCyclist();
		Link previousLink = cyclist.getCurrentLink();
		previousLink.outQ.remove();
		previousLink.outFlowCounter++;
		previousLink.reduceOccupiedSpace(cyclist, cyclist.getSpeed());
		cyclist.reportSpeed(previousLink.length);
		reportOutputTime(cyclist.getTEarliestExit());
		reportSpeedTime(cyclist.getTEarliestExit(), cyclist.getSpeedReport().getLast()[2]);
		cyclist.setCurrentLink(null);
	}

	public void advanceCyclist(QCycleAsVehicle qCyc){
		Cyclist cyclist = qCyc.getCyclist();
		PseudoLane pseudoLane = cyclist.selectPseudoLane(this); 
		double vTilde = cyclist.getVMax(pseudoLane);
		if(cyclist.speedFitsOnLink(vTilde, this)){
			Link previousLink = cyclist.getCurrentLink();
			previousLink.incrementOutFlowCounter();
			previousLink.getOutQ().remove();
			previousLink.reduceOccupiedSpace( cyclist, cyclist.getSpeed());
			double tLeave = Double.max(pseudoLane.tReady, cyclist.getTEarliestExit());

			previousLink.setWakeUpTime(tLeave);


			if(previousLink.getId() != Runner.sourceLink.getId()){
				cyclist.reportSpeed(previousLink.getLength(), tLeave);
				previousLink.reportOutputTime(tLeave);
				previousLink.reportSpeedTime(tLeave, cyclist.getSpeedReport().getLast()[2]);
			}
			cyclist.initialiseNewSpeedReportElement(this.getId(), tLeave);	



			cyclist.setSpeed(vTilde);
			cyclist.setTStart(tLeave);
			cyclist.setTEarliestExit(tLeave + this.length/vTilde);
			this.increaseOccupiedSpace(cyclist, vTilde);
			pseudoLane.updateTs(vTilde, tLeave);
			this.incrementInFlowCounter();
			outQ.add(new CyclistQObject(qCyc));
			cyclist.setCurrentLink(this);
		} else {
			System.err.println("Something is terribly wrong");
		}
	}



	/**
	 * Reduces the occupied space of link <code>linkId</code> by the safety distance corresponding to <code>speed</code>
	 * 
	 * @param linkId of the link that will have its space reduced.
	 * 
	 * @param speed on which the safety distance will be based.
	 */
	void reduceOccupiedSpace(Cyclist cyclist, double speed){
		this.supplementOccupiedSpace(-cyclist.getSafetyBufferDistance(speed));
	}

	void increaseOccupiedSpace(Cyclist cyclist, double speed){
		this.supplementOccupiedSpace(cyclist.getSafetyBufferDistance(speed));
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
		return !outQ.isEmpty() && Runner.t >= outQ.peek().getCyclist().getTEarliestExit();
	}


	public void reportOutputTime(double tLeave){
		outflowTimeReport.add(new Double[]{tLeave, (double) this.outFlowCounter});
	}


	public void reportSpeedTime(double t, double speed){
		speedTimeReport.add(new Double[]{t, speed});
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


	public void processLink(double now){
		boolean linkFullyProcessed = false;
		while(!linkFullyProcessed){
			if(this.getOutQ().isEmpty()){
				linkFullyProcessed = true;
			} else {
				QCycleAsVehicle cqo = this.getOutQ().peek().getQCycle();
				Cyclist cyclist = cqo.getCyclist();
				if(cyclist.getTEarliestExit() > now){
					linkFullyProcessed = true;
				} else {
					if(cyclist.getRoute().isEmpty()){
						this.finishCyclist(cqo);
					} else {
						Link nextLink = cyclist.getRoute().peek();
						if(nextLink.cyclistFitsOnLink(cyclist)){
							this.advanceCyclist(cqo);	
						} else {
							cyclist.setTEarliestExit(nextLink.getOutQ().peek().getCyclist().getTEarliestExit());
							linkFullyProcessed = true;
						}
					}
				} 
			} 
		}
	}


	public boolean cyclistFitsOnLink(Cyclist cyclist){
		PseudoLane pseudoLane = cyclist.selectPseudoLane(this);
		double vTilde = cyclist.getVMax(pseudoLane);
		return cyclist.speedFitsOnLink(vTilde, this);
	}

}
