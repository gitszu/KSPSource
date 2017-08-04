/**
 * 
 */
package mygraph;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.DoubleAccumulator;

import invertedindexmemory.InvertedIndex;
import kSP.candidate.KSPCandidate;
import kSP.candidate.KSPCandidateVisitor;
import multiThreadTask.KthScore;
import queryindex.VertexQwordsMap;
import utility.Global;
import utility.Utility;

import spatialindex.rtree.Data;
import spatialindex.rtree.NNEntry;
import spatialindex.spatialindex.IVisitor;
/**
 * Unweighted directed graph with vertex id starting from 0 to the end
 * The graph is implemented by arrays.
 * 
 * @author jmshi 
 */
public class GraphByArray {
	protected int[][] adjLists;
	protected int numVertices;
	//protected int[] preceder;
	//protected int[] distance2Source;
	// Given a source vertex, if a vertex is visited, the vertex's visitedFlag is set to the source vertex id
	//protected int[] visitedFlag;
	
	public GraphByArray(int numNodes) {
		this.numVertices = numNodes;
		this.adjLists = new int[numNodes][];
//		this.preceder = new int[numNodes];
//		this.distance2Source = new int[numNodes];
//		this.visitedFlag = new int[numNodes];
//		for (int i = 0; i < this.visitedFlag.length; i++) {
//			this.visitedFlag[i] = -1;
//		}
	}

	/**
	 * compute the alpha doc of vertex in BFS mode.
	 * */
	public Map<Integer, Double> alphaRadiusOfVertex(int vid, double radius, InvertedIndex idKeywordsMap)
			throws IOException {
		Map<Integer, Double> vid2KwordsSPD = new HashMap<Integer, Double>();
//		Queue<Integer> queue = new LinkedList<Integer>();
//		int source = vid;
//		preceder[vid] = -1;
//		distance2Source[vid] = 0;
//		visitedFlag[vid] = source;
//		queue.add(vid);
//
//		while (!queue.isEmpty()) {
//			int vertex = queue.poll();
//
//			if ((double) distance2Source[vertex] > radius) {
//				break;
//			}
//
//			List<Integer> containedKwords = idKeywordsMap.readPostinglist(vertex);
//
//			if (containedKwords == null) {
//				continue;
//			}
//
//			for (int i = 0; i < containedKwords.size(); i++) {
//				int kword = containedKwords.get(i);
//				// int kword = ((IntKey) keydatai.key).key;
//				if (!vid2KwordsSPD.containsKey(kword)) {
//					vid2KwordsSPD.put(kword, (double) distance2Source[vertex]);
//				}
//			}
//
//			// add the unvisited adj vertices of vertex into queue only
//			// if the unvisited adj vertices with distance <= radius
//			if (distance2Source[vertex] + 1 <= radius) {
//				int[] adjList = this.adjLists[vertex];
//				if (adjList == null) {
//					// there is no out-going edge from vertex, dead end,
//					// continue to next vertex
//					continue;
//				}
//				for (int i = 0; i < adjList.length; i++) {
//					int adjVertex = adjList[i];
//					if (visitedFlag[adjVertex] != source) {
//						// not visited yet
//						preceder[adjVertex] = vertex;
//						distance2Source[adjVertex] = 1 + distance2Source[vertex];
//						visitedFlag[adjVertex] = source;
//						queue.add(adjVertex);
//					}
//				}
//			}
//		}
		// this.reset();
		return vid2KwordsSPD;
	}

