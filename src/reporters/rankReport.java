/**
 * File: src/reporter/rankReport.java
 * -------------------------------------------------------------------------------------------
 * Date			Author      Changes
 * -------------------------------------------------------------------------------------------
 * 03/02/17		hcai		created; reporting ranks of different categories of callbacks and sources/sinks of each app
*/
package reporters;

import java.io.PrintStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import dua.Forensics;

import soot.*;

import soot.jimple.infoflow.android.data.AndroidMethod.CATEGORY;
import utils.utils;
import utils.iccAPICom.EVENTCAT;


public class rankReport extends securityReport { //implements Extension {
	
	public static void main(String args[]){
		args = preProcessArgs(opts, args);
		
		if (opts.traceFile==null || opts.traceFile.isEmpty()) {
			// nothing to do
			return;
		}
		if (opts.srcsinkFile==null || opts.srcsinkFile.isEmpty()) {
			if (opts.catsink==null || opts.catsrc==null) {
				// this report relies on an externally purveyed list of taint sources and sinks
				return;
			}
		}
		if (opts.callbackFile ==null || opts.callbackFile.isEmpty()) {
			if (opts.catCallbackFile==null) {
				// this report relies on an externally purveyed list of android callback interfaces
				return;
			}
		}

		rankReport grep = new rankReport();
		// examine catch blocks
		dua.Options.ignoreCatchBlocks = false;
		dua.Options.skipDUAAnalysis = true;
		dua.Options.modelAndroidLC = false;
		dua.Options.analyzeAndroid = true;
		
		soot.options.Options.v().set_src_prec(soot.options.Options.src_prec_apk);
		
		//output as APK, too//-f J
		soot.options.Options.v().set_output_format(soot.options.Options.output_format_dex);
		soot.options.Options.v().set_force_overwrite(true);
		Scene.v().addBasicClass("com.ironsource.mobilcore.BaseFlowBasedAdUnit",SootClass.SIGNATURES);
		
		Forensics.registerExtension(grep);
		Forensics.main(args);
	}
	
	public static <K, V extends Comparable<? super V>> Map<K, Integer> sortByValue( Map<K, V> map )
	{
	    List<Map.Entry<K, V>> list =
	        new LinkedList<Map.Entry<K, V>>( map.entrySet() );
	    Collections.sort( list, new Comparator<Map.Entry<K, V>>()
	    {
	        public int compare( Map.Entry<K, V> o1, Map.Entry<K, V> o2 )
	        {
	            return -(o1.getValue()).compareTo( o2.getValue() );
	        }
	    } );
	
	    Map<K, Integer> result = new LinkedHashMap<K, Integer>();
	    int k=1, r = k;
	    V lastval=null;
	    for (Map.Entry<K, V> entry : list)
	    {
	        if (lastval!=null && entry.getValue().compareTo(lastval)!=0) {
	        	r=k;
	        }
	        result.put( entry.getKey(), r);
	        k++;
	        lastval = entry.getValue();
	    }
	    return result;
	}
	
	public static <K, V extends Collection<?>> Map<K, Integer> sortByValueSize( Map<K, V> map )
	{
	    List<Map.Entry<K, V>> list =
	        new LinkedList<Map.Entry<K, V>>( map.entrySet() );
	    Collections.sort( list, new Comparator<Map.Entry<K, V>>()
	    {
	        public int compare( Map.Entry<K, V> o1, Map.Entry<K, V> o2 )
	        {
	            return -(o1.getValue().size() - o2.getValue().size() );
	        }
	    } );
	
	    Map<K, Integer> result = new LinkedHashMap<K, Integer>();
	    int k=1, r = k;
	    Integer lastval=null;
	    for (Map.Entry<K, V> entry : list)
	    {
	        if (lastval!=null && entry.getValue().size()!=lastval) {
	        	r=k;
	        }
	        result.put( entry.getKey(), r);
	        k++;
	        lastval = entry.getValue().size();
	    }
	    return result;
	}
		
