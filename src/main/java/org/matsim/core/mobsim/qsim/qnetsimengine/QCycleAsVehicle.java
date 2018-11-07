package org.matsim.core.mobsim.qsim.qnetsimengine;

import fastOrForcedToFollow.Cyclist;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.framework.DriverAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.PassengerAgent;
import org.matsim.vehicles.Vehicle;

import java.util.Collection;
import java.util.LinkedList;

import static fastOrForcedToFollow.Runner.theta_0;
import static fastOrForcedToFollow.Runner.theta_1;

public class QCycleAsVehicle implements QVehicle
{
	QVehicle qVehicle  ;
	
	public QCycleAsVehicle( Vehicle basicVehicle) {
		this.qVehicle = new QVehicleImpl(basicVehicle) ;
		
		final String id = basicVehicle.getId().toString() ;
		final double desiredSpeed = 13.0 ; // yyyy need to get from person
		final LinkedList<fastOrForcedToFollow.Link> route = null ; // don't need there here; will come from framework
		this.cyclist = new Cyclist(id, desiredSpeed, theta_0, theta_1, route) ;
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
	
	@Override public double getMaximumVelocity() {
		return qVehicle.getMaximumVelocity() ;
	}
	
	@Override public double getFlowCapacityConsumptionInEquivalents() {
		return qVehicle.getFlowCapacityConsumptionInEquivalents() ;
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