	/**
	 * For BSP to get the looseness score and also the semantic tree rooted at source
	 * @param source
	 * @param qwords
	 * @param vertexQwordsMap
	 * @param semanticTree
	 * @return
	 * @throws Exception
	 */
	public double getSemanticPlaceB(int source, Integer[] qwords, VertexQwordsMap<Integer> vertexQwordsMap,
			List<List<Integer>> semanticTree,GraphAttributes gb) throws Exception {
		Hashtable<Integer,GraphAttr> ht = new Hashtable<Integer,GraphAttr>();
		
		double looseness = 1.0;
		Set<Integer> keyVertices = new HashSet<Integer>();

		if (qwords.length == 0) {
			throw new IllegalArgumentException("must provide at least one query keyword");
		}

		Set<Integer> qwordsCopy = new HashSet<Integer>();
		for (int i = 0; i < qwords.length; i++) {
			qwordsCopy.add(qwords[i]);
		}

		if (source < 0 || source >= this.numVertices) {
			throw new Exception("source id is out of range, " + source + " should be in [0,"
					+ (this.numVertices - 1) + "]");
		}

//		ga.preceder[source] = -1;
//		ga.distance2Source[source] = 0;
//		ga.visitedFlag[source] = source;
		GraphAttr graphAttr = new GraphAttr(-1,0,source);
		ht.put(source, graphAttr);
		
		Queue<Integer> queue = new LinkedList<Integer>();
		queue.add(source);

		while (!queue.isEmpty()) {
			int vertex = queue.poll();
			Set<Integer> qwordsContainedByVertex = vertexQwordsMap.vertexQwordsMap.get(vertex);

			if (qwordsContainedByVertex != null && qwordsContainedByVertex.size() > 0) {
				// vertex contains some query keywords.
				int prevQwordSize = qwordsCopy.size();
				qwordsCopy.removeAll(qwordsContainedByVertex);
				int currQwordSize = qwordsCopy.size();
				if (prevQwordSize > currQwordSize) {
					// vertex is a key-vertex of keywords not discovered yet
					looseness += (prevQwordSize - currQwordSize) * ht.get(vertex).getDistance2Source();
					keyVertices.add(vertex);
					if (qwordsCopy.size() == 0) {
						break;
					}
				}
			}

			// add the unvisited adj vertices of vertex into queue
			int[] adjList = this.adjLists[vertex];
			if (adjList == null) {
				// there is no out-going edge from vertex, dead end, continue to next vertex
				continue;
			}
//			for (int i = 0; i < adjList.length; i++) {
//				int adjVertex = adjList[i];
//				if (ga.visitedFlag[adjVertex] != source) {
//					// not visited yet
//					ga.preceder[adjVertex] = vertex;
//					ga.distance2Source[adjVertex] = 1 + ga.distance2Source[vertex];
//					ga.visitedFlag[adjVertex] = source;
//					queue.add(adjVertex);
//				}
//			}
			for (int i = 0; i < adjList.length; i++) {
				int adjVertex = adjList[i];
				
				if (ht.get(adjVertex).getVisitedFlag() != source) {
					// not visited yet
					ht.get(adjVertex).setPreceder(vertex);
					//ga.preceder[adjVertex] = vertex;
					//ga.distance2Source[adjVertex] = 1 + ga.distance2Source[vertex];
					int temp = ht.get(vertex).getDistance2Source() + 1;
					ht.get(adjVertex).setDistance2Source(temp);
					//ga.visitedFlag[adjVertex] = source;
					ht.get(adjVertex).setVisitedFlag(source);
					queue.add(adjVertex);
				}
			}	
			
		}

		if (qwordsCopy.size() > 0) {
			looseness = Double.POSITIVE_INFINITY;
		}

		// compute semantic tree paths
		if (looseness != Double.POSITIVE_INFINITY) {
			for (Integer keyVertex : keyVertices) {
				semanticTree.add(this.getPath(source, keyVertex,ht));
			}
		}
		return looseness;
	}

