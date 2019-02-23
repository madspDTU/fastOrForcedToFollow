package org.matsim.core.router;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.FastAStarLandmarks;
import org.matsim.core.router.FastRouterDelegateFactory;
import org.matsim.core.router.util.PreProcessLandmarks;
import org.matsim.core.router.util.RoutingNetwork;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import fastOrForcedToFollow.configgroups.FFFConfigGroup;

public class DesiredSpeedBicycleFastAStarLandmarks extends FastAStarLandmarks {

	DesiredSpeedBicycleFastAStarLandmarks(RoutingNetwork routingNetwork, PreProcessLandmarks preProcessData,
			TravelDisutility costFunction, TravelTime timeFunction, double overdoFactor,
			FastRouterDelegateFactory fastRouterFactory) {
		super(routingNetwork, preProcessData, costFunction, timeFunction, overdoFactor, fastRouterFactory);
	}

	@Override
	public Path calcLeastCostPath(final Node fromNode, final Node toNode, final double startTime,
			final Person person, final Vehicle vehicle) {
		String idString = person.getId().toString();
		VehicleType vehicleType = VehicleUtils.getFactory().createVehicleType(
				Id.create(idString, VehicleType.class));
	
		double v_0 = (double) person.getAttributes().getAttribute( 
				FFFConfigGroup.DESIRED_SPEED );
		vehicleType.setMaximumVelocity(v_0);

		Vehicle actualVehicle = VehicleUtils.getFactory().createVehicle(
				Id.createVehicleId(idString), vehicleType);
			
		return super.calcLeastCostPath(fromNode, toNode, startTime, person, actualVehicle);
	}
}
