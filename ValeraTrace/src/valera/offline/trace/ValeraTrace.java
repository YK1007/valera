package valera.offline.trace;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Stack;
import java.util.TreeMap;
import java.util.Vector;

import valera.offline.io.ValeraUtil;

class MsgPostRelation {
	// Type1: external event (Looper[i]) -> Looper[i].
	// Type2: internal event (Looper[i]) -> Looper[i].
	// Type3: background thread -> Looper[i].
	// Type4: internal event (Looper[j]) -> Looper[i].
	// Type5: external event (Looper[j]) -> Looper[i].
	// Type6: binder thread -> Looper[i].
	// Type99: unknown.
	final static int TYPE_1_EXTERNAL_SAME_LOOPER = 1;
	final static int TYPE_2_INTERNAL_SAME_LOOPER = 2;
	final static int TYPE_3_BGK_THREAD_TO_LOOPER = 3;
	final static int TYPE_4_INTERNAL_TWO_LOOPER  = 4;
	final static int TYPE_5_EXTERNAL_TWO_LOOPER  = 5;
	final static int TYPE_6_BINDER_TO_LOOPER	 = 6;
	final static int TYPE_UNKNOWN				 = 99;
	
	int type;
	int tidFrom;
	int tidTo;
	int msgIdFrom;
	
	MsgPostRelation() {
		type = tidFrom = tidTo = msgIdFrom = -1;
	}
	
	static String TypeToStr(int type) {
		switch (type) {
		case TYPE_1_EXTERNAL_SAME_LOOPER:
			return "TYPE_1_EXTERNAL_SAME_LOOPER";
		case TYPE_2_INTERNAL_SAME_LOOPER:
			return "TYPE_2_INTERNAL_SAME_LOOPER";
		case TYPE_3_BGK_THREAD_TO_LOOPER:
			return "TYPE_3_BGK_THREAD_TO_LOOPER";
		case TYPE_4_INTERNAL_TWO_LOOPER:
			return "TYPE_4_INTERNAL_TWO_LOOPER";
		case TYPE_5_EXTERNAL_TWO_LOOPER:
			return "TYPE_5_EXTERNAL_TWO_LOOPER";
		case TYPE_6_BINDER_TO_LOOPER:
			return "TYPE_6_BINDER_TO_LOOPER";
		case TYPE_UNKNOWN:
			return "TYPE_UNKNOWN";
		default:
			return "N/A";
		}
	}
}

class Message {
	public static final int MSG_NIL = -1;
	public static final int FLAG_INTERNAL_MSG	= 0;
	public static final int FLAG_INPUT_EVENTS	= 1;
	public static final int FLAG_VSYNC			= 2;
	public static final int FLAG_IMM_EVENTS		= 3;
	public static final int FLAG_SENSOR_EVENTS	= 4;
	
	int msgId;
	int dispatcherType; // 1 - handleMessage; 2 - Runnable.
	String dispatcher;
	int handlerId;
	int handlerCnt;
	String callback;
	int what, arg1, arg2;
	String obj;
	String callingStack;
	MsgPostRelation relation;
	boolean isTracing;
	boolean isAsync;
	int flag;
	ArrayList<TraceInstruction> insns = new ArrayList<TraceInstruction>();

	Message(int msgId, int dispatcherType, String dispatcher, int handlerId,
			String callback, int what, int arg1, int arg2, String obj, String callingStack) {
		this.msgId = msgId;
		this.dispatcherType = dispatcherType;
		this.dispatcher = dispatcher;
		this.handlerId = handlerId;
		this.callback = callback;
		this.what = what;
		this.arg1 = arg1;
		this.arg2 = arg2;
		this.obj = obj;
		this.callingStack = callingStack;
		
		this.handlerCnt = 0;
		this.relation = null;
		this.isTracing = false;
		this.isAsync = false;
		this.flag = FLAG_INTERNAL_MSG;
	}
	
	Message(int msgId, int flag) {
		this.msgId = msgId;
		this.flag = flag;
	}
	
	public static String FlagToStr(int flag) {
		switch (flag) {
		case FLAG_INTERNAL_MSG:
			return "INTERNAL_MSG";
		case FLAG_INPUT_EVENTS:
			return "INPUT_EVENTS";
		case FLAG_VSYNC:
			return "VSYNC";
		case FLAG_IMM_EVENTS:
			return "IMM";
		case FLAG_SENSOR_EVENTS:
			return "SENSOR";
		default:
			return "UNKNOWN";
		}
	}
}

class TraceTag {
	public static final int VALERA_TAG_INTERP_ENTRY		= 1;
	public static final int VALERA_TAG_INTERP_EXIT		= 2;
	public static final int VALERA_TAG_METHOD_ENTRY		= 3;
	public static final int VALERA_TAG_METHOD_EXIT		= 4;
	public static final int VALERA_TAG_NATIVE_ENTRY		= 5;
	public static final int VALERA_TAG_NATIVE_EXIT		= 6;
	public static final int VALERA_TAG_FORK_THREAD		= 7;
	public static final int VALERA_TAG_ATTACH_Q			= 8;
	public static final int VALERA_TAG_POST_MESSAGE		= 9;
	public static final int VALERA_TAG_ACTION_BEGIN		= 10;
	public static final int VALERA_TAG_ACTION_END		= 11;
	public static final int VALERA_TAG_INPUTEVENT_BEGIN = 12;
	public static final int VALERA_TAG_INPUTEVENT_END 	= 13;
	public static final int VALERA_TAG_BINDER_BEGIN		= 14;
	public static final int VALERA_TAG_BINDER_END		= 15;
	public static final int VALERA_TAG_OBJECT_RW		= 16;
	public static final int VALERA_TAG_OBJECT_RW_QUICK	= 17;
	public static final int VALERA_TAG_STATIC_RW		= 18;
	public static final int VALERA_TAG_PACKED_SWITCH	= 19;
	public static final int VALERA_TAG_SPARSE_SWITCH	= 20;
	public static final int VALERA_TAG_IFTEST			= 21;
	public static final int VALERA_TAG_IFTESTZ			= 22;
	public static final int VALERA_TAG_LIFECYCLE		= 23;
	public static final int VALERA_TAG_VSYNC_BEGIN		= 24;
	public static final int VALERA_TAG_VSYNC_END		= 25;
	public static final int VALERA_TAG_DEBUG_PRINT		= 98;
	public static final int VALERA_TAG_SYSTEM_EXIT		= 99;
}

class ActionPostMsg {
	int tidFrom, tidTo, msgId;
	int actionId; // indicate this msg is post from looper.
}

abstract class Thread {
	int tid; // thread id
	int qid; // queue id
	String name;
	//ArrayList<TraceInstruction> insns;

	public Thread(int tid, String name) {
		this.tid = tid;
		this.qid = 0;
		this.name = name;
		//this.insns = new ArrayList<TraceInstruction>();
	}

	public void attachQ(int qid) {
		this.qid = qid;
	}

	public boolean isLooper() {
		return qid != 0;
	}
}

class LooperThread extends Thread {
	
