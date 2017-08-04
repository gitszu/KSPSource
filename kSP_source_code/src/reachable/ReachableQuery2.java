/**
 * 
 */
package reachable;

import java.util.Scanner;

/**
 * TF-label method (C++) for reachable query
 * Use JNI to call C++ shared library from java
 * @author jmshi 
 */
public class ReachableQuery2 {

	public native void initQuery(int sccN, String ind_filename);
	public native void freeQuery(int sccN);
	public native boolean queryReachable(int p, int q, int sccN);

	/**
	 * For test purpose
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println("usage: runnable sccNumber tf_label_indexfilename");
		System.loadLibrary("ReachableQuery");
		ReachableQuery2 sample = new ReachableQuery2();

		int sccN = Integer.parseInt(args[0]);
		String ind_filename = args[1];
		sample.initQuery(sccN, ind_filename);

		Scanner keyboard = new Scanner(System.in);
		System.out.println("source sink reachable or not: ");
		int p = keyboard.nextInt();
		int q = keyboard.nextInt();
		while (p >= 0 && q >= 0) {

			boolean isReachable = sample.queryReachable(p, q, sccN);
			if (isReachable) {
				System.out.println("true");
			} else {
				System.out.println("false");
			}
			System.out.println("source sink reachable or not: ");
			p = keyboard.nextInt();
			q = keyboard.nextInt();
		}
		keyboard.close();
		
		sample.freeQuery(sccN);
	}
}
