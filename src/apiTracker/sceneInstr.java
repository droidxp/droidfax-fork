package apiTracker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import dua.Extension;
import dua.Forensics;
import profile.InstrumManager;
import soot.ArrayType;
import soot.Body;
import soot.Local;
import soot.PatchingChain;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.javaToJimple.LocalGenerator;
import soot.jimple.ArrayRef;
import soot.jimple.AssignStmt;
import soot.jimple.IdentityStmt;
import soot.jimple.IntConstant;
import soot.jimple.Jimple;
import soot.jimple.NewArrayExpr;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;
import soot.jimple.infoflow.android.data.AndroidMethod;
import soot.jimple.infoflow.android.data.AndroidMethod.CATEGORY;
import soot.jimple.infoflow.android.data.parsers.CategorizedAndroidSourceSinkParser;

public class sceneInstr implements Extension {

    protected SootClass clsMonitor = null;
	protected SootMethod mApiTracker = null;

    protected static Options opts = new Options();

    protected static Set<CATEGORY> allcats = new HashSet<CATEGORY>();

    public final static String AndroidClassPattern = "(android|com\\.example\\.android|com\\.google|com\\.android|dalvik)\\.(.)+"; 
	public final static String OtherSDKClassPattern = "(gov\\.nist|java|javax|junit|libcore|net\\.oauth|org\\.apache|org\\.ccil|org\\.javia|" +
			"org\\.jivesoftware|org\\.json|org\\.w3c|org\\.xml|sun|com\\.adobe|com\\.svox|jp\\.co\\.omronsoft|org\\.kxml2|org\\.xmlpull)\\.(.)+";

    protected static int totalApiCalls = 0;

    protected static CategorizedAndroidSourceSinkParser catsinkparser;

    Map<String, CATEGORY> catsMap = new HashMap<String,CATEGORY>();
    
    public static void main(String args[]){
        args = preProcessArgs(opts, args);
		sceneInstr eventinstr = new sceneInstr();
		// examine catch blocks
		dua.Options.ignoreCatchBlocks = false;
		dua.Options.skipDUAAnalysis = false;
		dua.Options.modelAndroidLC = false;
		dua.Options.analyzeAndroid = true;
		
		soot.options.Options.v().set_src_prec(soot.options.Options.src_prec_apk);
		
		//output as APK, too//-f J
		soot.options.Options.v().set_output_format(soot.options.Options.output_format_dex);
		soot.options.Options.v().set_force_overwrite(true);
		
		Scene.v().addBasicClass("com.ironsource.mobilcore.BaseFlowBasedAdUnit",SootClass.SIGNATURES);
		Scene.v().addBasicClass("apiTracker.Monitor");
		
		Forensics.registerExtension(eventinstr);
		
		Forensics.main(args);
	}

    protected static String[] preProcessArgs(Options _opts, String[] args) {
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

    protected void init() {
        clsMonitor = Scene.v().getSootClass("apiTracker.Monitor");
        
        /** add our runtime monitor to application class so that it can be packed together 
         * with the instrumented code into the resulting APK package
         */
        clsMonitor.setApplicationClass();
        mApiTracker = clsMonitor.getMethodByName("apiCall");

        // get android api calls
        List<CATEGORY> ignoreCats = Arrays.asList(
            CATEGORY.ALL
        );
        List<CATEGORY> filteredCategories = Arrays.asList(CATEGORY.ALL.getDeclaringClass().getEnumConstants())
            .stream().filter(item -> ignoreCats.contains(item)).collect(Collectors.toList());
    
		allcats.addAll(filteredCategories);
        catsinkparser = 
			new CategorizedAndroidSourceSinkParser(allcats, opts.catsink, true, false);

        try {
            for (AndroidMethod am : catsinkparser.parse()) {
                catsMap.put(am.getSignature(), am.getCategory());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // System.out.println("[API-TRACKER] AllCats: " + allcats.toString());
        // System.out.println("[API-TRACKER] AllCatSrcs: " + allCatSrcs.toString());
	}
	
	public void run() {
		System.out.println("Running static analysis for sensitive apis tracking instrumentation");
		
		init();

        instrument();

        System.out.println("[API-TRACKER] Total SDK API Calls: " + String.valueOf(totalApiCalls));
	}

    public void instrument() {
        Iterator<SootClass> clsIt = Scene.v().getClasses().snapshotIterator();
        while (clsIt.hasNext()) {
            SootClass sClass = (SootClass) clsIt.next();

            if ( sClass.isPhantom() ) {	continue; }

            if (!sClass.getName().matches(AndroidClassPattern)) {
                // not SDK call
                continue;
            }

            totalApiCalls++;


            // get all SDK class methods
            List<SootMethod> methods = new ArrayList<SootMethod>();
			try {
				for (SootMethod me : sClass.getMethods()) {
					methods.add(me);
				}
			}
			catch (Exception e) {
				System.out.println("Something wrong here? for class " + sClass.getName());	
			}

            // traverse methods
            for (SootMethod sMethod : methods) {
                if ( !sMethod.isConcrete() ) {
                    // skip abstract methods and phantom methods, and native methods as well
                    continue; 
                }

                Body body = sMethod.retrieveActiveBody();
				PatchingChain<Unit> pchn = body.getUnits();
				
				Iterator<Unit> itchain = pchn.snapshotIterator();

                LocalGenerator bodyGenerator = new LocalGenerator(body);

                while (itchain.hasNext()) {
                    Unit curr = itchain.next();
					Stmt s = (Stmt) curr;
					if (!s.containsInvokeExpr()) {
						continue;
					}
					String calleename = s.getInvokeExpr().getMethod().getSignature();

                    if (catsMap.keySet().contains(calleename)) {
                        System.out.println("[API-TRACKER] Method call: " + calleename);

                        List<Stmt> methodCalls = new ArrayList<Stmt>();

                        // Value sampleString = StringConstant.v("ola mundo");
                        Value methodSignature = StringConstant.v(calleename);


                        // catch method params
                        List<Value> params = s.getInvokeExpr().getArgs();
                        
                        // transform to array of object
                        Local arrLocal = bodyGenerator.generateLocal(ArrayType.v(RefType.v("java.lang.Object"), 1));
                        NewArrayExpr arrExpr = Jimple.v().newNewArrayExpr(RefType.v("java.lang.Object"), IntConstant.v(params.size()));
                        AssignStmt assignArrToLocal = Jimple.v().newAssignStmt(arrLocal, arrExpr);
                        methodCalls.add(assignArrToLocal);
                        
                        for (int i=0;i<params.size();i++) {
                            ArrayRef idxRef = Jimple.v().newArrayRef(arrLocal, IntConstant.v(i));
                            AssignStmt assign = Jimple.v().newAssignStmt(idxRef, params.get(i));
                            methodCalls.add(assign);
                        }

                        List<Value> trackArgs = new ArrayList<>();
                        trackArgs.add(methodSignature);
                        trackArgs.add(arrLocal);
                        
                        Stmt callTracker = Jimple.v().newInvokeStmt( Jimple.v().newStaticInvokeExpr(mApiTracker.makeRef(), trackArgs) );
                        methodCalls.add(callTracker);
                        
                        // pchn.insertBefore(callTracker, s);
                        InstrumManager.v().insertAfter(pchn, methodCalls, s);

                        body.validate();
                    }
                }
            }
        }
    }

}
