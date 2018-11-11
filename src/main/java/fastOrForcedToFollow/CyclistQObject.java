package fastOrForcedToFollow;

import org.matsim.core.mobsim.qsim.qnetsimengine.QCycleAsVehicle;

/**
 * @author mpaulsen
 * 
 * A CyclistQObject is essentially a QCycleAsVehicle, but has a modififed compareTo method, allowing it to be sorted
 * by earliest possible exit time in the priority queue when inserted.
 *
 */
public class CyclistQObject implements Comparable<CyclistQObject>{

	
	/**
	 * The QCycleAsVehicle which constitutes the CyclistQObject.
	 */
	private QCycleAsVehicle qCyc;
	
	/**
	 * @param cyclist The cyclist that the QCycleAsVehicle and subsequently the CyclistQObject will be based on.
	 * A CyclistQObject requires a QCycleAsVehicle, which - by assuming BicycleVehicleType - 
	 * can be uniquely determined by the cyclist.
	 */
	CyclistQObject(Cyclist cyclist){
		this.qCyc = new QCycleAsVehicle(cyclist);
	}
		
	/**
	 * @param qCyc The QCycleAsVehicle that should be parsed on to the new CyclistQObject
	 * In this case, the old QCycleAsVehicle can be recycled.
	 */
	public CyclistQObject(QCycleAsVehicle qCyc){
		this.qCyc = qCyc;
	}

		
	/**
	 * @return The Cyclist associated with this CQO.
	 */
	public Cyclist getCyclist(){
		return qCyc.getCyclist();
	}
	
	/**
	 * @return The QCycleAsVehicle associated with this CQO.
	 */
	public QCycleAsVehicle getQCycle(){
		return this.qCyc;
	}
	
	
	/**
	 * @return The time at which the Cyclist entered the Q.
	 */
	public double getTime(){
		return this.qCyc.getCyclist().getTEarliestExit();
	}
	
	
	/**
	 * CyclistQObject are sorted based on the tEarliestExit of the cyclist.
	 */
	public int compareTo(CyclistQObject other){
			return Double.compare(this.qCyc.getCyclist().getTEarliestExit(), other.qCyc.getCyclist().getTEarliestExit());
	}

	/**
	 * CyclistQObject are sorted based on the tEarliestExit of the cyclist.
	 */
	public int compare(CyclistQObject cqo1, CyclistQObject cqo2){
			return Double.compare(cqo1.qCyc.getCyclist().getTEarliestExit(), cqo2.qCyc.getCyclist().getTEarliestExit());
	}

}
