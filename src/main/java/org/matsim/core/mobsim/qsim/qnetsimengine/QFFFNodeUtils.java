package org.matsim.core.mobsim.qsim.qnetsimengine;

import org.matsim.api.core.v01.network.Link;

public class QFFFNodeUtils {

	public static double calculatePositiveThetaDif(double mainTheta, double referenceTheta){
		//returns a value in the interval [0; 2PI[
		double thetaDif = mainTheta - referenceTheta;
		if( thetaDif < 0){
			thetaDif += 2*Math.PI;
		} 
		return thetaDif;
	}

	
	public static double calculateTheta(Link link){
		double x = link.getToNode().getCoord().getX() - link.getFromNode().getCoord().getX();
		double y = link.getToNode().getCoord().getY() - link.getFromNode().getCoord().getY();
		return Math.atan2(y,x);
	}

	public static double calculateInverseTheta(Link link){
		double x = link.getFromNode().getCoord().getX() - link.getToNode().getCoord().getX();
		double y = link.getFromNode().getCoord().getY() - link.getToNode().getCoord().getY();
		return Math.atan2(y,x);
	}

	
	public static int increaseInt(int i, int n){
		if(i == n -1){
			return 0;
		} else {
			return i + 1;
		}
	}
	
	 public static int decreaseInt(int i, int n){
		if(i == 0){
			return n - 1;
		} else {
			return i - 1;
		}
	}


}
