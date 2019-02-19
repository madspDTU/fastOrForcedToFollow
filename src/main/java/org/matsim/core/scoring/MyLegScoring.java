package org.matsim.core.scoring;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scoring.SumScoringFunction.LegScoring;


public class MyLegScoring implements LegScoring{

	private MyScoringParameters params;
	private Network network;
	private Person person;

	public MyLegScoring(final MyScoringParameters params, Network network, Person person) {
		this.params = params;
		this.network = network;
		this.person = person;
	}

	private double score = 0. ;

	@Override
	public void finish() {

	}

	@Override
	public double getScore() {
		return score ;
	}


	@Override
	public void handleLeg(Leg leg) {
		MyModeUtilityParameters modeParams = params.modeParams.get(leg.getMode());

		double travelTime = leg.getTravelTime();
		double distance = leg.getRoute().getDistance(); 
		double congestedTravelTime = 0;
		if( leg.getMode().equals(TransportMode.bike) ){
			if(distance == 0){
				travelTime = 0;
			} else {
				travelTime -= 1;
				double freeSpeed = (double) person.getAttributes().getAttribute("v_0");	
				congestedTravelTime = travelTime - Math.ceil(distance / freeSpeed);
			}
		} else if( leg.getMode().equals(TransportMode.car)){
			if(distance == 0){
				travelTime = 0;
			} else {
				travelTime -= 1;
				NetworkRoute route = (NetworkRoute) leg.getRoute();
				double freeFlowTravelTime = 0;
				for(Id<Link> id : route.getLinkIds()){
					Link link = network.getLinks().get(id);
					freeFlowTravelTime += Math.ceil(link.getLength() / link.getFreespeed());
				}
				Link link = network.getLinks().get(leg.getRoute().getEndLinkId());
				freeFlowTravelTime += Math.ceil(link.getLength() / link.getFreespeed());
				congestedTravelTime = travelTime - freeFlowTravelTime;
			}
		}
		this.score += travelTime * modeParams.marginalUtilityOfTraveling_s + 
				congestedTravelTime * modeParams.marginalUtilityOfCongestedTraveling_s +
				distance * modeParams.marginalUtilityOfDistance_m     +     modeParams.constant;
	}
}
