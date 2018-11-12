package org.matsim.core.mobsim.qsim.qnetsimengine;

import fastOrForcedToFollow.BicycleVehicle;
import fastOrForcedToFollow.Cyclist;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicleImpl;
import org.matsim.core.mobsim.framework.DriverAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.PassengerAgent;
import org.matsim.run.RunMatsim;
import org.matsim.vehicles.Vehicle;

import java.util.Collection;

public class QCycleAsVehicle implements QVehicle
{
//	public static class Factory implements QVehicleFactory {
//		@Override public QVehicle createQVehicle( Vehicle vehicle ){
//			QVehicle qvehicle ;
//			if ( leg.getMode().equals( TransportMode.bike ) ) {
//				qvehicle = new QCycleAsVehicle( vehicle, person ) ;
//			} else {
//				qvehicle = new QVehicleImpl( vehicle ) ;
//			}
//			return qvehicle ;
//		}
//	}


	QVehicle qVehicle  ;
	Cyclist cyclist ;

	public QCycleAsVehicle( Vehicle basicVehicle,  Person person) {
		final String id = basicVehicle.getId().toString();
		this.qVehicle = new QVehicleImpl(new BicycleVehicle(id));
		
		final double desiredSpeed = (double) person.getAttributes().getAttribute(RunMatsim.DESIRED_SPEED);
		final double z_c = (double) person.getAttributes().getAttribute(RunMatsim.HEADWAY_DISTANCE_PREFERENCE);
		this.cyclist = Cyclist.createIndividualisedCyclist(id, desiredSpeed, z_c);
	}
	
	public QCycleAsVehicle(Cyclist cyclist){
		this.qVehicle = new QVehicleImpl(new BicycleVehicle(cyclist.getId())) ;
		this.cyclist = cyclist;
	}
	
	public Cyclist getCyclist() {
		return this.cyclist;
	}

	public QVehicle getQVehicle(){
		return this.qVehicle;
	}

	
	
	@Override public double getLinkEnterTime() {
		//Uses cyclist's value
		return this.getCyclist().getTStart();
	}
	
	@Override public void setLinkEnterTime( final double linkEnterTime ) {
		//Uses cyclist's value
		this.getCyclist().setTStart( linkEnterTime );
	}
	
	@Override public double getMaximumVelocity() {
		//Uses cyclist's value;
		return this.getCyclist().getDesiredSpeed();
	}
	
	@Override public double getFlowCapacityConsumptionInEquivalents() {
		return qVehicle.getFlowCapacityConsumptionInEquivalents() ;
	}
	
	@Override public double getEarliestLinkExitTime() {
		//Uses cyclist's value
		return this.getCyclist().getTEarliestExit();
	}
	
	@Override public void setEarliestLinkExitTime( final double earliestLinkEndTime ) {
		//Uses cyclist's value
		this.cyclist.setTEarliestExit(earliestLinkEndTime);
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
		//Cannot (straightforwardly) use cyclist's value, because that Link is of another type.
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
		//See getCurrentLink regarind inheritance from cyclist.
	}
	
	@Override public void setDriver( final DriverAgent driver ) {
		qVehicle.setDriver( driver );
	}
	
	
}
