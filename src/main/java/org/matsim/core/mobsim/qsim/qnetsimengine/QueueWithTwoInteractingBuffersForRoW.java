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

public class QueueWithTwoInteractingBuffersForRoW extends QueueWithBufferForRoW {
		
	private LeftBufferWithCapacity leftBuffer;

	public QueueWithTwoInteractingBuffersForRoW(AbstractQLink.QLinkInternalInterface qlink, final VehicleQ<QVehicle> vehicleQueue, Id<Lane> laneId,
			double length, double effectiveNumberOfLanes, double flowCapacity_s, final NetsimEngineContext context, 
			FlowEfficiencyCalculator flowEfficiencyCalculator, Double leftBufferCapacity) {
		super(qlink, vehicleQueue, laneId, length, effectiveNumberOfLanes, flowCapacity_s, context, flowEfficiencyCalculator);
		this.leftBuffer = new LeftBufferWithCapacity(leftBufferCapacity);
	}


	@Override
	protected void actuallyAddToBuffer(QVehicleAndMoveType vehAndMt, double now) {
		if(newGeneralBuffer.isEmpty()) {
			generalBufferLastMovedTime = now;
		}
		newGeneralBuffer.add(vehAndMt);
	}


	@Override
	protected QVehicle getVehicleInLeftBuffers(Id<Vehicle> vehicleId) {
		for(QVehicleAndMoveType veh : leftBuffer.getQ()) {
			if(veh.getQVehicle().getId().equals(vehicleId)) {
				return veh.getQVehicle();
			}
		}
		return null;
	}


	@Override
	protected void addVehiclesFromLeftBuffer(Collection<MobsimVehicle> vehicles) {
		for(QVehicleAndMoveType veh : leftBuffer.getQ()) {
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
		QVehicle veh = leftBuffer.pollFirstVehicle();
		leftBuffer.setLastTimeMoved(now); // just in case there is another vehicle in the buffer that is now the new front-most
		return veh;
	}


	@Override
	protected void clearLeftBuffer(double now) {
		for (QVehicleAndMoveType veh : leftBuffer.getQ()) {
			context.getEventsManager().processEvent( new VehicleAbortsEvent(now, veh.getId(), veh.getCurrentLink().getId()));
			context.getEventsManager().processEvent( new PersonStuckEvent(now, veh.getDriver().getId(), veh.getCurrentLink().getId(), veh.getDriver().getMode()));

			context.getAgentCounter().incLost();
			context.getAgentCounter().decLiving();
		}
		leftBuffer.clearQueue();
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
	public boolean isNotOfferingGeneralVehicle() {
		if(newGeneralBuffer.isEmpty()) {
			return true;
		} 
		QVehicleAndMoveType veh;
		while((veh = newGeneralBuffer.peek()) != null) {
			if(veh.getMoveType() == MoveType.LEFT_TURN) {
				if(!leftBuffer.isCapacityReached()) {
					double now = context.getSimTimer().getTimeOfDay();
					generalBufferLastMovedTime = now;
					newGeneralBuffer.remove();
					if(leftBuffer.isEmpty()) {
						leftBuffer.setLastTimeMoved(now);
					}
					leftBuffer.addVehicle(veh);
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
	public boolean isNotOfferingLeftVehicle() {
		if(leftBuffer.isCapacityReached()) {
			return false;
		}
		if(newGeneralBuffer.isEmpty()) {
			return leftBuffer.isEmpty();
		}
		QVehicleAndMoveType veh;
		while((veh = newGeneralBuffer.peek()) != null) {
			if(veh.getMoveType() == MoveType.LEFT_TURN) {
				double now = context.getSimTimer().getTimeOfDay();
				generalBufferLastMovedTime = now;
				newGeneralBuffer.remove();
				if(leftBuffer.isEmpty()) {
					leftBuffer.setLastTimeMoved(now);
				}
				leftBuffer.addVehicle(veh);
				if(leftBuffer.isCapacityReached()) {
					return false;
				} else {
					continue;
				}
			} else {
				break;
			}
		}
		return leftBuffer.isEmpty();
	}

	@Override
	public QVehicleAndMoveType getFirstLeftVehicle() {
		return leftBuffer.peek();
	}

	@Override
	public double getLeftBufferLastTimeMoved() {
		return leftBuffer.getLastTimeMoved();
	}
	
	
	
	// The auxiliary LeftBufferWithCapacityClass.
	
	private class LeftBufferWithCapacity {
		
		private Queue<QVehicleAndMoveType> Q;
		private double lastTimeMoved;
		private double capacity;
		private double currentSize;
		
		protected LeftBufferWithCapacity(double capacity) {
			this.Q = new ConcurrentLinkedQueue<>();
			this.lastTimeMoved = Double.NEGATIVE_INFINITY;
			this.capacity = capacity;
			this.currentSize = 0;
		}
		
		protected Queue<QVehicleAndMoveType> getQ() {
			return this.Q;
		}
		
		protected void setLastTimeMoved(double time) {
			this.lastTimeMoved = time;
		}
		
		protected boolean isEmpty() {
			return this.Q.isEmpty();
		}

		private void incrementCurrentSize(double sizeInEquivalents) {
			this.currentSize += sizeInEquivalents;
		}
		
		protected QVehicle pollFirstVehicle() {
			QVehicle veh = this.Q.poll();
			incrementCurrentSize(-veh.getSizeInEquivalents());
			return veh;
		}

		private void addVehicle(QVehicleAndMoveType veh) {
			this.Q.add(veh);
			incrementCurrentSize(veh.getSizeInEquivalents());
		}
		
		protected boolean isCapacityReached() {
			return this.currentSize >= this.capacity;
		}

		protected double getLastTimeMoved() {
			return lastTimeMoved;
		}

		protected void clearQueue() {
			this.Q.clear();
		}

		protected QVehicleAndMoveType peek() {
			return this.Q.peek();
		}
		
	}

}