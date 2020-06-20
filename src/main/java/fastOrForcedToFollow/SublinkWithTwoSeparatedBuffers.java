package fastOrForcedToFollow;

import java.util.LinkedList;

import org.matsim.core.mobsim.qsim.qnetsimengine.QFFFNode.MoveType;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.core.mobsim.qsim.qnetsimengine.QCycleAndMoveType;

public class SublinkWithTwoSeparatedBuffers extends Sublink {
	
	
	private LinkedList<QCycleAndMoveType> leftLeavingVehicles;
	private double leftLastTimeMoved;

	public SublinkWithTwoSeparatedBuffers(String id, int psi, double subLinkLength) {
		super(id, psi, subLinkLength);
		this.leftLeavingVehicles = new LinkedList<QCycleAndMoveType>();
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
		if(veh.getMoveType() == MoveType.LEFT_TURN) {
			if(this.leftLeavingVehicles.isEmpty()) {
				this.leftLastTimeMoved = now;
			}
			this.leftLeavingVehicles.add(veh);
		} else {
			if(this.generalLeavingVehicles.isEmpty()) {
				this.generalLastTimeMoved = now;
			}
			this.generalLeavingVehicles.add(veh);
		}
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
		return this.generalLeavingVehicles.isEmpty();
	}




	@Override
	public boolean hasNoLeftLeavingVehicles(double now) {
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