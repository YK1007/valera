package valera.offline.trace;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Vector;

class Method {
	String name;
	String signature;
	String shorty;
	long address;

	public Method(String name, String signature, long addr) {
		this.name = name;
		this.signature = signature;
		this.address = addr;
		this.shorty = Util.signatureToShorty(signature);
	}
}

class Library {
	String name;
	long baseAddr;
	Map<Long, Method> addrMap;
	Map<String, Vector<Method>> symMap;

	public Library(String name) {
		this.name = name;
		this.baseAddr = 0L;
		this.addrMap = new HashMap<Long, Method>();
		this.symMap = new HashMap<String, Vector<Method>>();
	}

	public void addMethod(Method mtd) {
		Method m = addrMap.put(mtd.address, mtd);
		assert m == null;

		String key = mtd.name + ":" + mtd.shorty;
		if (!symMap.containsKey(key)) {
			Vector<Method> vm = new Vector<Method>();
			vm.add(mtd);
			symMap.put(key, vm);
		} else {
			Vector<Method> vm = symMap.get(key);
			vm.add(mtd);
		}
	}

	public String getLibName() {
		return name;
	}

	public void setBaseAddr(long addr) {
		this.baseAddr = addr;
	}

	public Vector<Method> findInSymbolMap(String name) {
		return symMap.get(name);
	}

	public Method findInAddrMap(long addr) {
		return addrMap.get(addr);
	}
}

class TraceEntry {
	public static final int TRACE_OP_INTERP_ENTRY = 1;
	public static final int TRACE_OP_METHOD_ENTRY = 2;
	public static final int TRACE_OP_METHOD_EXIT = 3;
	public static final int TRACE_OP_NATIVE_ENTRY = 4;
	public static final int TRACE_OP_NATIVE_EXIT = 5;

	public int op, tid;
	public String clazz, method, shorty;
	public long mtdAddr, libAddr;

	public TraceEntry(int op, int tid, String clazz, String method,
			String shorty, long mtdAddr, long libAddr) {
		this.op = op;
		this.tid = tid;
		this.clazz = clazz;
		this.method = method;
		this.shorty = shorty;
		this.mtdAddr = mtdAddr;
		this.libAddr = libAddr;
	}

	public static int parseOp(String op) {
		int res = -1;
		if (op.equals("I"))
			res = TraceEntry.TRACE_OP_INTERP_ENTRY;
		else if (op.equals("E"))
			res = TraceEntry.TRACE_OP_METHOD_ENTRY;
		else if (op.equals("X"))
			res = TraceEntry.TRACE_OP_METHOD_EXIT;
		else if (op.equals("NE"))
			res = TraceEntry.TRACE_OP_NATIVE_ENTRY;
		else if (op.equals("NX"))
			res = TraceEntry.TRACE_OP_NATIVE_EXIT;
		else
			assert "Wrong Trace OpCode" == null;

		return res;
	}
}

public class DexLibSymbol {

	Vector<Library> mDexLibs = new Vector<Library>();

	public Method getMethodToken(String line) {
		String[] ss = line.split(" +");
		assert ss.length == 3;

		long addr = Long.parseLong(ss[0].substring(0, ss[0].length() - 1), 16);
		String ss1[] = ss[2].split(":");

		return new Method(ss1[0], ss1[1], addr);
	}

	public void initSymbolFile(String filename) throws Exception {
		Scanner scanner = new Scanner(new BufferedReader(new FileReader(
				filename)));
		Library curLib = null;
		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();
			if (line.startsWith("library")) {
				if (curLib != null)
					mDexLibs.add(curLib);
				String libname = line.split("=")[1];
				curLib = new Library(libname);
			} else if (Character.isDigit(line.charAt(0))) {
				Method mtd = getMethodToken(line);
				curLib.addMethod(mtd);
			} else {
				System.err.println("WARNING: Invalid symbol file");
				System.exit(-1);
			}
		}
		if (curLib != null)
			mDexLibs.add(curLib);
	}

	public void initDexFileLoading(String filename) throws Exception {
		Scanner scanner = new Scanner(new BufferedReader(new FileReader(
				filename)));
		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();
			String ss[] = line.split(" +");
			assert ss.length == 2;
			String libName = ss[0];
			long addr = Long.parseLong(ss[1].substring(2), 16);
			for (Library lib : mDexLibs) {
				if (lib.getLibName().equals(libName))
					lib.setBaseAddr(addr);
			}
		}
	}

	public Method queryMethodByAddress(long mtdAddr, long libAddr)
			throws MethodNotFoundException {
		for (Library lib : mDexLibs) {
			if (lib.baseAddr == libAddr) {
				Method m = lib.findInAddrMap(mtdAddr - libAddr - 0x24);
				if (m != null)
					return m;
			}
		}
		throw new MethodNotFoundException(String.format(
				"method address %x library address %s not found", mtdAddr,
				libAddr));
	}
	
/*
	void parseTrace(String filename) throws Exception {
		Scanner scanner = new Scanner(new BufferedReader(new FileReader(
				filename)));
		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();
			String[] ss = line.split(" +");

			int op = TraceEntry.parseOp(ss[0]);
			int tid = Integer.parseInt(ss[1]);
			String clazz = ss[2].trim();
			String mtd = ss[3].trim();
			String shorty = ss[4].trim();
			int addr1 = Integer
					.parseInt(ss[5].substring(2, ss[5].length()), 16);
			int addr2 = 0;
			if (op == TraceEntry.TRACE_OP_INTERP_ENTRY
					|| op == TraceEntry.TRACE_OP_METHOD_ENTRY
					|| op == TraceEntry.TRACE_OP_METHOD_EXIT) {
				assert ss.length == 7;
				addr2 = Integer
						.parseInt(ss[6].substring(2, ss[6].length()), 16);
			} else if (op == TraceEntry.TRACE_OP_NATIVE_ENTRY
					|| op == TraceEntry.TRACE_OP_NATIVE_EXIT) {
				assert ss.length == 6;
			}

			TraceEntry te = new TraceEntry(op, tid,
					Util.transformMethodSymbol(clazz), mtd, shorty, addr1,
					addr2);

			if (op == TraceEntry.TRACE_OP_NATIVE_ENTRY
					|| op == TraceEntry.TRACE_OP_NATIVE_EXIT)
				continue;

			boolean found = false;
			for (Library lib : mDexLibs) {
				if (lib.baseAddr == te.libAddr) {
					Method m = lib
							.findInAddrMap(te.mtdAddr - te.libAddr - 0x10);
					if (m != null) {
						System.out.println(String.format(
								"Find[%d]: %s.%s:%s in %s", te.tid, te.clazz,
								te.method, te.shorty, lib.name));
						found = true;
					}
					break;
				}
			}
			if (!found) {
				System.out.println(String.format("NotFind[%d]: %s.%s:%s.",
						te.tid, te.clazz, te.method, te.shorty));
			}
		}
	}
*/

	/*
	 * public static void main(String[] args) throws Exception {
	 * SymbolProcessing sp = new SymbolProcessing();
	 * sp.initSymbolFile("/home/yhu009/Desktop/android/trace_analysis/symbol.txt"
	 * ); sp.initDexFileLoading(
	 * "/home/yhu009/Desktop/android/trace_analysis/dexload.txt");
	 * sp.parseTrace("/home/yhu009/Desktop/android/trace_analysis/trace"); }
	 */

}
