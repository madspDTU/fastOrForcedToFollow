/* *********************************************************************** *
 * project: org.matsim.*
 * InvertedNetworkLegRouter
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup.ModeRoutingParams;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.MyNetworkInverter;
import org.matsim.core.network.algorithms.NetworkInverter;
import org.matsim.core.network.algorithms.NetworkTurnInfoBuilderI;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.LinkToLinkTravelTime;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.Facility;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;


/**
 * This leg router takes travel times needed for turning moves into account. This is done by a
 * routing on an inverted network, i.e. the links of the street networks are converted to nodes and
 * for each turning move a link is inserted. This LegRouter can only be used if the
 * enableLinkToLinkRouting parameter in the controler config module is set and AStarLandmarks
 * routing is not enabled.
 * 
 * @author dgrether
 * @author michalm
 */
class MyLinkToLinkRoutingModule implements RoutingModule
{
	private final Network invertedNetwork;
	private final Network network;
	private final LeastCostPathCalculator leastCostPathCalculator;
	private final PopulationFactory populationFactory;
	private final String mode;
	private final Scenario scenario;
	private final RoutingModule walkRouter;

	MyLinkToLinkRoutingModule(final String mode, final PopulationFactory populationFactory,
			Network network, LeastCostPathCalculatorFactory leastCostPathCalcFactory,
			TravelDisutility travelDisutility,
			TravelTime invertedTravelTimes, Network invertedNetwork, Scenario scenario,
			final RoutingModule accessEgressToNetworkRouter)
	{
		this.network = network;
		this.populationFactory = populationFactory;
		this.mode = mode;
		this.scenario = scenario;
		this.walkRouter = accessEgressToNetworkRouter;
		this.invertedNetwork = invertedNetwork;
		
		if(leastCostPathCalcFactory instanceof DesiredSpeedBicycleFastAStarLandmarksFactory) {
			this.leastCostPathCalculator = ((DesiredSpeedBicycleFastAStarLandmarksFactory) leastCostPathCalcFactory).
					createDesiredSpeedPathCalculator(invertedNetwork, travelDisutility, invertedTravelTimes, this.mode);
		} else if(leastCostPathCalcFactory instanceof DesiredSpeedBicycleFastDijkstraFactory) {
			this.leastCostPathCalculator = ((DesiredSpeedBicycleFastDijkstraFactory) leastCostPathCalcFactory).
					createDesiredSpeedPathCalculator(invertedNetwork, travelDisutility, invertedTravelTimes, this.mode);	
		} else if(leastCostPathCalcFactory instanceof DesiredSpeedBicycleDijkstraFactory) {
				this.leastCostPathCalculator = ((DesiredSpeedBicycleDijkstraFactory) leastCostPathCalcFactory).
						createDesiredSpeedPathCalculator(invertedNetwork, travelDisutility, invertedTravelTimes, this.mode);	
		} else {
			this.leastCostPathCalculator = leastCostPathCalcFactory.createPathCalculator(invertedNetwork, travelDisutility, invertedTravelTimes);
		}

	}

	@Override
	public List<? extends PlanElement> calcRoute( final Facility fromFacility,
			final Facility toFacility, final double departureTime, final Person person)
	{	      

		Gbl.assertNotNull(fromFacility);
		Gbl.assertNotNull(toFacility);

		Link accessActLink = FacilitiesUtils.decideOnLink(fromFacility, this.network );
		Link egressActLink = FacilitiesUtils.decideOnLink(toFacility, this.network );

		Gbl.assertNotNull(accessActLink);
		Gbl.assertNotNull(egressActLink);


		////////////
		// Access //
		////////////

		double now = departureTime ;

		List<PlanElement> result = new ArrayList<>() ;

		// === access:
		{
			now = addBushwhackingLegFromFacilityToLinkIfNecessary( fromFacility, person, accessActLink, now, result, populationFactory, mode,
					scenario.getConfig() );
		}


		///////////////
		// Vehicular //
		///////////////

		now = addVehicularLeg(accessActLink, egressActLink, person, now, result);

		//////////////
		/// Egress ///
		//////////////

		addBushwhackingLegFromLinkToFacilityIfNecessary( toFacility, person, egressActLink, now, result, populationFactory, mode,
				scenario.getConfig() );



		return result;
	}

