package fastOrForcedToFollow;

/**
 * @author mpaulsen
 *
 */
public class SpatialLTM extends BasicLTM {

	@Override
	public double getSafetyBufferTime(double speed){
		return Runner.theta_0 / speed;
	}

}
