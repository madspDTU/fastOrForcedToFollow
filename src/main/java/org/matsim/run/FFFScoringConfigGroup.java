package org.matsim.run;

import java.util.Map;

import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup.StringGetter;

import fastOrForcedToFollow.scoring.MyModeUtilityParameters;
import fastOrForcedToFollow.scoring.MyScoringParameters;

public class FFFScoringConfigGroup extends ReflectiveConfigGroup {

	static final String GROUP_NAME = "fastOrForcedToFollowScoring";
	
	private MyScoringParameters scoringParameters;
	
//	@StringGetter( "scoringParameters" )
	public MyScoringParameters getMyScoringParameters() {
		return this.scoringParameters;
	}
	
	public void setScoringParameters(Map<String, MyModeUtilityParameters> modeParams){
		MyScoringParameters.Builder builder = new MyScoringParameters.Builder();
		builder.setModeParameters(modeParams);
	
		double worstUtility = 0;
		for(MyModeUtilityParameters util : modeParams.values()){
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
