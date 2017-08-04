/**
 * 
 */
package kSP;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.DoubleAccumulator;

import kSP.candidate.KSPCandidate;
import kSP.candidate.KSPCandidateVisitor;
import multiThreadTask.KthScore;
import multiThreadTask.SemanticPlaceMT;
import multiThreadTask.SemanticPlaceMT2;
import multiThreadTask.SimpleThreadpool;
import mygraph.GraphAttributes;
import queryindex.VertexQwordsMap;
import rdfindex.memory.RTreeWithGI;
import reachable.ReachableQuery;
import spatialindex.rtree.Data;
import spatialindex.rtree.NNEntry;
import spatialindex.rtree.NNEntryComparator;
import spatialindex.rtree.Node;
import spatialindex.rtree.RTree.NNComparator;
import spatialindex.spatialindex.IEntry;
import spatialindex.spatialindex.INearestNeighborComparator;
import spatialindex.spatialindex.IShape;
import spatialindex.spatialindex.IVisitor;
import utility.Global;

/**
 * Implementation of SP
 * @author jmshi
 *
 */
public class kSP_MT {
	protected RTreeWithGI rgi;
	Map<Integer, Map<Integer, Float>> alphaPostinglists = null;
	//protected  double kthScore = Double.POSITIVE_INFINITY;
	protected DoubleAccumulator kthScore = new DoubleAccumulator((x,y)-> y,0.0);
	//public static volatile KthScore kthScore = new KthScore(Double.POSITIVE_INFINITY); 
	
	Map<Integer, Double> alphaLoosenessBounds = new HashMap<Integer, Double>();

	VertexQwordsMap<Integer> vertexQwordsMap = null;

	ReachableQuery reachableTester = null;
	Map<Integer, Integer> vertexSCCMap = null;

	public kSP_MT(RTreeWithGI thatRGI, VertexQwordsMap<Integer> thatVertexQwordsMap,
			Map<Integer, Map<Integer, Float>> thatAlphaPostinglists, double alphaRadius, int alphaPagesize,
			ReachableQuery thatReachableTester, Map<Integer, Integer> thatVertexSCCMap) throws Exception {
		rgi = thatRGI;
		vertexQwordsMap = thatVertexQwordsMap;
		alphaPostinglists = thatAlphaPostinglists;
		reachableTester = thatReachableTester;
		vertexSCCMap = thatVertexSCCMap;
	}

	public void kSPComputation(int k, double alphaRadius, final IShape qpoint, Integer[] qwords,
			Map<Integer, ArrayList> postinglists, final IVisitor result,int leastFrequentQword,String uuid ) throws Exception {
		if (qpoint.getDimension() != rgi.getM_dimensoin())
			throw new IllegalArgumentException(
					"kSemanticLocationQuery: Shape has the wrong number of dimensions.");
		NNComparator nnc = rgi.new NNComparator();
		long start = System.currentTimeMillis();
		kSPComputation(k, alphaRadius, qpoint, qwords, postinglists, result, nnc,leastFrequentQword,uuid);
		long end = System.currentTimeMillis();
		//System.out.println(uuid+":      1--->  kSPComputation time:"+(end-start));
	}

	public void clearComputedAlphaLoosenessBounds() {
		alphaLoosenessBounds.clear();
	}

