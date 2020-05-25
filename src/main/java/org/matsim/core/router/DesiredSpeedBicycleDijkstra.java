package org.matsim.core.router;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.router.util.*;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import fastOrForcedToFollow.configgroups.FFFConfigGroup;

import javax.inject.Inject;
import javax.inject.Singleton;

public class DesiredSpeedBicycleDijkstra extends Dijkstra{

	DesiredSpeedBicycleDijkstra( Network network, TravelDisutility costFunction,
							 TravelTime timeFunction, PreProcessDijkstra preProcessData ) {
		super(network, costFunction, timeFunction, preProcessData);
	}

	DesiredSpeedBicycleDijkstra( final Network network, final TravelDisutility costFunction,
							 final TravelTime timeFunction ) {
		this(network, costFunction, timeFunction, null);
	}

	@Override
	public Path calcLeastCostPath(final Node fromNode, final Node toNode, final double startTime,
			final Person person, final Vehicle vehicle) {
		String idString = person.getId().toString();
		VehicleType vehicleType = VehicleUtils.getFactory().createVehicleType(
				Id.create(idString, VehicleType.class));
		vehicleType.setNetworkMode(TransportMode.bike);
		double v_0 = (double) person.getAttributes().getAttribute( 
				FFFConfigGroup.DESIRED_SPEED );
		vehicleType.setMaximumVelocity(v_0);

		Vehicle actualVehicle = VehicleUtils.getFactory().createVehicle(
				Id.createVehicleId(idString), vehicleType);
			
		return super.calcLeastCostPath(fromNode, toNode, startTime, person, actualVehicle);
	}

/*
	@Singleton
	public static class Factory implements LeastCostPathCalculatorFactory{

		private final boolean usePreProcessData;
		private final Map<Network, PreProcessDijkstra> preProcessData = new HashMap<>();

		@Inject
		public Factory() {
			this.usePreProcessData = false;
		}

		public Factory( final boolean usePreProcessData ) {
			this.usePreProcessData = usePreProcessData;
		}

		// yy there is no guarantee that "createPathCalculator" is called with the same network as the one that was used for "preProcessData".
		// This can happen for example when LinkToLink routing is switched on.  kai & theresa, feb'15
		// To fix this, we create the PreProcessData when the first LeastCostPathCalculator object is created and store it in a map using
		// the network as key. For the PreProcessDijkstra data this is fine, since it does not take travel times and disutilities into account.
		// For the AStarLandmarks data, we would have to include the other two arguments into the lookup value as well... cdobler, sep'17
		@Override
		public synchronized LeastCostPathCalculator createPathCalculator(final Network network, final TravelDisutility travelCosts, final TravelTime travelTimes) {
			if (this.usePreProcessData) {
				PreProcessDijkstra preProcessDijkstra = this.preProcessData.get(network);
				if (preProcessDijkstra == null) {
					preProcessDijkstra = new PreProcessDijkstra();
					preProcessDijkstra.run(network);
					this.preProcessData.put(network, preProcessDijkstra);
				}
				return new DesiredSpeedBicycleDijkstra(network, travelCosts, travelTimes, preProcessDijkstra);
			}
			return new DesiredSpeedBicycleDijkstra(network, travelCosts, travelTimes);
		}
	}
	*/
}
