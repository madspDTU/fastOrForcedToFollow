package org.matsim.core.scoring;

import java.util.HashMap;
import java.util.Map;
import org.matsim.core.api.internal.MatsimParameters;

public class MyScoringParameters implements MatsimParameters{

	public final Map<String, MyModeUtilityParameters> modeParams;
	public final double abortedPlanScore;

	private MyScoringParameters(final Map<String, MyModeUtilityParameters> modeParams,
			final double abortedPlanScore){
		this.modeParams = modeParams;
		this.abortedPlanScore = abortedPlanScore;
	}

	public static final class Builder {
		private Map<String, MyModeUtilityParameters> modeParams = new HashMap<String,
				MyModeUtilityParameters>();
		private double abortedPlanScore = 0;


		public Builder(){};

		public Builder setModeParameters(final Map<String, MyModeUtilityParameters> modeParams ) {
			this.modeParams = modeParams;
			return this;
		}

		public Builder setModeParameters(final double abortScore) {
			this.abortedPlanScore = abortScore;
			return this;
		}

		public MyScoringParameters build() {
			return new MyScoringParameters(modeParams, abortedPlanScore);
		}
	}

}
