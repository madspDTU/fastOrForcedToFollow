/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Provider;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.MyNetworkInverter;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.LinkToLinkTravelTime;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

import com.google.inject.name.Named;

public class FFFLinkToLinkRouting
    implements Provider<RoutingModule>
{
	private static final Logger log = Logger.getLogger( FFFLinkToLinkRouting.class ) ;
    private final String mode;

    @Inject PopulationFactory populationFactory;
	@Inject SingleModeNetworksCache singleModeNetworksCache;
	@Inject SingleModeInvertedNetworksCache singleModeInvertedNetworksCache;
	@Inject SingleModeInvertedTravelTimesCache singleModeInvertedTravelTimesCache;
	@Inject LeastCostPathCalculatorFactory leastCostPathCalcFactory;
    @Inject Map<String, TravelDisutilityFactory> travelDisutilities;
    @Inject Network network;
    @Inject Scenario scenario;
    @Inject LinkToLinkTravelTime travelTimes;
    @Inject	@Named(TransportMode.walk) private RoutingModule accessEgressRouter;

	
    
	
	

    public FFFLinkToLinkRouting(String mode)
    {
        this.mode = mode;
    }


    @Override
    public RoutingModule get()
    {
    	Network filteredNetwork = null;

		// Ensure this is not performed concurrently by multiple threads!
		synchronized (this.singleModeNetworksCache.getSingleModeNetworksCache()) {
			filteredNetwork = this.singleModeNetworksCache.getSingleModeNetworksCache().get(mode);
			if (filteredNetwork == null) {
				log.info("Filtering network for " + mode);
				TransportModeNetworkFilter filter = new TransportModeNetworkFilter(network);
				Set<String> modes = new HashSet<>(Arrays.asList(mode));
				filteredNetwork = NetworkUtils.createNetwork();
				filter.filter(filteredNetwork, modes);
				new NetworkCleaner().run(filteredNetwork); // mads
				this.singleModeNetworksCache.getSingleModeNetworksCache().put(mode, filteredNetwork);
			}
		}
		

		
		Network invertedNetwork = null;
		
		synchronized (this.singleModeInvertedNetworksCache.getSingleModeInvertedNetworksCache()){
			invertedNetwork = this.singleModeInvertedNetworksCache.getSingleModeInvertedNetworksCache().get(mode);
			if (invertedNetwork == null) {
				log.info("Inverting network for " + mode);
				invertedNetwork = new MyNetworkInverter(filteredNetwork).getInvertedNetwork();
				this.singleModeInvertedNetworksCache.getSingleModeInvertedNetworksCache().put(mode,invertedNetwork);
			}
		}
		
		FFFTravelTimesInvertedNetworkProxy invertedTravelTimes = null;
		synchronized (this.singleModeInvertedTravelTimesCache.getSingleModeInvertedTravelTimesCache()) {
			invertedTravelTimes = this.singleModeInvertedTravelTimesCache.getSingleModeInvertedTravelTimesCache().get(mode);
			if(invertedTravelTimes == null) {
				log.info("Creating inverted travel times for " + mode);
				invertedTravelTimes = new FFFTravelTimesInvertedNetworkProxy(filteredNetwork, travelTimes);
				this.singleModeInvertedTravelTimesCache.getSingleModeInvertedTravelTimesCache().put(mode, invertedTravelTimes);
			}
		}
		
		TravelDisutility travelDisutility = travelDisutilities.get(mode).createTravelDisutility(invertedTravelTimes);
		
		RoutingModule walkRouter = accessEgressRouter;
		if(this.mode.equals(TransportMode.walk)) {
			walkRouter = null;
		}
    	
		for(Link link : filteredNetwork.getLinks().values()) {
			Gbl.assertIf(link.getAllowedModes().size()==1);
			for(String linkMode : link.getAllowedModes()) {
				Gbl.assertIf(linkMode.equals(this.mode));
			}
		}
        return new FFFLinkToLinkRoutingModule(mode, populationFactory, filteredNetwork,
                leastCostPathCalcFactory, travelDisutility,  invertedTravelTimes, invertedNetwork,
                scenario, walkRouter);
    }
    
    public static class FFFTravelTimesInvertedNetworkProxy implements TravelTime {
		private final HashMap<Id<Node>,Link> nodeIdToLinkConversion;
		private LinkToLinkTravelTime linkToLinkTravelTime;


		private FFFTravelTimesInvertedNetworkProxy(Network network, LinkToLinkTravelTime l2ltt)
		{
			this.linkToLinkTravelTime = l2ltt;
			this.nodeIdToLinkConversion = new HashMap<Id<Node>, Link>(network.getLinks().size(),1.1f);
			for(Link link : network.getLinks().values()) {
				Id<Node> nodeId = Id.createNodeId(link.getId());
				this.nodeIdToLinkConversion.put(nodeId, link);
			}
		}


		/**
		 * In this case the link given as parameter is a link from the inverted network.
		 * 
		 * @see org.matsim.core.router.util.TravelTime#getLinkTravelTime(Link, double, Person,
		 *      Vehicle)
		 */
		@Override
		public double getLinkTravelTime(Link invLink, double time, Person person, Vehicle vehicle){
			Link fromLink = getFromLink(invLink);
			Link toLink = getToLink(invLink);
			return linkToLinkTravelTime.getLinkToLinkTravelTime(fromLink, toLink, time, person, vehicle);
		}


		private Link getFromLink(Link invLink) {
			return nodeIdToLinkConversion.get(invLink.getFromNode().getId());
		}
		
		private Link getToLink(Link invLink) {
			return nodeIdToLinkConversion.get(invLink.getToNode().getId());
		}
	}

}