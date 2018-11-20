package org.matsim.core.mobsim.qsim.qnetsimengine;

import fastOrForcedToFollow.Cyclist;
import fastOrForcedToFollow.Link;
import fastOrForcedToFollow.PseudoLane;
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
        return fffLink.getOccupiedSpace() < fffLink.getTotalLaneLength();
        // In contrast to the standAlone version, here we alllow the space to be
        // exceeded, but will subsequently disallow movements when exceeded
        // (Basically corresponding to allowing 1 extra bicycle).
    }

    @Override public void addFromUpstream( final QVehicle veh ) {
        //		log.warn( "addFromWait/Upstream: now=" + context.getSimTimer().getTimeOfDay() + "; linkId=" + fffLink.getId() + "; adding: vehId=" + veh.getId() );

        // activate link since there is now action on it:
        qLinkImpl.getInternalInterface().activateLink();

        veh.setCurrentLink( qLinkImpl.getLink() );

        // upcast:
        QCycle qCyc = (QCycle) veh;

        // get the Cyclist out of it:
        Cyclist cyclist = qCyc.getCyclist();

        // internal fff logic:
        PseudoLane pseudoLane = cyclist.selectPseudoLane( fffLink );
        double vTilde = cyclist.getVMax(pseudoLane);
        //printDelays(vTilde, cyclist);


        double tLeave = Double.max(pseudoLane.tReady, cyclist.getTEarliestExit()) ;

        cyclist.setSpeed(vTilde);
        cyclist.setTStart(tLeave);
        final double tEarliestExit = tLeave + fffLink.getLength() / vTilde;
        //		log.debug("tEarliestExit=" + tEarliestExit ) ;
        cyclist.setTEarliestExit( tEarliestExit );
        cyclist.setCurrentLink( fffLink );
        fffLink.increaseOccupiedSpace(cyclist, vTilde );
        pseudoLane.updateTs(vTilde, tLeave);


        // wrap the QCycleAsVehicle and memorize it:
        //							log.debug( "now=" + context.getSimTimer().getTimeOfDay() + "; linkId=" + fffLink.getId() + outqAsString( fffLink.getOutQ() ) ) ;
        fffLink.getOutQ().add(qCyc );
        //							log.debug( "now=" + context.getSimTimer().getTimeOfDay() + "; linkId=" + fffLink.getId() + outqAsString( fffLink.getOutQ() ) ) ;

        //mads: A rejected cyclist does not get a new tEarliestExit.
        // I think that is okay, it only causes an efficiency loss.

    }

    private void printDelays(double vTilde, Cyclist cyclist) {
        if(vTilde + 0.00001 < cyclist.getDesiredSpeed()){
            log.info("Cyclist "+ cyclist.getId() + " riding " + String.format("%.1f",
                     (cyclist.getDesiredSpeed() - vTilde)/cyclist.getDesiredSpeed()*100d )
                     + "% slower on link " + fffLink.getId() );
        }
    }

    @Override public boolean doSimStep() {
        //							log.debug("linkId=" + fffLink.getId() + "; entering mads link doSimStep method; now = " + context.getSimTimer().getTimeOfDay() ) ;

        // yyyyyy this method is missing some call to link.processLink or similar.
        // mads: It seems to do the equivalent to what QueueWithBuffer is doing?

        // mads: At the moment only 1 doSimStep is performed per bike.

        //							log.debug( outqAsString( fffLink.getOutQ() ) ) ;

        QCycle cqo;
        while((cqo = fffLink.getOutQ().peek()) != null){

            //								log.debug( "now=" + context.getSimTimer().getTimeOfDay() + "; linkId=" + fffLink.getId() + outqAsString( fffLink.getOutQ() ) );

            final double tEarliestExit = cqo.getCyclist().getTEarliestExit();
            if( tEarliestExit > context.getSimTimer().getTimeOfDay()){
                break;
            }
            //		log.debug( "tEarliestExit="  + tEarliestExit);
            fffLink.getOutQ().remove();
            fffLink.reduceOccupiedSpace(cqo.getCyclist(), cqo.getCyclist().getSpeed() );

            if(cqo.getDriver().isWantingToArriveOnCurrentLink()){
                qLinkImpl.letVehicleArrive(cqo );
                continue;
            }

            //Auxiliary buffer created to fit the piece into MATSim.
            fffLink.addVehicleToMovedDownstreamVehicles(cqo );

            final QNodeI toNode = qLinkImpl.getToNode();
            if ( toNode instanceof QNodeImpl ) {
                ((QNodeImpl) toNode).activateNode();
            }
        }
        return true;
    }

    @Override public boolean isNotOfferingVehicle() {
        return fffLink.isVehiclesMovedDownstreamEmpty();

    }

    @Override public QVehicle popFirstVehicle() {
        //	return fffLink.getOutQ().isEmpty() ? null : fffLink.getOutQ().poll().getQCycle();
        return fffLink.pollFirstVehicleMovedDownstream();
    }

    @Override public QVehicle getFirstVehicle() {

        //mads: MAJOR PROBLEM: Since the vehicle is removed from outQ in doSimStep(),
        //          the vehicle can no longer be accessed through the outQ.
        //              .... and no other way to access it exists. :/
        //return fffLink.getOutQ().isEmpty() ? null : fffLink.getOutQ().peek().getQCycle();'
        return fffLink.getFirstVehicleMovedDownstream();
    }

    @Override public boolean isAcceptingFromWait( final QVehicle veh ) {
        // use same logic as inserting from upstream:
        return this.isAcceptingFromUpstream() ;

    }

    @Override public void addFromWait( final QVehicle veh ) {

        // activate link since there is now action on it:
        qLinkImpl.getInternalInterface().activateLink();

        double now = context.getSimTimer().getTimeOfDay() ;
        ((QCycle) veh).getCyclist().setTEarliestExit( now );

        // just inserting them upstream.  For the time being, but might also be ok in the long run.
        this.addFromUpstream( veh );
    }

    @Override public boolean isActive() {
        return !fffLink.getOutQ().isEmpty();
        // Should be okay, passes tests compared to always true.
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
        return fffLink.getWakeUpTime();
    }

    @Override public double getLoadIndicator() {
        throw new RuntimeException( "not implemented" );
    }

    @Override public void initBeforeSimStep() {
        //Intentionally empty
    }
}