	/**
	 * For SPP to get the looseness score and also the semantic tree rooted at source
	 * @param source
	 * @param qwords
	 * @param loosenessThreshold
	 * @param vertexQwordsMap
	 * @param semanticTree
	 * @return
	 * @throws Exception
	 */
	public double getSemanticPlaceP(int source, Integer[] qwords,
			VertexQwordsMap<Integer> vertexQwordsMap, List<List<Integer>> semanticTree,KthScore kthScore) throws Exception {
		long startrs1 = System.currentTimeMillis();
		
		Hashtable<Integer,GraphAttr> ht = new Hashtable<Integer,GraphAttr>();
		
		double looseness = 1.0;
		Set<Integer> keyVertices = new HashSet<Integer>();

		if (qwords.length == 0) {
			throw new IllegalArgumentException("must provide at least one query keyword");
		}
		if (kthScore.getLoosenessThreshold() < 0) {
			throw new IllegalArgumentException("radius limitation must be >= 0");
		}
		
		Set<Integer> qwordsCopy = new HashSet<Integer>();
		for (int i = 0; i < qwords.length; i++) {
			qwordsCopy.add(qwords[i]);
		}

		if (source < 0 || source >= this.numVertices) {
			throw new Exception("source id is out of range, " + source + " should be in [0,"
					+ (this.numVertices - 1) + "]");
		}

		GraphAttr graphAttr = new GraphAttr(-1,0,source);
		ht.put(source, graphAttr);
		
		Queue<Integer> queue = new LinkedList<Integer>();
		queue.add(source);
		
		double preRadius = 0;		
		long startrs4 = System.currentTimeMillis();
		int count = 0;
		while (!queue.isEmpty()) {
			
			//System.out.println("count:"+ count++);
			int vertex = queue.poll();
			//double currentRadius = ga.distance2Source[vertex];
			
			double currentRadius = ht.get(vertex).getDistance2Source();
			if (currentRadius != preRadius) {
				double delta = currentRadius - preRadius;
				preRadius = currentRadius;
				looseness += delta * qwordsCopy.size();
				if (looseness > kthScore.getLoosenessThreshold()) { //Dynamic bound based pruning
					System.out.println(Thread.currentThread().getName() +" placeID : "+source +" looseness : "+looseness+ " currentRadius : " + currentRadius);
					looseness = Double.POSITIVE_INFINITY;					
					break;
				}
			}
			
			Set<Integer> qwordsContainedByVertex = vertexQwordsMap.vertexQwordsMap.get(vertex);
			if (qwordsContainedByVertex != null && qwordsContainedByVertex.size() > 0) {
				// vertex contains some query keywords.
				int prevQwordSize = qwordsCopy.size();
				qwordsCopy.removeAll(qwordsContainedByVertex);
				if (prevQwordSize > qwordsCopy.size()) {
					// vertex is a key-vertex
					keyVertices.add(vertex);
					if (qwordsCopy.size() == 0) {
						break;
					}
				}
			}		
			
			// add the unvisited adj vertices of vertex into queue
			int[] adjList = this.adjLists[vertex];
			if (adjList == null) {
				// there is no out-going edge from vertex, dead end, continue to next vertex
				continue;
			}
			
			for (int i = 0; i < adjList.length; i++) {
				
				int adjVertex = adjList[i];
				int vft=0;				
				if(ht.get(adjVertex) == null){
					vft = -1;
				}else{
					vft = ht.get(adjVertex).getVisitedFlag();
				}
				
				if (vft != source) {
					ht.put(adjVertex, new GraphAttr(vertex,(ht.get(vertex).getDistance2Source()+1),source));					
					queue.add(adjVertex);
				}
			}	
			
		}

		long endrs4 = System.currentTimeMillis();
		long timers4 = endrs4 - startrs4;
		System.out.println("source: "+source+"整个while循环的时间：" + timers4);
		
		if (qwordsCopy.size() > 0) {
			looseness = Double.POSITIVE_INFINITY;
		}

		// compute semantic tree paths
		if (looseness != Double.POSITIVE_INFINITY) {
			for (Integer keyVertex : keyVertices) {
				semanticTree.add(this.getPath(source, keyVertex,ht));
			}
		}
		return looseness;
	}
	
