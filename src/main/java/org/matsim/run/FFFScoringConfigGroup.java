package org.matsim.run;

import java.util.Map;

import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup.StringGetter;

import fastOrForcedToFollow.scoring.FFFModeUtilityParameters;
import fastOrForcedToFollow.scoring.FFFScoringParameters;

public class FFFScoringConfigGroup extends ReflectiveConfigGroup {

	static final String GROUP_NAME = "fastOrForcedToFollowScoring";
	
	private FFFScoringParameters scoringParameters;
	
//	@StringGetter( "scoringParameters" )
	public FFFScoringParameters getMyScoringParameters() {
		return this.scoringParameters;
	}
	
	public void setScoringParameters(Map<String, FFFModeUtilityParameters> modeParams){
		FFFScoringParameters.Builder builder = new FFFScoringParameters.Builder();
		builder.setModeParameters(modeParams);
	
		double worstUtility = 0;
		for(FFFModeUtilityParameters util : modeParams.values()){
			double combined = util.marginalUtilityOfCongestedTraveling_s + util.marginalUtilityOfTraveling_s;
			if(combined < worstUtility){
				worstUtility = combined;
			}
		}
		builder.setModeParameters(worstUtility);
		scoringParameters = builder.build();
	}
	
	
	public FFFScoringConfigGroup() {
		super( GROUP_NAME );
	}
}
