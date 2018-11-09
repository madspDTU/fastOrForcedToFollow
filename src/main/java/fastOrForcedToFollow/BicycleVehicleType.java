package fastOrForcedToFollow;

import org.matsim.api.core.v01.Id;
import org.matsim.vehicles.EngineInformation;
import org.matsim.vehicles.EngineInformation.FuelType;
import org.matsim.vehicles.EngineInformationImpl;
import org.matsim.vehicles.FreightCapacity;
import org.matsim.vehicles.VehicleCapacity;
import org.matsim.vehicles.VehicleType;

public class BicycleVehicleType implements VehicleType{
	
	private double width = 0.61;
	private double maxVelocity = Double.POSITIVE_INFINITY;
	private double length = Runner.lambda_c;
    private double pcuEquivalents = 1./4;
    private double flowEfficiencyFactor = 1.0;
	private EngineInformation engineInformation = new EngineInformationImpl(FuelType.biodiesel, 0);
	private String description = "Average bicycle";
	
	private double accessTime = 0.0;
	
	private double egressTime = 0.0;

	final private Id<VehicleType> id;

	private DoorOperationMode doorOperationMode = DoorOperationMode.serial;

	public BicycleVehicleType(){
		id = Id.create("BicycleVehicleType", VehicleType.class);
	}
	
	private VehicleCapacity capacity = new VehicleCapacity() {
		
		@Override
		public void setStandingRoom(Integer standingRoom) {
		}
		
		@Override
		public void setSeats(Integer seats) {

		}
		
		@Override
		public void setFreightCapacity(FreightCapacity freightCap) {
			
		}
		
		@Override
		public Integer getStandingRoom() {
			return 0;
		}
		
		@Override
		public Integer getSeats() {
			return 1;
		}
		
		@Override
		public FreightCapacity getFreightCapacity() {
			return null;
		}
	};
	
	BicycleVehicleType(Id<VehicleType> id){
		this.id = id;
	}
	
	
	@Override
	public void setDescription(String desc) {
		this.description = desc;
	}

	@Override
	public void setLength(double length) {
		this.length = length;
	}

	@Override
	public void setWidth(double width) {
		this.width = width;
	}

	@Override
	public void setMaximumVelocity(double meterPerSecond) {
		this.maxVelocity = meterPerSecond;
	}

	@Override
	public void setEngineInformation(EngineInformation currentEngineInfo) {
		this.engineInformation = currentEngineInfo;
	}

	@Override
	public void setCapacity(VehicleCapacity capacity) {
		this.capacity = capacity;
	}

	@Override
	public double getWidth() {
		return this.width;
	}

	@Override
	public double getMaximumVelocity() {
		return this.maxVelocity;
	}

	@Override
	public double getLength() {
		return this.length;
	}

	@Override
	public EngineInformation getEngineInformation() {
		return this.engineInformation;
	}

	@Override
	public String getDescription() {
		return this.description;
	}

	@Override
	public VehicleCapacity getCapacity() {
		return this.capacity;
	}

	@Override
	public Id<VehicleType> getId() {
		return this.id;
	}

	@Override
	public double getAccessTime() {
		return this.accessTime;
	}

	@Override
	public void setAccessTime(double seconds) {
		this.accessTime = seconds;
	}

	@Override
	public double getEgressTime() {
		return this.egressTime;
	}

	@Override
	public void setEgressTime(double seconds) {
		this.egressTime = seconds;
	}

	@Override
	public DoorOperationMode getDoorOperationMode() {
		return this.doorOperationMode;
	}

	@Override
	public void setDoorOperationMode(DoorOperationMode mode) {
		this.doorOperationMode = mode;
	}

	@Override
	public double getPcuEquivalents() {
		return this.pcuEquivalents;
	}

	@Override
	public void setPcuEquivalents(double pcuEquivalents) {
		this.pcuEquivalents = pcuEquivalents;
	}

	@Override
	public double getFlowEfficiencyFactor() {
		return this.flowEfficiencyFactor;
	}

	@Override
	public void setFlowEfficiencyFactor(double flowEfficiencyFactor) {
		this.flowEfficiencyFactor = flowEfficiencyFactor;
	}

}