	/**
	 * For SPP to get the looseness score and also the semantic tree rooted at source
	 * @param source
	 * @param qwords
	 * @param loosenessThreshold
	 * @param vertexQwordsMap
	 * @param semanticTree
	 * @return
	 * @throws Exception
	 */
	public double getSemanticPlaceP2(int source, Integer[] qwords,double loosenessThreshold,
			VertexQwordsMap<Integer> vertexQwordsMap, List<List<Integer>> semanticTree) throws Exception {
		
		Hashtable<Integer,GraphAttr> ht = new Hashtable<Integer,GraphAttr>();
		
		double looseness = 1.0;
		Set<Integer> keyVertices = new HashSet<Integer>();

		if (qwords.length == 0) {
			throw new IllegalArgumentException("must provide at least one query keyword");
		}
		if (loosenessThreshold < 0) {
			throw new IllegalArgumentException("radius limitation must be >= 0");
		}
		
		Set<Integer> qwordsCopy = new HashSet<Integer>();
		for (int i = 0; i < qwords.length; i++) {
			qwordsCopy.add(qwords[i]);
		}

		if (source < 0 || source >= this.numVertices) {
			throw new Exception("source id is out of range, " + source + " should be in [0,"
					+ (this.numVertices - 1) + "]");
		}

		GraphAttr graphAttr = new GraphAttr(-1,0,source);
		ht.put(source, graphAttr);
		
		Queue<Integer> queue = new LinkedList<Integer>();
		queue.add(source);
		
		double preRadius = 0;		
		long startrs4 = System.currentTimeMillis();
		
		
		long sum1 = 0, sum2 = 0,sum3 = 0, sum4 = 0;
		while (!queue.isEmpty()) {
			
			// todo 
			long end_1 = System.currentTimeMillis();
			int vertex = queue.poll();			
			double currentRadius = ht.get(vertex).getDistance2Source();
			if (currentRadius != preRadius) {
				double delta = currentRadius - preRadius;
				preRadius = currentRadius;
				looseness += delta * qwordsCopy.size();
				if (looseness > loosenessThreshold) { //Dynamic bound based pruning		
					//System.out.println("3：可删除的placeData:" + source + " looseness: "+looseness +" loosenessThreshold: "+loosenessThreshold);
					looseness = Double.POSITIVE_INFINITY;					
					break;
				}
			}
			long end_2 = System.currentTimeMillis();
			
			sum1+= (end_2 - end_1);
			
			Set<Integer> qwordsContainedByVertex = vertexQwordsMap.vertexQwordsMap.get(vertex);
			
			long end_3 = System.currentTimeMillis();
			sum2+= (end_3 - end_2);
			
			if (qwordsContainedByVertex != null && qwordsContainedByVertex.size() > 0) {
				// vertex contains some query keywords.
				int prevQwordSize = qwordsCopy.size();
				qwordsCopy.removeAll(qwordsContainedByVertex);
				if (prevQwordSize > qwordsCopy.size()) {
					// vertex is a key-vertex
					keyVertices.add(vertex);
					if (qwordsCopy.size() == 0) {
						break;
					}
				}
			}		
			long end_4 = System.currentTimeMillis();
			sum3+= (end_4 - end_3);
			
			// add the unvisited adj vertices of vertex into queue
			int[] adjList = this.adjLists[vertex];
			if (adjList == null) {
				// there is no out-going edge from vertex, dead end, continue to next vertex
				continue;
			}
			
			for (int i = 0; i < adjList.length; i++) {
				
				int adjVertex = adjList[i];
				int vft=0;				
				if(ht.get(adjVertex) == null){
					vft = -1;
				}else{
					vft = ht.get(adjVertex).getVisitedFlag();
				}
				
				if (vft != source) {
					ht.put(adjVertex, new GraphAttr(vertex,(ht.get(vertex).getDistance2Source()+1),source));					
					queue.add(adjVertex);
				}
			}	
		}
		
		long endrs4 = System.currentTimeMillis();
		
		if (qwordsCopy.size() > 0) {
			looseness = Double.POSITIVE_INFINITY;
		}

		// compute semantic tree paths
		if (looseness != Double.POSITIVE_INFINITY) {
			for (Integer keyVertex : keyVertices) {
				semanticTree.add(this.getPath(source, keyVertex,ht));
			}
		}
		return looseness;
	}
	
