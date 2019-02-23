package org.matsim.core.router;

import java.util.ArrayList;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.framework.HasPerson;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.util.PreProcessDijkstra;
import org.matsim.core.router.util.RoutingNetwork;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.run.RunMatsim;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleTypeImpl;
import org.matsim.vehicles.VehicleUtils;

import fastOrForcedToFollow.configgroups.FFFConfigGroup;

public class DesiredSpeedBicycleDijkstra extends Dijkstra{

	DesiredSpeedBicycleDijkstra(Network network, TravelDisutility costFunction,
			TravelTime timeFunction, PreProcessDijkstra preProcessData) {
		super(network, costFunction, timeFunction, preProcessData);
	}

	protected DesiredSpeedBicycleDijkstra(final Network network, final TravelDisutility costFunction, 
			final TravelTime timeFunction) {
		this(network, costFunction, timeFunction, null);
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

		
	//If needing to determine mode first....
	//	String mode = determineMode(startTime, person);	
	//	if(mode.equals(TransportMode.car)){
	//		vehicleType.setMaximumVelocity(Double.POSITIVE_INFINITY);	
	//	} else if (mode.equals(TransportMode.bike)) {
	//		double v_0 = (double) person.getAttributes().getAttribute( 
	//				RunMatsim.DESIRED_SPEED );
	//		vehicleType.setMaximumVelocity(v_0);
	//	} else {
	//		System.err.println("No valid mode found");
	//		Gbl.assertIf(false);
	//	}
	}

	private String determineMode(final double startTime, final Person person) {
		ArrayList<PlanElement> pes = (ArrayList<PlanElement>) person.getSelectedPlan().getPlanElements();
		double dif = Double.NEGATIVE_INFINITY;
		String mode = "";
		
		for(int i = 0; i < person.getSelectedPlan().getPlanElements().size(); i++){
			if( pes.get(i) instanceof Leg){
				Leg leg = (Leg) pes.get(i);
				if(leg.getMode().equals(TransportMode.car) || leg.getMode().equals(TransportMode.bike)){
					Activity activity = (Activity) pes.get(i-1);
					if(activity.getType().contains("interaction")){
						activity = (Activity) pes.get(i-3);
					}
					double endTime = activity.getEndTime();
					
					if(endTime >= startTime){
						if(endTime - startTime < -dif){
							dif = endTime - startTime;
							mode = leg.getMode();
						}
						return mode;
					} else {
						dif =  endTime - startTime;
						mode = leg.getMode();
					}
				}
			}
		}
		return mode;
	}

}
