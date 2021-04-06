package org.matsim.core.mobsim.qsim.qnetsimengine;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class Counters {
	
	public static AtomicLong countStuckEvents = new AtomicLong(0);
	
	public static AtomicInteger countSingleLanes = new AtomicInteger(0);
	public static AtomicInteger countInteractingLanes = new AtomicInteger(0);
	public static AtomicInteger countSeparatedLanes = new AtomicInteger(0);
	
	public static AtomicInteger countMergingNodes = new AtomicInteger(0);
	public static AtomicInteger countDivergingNodes = new AtomicInteger(0);
	public static AtomicInteger countDirectedPriorityNodes = new AtomicInteger(0);
	public static AtomicInteger countRightPriorityNodes = new AtomicInteger(0);
	public static AtomicInteger countAntiPriorityNodes = new AtomicInteger(0);
	
	

}