	private double addVehicularLeg(Link accessLink, Link egressLink, final Person person,
			double now, List<PlanElement> result) {

		Leg newLeg = this.populationFactory.createLeg( this.mode );


		if (!accessLink.getId().equals(egressLink.getId())) {
			// (a "true" route)	        
			Node fromInvNode = this.invertedNetwork.getNodes()
					.get(Id.create(accessLink.getId(), Node.class));
			Node toInvNode = this.invertedNetwork.getNodes().get(Id.create( egressLink.getId(), Node.class));

			Gbl.assertNotNull(fromInvNode);
			Gbl.assertNotNull(toInvNode);
			
			Path invPath = leastCostPathCalculator.calcLeastCostPath(fromInvNode, toInvNode, now, person, null);
			if (invPath == null) {
				throw new RuntimeException("No route found on inverted network from link "
						+ accessLink.getId() + " to link " + egressLink.getId() + ".");
			}		
			Path path = invertPath(invPath);

			NetworkRoute route = this.populationFactory.getRouteFactories().createRoute(NetworkRoute.class, accessLink.getId(), egressLink.getId());
			route.setLinkIds(accessLink.getId(), NetworkUtils.getLinkIds(path.links), egressLink.getId());
			route.setTravelTime(path.travelTime);
			route.setTravelCost(path.travelCost);
			route.setDistance(RouteUtils.calcDistance(route, 1.0, 1.0, this.network));
			newLeg.setRoute(route);
			newLeg.setTravelTime(path.travelTime);
		} else {
			// create an empty route == staying on place if toLink == endLink
			// note that we still do a route: someone may drive from one location to another on the link. kai, dec'15
			NetworkRoute route = this.populationFactory.getRouteFactories().createRoute(NetworkRoute.class, accessLink.getId(), egressLink.getId());
			route.setTravelTime(0);
			route.setDistance(0.0);
			newLeg.setRoute(route);
			newLeg.setTravelTime(0);
		}		
		newLeg.setDepartureTime(now);
		result.add(newLeg);
		now = newLeg.getDepartureTime().seconds() + newLeg.getTravelTime().seconds();
		return now;
	}

	private Path invertPath(Path invPath)
	{
		int invLinkCount = invPath.links.size();//==> normal node count

		//path search is called only if fromLinkId != toLinkId
		//see: org.matsim.core.router.NetworkRoutingModule.routeLeg()
		//implies: fromInvNode != toInvNode
		if (invLinkCount == 0) {
			throw new RuntimeException(
					"The path in the inverted network should consist of at least one link.");
		}

		List<Link> links = new ArrayList<>(invLinkCount - 1);
		for (int i = 1; i < invLinkCount; i++) {
			Id<Link> linkId = Id.create(invPath.nodes.get(i).getId(), Link.class);
			Link link = network.getLinks().get(linkId); 
			if(link == null) {
				System.err.println(linkId + " doesn't exist in network. In inverted network? " + invertedNetwork.getLinks().containsKey(linkId));
			}
			links.add(network.getLinks().get(linkId));
		}

		List<Node> nodes = new ArrayList<>(invLinkCount);
		//        nodes.add(links.get(0).getFromNode());
		/* use the first link of the inverted path instead of the first node of the just created link list. also works for invLinkCount 1. theresa, jan'17 */
		nodes.add(network.getNodes().get(Id.create(invPath.links.get(0).getId(), Node.class)));
		for (Link l : links) {
			if(l == null) {
				System.err.println("A link in the inverted path is null!?!?! " + invLinkCount);
			} 
			nodes.add(l.getToNode());
		}

		return new Path(nodes, links, invPath.travelTime, invPath.travelCost);
	}


	



