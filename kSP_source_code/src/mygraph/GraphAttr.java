package mygraph;

public class GraphAttr {
	
	private int preceder;
	private int distance2Source;	
	private int visitedFlag;
	
	public GraphAttr(int preceder,int distance2Source,int visitedFlag){
		this.preceder = preceder ;
		this.distance2Source = distance2Source ;
		this.visitedFlag = visitedFlag;
	}
	
	public int getPreceder() {
		return preceder;
	}

	public void setPreceder(int preceder) {
		this.preceder = preceder;
	}

	public int getDistance2Source() {
		return distance2Source;
	}

	public void setDistance2Source(int distance2Source) {
		this.distance2Source = distance2Source;
	}

	public int getVisitedFlag() {
		return visitedFlag;
	}

	public void setVisitedFlag(int visitedFlag) {
		this.visitedFlag = visitedFlag;
	}

}
