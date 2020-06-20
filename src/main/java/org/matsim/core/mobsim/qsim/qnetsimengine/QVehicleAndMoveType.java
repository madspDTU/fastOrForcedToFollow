package org.matsim.core.mobsim.qsim.qnetsimengine;

import java.util.Collection;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.framework.DriverAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.PassengerAgent;
import org.matsim.vehicles.Vehicle;

public class QVehicleAndMoveType implements QVehicle{
	
	private QVehicle veh;
	private QFFFNode.MoveType moveType;
	private int outDirection;
	
	public QVehicleAndMoveType(final QVehicle veh, final QFFFNode.MoveType moveType, int outDirection) {
		this.veh = veh;
		this.moveType = moveType;
		this.outDirection = outDirection;
	}

	
	public QVehicle getQVehicle() {
		return veh;
	}

	public QFFFNode.MoveType getMoveType() {
		return moveType;
	}

	@Override
	public double getEarliestLinkExitTime() {
		return veh.getEarliestLinkExitTime();
	}

	@Override
	public void setEarliestLinkExitTime(double earliestLinkEndTime) {
		veh.setEarliestLinkExitTime(earliestLinkEndTime);
	}

	@Override
	public Vehicle getVehicle() {
		return veh.getVehicle();
	}

	@Override
	public MobsimDriverAgent getDriver() {
		return veh.getDriver();
	}

	@Override
	public Id<Vehicle> getId() {
		return veh.getId();
	}

	@Override
	public Link getCurrentLink() {
		return veh.getCurrentLink();
	}

	@Override
	public boolean addPassenger(PassengerAgent passenger) {
		return veh.addPassenger(passenger);
	}

	@Override
	public boolean removePassenger(PassengerAgent passenger) {
		return veh.removePassenger(passenger);
	}

	@Override
	public Collection<? extends PassengerAgent> getPassengers() {
		return veh.getPassengers();
	}

	@Override
	public int getPassengerCapacity() {
		return veh.getPassengerCapacity();
	}

	@Override
	public void setCurrentLink(Link link) {
		veh.setCurrentLink(link);
	}

	@Override
	public void setDriver(DriverAgent driver) {
		veh.setDriver(driver);
	}

	@Override
	public double getLinkEnterTime() {
		return veh.getLinkEnterTime();
	}

	@Override
	public void setLinkEnterTime(double linkEnterTime) {
		veh.setLinkEnterTime(linkEnterTime);
	}

	@Override
	public double getMaximumVelocity() {
		return veh.getMaximumVelocity();
	}

	@Override
	public double getSizeInEquivalents() {
		return veh.getSizeInEquivalents();
	}

	public int getOutDirection() {
		return outDirection;
	}
	
	

}
