/**
 * 
 */
package kSP;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import kSP.candidate.KSPCandidate;
import kSP.candidate.KSPCandidateVisitor;
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
 * Implementation of SPP
 * @author jmshi
 *
 */
public class kSPP {
	protected RTreeWithGI rgi;
	VertexQwordsMap<Integer> vertexQwordsMap = null;
	double kthScore = Double.POSITIVE_INFINITY;

	ReachableQuery reachableTester = null;
	Map<Integer, Integer> vertexSCCMap = null;

	public kSPP(RTreeWithGI thatRGI,
			VertexQwordsMap<Integer> thatVertexQwordsMap, ReachableQuery thatReachableTester,
			Map<Integer, Integer> thatVertexSCCMap) {
		rgi = thatRGI;
		vertexQwordsMap = thatVertexQwordsMap;
		reachableTester = thatReachableTester;
		vertexSCCMap = thatVertexSCCMap;
	}

	public void kSPComputation(int k, final IShape qpoint, Integer[] qwords, Map<Integer, ArrayList> postinglists,
			final IVisitor v,GraphAttributes ga) throws Exception {
		if (qpoint.getDimension() != rgi.getM_dimensoin())
			throw new IllegalArgumentException(
					"kSemanticLocationQuery: Shape has the wrong number of dimensions.");
		NNComparator nnc = rgi.new NNComparator();
		kSPComputation(k, qpoint, qwords, postinglists, v, nnc,ga);
	}

	private void kSPComputation(int k, final IShape qpoint, Integer[] qwords, Map<Integer, ArrayList> postinglists,
			final IVisitor v, final INearestNeighborComparator nnc,GraphAttributes ga)
			throws Exception {
		if (qpoint.getDimension() != rgi.getM_dimensoin())
			throw new IllegalArgumentException("kSemanticLocationQuery: Shape has the wrong number of dimensions.");

		//rgi.readLock();

		try {
			/* I need a priority queue here. It turns out that TreeSet sorts unique keys only and since I am
		 	   sorting according to distances, it is not assured that all distances will be unique. TreeMap
			   also sorts unique keys. Thus, I am simulating a priority queue using an ArrayList and binarySearch. */
			ArrayList queue = new ArrayList();

			Node n = null;
			Data nd = new Data(0.0, null, rgi.getRoot(), -1);
			queue.add(new NNEntry(nd, 0.0, rgi.getTreeHeight()));

			while (queue.size() != 0) {
				NNEntry first = (NNEntry) queue.remove(0);
				if (kthScore != Double.POSITIVE_INFINITY
						&& kthScore < first.m_minDist) {
					break;
				}
				if (first.level >= 0) {// node
					Data firstData = (Data) first.m_pEntry;

					n = rgi.readNode(firstData.getIdentifier());
					Global.count[0]++;

					for (int cChild = 0; cChild < n.m_children; cChild++) {
						IEntry e;
						double minSpatialDist;

						if (n.m_level == 0) {
							minSpatialDist = qpoint.getMinimumDistance(n.m_pMBR[cChild]);
							if (kthScore != Double.POSITIVE_INFINITY && kthScore < minSpatialDist) {
								continue;
							}
							e = new Data(minSpatialDist, n.m_pMBR[cChild], n.m_pIdentifier[cChild], n.getIdentifier());
						} else {
							minSpatialDist = qpoint.getMinimumDistance(n.m_pMBR[cChild]);
							if (kthScore != Double.POSITIVE_INFINITY && kthScore < minSpatialDist) {
								continue;
							}
							e = new Data(minSpatialDist, n.m_pMBR[cChild], n.m_pIdentifier[cChild], n.m_identifier);
						}

						NNEntry e2 = new NNEntry(e, minSpatialDist, n.m_level - 1);

						@SuppressWarnings("unchecked")
						int loc = Collections.binarySearch(queue, e2, new NNEntryComparator());
						if (loc >= 0)
							queue.add(loc, e2);
						else
							queue.add((-loc - 1), e2);
					}
				} else {
					Data placeData = (Data) first.m_pEntry;
					// unqualified place pruning
					if (this.placeReachablePrune(placeData.getIdentifier(), qwords)) {
						Global.count[5]++;
						continue;
					}
					
					double loosenessThreshold = Double.POSITIVE_INFINITY;
					if (kthScore != Double.POSITIVE_INFINITY) {
						loosenessThreshold = (kthScore / placeData.getWeight());
					}
					// compute shortest path between place and qword
					Global.count[3]++;
					List<List<Integer>> semanticTree = new ArrayList<List<Integer>>();
					long start = System.currentTimeMillis();
					double looseness = this.rgi.getGraph().getSemanticPlaceP2(placeData.getIdentifier(),
							qwords, loosenessThreshold, vertexQwordsMap, semanticTree);
					long end = System.currentTimeMillis();
					Global.runtime[1] += end - start;

					Global.count[1]++;// number of all places having computed their semantic place.
					if (looseness < 1) {
						throw new Exception("semantic score " + looseness + " < 1, for place" + placeData.getIdentifier());
					}
					// place is a valid candidate that connects to all qwords
					if (looseness != Double.POSITIVE_INFINITY) {
						Global.count[2]++;// number of valid place candidate
						double rankingScore = placeData.getWeight() * looseness;
						KSPCandidate candidate = new KSPCandidate(new NNEntry(placeData, rankingScore),
								semanticTree);

						((KSPCandidateVisitor) v).addPlaceCandidate(candidate);
						kthScore = ((KSPCandidateVisitor) v).size() >= k ? ((KSPCandidateVisitor) v)
								.getWorstRankingScore() : Double.POSITIVE_INFINITY;
					}
				}
				long curTime = System.currentTimeMillis();
				if (curTime - Global.startTime > Global.runtimeThreshold) {
					break;
				}
			}
		} finally {
			//rgi.readUnlock();
		}
	}

	/**
	 * Unqualified place pruning
	 * @param place
	 * @param qwords
	 * @return
	 */
	private boolean placeReachablePrune(int place, Integer[] qwords) {
		boolean isPruned = false;

		/*
		 * For unqualified place pruning, based on the observation that infrequent query keywords have a high chance to make a place unqualified, 
		 * we prioritize them when issuing reachability queries.
		 * Furthermore, the least Frequent query keyword is powerful enough for pruning.
		 * */
		int placeSCC = vertexSCCMap.get(place);
		Global.count[4]++;
		if (!reachableTester.queryReachable(placeSCC, Global.leastFrequentQword, Global.numSCCs)) {
			isPruned = true;
		}
		return isPruned;
	}
}