	public double getSemanticPlaceP3(int source, Integer[] qwords,
			VertexQwordsMap<Integer> vertexQwordsMap, List<List<Integer>> semanticTree,double m_minDist,double weight,DoubleAccumulator kthScore) throws Exception {
		long startrs1 = System.currentTimeMillis();
		
		Hashtable<Integer,GraphAttr> ht = new Hashtable<Integer,GraphAttr>();
		//HashMap<Integer,GraphAttr> ht = new HashMap<Integer,GraphAttr>();
		
		
		double looseness = 1.0;
		Set<Integer> keyVertices = new HashSet<Integer>();

		if (qwords.length == 0) {
			throw new IllegalArgumentException("must provide at least one query keyword");
		}

		
		Set<Integer> qwordsCopy = new HashSet<Integer>();
		for (int i = 0; i < qwords.length; i++) {
			qwordsCopy.add(qwords[i]);
		}

		if (source < 0 || source >= this.numVertices) {
			throw new Exception("source id is out of range, " + source + " should be in [0,"
					+ (this.numVertices - 1) + "]");
		}

		GraphAttr graphAttr = new GraphAttr(-1,0,source);
		ht.put(source, graphAttr);
		
		//Queue<Integer> queue = new LinkedList<Integer>();
		Queue<Integer> queue = new ArrayDeque<Integer>();
		queue.add(source);
		
		double preRadius = 0;		
		long startrs4 = System.currentTimeMillis();
		ArrayList<Long> timeArray = new ArrayList<Long>();
		
		long sum1 = 0, sum2 = 0,sum3 = 0, sum4 = 0,sum5=0,sum6=0,sum7=0,sum8=0,sum9=0;
		while (!queue.isEmpty()) {
			// todo 
			//double score = kthScore.get();
			if(kthScore.get() < m_minDist){
				//System.out.println(" 3：可删除的placeData:" + source+" kthScore:"+kthScore.get() +" m_minDIst: "+m_minDist);
				break;
			}
			
			double loosenessThreshold = Double.POSITIVE_INFINITY;			
			if (kthScore.get() != Double.POSITIVE_INFINITY) {						
				loosenessThreshold = (kthScore.get() / weight);
			}			
			if (loosenessThreshold < 0) {
				throw new IllegalArgumentException("radius limitation must be >= 0");
			}
			
			long end_1 = System.currentTimeMillis();
			int vertex = queue.poll();			
			double currentRadius = ht.get(vertex).getDistance2Source();
			if (currentRadius != preRadius) {
				double delta = currentRadius - preRadius;
				preRadius = currentRadius;
				looseness += delta * qwordsCopy.size();
				if (looseness > loosenessThreshold) { //Dynamic bound based pruning		
					//System.out.println(" 4：可删除的placeData:" + source + " looseness: "+looseness +" loosenessThreshold: "+loosenessThreshold);
					looseness = Double.POSITIVE_INFINITY;					
					break;
				}
			}
			long end_2 = System.currentTimeMillis();
			
			sum1+= (end_2 - end_1);
			
			Set<Integer> qwordsContainedByVertex = vertexQwordsMap.vertexQwordsMap.get(vertex);
			
			long end_3 = System.currentTimeMillis();
			sum2+= (end_3 - end_2);
			
			if (qwordsContainedByVertex != null && qwordsContainedByVertex.size() > 0) {
				// vertex contains some query keywords.
				int prevQwordSize = qwordsCopy.size();
				qwordsCopy.removeAll(qwordsContainedByVertex);
				if (prevQwordSize > qwordsCopy.size()) {
					// vertex is a key-vertex
					keyVertices.add(vertex);
					if (qwordsCopy.size() == 0) {
						break;
					}
				}
			}		
			long end_4 = System.currentTimeMillis();
			//sum3+= (end_4 - end_3);
			
			// add the unvisited adj vertices of vertex into queue
			int[] adjList = this.adjLists[vertex];
			if (adjList == null) {
				// there is no out-going edge from vertex, dead end, continue to next vertex
				continue;
			}

			long start_9 = System.currentTimeMillis();
			//synchronized(GraphByArray.class){
				for (int i = 0; i < adjList.length; i++) {
					int adjVertex = adjList[i];		
					
					int vft=0;				
					GraphAttr graphAttribute= ht.get(adjVertex);
					if(graphAttribute == null){
						vft = -1;
					}else{
						vft = graphAttribute.getVisitedFlag();
					}
										
					if (vft != source) {
						ht.put(adjVertex, new GraphAttr(vertex,(ht.get(vertex).getDistance2Source()+1),source));	
						queue.add(adjVertex);
					}
				}	
			//}
		}
		
		if (qwordsCopy.size() > 0) {
			looseness = Double.POSITIVE_INFINITY;
		}

		// compute semantic tree paths
		if (looseness != Double.POSITIVE_INFINITY) {
			for (Integer keyVertex : keyVertices) {
				semanticTree.add(this.getPath(source, keyVertex,ht));
			}
		}
		return looseness;
	}

	
	/**
	 * For SPP to get the looseness score and also the semantic tree rooted at source
	 * @param source
	 * @param qwords
	 * @param loosenessThreshold
	 * @param vertexQwordsMap
	 * @param semanticTree
	 * @return
	 * @throws Exception
	 */
	public void getSemanticPlaceP4(Data placeData, Integer[] qwords ,VertexQwordsMap<Integer> vertexQwordsMap,
			List<List<Integer>> semanticTree,DoubleAccumulator kthScore,IVisitor result,int k,AtomicBoolean terminateFlag) throws Exception {
		
		int source = placeData.getIdentifier();
		Hashtable<Integer,GraphAttr> ht = new Hashtable<Integer,GraphAttr>();
		
		double looseness = 1.0;
		Set<Integer> keyVertices = new HashSet<Integer>();

		if (qwords.length == 0) {
			throw new IllegalArgumentException("must provide at least one query keyword");
		}
		
		Set<Integer> qwordsCopy = new HashSet<Integer>();
		for (int i = 0; i < qwords.length; i++) {
			qwordsCopy.add(qwords[i]);
		}

		if (source < 0 || source >= this.numVertices) {
			throw new Exception("source id is out of range, " + source + " should be in [0,"
					+ (this.numVertices - 1) + "]");
		}

		GraphAttr graphAttr = new GraphAttr(-1,0,source);
		ht.put(source, graphAttr);
		
		Queue<Integer> queue = new LinkedList<Integer>();
		queue.add(source);
		
		long threshold_start = System.currentTimeMillis();
		double preRadius = 0;		
		while (!queue.isEmpty()) {
			
			double loosenessThreshold = Double.POSITIVE_INFINITY;
			if (kthScore.get() != Double.POSITIVE_INFINITY) {
				loosenessThreshold = (kthScore.get() / placeData.getWeight());
				if (loosenessThreshold < 0) {
					throw new IllegalArgumentException("radius limitation must be >= 0");
				}
			}

			int vertex = queue.poll();			
			double currentRadius = ht.get(vertex).getDistance2Source();
			if (currentRadius != preRadius) {
				double delta = currentRadius - preRadius;
				preRadius = currentRadius;
				looseness += delta * qwordsCopy.size();
				if (looseness > loosenessThreshold) { //Dynamic bound based pruning							
					looseness = Double.POSITIVE_INFINITY;					
					break;
				}
			}
			
			Set<Integer> qwordsContainedByVertex = vertexQwordsMap.vertexQwordsMap.get(vertex);
			
			if (qwordsContainedByVertex != null && qwordsContainedByVertex.size() > 0) {
				// vertex contains some query keywords.
				int prevQwordSize = qwordsCopy.size();
				qwordsCopy.removeAll(qwordsContainedByVertex);
				if (prevQwordSize > qwordsCopy.size()) {
					// vertex is a key-vertex
					keyVertices.add(vertex);
					if (qwordsCopy.size() == 0) {
						break;
					}
				}
			}		
			
			// add the unvisited adj vertices of vertex into queue
			int[] adjList = this.adjLists[vertex];
			if (adjList == null) {
				// there is no out-going edge from vertex, dead end, continue to next vertex
				continue;
			}
			
			for (int i = 0; i < adjList.length; i++) {
				
				int adjVertex = adjList[i];
				int vft=0;				
				if(ht.get(adjVertex) == null){
					vft = -1;
				}else{
					vft = ht.get(adjVertex).getVisitedFlag();
				}
				
				if (vft != source) {
					ht.put(adjVertex, new GraphAttr(vertex,(ht.get(vertex).getDistance2Source()+1),source));					
					queue.add(adjVertex);
				}
			}
			
			long threshold_end = System.currentTimeMillis();
			if ((threshold_end - threshold_start) > Global.runtimeThreshold) {
				terminateFlag.set(false);
				break;
			}
		}
		
		if (qwordsCopy.size() > 0) {
			looseness = Double.POSITIVE_INFINITY;
		}

		// compute semantic tree paths
		if (looseness != Double.POSITIVE_INFINITY) {
			for (Integer keyVertex : keyVertices) {
				semanticTree.add(this.getPath(source, keyVertex,ht));
			}
		}
		
		Global.count[1]++;
		if (looseness < 1) {
			throw new Exception("semantic score " + looseness + " < 1, for place"
					+ placeData.getIdentifier());
		}
		
		// place is a valid candidate that connects to all qwords
		synchronized(GraphByArray.class){
			if (looseness != Double.POSITIVE_INFINITY) {
				Global.count[2]++;// number of valid place candidate
				double rankingScore = placeData.getWeight() * looseness;
				KSPCandidate candidate = new KSPCandidate(new NNEntry(placeData, rankingScore),semanticTree);

				((KSPCandidateVisitor) result).addPlaceCandidate(candidate);
				double score = ((KSPCandidateVisitor) result).size() >= k ? ((KSPCandidateVisitor) result)
						.getWorstRankingScore() : Double.POSITIVE_INFINITY;
				kthScore.accumulate(score);
			}
		}	
	}

