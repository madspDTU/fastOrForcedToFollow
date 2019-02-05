package org.matsim.core.scoring;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.scoring.SumScoringFunction.LegScoring;
import org.matsim.core.scoring.functions.ModeUtilityParameters;
import org.matsim.core.scoring.functions.ScoringParameters;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;

import com.google.inject.Inject;

public class ScoringFunctionPenalisingCongestedTimeFactory implements ScoringFunctionFactory {
	@Inject private ScoringParametersForPerson pparams ;
	@Inject MainModeIdentifier mainModeIdentifier ;
	@Override public ScoringFunction createNewScoringFunction( final Person person ) {
		final ScoringParameters params = pparams.getScoringParameters( person );

		SumScoringFunction ssf = new SumScoringFunction();

		ssf.addScoringFunction( new LegScoring() {
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
				if(leg.getMode().equals(TransportMode.bike)){
					ModeUtilityParameters modeParams = params.modeParams.get(leg.getMode());

					double travelTime = leg.getTravelTime();
					double congestedTravelTime = 0;
					if(person.getAttributes().getAsMap().containsKey("v_0")){
						double freeSpeed = (double) person.getAttributes().getAttribute("v_0");		
						double freeFlowTime = leg.getRoute().getDistance() / freeSpeed;
						congestedTravelTime = travelTime - Math.ceil(freeFlowTime);
					}

					this.score += travelTime * modeParams.marginalUtilityOfTraveling_s + congestedTravelTime * modeParams.marginalUtilityOfDistance_m; //Horrible way to do it, but it will work.
					//In the future make a better way to access coefficient. 
				}
			} 
		} );
		return ssf ;
	}
}
