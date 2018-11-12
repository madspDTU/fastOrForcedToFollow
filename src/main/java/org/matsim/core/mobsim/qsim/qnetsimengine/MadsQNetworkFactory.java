/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.core.mobsim.qsim.qnetsimengine;


import fastOrForcedToFollow.Cyclist;
import fastOrForcedToFollow.CyclistQObject;
import fastOrForcedToFollow.PseudoLane;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.qsim.interfaces.AgentCounter;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.pt.TransitStopAgentTracker;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngine.NetsimInternalInterface;
import org.matsim.core.mobsim.qsim.qnetsimengine.vehicleq.VehicleQ;
import org.matsim.lanes.Lane;
import org.matsim.vehicles.Vehicle;
import org.matsim.vis.snapshotwriters.SnapshotLinkWidthCalculator;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;


/**
 * The idea here is that there are the following levels:<ul>
 * <li> Run-specific objects, such as {@link QSimConfigGroup}, {@link EventsManager}, etc.  These are in general guice-injected, but can
 * also be configured by the constructor.  In the longer run, I would like to get rid of {@link Scenario}, but I haven't checked where
 * it us truly needed yet.
 * <li> Mobsim-specific objects, such as {@link AgentCounter} or {@link QNetsimEngine}.  Since the mobsim is re-created in every
 * iteration, they cannot be injected via guice, at least not via the global inject mechanism that has the override facility.
 * <li> The last level are link- oder node-specific objects such as {@link Link}  or {@QNode}.  They are arguments to the 
 * creational methods.
 * <li> The main underlying implementations are {@link QueueWithBuffer}, the factory of which is inserted into {@link QLinkImpl}.  This
 * was the syntax that could subsume all other syntactic variants.  Builders are used to set defaults and to avoid overly long
 * constructors.
 * <li> {@link QLinkImpl} is essentially the container where vehicles are parked, agents perform activities, etc.  MATSim has some tendency to
 * have them centralized in the QSim (see, e.g., {@link TransitStopAgentTracker}), but both for parallel computing and for visualization, 
 * having agents on a decentralized location is helpful.  
 * <li> Most functionality of {@link QLinkImpl} is actually in {@link AbstractQLink}, which
 * can also be used as basic infrastructure by other qnetworks.  
 * <li> {@link QueueWithBuffer} is an instance of {@link QLaneI} and can be replaced accordingly.  
 * <li> One can also replace the {@link VehicleQ} that works inside {@link QueueWithBuffer}. 
 * </ul>
 * 
 * @author dgrether, knagel
 * 
 * @see ConfigurableQNetworkFactory
 */
public final class MadsQNetworkFactory extends QNetworkFactory {
	private static final Logger log = Logger.getLogger( MadsQNetworkFactory.class ) ;

