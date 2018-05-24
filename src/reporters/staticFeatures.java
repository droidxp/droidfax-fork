/**
 * File: src/reporter/staticFeatures.java
 * -------------------------------------------------------------------------------------------
 * Date			Author      Changes
 * -------------------------------------------------------------------------------------------
 * 05/22/18		hcai		created; for computing all static features based on the droidfax characterization metrics
*/
package reporters;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import dua.Extension;
import dua.Forensics;
import dua.global.ProgramFlowGraph;
import soot.*;
import soot.jimple.Stmt;
import soot.jimple.infoflow.android.data.AndroidMethod;
import soot.jimple.infoflow.android.data.AndroidMethod.CATEGORY;
import soot.jimple.infoflow.android.data.parsers.CategorizedAndroidSourceSinkParser;
import utils.AndroidEntryPointConstants;
import utils.iccAPICom;
import utils.utils;
import utils.iccAPICom.EVENTCAT;


public class staticFeatures implements Extension {
	
	protected static reportOpts opts = new reportOpts();
	
	public final static String AndroidClassPattern = "(android|com\\.example\\.android|com\\.google|com\\.android|dalvik)\\.(.)+"; 
	public final static String OtherSDKClassPattern = "(gov\\.nist|java|javax|junit|libcore|net\\.oauth|org\\.apache|org\\.ccil|org\\.javia|" +
			"org\\.jivesoftware|org\\.json|org\\.w3c|org\\.xml|sun|com\\.adobe|com\\.svox|jp\\.co\\.omronsoft|org\\.kxml2|org\\.xmlpull)\\.(.)+";

	// application code coverage statistics
	protected final covStat appClsCov = new covStat("Application Class");
	protected final covStat appMethodCov = new covStat("Application Method");
	// user/third-party library code coverage  
	protected final covStat ulClsCov = new covStat("Library Class");
	protected final covStat ulMethodCov = new covStat("Library Method");
	// framework library (Android SDK) code coverage  
	protected final covStat sdkClsCov = new covStat("SDK Class");
	protected final covStat sdkMethodCov = new covStat("SDK Method");
	
	protected final covStat inIccCov = new covStat("Incoming ICC Coverage");
	protected final covStat outIccCov = new covStat("Outgoing ICC Coverage");
	
	protected final covStat srcCov = new covStat("source coverage");
	protected final covStat sinkCov = new covStat("sink coverage");

	protected final covStat lifecycleCov = new covStat("lifecylce method coverage");
	protected final covStat eventhandlerCov = new covStat("event handler coverage");
	
	/** for categorized source/sink */
	Map<String, CATEGORY> allCatSrcs = new HashMap<String,CATEGORY>();
	Map<String, CATEGORY> allCatSinks = new HashMap<String,CATEGORY>();
	Map<CATEGORY, Set<String>> traversedCatSrcs = new HashMap<CATEGORY, Set<String>>();
	Map<CATEGORY, Set<String>> traversedCatSinks = new HashMap<CATEGORY, Set<String>>();
	
	/** for callbacks */
	Set<String> callbackClses = new HashSet<String>();
	Set<SootClass> callbackSootClses = new HashSet<SootClass>();
	
	Map<String,EVENTCAT> catCallbackClses = new HashMap<String,EVENTCAT>();
	
	// more fine-grained classification of callbacks
	Map<EVENTCAT, Set<String>> traversedCatEventHandlerMethods = new HashMap<EVENTCAT, Set<String>>();
	Map<String, Set<String>> traversedCatLifecycleMethods = new HashMap<String, Set<String>>();
	
	String packName = "";

