/**
 * 
 */
package precomputation;

import utility.IindexUtility;
import utility.Utility;

/**
 * 
 * Build inverted index stored in plain text file.
 * 
 * @author jmshi
 * 
 */
public class InvertedIndexPlainTextBuilder {
	
	public static void main(String[] args) throws Exception {
		if (args.length != 4) {
			throw new Exception("\n Usage: runnable configFile inputDocFile outputIindexFile isWeighted(y/n)");
		}
		Utility.loadInitialConfig(args[0]);
		String inputDocFile = args[1];
		String outputIindexFile = args[2];
		String isWeighted = args[3];
		long start = System.currentTimeMillis();
		if (isWeighted.contains("y")) {
			IindexUtility.InvertedIndexWeigthedBuilder(inputDocFile, outputIindexFile);
		}
		else{
			IindexUtility.InvertedIndexBuilder(inputDocFile, outputIindexFile);
		}
		long end = System.currentTimeMillis();
		System.out.println("Revision Minutes: " + ((end - start) / 1000.0f) / 60.0f);
	}
}

