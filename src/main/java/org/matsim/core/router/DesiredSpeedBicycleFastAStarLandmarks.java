package org.matsim.core.router;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.router.util.PreProcessLandmarks;
import org.matsim.core.router.util.RoutingNetwork;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import fastOrForcedToFollow.configgroups.FFFConfigGroup;

public class DesiredSpeedBicycleFastAStarLandmarks extends FastAStarLandmarks {


	public String mode;

	DesiredSpeedBicycleFastAStarLandmarks(RoutingNetwork routingNetwork, PreProcessLandmarks preProcessData,
			TravelDisutility costFunction, TravelTime timeFunction, double overdoFactor,
			FastRouterDelegateFactory fastRouterFactory, String mode) {
		super(routingNetwork, preProcessData, costFunction, timeFunction, overdoFactor, fastRouterFactory);
		this.mode = mode;
	}

	@Override
	public Path calcLeastCostPath(final Node fromNode, final Node toNode, final double startTime,
			final Person person, final Vehicle vehicle) {

		String idString = person.getId().toString();
		VehicleType vehicleType = VehicleUtils.getFactory().createVehicleType(
				Id.create(person.getId(), VehicleType.class));
		vehicleType.setNetworkMode(this.mode);
		if(this.mode.equals(TransportMode.bike)) {
			double v_0 = (double) person.getAttributes().getAttribute( FFFConfigGroup.DESIRED_SPEED );
			vehicleType.setMaximumVelocity(v_0);
		} else if(this.mode.equals(TransportMode.car)) {
			vehicleType.setMaximumVelocity(130/3.6);
		} else if (this.mode.equals(TransportMode.truck)) {
			vehicleType.setMaximumVelocity(80/3.6);
		}
		Vehicle actualVehicle = VehicleUtils.getFactory().createVehicle(
				Id.createVehicleId(idString), vehicleType);	
		double minTravelCostPerLength = 1/60. / vehicleType.getMaximumVelocity();
		super.setMinTravelCostPerLength(minTravelCostPerLength);
		
		Path path =  super.calcLeastCostPath(fromNode, toNode, startTime, person, actualVehicle);	
		
		Gbl.assertIf(super.getMinTravelCostPerLength() == minTravelCostPerLength);
		return path;
	}

	/*
	@Singleton
	public static class Factory implements LeastCostPathCalculatorFactory{

		private final RoutingNetworkFactory routingNetworkFactory;
		private final Map<Network, RoutingNetwork> routingNetworks = new HashMap<>();
		private final Map<Network, PreProcessLandmarks> preProcessData = new HashMap<>();

		private final int nThreads;

		@Inject
		public Factory( final GlobalConfigGroup globalConfigGroup ) {
			this(FastRouterType.ARRAY, globalConfigGroup.getNumberOfThreads());
		}

		public Factory() {
			// the value of 8 threads was the default that I found here when making this configurable without injection.
			// This was in the create method, with a comment from kai, nov 17.
			// Not sure why this is a good default. td, nov 18
			this(8);
		}

		public Factory( int nThreads ) {
			this(FastRouterType.ARRAY, nThreads);
		}

		// hide this constructor, as only one router type is allowed anyway...
		private Factory( final FastRouterType fastRouterType, int numberOfThreads ) {
			switch (fastRouterType) {
			case ARRAY:
				this.routingNetworkFactory = new ArrayRoutingNetworkFactory();
				break;
			case POINTER:
				throw new RuntimeException("PointerRoutingNetworks are no longer supported. Use ArrayRoutingNetworks instead. Aborting!");
			default:
				throw new RuntimeException("Undefined FastRouterType: " + fastRouterType);
			}

			this.nThreads = numberOfThreads;
		}

		public synchronized LeastCostPathCalculator createPathCalculator(final Network network, final TravelDisutility travelCosts, final TravelTime travelTimes) {
			RoutingNetwork routingNetwork = this.routingNetworks.get(network);
			PreProcessLandmarks preProcessLandmarks = this.preProcessData.get(network);

			if (routingNetwork == null) {
				routingNetwork = this.routingNetworkFactory.createRoutingNetwork(network);

				if (preProcessLandmarks == null) {
					preProcessLandmarks = new PreProcessLandmarks(travelCosts);
					preProcessLandmarks.setNumberOfThreads(nThreads);
					preProcessLandmarks.run(network);
					this.preProcessData.put(network, preProcessLandmarks);

					for (RoutingNetworkNode node : routingNetwork.getNodes().values()) {
						node.setDeadEndData(preProcessLandmarks.getNodeData(node.getNode()));
					}
				}

				this.routingNetworks.put(network, routingNetwork);
			}
			FastRouterDelegateFactory fastRouterFactory = new ArrayFastRouterDelegateFactory();

			final double overdoFactor = 1.0;
			return new FastAStarLandmarks(routingNetwork, preProcessLandmarks, travelCosts, travelTimes, overdoFactor, fastRouterFactory);
		}


		public synchronized LeastCostPathCalculator createDesiredSpeedPathCalculator(final Network network, final TravelDisutility travelCosts, final TravelTime travelTimes) {
			RoutingNetwork routingNetwork = this.routingNetworks.get(network);
			PreProcessLandmarks preProcessLandmarks = this.preProcessData.get(network);

			if (routingNetwork == null) {
				routingNetwork = this.routingNetworkFactory.createRoutingNetwork(network);

				if (preProcessLandmarks == null) {
					preProcessLandmarks = new PreProcessLandmarks(travelCosts);
					preProcessLandmarks.setNumberOfThreads(nThreads);
					preProcessLandmarks.run(network);
					this.preProcessData.put(network, preProcessLandmarks);

					for (RoutingNetworkNode node : routingNetwork.getNodes().values()) {
						node.setDeadEndData(preProcessLandmarks.getNodeData(node.getNode()));
					}
				}

				this.routingNetworks.put(network, routingNetwork);
			}
			FastRouterDelegateFactory fastRouterFactory = new ArrayFastRouterDelegateFactory();

			final double overdoFactor = 1.0;
			return new DesiredSpeedBicycleFastAStarLandmarks(routingNetwork, preProcessLandmarks, travelCosts, travelTimes, overdoFactor, fastRouterFactory);
		}
	}
	 */
}