	public static void main(String args[]){
		args = preProcessArgs(opts, args);
		
		if (opts.catsink==null || opts.catsrc==null) {
			// this report relies on an externally purveyed list of taint sources and sinks
			return;
		}
		if (opts.catCallbackFile==null) {
			// this report relies on an externally purveyed list of android callback interfaces
			return;
		}

		staticFeatures grep = new staticFeatures();
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
	
	protected static String[] preProcessArgs(reportOpts _opts, String[] args) {
		opts = _opts;
		args = opts.process(args);
		
		String[] argsForDuaF;
		int offset = 0;

		argsForDuaF = new String[args.length + 2 - offset];
		System.arraycopy(args, offset, argsForDuaF, 0, args.length-offset);
		argsForDuaF[args.length+1 - offset] = "-paramdefuses";
		argsForDuaF[args.length+0 - offset] = "-keeprepbrs";
		
		return argsForDuaF;
	}
	
	/**
	 * Descendants may want to use customized event monitors
	 */
	/** mapping from component type to component classes */
	Map<String, Set<String>> sct2cc = new HashMap<String, Set<String>>();
	
	protected void init() {
		packName = ProgramFlowGraph.appPackageName;
		
		for (String ctn : iccAPICom.component_type_names) {
			sct2cc.put(ctn, new HashSet<String>());
		}
		
		if (opts.catsink!=null && opts.catsrc!=null) {
			readCatSrcSinks();
		}
		
		try {
			if (opts.catCallbackFile!=null) {
				loadCatAndroidCallbacks();
			}

			for (String clsname : callbackClses) {
				callbackSootClses.add( Scene.v().getSootClass(clsname) );
			}
		}
		catch (Exception e) {
			System.err.println("Failed in parsing the androidCallbacks file: ");
			e.printStackTrace(System.err);
			System.exit(-1);
		}
		for (String lct : iccAPICom.component_type_names) {
			traversedCatLifecycleMethods.put(lct, new HashSet<String>());
		}
	}
	
	protected void readCatSrcSinks() {
		Set<CATEGORY> allcats = new HashSet<CATEGORY>();
		allcats.addAll(Arrays.asList(CATEGORY.ALL.getDeclaringClass().getEnumConstants()));
		CategorizedAndroidSourceSinkParser catsrcparser = 
			new CategorizedAndroidSourceSinkParser(allcats, opts.catsrc, true, false);
		CategorizedAndroidSourceSinkParser catsinkparser = 
			new CategorizedAndroidSourceSinkParser(allcats, opts.catsink, false, true);
		
		for (CATEGORY cat : allcats) {
			traversedCatSrcs.put(cat, new HashSet<String>());
			traversedCatSinks.put(cat, new HashSet<String>());
		}

		try {
			for (AndroidMethod am : catsrcparser.parse()) {
				allCatSrcs.put(am.getSignature(), am.getCategory());
				
			}
			for (AndroidMethod am : catsinkparser.parse()) {
				allCatSinks.put(am.getSignature(), am.getCategory());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public String isCallbackClass(SootClass cls) {
		FastHierarchy har = Scene.v().getOrMakeFastHierarchy();
		for (SootClass scls : callbackSootClses) {
			if (har.getAllSubinterfaces(scls).contains(cls)) {
				return scls.getName();
			}
			if (har.getAllImplementersOfInterface(scls).contains(cls)) {
				return scls.getName();
			}
		}
		return null;
	}
	public boolean isCallbackClassActive(SootClass cls) {
		Hierarchy har = Scene.v().getActiveHierarchy();
		for (SootClass scls : callbackSootClses) {
			if (har.getSubinterfacesOf(scls).contains(cls)) {
				return true;
			}
			if (har.getImplementersOf(scls).contains(cls)) {
				return true;
			}
		}
		return false;
	}
	Set<EVENTCAT> allCBCats = new HashSet<EVENTCAT>(Arrays.asList(EVENTCAT.ALL.getDeclaringClass().getEnumConstants()));
	Map<String,EVENTCAT> cat2Literal = new HashMap<String,EVENTCAT>();
	private void loadCatAndroidCallbacks() throws IOException {
		BufferedReader rdr = null;
		for (EVENTCAT cat : allCBCats) {
			cat2Literal.put(cat.toString(),cat);

			traversedCatEventHandlerMethods.put(cat, new HashSet<String>());
		}
		try {
			String fileName = opts.catCallbackFile;
			if (!new File(fileName).exists()) {
				throw new RuntimeException("categorized Callback definition file not found");
			}
			rdr = new BufferedReader(new FileReader(fileName));
			String line;
			EVENTCAT curcat = EVENTCAT.ALL;
			while ((line = rdr.readLine()) != null) {
				line = line.trim();
				if (line.isEmpty()) continue;
				
				if (cat2Literal.keySet().contains(line)) {
					curcat = cat2Literal.get(line);
					continue;
				}
				if (curcat == EVENTCAT.ALL) continue;
				catCallbackClses.put(line,curcat);
				
				// maintain a holistic list of ALL callback classes as well
				callbackClses.add(line);
			}
		}
		finally {
			if (rdr != null)
				rdr.close();
		}
	}
	
	public void run() {
		System.out.println("computing static features...");
		//System.out.println(Scene.v().getPkgList());
		
		init();
		
		traverse();
		
		String dir = opts.resultDir;
		
		try {
			String fngdistfeature = dir + File.separator + "staticfeatures.txt";
			PrintStream psgdistfeature = new PrintStream (new FileOutputStream(fngdistfeature,true));
			collectFeatures(psgdistfeature);
		}
		catch (Exception e) {e.printStackTrace();}
			
		System.exit(0);
	}

	public void traverse() {
		/* traverse all classes */
		Iterator<SootClass> clsIt = Scene.v().getClasses().snapshotIterator();//.iterator(); //ProgramFlowGraph.inst().getAppClasses().iterator();
		while (clsIt.hasNext()) {
			SootClass sClass = (SootClass) clsIt.next();
			if ( sClass.isPhantom() ) {	continue; }
			boolean isAppCls = false, isSDKCls = false, isULCls = false;
			//if ( sClass.isApplicationClass() ) {
			if (sClass.getName().contains(packName)) {	
				appClsCov.incTotal();
				isAppCls = true;
			}
			else {
				// differentiate user library from SDK library
				if (sClass.getName().matches(AndroidClassPattern) || sClass.getName().matches(OtherSDKClassPattern)) {
					sdkClsCov.incTotal();
					isSDKCls = true;
				}
				//else if (!sClass.getName().contains(packName)) {
				else {	
					ulClsCov.incTotal();
					isULCls = true;
				}
			}
			
			String ctn = iccAPICom.getComponentType(sClass);
			boolean isComponent = false;
			if (ctn.compareTo("Unknown")!=0) {
				sct2cc.get(ctn).add( sClass.getName() );
				isComponent = true;
			}
			
			String CallbackCls = isCallbackClass(sClass);
			
			/* traverse all methods of the class */
			Iterator<SootMethod> meIt = sClass.getMethods().iterator();
			while (meIt.hasNext()) {
				SootMethod sMethod = (SootMethod) meIt.next();
				String meId = sMethod.getSignature();
				
				if ( !sMethod.isConcrete() ) {
					// skip abstract methods and phantom methods, and native methods as well
					//continue; 
				}
				
				if (isAppCls) {
					appMethodCov.incTotal();
				}
				else if (isSDKCls ){
					sdkMethodCov.incTotal();
				}
				else {
					assert isULCls;
					ulMethodCov.incTotal();
				}
				
				
				if (isComponent && AndroidEntryPointConstants.isLifecycleMethod(sMethod.getSubSignature())) {
					lifecycleCov.incTotal();
					
					String lifecycleType = AndroidEntryPointConstants.getLifecycleType(sMethod.getSubSignature());
					traversedCatLifecycleMethods.get(lifecycleType).add(meId);
				}
				
				if (CallbackCls!=null && sMethod.getName().startsWith("on")) {
					eventhandlerCov.incTotal();
					
					EVENTCAT ehType = EVENTCAT.ALL;
					if (opts.catCallbackFile!=null) {
						ehType = catCallbackClses.get(CallbackCls);
						traversedCatEventHandlerMethods.get(ehType).add(meId);
					}
				}
				
				if ( !sMethod.isConcrete() ) {
                    // skip abstract methods and phantom methods, and native methods as well
                    continue; 
                }
				
				Body body = sMethod.retrieveActiveBody();
				PatchingChain<Unit> pchn = body.getUnits();
				
				Iterator<Unit> itchain = pchn.snapshotIterator();
				while (itchain.hasNext()) {
					Stmt s = (Stmt)itchain.next();
					if (iccAPICom.is_IntentSendingAPI(s)) {
						outIccCov.incTotal();
					}
					else if (iccAPICom.is_IntentReceivingAPI(s)) {
						inIccCov.incTotal();
					}
					
					if (!s.containsInvokeExpr()) {
						continue;
					}
					String calleename = s.getInvokeExpr().getMethod().getSignature();
					
					if (opts.catsink!=null && opts.catsrc!=null) {
						if (allCatSrcs.keySet().contains(calleename)) {

							Set<String> cts = traversedCatSrcs.get(allCatSrcs.get(calleename));
							if (null==cts) {
								cts = new HashSet<String>();
								traversedCatSrcs.put(allCatSrcs.get(calleename), cts);
							}
							if (cts.add(calleename))
								srcCov.incTotal();
						}
						if (allCatSinks.keySet().contains(calleename)) {

							Set<String> cts = traversedCatSinks.get(allCatSinks.get(calleename));
							if (null==cts) {
								cts = new HashSet<String>();
								traversedCatSinks.put(allCatSinks.get(calleename), cts);
							}
							if (cts.add(calleename))
								sinkCov.incTotal();
						}
					}
				}
				
			} // -- while (meIt.hasNext()) 
			
		} // -- while (clsIt.hasNext())
	}
	
	static String percentage(int a, int b) {
		DecimalFormat df = new DecimalFormat("#.####");
		if (b==0) return df.format(0); 
		return df.format(a*1.0/b);
	}

	// gather metrics used as potential ML classification features 
	public void collectFeatures(PrintStream os) {
		if (opts.debugOut) {
			os.println("*** general feature collection *** ");
			os.print("format: packagename"+"\t");
		}
		
		if (opts.debugOut) {
			os.println("userCode-cls"+"\t"+"3rdLib-cls"+"\t"+"sdk-cls"+"\t"+"userCode-me"+"\t"+"3rdlib-me"+"\t"+"sdk-me"+
			   "\t"+"activity"+"\t"+"service"+"\t"+"receiver"+"\t"+"provider");
		}
		//System.out.println("apkname = " + utils.getFileNameFromPath(soot.options.Options.v().process_dir().get(0)));
		//os.print (utils.getFileNameFromPath(soot.options.Options.v().process_dir().get(0)));
		String featureKey = utils.getFeatureKey(soot.options.Options.v().process_dir().get(0), opts);
		System.out.println("feature-vector-key = " + featureKey);
		os.print (featureKey);
		
		// 1. composition - percentage of each code layer (w.r.t call targets) 
		int sclsTotal = (appClsCov.getTotal()+ulClsCov.getTotal()+sdkClsCov.getTotal());
		int smeTotal = (appMethodCov.getTotal()+ulMethodCov.getTotal()+sdkMethodCov.getTotal());
		os.print("\t" + percentage(appClsCov.getTotal(),sclsTotal) + "\t" + 
				   percentage(ulClsCov.getTotal(),sclsTotal) + "\t" + 
				   percentage(sdkClsCov.getTotal(),sclsTotal) + "\t" + 
				   percentage(appMethodCov.getTotal(),smeTotal) + "\t" + 
				   percentage(ulMethodCov.getTotal(),smeTotal) + "\t" + 
				   percentage(sdkMethodCov.getTotal(),smeTotal));

		// 2. component distribution - percentage of each type 
		int sctsum = 0;
		for (int i=0;i<4;i++) {
			String key = iccAPICom.component_type_names[i];
			sctsum += sct2cc.get(key).size();
		}
		for (int i=0;i<4;i++) {
			String key = iccAPICom.component_type_names[i];
			os.print("\t"+percentage(sct2cc.get(key).size(),sctsum));
		}
		
		// 3. percentage of incoming and outgoing ICC calls in code
		os.print("\t" + percentage(inIccCov.getTotal(),smeTotal) + "\t" + 
				   percentage(outIccCov.getTotal(),smeTotal));
		
		// 4. percentage of source and sink calls in code
		os.print("\t" + percentage(srcCov.getTotal(),smeTotal) + "\t" + 
				   percentage(sinkCov.getTotal(),smeTotal));
		
		// 5. source categorization
		for (CATEGORY cat : traversedCatSrcs.keySet()) {
			os.print( "\t" + percentage(traversedCatSrcs.get(cat).size(),srcCov.getTotal()));
		}
		// 6. sink categorization
		for (CATEGORY cat : traversedCatSinks.keySet()) {
			os.print( "\t" + percentage(traversedCatSinks.get(cat).size(),sinkCov.getTotal()));
		}
		
		// 7. percentage of lifecycle and event-handling callback calls in code
		os.print("\t" + percentage(lifecycleCov.getTotal(),smeTotal) + "\t" + 
				   percentage(eventhandlerCov.getTotal(),smeTotal));
		
		// 8. lifecycle categorization
		for (String lct : traversedCatLifecycleMethods.keySet()) {
			os.print("\t" + percentage(traversedCatLifecycleMethods.get(lct).size(),lifecycleCov.getTotal())); 
		}
		// 9. event-handler categorization
		for (EVENTCAT et : traversedCatEventHandlerMethods.keySet()) {
			os.print("\t" + percentage(traversedCatEventHandlerMethods.get(et).size(), eventhandlerCov.getTotal()));
		}
		
		
		os.println();
	}
}  

/* vim :set ts=4 tw=4 tws=4 */