	static class UnknownEvent {
		int lastMsgId;
		ArrayList<TraceInstruction> insns = new ArrayList<TraceInstruction>();
	}
	
	// The instructions that executed before first action.
	ArrayList<TraceInstruction> insns = new ArrayList<TraceInstruction>();
	
	// The insns executed on looper, but not in an action (excluding the insns
	// before the first action).
	ArrayList<UnknownEvent> unknown = new ArrayList<UnknownEvent>();
	
	ArrayList<Message> events = new ArrayList<Message>();
	int curActionId = Message.MSG_NIL; // current msgId the looper is executing.
	int lastActionId = Message.MSG_NIL;
	int numActionExecuted = 0;

	public LooperThread(int tid, String name) {
		super(tid, name);
	}
}

class BackgroundThread extends Thread {
	ArrayList<TraceInstruction> insns = new ArrayList<TraceInstruction>();

	public BackgroundThread(int tid, String name) {
		super(tid, name);
	}
}

class BinderThread extends Thread {
	ArrayList<TraceInstruction> insns = new ArrayList<TraceInstruction>();

	public BinderThread(int tid, String name) {
		super(tid, name);
	}
	
}

abstract class TraceInstruction {
	int tag, tid;
	PrintStream printer;
	
	TraceInstruction(int tag, int tid) {
		this.tag = tag;
		this.tid = tid;
		this.printer = System.out;
	}
	
	public void setPrintStream(PrintStream ps) {
		this.printer = ps;
	}
	
	protected void printIndent(int indent) {
		for (int i = 0; i < indent; i++)
			printer.print(' ');
	}
	
	abstract void print(int indent);
}

class TraceMethod extends TraceInstruction {
	static final int METHOD_ENTER = 1;
	static final int METHOD_EXIT = 2;
	String className;
	String mtdName;
	String shorty;
	boolean isNative;
	int type;
	
	TraceMethod(int tag, int tid, String className, String mtdName, String shorty, boolean isNative, int type) {
		super(tag, tid);
		this.className = className;
		this.mtdName = mtdName;
		this.shorty = shorty;
		this.isNative = isNative;
		this.type = type;
	}

	@Override
	void print(int indent) {
		printIndent(indent);
		if (type == METHOD_ENTER)
			printer.print(">>>>> ");
		else if (type == METHOD_EXIT)
			printer.print("<<<<< ");
		printer.printf("tid=%d class=%s method=%s shorty=%s isnative=%b\n",
				tid, className, mtdName, shorty, isNative);
	}
}

class TraceMemoryRW extends TraceInstruction {
	static final int MEMORY_READ = 1;
	static final int MEMORY_WRITE = 2;
	int pc;
	String className;
	String fieldName;
	long objAddr;
	int fieldIdx;
	int rw;
	boolean isStatic;
	
	TraceMemoryRW(int tag, int tid, int pc, String className, String fieldName, long objAddr, 
			int fieldIdx, int rw, boolean isStatic) {
		super(tag, tid);
		this.pc = pc;
		this.className = className;
		this.fieldName = fieldName;
		this.objAddr = objAddr;
		this.fieldIdx = fieldIdx;
		this.rw = rw;
		this.isStatic = isStatic;
	}

	@Override
	void print(int indent) {
		printIndent(indent);
		char ch = rw == MEMORY_READ ? 'R' : 'W';
		printer.printf("rw=%c tid=%d pc=%d class=%s fieldName=%s obj=%s fieldIdx=%d static=%b\n",
				ch, tid, pc, className, fieldName, Long.toHexString(objAddr), fieldIdx, isStatic);
	}
}

class TraceSwitch extends TraceInstruction {
	static final int PACKED_SWITCH = 1;
	static final int SPARSE_SWITCH = 2;
	int pc;
	int offset;
	int type;
	
	TraceSwitch(int tag, int tid, int pc, int offset, int type){
		super(tag, tid);
		this.pc = pc;
		this.offset = offset;
		this.type = type;
	}

	@Override
	void print(int indent) {
		printIndent(indent);
		printer.printf("switch: tid=%d pc=%s offset=%d\n",
				tid, Integer.toHexString(pc), offset);
	}
}

class TraceIfTest extends TraceInstruction {
	static final int IFTEST = 1;
	static final int IFZTEST = 2;
	int pc;
	boolean taken;
	int type;
	
	TraceIfTest(int tag, int tid, int pc, boolean taken, int type) {
		super(tag, tid);
		this.pc = pc;
		this.taken = taken;
		this.type = type;
	}

	@Override
	void print(int indent) {
		printIndent(indent);
		String ch = type == IFZTEST ? "z" : "";
		printer.printf("if%s: tid=%d pc=%s taken=%b\n",
				ch, tid, Integer.toHexString(pc), taken);
	}
}

class TraceForkThread extends TraceInstruction {
	int tidChild;
	
	TraceForkThread(int tag, int tid, int tidChild) {
		super(tag, tid);
		this.tidChild = tidChild;
	}

	@Override
	void print(int indent) {
		printIndent(indent);
		printer.printf("fork_thread: tid=%d new_thrd=%d\n",
				tid, tidChild);
	}
}

class TraceAttachQ extends TraceInstruction {
	int qid;

	TraceAttachQ(int tag, int tid, int qid) {
		super(tag, tid);
		this.qid = qid;
	}

	@Override
	void print(int indent) {
		printIndent(indent);
		printer.printf("attachQ: tid=%d qid=%d\n",
				tid, qid);
	}
	
}

class TracePostMessage extends TraceInstruction {
	int msgId, fromTid, toTid;

	TracePostMessage(int tag, int fromTid, int toTid, int msgId) {
		super(tag, fromTid);
		this.fromTid = fromTid;
		this.toTid = toTid;
		this.msgId = msgId;
	}

	@Override
	void print(int indent) {
		printIndent(indent);
		printer.printf("post_msg: tid1=%d tid2=%d msgId=%d\n",
				fromTid, toTid, msgId);
	}
}

class TraceActionBegin extends TraceInstruction {
	int msgId;
	
	TraceActionBegin(int tag, int tid, int msgId) {
		super(tag, tid);
		this.msgId = msgId;
	}

	@Override
	void print(int indent) {
		printIndent(indent);
		printer.printf("action_begin: tid=%d msgId=%d\n",
				tid, msgId);
	}
}

class TraceActionEnd extends TraceInstruction {
	int msgId;
	
	TraceActionEnd(int tag, int tid, int msgId) {
		super(tag, tid);
		this.msgId = msgId;
	}

	@Override
	void print(int indent) {
		printIndent(indent);
		printer.printf("action_end: tid=%d msgId=%d\n",
				tid, msgId);
	}
}

class ActivityLifecycle {
	public static final int ON_INIT  = 0;
	public static final int ON_START = 1;
	public static final int ON_STOP  = 2;
	
	String activityName;
	long totalTime;
	int state; // ON_START | ON_STOP;
	long timestamp;
	
	public ActivityLifecycle(String activityName) {
		this.activityName = activityName;
		this.totalTime = 0;
		this.state = ON_INIT;
		this.timestamp = 0;
	}
}