	public void reportSrcs(PrintStream os) {
		// list src/sink by category if applicable
		if (opts.debugOut) {
			os.println("[SOURCE]");
			os.println("format: category\t s_source\t d_source\t d_allSrcInCall\t d_allEscapeSrcs\t d_allEscapeSrcInCalls"+
						"\t s_sourceP\t d_sourceP\t d_allSrcInCallP\t d_allEscapeSrcsP\t d_allEscapeSrcInCallsP");
		}
		
		Map<CATEGORY, Integer> sortedtraversedCatSrcs = sortByValueSize(traversedCatSrcs);
		Map<CATEGORY, Integer> sortedcoveredCatSrcs = sortByValueSize(coveredCatSrcs);
		Map<CATEGORY, Integer> sortedallCatSrcInCalls = sortByValue (allCatSrcInCalls);
		Map<CATEGORY, Integer> sortedallEscapeCatSrcs = sortByValue (allEscapeCatSrcs);
		Map<CATEGORY, Integer> sortedallEscapeCatSrcInCalls = sortByValue (allEscapeCatSrcInCalls);
		
		for (CATEGORY cat : traversedCatSrcs.keySet()) {
			os.println( cat + "\t" + traversedCatSrcs.get(cat).size() + "\t" + 
					(coveredCatSrcs.containsKey(cat)?coveredCatSrcs.get(cat).size():0) + "\t" +
					(allCatSrcInCalls.containsKey(cat)?allCatSrcInCalls.get(cat):0) + "\t" + 
					(allEscapeCatSrcs.containsKey(cat)?allEscapeCatSrcs.get(cat):0) + "\t" +
					allEscapeCatSrcInCalls.get(cat) + "\t" +
					sortedtraversedCatSrcs.get(cat) + "\t" + 
					sortedcoveredCatSrcs.get(cat) + "\t" +
					sortedallCatSrcInCalls.get(cat) + "\t" + 
					sortedallEscapeCatSrcs.get(cat) + "\t" +
					sortedallEscapeCatSrcInCalls.get(cat) );
		}
	}

	public void reportSinks(PrintStream os) {
		if (opts.debugOut) {
			os.println("[SINK]");
			os.println("format: category\t s_sink\t d_sink\t d_allSinkInCall\t d_allReachableSinks\t d_allReachableSinkInCalls"+
			"\t s_sinkP\t d_sinkP\t d_allSinkInCallP\t d_allReachableSinksP\t d_allReachableSinkInCallsP");
		}
		
		Map<CATEGORY, Integer> sortedtraversedCatSinks = sortByValueSize(traversedCatSinks);
		Map<CATEGORY, Integer> sortedcoveredCatSinks = sortByValueSize(coveredCatSinks);
		
		Map<CATEGORY, Integer> sortedallCatSinkInCalls = sortByValue (allCatSinkInCalls);
		Map<CATEGORY, Integer> sortedallReachableCatSinks = sortByValue (allReachableCatSinks);
		Map<CATEGORY, Integer> sortedallReachableCatSinkInCalls = sortByValue (allReachableCatSinkInCalls);
		
		for (CATEGORY cat : traversedCatSinks.keySet()) {
			os.println( cat + "\t" + traversedCatSinks.get(cat).size() + "\t" + 
					(coveredCatSinks.containsKey(cat)?coveredCatSinks.get(cat).size():0) + "\t" +
					(allCatSinkInCalls.containsKey(cat)?allCatSinkInCalls.get(cat):0) + "\t" + 
					(allReachableCatSinks.containsKey(cat)?allReachableCatSinks.get(cat):0) + "\t" +
					allReachableCatSinkInCalls.get(cat) + "\t" +
					sortedtraversedCatSinks.get(cat) + "\t" + 
					sortedcoveredCatSinks.get(cat) + "\t" +
					sortedallCatSinkInCalls.get(cat) + "\t" + 
					sortedallReachableCatSinks.get(cat) + "\t" +
					sortedallReachableCatSinkInCalls.get(cat) );
		}
	}
	

	public void reportLifecycleMethods(PrintStream os) {
		/** report statistics for the current trace */
		if (opts.debugOut) {
			os.println(lifecycleCov);
			os.println(eventhandlerCov);
		}
		
		if (opts.debugOut) {
			os.println("[LifecycleMethods]");
			os.println("format: category\t s_lifecycle\t d_lifecycle\t d_lifecycleInCalls" + 
			"\t s_lifecycleP\t d_lifecycleP\t d_lifecycleInCallsP");
		}
		
		
		Map<String, Integer> sortedtraversedCatLifecycleMethods = sortByValueSize( traversedCatLifecycleMethods );
		Map<String, Integer> sortedcoveredCatLifecycleMethods = sortByValueSize( coveredCatLifecycleMethods );
		Map<String, Integer> sortedallCatLCInCalls = sortByValue (allCatLCInCalls);
		
		for (String lct : traversedCatLifecycleMethods.keySet()) {
			os.println(lct + "\t" + traversedCatLifecycleMethods.get(lct).size() + "\t" + 
						coveredCatLifecycleMethods.get(lct).size() + "\t" + allCatLCInCalls.get(lct) + "\t" +
						sortedtraversedCatLifecycleMethods.get(lct) + "\t" + 
						sortedcoveredCatLifecycleMethods.get(lct) + "\t" + 
						sortedallCatLCInCalls.get(lct)); 
		}
	}

