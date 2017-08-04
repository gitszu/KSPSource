package query;

import spatialindex.spatialindex.Point;
import utility.Global;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.Callable;

import kSP.kSP;
import kSP.kSPP;
import mygraph.GraphAttributes;
import queryindex.VertexQwordsMap;
import rdfindex.memory.RTreeWithGI;
import reachable.ReachableQuery;
import spatialindex.spatialindex.IVisitor;

//rgi, vertexQwordsMap, alphaPostinglists,
//alphaRadius, -1, reachableTester, vertexSCCMap
//k, alphaRadius, qpoint, qwords, postinglists, v,ga

public class MTSP_Task2 implements Runnable{
	
	private RTreeWithGI rgi;
	private VertexQwordsMap<Integer> vertexQwordsMap;
	private Map<Integer, Map<Integer, Float>> alphaPostinglists;
	private double alphaRadius;
	private ReachableQuery reachableTester;
	private Map<Integer, Integer> vertexSCCMap;
	private int k;
	private Point qpoint;
	private Integer[] qwords;
	private Map<Integer, ArrayList> postinglists;
	private IVisitor v;
	private int leastFrequentQword;
	
	public MTSP_Task2(RTreeWithGI rgi,VertexQwordsMap<Integer> vertexQwordsMap,Map<Integer, Map<Integer, Float>> alphaPostinglists,
			double alphaRadius,ReachableQuery reachableTester,Map<Integer, Integer> vertexSCCMap,int k,Point qpoint,Integer[] qwords,
			Map<Integer, ArrayList> postinglists,IVisitor v,int leastFrequentQword){
		
		this.rgi=rgi;
		this.vertexQwordsMap=vertexQwordsMap;
		this.alphaPostinglists=alphaPostinglists;
		this.alphaRadius=alphaRadius;
		this.reachableTester=reachableTester;
		this.vertexSCCMap=vertexSCCMap;
		this.k=k;
		this.qpoint=qpoint;
		this.qwords=qwords;
		this.postinglists=postinglists;
		this.v=v;
		this.leastFrequentQword  = leastFrequentQword;
	}

	@Override
	public void run(){
		long start = System.currentTimeMillis();
		Global.startTime = start;
		
		// TODO Auto-generated method stub
		try {
			GraphAttributes ga = new GraphAttributes(Global.numNodes);
			kSP kSPExecutor = new kSP(rgi, vertexQwordsMap, alphaPostinglists,
					alphaRadius, -1, reachableTester, vertexSCCMap);
			kSPExecutor.kSPComputation(k, alphaRadius, qpoint, qwords, postinglists, v,ga,leastFrequentQword);
			kSPExecutor.clearComputedAlphaLoosenessBounds();
		} catch (Exception e) {
			// TODO: handle exception
		}
		
		long end = System.currentTimeMillis();
		long time  = (end - start);		
		System.out.println(Thread.currentThread().getName()+"::一次查询使用的时间：" + time+" ：查询结果："+v);
		
	}
}