public class ValeraTrace {
	
	public final static int PRINT_LEVEL_NONE = 0;
	public final static int PRINT_LEVEL_SHORT = 1;
	public final static int PRINT_LEVEL_DETAIL = 2;
	
	public final static int WAKEUP_AND_CHECK	= 100;
	public final static int REPLAY_INPUT_EVENT	= 101;
	public final static int REPLAY_IMM_EVENT	= 102;
	public final static int REPLAY_SENSOR_EVENT	= 103;
	
	
	private String filename;
	// tid -> thread
	private Map<Integer, Thread> mThrdMap = new TreeMap<Integer, Thread>();
	// looper hashcode -> tid
	private Map<Integer, Integer> mThrdQueueMap = new TreeMap<Integer, Integer>();
	// msgId -> msg
	private Map<Integer, Message> mMsgMap = new TreeMap<Integer, Message>();
	private LinkedList<Integer> mExeOrder = new LinkedList<Integer>();
	private HashMap<String, ActivityLifecycle> mActivityLifecycle = new HashMap<String, ActivityLifecycle>();
	

	public ValeraTrace(String filename) {
		this.filename = filename;
		Thread mainThrd = new LooperThread(1, "main");
		Thread binderThrd = new BinderThread(0, "binder");
		mThrdMap.put(mainThrd.tid, mainThrd);
		mThrdMap.put(binderThrd.tid, binderThrd);
	}
	
	private void addTraceInstrToThread(Thread thrd, TraceInstruction ins) {
		int tid = thrd.tid;
		
		if (thrd.isLooper()) {
			ValeraUtil.valeraAssert(thrd instanceof LooperThread, "Should be a looper thread.");
			LooperThread looper = (LooperThread) thrd;
			int curActionId = looper.curActionId;
			// Looper can execute non-action code only before the first action is executed.
			if (looper.numActionExecuted == 0) {
				looper.insns.add(ins);
			} else if (curActionId != Message.MSG_NIL) {
				Message msg = mMsgMap.get(curActionId);
				ValeraUtil.valeraAssert(msg != null, "Action not found " + curActionId);
				msg.insns.add(ins);
			} else {
				if (looper.unknown.size() == 0) {
					LooperThread.UnknownEvent ue = new LooperThread.UnknownEvent();
					ue.lastMsgId = looper.lastActionId;
					ue.insns.add(ins);
					looper.unknown.add(ue);
				} else {
					LooperThread.UnknownEvent ue = looper.unknown.get(looper.unknown.size() - 1);
					if (ue.lastMsgId == looper.lastActionId) {
						ue.insns.add(ins);
					} else {
						ue = new LooperThread.UnknownEvent();
						ue.lastMsgId = looper.lastActionId;
						ue.insns.add(ins);
						looper.unknown.add(ue);
					}
				}
			}
		} else {
			if (thrd instanceof BackgroundThread) {
				BackgroundThread bt = (BackgroundThread) thrd;
				bt.insns.add(ins);
			} else if (thrd instanceof BinderThread) {
				BinderThread bt = (BinderThread) thrd;
				bt.insns.add(ins);
			} else {
				ValeraUtil.valeraAssert(false, "Non-looper thread can only be background or binder thread.");
			}
		}
	}
	
	private void addExternalEventToMainThread(int tid, Message msg) {
		Thread thrd = mThrdMap.get(tid);
		ValeraUtil.valeraAssert(thrd instanceof LooperThread && thrd.tid == 1, 
				"Input event can only executed on main thread");
		LooperThread main = (LooperThread) thrd;
		main.events.add(msg);
	}
	
	private void setCurActionIdOnLooperThread(int tid, int msgId) {
		Thread thrd = mThrdMap.get(tid);
		ValeraUtil.valeraAssert(thrd.isLooper() && thrd instanceof LooperThread, 
				"Action cannot occur on non-looper thread");
		LooperThread lthrd = (LooperThread) thrd;
		if (msgId == Message.MSG_NIL)
			lthrd.lastActionId = lthrd.curActionId;
		lthrd.curActionId = msgId;
		lthrd.numActionExecuted++;
	}

