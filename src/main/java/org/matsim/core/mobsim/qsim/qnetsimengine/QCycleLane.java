package org.matsim.core.mobsim.qsim.qnetsimengine;

import fastOrForcedToFollow.Cyclist;
import fastOrForcedToFollow.Link;
import fastOrForcedToFollow.PseudoLane;
import fastOrForcedToFollow.Runner;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.lanes.Lane;
import org.matsim.vehicles.Vehicle;

import java.util.ArrayList;
import java.util.Collection;

class QCycleLane implements QLaneI{
    private static final Logger log = Logger.getLogger( QCycleLane.class ) ;

    private final fastOrForcedToFollow.Link fffLink;
    private final AbstractQLink qLinkImpl;
    private final NetsimEngineContext context;

    public QCycleLane( Link fffLink, AbstractQLink qLinkImpl, NetsimEngineContext context ){
        this.fffLink = fffLink;
        this.qLinkImpl = qLinkImpl;
        this.context = context;
    }

    @Override public Id<Lane> getId() {
        return Id.create( fffLink.getId(), Lane.class ) ;
    }

    @Override public boolean isAcceptingFromUpstream() {
        return !fffLink.isLinkFull();
        // In contrast to the standAlone version, here we alllow the space to be
        // exceeded, but will subsequently disallow movements when exceeded
        // (Basically corresponding to allowing 1 extra bicycle).
    }

    @Override public void addFromUpstream( final QVehicle veh ) {
        // activate link since there is now action on it:
        qLinkImpl.getInternalInterface().activateLink();

        veh.setCurrentLink( qLinkImpl.getLink() );

        // upcast:
        QCycle qCyc = (QCycle) veh;

        // get the Cyclist out of it:
        Cyclist cyclist = qCyc.getCyclist();

        // internal fff logic:
        
        	// Selecting the appropriate pseudoLane:
        PseudoLane pseudoLane = cyclist.selectPseudoLane( fffLink );

        	// Assigning a provisional, maximum speed for this link:
        double vTilde = cyclist.getVMax(pseudoLane);
        cyclist.setSpeed(vTilde);
        
        		//					 printDelay(cyclist);

        	// The time at which the tip of the cyclist enters the beginning of the link:
        double tStart = Double.max(pseudoLane.getTReady(), cyclist.getTEarliestExit()) ;
         
        	// Calculating earliest possible exit of the link:
        final double tEarliestExit = tStart + pseudoLane.getLength() / vTilde;
        cyclist.setTEarliestExit( tEarliestExit );
        
        	// Increasing the occupied space on link:
        fffLink.increaseOccupiedSpace(cyclist, vTilde );
        
        	// Updating tReady and tExit of the link:
       double tOneBicycleLength = cyclist.getBicycleLength() / vTilde;
       pseudoLane.setTReady(tStart + tOneBicycleLength);
       pseudoLane.setTEnd(cyclist.getTEarliestExit() + tOneBicycleLength);
    

      		// Add qCycle to the downstream queue of the next link.
        fffLink.getOutQ().add(qCyc );
  
    }

    
//    /**
//     * Auxiliary method that can be used for logging/printing cyclist delays on individual links.
//     * @param vTilde
//     * @param cyclist
//     */
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
        while((cqo = fffLink.getOutQ().peek()) != null){

            //								log.debug( "now=" + context.getSimTimer().getTimeOfDay() + "; linkId=" + fffLink.getId() + outqAsString( fffLink.getOutQ() ) );

            final double tEarliestExit = cqo.getEarliestLinkExitTime();
            if( tEarliestExit > context.getSimTimer().getTimeOfDay()){
                break;
            }
            fffLink.getOutQ().remove();
            fffLink.reduceOccupiedSpace(cqo.getCyclist(), cqo.getCyclist().getSpeed() );

            if(cqo.getDriver().isWantingToArriveOnCurrentLink()){
                qLinkImpl.letVehicleArrive(cqo );
                continue;
            }

            //Auxiliary buffer created to fit the piece into MATSim.
            fffLink.addVehicleToLeavingVehicles(cqo );
            fffLink.setLastTimeMoved(tEarliestExit);
            

            final QNodeI toNode = qLinkImpl.getToNode();
            if ( toNode instanceof QNodeImpl ) {
                ((QNodeImpl) toNode).activateNode();
            }
        }
        return true;
    }

    @Override public boolean isNotOfferingVehicle() {
        return fffLink.hasNoLeavingVehicles();

    }

    @Override public QVehicle popFirstVehicle() {
        return fffLink.pollFirstLeavingVehicle();
    }

    @Override public QVehicle getFirstVehicle() {
        return fffLink.getFirstLeavingVehicle();
    }

    @Override public boolean isAcceptingFromWait( final QVehicle veh ) {
        // use same logic as inserting from upstream:
        return this.isAcceptingFromUpstream() ;

    }

    @Override public void addFromWait( final QVehicle veh ) {

        // ensuring that the first provisional earliest link exit cannot be before now.
        double now = context.getSimTimer().getTimeOfDay() ;
        ((QCycle) veh).getCyclist().setTEarliestExit( now );

        // just inserting them upstream.  For the time being, but might also be ok in the long run.
        this.addFromUpstream( veh );
    }

    @Override public boolean isActive() {
        return !fffLink.getOutQ().isEmpty();
    }

    @Override public double getSimulatedFlowCapacityPerTimeStep() {
        throw new RuntimeException( "not implemented" );
    }

    @Override public void recalcTimeVariantAttributes() {
        throw new RuntimeException( "not implemented" );
    }

    @Override public QVehicle getVehicle( final Id<Vehicle> vehicleId ) {
        for(QCycle cqo : fffLink.getOutQ()){
            if( cqo.getCyclist().getId().equals( vehicleId.toString() ) ){
                return cqo ;
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
        fffLink.getOutQ().clear();
    }

    @Override public Collection<MobsimVehicle> getAllVehicles() {
        ArrayList<MobsimVehicle> qCycs = new ArrayList<MobsimVehicle>();
        for(QCycle cqo : fffLink.getOutQ()){
            qCycs.add(cqo);
        }
        return qCycs;
    }

    @Override public double getLastMovementTimeOfFirstVehicle() {
        return fffLink.getLastTimeMoved();
    }

    @Override public double getLoadIndicator() {
        throw new RuntimeException( "not implemented" );
    }

    @Override public void initBeforeSimStep() {
        //Intentionally empty
    }
}
