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
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngine.NetsimInternalInterface;
import org.matsim.core.mobsim.qsim.qnetsimengine.vehicleq.VehicleQ;
import org.matsim.vis.snapshotwriters.SnapshotLinkWidthCalculator;

import fastOrForcedToFollow.Sublink;
import fastOrForcedToFollow.configgroups.FFFConfigGroup;
import fastOrForcedToFollow.configgroups.FFFNodeConfigGroup;

import javax.inject.Inject;


//Extends the original and overrides a single method

public class MadsQNetworkFactoryWithQFFFNodes extends AbstractMadsQNetworkFactory{

	private static final Logger log = Logger.getLogger( MadsQNetworkFactoryWithQFFFNodes.class ) ;
	private FFFNodeConfigGroup fffNodeConfig;

	@Inject MadsQNetworkFactoryWithQFFFNodes(EventsManager events, Scenario scenario) {
		super(events, scenario);
		this.fffNodeConfig = ConfigUtils.addOrGetModule(scenario.getConfig(), FFFNodeConfigGroup.class);
	}

	@Override
	public QLinkI createNetsimLink(final Link link, final QNodeI toQueueNode) {
		if ( link.getAllowedModes().contains( TransportMode.bike ) ) {

			Gbl.assertIf( link.getAllowedModes().size()==1 ); // not possible with multi-modal links! kai, oct'18
			QLinkImpl.Builder linkBuilder = new QLinkImpl.Builder( context, netsimEngine );
			linkBuilder.setLaneFactory( new QCycleLaneWithSublinks.Builder(context, fffConfig));
			return linkBuilder.build( link, toQueueNode );
		} else {
			QLinkImpl.Builder linkBuilder = new QLinkImpl.Builder( context, netsimEngine );
			linkBuilder.setLaneFactory(new QueueWithBufferForRoW.Builder( context ));
			return linkBuilder.build( link, toQueueNode );
		}
	}

	@Override
	public QNodeI createNetsimNode(final Node node) {
		QFFFNode.Builder builder = new QFFFNode.Builder( netsimEngine, context, fffNodeConfig ) ;
		return builder.build( node ) ;
	}
	
}
