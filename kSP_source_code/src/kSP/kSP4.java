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
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.DoubleAccumulator;

import kSP.candidate.KSPCandidate;
import kSP.candidate.KSPCandidateVisitor;
import multiThreadTask.SemanticPlaceMT4;
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
public class kSP4 {
	protected RTreeWithGI rgi;
	Map<Integer, Map<Integer, Float>> alphaPostinglists = null;
	//double kthScore = Double.POSITIVE_INFINITY;
	Map<Integer, Double> alphaLoosenessBounds = new HashMap<Integer, Double>();

	VertexQwordsMap<Integer> vertexQwordsMap = null;

	ReachableQuery reachableTester = null;
	Map<Integer, Integer> vertexSCCMap = null;

	public kSP4(){}
	
	public kSP4(RTreeWithGI thatRGI, VertexQwordsMap<Integer> thatVertexQwordsMap,
			Map<Integer, Map<Integer, Float>> thatAlphaPostinglists, double alphaRadius, int alphaPagesize,
			ReachableQuery thatReachableTester, Map<Integer, Integer> thatVertexSCCMap) throws Exception {
		rgi = thatRGI;
		vertexQwordsMap = thatVertexQwordsMap;
		alphaPostinglists = thatAlphaPostinglists;
		reachableTester = thatReachableTester;
		vertexSCCMap = thatVertexSCCMap;
	}

//	public void kSPComputation(int k, double alphaRadius, final IShape qpoint, Integer[] qwords,
//			Map<Integer, ArrayList> postinglists, final IVisitor result,int leastFrequentQword) throws Exception {
//		if (qpoint.getDimension() != rgi.getM_dimensoin())
//			throw new IllegalArgumentException(
//					"kSemanticLocationQuery: Shape has the wrong number of dimensions.");
//		NNComparator nnc = rgi.new NNComparator();
//		kSPComputation(k, alphaRadius, qpoint, qwords, postinglists, result, nnc,leastFrequentQword);
//
//	}

	public void clearComputedAlphaLoosenessBounds() {
		alphaLoosenessBounds.clear();
	}

	public void kSPComputation(int k, double alphaRadius, final IShape qpoint, Integer[] qwords,
			Map<Integer, ArrayList> postinglists, final IVisitor result,int leastFrequentQword,
			LinkedBlockingQueue<Data> placeDataQueue,DoubleAccumulator kthScore,AtomicBoolean terminateFlag) throws Exception {
		if (qpoint.getDimension() != rgi.getM_dimensoin())
			throw new IllegalArgumentException(
					"kSemanticLocationQuery: Shape has the wrong number of dimensions.");

		NNComparator nnc = rgi.new NNComparator();
		
		//rgi.readLock();
		long threshold_start = System.currentTimeMillis();
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
			
			while (queue.size() != 0) {
				NNEntry first = (NNEntry) queue.remove(0);
				
				if (kthScore.get() < first.m_minDist) {			
					terminateFlag.set(false);
					break;
				}
				if (first.level >= 0) {// node
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
					placeDataQueue.add(placeData);
				}
				
			}

		} finally {
			//rgi.readUnlock();
		}
	}
	
	
	public void getSemanticTree(LinkedBlockingQueue<Data> placeDataQueue,Integer[] qwords,
			IVisitor result,int k,DoubleAccumulator kthScore,AtomicBoolean terminateFlag){
		
		SimpleThreadpool subExecutor = SimpleThreadpool.getInstance(2);
		try {
			Data placeData= null;

			while(terminateFlag.get()){
				System.out.println("terminateFlag.get(): "+terminateFlag.get());
				while((placeData = placeDataQueue.poll()) != null){
					
					// compute shortest path between place and qword
					Global.count[3]++;
					List<List<Integer>> semanticTree = new ArrayList<List<Integer>>();
										
					SemanticPlaceMT4 task = new SemanticPlaceMT4(placeData,
							qwords, vertexQwordsMap, semanticTree,rgi,result,k,kthScore,terminateFlag);
					subExecutor.execute(task);				
				}
			}

			subExecutor.stop();
			subExecutor.awaitTermination();

		} catch (Exception e) {
			// TODO: handle exception
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
			//System.out.println("place: "+place+ " placeSCC: "+placeSCC +" leastFrequentQword: "+ leastFrequentQword+" Global.numSCCs: "+Global.numSCCs);
			if (!reachableTester.queryReachable(placeSCC, leastFrequentQword, Global.numSCCs)) {
				isPruned = true;
			}
			//System.out.println(Thread.currentThread().getName()+": place :"+place +": isPruned :"+isPruned);
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
