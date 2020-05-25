package fastOrForcedToFollow.configgroups;

import java.util.HashMap;

import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup.StringGetter;
import org.matsim.core.mobsim.qsim.qnetsimengine.QFFFAbstractNode;

public class FFFNodeConfigGroup extends ReflectiveConfigGroup {

	static final String GROUP_NAME = "fastOrForcedToFollowNodeModelling";

	private HashMap<String, Integer>  roadtypeToValueMap;
	private String[] roadTypes = new String[] {"motorway","motorway_link","trunk","trunk_link","primary","primary_link",
			"secondary","secondary_link","tertiary","tertiary_link","unclassified","residential","living_street","cycleway","footway","path","track",
			"pedestrian", "service"};
	
	
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

	private boolean omitLinkLeaveEvents = false;

	private boolean approximateNullLinkToLinkTravelTimes = false;

	private int smallRoadLeftBufferCapacity = QFFFAbstractNode.smallRoadLeftBufferCapacity; //change to 3 or to.
	
//	@StringGetter( "bundleAngleTolerance" )	
	public double getBundleTol() {
		return this.bundleTol;
	}
//	@StringSetter( "bundleAngleTolerance" )
	public void setBundleTol(final double bundleTol) {
		this.bundleTol = bundleTol;
	}
	
	public boolean getOmitLinkLeaveEvents() {
		return this.omitLinkLeaveEvents;
	}
	public void setOmitLinkLeaveEvents(boolean skipLinkLeaveEvents) {
		this.omitLinkLeaveEvents = skipLinkLeaveEvents;
	}
	
	public boolean getApproximateNullLinkToLinkTravelTimes() {
		return this.approximateNullLinkToLinkTravelTimes;
	}
	public void setApproximateNullLinkToLinkTravelTimes(boolean approximateNullLinkToLinkTravelTimes) {
		this.approximateNullLinkToLinkTravelTimes  = approximateNullLinkToLinkTravelTimes;
	}
	

	public FFFNodeConfigGroup() {
		super( GROUP_NAME );
		this.roadtypeToValueMap = new HashMap<String,Integer>();
		for(int i = 0; i < roadTypes.length; i++) {
			this.roadtypeToValueMap.put(roadTypes[i], -i);
		}
	}
	
	public HashMap<String, Integer> getRoadTypeToValueMap() {
		return this.roadtypeToValueMap;
	}

	public int getSmallRoadLeftBufferCapacity() {
		return this.smallRoadLeftBufferCapacity ;
	}
	
	public void setSmallRoadLeftBufferCapacity(int cap) {
		this.smallRoadLeftBufferCapacity = cap ;
	}
}