	private void addBushwhackingLegFromLinkToFacilityIfNecessary( final Facility toFacility, final Person person,
			final Link egressActLink, double now, final List<PlanElement> result,
			final PopulationFactory populationFactory, final String stageActivityType,
			Config config ) {


		if( isNotNeedingBushwhackingLeg( toFacility ) ) {
			return;
		}

		Coord startCoord = egressActLink.getToNode().getCoord() ;
		Gbl.assertNotNull( startCoord );

		final Id<Link> startLinkId = egressActLink.getId();

		// check whether we already have an identical interaction activity directly before
		PlanElement lastPlanElement = result.get( result.size() - 1 );
		if ( lastPlanElement instanceof Leg ) {
			final Activity interactionActivity = createInteractionActivity( startCoord, startLinkId, stageActivityType );
			result.add( interactionActivity ) ;
		} else {
			// don't add another (interaction) activity
			// TODO: assuming that this is an interaction activity, e.g. walk - drt interaction - walk
			// Not clear what we should do if it is not an interaction activity (and how that could happen).
		}

		Id<Link> endLinkId = toFacility.getLinkId();
		if ( endLinkId==null ) {
			endLinkId = startLinkId;
		}

		if (mode.equals(TransportMode.walk)) {
			Leg egressLeg = populationFactory.createLeg( TransportMode.non_network_walk ) ;
			egressLeg.setDepartureTime( now );
			routeBushwhackingLeg(person, egressLeg, startCoord, toFacility.getCoord(), now, startLinkId, endLinkId, populationFactory, config ) ;
			result.add( egressLeg ) ;
		} else {
			Facility fromFacility = FacilitiesUtils.wrapLink(egressActLink);
			result.addAll(walkRouter.calcRoute(fromFacility, toFacility, now, person));
		}
	}

	private static boolean isNotNeedingBushwhackingLeg( Facility toFacility ){
		if ( toFacility.getCoord() == null ) {
			// facility does not have a coordinate; we cannot bushwhack
			return true;
		}
		// trip ends on link; no need to bushwhack (this is, in fact, not totally clear: might be link on network of other mode)
		return toFacility instanceof LinkWrapperFacility;
	}

	private double addBushwhackingLegFromFacilityToLinkIfNecessary( final Facility fromFacility, final Person person,
			final Link accessActLink, double now, final List<PlanElement> result,
			final PopulationFactory populationFactory, final String stageActivityType,
			Config config ) {
		if ( isNotNeedingBushwhackingLeg( fromFacility ) ) {
			return now ;
		}

		Coord endCoord  = accessActLink.getToNode().getCoord() ;
		// yyyy think about better solution: this may generate long walks along the link. (e.g. orthogonal projection)
		Gbl.assertNotNull(endCoord);

		if (mode.equals(TransportMode.walk)) {
			Leg accessLeg = populationFactory.createLeg( TransportMode.non_network_walk ) ;
			accessLeg.setDepartureTime( now );

			final Id<Link> startLinkId = fromFacility.getLinkId() ;
			if ( startLinkId==null ){
				accessActLink.getId();
			}

			now += routeBushwhackingLeg(person, accessLeg, fromFacility.getCoord(), endCoord, now, startLinkId, accessActLink.getId(), populationFactory,
					config ) ;
			// yyyy might be possible to set the link ids to null. kai & dominik, may'16

			result.add( accessLeg ) ;
		} else {
			Facility toFacility = FacilitiesUtils.wrapLink(accessActLink);
			List<? extends PlanElement> accessTrip = walkRouter.calcRoute(fromFacility, toFacility, now, person);
			for (PlanElement planElement: accessTrip) {
				now = TripRouter.calcEndOfPlanElement( now, planElement, config ) ;
			}
			result.addAll(accessTrip);
		}

		final Activity interactionActivity = createInteractionActivity(endCoord, accessActLink.getId(), stageActivityType );
		result.add( interactionActivity ) ;

		return now;
	}

	private static Activity createInteractionActivity( final Coord interactionCoord, final Id<Link> interactionLink, final String mode ) {
		Activity act = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(interactionCoord, interactionLink, mode);
		act.setMaximumDuration(0.0);
		return act;
	}

