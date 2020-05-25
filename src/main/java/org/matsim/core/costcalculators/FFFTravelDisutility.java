package org.matsim.core.costcalculators;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

public class FFFTravelDisutility implements TravelDisutility {

	private final TravelTime travelTime;
	private final double marginalCostOfTime;
	private final double marginalCostOfCongestedTime;

	FFFTravelDisutility(final TravelTime travelTime, final double marginalCostOfTime_s,
			final double marginalCostOfCongestedTime_s){
		this.travelTime = travelTime;
		this.marginalCostOfTime = marginalCostOfTime_s;
		this.marginalCostOfCongestedTime = marginalCostOfCongestedTime_s;
	}

	@Override
	public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle) {
		//TODO Consider using a different travelTime that returns tt and congestedTT immediately...
		double linkTravelTime = Math.ceil(this.travelTime.getLinkTravelTime(link, time, person, vehicle));
		if(vehicle == null) {
			return this.marginalCostOfTime * linkTravelTime;
		}
		double freespeedLinkTravelTime = Math.ceil(link.getLength() / vehicle.getType().getMaximumVelocity());
		double congestedTime = linkTravelTime - freespeedLinkTravelTime;
		return this.marginalCostOfTime * linkTravelTime + congestedTime * this.marginalCostOfCongestedTime;
	}

	@Override
	public double getLinkMinimumTravelDisutility(final Link link) {
		return Math.ceil(link.getLength() / link.getFreespeed()) * this.marginalCostOfTime;
	}

}