	public void reportEventHandlers(PrintStream os) {
		/** report statistics for the current trace */
		if (opts.debugOut) {
			os.println(lifecycleCov);
			os.println(eventhandlerCov);
		}
		
		if (opts.debugOut) {
			os.println("[EventHandlers]");
			os.println("format: category\t s_eventhandler\t d_eventHandler\t d_eventhandlerInCalls" +
			"\t s_eventhandlerP\t d_eventHandlerP\t d_eventhandlerInCallsP");
		}
		
		Map<EVENTCAT, Integer> sortedtraversedCatEventHandlerMethods = sortByValueSize( traversedCatEventHandlerMethods );
		Map<EVENTCAT, Integer> sortedcoveredCatEventHandlerMethods = sortByValueSize( coveredCatEventHandlerMethods );
		Map<EVENTCAT, Integer> sortedallCatEHInCalls = sortByValue (allCatEHInCalls);
		
		
		for (EVENTCAT et : traversedCatEventHandlerMethods.keySet()) {
			os.println(et + "\t" + traversedCatEventHandlerMethods.get(et).size() + "\t" + 
					coveredCatEventHandlerMethods.get(et).size() + "\t" + allCatEHInCalls.get(et) + "\t" +
					sortedtraversedCatEventHandlerMethods.get(et) + "\t" + 
					sortedcoveredCatEventHandlerMethods.get(et) + "\t" + 
					sortedallCatEHInCalls.get(et));
		}
	}
	
