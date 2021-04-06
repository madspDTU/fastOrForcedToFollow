package org.matsim.core.mobsim.qsim.qnetsimengine;

import java.util.Collection;

import org.matsim.api.core.v01.Id;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.qnetsimengine.QFFFNode.MoveType;
import org.matsim.core.mobsim.qsim.qnetsimengine.flow_efficiency.FlowEfficiencyCalculator;
import org.matsim.core.mobsim.qsim.qnetsimengine.vehicleq.VehicleQ;
import org.matsim.lanes.Lane;
import org.matsim.vehicles.Vehicle;

public class QueueWithSingleBufferForRoW extends QueueWithBufferForRoW {

	public QueueWithSingleBufferForRoW(AbstractQLink.QLinkInternalInterface qlink, final VehicleQ<QVehicle> vehicleQueue, Id<Lane> laneId,
			double length, double effectiveNumberOfLanes, double flowCapacity_s, final NetsimEngineContext context, 
			FlowEfficiencyCalculator flowEfficiencyCalculator) {
		super(qlink, vehicleQueue, laneId, length, effectiveNumberOfLanes, flowCapacity_s, context, flowEfficiencyCalculator);
	}

	@Override
	protected void actuallyAddToBuffer(QVehicleAndMoveType veh, double now) {
		if(newGeneralBuffer.isEmpty()) {
			generalBufferLastMovedTime = now;
		}
		newGeneralBuffer.add(veh);
	}

	@Override
	protected QVehicle getVehicleInLeftBuffers(Id<Vehicle> vehicleId) {
		return null;
	}

	@Override
	protected void addVehiclesFromLeftBuffer(Collection<MobsimVehicle> vehicles) {
		//Do nothing
	}

	@Override
	protected QVehicle actuallyRemoveFirstGeneralVehicle(double now) {
		QVehicle veh = newGeneralBuffer.poll();
		generalBufferLastMovedTime = now; // just in case there is another vehicle in the buffer that is now the new front-most
		return veh;
	}

	@Override
	//This version is called when removing left-turning vehicles, even though they are in the general buffer....
	protected QVehicle actuallyRemoveFirstLeftVehicle(double now) {
		return actuallyRemoveFirstGeneralVehicle(now);
	}

	@Override
	protected void clearLeftBuffer(double now) {
		//Do nothing	
	}

	@Override
	protected boolean isLeftBufferEmpty() {
		return true;
	}

	@Override
	public boolean isNotOfferingVehicle() {
		return newGeneralBuffer.isEmpty();
	}
	

	@Override
	public boolean isNotOfferingGeneralVehicle() {
		return newGeneralBuffer.isEmpty();
	}

	@Override
	public boolean isNotOfferingLeftVehicle() {
		return true;
	}

	@Override
	public QVehicleAndMoveType getFirstLeftVehicle() {
		System.err.println("Should NOT get first left vehicle in single buffer situation!");
		return null;
	}

	@Override
	public double getLeftBufferLastTimeMoved() {
		return generalBufferLastMovedTime;
	}
}