	/**
	 * @param sink
	 * @throws Exception
	 */
	public List<Integer> getPath(int source, int sink,Hashtable<Integer,GraphAttr> ht) throws Exception {
		List<Integer> path = new ArrayList<Integer>();
		path.add(sink);
		//int precedeVertex = ga.preceder[sink];
		int precedeVertex = ht.get(sink).getPreceder();
		while (precedeVertex != -1) {
			path.add(precedeVertex);
			//precedeVertex = ga.preceder[precedeVertex];
			precedeVertex = ht.get(precedeVertex).getPreceder();
		}
		Collections.reverse(path);
		// String pathStr = "";
		// for (int i = 0; i < path.size(); i++) {
		// pathStr += path.get(i) + ",";
		// }
		// System.out.println(source + "->" + sink + ": " + pathStr);
		if (path.get(0) != source) {
			throw new Exception("shortest path extracted wrongly. " /* + pathStr */);
		}
		//System.out.println(path.toArray().toString());
		return path;
	}

	/**
	 * @param sink
	 * @throws Exception
	 */
	public List<Integer> getPath(int source, int sink,HashMap<Integer,GraphAttr> ht) throws Exception {
		List<Integer> path = new ArrayList<Integer>();
		path.add(sink);
		//int precedeVertex = ga.preceder[sink];
		int precedeVertex = ht.get(sink).getPreceder();
		while (precedeVertex != -1) {
			path.add(precedeVertex);
			//precedeVertex = ga.preceder[precedeVertex];
			precedeVertex = ht.get(precedeVertex).getPreceder();
		}
		Collections.reverse(path);
		// String pathStr = "";
		// for (int i = 0; i < path.size(); i++) {
		// pathStr += path.get(i) + ",";
		// }
		// System.out.println(source + "->" + sink + ": " + pathStr);
		if (path.get(0) != source) {
			throw new Exception("shortest path extracted wrongly. " /* + pathStr */);
		}
		//System.out.println(path.toArray().toString());
		return path;
	}
	public void reset() {
//		for (int i = 0; i < visitedFlag.length; i++) {
//			visitedFlag[i] = -1;
//			distance2Source[i] = 0;
//			preceder[i] = 0;
//		}
	}

