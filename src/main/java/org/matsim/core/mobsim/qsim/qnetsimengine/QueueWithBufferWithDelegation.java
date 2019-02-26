package org.matsim.core.mobsim.qsim.qnetsimengine;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.interfaces.SignalGroupState;
import org.matsim.core.mobsim.qsim.interfaces.SignalizeableItem;
import org.matsim.lanes.Lane;
import org.matsim.vehicles.Vehicle;

import java.util.Collection;

class QueueWithBufferWithDelegation implements QLaneI, SignalizeableItem{

	// replace inheritance by delegation (or composition), "Effective Java".
	// design for inheritance of prohibit it.   Design for it: public methods should be final or empty/abstract.

	QueueWithBuffer delegate ;

	QueueWithBufferWithDelegation() {
//		delegate = new QueueWithBuffer(qlink, vehicleQueue, laneId, length, effectiveNumberOfLanes, flowCapacity_s, context) ;
	}

	@Override public void setSignalStateAllTurningMoves( SignalGroupState state ){ delegate.setSignalStateAllTurningMoves( state ); }
	@Override public void recalcTimeVariantAttributes(){ delegate.recalcTimeVariantAttributes(); }
	@Override public void setSignalStateForTurningMove( SignalGroupState state, Id<Link> toLinkId ){ delegate.setSignalStateForTurningMove( state, toLinkId ); }

	@Override
	public boolean hasGreenForToLink( Id<Link> toLinkId ){
		return delegate.hasGreenForToLink( toLinkId );
	}

	@Override
	public boolean hasGreenForAllToLinks(){
		return delegate.hasGreenForAllToLinks();
	}

	@Override
	public void setSignalized( boolean isSignalized ){
		delegate.setSignalized( isSignalized );
	}

	@Override
	public Id<Lane> getId(){
		return delegate.getId();
	}

	@Override
	public double getLoadIndicator(){
		return delegate.getLoadIndicator();
	}

	@Override
	public void addFromWait( QVehicle veh ){
		delegate.addFromWait( veh );
	}

	@Override
	public boolean isAcceptingFromWait( QVehicle veh ){
		return delegate.isAcceptingFromWait( veh );
	}

	@Override
	public boolean isActive(){
		return delegate.isActive();
	}

	@Override
	public double getSimulatedFlowCapacityPerTimeStep(){
		return delegate.getSimulatedFlowCapacityPerTimeStep();
	}

	@Override
	public QVehicle getVehicle( Id<Vehicle> vehicleId ){
		return delegate.getVehicle( vehicleId );
	}

	@Override
	public double getStorageCapacity(){
		return delegate.getStorageCapacity();
	}

	@Override
	public VisData getVisData(){
		return delegate.getVisData();
	}

	@Override
	public void addTransitSlightlyUpstreamOfStop( QVehicle veh ){
		delegate.addTransitSlightlyUpstreamOfStop( veh );
	}

	@Override
	public void changeUnscaledFlowCapacityPerSecond( double val ){
		delegate.changeUnscaledFlowCapacityPerSecond( val );
	}

	@Override
	public void changeEffectiveNumberOfLanes( double val ){
		delegate.changeEffectiveNumberOfLanes( val );
	}

	@Override
	public boolean doSimStep(){
		return delegate.doSimStep();
	}

	@Override
	public void clearVehicles(){
		delegate.clearVehicles();
	}

	@Override
	public Collection<MobsimVehicle> getAllVehicles(){
		return delegate.getAllVehicles();
	}

	@Override
	public void addFromUpstream( QVehicle veh ){
		delegate.addFromUpstream( veh );
	}

	@Override
	public boolean isNotOfferingVehicle(){
		return delegate.isNotOfferingVehicle();
	}

	@Override
	public QVehicle popFirstVehicle(){
		return delegate.popFirstVehicle();
	}

	@Override
	public QVehicle getFirstVehicle(){
		return delegate.getFirstVehicle();
	}

	@Override
	public double getLastMovementTimeOfFirstVehicle(){
		return delegate.getLastMovementTimeOfFirstVehicle();
	}

	@Override
	public boolean isAcceptingFromUpstream(){
		return delegate.isAcceptingFromUpstream();
	}

	@Override
	public void initBeforeSimStep(){
		delegate.initBeforeSimStep();
	}



}
