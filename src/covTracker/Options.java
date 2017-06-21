/**
 * File: src/covTracker/Options.java
 * -------------------------------------------------------------------------------------------
 * Date			Author      Changes
 * -------------------------------------------------------------------------------------------
 * 10/19/15		hcai		created; for command-line argument processing for DynCG Instrumenter
 * 02/04/17		hcai		added option for monitoring events (triggers of event-handling callbacks)
 *
*/
package covTracker;

import java.util.ArrayList;
import java.util.List;

public class Options {
	protected boolean debugOut = false;
	protected boolean dumpJimple = false;
	protected boolean dumpFunctionList = false;
	
	protected boolean instrall = false;
	
	public boolean debugOut() { return debugOut; }
	public boolean dumpJimple() { return dumpJimple; }
	public boolean dumpFunctionList() { return dumpFunctionList; }

	protected boolean instrall() { return instrall; }
	
	
	public final static int OPTION_NUM = 5;
	
	public String[] process(String[] args) {
		List<String> argsFiltered = new ArrayList<String>();
		boolean allowPhantom = true;
		
		for (int i = 0; i < args.length; ++i) {
			String arg = args[i];

			if (arg.equals("-debug")) {
				debugOut = true;
			}
			else if (arg.equals("-dumpJimple")) {
				dumpJimple = true;
			}
			else if (arg.equals("-dumpFunctionList")) {
				dumpFunctionList = true;
			}
			else if (arg.equals("-instrall")) {
				instrall = true;
			}
			else if (arg.equals("-nophantom")) {
				allowPhantom = false;
			}
			else {
				argsFiltered.add(arg);
			}
		}
		
		if (allowPhantom) {
			argsFiltered.add("-allowphantom");
		}
		
		String[] arrArgsFilt = new String[argsFiltered.size()];
		return (String[]) argsFiltered.toArray(arrArgsFilt);
	}
}

/* vim :set ts=4 tw=4 tws=4 */

