package mygraph;

public class GraphAttributes {

	protected int[] preceder;
	protected int[] distance2Source;
	// Given a source vertex, if a vertex is visited, the vertex's visitedFlag is set to the source vertex id
	protected int[] visitedFlag;
	
	public GraphAttributes(int numNodes){
		this.preceder = new int[numNodes];
		this.distance2Source = new int[numNodes];
		this.visitedFlag = new int[numNodes];
		for (int i = 0; i < this.visitedFlag.length; i++) {
			this.visitedFlag[i] = -1;
		}
	}
	
	
	public int[] getPreceder() {
		return preceder;
	}
	public void setPreceder(int[] preceder) {
		this.preceder = preceder;
	}
	public int[] getDistance2Source() {
		return distance2Source;
	}
	public void setDistance2Source(int[] distance2Source) {
		this.distance2Source = distance2Source;
	}
	public int[] getVisitedFlag() {
		return visitedFlag;
	}
	public void setVisitedFlag(int[] visitedFlag) {
		this.visitedFlag = visitedFlag;
	}
	
	
}
