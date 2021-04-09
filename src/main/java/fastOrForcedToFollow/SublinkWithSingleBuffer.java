package fastOrForcedToFollow;

import java.util.LinkedList;

import org.matsim.core.mobsim.qsim.qnetsimengine.QCycleAndMoveType;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;

public class SublinkWithSingleBuffer extends Sublink {
	
	public SublinkWithSingleBuffer(String id, int psi, double subLinkLength) {
		super(id, psi, subLinkLength);
	}
	
	@Override
	public boolean hasNoLeavingVehicles(){
		return generalLeavingVehicles.isEmpty();
	}
	
	@Override
	public LinkedList<QVehicle> getAllLeavingVehicles(){
		LinkedList<QVehicle> linkedList = new LinkedList<QVehicle>();
		linkedList.addAll(this.generalLeavingVehicles);
		return linkedList;
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
		if(this.generalLeavingVehicles.isEmpty()) {
			this.generalLastTimeMoved = now;
		}
		this.generalLeavingVehicles.addLast(veh);
	}

	@Override
	public QCycleAndMoveType getFirstLeftLeavingVehicle() {
		System.err.println("Should NOT get first left vehicle in single buffer situation!");
		return null;
	}

	@Override
	public  QCycleAndMoveType pollFirstLeftLeavingVehicle(double now) {
		return (QCycleAndMoveType) pollFirstGeneralLeavingVehicle(now);
	}

	@Override
	public boolean hasNoGeneralLeavingVehicles(double now) {
		return this.generalLeavingVehicles.isEmpty();
	}

	@Override
	public boolean hasNoLeftLeavingVehicles(double now) {
		return true;
	}

	@Override
	public double getLeftBufferLastTimeMoved() {
		return this.generalLastTimeMoved;
	}

	@Override
	public void setLastTimeMovedLeft(double lastMoved) {
		this.generalLastTimeMoved = lastMoved;
	}

	
}
