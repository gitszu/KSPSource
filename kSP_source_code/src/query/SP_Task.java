package query;

import spatialindex.spatialindex.Point;
import utility.Global;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import invertedindex.InvertedIndexHash;
import kSP.kBSP;
import kSP.kSP;
import kSP.kSPP;
import mygraph.GraphAttributes;
import queryindex.VertexQwordsMap;
import rdfindex.memory.RTreeWithGI;
import reachable.ReachableQuery;
import spatialindex.spatialindex.IVisitor;

public class SP_Task implements Callable<String>{

	private int k;
	private Point qpoint;
	private Integer[] qwords;
	
	private IVisitor v;
	private ReachableQuery reachableTester;
	private Map<Integer, Integer> vertexSCCMap;
	private int numberNodes;
	private double alphaRadius;
	private RTreeWithGI rgi;
	
	private VertexQwordsMap<Integer> vertexQwordsMap;
	private Map<Integer, Map<Integer, Float>> alphaPostinglists;
	private Map<Integer, ArrayList> postinglists;
	
	private InvertedIndexHash alphaIindex;
	 
	
	public SP_Task(int k,Point qpoint,Integer[] qwords,
			IVisitor v,RTreeWithGI rgi,int numberNodes,ReachableQuery reachableTester
			,Map<Integer, Integer> vertexSCCMap,InvertedIndexHash alphaIindex,double alphaRadius) throws Exception{
		this.k = k;
		this.qpoint = qpoint;
		this.qwords = qwords;
		this.v = v;
		this.rgi = rgi;
		this.numberNodes = numberNodes;
		this.reachableTester = reachableTester;
		this.vertexSCCMap = vertexSCCMap;
		this.setAlphaIindex(alphaIindex);
		this.alphaRadius = alphaRadius;
		
		//the alpha-WN posting lists of query keywords for the alpha-WN precomputed index
		this.alphaPostinglists = new HashMap<Integer, Map<Integer, Float>>();
		//the posting lists of query keywords for the RDF data
		this.postinglists = new HashMap<Integer, ArrayList>();

		

		
		int leastFrequency = Integer.MAX_VALUE;
		for (int i = 0; i < qwords.length; i++) {
			ArrayList postinglist = rgi.readPostingList(qwords[i]);
			postinglists.put(qwords[i], postinglist);

			if (postinglist.size() < leastFrequency) {
				Global.leastFrequentQword = qwords[i];
			}
		}
		
		for (int i = 0; i < qwords.length; i++) {
			Map<Integer, Float> alphaPostinglist = alphaIindex.readPostingListMap(qwords[i],true);
			alphaPostinglists.put(qwords[i], alphaPostinglist);
		}
		
		this.vertexQwordsMap = new VertexQwordsMap<Integer>(qwords, this.postinglists, false);
		
	}
	
	@Override
	public String call() throws Exception {
		long start = System.currentTimeMillis();
		Global.startTime = start;
		
		GraphAttributes ga = new GraphAttributes(this.numberNodes);

		
		kSP kSPExecutor = new kSP(rgi, vertexQwordsMap, alphaPostinglists,
				alphaRadius, -1, reachableTester, vertexSCCMap);
		//kSPExecutor.kSPComputation(k, alphaRadius, qpoint, qwords, postinglists, v,ga);

		kSPExecutor.clearComputedAlphaLoosenessBounds();
		long end = System.currentTimeMillis();
		long time = (end - start);

		System.out.println(Thread.currentThread().getName()+"::一次查询使用的时间：" + time+" ：查询结果："+v);
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

	/**
	 * @return the alphaIindex
	 */
	public InvertedIndexHash getAlphaIindex() {
		return alphaIindex;
	}

	/**
	 * @param alphaIindex the alphaIindex to set
	 */
	public void setAlphaIindex(InvertedIndexHash alphaIindex) {
		this.alphaIindex = alphaIindex;
	}



}
