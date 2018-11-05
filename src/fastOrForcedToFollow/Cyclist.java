package fastOrForcedToFollow;

import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;


/**
 * @author mpaulsen
 *
 */
public class Cyclist {

	private int id;
	private double desiredSpeed;
	private double speed = -1; //Current speed
	private double tStart = 0; // Time at which the cyclist entered the link.
	private LinkedList<Double[]> speedReport = new LinkedList<Double[]>(); 
	private LinkedList<Link> route;
	private final LinkTransmissionModel ltm;
	private double theta_0;
	private double theta_1;

	Cyclist(int id, double cruiseSpeed, double z_c, LinkedList<Link> route) throws InstantiationException, IllegalAccessException{
		this.id = id;
		this.desiredSpeed = cruiseSpeed;
		this.theta_0 = Runner.theta_0 + z_c * Runner.zeta_0;
		this.theta_1 = Runner.theta_1 + z_c * Runner.zeta_1;
		this.route = route;
		this.ltm = new LinkTransmissionModel(theta_0, theta_1);
		speedReport.add(new Double[]{-1d, 0d,-1d});
	}

	/**
	 * Advances the cyclist to next link if there is sufficient space on the next link.
	 * 
	 * @param lqo The <code>LinkQObject</code> which is currently begin processed.
	 * 
	 * @return <true> if the cyclist could enter the next link, and
	 *         <false> otherwise.
	 */

	public boolean advanceCyclist(int linkId, double time){
		Link nextLink = this.route.peekFirst();
		double originalSpeed = this.speed;
		PseudoLane pseudoLane = this.ltm.selectPseudoLaneAndAdaptSpeed(nextLink, this, time); 
		if(this.ltm.getSafetyBufferDistance(this.speed) <= nextLink.getTotalLaneLength() - nextLink.getOccupiedSpace() ||
				nextLink.getOccupiedSpace() < 0.2){
			this.route.removeFirst();
			Link currentLink = Runner.linksMap.get(linkId);
			currentLink.incrementOutFlowCounter();
			currentLink.getOutQ().remove();
			this.ltm.reduceOccupiedSpace(linkId, originalSpeed);
			this.ltm.increaseOccupiedSpace(nextLink.getId(), this.speed);
			this.tStart = Double.max(pseudoLane.tReady, time);
			currentLink.setWakeUpTime(this.tStart);
			pseudoLane.updateTs(speed, time);
			double tArrivalAtNextLink = pseudoLane.tEnd - Runner.lambda_c/speed;
			moveToNextQ(nextLink, tArrivalAtNextLink);
			nextLink.sendNotification(Math.max(tArrivalAtNextLink,nextLink.getWakeUpTime()));
			return true;
		} else {
			this.speed = originalSpeed; // reset speed to original value;
			return false; // parse information on to previous method
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
	public int getId(){
		return id;
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


	public void initialiseNewSpeedReportElement(double id, double t){
		speedReport.addLast(new Double[]{id, t, -1d});
	}


	public void moveToNextQ(Link nextLink, double tEnd){
		nextLink.incrementInFlowCounter();
		nextLink.getOutQ().add(new CyclistQObject(tEnd,this));
	}

	public void reportSpeed(double time){
		speedReport.addLast(new Double[]{time, this.speed});
	}

	public void reportSpeed(double length, double t){
		speedReport.getLast()[2] = length/(t - speedReport.getLast()[1]);
	}




	public void setSpeed(double newCurrentSpeed){
		if(newCurrentSpeed < desiredSpeed){
			this.speed = newCurrentSpeed;
		} else {
			this.speed = desiredSpeed;
		}
	}

	public void terminateCyclist(int linkId){
		ltm.reduceOccupiedSpace(linkId, this.speed);
	}



}