	public void collectFeatures(PrintStream os) {
		// only take the top 5/6 most assessed categories as features
		CATEGORY[] srccats = {CATEGORY.ACCOUNT_INFORMATION, CATEGORY.CALENDAR_INFORMATION, CATEGORY.LOCATION_INFORMATION,
				CATEGORY.NETWORK_INFORMATION, CATEGORY.SYSTEM_SETTINGS};
		//{"ACCOUNT_INFORMATION", "CALENDAR_INFORMATION", "LOCATION_INFORMATION", "NETWORK_INFORMATION", "SYSTEM_SETTINGS"};

		CATEGORY[] sinkcats = {CATEGORY.ACCOUNT_SETTINGS, CATEGORY.FILE, CATEGORY.LOG, CATEGORY.NETWORK, CATEGORY.SMS_MMS, CATEGORY.SYSTEM_SETTINGS};
		//{"ACCOUNT_SETTINGS", "FILE", "LOG", "NETWORK", "SMS_MMS", "SYSTEM_SETTINGS"};
		String[] lccats = {"Activity", "Application", "BroadcastReceiver", "ContentProvider", "Service"};
		EVENTCAT[] ehcats = {EVENTCAT.APPLICATION_MANAGEMENT, EVENTCAT.SYSTEM_STATUS, EVENTCAT.LOCATION_STATUS, EVENTCAT.HARDWARE_MANAGEMENT, EVENTCAT.NETWORK_MANAGEMENT, 
				EVENTCAT.APP_BAR, EVENTCAT.MEDIA_CONTROL, EVENTCAT.VIEW, EVENTCAT.WIDGET, EVENTCAT.DIALOG};
		//{"DIALOG", "HARDWARE_MANAGEMENT", "MEDIA_CONTROL", "NETWORK_MANAGEMENT", "VIEW",	"WIDGET"}; 
		
		//Map<CATEGORY, Integer> sortedtraversedCatSrcs = sortByValueSize(traversedCatSrcs);
		Map<CATEGORY, Integer> sortedcoveredCatSrcs = sortByValueSize(coveredCatSrcs);
		Map<CATEGORY, Integer> sortedallCatSrcInCalls = sortByValue (allCatSrcInCalls);
		Map<CATEGORY, Integer> sortedallEscapeCatSrcs = sortByValue (allEscapeCatSrcs);
		Map<CATEGORY, Integer> sortedallEscapeCatSrcInCalls = sortByValue (allEscapeCatSrcInCalls);
		
		//Map<CATEGORY, Integer> sortedtraversedCatSinks = sortByValueSize(traversedCatSinks);
		Map<CATEGORY, Integer> sortedcoveredCatSinks = sortByValueSize(coveredCatSinks);
		Map<CATEGORY, Integer> sortedallCatSinkInCalls = sortByValue (allCatSinkInCalls);
		Map<CATEGORY, Integer> sortedallReachableCatSinks = sortByValue (allReachableCatSinks);
		Map<CATEGORY, Integer> sortedallReachableCatSinkInCalls = sortByValue (allReachableCatSinkInCalls);
		
		//Map<String, Integer> sortedtraversedCatLifecycleMethods = sortByValueSize( traversedCatLifecycleMethods );
		Map<String, Integer> sortedcoveredCatLifecycleMethods = sortByValueSize( coveredCatLifecycleMethods );
		Map<String, Integer> sortedallCatLCInCalls = sortByValue (allCatLCInCalls);
		
		//Map<EVENTCAT, Integer> sortedtraversedCatEventHandlerMethods = sortByValueSize( traversedCatEventHandlerMethods );
		Map<EVENTCAT, Integer> sortedcoveredCatEventHandlerMethods = sortByValueSize( coveredCatEventHandlerMethods );
		Map<EVENTCAT, Integer> sortedallCatEHInCalls = sortByValue (allCatEHInCalls);
		
		
		if (opts.debugOut) { 
			os.println("*** security feature collection *** "); 
			os.print("format: packagename"+"\t"+"src"+"\t"+"sink"+"\t"+"srcIns"+"\t"+"sinkIns"
					+"\t"+"riskSrc"+"\t"+"riskSink"+ "\t" + "riskSrcIns" + "\t" + "riskSinkIns");
			for (CATEGORY srccatT:srccats) {
				String srccat = srccatT.toString();
				os.print("\t" + srccat);
				os.print("\t" + srccat+"-Ins");
				os.print("\t" + srccat+"-escape");
				os.print("\t" + srccat+"-escape-Ins");
			}

			for (CATEGORY sinkcatT:sinkcats) {
				String sinkcat = sinkcatT.toString();
				os.print("\t" + sinkcat);
				os.print("\t" + sinkcat+"-Ins");
				os.print("\t" + sinkcat+"-reach");
				os.print("\t" + sinkcat+"-reach-Ins");
			}

			os.print("\t" + "lc" + "\t" + "eh" + "\t" + "lc-ins" + "\t" + "eh-ins");
			for (String lccat:lccats) {
				os.print("\t" + lccat);
				os.print("\t" + lccat+"-Ins");
			}
			for (EVENTCAT ehcatT:ehcats) {
				String ehcat = ehcatT.toString();
				os.print("\t" + ehcat);
				os.print("\t" + ehcat+"-Ins");
			}
			os.println();
		}
		// 1. src/sink usage and reachability
		//os.print(this.packName);
		os.print (utils.getFileNameFromPath(soot.options.Options.v().process_dir().get(0)));
		
		os.print("\t" + percentage(srcCov.getCovered(),allCoveredMethods.size()) +
				   "\t" + percentage(sinkCov.getCovered(), allCoveredMethods.size()) +
				   "\t" + percentage(allSrcInCalls, allMethodInCalls) + 
				   "\t" + percentage(allSinkInCalls, allMethodInCalls) + 
				   "\t" + percentage(allEscapeSrcs, srcCov.getCovered()) + 
				   "\t" + percentage(allReachableSinks, sinkCov.getCovered()) + 
				   "\t" + percentage(allEscapeSrcInCalls, allSrcInCalls) +
				   "\t" + percentage(allReachableSinkInCalls, allSinkInCalls));
		
		// 2. src/sink categorization (only the ones in which we found significant different between benign and malware traces)
		for (CATEGORY srccat:srccats) {
			os.print("\t" +
					sortedcoveredCatSrcs.get(srccat) + "\t" +
					sortedallCatSrcInCalls.get(srccat) + "\t" + 
					sortedallEscapeCatSrcs.get(srccat) + "\t" +
					sortedallEscapeCatSrcInCalls.get(srccat) );
		}
		
		for (CATEGORY sinkcat:sinkcats) {
			os.print("\t" +
					sortedcoveredCatSinks.get(sinkcat) + "\t" +
					sortedallCatSinkInCalls.get(sinkcat) + "\t" + 
					sortedallReachableCatSinks.get(sinkcat) + "\t" +
					sortedallReachableCatSinkInCalls.get(sinkcat) );
		}
		
		// 3. callback usage
		os.print("\t" + 
				percentage(lifecycleCov.getCovered(), allCoveredMethods.size()) + "\t" +
				percentage(eventhandlerCov.getCovered(), allCoveredMethods.size()) + "\t" +				
				percentage(allLCInCalls, allMethodInCalls) + "\t" +
				percentage(allEHInCalls, allMethodInCalls));
		
		// 4. callback categorization
		for (String lccat:lccats) {
			os.print("\t" +
					sortedcoveredCatLifecycleMethods.get(lccat) + "\t" + 
					sortedallCatLCInCalls.get(lccat));	
		}
		for (EVENTCAT ehcatT:ehcats) {
			os.print("\t" +
					sortedcoveredCatEventHandlerMethods.get(ehcatT) + "\t" + 
					sortedallCatEHInCalls.get(ehcatT) );
		}
		
		os.println();
	}
}

/* vim :set ts=4 tw=4 tws=4 */

