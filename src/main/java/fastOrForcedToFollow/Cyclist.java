package fastOrForcedToFollow;

import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;


/**
 * @author mpaulsen
 *
 */
public class Cyclist {

	private final String id;
	private final double desiredSpeed;
	private double speed = -1; //Current speed
	private double tStart = 0; // Time at which the cyclist entered the link.      'madsp: Stricly not needed.
	private double tEarliestExit = 0;
	private LinkedList<Double[]> speedReport = new LinkedList<Double[]>(); 
	private LinkedList<Link> route;
	private final LinkTransmissionModel ltm;

	
	private Cyclist(String id, double desiredSpeed, double theta_0, double theta_1, LinkedList<Link> route){
		this.id = id;
		this.desiredSpeed = desiredSpeed;
		this.ltm = new LinkTransmissionModel(theta_0, theta_1);
		this.route = route;
		speedReport.add(new Double[]{-1d, 0d,-1d});
	}
	
	public static Cyclist createGlobalCyclist(String id, double desiredSpeed, LinkedList<Link> route){
		return new Cyclist(id, desiredSpeed, Runner.theta_0, Runner.theta_1, route);
	}
	
	public static Cyclist createIndividualisedCyclist(String id, double desiredSpeed, double z_c, LinkedList<Link> route){
		return new Cyclist(id, desiredSpeed, Runner.theta_0 + z_c*Runner.zeta_0, Runner.theta_1 + z_c*Runner.zeta_1, route);
	}
	
	
	


	/**
	 * Advances the cyclist to next link if there is sufficient space on the next link.
	 * 
	 * @param lqo The <code>LinkQObject</code> which is currently begin processed.
	 * 
	 * @return <true> if the cyclist could enter the next link, and
	 *         <false> otherwise.
	 */

	public void advanceCyclist(String previousLinkId){
		Link nextLink = this.route.peekFirst();
		PseudoLane pseudoLane = this.selectPseudoLane(nextLink); 
		double vTilde = this.getVMax(pseudoLane);
		if(speedFitsOnLink(vTilde, nextLink)){
			this.route.removeFirst();
			Link previousLink = Runner.linksMap.get(previousLinkId);
			previousLink.incrementOutFlowCounter();
			previousLink.getOutQ().remove();
			reduceOccupiedSpace(previousLinkId, this.speed);
			double tLeave = Double.max(pseudoLane.tReady, this.tEarliestExit);
			
			previousLink.setWakeUpTime(tLeave);
			
			
				
			if(previousLink.getId() != Runner.sourceLink.getId()){
				this.reportSpeed(previousLink.getLength(), tLeave);
				previousLink.reportOutputTime(tLeave);
				previousLink.reportSpeedTime(tLeave, getSpeedReport().getLast()[2]);
			}
			initialiseNewSpeedReportElement(nextLink.getId(), tLeave);	
			
			
			
			this.setSpeed(vTilde);
			this.setTStart(tLeave);
			this.setTEarliestExit(tLeave + nextLink.getLength()/vTilde);
			increaseOccupiedSpace(nextLink.getId(), vTilde);
			pseudoLane.updateTs(vTilde, tLeave);
			nextLink.incrementInFlowCounter();
			nextLink.getOutQ().add(new CyclistQObject(this));
		} else {
			System.err.println("Something is terribly wrong");
		}
	}

	public void exportSpeeds(String baseDir) throws IOException{
		FileWriter writer = new FileWriter(baseDir + "/Cyclists/speedsOfCyclist" + id +
				"_" + Runner.N + "Persons.csv");
		writer.append("Time;Speed\n");
		for(Double[] reportElement : speedReport){
			writer.append(reportElement[0] + ";" + reportElement[1] + "\n");
		}
		writer.flush();
		writer.close();
	}

	/**
	 * @return The desired speed (in m/s) of the cyclist.
	 */
	public double getDesiredSpeed(){
		return desiredSpeed;
	}

	/**
	 * @return The integer id of the cyclist.
	 */
	public String getId(){
		return id;
	}

	public LinkTransmissionModel getLTM(){
		return this.ltm;
	}
	
	/**
	 * @return The (remaining part of the) route of the cyclist.
	 */
	public LinkedList<Link> getRoute(){
		return route;
	}

	/**
	 * @return The speed report containing the speed (in m/s) at certain times (in seconds) during the simulation.
	 */
	public LinkedList<Double[]> getSpeedReport(){
		return speedReport;
	}


	public void initialiseNewSpeedReportElement(String id, double t){
		speedReport.addLast(new Double[]{Double.valueOf(id), t, -1d});
	}


	public void moveToNextQ(Link nextLink, double tEnd){
	}

	public void reportSpeedWeird(double time){
		speedReport.addLast(new Double[]{time, this.speed});
	}

	public void reportSpeed(double length){
		speedReport.getLast()[2] = length/(this.tEarliestExit - speedReport.getLast()[1]);
	}
	
	public void reportSpeed(double length, double tLeave){
		speedReport.getLast()[2] = length/(tLeave - this.tStart);
	}




	public void setSpeed(double newCurrentSpeed){
		if(newCurrentSpeed < desiredSpeed){
			this.speed = newCurrentSpeed;
		} else {
			this.speed = desiredSpeed;
		}
	}

	public void terminateCyclist(String linkId){
		ltm.reduceOccupiedSpace(linkId, this.speed);
	}

	public boolean speedFitsOnLink(final double speed, final Link link){
		return this.ltm.getSafetyBufferDistance(speed) + link.getOccupiedSpace() < link.getTotalLaneLength() || link.getOccupiedSpace() < 0.1;
	}
	
	public PseudoLane selectPseudoLane(Link receivingLink){
		return this.ltm.selectPseudoLane(receivingLink, this.desiredSpeed, this.tEarliestExit);
	}
	
	public double getVMax(final PseudoLane pseudoLane){
		return Math.min(desiredSpeed,this.ltm.getVMax(pseudoLane, this.tEarliestExit));
	}

	
	public double getTEarliestExit(){
		return this.tEarliestExit;
	}
	
	public double getTStart(){
		return this.tStart;
	}
	
	public void setTEarliestExit(double time){
		this.tEarliestExit = time;
	}
	
	public void setTStart(double time){
		this.tStart = time;
	}

	public boolean isNotInFuture(double now){
		return this.getTEarliestExit() <= now;
	}
	
	public boolean fitsOnLink(final Link link){
		PseudoLane pseudoLane = selectPseudoLane(link);
		double vTilde = getVMax(pseudoLane);
		return speedFitsOnLink(vTilde, link);
	}
	
	
	public void increaseOccupiedSpace(String linkId, double speed){
		this.ltm.increaseOccupiedSpace(linkId, speed);
	}
	
	public void reduceOccupiedSpace(String linkId, double speed){
		this.ltm.reduceOccupiedSpace(linkId, speed);
	}
	
	
	public double getSpeed(){
		return this.speed;
	}
}
