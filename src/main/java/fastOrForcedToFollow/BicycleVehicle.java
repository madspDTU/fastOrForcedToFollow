package fastOrForcedToFollow;

import org.matsim.api.core.v01.Id;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

public class BicycleVehicle implements Vehicle{

	private final Id<Vehicle> id;
	private final BicycleVehicleType vehicleType;

	@Override
	public Id<Vehicle> getId() {
		return this.id;
	}

	@Override
	public VehicleType getType() {
		return vehicleType;
	}
	
	public BicycleVehicle( String id ){
		this.id = Id.createVehicleId(id);
		this.vehicleType = new BicycleVehicleType();
	}

}
