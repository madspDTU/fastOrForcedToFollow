package fastOrForcedToFollow.scoring;

import java.util.HashMap;
import java.util.Map;

import org.matsim.core.api.internal.MatsimParameters;

public class FFFScoringParameters implements MatsimParameters{

	public final Map<String, FFFModeUtilityParameters> modeParams;
	public final double abortedPlanUtility;

	private FFFScoringParameters(final Map<String, FFFModeUtilityParameters> modeParams,
			final double abortedPlanScore){
		this.modeParams = modeParams;
		this.abortedPlanUtility = abortedPlanScore;
	}

	public static final class Builder {
		private Map<String, FFFModeUtilityParameters> modeParams = new HashMap<String,
				FFFModeUtilityParameters>();
		private double abortedPlanScore = 0;


		public Builder(){};

		public Builder setModeParameters(final Map<String, FFFModeUtilityParameters> modeParams ) {
			this.modeParams = modeParams;
			return this;
		}

		public Builder setAbortPlanScore(final double abortScore) {
			this.abortedPlanScore = abortScore;
			return this;
		}

		public FFFScoringParameters build() {
			return new FFFScoringParameters(modeParams, abortedPlanScore);
		}
	}

}