	private void kSPComputation(int k, double alphaRadius, final IShape qpoint, Integer[] qwords,
			Map<Integer, ArrayList> postinglists, final IVisitor result, final INearestNeighborComparator nnc,
			int leastFrequentQword,String uuid ) throws Exception {
		if (qpoint.getDimension() != rgi.getM_dimensoin())
			throw new IllegalArgumentException(
					"kSemanticLocationQuery: Shape has the wrong number of dimensions.");

		kthScore.accumulate(Double.POSITIVE_INFINITY);
		//rgi.readLock();
		long startall = System.currentTimeMillis();
		//KthScore kthScore = new KthScore(Double.POSITIVE_INFINITY);
		try {
			/* I need a priority queue here. It turns out that TreeSet sorts unique keys only and since I am
		 	   sorting according to distances, it is not assured that all distances will be unique. TreeMap
			   also sorts unique keys. Thus, I am simulating a priority queue using an ArrayList and binarySearch. */
			ArrayList queue = new ArrayList();

			Node n = null;
			Data nd = new Data(0.0, null, rgi.getRoot(), -1);

			if (rgi.getTreeHeight() < 0) {
				throw new Exception("rtree height " + rgi.getTreeHeight() + " invalid");
			}
			queue.add(new NNEntry(nd, 0.0, rgi.getTreeHeight()));

			/**
			 * create thread pool
			 */
		    ThreadPoolExecutor subExecutor = new ThreadPoolExecutor(3, 3, 200, TimeUnit.MILLISECONDS,new ArrayBlockingQueue<Runnable>(3));
			//ThreadPoolExecutor subExecutor = new ThreadPoolExecutor(2, 100, 200, TimeUnit.MILLISECONDS,new LinkedBlockingQueue<Runnable>());
			//SimpleThreadpool subExecutor = SimpleThreadpool.getInstance(2);
			long start_w = System.currentTimeMillis();

		    
			while (queue.size() != 0) {
				
				NNEntry first = (NNEntry) queue.remove(0);
				//kthScore = ((KSPCandidateVisitor) result).size() >= k ? ((KSPCandidateVisitor) result).getWorstRankingScore() : Double.POSITIVE_INFINITY; 
			
				//double kthscore = kthScore.get();
				if (kthScore.get() < first.m_minDist) {
					//System.out.println("符合条件结束 ："+"kthScore:"+kthScore+"    m_minDist:"+first.m_minDist);
					break;
				}
				
				if (first.level >= 0) {// node
					long startw2 = System.currentTimeMillis();
					Data firstData = (Data) first.m_pEntry;

					n = rgi.readNode(firstData.getIdentifier());
					Global.count[0]++;
					
					for (int cChild = 0; cChild < n.m_children; cChild++) {
						double minSpatialDist = qpoint.getMinimumDistance(n.m_pMBR[cChild]);
						double alphaLoosenessBound = 0;
						if (n.m_level == 0) {
							//children of n are places
							alphaLoosenessBound = this.getAlphaLoosenessBound(n.getChildIdentifier(cChild), alphaRadius,
									qpoint, qwords);
						}
						else{
							//ATTENTION: children of n are nodes that have -id-1 as identifier in alpha index
							alphaLoosenessBound = this.getAlphaLoosenessBound((-n.getChildIdentifier(cChild) - 1),
									alphaRadius, qpoint, qwords);
						}
						double alphaRankingScoreBound = minSpatialDist * alphaLoosenessBound;
						if (alphaRankingScoreBound > kthScore.get()) {
							continue;
						}
						IEntry eChild = new Data(minSpatialDist, n.m_pMBR[cChild],
								n.m_pIdentifier[cChild], n.m_identifier);
						NNEntry eChild2 = new NNEntry(eChild, alphaRankingScoreBound, n.m_level - 1);

						int loc = insertIntoHeapH(queue, eChild2);
					}

				} else {										
					Data placeData = (Data) first.m_pEntry;
					if (kthScore.get() < first.m_minDist) {					
						continue;
					}
								
					// unqualified place pruning
					if (this.placeReachablePrune(placeData.getIdentifier(), qwords,leastFrequentQword)) {
						Global.count[5]++;// pruned
						continue;
					}

					double loosenessThreshold = Double.POSITIVE_INFINITY;
					if (kthScore.get() != Double.POSITIVE_INFINITY) {						
						loosenessThreshold = (kthScore.get() / placeData.getWeight());
					}
					
					// compute shortest path between place and qword
					Global.count[3]++;
					List<List<Integer>> semanticTree = new ArrayList<List<Integer>>();
					
					SemanticPlaceMT2 task = new SemanticPlaceMT2( placeData,qwords, loosenessThreshold, vertexQwordsMap
							,semanticTree, rgi, result ,k,kthScore,first.m_minDist,uuid);
					subExecutor.execute(task);	
					
					long startw3= System.currentTimeMillis();
					if(subExecutor.getQueue().size() == 3){
						while(true){
							if(subExecutor.getQueue().size() != 3){
								break;
							}
						}
					}			
				}
				
				long curTime = System.currentTimeMillis();
				if (curTime - Global.startTime > Global.runtimeThreshold) {
					break;
				}
			}
//			subExecutor.stop();
//			subExecutor.awaitTermination();
			subExecutor.shutdown();			

			while(true){
				if(subExecutor.isTerminated() == true){
					break;
				}
			}
	
		} finally {
			//rgi.readUnlock();
		}
	}

	/**
	 * @param queue
	 * @param e2
	 * @return
	 */
	public int insertIntoHeapH(ArrayList queue, NNEntry e2) {
		int loc = Collections.binarySearch(queue, e2, new NNEntryComparator());
		if (loc >= 0)
			queue.add(loc, e2);
		else
			queue.add((-loc - 1), e2);
		return loc;
	}

	/**
	 * Unqualified place pruning
	 * @param place
	 * @param qwords
	 * @return
	 */
	private boolean placeReachablePrune(int place, Integer[] qwords,int leastFrequentQword) {
		boolean isPruned = false;
		/*
		 * For unqualified place pruning, based on the observation that infrequent query keywords have a high chance to make a place unqualified, 
		 * we prioritize them when issuing reachability queries.
		 * Furthermore, the least Frequent query keyword is powerful enough for pruning.
		 * */
		int placeSCC = vertexSCCMap.get(place);
		Global.count[4]++;
		
		if (!reachableTester.queryReachable(placeSCC,leastFrequentQword, Global.numSCCs)) {
			isPruned = true;
		}
		return isPruned;
	}

	/**
	 * 
	 * @param id
	 * @param alphaRadius
	 * @param qpoint
	 * @param qwords
	 * @return
	 * @throws IOException
	 */
	public double getAlphaLoosenessBound(int id, double alphaRadius, final IShape qpoint, Integer[] qwords) throws IOException {

		if (this.alphaLoosenessBounds.containsKey(id)) {
			return this.alphaLoosenessBounds.get(id);
		}

		double alphaLoosenessBound = 1;
		for (Entry<Integer, Map<Integer, Float>> entry : this.alphaPostinglists.entrySet()) {
			Map<Integer, Float> alphaPostinglist = entry.getValue();
			if (alphaPostinglist != null && alphaPostinglist.containsKey(id)) {
				alphaLoosenessBound += alphaPostinglist.get(id);
			} else {
				alphaLoosenessBound += alphaRadius + 1;
			}
		}
		this.alphaLoosenessBounds.put(id, alphaLoosenessBound);

		return alphaLoosenessBound;
	}
}
