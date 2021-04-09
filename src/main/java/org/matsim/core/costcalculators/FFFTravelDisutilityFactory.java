package org.matsim.core.costcalculators;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import fastOrForcedToFollow.configgroups.FFFScoringConfigGroup;

public class FFFTravelDisutilityFactory implements TravelDisutilityFactory {

	private double utilityOfTraveling_s;
	private double utilityOfCongestedTraveling_s;

	public FFFTravelDisutilityFactory(String mode, Config config ) {
		this.utilityOfTraveling_s = ConfigUtils.addOrGetModule(config, FFFScoringConfigGroup.class).getMyScoringParameters().
				modeParams.get(mode).marginalUtilityOfTraveling_s;
		this.utilityOfCongestedTraveling_s = ConfigUtils.addOrGetModule(config, FFFScoringConfigGroup.class).getMyScoringParameters().
				modeParams.get(mode).marginalUtilityOfCongestedTraveling_s;
	}
			
	@Override
	public TravelDisutility createTravelDisutility(TravelTime timeCalculator) {
		return new FFFTravelDisutility(timeCalculator, -utilityOfTraveling_s, -utilityOfCongestedTraveling_s);
	}

}
