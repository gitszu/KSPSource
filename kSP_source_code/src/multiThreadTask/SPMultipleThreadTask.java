package multiThreadTask;

import spatialindex.spatialindex.Point;
import utility.Global;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadPoolExecutor;

import kSP.kSP;
import kSP.kSPP;
import kSP.kSP_MT;
import mygraph.GraphAttributes;
import queryindex.VertexQwordsMap;
import rdfindex.memory.RTreeWithGI;
import reachable.ReachableQuery;
import spatialindex.spatialindex.IVisitor;

/**
 * SP算法，多线程任务
 * @author yuanshuai
 *
 */
public class SPMultipleThreadTask implements Callable<String>{
	
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
	
	public SPMultipleThreadTask(RTreeWithGI rgi,VertexQwordsMap<Integer> vertexQwordsMap,Map<Integer, Map<Integer, Float>> alphaPostinglists,
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
		this.leastFrequentQword = leastFrequentQword;
	}
	
	@Override
	public String call() throws Exception {

		long start = System.currentTimeMillis();
		Global.startTime = start;
		String uuid = UUID.randomUUID().toString();
		kSP_MT kSPExecutor = new kSP_MT(rgi, vertexQwordsMap, alphaPostinglists,
				alphaRadius, -1, reachableTester, vertexSCCMap);
		kSPExecutor.kSPComputation(k, alphaRadius, qpoint, qwords, postinglists, v,leastFrequentQword,uuid);
		kSPExecutor.clearComputedAlphaLoosenessBounds();
		
		System.out.println("子线程完毕");	
		//System.out.println(Thread.currentThread().getName()+"::一次查询使用的时间：" + time+" ：查询结果："+v);
		
		return v.toString();
	}
}
