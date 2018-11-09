package org.matsim.core.mobsim.qsim.qnetsimengine;

import fastOrForcedToFollow.BicycleVehicle;
import fastOrForcedToFollow.Cyclist;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.mobsim.framework.DriverAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.PassengerAgent;
import org.matsim.run.RunMatsim;
import org.matsim.vehicles.Vehicle;

import java.util.Collection;
import java.util.LinkedList;

import static fastOrForcedToFollow.Runner.theta_0;
import static fastOrForcedToFollow.Runner.theta_1;

public class QCycleAsVehicle implements QVehicle
{
	QVehicle qVehicle  ;
	Cyclist cyclist ;
	
	//mads: Should be alright now. s
	
	public QCycleAsVehicle( Vehicle basicVehicle,  Person person) {
		final String id = basicVehicle.getId().toString();
		this.qVehicle = new QVehicleImpl(new BicycleVehicle(id));
		
		final double desiredSpeed = (double) person.getAttributes().getAttribute(RunMatsim.DESIRED_SPEED);
		final double z_c = (double) person.getAttributes().getAttribute(RunMatsim.HEADWAY_DISTANCE_PREFERENCE);
		final LinkedList<fastOrForcedToFollow.Link> route = null ; // don't need there here; will come from framework
		this.cyclist = Cyclist.createIndividualisedCyclist(id, desiredSpeed, z_c, route);
	}
	
	public QCycleAsVehicle(Cyclist cyclist){
		this.qVehicle = new QVehicleImpl(new BicycleVehicle(cyclist.getId())) ;
		this.cyclist = cyclist;
	}
	
	public Cyclist getCyclist() {
		return this.cyclist;
	}
	

	
	
	@Override public double getLinkEnterTime() {
		return qVehicle.getLinkEnterTime();
	}
	
	@Override public void setLinkEnterTime( final double linkEnterTime ) {
		qVehicle.setLinkEnterTime( linkEnterTime );
	}
	
	@Override public double getMaximumVelocity() {
		return qVehicle.getMaximumVelocity() ;
		//this.cyclist.getDesiredSpeed();
	}
	
	@Override public double getFlowCapacityConsumptionInEquivalents() {
		return qVehicle.getFlowCapacityConsumptionInEquivalents() ;
	}
	
	@Override public double getEarliestLinkExitTime() {
		return qVehicle.getEarliestLinkExitTime();
		//this.cyclist.getTEarliestExit();
	}
	
	@Override public void setEarliestLinkExitTime( final double earliestLinkEndTime ) {
		qVehicle.setEarliestLinkExitTime( earliestLinkEndTime );
		//this.cyclist.setTEarliestExit(earliestLinkEndTime);
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
		//	this.cyclist.getCurrentLink();
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
		//this.cyclist.setCurrentLink(link);
	}
	
	@Override public void setDriver( final DriverAgent driver ) {
		qVehicle.setDriver( driver );
	}
	
	
}