	public void loadGraph(String edgefile) throws Exception {
		// add edges
		String line;
		int cntlines = 0;
		BufferedReader reader = Utility.getBufferedReader(edgefile);

		int countEdges = 0;
		String[] adjListStr;
		String[] adjVerticesStr;
		while ((line = reader.readLine()) != null) {
			cntlines++;
			if (cntlines % 10000 == 0) {
				System.out.println("adding vertex with edges " + cntlines);
			}
			if (line.contains(Global.delimiterPound)) {
				continue;
			}
			adjListStr = line.split(Global.delimiterLevel1);
			if (adjListStr.length != 2) {
				throw new Exception(line + " adjlist wrong");
			}
			int vertex = Integer.parseInt(adjListStr[0]);

			adjVerticesStr = adjListStr[1].split(Global.delimiterLevel2);
			int[] adjVertices = new int[adjVerticesStr.length];
			for (int i = 0; i < adjVerticesStr.length; i++) {
				adjVertices[i] = Integer.parseInt(adjVerticesStr[i]);
			}
			this.adjLists[vertex] = adjVertices;

			countEdges += adjVertices.length;
		}
		reader.close();
	}

	/**
	 * For test purpose
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {

		if (args.length != 2) {
			System.out.println("usage:runnable configfile edgefile");
			System.exit(-1);
		}
		// Utility.loadInitialConfig(args[0]);
		Global.numNodes = 8;
		GraphByArray graph = new GraphByArray(Global.numNodes);
		String edgefile = args[1];
		graph.loadGraph(edgefile);
	}
}
