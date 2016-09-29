package valera.offline.io;

public class ValeraUtil {

	public static String getCallingStack() {

		StackTraceElement[] cause = Thread.currentThread().getStackTrace();
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < cause.length; i++) {
			sb.append(cause[i]);
			sb.append("\n");
		}
		return sb.toString();
	}
	
	public static void valeraAssert(boolean condition, String msg) {
		if (condition == false) {
			System.err.println("Valera Assert Failed: " + msg);
			System.err.println(getCallingStack());
			System.exit(-1);
		}
	}
	
	public static void valeraAbort(Exception e) {
		System.err.println(e.toString());
		e.printStackTrace();
		System.exit(-1);
	}
}
