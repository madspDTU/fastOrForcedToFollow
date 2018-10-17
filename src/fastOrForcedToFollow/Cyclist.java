package fastOrForcedToFollow;

import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;


public class Cyclist {

	private int id;
	private double desiredSpeed;
	private double speed = -1; //Current speed
	private double tStart = 0;
	private LinkedList<Double[]> speedReport = new LinkedList<Double[]>(); 
	// Id, tStart, v
	// Has to be done such that the speed is reported
	// when exiting the link. The speed is unknown at
	// entry since the next link may be filled up
	// (has its storage capactity exceeded).


	public LinkedList<Link> route;
	public LinkTransmissionModel ltm;

	Cyclist(int id, double cruiseSpeed, LinkedList<Link> route, LinkTransmissionModel ltm) throws InstantiationException, IllegalAccessException{
		this.id = id;
		this.desiredSpeed = cruiseSpeed;
		this.route = route;
		this.ltm = ltm;
		speedReport.add(new Double[]{-1d, 0d,-1d});
	}

	/**
	 * @return The speed report containing the speed (in m/s) at certain times (in seconds) during the simulation.
	 */
	public LinkedList<Double[]> getSpeedReport(){
		return speedReport;
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
	
	public void reportSpeed(double time){
		speedReport.addLast(new Double[]{time, this.speed});
	}

	public void initialiseNewSpeedReportElement(double id, double t){
		speedReport.addLast(new Double[]{id, t, -1d});
	}


	public void reportSpeed(double length, double t){
		speedReport.getLast()[2] = length/(t - speedReport.getLast()[1]);
	}


	public void exportSpeeds(String baseDir) throws IOException{
		FileWriter writer = new FileWriter(baseDir + "/Cyclists/speedsOfCyclist" + id +
				"_" + Runner.N + "Persons_" + Runner.circuitString + ".csv");
		writer.append("Time;Speed\n");
		for(Double[] reportElement : speedReport){
			writer.append(reportElement[0] + ";" + reportElement[1] + "\n");
		}
		writer.flush();
		writer.close();
	}

	public boolean advanceCyclist(LinkQObject lqo){
		Link nextLink = route.pollFirst();
		double oldSpeed = this.speed;
		PseudoLane pseudoLane = ltm.selectPseudoLaneAndAdaptSpeed(nextLink, this, lqo.getTime()); 
		if(ltm.getSafetyBufferDistance(this.speed) <= nextLink.getTotalLaneLength() - nextLink.getOccupiedSpace()){
			ltm.reduceOccupiedSpace(lqo.getId(), oldSpeed);
			ltm.increaseOccupiedSpace(nextLink.getId(), this.speed);
			this.tStart = Double.max(pseudoLane.tReady, lqo.getTime());
			Runner.linksMap.get(lqo.getId()).setWakeUpTime(this.tStart);
			ltm.updatePseudoLane(pseudoLane, speed, lqo.getTime());
			double tEnd = pseudoLane.tEnd - ltm.getSafetyBufferTime(speed);
			if(Runner.waitingTimeCountsOnNextLink){
				reportSpeed(nextLink.getLength(), tEnd);
			}
			moveToNextQ(nextLink, tEnd);
			sendNotification(nextLink.getId(), Math.max(tEnd,nextLink.getWakeUpTime()));
			if(Runner.circuit){
				route.addLast(nextLink);
			}
			return true;
		} else {
			this.speed = oldSpeed; // reset speed;
			route.addFirst(nextLink); //reset link;
			return false; // parse information on to previous method
		}
	}

	public void sendNotification(int linkId, double tEnd){
		if(tEnd < Runner.T){
			if(tEnd <= Runner.t){
				sendShortTermNotification(linkId, tEnd);
			} else {
				int k  = ((int) tEnd) / ((int) Runner.tau) + 1;
				if( !Runner.notificationArray[k].containsKey(linkId) || tEnd < Runner.notificationArray[k].get(linkId) ){
					Runner.notificationArray[k].put(linkId, tEnd);	
				}
			}
		}
	}

	public void sendShortTermNotification(int linkId, double tEnd){
		Runner.shortTermPriorityQueue.add(new LinkQObject(tEnd, linkId));
	}

	public void moveToNextQ(Link nextLink, double tEnd){
		nextLink.incrementInFlowCounter();
		nextLink.getOutQ().add(new CyclistQObject(tEnd,this));
	}

	public void setSpeed(double newCurrentSpeed){
		if(newCurrentSpeed < desiredSpeed){
			this.speed = newCurrentSpeed;
		} else {
			this.speed = desiredSpeed;
		}
	}

	public void terminateCyclist(LinkQObject loq){
		ltm.reduceOccupiedSpace(loq.getId(), this.speed);
	}



}
