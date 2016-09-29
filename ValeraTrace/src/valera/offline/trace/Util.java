package valera.offline.trace;

public class Util {
	
	// Ljava/lang/Thread; => java.lang.Thread
	public static String transformMethodSymbol(String s) {
		assert s.charAt(0) == 'L' && s.charAt(s.length()-1) == ';';
		StringBuilder sb = new StringBuilder();
		for (int i = 1; i < s.length()-1; i++) {
			char ch = s.charAt(i);
			if (ch == '/' || ch == '$')
				ch = '.';
			sb.append(ch);
		}
		return sb.toString();
	}
	
	// (Ljava/lang/Thread;)V => VL
	public static String signatureToShorty(String sig) {
		StringBuilder arg = new StringBuilder();
		StringBuilder ret = new StringBuilder();
		int lp = sig.indexOf('(');
		int rp = sig.indexOf(')');
		int i;

		assert lp >= 0 && rp >= 0;
		
		// parse arguments
		for (i = lp; i <= rp; i++) {
			char ch = sig.charAt(i);
			if (ch == '(' || ch == ')') {
				continue;
			} else if (ch == 'L') {
				arg.append('L');
				while (sig.charAt(i) != ';') i++;
			} else if (ch == '[') {
				arg.append('L');
				while (sig.charAt(i) == '[') i++;
				if (sig.charAt(i) == 'L')
					while (sig.charAt(i) != ';') i++;
			} else {
				arg.append(ch);
			}
		}

		// parse return type
		char ch = sig.charAt(i);
		if (ch == 'L' || ch == '[')
			ret.append('L');
		else
			ret.append(ch);
		
		return ret.toString() + arg.toString();
	}

}
