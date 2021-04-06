package fastOrForcedToFollow.timeoutmodifiers;

import java.util.LinkedList;

import org.matsim.core.mobsim.qsim.qnetsimengine.QFFFNodeUtils;

import fastOrForcedToFollow.timeoutmodifiers.ConflictingMovesData.Element;

public abstract class TimeoutModifier {
	
	public abstract void updateTimeouts(double[][] bicycleTimeouts, double[][] carTimeouts, int inDirection,
			int outDirection,  double nowish, double nowish2, boolean isSecondary[]);
	
	
	public static void updateTimeoutsWithConflictingMovesBicycleData(double[][] bicycleTimeouts, double[][] carTimeouts, 
			double nowish, double nowish2, 
			ConflictingMovesData conflictingMovesData) {
		updateTimeoutsWithConflictingMovesData(bicycleTimeouts, carTimeouts, nowish, nowish2, conflictingMovesData.getBicycleData());
	}
	
	public static void updateTimeoutsWithConflictingMovesCarData(double[][] bicycleTimeouts, double[][] carTimeouts, 
			double nowish, double nowish2, 
			ConflictingMovesData conflictingMovesData) {
		updateTimeoutsWithConflictingMovesData(bicycleTimeouts, carTimeouts, nowish, nowish2, conflictingMovesData.getCarData());
	}
	
	private static void updateTimeoutsWithConflictingMovesData(double[][] bicycleTimeouts, double[][] carTimeouts, 
			double nowish, double nowish2, LinkedList<Element> conflictingMovesData) {
		for(Element element : conflictingMovesData) {
			switch(element.getModeIdentifier()) {
			case Car:
				if(element.isSameRank()) {
					carTimeouts[element.getFromDirection()][element.getToDirection()] = nowish;
				} else {
					carTimeouts[element.getFromDirection()][element.getToDirection()] = nowish2;
				}
				break;
			case Bicycle:
				if(element.isSameRank()) {
					bicycleTimeouts[element.getFromDirection()][element.getToDirection()] = nowish;
				} else {
					bicycleTimeouts[element.getFromDirection()][element.getToDirection()] = nowish2;
				}
				break;
			default:
				break;
			}
		}
	}

	public static boolean moveNotHinderedByConflicts(double t, double t_m) {
		return t > t_m;
	}
	
	
	protected void updateEntry(double[][] matrix, int dim1, int dim2, double newTime){
	//	matrix[dim1][dim2] = Double.max(matrix[dim1][dim2], newTime);
		double oldTime = matrix[dim1][dim2];
		if(oldTime < newTime){
			matrix[dim1][dim2] = newTime;
		}
	}
	
	
	protected void updateTimeoutsCarPrimaryGeneralOrRightPriority(double[][] bicycleTimeouts, double[][] carTimeouts, int inDirection,
			int outDirection,  double nowish2) {

		//Suited for right priority if using nowish2. 
		//Suited for general priority moves when using nowish2.
		
		int n = bicycleTimeouts[0].length;

		// Car movements to out direction
			for(int i = 0; i < n; i++){ // All car movements to out direction except from indirection
				if(i != inDirection){
					updateEntry(carTimeouts,i,outDirection,nowish2);
				}
			}

		if(inDirection == outDirection) {
			return;
		}


		//Conflicting car movements
		int r = QFFFNodeUtils.increaseInt(inDirection, n); //new
			while(r != outDirection){
				for(int i = 0; i < n; i++){
					updateEntry(carTimeouts,r,i,nowish2); // all car movements from the right 	
					if(i != inDirection){	
						updateEntry(carTimeouts,i,r,nowish2); // all car movements to the right
					}
				}
				r = QFFFNodeUtils.increaseInt(r,n);
			}

		// Conflicting bicycle movements
		r = inDirection;
			while(r != outDirection){
				int l = inDirection;
				while(l != outDirection){
					updateEntry(bicycleTimeouts,r,l,nowish2);	// All bicycle movements from right to left.
					if(l != inDirection){
						updateEntry(bicycleTimeouts,l,r,nowish2); // All bicycle movements from left to right (except from inDirection).
					}
					l = QFFFNodeUtils.decreaseInt(l,n);
				}
				if(r != inDirection) { // new
					updateEntry(bicycleTimeouts,l,r,nowish2); // The final one (from outDirection)	
				} 
				r = QFFFNodeUtils.increaseInt(r,n);
			}
	}

}
