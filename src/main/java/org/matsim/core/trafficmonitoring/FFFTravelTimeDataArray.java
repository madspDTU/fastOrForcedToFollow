package org.matsim.core.trafficmonitoring;

import org.matsim.api.core.v01.network.Link;

public class FFFTravelTimeDataArray extends TravelTimeData{


	public final short[] timeCnt;
	public final double[] travelTimes;
	public final Link link;

	FFFTravelTimeDataArray(final Link link, final int numSlots) {
		this.timeCnt = new short[numSlots];
		this.travelTimes = new double[numSlots];
		this.link = link;
		resetTravelTimes();
	}

	@Override
	public void resetTravelTimes() {
		for (int i = 0; i < this.travelTimes.length; i++) {
			this.timeCnt[i] = 0;
			this.travelTimes[i] = -1.0;
		}
	}

	@Override
	public void setTravelTime( final int timeSlot, final double traveltime ) {
		this.timeCnt[timeSlot] = 1;
		this.travelTimes[timeSlot] = traveltime;
	}

	@Override
	public void addTravelTime(final int timeSlot, final double traveltime) {
		short cnt = this.timeCnt[timeSlot];
		double sum = this.travelTimes[timeSlot] * cnt;

		sum += traveltime;
		cnt++;

		this.travelTimes[timeSlot] = sum / cnt;
		this.timeCnt[timeSlot] = cnt;
	}

	public double getRawTravelTime(final int timeSlot, final double now) {
		return this.travelTimes[timeSlot];		
	}
	
	@Override
	public double getTravelTime(final int timeSlot, final double now) {
		System.err.println("This dould not happen. Due to the implementation, getTraveltime of FFFTravelTimeArray must not be called. " +
				"Use getUnspoiledTravelTime instead.");
		System.exit(-1);
		
		double ttime = this.travelTimes[timeSlot];
		if (ttime >= 0.0) return ttime; // negative values are invalid.

		// ttime can only be <0 if it never accumulated anything, i.e. if cnt == 9, so just use freespeed
		double freespeed = this.link.getLength() / this.link.getFreespeed(now);
		this.travelTimes[timeSlot] = freespeed;
		return freespeed;
	}

}
