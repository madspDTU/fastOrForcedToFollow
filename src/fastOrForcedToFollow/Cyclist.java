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
	private LinkTransmissionModel ltm;

	Cyclist(int id, double cruiseSpeed, LinkedList<Link> route, LinkTransmissionModel ltm) throws InstantiationException, IllegalAccessException{
		this.id = id;
		this.desiredSpeed = cruiseSpeed;
		this.route = route;
		this.ltm = ltm;
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
		Link nextLink = route.peekFirst();
		double originalSpeed = this.speed;
		PseudoLane pseudoLane = ltm.selectPseudoLaneAndAdaptSpeed(nextLink, this, time); 
		if(ltm.getSafetyBufferDistance(this.speed) <= nextLink.getTotalLaneLength() - nextLink.getOccupiedSpace()){
			route.removeFirst();
			ltm.reduceOccupiedSpace(linkId, originalSpeed);
			ltm.increaseOccupiedSpace(nextLink.getId(), this.speed);
			this.tStart = Double.max(pseudoLane.tReady, time);
			Link currentLink = Runner.linksMap.get(linkId);
			currentLink.setWakeUpTime(this.tStart);
			ltm.updatePseudoLane(pseudoLane, speed, time);
			double tEnd = pseudoLane.tEnd - ltm.getSafetyBufferTime(speed);
			moveToNextQ(nextLink, tEnd);
			currentLink.getOutQ().remove();
			currentLink.incrementOutFlowCounter();
			nextLink.sendNotification(Math.max(tEnd,nextLink.getWakeUpTime()));
			
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
