package multiThreadTask;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.DoubleAccumulator;

import kSP.candidate.KSPCandidate;
import kSP.candidate.KSPCandidateVisitor;
import mygraph.GraphAttributes;
import mygraph.GraphByArray;
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

public class SemanticPlaceMT4 implements Runnable{
	
	private Data placeData;
	private Integer[] qwords;
	private VertexQwordsMap<Integer> vertexQwordsMap;
	private List<List<Integer>> semanticTree;
	private RTreeWithGI rgi;
	private IVisitor result;
	private int k;
	private DoubleAccumulator kthScore;
	private AtomicBoolean terminateFlag;
	
	public SemanticPlaceMT4(Data placeData,Integer[] qwords ,VertexQwordsMap<Integer> vertexQwordsMap
			,List<List<Integer>> semanticTree,RTreeWithGI rgi,IVisitor result,int k ,
			DoubleAccumulator kthScore,AtomicBoolean terminateFlag){
		this.placeData = placeData;
		this.qwords = qwords;
		this.vertexQwordsMap = vertexQwordsMap;
		this.semanticTree = semanticTree;
		this.rgi = rgi;
		this.result = result;
		this.k = k;
		this.kthScore = kthScore;
		this.terminateFlag  = terminateFlag;
	}

	@Override
	public void run() {

		try {				
			rgi.getGraph().getSemanticPlaceP4(placeData,qwords,
					vertexQwordsMap, semanticTree,kthScore,result,k,terminateFlag);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
}

}
