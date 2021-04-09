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
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngineI.NetsimInternalInterface;
import org.matsim.core.mobsim.qsim.qnetsimengine.QueueWithBufferForRoW.Builder;
import org.matsim.core.mobsim.qsim.qnetsimengine.vehicleq.VehicleQ;
import org.matsim.vis.snapshotwriters.SnapshotLinkWidthCalculator;

import fastOrForcedToFollow.Sublink;
import fastOrForcedToFollow.configgroups.FFFConfigGroup;
import fastOrForcedToFollow.configgroups.FFFNodeConfigGroup;

import javax.inject.Inject;


//Extends the original and overrides a single method

public class FFFQNetworkFactoryWithQFFFNodes extends AbstractFFFQNetworkFactory{

	private static final Logger log = Logger.getLogger( FFFQNetworkFactoryWithQFFFNodes.class ) ;
	private FFFNodeConfigGroup fffNodeConfig;

	@Inject FFFQNetworkFactoryWithQFFFNodes(EventsManager events, Scenario scenario) {
		super(events, scenario);
		this.fffNodeConfig = ConfigUtils.addOrGetModule(scenario.getConfig(), FFFNodeConfigGroup.class);
	}

	@Override
	public QLinkI createNetsimLink(final Link link, final QNodeI toQueueNode) {
		
	//	QFFFAbstractNode nodeType = ((QFFFNode) toQueueNode).getQFFFAbstractNode();
		
		if ( link.getAllowedModes().contains( TransportMode.bike ) ) {

			Gbl.assertIf( link.getAllowedModes().size()==1 ); // not possible with multi-modal links! kai, oct'18
			QLinkImpl.Builder linkBuilder = new QLinkImpl.Builder( context, netsimEngine );
			QCycleLaneWithSublinks.Builder laneBuilder = new QCycleLaneWithSublinks.Builder(context, fffConfig);
			if(link.getNumberOfLanes() == 1) {
			laneBuilder.setLeftBufferCapacity(-1);
			} else if (link.getNumberOfLanes() == 2) {
				laneBuilder.setLeftBufferCapacity(fffNodeConfig.getTwoLaneLeftBufferCapacityForBicycles());
			} else if (link.getNumberOfLanes() >= 3){
				laneBuilder.setLeftBufferCapacity(Integer.MAX_VALUE);
			}
			linkBuilder.setLaneFactory( laneBuilder);
			return linkBuilder.build( link, toQueueNode );			
		} else {
			QLinkImpl.Builder linkBuilder = new QLinkImpl.Builder( context, netsimEngine );
			Builder laneBuilder = new QueueWithBufferForRoW.Builder( context );
			int roadValue = fffNodeConfig.getRoadTypeToValueMap().get(link.getAttributes().getAttribute("type"));
			int roadValueTol = fffNodeConfig.getRoadTypeToValueMap().get("tertiary_link"); 
			if(link.getNumberOfLanes() == 1 && roadValue < roadValueTol) {
				laneBuilder.setMaximumLeftBufferLength(-1);
			} else if (link.getNumberOfLanes() == 1  && roadValue >= roadValueTol) {
				laneBuilder.setMaximumLeftBufferLength(fffNodeConfig.getSmallRoadLeftBufferCapacity());
			} else {
				laneBuilder.setMaximumLeftBufferLength(Integer.MAX_VALUE);
			}
			linkBuilder.setLaneFactory(laneBuilder);
			return linkBuilder.build( link, toQueueNode );
		}
	}

	@Override
	public QNodeI createNetsimNode(final Node node) {
		QFFFNode.Builder builder = new QFFFNode.Builder( netsimEngine, context, fffNodeConfig, scenario ) ;
		return builder.build( node ) ;
	}
	
}
