package org.matsim.core.scoring;

public class MyModeUtilityParameters {
	
	public static class Builder {
		private double marginalUtilityOfTraveling_s = 0;
		private double marginalUtilityOfCongestedTraveling_s = 0;
		private double marginalUtilityOfDistance_m = 0;
		private double constant = 0;

		
		public Builder() {}

		public Builder setMarginalUtilityOfTraveling_s(double marginalUtilityOfTraveling_s) {
			this.marginalUtilityOfTraveling_s = marginalUtilityOfTraveling_s;
			return this;
		}
		
		public Builder setMarginalUtilityOfDistance_m(double marginalUtilityOfDistance_m) {
			this.marginalUtilityOfDistance_m = marginalUtilityOfDistance_m;
			return this;
		}

		public Builder setMarginalUtilityOfCongestedTraveling_s(double marginalUtilityOfCongestedTraveling_s) {
			this.marginalUtilityOfTraveling_s = marginalUtilityOfCongestedTraveling_s;
			return this;
		}

		public Builder setConstant(double constant) {
			this.constant = constant;
			return this;
		}

		public MyModeUtilityParameters build() {
			return new MyModeUtilityParameters(
					marginalUtilityOfTraveling_s,
					marginalUtilityOfCongestedTraveling_s,
					marginalUtilityOfDistance_m,
					constant);
		}
	}

	public MyModeUtilityParameters(
			double marginalUtilityOfTraveling_s,
			double marginalUtilityOfCongestedTraveling_s,
			double marginalUtilityOfDistance_m,
			double constant
			) {
		this.marginalUtilityOfTraveling_s = marginalUtilityOfTraveling_s;
		this.marginalUtilityOfCongestedTraveling_s = marginalUtilityOfCongestedTraveling_s;
		this.marginalUtilityOfDistance_m = marginalUtilityOfDistance_m;
		this.constant = constant;
	}

	public final double marginalUtilityOfTraveling_s;
	public final double marginalUtilityOfCongestedTraveling_s;
	public final double marginalUtilityOfDistance_m;
	public final double constant;

}
