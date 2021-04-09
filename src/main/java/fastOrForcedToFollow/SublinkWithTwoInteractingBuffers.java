package fastOrForcedToFollow;

import java.util.LinkedList;

import org.matsim.core.mobsim.qsim.qnetsimengine.QCycleAndMoveType;
import org.matsim.core.mobsim.qsim.qnetsimengine.QFFFNode.MoveType;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;

public class SublinkWithTwoInteractingBuffers extends Sublink {


	private LinkedList<QCycleAndMoveType> leftLeavingVehicles;
	private double leftLastTimeMoved;
	private int leftBufferCapacity;


	public SublinkWithTwoInteractingBuffers(String id, int psi, double subLinkLength, int leftBufferCapactiy) {
		super(id, psi, subLinkLength);
		this.leftLeavingVehicles = new LinkedList<QCycleAndMoveType>();
		this.leftBufferCapacity = leftBufferCapactiy;
	}




	@Override
	public boolean hasNoLeavingVehicles() {
		return this.generalLeavingVehicles.isEmpty() && this.leftLeavingVehicles.isEmpty();
	}

	@Override
	public LinkedList<QVehicle> getAllLeavingVehicles() {
		LinkedList<QVehicle> allLeavingVehicles = new LinkedList<QVehicle>();
		allLeavingVehicles.addAll(generalLeavingVehicles);
		allLeavingVehicles.addAll(leftLeavingVehicles);
		return allLeavingVehicles;
	}

	@Override
	public void addToLeavingVehicles(QCycleAndMoveType veh, double now) {
		if(this.generalLeavingVehicles.isEmpty()) {
			this.generalLastTimeMoved = now;
		}
		this.generalLeavingVehicles.add(veh);
	}


	@Override
	public void addVehicleToFrontOfLeavingVehicles(QCycleAndMoveType veh, double now) {
		if(veh.getMoveType() == MoveType.LEFT_TURN) {
			if(this.leftLeavingVehicles.isEmpty()) {
				this.leftLastTimeMoved = now;
			}
			this.leftLeavingVehicles.addFirst(veh);
		} else {
			if(this.generalLeavingVehicles.isEmpty()) {
				this.generalLastTimeMoved = now;
			}
			this.generalLeavingVehicles.addFirst(veh);
		}
	}




	@Override
	public QCycleAndMoveType getFirstLeftLeavingVehicle() {
		return this.leftLeavingVehicles.getFirst();
	}

	@Override
	public QCycleAndMoveType pollFirstLeftLeavingVehicle(double now) {
		this.leftLastTimeMoved = now;
		return this.leftLeavingVehicles.pollFirst();
	}




	@Override
	public boolean hasNoGeneralLeavingVehicles(double now) {
		if(this.generalLeavingVehicles.isEmpty()) {
			return true;
		} 
		QVehicle qVeh;
		while((qVeh = this.generalLeavingVehicles.peek()) != null) {
			QCycleAndMoveType veh = (QCycleAndMoveType) qVeh;
			if(veh.getMoveType() == MoveType.LEFT_TURN) {
				if(this.leftLeavingVehicles.size() < leftBufferCapacity) {
					this.generalLastTimeMoved = now;
					this.generalLeavingVehicles.remove();
					if(this.leftLeavingVehicles.isEmpty()) {
						this.leftLastTimeMoved = now;
					}
					this.leftLeavingVehicles.add(veh);
					continue;
				} else {
					return true;
				}
			} else {
				return false;
			}
		}
		return true;
	}




	@Override
	public boolean hasNoLeftLeavingVehicles(double now) {
		// can be above capacity if receiving partially left turning cyclists!
		if(this.leftLeavingVehicles.size() >= leftBufferCapacity) { 
			return false;
		}
		if(this.generalLeavingVehicles.isEmpty()) {
			return this.leftLeavingVehicles.isEmpty();
		}
		QVehicle qVeh;
		while((qVeh = this.generalLeavingVehicles.peek()) != null) {
			QCycleAndMoveType veh = (QCycleAndMoveType) qVeh;
			if(veh.getMoveType() == MoveType.LEFT_TURN) {
				this.generalLastTimeMoved = now;
				this.generalLeavingVehicles.remove();
				if(this.leftLeavingVehicles.isEmpty()) {
					this.leftLastTimeMoved = now;
				}
				this.leftLeavingVehicles.add(veh);
				if(this.leftLeavingVehicles.size() == leftBufferCapacity) {
					return false;
				} else {
					continue;
				}
			} else {
				break;
			}
		}
		return this.leftLeavingVehicles.isEmpty();
	}




	@Override
	public double getLeftBufferLastTimeMoved() {
		return this.leftLastTimeMoved;
	}




	@Override
	public void setLastTimeMovedLeft(double lastMoved) {
		this.leftLastTimeMoved = lastMoved;
	}



}
