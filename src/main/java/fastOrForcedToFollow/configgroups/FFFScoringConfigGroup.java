package fastOrForcedToFollow.configgroups;

import java.util.Map;

import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup.StringGetter;

import fastOrForcedToFollow.scoring.FFFModeUtilityParameters;
import fastOrForcedToFollow.scoring.FFFScoringParameters;

public class FFFScoringConfigGroup extends ReflectiveConfigGroup {

	static final String GROUP_NAME = "fastOrForcedToFollowScoring";
		
	private FFFScoringParameters scoringParameters;
	private double planBeta = 1;
//	private double planInertia = 2;
	private double threshold = 1.3; // Must be >= 1;
	private int maximumMemory = 20; // Must be >= 1
	private PlanSelectorType planSelectorType = PlanSelectorType.BoundedLogit;
	
	public enum PlanSelectorType{BoundedLogit, GradualBoundedLogit, BestBounded};
	
//	@StringGetter( "scoringParameters" )
	public FFFScoringParameters getMyScoringParameters() {
		return this.scoringParameters;
	}
	
	public double getPlanBeta() {
		return planBeta;
	}
	
	public void setPlanBeta(double planBeta) {
		this.planBeta = planBeta;
	}
	
//	public double getPlanInertia() {
//		return planInertia;
//	}
	
//	public void setPlanInertia(double planInertia) {
//		this.planInertia = planInertia;
//	}

//	@StringSetter( "scoringParameters" )
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
		builder.setAbortPlanScore(worstUtility);
		this.scoringParameters = builder.build();
	}
	
	
	public FFFScoringConfigGroup() {
		super( GROUP_NAME );
	}

	public double getThreshold() {
		return this.threshold;
	}

	public void setThreshold(double threshold) {
		if(threshold >= 1) {
			this.threshold = threshold;
		} else {
			double newthreshold = 1 + (threshold % 1);
			System.err.println("Threshold value " + threshold + " is less than 1, and thus invalid. Changed to " + newthreshold);	
			this.threshold = threshold;
		}
	}

	public int getMaximumMemory() {
		return this.maximumMemory;
	}

	public void setMaximumMemory(int maximumMemory) {
		if(maximumMemory >= 1) {
			this.maximumMemory = maximumMemory;
		} else {
			int newmaximumMemory = maximumMemory;
			if(newmaximumMemory == 0) {
				newmaximumMemory = 1;
			} else {
				newmaximumMemory *= -1;
			}
			System.err.println("Maximum memory value " + maximumMemory + " is less than 1, and thus invalid. Changed to " + newmaximumMemory);	
			this.maximumMemory = newmaximumMemory;
		}
	}

	public PlanSelectorType getPlanSelectorType() {
		return planSelectorType;
	}

	public void setPlanSelectorType(PlanSelectorType selectorType) {
		this.planSelectorType = selectorType;
	}
}
