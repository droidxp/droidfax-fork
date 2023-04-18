package apiTracker;

public class Monitor {

    private static boolean active = false;

    public static void apiCall (String methodSignature, Object[] params) {
		//if (active) return;
		active = true;
		//try { 
        apiCall_impl(methodSignature, params); 
        //}
		//finally { active = false; }
	}

	private static void apiCall_impl(String methodSignature, Object[] params) {
		try {
			String args = "";
			for (Object param : params) {
                if (param == null) {
                    args += "null" + ", ";
                } else {
                    args += "\"" + param.toString().replaceAll("\"", "\\\\\"") + "\", ";
                }
			}

            if (args.length() >= 2) {
                args = args.substring(0, args.length() - 2);
            }

			android.util.Log.i("apicall-monitor", "[API-TRACK]: " + methodSignature + " => " + args);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
    
}
