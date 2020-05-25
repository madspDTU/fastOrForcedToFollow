package org.matsim.core.mobsim.qsim.qnetsimengine;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup.LinkDynamics;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.interfaces.SignalGroupState;
import org.matsim.core.mobsim.qsim.interfaces.SignalizeableItem;
import org.matsim.core.mobsim.qsim.pt.TransitDriverAgent;
import org.matsim.core.mobsim.qsim.qnetsimengine.AbstractQLink.HandleTransitStopResult;
import org.matsim.core.mobsim.qsim.qnetsimengine.AbstractQLink.QLinkInternalInterface;
import org.matsim.core.mobsim.qsim.qnetsimengine.QLinkImpl.LaneFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.flow_efficiency.DefaultFlowEfficiencyCalculator;
import org.matsim.core.mobsim.qsim.qnetsimengine.flow_efficiency.FlowEfficiencyCalculator;
import org.matsim.core.mobsim.qsim.qnetsimengine.vehicleq.FIFOVehicleQ;
import org.matsim.core.mobsim.qsim.qnetsimengine.vehicleq.PassingVehicleQ;
import org.matsim.core.mobsim.qsim.qnetsimengine.vehicleq.VehicleQ;
import org.matsim.core.utils.misc.Time;
import org.matsim.lanes.Lane;
import org.matsim.vehicles.Vehicle;
import org.matsim.vis.snapshotwriters.VisData;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

class QueueWithBufferWithDelegation implements QLaneI, SignalizeableItem{

	// replace inheritance by delegation (or composition), "Effective Java".
	// design for inheritance of prohibit it.   Design for it: public methods should be final or empty/abstract.

	QueueWithBuffer delegate;
	private final VehicleQ<QVehicle> vehQueue;
	private final Queue<QVehicle> generalBuffer;
	private final Queue<QVehicle> leftBuffer;
	private NetsimEngineContext context;
	private QLinkInternalInterface qLink;
	private double bufferLastMovedTime;
	private FlowEfficiencyCalculator flowEfficiencyCalculator;

	private static class FlowcapAccumulate {
		private double timeStep = 0.;//Double.NEGATIVE_INFINITY ;
		private double value = 0. ;
		private double getTimeStep(){
			return this.timeStep;
		}
		private void setTimeStep(double now) {
			this.timeStep = now;
		}
		private double getValue() {
			return value;
		}
		private void setValue(double value ) {
			this.value = value;
		}
		private void addValue(double value1, double now) {
			this.value += value1;
			this.timeStep = now ;
		}
	}
	private final FlowcapAccumulate flowcap_accumulate = new FlowcapAccumulate() ;
	// might be changed back to standard double after all of this was figured out. kai, sep'14
	private double unscaledFlowCapacity_s;
	private double flowCapacityPerTimeStep;
	private double usedStorageCapacity;




	static final class Builder implements LaneFactory {
		private final NetsimEngineContext context;
		Builder( final NetsimEngineContext context ) {
			this.context = context ;
		}
		@Override public QueueWithBufferWithDelegation createLane( AbstractQLink qLink ) {
			// a number of things I cannot configure before I have the qlink:
			QueueWithBuffer.Builder delegateBuilder = new QueueWithBuffer.Builder(context);
			QueueWithBuffer delegate = delegateBuilder.createLane(qLink);
			return new QueueWithBufferWithDelegation(delegate, context, qLink.getInternalInterface());
		} ;
	}


