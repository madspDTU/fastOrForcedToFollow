package fastOrForcedToFollow.configgroups;

import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup.StringGetter;

public class FFFNodeConfigGroup extends ReflectiveConfigGroup {

	static final String GROUP_NAME = "fastOrForcedToFollowNodeModelling";

	/**
	 * 
	 */
	private double bicycleDelay = 1.;
//	@StringGetter( "bicycleDelay" )
	public double getBicycleDelay() {
		return this.bicycleDelay;
	}
//	@StringSetter( "bicycleDelay" )
	public void setBicycleDelay(final double bicycleDelay) {
		this.bicycleDelay = bicycleDelay;
	}
	
	/**
	 * 
	 */
	private double carDelay = 1.;
//	@StringGetter( "carDelay" )	
	public double getCarDelay() {
		return this.carDelay;
	}
//	@StringSetter( "carDelay" )
	public void setCarDelay(final double carDelay) {
		this.carDelay = carDelay;
	}

	/**
	 * 
	 */
	private double bundleTol = Math.PI/12;
//	@StringGetter( "bundleAngleTolerance" )	
	public double getBundleTol() {
		return this.bundleTol;
	}
//	@StringSetter( "bundleAngleTolerance" )
	public void setBundleTol(final double bundleTol) {
		this.bundleTol = bundleTol;
	}

	public FFFNodeConfigGroup() {
		super( GROUP_NAME );
	}
}
