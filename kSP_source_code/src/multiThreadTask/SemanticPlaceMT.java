package multiThreadTask;

import java.util.List;
import java.util.concurrent.Callable;

import kSP.candidate.KSPCandidate;
import kSP.candidate.KSPCandidateVisitor;
import mygraph.GraphAttributes;
import queryindex.VertexQwordsMap;
import rdfindex.memory.RTreeWithGI;
import spatialindex.rtree.Data;
import spatialindex.rtree.NNEntry;
import spatialindex.spatialindex.IVisitor;
import utility.Global;

/**
 * SP 算法, SemanticPlace多线程任务
 * 
 * @author YS
 *
 */
public class SemanticPlaceMT implements Callable<String>{
	private Data placeData;
	private Integer[] qwords;
	private double loosenessThreshold ;
	private VertexQwordsMap<Integer> vertexQwordsMap;
	private List<List<Integer>> semanticTree;
	private RTreeWithGI rgi;
	private IVisitor result;
	private int k;
	
	public SemanticPlaceMT(Data placeData,Integer[] qwords,double loosenessThreshold,VertexQwordsMap<Integer> vertexQwordsMap
			,List<List<Integer>> semanticTree,RTreeWithGI rgi,IVisitor result,int k){
		this.placeData = placeData;
		this.qwords = qwords;
		this.loosenessThreshold = loosenessThreshold;
		this.vertexQwordsMap = vertexQwordsMap;
		this.semanticTree = semanticTree;
		this.rgi = rgi;
		this.result = result;
		this.k = k;
	}

	@Override
	public String call() throws Exception {
				
		
		// TODO Auto-generated method stub		
		double looseness = rgi.getGraph().getSemanticPlaceP2(placeData.getIdentifier(),
		qwords, loosenessThreshold, vertexQwordsMap, semanticTree);
		
		if (looseness < 1) {
		throw new Exception("semantic score " + looseness + " < 1, for place"
				+ placeData.getIdentifier());
	    }
		
		synchronized (SemanticPlaceMT.class) {
			if (looseness != Double.POSITIVE_INFINITY) {
				//System.out.println(Thread.currentThread().getName()+" flag4");
			   Global.count[2]++;// number of valid place candidate
				double rankingScore = placeData.getWeight() * looseness;
				KSPCandidate candidate = new KSPCandidate(new NNEntry(placeData, rankingScore),semanticTree);		
				((KSPCandidateVisitor) result).addPlaceCandidate(candidate);		
				Global.endTime = System.currentTimeMillis();		
				//System.out.println(Thread.currentThread().getName()+" flag5");
			}
	}
		return null;
	}

}
