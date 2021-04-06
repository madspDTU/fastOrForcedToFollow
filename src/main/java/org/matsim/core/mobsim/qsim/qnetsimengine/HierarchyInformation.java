package org.matsim.core.mobsim.qsim.qnetsimengine;

public class HierarchyInformation implements Comparable<HierarchyInformation> {

	private int roadValue;
	private double capacity;

	public HierarchyInformation(int roadValue, double capacity) {
		this.setRoadValue(roadValue);
		this.setCapacity(capacity);
	}
	
	

	@Override
	public int compareTo(HierarchyInformation other) {
		int val = this.roadValue - other.roadValue;
		if(val != 0) {
			return val;
		}

		if(this.capacity == other.capacity) {
			return 0;
		} else if( this.capacity > other.capacity) {
			return 1;
		} else {
			return -1;
		}
	}

	public int getRoadValue() {
		return roadValue;
	}

	public void setRoadValue(int roadValue) {
		this.roadValue = roadValue;
	}

	public double getCapacity() {
		return capacity;
	}

	public void setCapacity(double capacity) {
		this.capacity = capacity;
	}

}
