package kth.courses.dt2300.hearmythoughts;

import java.util.LinkedList;

public class RMS {

	private float rms;
	private LinkedList<Float> values = new LinkedList<Float>();
	
	public RMS(int size) {
		for (int i = 0; i < size; i++) {
			values.add(0f);
		}
	}
	
	public void add(float v) {
		values.add(v);
		values.removeFirst();
		calculateRMS();
	}
	
	private void calculateRMS() {
		rms = 0;
		for (float e : values) {
			rms += Math.pow(e, 2);
		}
		rms = (float) Math.sqrt(rms / values.size());
	}
	
	public float getRMS() {
		return rms;
	}
	
}
