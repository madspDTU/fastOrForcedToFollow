package fastOrForcedToFollow.timeoutmodifiers;

import java.util.LinkedList;


public class ConflictingMovesData {
	
	public enum ModeIdentifier{Bicycle, Car};
	

	
	private LinkedList<Element> bicycleData;
	private LinkedList<Element> carData;
	
	public ConflictingMovesData(LinkedList<Element> bicycleData, LinkedList<Element> carData){
		this.bicycleData = bicycleData;
		this.carData = carData;
	}
	
	LinkedList<Element>  getBicycleData(){
		return this.bicycleData;
	}
	
	LinkedList<Element>  getCarData(){
		return this.carData;
	}
	
	
		
	public static class Element {
		private final int fromDirection;
		private final int toDirection;
		private final ModeIdentifier modeIdentifier;
		private final boolean sameRank;
			
		public Element(int fromDirection, int toDirection, ModeIdentifier modeIdentifier, boolean sameRank){
			this.fromDirection = fromDirection;
			this.toDirection = toDirection;
			this.modeIdentifier = modeIdentifier;
			this.sameRank = sameRank;
		}
		
		public int getFromDirection() {
			return this.fromDirection;
		}
		
		public int getToDirection() {
			return this.toDirection;
		}
		
		public ModeIdentifier getModeIdentifier() {
			return this.modeIdentifier;
		}
		
		public boolean isSameRank() {
			return this.sameRank;
		}
		
	}
	
}