	public void parse(int printLevel) throws FileNotFoundException {
		Scanner scanner = new Scanner(new BufferedInputStream(new FileInputStream(filename)));
		
		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();
			//linenum++;
			String[] tk = line.split(" +");
			int action = Integer.parseInt(tk[0].trim());
			
			switch (action) {
			case TraceTag.VALERA_TAG_INTERP_ENTRY:
			case TraceTag.VALERA_TAG_INTERP_EXIT:
			case TraceTag.VALERA_TAG_METHOD_ENTRY:
			case TraceTag.VALERA_TAG_METHOD_EXIT:
			case TraceTag.VALERA_TAG_NATIVE_ENTRY:
			case TraceTag.VALERA_TAG_NATIVE_EXIT:
			{
				ValeraUtil.valeraAssert(tk.length == 5, "INTERP_ENTRY FORMAT ERROR: " + line);
				int tid = Integer.parseInt(tk[1].trim());
				String clazzName = tk[2].trim();
				String mtdName = tk[3].trim();
				String shorty = tk[4].trim();
				
				Thread thrd = mThrdMap.get(tid);
				ValeraUtil.valeraAssert(thrd != null, "Cannot find tid " + tid);
				
				TraceMethod mtd = null;
				switch (action) {
				case TraceTag.VALERA_TAG_INTERP_ENTRY:
					mtd = new TraceMethod(action, tid, clazzName, mtdName, shorty, false, TraceMethod.METHOD_ENTER);
					break;
				case TraceTag.VALERA_TAG_INTERP_EXIT:
					mtd = new TraceMethod(action, tid, clazzName, mtdName, shorty, false, TraceMethod.METHOD_EXIT);
					break;
				case TraceTag.VALERA_TAG_METHOD_ENTRY:
					mtd = new TraceMethod(action, tid, clazzName, mtdName, shorty, false, TraceMethod.METHOD_ENTER);
					break;
				case TraceTag.VALERA_TAG_METHOD_EXIT:
					mtd = new TraceMethod(action, tid, clazzName, mtdName, shorty, false, TraceMethod.METHOD_EXIT);
					break;
				case TraceTag.VALERA_TAG_NATIVE_ENTRY:
					mtd = new TraceMethod(action, tid, clazzName, mtdName, shorty, true, TraceMethod.METHOD_ENTER);
					break;
				case TraceTag.VALERA_TAG_NATIVE_EXIT:
					mtd = new TraceMethod(action, tid, clazzName, mtdName, shorty, true, TraceMethod.METHOD_EXIT);
					break;
				default:
					ValeraUtil.valeraAssert(false, "Should not reach here!");
				}
				
				addTraceInstrToThread(thrd, mtd);

			}
			break;
			case TraceTag.VALERA_TAG_FORK_THREAD:
			{
				ValeraUtil.valeraAssert(tk.length == 5, "FORK_THREAD FORMAT ERROR: " + line);
				int tid1 = Integer.parseInt(tk[1].trim());
				String name1 = tk[2].trim();
				int tid2 = Integer.parseInt(tk[3].trim());
				String name2 = tk[4].trim();
				
				Thread thrd1 = mThrdMap.get(tid1);
				ValeraUtil.valeraAssert(thrd1 != null, "Null thread in thread map " + tid1);
				Thread thrd2 = new BackgroundThread(tid2, name2);
				mThrdMap.put(thrd2.tid, thrd2);
				
				TraceForkThread fork = new TraceForkThread(action, tid1, tid2);
				addTraceInstrToThread(thrd1, fork);
			}
			break;
			case TraceTag.VALERA_TAG_ATTACH_Q:
			{
				ValeraUtil.valeraAssert(tk.length == 3, "ATTACH_Q FORMAT ERROR: " + line);
				int tid = Integer.parseInt(tk[1].trim());
				int hashCode = Integer.parseInt(tk[2].trim());
				
				// Change thread from background thread to looper thread.
				Thread thrd = mThrdMap.get(tid);
				ValeraUtil.valeraAssert(thrd != null, "Invalid tid " + tid);
				thrd = new LooperThread(tid, thrd.name);
				thrd.attachQ(hashCode);
				mThrdMap.put(tid, thrd);
				
				ValeraUtil.valeraAssert(mThrdQueueMap.containsKey(hashCode) == false, 
						"Queue hashcode " + hashCode + " already exist.");
				mThrdQueueMap.put(hashCode, tid);
				
				//TraceAttachQ aq = new TraceAttachQ(action, tid, hashCode);
			}
			break;
			case TraceTag.VALERA_TAG_POST_MESSAGE:
			{
				ValeraUtil.valeraAssert(tk.length == 12, "POST_MESSAGE FORMAT ERROR: " + line);
				int tid = Integer.parseInt(tk[1].trim());
				long relativeTime = Long.parseLong(tk[2].trim());
				int hashQ = Integer.parseInt(tk[3].trim());
				int msgId = Integer.parseInt(tk[4].trim());
				String[] ss = tk[5].trim().split(":");
				String dispatcher = ss[0].trim();
				int handlerId = Integer.parseInt(ss[1].trim());
				String callback = tk[6].trim().equals("null") ? null : tk[5].trim().split("@")[0];
				int what = Integer.parseInt(tk[7].trim());
				int arg1 = Integer.parseInt(tk[8].trim());
				int arg2 = Integer.parseInt(tk[9].trim());
				String obj = tk[10].trim().equals("null") ? null : tk[9].trim();
				String callStack = tk[11].trim();
				
				Thread thrd = mThrdMap.get(tid);
				ValeraUtil.valeraAssert(thrd != null, "Null thread in thread map " + tid);
				
				Message msg = mMsgMap.get(msgId);
				ValeraUtil.valeraAssert(msg == null, "Message " + msgId + " already exist");
				int type = callback == null ? 1 : 2;
				msg = new Message(msgId, type, dispatcher, handlerId, callback, what, arg1, arg2, obj, callStack);
				mMsgMap.put(msgId, msg);
				
				switch (printLevel) {
					case PRINT_LEVEL_SHORT: {
						System.out.printf("POST_MSG time=%d\n", relativeTime);
					} break;
					case PRINT_LEVEL_DETAIL: {
						System.out.printf("POST_MSG msgId=%d reltime=%d tid=%d handler=%s handlerId=%d callback=%s what=%d arg1=%d arg2=%d obj=%s callstack=%s\n", 
								msgId, relativeTime, thrd.tid, dispatcher, handlerId, callback, what, arg1, arg2, obj, callStack);
					} break;
				}
				
				Thread thrdFrom = thrd;
				ValeraUtil.valeraAssert(mThrdQueueMap.containsKey(hashQ), "Hashcode " + hashQ + " does not exist.");
				int tidTo = mThrdQueueMap.get(hashQ);
				ValeraUtil.valeraAssert(mThrdMap.containsKey(tidTo), "Post message toThrd not exist " + tidTo);
				Thread thrdTo = mThrdMap.get(tidTo);
				
				MsgPostRelation mpr = new MsgPostRelation();
				mpr.tidFrom = thrdFrom.tid;
				mpr.tidTo = thrdTo.tid;
				msg.relation = mpr;
				
				// Poster is from a looper thread.
				if (thrdFrom.isLooper()) {
					// post from inside an action.
					LooperThread looper = (LooperThread) thrdFrom;
					if (looper.curActionId != Message.MSG_NIL) {
						int actionId = looper.curActionId;
						Message m = mMsgMap.get(actionId);
						ValeraUtil.valeraAssert(m != null, "Message " + actionId + " not found.");
						mpr.msgIdFrom = actionId;
						
						// internal action
						if (m.flag == Message.FLAG_INTERNAL_MSG) {
							if (thrdFrom.qid == thrdTo.qid) {
								// type 2: internal event same looper
								mpr.type = MsgPostRelation.TYPE_2_INTERNAL_SAME_LOOPER;
							} else {
								// type 4: internal event different looper
								mpr.type = MsgPostRelation.TYPE_4_INTERNAL_TWO_LOOPER;
							}
						} else { // external event
							if (thrdFrom.qid == thrdTo.qid) {
								// type 1: external event same looper
								mpr.type = MsgPostRelation.TYPE_1_EXTERNAL_SAME_LOOPER;
							} else {
								// type 5: external event different looper
								mpr.type = MsgPostRelation.TYPE_5_EXTERNAL_TWO_LOOPER;
							}
						}
					} else { // unknown source of post.
						mpr.type = MsgPostRelation.TYPE_UNKNOWN;
						if (printLevel != PRINT_LEVEL_NONE) {
							System.out.printf("WARNING: msg %d post from looper thread but from unknown action.\n", msgId);
						}
					}
				} else {
					// binder thread
					if (thrdFrom.tid == 0)
						mpr.type = MsgPostRelation.TYPE_6_BINDER_TO_LOOPER;
					else
					// background thread
						mpr.type = MsgPostRelation.TYPE_3_BGK_THREAD_TO_LOOPER;
				}
				
				TracePostMessage pm = new TracePostMessage(action, thrdFrom.tid, thrdTo.tid, msgId);
				this.addTraceInstrToThread(thrdFrom, pm);
			}
			break;
			case TraceTag.VALERA_TAG_ACTION_BEGIN:
			{
				ValeraUtil.valeraAssert(tk.length == 7, "ACTION_BEGIN FORMAT ERROR: " + line);
				int tid = Integer.parseInt(tk[1].trim());
				int msgId = Integer.parseInt(tk[2].trim());
				long relativeTime = Long.parseLong(tk[3].trim());
				int hCnt = Integer.parseInt(tk[4].trim());
				int isAsync = Integer.parseInt(tk[5].trim());
				int isTrace = Integer.parseInt(tk[6].trim());
				
				Thread thrd = mThrdMap.get(tid);
				ValeraUtil.valeraAssert(thrd != null, "Null thread in thread map " + tid);
				Message msg = mMsgMap.get(msgId);
				ValeraUtil.valeraAssert(msg != null, "Null message in message map " + msgId);
				msg.handlerCnt = hCnt;
				msg.isTracing = isTrace > 0 ? true : false;
				msg.isAsync = isAsync != 0 ? true : false;
				
				mExeOrder.add(msgId);
				
				setCurActionIdOnLooperThread(tid, msgId);
								
				if (printLevel != PRINT_LEVEL_NONE) {
					MsgPostRelation r = msg.relation;
					switch (r.type) {
					case MsgPostRelation.TYPE_1_EXTERNAL_SAME_LOOPER:
					case MsgPostRelation.TYPE_2_INTERNAL_SAME_LOOPER:
					case MsgPostRelation.TYPE_4_INTERNAL_TWO_LOOPER:
					case MsgPostRelation.TYPE_5_EXTERNAL_TWO_LOOPER:
					{
						Message m = mMsgMap.get(r.msgIdFrom);
						ValeraUtil.valeraAssert(m != null, "Cannot find msg " + r.msgIdFrom);
						StringBuilder sb = new StringBuilder();
						
						sb.append(Message.FlagToStr(m.flag));
						if (m.flag == Message.FLAG_INTERNAL_MSG) {
							sb.append(" " + m.msgId);
							sb.append(" " + m.dispatcher);
							if (m.dispatcherType == 1) {
								sb.append(" " + m.what);
								sb.append(" " + m.arg1);
								sb.append(" " + m.arg2);
								sb.append(" " + m.obj);
							} else {
								sb.append(" " + m.callback);
							}
						}
						//System.out.printf("MSG_POST_TYPE: %s %s\n", MsgPostRelation.TypeToStr(r.type), sb);
					}
						break;
						
					case MsgPostRelation.TYPE_3_BGK_THREAD_TO_LOOPER:
					case MsgPostRelation.TYPE_6_BINDER_TO_LOOPER:
					{
						//System.out.printf("MSG_POST_TYPE: %s\n", MsgPostRelation.TypeToStr(r.type));
					}
						break;
					}
					
					switch (printLevel) {
						case PRINT_LEVEL_SHORT: {
							System.out.printf("ACTION_BEGIN time=%d type=%s\n", relativeTime, 
									MsgPostRelation.TypeToStr(msg.relation.type));
						} break;
						case PRINT_LEVEL_DETAIL: {
							System.out.printf("ACTION_BEGIN msgId=%d reltime=%d tid=%d hash=%s handler=%s handlerId=%d handlerCnt=%d "
									+ "callback=%s what=%d arg1=%d arg2=%d obj=%s isAsync=%d isTrace=%d postType=%s\n", 
								msgId, relativeTime, thrd.tid, Integer.toHexString(thrd.qid), msg.dispatcher, 
								msg.handlerId, hCnt, msg.callback, msg.what, msg.arg1, msg.arg2, msg.obj, 
								isAsync, isTrace, MsgPostRelation.TypeToStr(msg.relation.type));
						} break;
					}
				}
				
				ValeraUtil.valeraAssert(thrd instanceof LooperThread, "Message can only executed on looper thread");
				LooperThread looper = (LooperThread) thrd;
				looper.events.add(msg);
			}
			break;
			case TraceTag.VALERA_TAG_ACTION_END:
			{
				ValeraUtil.valeraAssert(tk.length == 7, "ACTION_END FORMAT ERROR: " + line);
				int tid = Integer.parseInt(tk[1].trim());
				int msgId = Integer.parseInt(tk[2].trim());
				long relativeTime = Long.parseLong(tk[3].trim());
				int hCnt = Integer.parseInt(tk[4].trim());
				int isAsync = Integer.parseInt(tk[5].trim());
				int isTrace = Integer.parseInt(tk[6].trim());
				
				Thread thrd = mThrdMap.get(tid);
				ValeraUtil.valeraAssert(thrd != null, "Null thread in thread map " + tid);
				
				ValeraUtil.valeraAssert(thrd.isLooper(), "Action cannot occur on non-looper thread");
				setCurActionIdOnLooperThread(tid, Message.MSG_NIL);
				
				//if (doPrint) {
				//	System.out.printf("ACTION_END msgId=%d reltime=%d tid=%d\n", msgId, relativeTime, thrd.tid);
				//}
			}
			break;
			case TraceTag.VALERA_TAG_INPUTEVENT_BEGIN:
			{
				ValeraUtil.valeraAssert(tk.length == 6, "INPUTEVENT_BEGIN FORMAT ERROR: " + line);
				int tid = Integer.parseInt(tk[1].trim());
				int msgId = Integer.parseInt(tk[2].trim());
				long relativeTime = Long.parseLong(tk[3].trim());
				String eventType = tk[4].trim();
				String actionType = tk[5].trim();
				
				setCurActionIdOnLooperThread(tid, msgId);
				
				Message msg = mMsgMap.get(msgId);
				ValeraUtil.valeraAssert(msg == null, "Message " + msgId + " already exist");
				msg = new Message(msgId, Message.FLAG_INPUT_EVENTS);
				mMsgMap.put(msgId, msg);
				
				switch (printLevel) {
				case PRINT_LEVEL_SHORT: {
					System.out.printf("INPUTEVENT_BEGIN time=%d\n", 
							msgId, relativeTime);
				} break;
				case PRINT_LEVEL_DETAIL: {
					System.out.printf("INPUTEVENT_BEGIN msgId=%d reltime=%d tid=%d event=%s action=%s\n", 
							msgId, relativeTime, tid, eventType, actionType);
				} break;
			}
				
				addExternalEventToMainThread(tid, msg);
			}
			break;
			case TraceTag.VALERA_TAG_INPUTEVENT_END:
			{
				ValeraUtil.valeraAssert(tk.length == 6, "INPUTEVENT_END FORMAT ERROR: " + line);
				int tid = Integer.parseInt(tk[1].trim());
				int msgId = Integer.parseInt(tk[2].trim());
				long relativeTime = Long.parseLong(tk[3].trim());
				String eventType = tk[4].trim();
				String actionType = tk[5].trim();
				
				setCurActionIdOnLooperThread(tid, Message.MSG_NIL);
				
				//if (doPrint) {
				//	System.out.printf("INPUTEVENT_END msgId=%d tid=%d reltime=%d event=%s action=%s\n", 
				//		msgId, relativeTime, tid, eventType, actionType);
				//}
			}
			break;
			case TraceTag.VALERA_TAG_VSYNC_BEGIN:
			{
				ValeraUtil.valeraAssert(tk.length == 3, "VSYNC_BEGIN FORMAT ERROR: " + line);
				int tid = Integer.parseInt(tk[1].trim());
				int msgId = Integer.parseInt(tk[2].trim());
				
				setCurActionIdOnLooperThread(tid, msgId);
				
				Message msg = mMsgMap.get(msgId);
				ValeraUtil.valeraAssert(msg == null, "Message " + msgId + " already exist");
				msg = new Message(msgId, Message.FLAG_VSYNC);
				mMsgMap.put(msgId, msg);
				
				if (printLevel == PRINT_LEVEL_DETAIL) {
					System.out.printf("VSYNC_BEGIN msgId=%d tid=%d\n", 
						msgId, tid);
				}
				
				addExternalEventToMainThread(tid, msg);
			}
			break;
			case TraceTag.VALERA_TAG_VSYNC_END:
			{
				ValeraUtil.valeraAssert(tk.length == 3, "VSYNC_END FORMAT ERROR: " + line);
				int tid = Integer.parseInt(tk[1].trim());
				int msgId = Integer.parseInt(tk[2].trim());
				
				setCurActionIdOnLooperThread(tid, Message.MSG_NIL);
				
				// if (doPrint) {
				// 	System.out.printf("VSYNC_END msgId=%d tid=%d\n", 
				// 		msgId, tid);
				// }
			}
			break;
			case TraceTag.VALERA_TAG_BINDER_BEGIN:
			{
				// FIXME: binder thread is initially 0. 
				// Need to distinguish different binder thread when adding insns. 
				// by systemTid??
				ValeraUtil.valeraAssert(tk.length == 6, "BINDER_BEGIN FORMAT ERROR: " + line);
				int tid = Integer.parseInt(tk[1].trim());
				int code = Integer.parseInt(tk[2].trim());
				long dataObj = Long.parseLong(tk[3].trim(), 16);
				long replyObj = Long.parseLong(tk[4].trim(), 16);
				int flags = Integer.parseInt(tk[5].trim());
				
				if (printLevel == PRINT_LEVEL_DETAIL) {
					System.out.printf("BINDER_BEGIN tid=%d code=%d dataObj=%x replyObj=%x flags=%d\n", 
							tid, code, dataObj, replyObj, flags);
				}
				
				// TODO: Add binder begin trace action to binder thread?
			}
			break;
			case TraceTag.VALERA_TAG_BINDER_END:
			{
				// FIXME: binder thread is initially 0. 
				// Need to distinguish different binder thread when adding insns. 
				// by systemTid??
				ValeraUtil.valeraAssert(tk.length == 6, "BINDER_END FORMAT ERROR: " + line);
				int tid = Integer.parseInt(tk[1].trim());
				int code = Integer.parseInt(tk[2].trim());
				long dataObj = Long.parseLong(tk[3].trim(), 16);
				long replyObj = Long.parseLong(tk[4].trim(), 16);
				int flags = Integer.parseInt(tk[5].trim());
				
				if (printLevel == PRINT_LEVEL_DETAIL) {
					System.out.printf("BINDER_END tid=%d code=%d dataObj=%x replyObj=%x flags=%d\n", 
							tid, code, dataObj, replyObj, flags);
				}
				
				// TODO: Add binder end trace action to binder thread?
			}
			break;
			case TraceTag.VALERA_TAG_OBJECT_RW:
			{
				ValeraUtil.valeraAssert(tk.length == 9, "OBJECT_RW FORMAT ERROR: " + line);
				int tid = Integer.parseInt(tk[1].trim());
				int pc = Integer.parseInt(tk[2].trim());
				String className = tk[3].trim();
				String fieldName = tk[4].trim();
				int byteOffset = Integer.parseInt(tk[5].trim());
				long objAddr = Long.parseLong(tk[6].trim(), 16);
				int fieldIdx = Integer.parseInt(tk[7].trim());
				String rw = tk[8].trim();
				
				Thread thrd = mThrdMap.get(tid);
				ValeraUtil.valeraAssert(thrd != null, "Null thread in thread map " + tid);
				
				TraceMemoryRW mem = new TraceMemoryRW(action, tid, pc, className, fieldName, objAddr, fieldIdx, 
						rw.charAt(0) == 'R' ? TraceMemoryRW.MEMORY_READ : TraceMemoryRW.MEMORY_WRITE, false);
				
				addTraceInstrToThread(thrd, mem);
			}
			break;
			case TraceTag.VALERA_TAG_OBJECT_RW_QUICK:
			{
				ValeraUtil.valeraAssert(false, "OBJECT_RW_QUICK is deprecated.");
				/*
				ValeraUtil.valeraAssert(tk.length == 7, "OBJECT_RW_QUICK FORMAT ERROR: " + line);
				int tid = Integer.parseInt(tk[1].trim());
				int pc = Integer.parseInt(tk[2].trim());
				String className = tk[3].trim();
				long objAddr = Long.parseLong(tk[4].trim(), 16);
				int fieldIdx = Integer.parseInt(tk[5].trim());
				String rw = tk[6].trim();
				
				TraceMemoryRW mem = new TraceMemoryRW(action, tid, pc, className, null, objAddr, fieldIdx, 
						rw.charAt(0) == 'R' ? TraceMemoryRW.MEMORY_READ : TraceMemoryRW.MEMORY_WRITE, false);
				
				if (curActionId > 0) {
					Message msg = mMsgMap.get(curActionId);
					ValeraUtil.valeraAssert(msg != null, "Action not found " + curActionId);
					if (msg.isTracing) {
						TraceMemoryRW mem = new TraceMemoryRW(action, tid, pc, className, null, objAddr, fieldIdx, 
								rw.charAt(0) == 'R' ? TraceMemoryRW.MEMORY_READ : TraceMemoryRW.MEMORY_WRITE, false);
						msg.insns.add(mem);
					}
				}
				*/
			}
			break;
			case TraceTag.VALERA_TAG_STATIC_RW:
			{
				ValeraUtil.valeraAssert(tk.length == 8, "STATIC_RW FORMAT ERROR: " + line);
				int tid = Integer.parseInt(tk[1].trim());
				int pc = Integer.parseInt(tk[2].trim());
				String className = tk[3].trim();
				String fieldName = tk[4].trim();
				long objAddr = Long.parseLong(tk[5].trim(), 16);
				int fieldIdx = Integer.parseInt(tk[6].trim());
				String rw = tk[7].trim();
				
				Thread thrd = mThrdMap.get(tid);
				ValeraUtil.valeraAssert(thrd != null, "Null thread in thread map " + tid);
				
				TraceMemoryRW mem = new TraceMemoryRW(action, tid, pc, className, fieldName, objAddr, fieldIdx, 
						rw.charAt(0) == 'R' ? TraceMemoryRW.MEMORY_READ : TraceMemoryRW.MEMORY_WRITE, true);
				
				addTraceInstrToThread(thrd, mem);
			}
			break;
			case TraceTag.VALERA_TAG_PACKED_SWITCH:
			case TraceTag.VALERA_TAG_SPARSE_SWITCH:
			{
				ValeraUtil.valeraAssert(tk.length == 4, "PACKED_SWITCH FORMAT ERROR: " + line);
				int tid = Integer.parseInt(tk[1].trim());
				int pc = Integer.parseInt(tk[2].trim());
				int offset = Integer.parseInt(tk[3].trim());
				
				Thread thrd = mThrdMap.get(tid);
				ValeraUtil.valeraAssert(thrd != null, "Null thread in thread map " + tid);
				
				TraceSwitch swtch = null;
				switch (action) {
				case TraceTag.VALERA_TAG_PACKED_SWITCH:
					swtch = new TraceSwitch(action, tid, pc, offset, TraceSwitch.PACKED_SWITCH);
					break;
				case TraceTag.VALERA_TAG_SPARSE_SWITCH:
					swtch = new TraceSwitch(action, tid, pc, offset, TraceSwitch.SPARSE_SWITCH);
					break;
				default:
					ValeraUtil.valeraAssert(swtch != null, "Should not reach here!");
				}
				
				addTraceInstrToThread(thrd, swtch);
			}
			break;
			case TraceTag.VALERA_TAG_IFTEST:
			case TraceTag.VALERA_TAG_IFTESTZ:
			{
				ValeraUtil.valeraAssert(tk.length == 4, "IFTEST FORMAT ERROR: " + line);
				int tid = Integer.parseInt(tk[1].trim());
				int pc = Integer.parseInt(tk[2].trim());
				int taken = Integer.parseInt(tk[3].trim());
				
				Thread thrd = mThrdMap.get(tid);
				ValeraUtil.valeraAssert(thrd != null, "Null thread in thread map " + tid);
				
				TraceIfTest iftest = null;
				switch (action) {
				case TraceTag.VALERA_TAG_IFTEST:
					iftest = new TraceIfTest(action, tid, pc, taken > 0 ? true : false, TraceIfTest.IFTEST);
					break;
				case TraceTag.VALERA_TAG_IFTESTZ:
					iftest = new TraceIfTest(action, tid, pc, taken > 0 ? true : false, TraceIfTest.IFZTEST);
					break;
				default:
					break;
				}
				
				addTraceInstrToThread(thrd, iftest);
			}
			break;
			case TraceTag.VALERA_TAG_LIFECYCLE:
			{
				ValeraUtil.valeraAssert(tk.length == 5, "LIFECYCLE FORMAT ERROR: " + line);
				
				int tid = Integer.parseInt(tk[1].trim());
				String operation = tk[2].trim();
				String activity = tk[3].trim();
				// FIXME: use nano time for precise??
				long timestamp = Long.parseLong(tk[4].trim());
				
				ActivityLifecycle al = mActivityLifecycle.get(activity);
				if (al == null) {
					al = new ActivityLifecycle(activity);
					mActivityLifecycle.put(activity, al);
				}
				
				if (operation.equals("onStart")) {
					ValeraUtil.valeraAssert(al.state == ActivityLifecycle.ON_INIT
							| al.state == ActivityLifecycle.ON_STOP, "Before onStart, state should be init or onStop. but it's " + al.state);
					al.state = ActivityLifecycle.ON_START;
					al.timestamp = timestamp;
				}
				else if (operation.equals("onStop")) {
					ValeraUtil.valeraAssert(al.state == ActivityLifecycle.ON_START, "onStart should happen before onStop. but it's " + al.state);
					long t = timestamp - al.timestamp;
					al.totalTime += t;
					
					al.state = ActivityLifecycle.ON_STOP;
					al.timestamp = 0;
				}
			}
			break;
			case TraceTag.VALERA_TAG_DEBUG_PRINT:
			{
				ValeraUtil.valeraAssert(tk.length >= 3, "DEBUG_PRINT FORMAT ERROR: " + line);
				
				if (printLevel == PRINT_LEVEL_DETAIL) {
					System.out.println("DEBUG_PRINT " + line);
				}
			}
			break;
			case TraceTag.VALERA_TAG_SYSTEM_EXIT:
			{
				ValeraUtil.valeraAssert(tk.length == 2, "SYSTEM_EXIT FORMAT ERROR: " + line);
			}
			break;
			default:
				throw new RuntimeException("Unknown Action " + action);
			}
		}
		
