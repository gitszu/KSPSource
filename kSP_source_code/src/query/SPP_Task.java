package query;

import spatialindex.spatialindex.Point;
import utility.Global;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.Callable;

import kSP.kBSP;
import kSP.kSPP;
import mygraph.GraphAttributes;
import queryindex.VertexQwordsMap;
import rdfindex.memory.RTreeWithGI;
import reachable.ReachableQuery;
import spatialindex.spatialindex.IVisitor;

public class SPP_Task implements Callable<String>{

	private int k;
	private Point qpoint;
	private Integer[] qwords;
	private Map<Integer, ArrayList> postinglists;
	private IVisitor v;
	private ReachableQuery reachableTester;
	private Map<Integer, Integer> vertexSCCMap;
	private int numberNodes;
	

	private RTreeWithGI rgi;
	private VertexQwordsMap<Integer> vertexQwordsMap;
	
	public SPP_Task(int k,Point qpoint,Integer[] qwords,Map<Integer, ArrayList> postinglists,
			IVisitor v,RTreeWithGI rgi,VertexQwordsMap<Integer> vertexQwordsMap,int numberNodes,ReachableQuery reachableTester
			,Map<Integer, Integer> vertexSCCMap){
		this.k = k;
		this.qpoint = qpoint;
		this.qwords = qwords;
		this.postinglists = postinglists;
		this.v = v;
		this.rgi = rgi;
		this.vertexQwordsMap = vertexQwordsMap;
		this.numberNodes = numberNodes;
		this.reachableTester = reachableTester;
		this.vertexSCCMap = vertexSCCMap;
	}
	
	@Override
	public String call() throws Exception {
		long start = System.currentTimeMillis();
		Global.startTime = start;
		
		GraphAttributes ga = new GraphAttributes(this.numberNodes);
		kSPP kSPExecutor = new kSPP(rgi, vertexQwordsMap, reachableTester, vertexSCCMap);
		kSPExecutor.kSPComputation(k, qpoint, qwords, postinglists, v,ga);

		long end = System.currentTimeMillis();
		long time = (end - start);
		return v.toString()+"::" + time;
	}
	
		
	public int getK() {
		return k;
	}
	public void setK(int k) {
		this.k = k;
	}
	public Point getQpoint() {
		return qpoint;
	}
	public void setQpoint(Point qpoint) {
		this.qpoint = qpoint;
	}
	public Integer[] getQwords() {
		return qwords;
	}
	public void setQwords(Integer[] qwords) {
		this.qwords = qwords;
	}
	public Map<Integer, ArrayList> getPostinglists() {
		return postinglists;
	}
	public void setPostinglists(Map<Integer, ArrayList> postinglists) {
		this.postinglists = postinglists;
	}
	public IVisitor getV() {
		return v;
	}
	public void setV(IVisitor v) {
		this.v = v;
	}
	public RTreeWithGI getRgi() {
		return rgi;
	}
	public void setRgi(RTreeWithGI rgi) {
		this.rgi = rgi;
	}
	public VertexQwordsMap<Integer> getVertexQwordsMap() {
		return vertexQwordsMap;
	}
	public void setVertexQwordsMap(VertexQwordsMap<Integer> vertexQwordsMap) {
		this.vertexQwordsMap = vertexQwordsMap;
	}
	public int getNumberNodes() {
		return numberNodes;
	}

	public void setNumberNodes(int numberNodes) {
		this.numberNodes = numberNodes;
	}



}
