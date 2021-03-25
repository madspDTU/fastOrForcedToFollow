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


import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.qsim.interfaces.AgentCounter;
import org.matsim.core.mobsim.qsim.pt.TransitStopAgentTracker;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngineI;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngineI.NetsimInternalInterface;
import org.matsim.core.mobsim.qsim.qnetsimengine.vehicleq.VehicleQ;
import org.matsim.vis.snapshotwriters.SnapshotLinkWidthCalculator;

import fastOrForcedToFollow.Sublink;
import fastOrForcedToFollow.configgroups.FFFConfigGroup;

import javax.inject.Inject;
abstract class AbstractMadsQNetworkFactory implements QNetworkFactory {
	private static final Logger log = Logger.getLogger( AbstractMadsQNetworkFactory.class ) ;

	private EventsManager events ;
	protected Scenario scenario ;
	// (vis needs network and may need population attributes and config; in consequence, makes sense to have scenario here. kai, apr'16)
	protected NetsimEngineContext context;
	protected NetsimInternalInterface netsimEngine ;
	protected FFFConfigGroup fffConfig;
	
	@Inject AbstractMadsQNetworkFactory( EventsManager events, Scenario scenario ) {
		this.events = events;
		this.scenario = scenario;
		this.fffConfig = ConfigUtils.addOrGetModule(scenario.getConfig(), FFFConfigGroup.class);
	}

	@Override
	public void initializeFactory( AgentCounter agentCounter, MobsimTimer mobsimTimer, NetsimInternalInterface netsimEngine1 ) {
		this.netsimEngine = netsimEngine1;
		double effectiveCellSize = scenario.getNetwork().getEffectiveCellSize() ;

		SnapshotLinkWidthCalculator linkWidthCalculator = new SnapshotLinkWidthCalculator();
		linkWidthCalculator.setLinkWidthForVis( scenario.getConfig().qsim().getLinkWidthForVis() );
		linkWidthCalculator.setLaneWidth( scenario.getNetwork().getEffectiveLaneWidth() );

		AbstractAgentSnapshotInfoBuilder agentSnapshotInfoBuilder = QNetsimEngineWithThreadpool.createAgentSnapshotInfoBuilder(scenario, linkWidthCalculator);
		
		context = new NetsimEngineContext( events, effectiveCellSize, agentCounter, agentSnapshotInfoBuilder, scenario.getConfig().qsim(), 
				mobsimTimer, linkWidthCalculator );
	}

	@Override
	public abstract QLinkI createNetsimLink(final Link link, final QNodeI toQueueNode);

	@Override
	public abstract QNodeI createNetsimNode(final Node node);
		
}
