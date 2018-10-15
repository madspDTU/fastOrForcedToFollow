package fastOrForcedToFollow;

public class SpatialLTM extends BasicLTM {

	@Override
	public double getSafetyBufferTime(double speed){
		return Runner.l / speed;
	}

}
