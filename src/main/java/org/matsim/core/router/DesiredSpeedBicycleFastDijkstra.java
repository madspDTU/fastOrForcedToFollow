/* *********************************************************************** *
 * project: org.matsim.*
 * FastDijkstra.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package org.matsim.core.router;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.PreProcessDijkstra;
import org.matsim.core.router.util.RoutingNetwork;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import fastOrForcedToFollow.configgroups.FFFConfigGroup;

/**
 * <p>
 * Performance optimized version of the Dijkstra {@link org.matsim.core.router.Dijkstra} 
 * least cost path router which uses its own network to route within.
 * </p>
 * 
 * @see org.matsim.core.router.Dijkstra
 * @see org.matsim.core.router.util.RoutingNetwork
 * @author cdobler
 */
public class DesiredSpeedBicycleFastDijkstra extends FastDijkstra {

	/*
	 * Create the routing network here and clear the nodeData map 
	 * which is not used by this implementation.
	 */
	DesiredSpeedBicycleFastDijkstra(final RoutingNetwork routingNetwork, final TravelDisutility costFunction, final TravelTime timeFunction,
			final PreProcessDijkstra preProcessData, final FastRouterDelegateFactory fastRouterFactory) {
		super(routingNetwork, costFunction, timeFunction, preProcessData, fastRouterFactory);
	}
		
	/*
	 * Replace the references to the from and to nodes with their corresponding
	 * nodes in the routing network.
	 */
	@Override
	public Path calcLeastCostPath(final Node fromNode, final Node toNode, final double startTime, final Person person, final Vehicle vehicle) {
		
		//May be lacking a crucial initialisation here!
		
		String idString = person.getId().toString();
		VehicleType vehicleType = VehicleUtils.getFactory().createVehicleType(
				Id.create(idString, VehicleType.class));
		vehicleType.setNetworkMode(TransportMode.bike);
		double v_0 = (double) person.getAttributes().getAttribute( FFFConfigGroup.DESIRED_SPEED );
		vehicleType.setMaximumVelocity(v_0);
		Vehicle actualVehicle = VehicleUtils.getFactory().createVehicle(
				Id.createVehicleId(idString), vehicleType);
		
		return super.calcLeastCostPath(fromNode, toNode, startTime, person, actualVehicle);
	}
	
}
