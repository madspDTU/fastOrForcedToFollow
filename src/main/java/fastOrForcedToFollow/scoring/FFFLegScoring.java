package fastOrForcedToFollow.scoring;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scoring.SumScoringFunction.LegScoring;


public class FFFLegScoring implements LegScoring{

	private FFFScoringParameters params;
	private Network network;
	private Person person;

	private static final Logger log = Logger.getLogger(FFFLegScoring.class);

	public FFFLegScoring(final FFFScoringParameters params, Network network, Person person) {
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
		FFFModeUtilityParameters modeParams = params.modeParams.get(leg.getMode());

		double travelTime = leg.getTravelTime().seconds();
		double distance = leg.getRoute().getDistance(); 
		double congestedTravelTime = 0;
		if( leg.getMode().equals(TransportMode.bike) ){
			if(distance == 0){
				travelTime = 0;
			} else {
				travelTime -= 1;
				double freeSpeed = (double) person.getAttributes().getAttribute("v_0");	
				congestedTravelTime = travelTime - Math.ceil(distance / freeSpeed);
				if(congestedTravelTime < 0) {
					log.warn("Cyclist " + person.getId() + "(v_0=" + freeSpeed+ ") had negative (" + congestedTravelTime + ") congested travel time. TT=" + 
							travelTime + ", dist=" + distance);
				}
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
				if(congestedTravelTime < 0) {
					log.warn("Car " + person.getId() + " had negative (" + congestedTravelTime + ") congested travel time. TT=" + 
							travelTime + ", dist=" + distance);
				}
			}
		} else if( leg.getMode().equals(TransportMode.truck)) {
			if(distance == 0){
				travelTime = 0;
			} else {
				travelTime -= 1;
				NetworkRoute route = (NetworkRoute) leg.getRoute();
				double freeFlowTravelTime = 0;
				for(Id<Link> id : route.getLinkIds()){
					Link link = network.getLinks().get(id);
					double freeSpeed = Math.min(80/3.6, link.getFreespeed());
					freeFlowTravelTime += Math.ceil(link.getLength() / freeSpeed);
				}
				Link link = network.getLinks().get(leg.getRoute().getEndLinkId());
				double freeSpeed = Math.min(80/3.6, link.getFreespeed());
				freeFlowTravelTime += Math.ceil(link.getLength() / freeSpeed);
				congestedTravelTime = travelTime - freeFlowTravelTime;
				if(congestedTravelTime < 0) {
					log.warn("Truck " + person.getId() + " had negative (" + congestedTravelTime + ") congested travel time. TT=" + 
							travelTime + ", dist=" + distance);
				}
			}
		}

		//TODO Here you would be able to change the scoring function of trucks right away (e.g. penalise congTime by 200% instead of 150%). 
		this.score += travelTime * modeParams.marginalUtilityOfTraveling_s + 
				congestedTravelTime * modeParams.marginalUtilityOfCongestedTraveling_s +
				distance * modeParams.marginalUtilityOfDistance_m     +     modeParams.constant;
	}
}
