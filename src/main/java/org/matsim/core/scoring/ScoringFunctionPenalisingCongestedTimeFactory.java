package org.matsim.core.scoring;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.scoring.SumScoringFunction.ActivityScoring;
import org.matsim.core.scoring.SumScoringFunction.LegScoring;
import org.matsim.core.scoring.SumScoringFunction.AgentStuckScoring;
import org.matsim.core.scoring.SumScoringFunction.MoneyScoring;
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

		//LegScoring
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
				ModeUtilityParameters modeParams = params.modeParams.get(leg.getMode());

				double travelTime = leg.getTravelTime();
				double distance = leg.getRoute().getDistance(); 
				double freeSpeed = (double) person.getAttributes().getAttribute("v_0");	
				double congestedTravelTime = 0;
				if( leg.getMode().equals(TransportMode.bike) ){
					if(distance == 0){
						travelTime = 0;
					} else {
						travelTime -= 1;
						congestedTravelTime = travelTime - Math.ceil(distance / freeSpeed);
					}
				}


				//@Kai, I hope you'll survive seing such poor coding :D I have used the coefficient for length (which is
				// unused) to store the coefficient for congested travel time. 
				this.score += travelTime * modeParams.marginalUtilityOfTraveling_s + congestedTravelTime * modeParams.marginalUtilityOfDistance_m; //Horrible way to do it, but it will work.
				//In the future make a better way to access coefficient. 
			}
		} );

		//Empty methods
		{
			//ActivityScoring (nothing happens)
			ssf.addScoringFunction( new ActivityScoring(){
				@Override
				public void finish() {
				}
				@Override
				public double getScore() {
					return 0;
				}
				@Override
				public void handleFirstActivity(Activity act) {
				}
				@Override
				public void handleActivity(Activity act) {
				}
				@Override
				public void handleLastActivity(Activity act) {
				}	
			});

			//AgentStuckScoring (nothing happens)
			ssf.addScoringFunction(new AgentStuckScoring(){
				@Override
				public void finish() {
				}
				@Override
				public double getScore() {
					return 0;
				}
				@Override
				public void agentStuck(double time) {
				}			
			});

			//MoneyScoring (nothing happens)
			ssf.addScoringFunction(new MoneyScoring(){
				@Override
				public void finish() {
				}
				@Override
				public double getScore() {
					return 0;
				}
				@Override
				public void addMoney(double amount) {
				}			
			});
		}

		return ssf ;
	}
}
