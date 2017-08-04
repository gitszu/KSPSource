package query;

import spatialindex.spatialindex.Point;
import utility.Global;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.DoubleAccumulator;

import kSP.kSP;
import kSP.kSP4;
import kSP.kSPP;
import kSP.kSP_MT;
import kSP.candidate.KSPCandidateVisitor;
import mygraph.GraphAttributes;
import queryindex.VertexQwordsMap;
import rdfindex.memory.RTreeWithGI;
import reachable.ReachableQuery;
import spatialindex.spatialindex.IVisitor;
import spatialindex.rtree.Data;

//rgi, vertexQwordsMap, alphaPostinglists,
//alphaRadius, -1, reachableTester, vertexSCCMap
//k, alphaRadius, qpoint, qwords, postinglists, v,ga

public class MTSP_Task4 implements Runnable{
	
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
	private int leastFrequentQword;
	
	public MTSP_Task4(RTreeWithGI rgi,VertexQwordsMap<Integer> vertexQwordsMap,Map<Integer, Map<Integer, Float>> alphaPostinglists,
			double alphaRadius,ReachableQuery reachableTester,Map<Integer, Integer> vertexSCCMap,int k,Point qpoint,Integer[] qwords,
			Map<Integer, ArrayList> postinglists,int leastFrequentQword){
		
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
		this.leastFrequentQword = leastFrequentQword;
	}	
	
	public void run() {
		
		long start = System.currentTimeMillis();
		Global.startTime = start;
		
		// shared variables
		// kthScore.accumulate(score);
		LinkedBlockingQueue<Data> placeDataQueue = new LinkedBlockingQueue<Data>();
		//ConcurrentLinkedQueue<Data> placeDataQueue = new ConcurrentLinkedQueue<>();
		DoubleAccumulator kthScore = new DoubleAccumulator((x,y)-> y,0.0);
		IVisitor result = new KSPCandidateVisitor(k);
		AtomicBoolean terminateFlag = new AtomicBoolean(true);
		
		try {		
			kSP4 kSPExecutor = new kSP4(rgi, vertexQwordsMap, alphaPostinglists,
					alphaRadius, -1, reachableTester, vertexSCCMap);
			kSPExecutor.kSPComputation(k, alphaRadius, qpoint, qwords, postinglists,result,leastFrequentQword,placeDataQueue,kthScore,terminateFlag);
			
			kSPExecutor.getSemanticTree(placeDataQueue, qwords, result, k, kthScore,terminateFlag);
			kSPExecutor.clearComputedAlphaLoosenessBounds();
			
		} catch (Exception e) {
			// TODO: handle exception
		}
		long end = System.currentTimeMillis();
		long time  = (end - start);		
		System.out.println(Thread.currentThread().getName()+"::一次查询使用的时间：" + time+" ：查询结果："+result);
				
	}
}