		// Print out suspicious code on looper (not within an action);
		for(Map.Entry<Integer, Thread> entry : mThrdMap.entrySet()){
		    Thread thrd = entry.getValue();
			if (thrd instanceof LooperThread) {
				LooperThread looper = (LooperThread) thrd;
				for (LooperThread.UnknownEvent ue : looper.unknown) {
					System.err.printf("Last Event on Looper/%d/%x is %d \n", looper.tid, looper.qid, ue.lastMsgId);
					for (TraceInstruction ins : ue.insns) {
						ins.setPrintStream(System.err);
						ins.print(2);
					}
					System.err.println();
				}
			}
		}
	}
	
	private boolean inFilterList(Message msg) {
		String dispatcher = msg.dispatcher;
		if (dispatcher.contains("android.view.Choreographer")
		 || dispatcher.contains("valera.ValeraHandler"))
			return true;
		return false;
	}

	public void compare(ValeraTrace other) {
		final int INVALID = -999999;
		
		int size1 = this.mExeOrder.size();
		Integer list1[] = new Integer[size1];
		int index1[] = new int[size1];
		Arrays.fill(index1, INVALID);
		this.mExeOrder.toArray(list1);
		
		int size2 = other.mExeOrder.size();
		Integer list2[] = new Integer[size2];
		int index2[] = new int[size2];
		Arrays.fill(index2, INVALID);
		other.mExeOrder.toArray(list2);
		
		for (int i = 0; i < size1; i++) {
			Message msg1 = this.mMsgMap.get(list1[i]);
			if (inFilterList(msg1)) {
				index1[i] = -1;
				continue;
			}
			for (int j = 0; j < size2; j++) {
				//if (index1[i] != INVALID)
				//	break;
				if (index2[j] == INVALID) {
					Message msg2 = other.mMsgMap.get(list2[j]);
					if (inFilterList(msg2)) {
						index2[j] = -1;
						continue;
					}
					
					/*
					System.out.println(String.format("msg1: msgId=%d handler=%s handlerId=%d callback=%s what=%d arg1=%d arg2=%d obj=%s", 
							msg1.msgId, msg1.dispatcher, msg1.handlerId, msg1.callback, 
							msg1.what, msg1.arg1, msg1.arg2, msg1.obj));
					System.out.println(String.format("msg2: msgId=%d handler=%s handlerId=%d callback=%s what=%d arg1=%d arg2=%d obj=%s", 
							msg2.msgId, msg2.dispatcher, msg2.handlerId, msg2.callback, 
							msg2.what, msg2.arg1, msg2.arg2, msg2.obj));
					*/
					
					String callback1 = msg1.callback == null ? "null" : msg1.callback.split("@")[0];
					String callback2 = msg2.callback == null ? "null" : msg2.callback.split("@")[0];
					if (msg1.dispatcher.equals(msg2.dispatcher) && 
						msg1.handlerId == msg2.handlerId &&
						callback1.equals(callback2) && 
						msg1.what == msg2.what) {
						//msg1.arg1 == msg2.arg1 &&
						//msg1.arg2 == msg2.arg2) {
						index1[i] = j;
						index2[j] = i;
						break;
						//System.out.println("EQUAL!!!");
					}
					//System.out.println();
				}
			}
		}
		
		System.out.println("No matched event of 1:");
		for (int i = 0; i < size1; i++) {
			if (index1[i] == INVALID) {
				Message msg = this.mMsgMap.get(list1[i]);
				System.out.printf("msgId=%d handler=%s handlerId=%d callback=%s what=%d arg1=%d arg2=%d obj=%s\n", 
					msg.msgId, msg.dispatcher, msg.handlerId, msg.callback, 
					msg.what, msg.arg1, msg.arg2, msg.obj);
			}
		}
		System.out.println();
		System.out.println();
		System.out.println();
		System.out.println("No matched event of 2:");
		for (int j = 0; j < size2; j++) {
			if (index2[j] == INVALID) {
				Message msg = other.mMsgMap.get(list2[j]);
				System.out.printf("msgId=%d handler=%s handlerId=%d callback=%s what=%d arg1=%d arg2=%d obj=%s\n", 
					msg.msgId, msg.dispatcher, msg.handlerId, msg.callback, 
					msg.what, msg.arg1, msg.arg2, msg.obj);
			}
		}
		
	}
	
	public void queryActionGraph() {
		System.out.println("Please input actionId. -1 for end.");
		Scanner cin = new Scanner(System.in);
		while (cin.hasNextInt()) {
			int actionId = cin.nextInt();
			if (actionId < 0)
				break;
			boolean done = false;
			Message msg = mMsgMap.get(actionId);
			ValeraUtil.valeraAssert(msg != null, "Action not found: " + actionId);

			while (!done) {
				MsgPostRelation rel = msg.relation;
				System.out.printf("=> msgId=%d handler=%s handlerId=%d handlerCnt=%d callback=%s what=%d arg1=%d arg2=%d obj=%s;"
						+ " relation type=%d tid=%d -> tid=%d fromMsgId=%d\n", 
					msg.msgId, msg.dispatcher, msg.handlerId, msg.handlerCnt, msg.callback, 
					msg.what, msg.arg1, msg.arg2, msg.obj, 
					rel.type, rel.tidFrom, rel.tidTo, rel.msgIdFrom);
				
				if (rel.type == 2 || rel.type == 4) {
					msg = mMsgMap.get(rel.msgIdFrom);
					ValeraUtil.valeraAssert(msg != null, "Action not found: " + rel.msgIdFrom);
				} else {
					done = true;
				}
			}
			
			System.out.println("\n\n");
			System.out.println("Please input actionId. -1 for end.");
		}
	}
	
	public void printActionTrace() {
		int size = this.mExeOrder.size();
		Integer list[] = new Integer[size];
		this.mExeOrder.toArray(list);
		
		for (int i = 0; i < size; i++) {
			Message msg = this.mMsgMap.get(list[i]);
			if (msg.isTracing) {
				System.out.printf("ACTION_BEGIN msgId=%d handler=%s handlerId=%d handlerCnt=%d callback=%s what=%d arg1=%d arg2=%d obj=%s isTrace=%b\n", 
						msg.msgId, msg.dispatcher, msg.handlerId, msg.handlerCnt, msg.callback, msg.what, msg.arg1, msg.arg2, msg.obj, msg.isTracing);
				System.out.printf("SCHEDULE %s %d %s %d %d %d\n", 
						msg.dispatcher, msg.handlerId, msg.callback, msg.what, msg.arg1, msg.arg2);
				int indent = 0;
				for (TraceInstruction ins : msg.insns) {
					if ((ins instanceof TraceMethod) && ((TraceMethod) ins).type == TraceMethod.METHOD_ENTER)
						indent++;
					if (indent >= 0)
						ins.print(indent);
					if ((ins instanceof TraceMethod) && ((TraceMethod) ins).type == TraceMethod.METHOD_EXIT && indent >= 0)
						indent--;
				}
				System.out.printf("ACTION_END msgId=%d handler=%s handlerId=%d handlerCnt=%d callback=%s what=%d arg1=%d arg2=%d obj=%s isTrace=%b\n", 
						msg.msgId, msg.dispatcher, msg.handlerId, msg.handlerCnt, msg.callback, msg.what, msg.arg1, msg.arg2, msg.obj, msg.isTracing);
				System.out.println();
				System.out.println();
				System.out.println();
			}
		}
	}
	
	public void printLifecycle() {
		long totalTime = 0;
		for (Entry<String, ActivityLifecycle> entry : mActivityLifecycle.entrySet()) {
			ActivityLifecycle al = entry.getValue();
			totalTime += al.totalTime;
			System.out.println(al.activityName + " " + al.totalTime + " ms.");
		}
		System.out.println("Total time is " + totalTime + " ms.");
	}
	
	// Some events may be absent during different runs, ignore them.
	private boolean inBlackList(Message msg) {
		// TODO: add a blacklist file to ease extension.
		String dispatcher = msg.dispatcher;
		String callback = msg.callback;
		int what = msg.what;
		
		// Ignore ViewRootHandler 22 - Animation
		if (dispatcher != null && dispatcher.equals("android.view.ViewRootImpl$ViewRootHandler") && what == 22)
			return true;
		
		return false;
	}
	
	public void generateSchedule() {
		LooperThread mainThrd = (LooperThread) mThrdMap.get(1);
		ValeraUtil.valeraAssert(mainThrd != null, "Cannot find main looper thread");
		
		int cntVsync = 0;
		
		for (Message msg : mainThrd.events) {
			
			if (inBlackList(msg))
				continue;
			
			if (msg.isAsync || msg.flag == Message.FLAG_VSYNC) {
				cntVsync++;
				continue;
			}
			
			/*
			// transitively check whether this event is from binder.
			MsgPostRelation mpr = msg.relation;
			int msgId = msg.msgId;
			int trans = 0;
			while (msgId != -1 && mpr != null) {
				if (mpr.type == MsgPostRelation.TYPE_6_BINDER_TO_LOOPER) {
					System.out.printf("WARNING: msg[%d] come from binder, trans=%d\n", msg.msgId, trans);
					break;
				}
				if (mpr.type == MsgPostRelation.TYPE_2_INTERNAL_SAME_LOOPER
				 || mpr.type == MsgPostRelation.TYPE_4_INTERNAL_TWO_LOOPER) {
					Message m = mMsgMap.get(mpr.msgIdFrom);
					msgId = m.msgId;
					mpr = m.relation;
					trans++;
				} else {
					break;
				}
			}
			*/
			
			switch (msg.flag) {
			case Message.FLAG_INTERNAL_MSG: {
				System.out.printf("%s %d %s %d %d %d\n", msg.dispatcher, 
						msg.handlerId, msg.callback, msg.what, msg.arg1, msg.arg2);
			} break;
			case Message.FLAG_INPUT_EVENTS: {
				System.out.printf("%s %d %s %d %d %d\n", "valera.ValeraHandler", 
						1, "null", ValeraTrace.REPLAY_INPUT_EVENT, cntVsync, 0);
				cntVsync = 0;
			} break;
			case Message.FLAG_IMM_EVENTS: {
				// TODO: impl later.
				System.out.println("WARNING: IMPL LATER");
			} break;
			case Message.FLAG_SENSOR_EVENTS: {
				// TODO: impl later.
				System.out.println("WARNING: IMPL LATER");
			} break;
			case Message.FLAG_VSYNC: {
				// Ingore vsync interrupt.
			} break;
			}
		}
	}
}
