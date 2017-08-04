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
public class SPMultipleThreadTask2 implements Runnable{
	
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
	//private ThreadPoolExecutor subExecutor;
	
	public SPMultipleThreadTask2(RTreeWithGI rgi,VertexQwordsMap<Integer> vertexQwordsMap,Map<Integer, Map<Integer, Float>> alphaPostinglists,
			double alphaRadius,ReachableQuery reachableTester,Map<Integer, Integer> vertexSCCMap,int k,Point qpoint,Integer[] qwords,
			Map<Integer, ArrayList> postinglists,IVisitor v,int leastFrequentQword ){
		
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
	public void run()  {

		// System.out.println("创建查询子线程共15个 ：  "+Thread.currentThread().getName());	
		 
		long start = System.currentTimeMillis();
		String uuid = UUID.randomUUID().toString();
		Global.startTime = start;
		try {
			kSP_MT kSPExecutor = new kSP_MT(rgi, vertexQwordsMap, alphaPostinglists,
					alphaRadius, -1, reachableTester, vertexSCCMap);
			kSPExecutor.kSPComputation(k, alphaRadius, qpoint, qwords, postinglists, v,leastFrequentQword,uuid);
			kSPExecutor.clearComputedAlphaLoosenessBounds();	
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		long end = System.currentTimeMillis();
		long time = end - start;
		System.out.println(uuid+" "+Thread.currentThread().getName()+"::一次查询使用的时间：" + time+" ：查询结果："+v);
	}
}