	private EventsManager events ;
	private Scenario scenario ;
	// (vis needs network and may need population attributes and config; in consequence, makes sense to have scenario here. kai, apr'16)
	private NetsimEngineContext context;
	private NetsimInternalInterface netsimEngine ;
	@Inject MadsQNetworkFactory( EventsManager events, Scenario scenario ) {
		this.events = events;
		this.scenario = scenario;
	}
	@Override
	void initializeFactory( AgentCounter agentCounter, MobsimTimer mobsimTimer, NetsimInternalInterface netsimEngine1 ) {
		this.netsimEngine = netsimEngine1;
		double effectiveCellSize = scenario.getNetwork().getEffectiveCellSize() ;

		SnapshotLinkWidthCalculator linkWidthCalculator = new SnapshotLinkWidthCalculator();
		linkWidthCalculator.setLinkWidthForVis( scenario.getConfig().qsim().getLinkWidthForVis() );
		linkWidthCalculator.setLaneWidth( scenario.getNetwork().getEffectiveLaneWidth() );

		AbstractAgentSnapshotInfoBuilder agentSnapshotInfoBuilder = QNetsimEngine.createAgentSnapshotInfoBuilder( scenario, linkWidthCalculator );

		context = new NetsimEngineContext( events, effectiveCellSize, agentCounter, agentSnapshotInfoBuilder, scenario.getConfig().qsim(), 
				mobsimTimer, linkWidthCalculator );
	}
	@Override
	QLinkI createNetsimLink(final Link link, final QNodeI toQueueNode) {
		if ( link.getAllowedModes().contains( TransportMode.bike ) ) {
			Gbl.assertIf( link.getAllowedModes().size()==1 ); // not possible with multi-modal links! kai, oct'18
			final String id = link.getId().toString();
			final int psi = (int) link.getNumberOfLanes() ;
			final double length=link.getLength() ;
	//		final double bicyclePCE = new BicycleVehicleType().getPcuEquivalents();
			fastOrForcedToFollow.Link link1 = null;
			try {
				link1 = new fastOrForcedToFollow.Link(id, psi, length) ;
			} catch ( InstantiationException | IllegalAccessException e ) {
				e.printStackTrace();
			}

			final fastOrForcedToFollow.Link fffLink = link1;
			QLinkImpl.Builder linkBuilder = new QLinkImpl.Builder( context, netsimEngine );
			linkBuilder.setLaneFactory( new QLinkImpl.LaneFactory(){
				@Override public QLaneI createLane(AbstractQLink qLinkImpl ) {


					return new QLaneI(){

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
						//	log.debug( "adding: veh=" + veh.getId() );

							// upcast:
							QCycleAsVehicle qCyc = (QCycleAsVehicle) veh;

							// get the Cyclist out of it:
							Cyclist cyclist = qCyc.getCyclist();

							// set back pointer (yy should be done in factory)
							cyclist.setQCycleAsVehicle(qCyc);

							// internal fff logic:
							PseudoLane pseudoLane = cyclist.selectPseudoLane(fffLink);
							double vTilde = cyclist.getVMax(pseudoLane);
							double tLeave = Double.max(pseudoLane.tReady, cyclist.getTEarliestExit());
							cyclist.setSpeed(vTilde);
							cyclist.setTStart(tLeave);
							cyclist.setTEarliestExit(tLeave + fffLink.getLength()/vTilde);
							fffLink.increaseOccupiedSpace(cyclist, vTilde);
							pseudoLane.updateTs(vTilde, tLeave);
							cyclist.setCurrentLink(fffLink);
							
							// wrap the QCycleAsVehicle and memorize it:
							fffLink.getOutQ().add(new CyclistQObject(qCyc));

							//mads: A rejected cyclist does not get a new tEarliestExit.
								// I think that is okay, it only causes an efficiency loss.

						}

						@Override public boolean doSimStep() {
							// yyyyyy this method is missing some call to link.processLink or similar.
							// mads: It seems to do the equivalent to what QueueWithBuffer is doing.

							CyclistQObject cqo;
							while((cqo = fffLink.getOutQ().peek()) != null){
								if(cqo.getCyclist().getTEarliestExit() > context.getSimTimer().getTimeOfDay()){
									break;
								}
								if(cqo.getQCycle().getDriver().isWantingToArriveOnCurrentLink()){
									qLinkImpl.letVehicleArrive(cqo.getQCycle());
									fffLink.getOutQ().remove();
									fffLink.reduceOccupiedSpace(cqo.getCyclist(), cqo.getCyclist().getSpeed());
									continue;
								}
								fffLink.getOutQ().remove();
								fffLink.reduceOccupiedSpace(cqo.getCyclist(), cqo.getCyclist().getSpeed());
								
								//Auxiliary buffer created to fit the piece into MATSim. 
								fffLink.addVehicleToMovedDownstreamVehicles(cqo.getQCycle());
								
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
							// just inserting them upstream.  For the time being, but might also be ok in the long run.
							this.addFromUpstream( veh );
						}


						@Override public boolean isActive() {
							return true;// yy always active, to get started
						}

						@Override public double getSimulatedFlowCapacityPerTimeStep() {
							throw new RuntimeException( "not implemented" );
						}

						@Override public void recalcTimeVariantAttributes() {
							throw new RuntimeException( "not implemented" );
						}

						@Override public QVehicle getVehicle( final Id<Vehicle> vehicleId ) {
							QCycleAsVehicle qCyc = null;
							for(CyclistQObject cqo : fffLink.getOutQ()){
								if(cqo.getCyclist().getId() == vehicleId.toString()){
									qCyc = cqo.getQCycle();
								}
							}
							return qCyc;
						}

						@Override public double getStorageCapacity() {
							throw new RuntimeException( "not implemented" );
						}

						@Override public VisData getVisData() {
							throw new RuntimeException( "not implemented" );
						}

						@Override public void addTransitSlightlyUpstreamOfStop( final QVehicle veh ) {
							throw new RuntimeException( "not implemented" );
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
							for(CyclistQObject cqo : fffLink.getOutQ()){
								qCycs.add(cqo.getQCycle());
							}
							return qCycs;
						}

						@Override public double getLastMovementTimeOfFirstVehicle() {
							throw new RuntimeException( "not implemented" );
						}

						@Override public double getLoadIndicator() {
							throw new RuntimeException( "not implemented" );
						}

						@Override public void initBeforeSimStep() {
							//Intentionally empty
						}
					} ;
				}
			} );
			return linkBuilder.build( link, toQueueNode );
		} else {
			QLinkImpl.Builder linkBuilder = new QLinkImpl.Builder( context, netsimEngine );
			return linkBuilder.build( link, toQueueNode );
		}

	}
	@Override
	QNodeI createNetsimNode(final Node node) {
		QNodeImpl.Builder builder = new QNodeImpl.Builder( netsimEngine, context ) ;
		return builder.build( node ) ;
	}
}