	private static double routeBushwhackingLeg( Person person, Leg leg, Coord fromCoord, Coord toCoord, double depTime,
			Id<Link> dpLinkId, Id<Link> arLinkId, PopulationFactory pf, Config config ) {
		ModeRoutingParams params = null ;
		ModeRoutingParams tmp;
		final Map<String, ModeRoutingParams> paramsMap = config.plansCalcRoute().getModeRoutingParams();
		if ( (tmp = paramsMap.get( TransportMode.non_network_walk ) ) != null ){
			params = tmp;
		} else if ( (tmp = paramsMap.get(  TransportMode.walk ) ) != null ) {
			params = tmp ;
		} else{
			params = new ModeRoutingParams();
			// old defaults
			params.setBeelineDistanceFactor( 1.3 );
			params.setTeleportedModeSpeed( 2.0 );
		}
		return routeBushwhackingLeg(person, leg, fromCoord, toCoord, depTime, dpLinkId, arLinkId, pf, params);
	}

	static double routeBushwhackingLeg(Person person, Leg leg, Coord fromCoord, Coord toCoord, double depTime,
			Id<Link> dpLinkId, Id<Link> arLinkId, PopulationFactory pf, ModeRoutingParams params) {
		// I don't think that it makes sense to use a RoutingModule for this, since that again makes assumptions about how to
		// map facilities, and if you follow through to the teleportation routers one even finds activity wrappers, which is yet another
		// complication which I certainly don't want here.  kai, dec'15

		// dpLinkId, arLinkId need to be in Route for lots of code to function.   So I am essentially putting in the "street address"
		// for completeness. Note that if we are walking to a parked car, this can be different from the car link id!!  kai, dec'15

		// make simple assumption about distance and walking speed
		double dist = CoordUtils.calcEuclideanDistance(fromCoord,toCoord);

		// create an empty route, but with realistic travel time
		Route route =pf.getRouteFactories().createRoute(Route.class, dpLinkId, arLinkId ); 

		Gbl.assertNotNull( params );
		double beelineDistanceFactor = params.getBeelineDistanceFactor();
		double networkTravelSpeed = params.getTeleportedModeSpeed();

		double estimatedNetworkDistance = dist * beelineDistanceFactor;
		int travTime = (int) (estimatedNetworkDistance / networkTravelSpeed);
		route.setTravelTime(travTime);
		route.setDistance(estimatedNetworkDistance);
		leg.setRoute(route);
		leg.setDepartureTime(depTime);
		leg.setTravelTime(travTime);
		return travTime;
	}

	@Override
	public String toString() {
		return "[NetworkRoutingModule: mode="+this.mode+"]";
	}


	/*package (Tests)*/ double routeLeg(Person person, Leg leg, Link fromLink, Link toLink, double depTime) {
		double travTime;

		Node startNode = fromLink.getToNode();	// start at the end of the "current" link
		Node endNode = toLink.getFromNode(); // the target is the start of the link


		if (toLink != fromLink) { // (a "true" route)

			Id<Vehicle> vehicleId = VehicleUtils.getVehicleId(person, leg.getMode());
			Vehicle vehicle = scenario.getVehicles().getVehicles().get(vehicleId);
			Path path = leastCostPathCalculator.calcLeastCostPath(startNode, endNode, depTime, person, vehicle);
			if (path == null) throw new RuntimeException("No route found from node " + startNode.getId() + " to node " + endNode.getId() + ".");

			NetworkRoute route = this.populationFactory.getRouteFactories().createRoute(NetworkRoute.class, fromLink.getId(), toLink.getId());
			route.setLinkIds(fromLink.getId(), NetworkUtils.getLinkIds(path.links), toLink.getId());
			route.setTravelTime((int) path.travelTime);
			route.setTravelCost(path.travelCost);
			route.setDistance(RouteUtils.calcDistance(route, 1.0,1.0, this.network ) );
			leg.setRoute(route);
			travTime = (int) path.travelTime;

		} else {
			// create an empty route == staying on place if toLink == endLink
			// note that we still do a route: someone may drive from one location to another on the link. kai, dec'15
			NetworkRoute route = this.populationFactory.getRouteFactories().createRoute(NetworkRoute.class, fromLink.getId(), toLink.getId());
			route.setTravelTime(0);
			route.setDistance(0.0);
			leg.setRoute(route);
			travTime = 0;
		}

		leg.setDepartureTime(depTime);
		leg.setTravelTime(travTime);

		return travTime;
	}

}