	private QueueWithBufferWithDelegation(QueueWithBuffer delegate, NetsimEngineContext context, QLinkInternalInterface qLinkII) {
		this.vehQueue = new FIFOVehicleQ() ;
		this.delegate = delegate;
		this.context = context;
		this.qLink = qLinkII;
		this.bufferLastMovedTime = Time.getUndefinedTime();
		this.generalBuffer = new LinkedList<QVehicle>();
		this.leftBuffer = new LinkedList<QVehicle>();
		this.flowEfficiencyCalculator = new DefaultFlowEfficiencyCalculator();
		this.unscaledFlowCapacity_s = ((Link)qLink.getLink()).getFlowCapacityPerSec();
		this.flowCapacityPerTimeStep = this.unscaledFlowCapacity_s * context.qsimConfig.getTimeStepSize() * context.qsimConfig.getFlowCapFactor() ;
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

	//NotTheSame
	@Override
	public void addFromWait( QVehicle veh ){
		addToBuffer(veh);
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
		this.vehQueue.addFirst(veh) ;
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
	public double getLastMovementTimeOfFirstVehicle(){
		return bufferLastMovedTime;
	}

	@Override
	public boolean isAcceptingFromUpstream(){
		return delegate.isAcceptingFromUpstream();
	}

	@Override
	public void initBeforeSimStep(){
		delegate.initBeforeSimStep();
		updateSlowFlowAccumulation();
	}


	@Override
	public final QVehicle getFirstVehicle() {
		if (!this.generalBuffer.isEmpty()) {
			return getFirstGeneralVehicle();
		}
		if (!this.leftBuffer.isEmpty()){
			return getFirstLeftVehicle();
		} 
		return vehQueue.peek();
	}

	public final QVehicle getFirstGeneralVehicle() {
		return this.generalBuffer.peek() ;
	}
	public final QVehicle getFirstLeftVehicle() {
		return this.leftBuffer.peek() ;
	}

	private void addToBuffer(final QVehicle veh) {

		// yy might make sense to just accumulate to "zero" and go into negative when something is used up.
		// kai/mz/amit, mar'12
		double now = context.getSimTimer().getTimeOfDay() ;
		flowcap_accumulate.addValue(-getFlowCapacityConsumptionInEquivalents(veh), now);

		Id<Link> nextLinkId = veh.getDriver().chooseNextLinkId();
		QFFFNode toQFFFNode = (QFFFNode) this.qLink.getToNodeQ();
		QFFFAbstractNode toQFFFAbstractNode = toQFFFNode.getQFFFAbstractNode();
		// Left buffer
		if(toQFFFAbstractNode instanceof QFFFNodeDirectedPriorityNode && 
				((QFFFNodeDirectedPriorityNode) toQFFFAbstractNode).isCarLeftTurn(this.qLink.getId(), nextLinkId) ){
			if(generalBuffer.isEmpty() && leftBuffer.isEmpty()){
				bufferLastMovedTime = now;
			}
			leftBuffer.add(veh);
		} else { 	// General buffer
			if(generalBuffer.isEmpty() && leftBuffer.isEmpty()){
				bufferLastMovedTime = now;
			}
			generalBuffer.add(veh);
		}
		toQFFFNode.activateNode();
	}


	private double getFlowCapacityConsumptionInEquivalents(QVehicle vehicle) {
		double flowEfficiency = flowEfficiencyCalculator.calculateFlowEfficiency(vehicle.getVehicle(), qLink.getLink());
		return vehicle.getSizeInEquivalents() / flowEfficiency;
	}


	private void updateSlowFlowAccumulation(){
		if (this.flowcap_accumulate.getValue() < flowCapacityPerTimeStep && isNotOfferingVehicle() ){
			double newFlowCap = Math.min(flowcap_accumulate.getValue() + flowCapacityPerTimeStep,
					flowCapacityPerTimeStep);
			flowcap_accumulate.setValue(newFlowCap);
		}
	}

	private QVehicle peekFromVehQueue(){
		return vehQueue.peek();
	}


	@Override
	public boolean doSimStep() {
		double now = context.getSimTimer().getTimeOfDay() ;

		QVehicle veh;
		while((veh = peekFromVehQueue()) !=null){
			//we have an original QueueLink behaviour
			if (veh.getEarliestLinkExitTime() > now){
				break;
			}

			MobsimDriverAgent driver = veh.getDriver();

			if (driver instanceof TransitDriverAgent) {
				HandleTransitStopResult handleTransitStop = qLink.handleTransitStop(
						now, veh, (TransitDriverAgent) driver, this.qLink.getId()
						);
				if (handleTransitStop == HandleTransitStopResult.accepted) {
					// vehicle has been accepted into the transit vehicle queue of the link.
					removeVehicleFromQueue(veh) ;
					continue;
				} else if (handleTransitStop == HandleTransitStopResult.rehandle) {
					continue; // yy why "continue", and not "break" or "return"?  Seems to me that this
					// is currently only working because qLink.handleTransitStop(...) also increases the
					// earliestLinkExitTime for the present vehicle.  kai, oct'13
					// zz From my point of view it is exactly like described above. dg, mar'14
					//				} else if (handleTransitStop == HandleTransitStopResult.continue_driving) {
					// Do nothing, but go on..
				}
			}

			// Check if veh has reached destination:
			if ( driver.isWantingToArriveOnCurrentLink() ) {
				qLink.letVehicleArrive( veh );

				// remove _after_ processing the arrival to keep link active:
				vehQueue.remove( veh ) ;

				continue;
			}

			/* is there still any flow capacity left? */
			if (!hasFlowCapacityLeft(veh) ) {
				break;
			}

			addToBuffer(veh);
			
			//Event here....
			
			removeVehicleFromQueue(veh);
			if(context.qsimConfig.isRestrictingSeepage()
					&& context.qsimConfig.getLinkDynamics()==LinkDynamics.SeepageQ
					&& context.qsimConfig.getSeepModes().contains(veh.getDriver().getMode()) ) {
			}
		} // end while
		return true;
	}



	private boolean hasFlowCapacityLeft(QVehicle veh) {
		return flowcap_accumulate.getValue() > 0.0 || veh.getVehicle().getType()
				.getPcuEquivalents() <= context.qsimConfig.getPcuThresholdForFlowCapacityEasing();
	}




	private void removeVehicleFromQueue(QVehicle veh) {
		vehQueue.remove(veh);
		usedStorageCapacity -= veh.getSizeInEquivalents();
	}
}
