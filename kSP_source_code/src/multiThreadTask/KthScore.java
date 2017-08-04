package multiThreadTask;

public class KthScore {

	public KthScore(double Score){
		this.Score = Score;
	}
	
	private double Score;
	private double loosenessThreshold;	

	public double getScore() {
		return Score;
	}
	
	public void setScore(double score) {
		Score = score;
	}
	
	public double getLoosenessThreshold() {
		return loosenessThreshold;
	}

	public void setLoosenessThreshold(double loosenessThreshold) {
		this.loosenessThreshold = loosenessThreshold;
	}
}
