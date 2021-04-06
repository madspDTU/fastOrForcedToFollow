package org.matsim.core.mobsim.qsim.qnetsimengine;

import java.util.Collection;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.framework.DriverAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.PassengerAgent;
import org.matsim.core.mobsim.qsim.qnetsimengine.QFFFNode.MoveType;
import org.matsim.vehicles.Vehicle;

import fastOrForcedToFollow.Cyclist;

public class QCycleAndMoveType implements QVehicle{
	
	private QCycle qCyc;
	private MoveType moveType;
	private int outDirection;
	
	public QCycleAndMoveType(final QCycle veh, final MoveType moveType, int outDirection) {
		this.qCyc = veh;
		this.moveType = moveType;
		this.outDirection = outDirection;
	}

	public Cyclist getCyclist() {
		return qCyc.getCyclist();
	}
	
	public QCycle getQCycle() {
		return qCyc;
	}
	
	public QVehicle getQVehicle() {
		return qCyc.getQVehicle();
	}

	@Override
	public double getEarliestLinkExitTime() {
		return qCyc.getEarliestLinkExitTime();
	}

	@Override
	public void setEarliestLinkExitTime(double earliestLinkEndTime) {
		qCyc.setEarliestLinkExitTime(earliestLinkEndTime);
	}

	@Override
	public Vehicle getVehicle() {
		return qCyc.getVehicle();
	}

	@Override
	public MobsimDriverAgent getDriver() {
		return qCyc.getDriver();
	}

	@Override
	public Id<Vehicle> getId() {
		return qCyc.getId();
	}

	@Override
	public Link getCurrentLink() {
		return qCyc.getCurrentLink();
	}

	@Override
	public boolean addPassenger(PassengerAgent passenger) {
		return qCyc.addPassenger(passenger);
	}

	@Override
	public boolean removePassenger(PassengerAgent passenger) {
		return qCyc.removePassenger(passenger);
	}

	@Override
	public Collection<? extends PassengerAgent> getPassengers() {
		return qCyc.getPassengers();
	}

	@Override
	public int getPassengerCapacity() {
		return qCyc.getPassengerCapacity();
	}

	@Override
	public void setCurrentLink(Link link) {
		qCyc.setCurrentLink(link);
	}

	@Override
	public void setDriver(DriverAgent driver) {
		qCyc.setDriver(driver);
	}

	@Override
	public double getLinkEnterTime() {
		return qCyc.getLinkEnterTime();
	}

	@Override
	public void setLinkEnterTime(double linkEnterTime) {
		qCyc.setLinkEnterTime(linkEnterTime);
	}

	@Override
	public double getMaximumVelocity() {
		return qCyc.getMaximumVelocity();
	}

	@Override
	public double getSizeInEquivalents() {
		return qCyc.getSizeInEquivalents();
	}

	public int getOutDirection() {
		return outDirection;
	}
	

	public QFFFNode.MoveType getMoveType() {
		return moveType;
	}
	

}
