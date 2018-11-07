package org.matsim.core.mobsim.qsim.qnetsimengine;

import fastOrForcedToFollow.Cyclist;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.framework.DriverAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.PassengerAgent;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicleImpl;
import org.matsim.vehicles.Vehicle;

import java.util.Collection;

public class QCycleAsVehicle implements QVehicle
{
	QVehicle qVehicle  ;
	
	public QCycleAsVehicle() {
		final Vehicle basicVehicle;
		this.qVehicle = new QVehicleImpl(basicVehicle) ;
		this.cyclist = ... ;
	}
	
	public Cyclist getCyclist() {
		return cyclist;
	}
	
	Cyclist cyclist ;
	
	@Override public double getLinkEnterTime() {
		return qVehicle.getLinkEnterTime();
	}
	
	@Override public void setLinkEnterTime( final double linkEnterTime ) {
		qVehicle.setLinkEnterTime( linkEnterTime );
	}
	
	@Override public double getEarliestLinkExitTime() {
		return qVehicle.getEarliestLinkExitTime();
	}
	
	@Override public void setEarliestLinkExitTime( final double earliestLinkEndTime ) {
		qVehicle.setEarliestLinkExitTime( earliestLinkEndTime );
	}
	
	@Override public double getSizeInEquivalents() {
		return qVehicle.getSizeInEquivalents();
	}
	
	@Override public Vehicle getVehicle() {
		return qVehicle.getVehicle();
	}
	
	@Override public MobsimDriverAgent getDriver() {
		return qVehicle.getDriver();
	}
	
	@Override public Id<Vehicle> getId() {
		return qVehicle.getId();
	}
	
	@Override public Link getCurrentLink() {
		return qVehicle.getCurrentLink();
	}
	
	@Override public boolean addPassenger( final PassengerAgent passenger ) {
		return qVehicle.addPassenger( passenger );
	}
	
	@Override public boolean removePassenger( final PassengerAgent passenger ) {
		return qVehicle.removePassenger( passenger );
	}
	
	@Override public Collection<? extends PassengerAgent> getPassengers() {
		return qVehicle.getPassengers();
	}
	
	@Override public int getPassengerCapacity() {
		return qVehicle.getPassengerCapacity();
	}
	
	@Override public void setCurrentLink( final Link link ) {
		qVehicle.setCurrentLink( link );
	}
	
	@Override public void setDriver( final DriverAgent driver ) {
		qVehicle.setDriver( driver );
	}
	
	
}
