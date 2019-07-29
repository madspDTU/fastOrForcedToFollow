package org.matsim.core.mobsim.qsim.qnetsimengine;

import fastOrForcedToFollow.Cyclist;
import fastOrForcedToFollow.Sublink;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.lanes.Lane;
import org.matsim.vehicles.Vehicle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.PriorityQueue;

class QCycleLaneWithoutCongestion implements QLaneI{
	private static final Logger log = Logger.getLogger( QCycleLaneWithoutCongestion.class ) ;

	private final Id<Lane> id;
	private final AbstractQLink qLinkImpl;
	private final PriorityQueue<QCycle> globalQ;
	private final NetsimEngineContext context;
	private double lastTimeMoved;
	private LinkedList<QVehicle> leavingVehicles;

	public QCycleLaneWithoutCongestion(String id, AbstractQLink qLinkImpl, NetsimEngineContext context){
		this.qLinkImpl = qLinkImpl;
		this.id = Id.create(id.substring(0, id.toString().length() - 6), Lane.class);
		this.globalQ = new PriorityQueue<>( new Comparator<QCycle>(){
			@Override
			public int compare( QCycle qc1, QCycle qc2 ){
				return Double.compare(qc1.getCyclist().getTEarliestExit(), qc2.getCyclist().getTEarliestExit());
			}
		} ) ;
		this.context = context;
		this.lastTimeMoved = 0.;
		leavingVehicles = new LinkedList<QVehicle>();
	}


	@Override public Id<Lane> getId() { //Done!?
		return id;
	}

	@Override public boolean isAcceptingFromUpstream() { //Done!
		return true;
	}

	@Override public void addFromUpstream( final QVehicle veh ) {  
		// activate link since there is now action on it:
		qLinkImpl.getInternalInterface().activateLink();

		veh.setCurrentLink( qLinkImpl.getLink() );

		// upcast:
		QCycle qCyc = (QCycle) veh;

		// get the Cyclist out of it:
		Cyclist cyclist = qCyc.getCyclist();

		double vTilde = cyclist.getDesiredSpeed();
		cyclist.setSpeed(vTilde);

		double tStart = cyclist.getTEarliestExit() ;

		// Calculating earliest possible exit of the link:
		final double tEarliestExit = tStart + qLinkImpl.getLink().getLength() / vTilde;
		cyclist.setTEarliestExit( tEarliestExit );

		// Add qCycle to the downstream queue of the next link.
		//	fffLinkArray[0].getOutQ().add(qCyc ); 
		globalQ.add(qCyc);

	}


	/**
	 * Auxiliary method that can be used for logging/printing cyclist delays on individual links.
	 * @param vTilde
	 * @param cyclist
	 */
	//    private void printDelay(Cyclist cyclist) {
	//    	double vTilde = cyclist.getSpeed();
	//        if(vTilde + 0.00001 < cyclist.getDesiredSpeed()){
	//            log.info("Cyclist "+ cyclist.getId() + " riding " + String.format("%.1f",
	//                     (cyclist.getDesiredSpeed() - vTilde)/cyclist.getDesiredSpeed()*100d )
	//                     + "% slower on link " + fffLink.getId() );
	//        }
	//    }

	@Override public boolean doSimStep() {
		QCycle cqo;

		while((cqo = globalQ.peek()) != null){

			double tEarliestExit = cqo.getEarliestLinkExitTime();
			if( tEarliestExit > context.getSimTimer().getTimeOfDay()){
				break;
			}

			globalQ.remove();


			if(cqo.getDriver().isWantingToArriveOnCurrentLink()){
				qLinkImpl.letVehicleArrive(cqo );
				continue;
			}


			//Auxiliary buffer created to fit the piece into MATSim.
			leavingVehicles.add(cqo);
			lastTimeMoved = tEarliestExit;


			final QNodeI toNode = qLinkImpl.getToNode();
			if ( toNode instanceof QNodeImpl ) {
				((QNodeImpl) toNode).activateNode();
			}
		}
	return true;
}

@Override public boolean isNotOfferingVehicle() {
	return leavingVehicles.isEmpty();

}

@Override public QVehicle popFirstVehicle() {
	return leavingVehicles.pollFirst();
}

@Override public QVehicle getFirstVehicle() {
	return leavingVehicles.peekFirst();
}

@Override public boolean isAcceptingFromWait( final QVehicle veh ) {
	// use same logic as inserting from upstream:
	return this.isAcceptingFromUpstream() ;

}

@Override public void addFromWait( final QVehicle veh ) {

	// ensuring that the first provisional earliest link exit cannot be before now.
	double now = context.getSimTimer().getTimeOfDay() ;
	QCycle qCyc = (QCycle) veh;
	qCyc.getCyclist().setTEarliestExit( now );
	
	
	if(qCyc.getDriver().isWantingToArriveOnCurrentLink()){
		qLinkImpl.letVehicleArrive(qCyc);
		return;
	}
	//Auxiliary buffer created to fit the piece into MATSim.
	leavingVehicles.add(qCyc);
	lastTimeMoved = now;


	final QNodeI toNode = qLinkImpl.getToNode();
	if ( toNode instanceof QNodeImpl ) {
		((QNodeImpl) toNode).activateNode();
	}
}

@Override public boolean isActive() {
	if(globalQ.isEmpty() && isNotOfferingVehicle()){
		return false;
	} else {
		return true;
	}
}

@Override public double getSimulatedFlowCapacityPerTimeStep() {
	throw new RuntimeException( "not implemented" );
}

@Override public void recalcTimeVariantAttributes() {
	throw new RuntimeException( "not implemented" );
}

@Override public QVehicle getVehicle( final Id<Vehicle> vehicleId ) {
	for(QCycle cqo : globalQ){
		if( cqo.getVehicle().getId().equals( vehicleId ) ){
			return cqo;
		}
	}
	return null ;
}

@Override public double getStorageCapacity() {
	throw new RuntimeException( "not implemented" );
}

@Override public VisData getVisData() {
	throw new RuntimeException( "not implemented" );
}

@Override public void addTransitSlightlyUpstreamOfStop( final QVehicle veh ) {
	this.addFromUpstream(veh);
	//Not sure about that
}

@Override public void changeUnscaledFlowCapacityPerSecond( final double val ) {
	throw new RuntimeException( "not implemented" );
}

@Override public void changeEffectiveNumberOfLanes( final double val ) {
	throw new RuntimeException( "not implemented" );
}


@Override public void clearVehicles() {
	globalQ.clear();
}

@Override public Collection<MobsimVehicle> getAllVehicles() {
	ArrayList<MobsimVehicle> qCycs = new ArrayList<MobsimVehicle>();
	for(QCycle cqo : globalQ){
		qCycs.add(cqo);
	}
	return qCycs;
}

@Override public double getLastMovementTimeOfFirstVehicle() {
	return lastTimeMoved;
}

@Override public double getLoadIndicator() {
	throw new RuntimeException( "not implemented" );
}

@Override public void initBeforeSimStep() {
	//Intentionally empty
}
}
