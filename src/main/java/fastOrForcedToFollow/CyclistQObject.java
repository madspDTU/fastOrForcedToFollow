package fastOrForcedToFollow;

import org.matsim.core.mobsim.qsim.qnetsimengine.QCycleAsVehicle;

/**
 * @author mpaulsen
 * 
 * A CyclistQObject is essentially a CyclistQObject, but has a modififed compareTo method, allowing it to be sorted
 * by earliest possible exit time in the priority queue when inserted.
 *
 */
public class CyclistQObject implements Comparable<CyclistQObject>{

	
	private QCycleAsVehicle qCyc; // The cyclist.

	CyclistQObject(Cyclist cyclist){
		this.qCyc = new QCycleAsVehicle(cyclist);
	}
	
	CyclistQObject(QCycleAsVehicle qCyc){
		this.qCyc = qCyc;
	}

	
	

	/**
	 * @return The cyclist associated with this CQO.
	 */
	public Cyclist getCyclist(){
		return qCyc.getCyclist();
	}
	
	
	public QCycleAsVehicle getQCycle(){
		return this.qCyc;
	}
	
	
	/**
	 * @return The time at which the cyclist entered the Q.
	 */
	
	public double getTime(){
		return this.qCyc.getCyclist().getTEarliestExit();
	}
	
	public int compareTo(CyclistQObject other){
			return Double.compare(this.qCyc.getCyclist().getTEarliestExit(), other.qCyc.getCyclist().getTEarliestExit());
	}

	public int compare(CyclistQObject cqo1, CyclistQObject cqo2){
			return Double.compare(cqo1.qCyc.getCyclist().getTEarliestExit(), cqo2.qCyc.getCyclist().getTEarliestExit());
	}

}
