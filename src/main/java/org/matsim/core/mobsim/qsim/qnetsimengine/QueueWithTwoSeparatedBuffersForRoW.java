package org.matsim.core.mobsim.qsim.qnetsimengine;

import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.VehicleAbortsEvent;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.qnetsimengine.QFFFNode.MoveType;
import org.matsim.core.mobsim.qsim.qnetsimengine.flow_efficiency.FlowEfficiencyCalculator;
import org.matsim.core.mobsim.qsim.qnetsimengine.vehicleq.VehicleQ;
import org.matsim.core.utils.misc.Time;
import org.matsim.lanes.Lane;
import org.matsim.vehicles.Vehicle;

public class QueueWithTwoSeparatedBuffersForRoW extends QueueWithBufferForRoW {

	private final Queue<QVehicleAndMoveType> leftBuffer = new ConcurrentLinkedQueue<>() ;
	private double leftBufferLastMovedTime = Double.NEGATIVE_INFINITY;

	
	public QueueWithTwoSeparatedBuffersForRoW(AbstractQLink.QLinkInternalInterface qlink, final VehicleQ<QVehicle> vehicleQueue, Id<Lane> laneId,
			double length, double effectiveNumberOfLanes, double flowCapacity_s, final NetsimEngineContext context, 
			FlowEfficiencyCalculator flowEfficiencyCalculator) {
		super(qlink, vehicleQueue, laneId, length, effectiveNumberOfLanes, flowCapacity_s, context, flowEfficiencyCalculator);
	}


	@Override
	protected void actuallyAddToBuffer(QVehicleAndMoveType vehAndMt, double now) {
		if(vehAndMt.getMoveType() == MoveType.LEFT_TURN) {
			if(leftBuffer.isEmpty()) {
				leftBufferLastMovedTime = now;
			}
			leftBuffer.add(vehAndMt);
		} else {
			if(newGeneralBuffer.isEmpty()) {
				generalBufferLastMovedTime = now;
			}
			newGeneralBuffer.add(vehAndMt);
		}
			
	}
	
	@Override
	protected QVehicle getVehicleInLeftBuffers(Id<Vehicle> vehicleId) {
		for(QVehicleAndMoveType veh : leftBuffer) {
			if(veh.getQVehicle().getId().equals(vehicleId)) {
				return veh.getQVehicle();
			}
		}
		return null;
	}
	
	@Override
	protected void addVehiclesFromLeftBuffer(Collection<MobsimVehicle> vehicles) {
		for(QVehicleAndMoveType veh : leftBuffer) {
			vehicles.add(veh.getQVehicle());
		}
	}
	
	@Override
	protected QVehicle actuallyRemoveFirstGeneralVehicle(double now) {
		QVehicle veh = newGeneralBuffer.poll();
		generalBufferLastMovedTime = now; // just in case there is another vehicle in the buffer that is now the new front-most
		return veh;
	}


	@Override
	protected QVehicle actuallyRemoveFirstLeftVehicle(double now) {
		QVehicle veh = leftBuffer.poll();
		leftBufferLastMovedTime = now; // just in case there is another vehicle in the buffer that is now the new front-most
		return veh;
	}
	
	@Override
	protected void clearLeftBuffer(double now) {
		for (QVehicleAndMoveType veh : leftBuffer) {
			context.getEventsManager().processEvent( new VehicleAbortsEvent(now, veh.getId(), veh.getCurrentLink().getId()));
			context.getEventsManager().processEvent( new PersonStuckEvent(now, veh.getDriver().getId(), veh.getCurrentLink().getId(), veh.getDriver().getMode()));

			context.getAgentCounter().incLost();
			context.getAgentCounter().decLiving();
		}
		leftBuffer.clear();
	}

	@Override
	protected boolean isLeftBufferEmpty() {
		return leftBuffer.isEmpty();
	}
	
	@Override
	public final boolean isNotOfferingVehicle() {
		return newGeneralBuffer.isEmpty() && leftBuffer.isEmpty();
	}


	@Override
	public final boolean isNotOfferingGeneralVehicle() {
		return newGeneralBuffer.isEmpty();
	}


	@Override
	public boolean isNotOfferingLeftVehicle() {
		return leftBuffer.isEmpty();
	}

	@Override
	public QVehicleAndMoveType getFirstLeftVehicle() {
		return leftBuffer.peek();
	}
	
	
	@Override
	public double getLeftBufferLastTimeMoved() {
		return leftBufferLastMovedTime;
	}
	
}
