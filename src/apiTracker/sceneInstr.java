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
import soot.NullType;
import soot.PrimType;
import soot.Type;
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
import soot.jimple.NullConstant;
import soot.jimple.infoflow.android.data.AndroidMethod;
import soot.jimple.infoflow.android.data.AndroidMethod.CATEGORY;
import soot.jimple.infoflow.android.data.parsers.CategorizedAndroidSourceSinkParser;

public class sceneInstr implements Extension {

    protected SootClass clsMonitor = null;
	protected SootMethod mApiTracker = null;

    public static Options opts = new Options();

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

    public static String[] preProcessArgs(Options _opts, String[] args) {
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
        List<CATEGORY> ignoreCats = Arrays.asList();
        List<CATEGORY> filteredCategories = Arrays.asList(CATEGORY.ALL.getDeclaringClass().getEnumConstants())
            .stream().filter(item -> !ignoreCats.contains(item)).collect(Collectors.toList());
    
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

            if (sClass.getName().matches(AndroidClassPattern) || sClass.getName().matches(OtherSDKClassPattern)) {
        		// just modify methods of application classes
	            //System.out.println("[API-TRACKER] Refused class name: " + sClass.getName());
                continue;
            }

            if (sClass.getName().equals("apiTracker.Monitor") 
                || sClass.getName().equals("dynCG.Monitor")
                || sClass.getName().equals("utils.MethodEventComparator")
                || sClass.getName().equals("utils.logicClock")
                || sClass.getName().equals("intentTracker.Monitor")
                || sClass.getName().equals("eventTracker.Monitor")
                || sClass.getName().equals("covTracker.covMonitor")
            ) {
                // ignore my own monitor method
                // also ignore other monitors
                continue;
            }

	        System.out.println("[API-TRACKER] Accepted class name: " + sClass.getName());

            // get all class methods
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

		            if (s.getInvokeExpr().getMethod().getParameterCount() <= 0) {
			            // ignore method calls that have 0 args
			            continue;
		            }


                    if (!catsMap.keySet().contains(calleename)) {
			            // if not sensitive API call, ignore
			            continue;
		            }

                    List<Stmt> methodCalls = new ArrayList<Stmt>();

                    Value methodSignature = StringConstant.v(calleename);

                    int argCount = s.getInvokeExpr().getArgCount();

                    // create empty array of objects
                    Local arrLocal = bodyGenerator.generateLocal(ArrayType.v(RefType.v("java.lang.Object"), 1));
                    NewArrayExpr arrExpr = Jimple.v().newNewArrayExpr(RefType.v("java.lang.Object"), IntConstant.v(argCount));
                    AssignStmt assignArrToLocal = Jimple.v().newAssignStmt(arrLocal, arrExpr);
                    methodCalls.add(assignArrToLocal);

                    boolean valid = true;

                    for (int i=0;i<argCount;i++) {
                        ArrayRef idxRef = Jimple.v().newArrayRef(arrLocal, IntConstant.v(i));
                        Value arg = s.getInvokeExpr().getArg(i);

                        if (arg.getType() instanceof NullType) {
                            // if null, sets null
                            AssignStmt assign = Jimple.v().newAssignStmt(idxRef, NullConstant.v());
                            methodCalls.add(assign);
                        } else if (arg.getType() instanceof PrimType) {
                            // if its a primitive type, need to have a boxed class to pass it as Object param
                            RefType boxedType = ((PrimType) arg.getType()).boxedType();
                            Local boxedTypeLocal = bodyGenerator.generateLocal(boxedType);
                            
                            // create list with primitive type to search for the valueOf method
                            List<Type> primTypeList = new ArrayList<>();
                            primTypeList.add(arg.getType());

                            if (!boxedType.getSootClass().declaresMethod("valueOf", primTypeList)) {
                                // if we cant convert the primitive to a boxed value, we cant pass it to our method
                                valid = false;
                                break;
                            }

                            try {
                                // calls valueOf for this boxed class
                                AssignStmt callBoxedValueOf = Jimple.v().newAssignStmt( boxedTypeLocal, 
                                    Jimple.v().newStaticInvokeExpr(boxedType.getSootClass().getMethod("valueOf", primTypeList).makeRef(), arg) );
                                AssignStmt assign = Jimple.v().newAssignStmt(idxRef, boxedTypeLocal);
                                methodCalls.add(callBoxedValueOf);
                                methodCalls.add(assign);
                            
                                System.out.println("[API-TRACKER] [BOXING] Boxed primitive type in method " + calleename 
                                    + " (" + i + "): " + arg.getType() + " => " + boxedType);
                            } catch (Exception e) {
                                // guarantee that we wont fall on AmbiguousMethodException (not supposed to)
                                System.err.println("[API-TRACKER] [BOXING] There was an error boxing a parameter: " + e.getMessage());
                                valid = false;
                                e.printStackTrace();
                                break;
                            }
                        } else if (arg.getType() instanceof ArrayType) {
                            // if its an ArrayType, it's fine.
                            System.out.println("[API-TRACKER] [WARN] ArrayType: " + ((ArrayType) arg.getType()).baseType);
                            AssignStmt assign = Jimple.v().newAssignStmt(idxRef, arg);
                            methodCalls.add(assign);
                        } else {
                            // if its a RefType, it's fine.
                            AssignStmt assign = Jimple.v().newAssignStmt(idxRef, arg);
                            methodCalls.add(assign);
                        }
                    }

                    if (!valid) continue;

                    // create list of params to be passed to the Monitor
                    List<Value> trackArgs = new ArrayList<>();
                    trackArgs.add(methodSignature);
                    trackArgs.add(arrLocal);
                    
                    // adds the Monitor.apiCall call before the actual method call
                    Stmt callTracker = Jimple.v().newInvokeStmt( Jimple.v().newStaticInvokeExpr(mApiTracker.makeRef(), trackArgs) );
                    methodCalls.add(callTracker);
                    
                    InstrumManager.v().insertBeforeNoRedirect(pchn, methodCalls, s);

                    System.out.println("[API-TRACKER] Method call: " + calleename);

                    //body.validate();

                    totalApiCalls++;
                }
            }
        }
    }

}
