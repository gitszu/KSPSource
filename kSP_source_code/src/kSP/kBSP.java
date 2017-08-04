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
 * Implementation of BSP
 * 
 * @author jmshi
 *
 */
public class kBSP {
	protected RTreeWithGI rgi;
	VertexQwordsMap<Integer> vertexQwordsMap = null;
	double kthScore = Double.POSITIVE_INFINITY;


	public kBSP(RTreeWithGI thatRGI, VertexQwordsMap<Integer> thatVertexQwordsMap) {
		rgi = thatRGI;
		vertexQwordsMap = thatVertexQwordsMap;
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
//		rgi.readLock();

		try {
			/* I need a priority queue here. It turns out that TreeSet sorts unique keys only and since I am
		 	   sorting according to distances, it is not assured that all distances will be unique. TreeMap
			   also sorts unique keys. Thus, I am simulating a priority queue using an ArrayList and binarySearch. */
			ArrayList queue = new ArrayList();

			Node n = null;
			Data nd = new Data(0.0, null, rgi.getRoot(), -1);
			queue.add(new NNEntry(nd, 0.0, rgi.getTreeHeight()));

			int printcount = 0;
			
			while (queue.size() != 0) {
				printcount++;
				NNEntry first = (NNEntry) queue.remove(0);
				if (kthScore != Double.POSITIVE_INFINITY
						&& kthScore < first.m_minDist) {
					break;
				}
				if (first.level >= 0) {//R-tree node
					Data firstData = (Data) first.m_pEntry;
					
					n = rgi.readNode(firstData.getIdentifier());
					Global.count[0]++;
					
					for (int cChild = 0; cChild < n.m_children; cChild++) {
						IEntry e;
						double minSpatialDist;

						if (n.m_level == 0) {
							minSpatialDist = qpoint.getMinimumDistance(n.m_pMBR[cChild]);
							if (kthScore != Double.POSITIVE_INFINITY
									&& kthScore < minSpatialDist) {
								continue;
							}
							
							e = new Data(minSpatialDist, n.m_pMBR[cChild], n.m_pIdentifier[cChild],
									n.getIdentifier());
						} else {
							minSpatialDist = qpoint.getMinimumDistance(n.m_pMBR[cChild]);
							if (kthScore != Double.POSITIVE_INFINITY
									&& kthScore < minSpatialDist) {
								continue;
							}
							e = new Data(minSpatialDist, n.m_pMBR[cChild], n.m_pIdentifier[cChild],
									n.m_identifier);
						}

						NNEntry e2 = new NNEntry(e, minSpatialDist, n.m_level - 1);

						// Why don't I use a TreeSet here? See comment above...
						@SuppressWarnings("unchecked")
						int loc = Collections.binarySearch(queue, e2, new NNEntryComparator());
						if (loc >= 0)
							queue.add(loc, e2);
						else
							queue.add((-loc - 1), e2);
					}
				} else {
					Data placeData = (Data) first.m_pEntry;
					//System.out.println(placeData.getShape().getMBR().getLow(0) + "," +placeData.getShape().getMBR().getLow(1));
					Global.count[3]++;
					List<List<Integer>> semanticTree = new ArrayList<List<Integer>>();
					long start = System.currentTimeMillis();
					double looseness = this.rgi.getGraph().getSemanticPlaceB(placeData.getIdentifier(),
							qwords, vertexQwordsMap, semanticTree,ga);
					
					long end = System.currentTimeMillis();
					Global.runtime[1] += end - start;

					Global.count[1]++; // number of all places having computed their semantic place.
					if (looseness < 1) {
						throw new Exception("semantic score " + looseness + " < 1, for place"
								+ placeData.getIdentifier());
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
}
