package valera.offline;

import valera.offline.io.IOPostProcessor;
import valera.offline.io.InputEventProcessor;
import valera.offline.trace.ValeraTrace;
import valera.http.OfflineHttpParser;

public class Main {
	
	public static void usage() {
		System.out.println("Usage: java -jar ValeraTool.jar <cmd> <args>");
		System.out.println(
				"  iorewrite <io.bin> \n" +
				"  print_io_content <io.bin> \n" + 
				"  print_io_event <io.bin> \n" + 
				"  print_conn_content <io.bin> <connId> \n" + 
				"  print_trace_event <event_trace> \n" +
				"  print_trace_content <event_trace> \n" + 
				"  compare_events <event_trace1> <event_trace2> \n" +
				"  query_action_graph <event_trace> \n" + 
				"  action_insns <event_trace> \n" +
				"  lifecycle <event_trace> \n" + 
				"  generate_schedule <event_trace> \n" + 
				"  print_input_event <inputevent.bin>\n" + 
				"  print_input_content <inputevent.bin>\n");
	}
	
	public static void ValeraToolAssert(boolean condition) {
		if (condition == false) {
			usage();
			System.exit(-1);
		}
	}
	
	public static void main(String[] arg) throws Exception {
		Main.ValeraToolAssert(arg.length > 0);
		String cmd = arg[0];
		if (cmd.equals("iorewrite")) {
			Main.ValeraToolAssert(arg.length == 2);
			String filename = arg[1];
			IOPostProcessor iop = new IOPostProcessor(filename);
			iop.parse(IOPostProcessor.PRINT_LEVEL_NONE);
			iop.check();
			iop.rewrite();
		} else if (cmd.equals("print_io_event")) {
			Main.ValeraToolAssert(arg.length == 2);
			String filename = arg[1];
			IOPostProcessor iop = new IOPostProcessor(filename);
			iop.parse(IOPostProcessor.PRINT_LEVEL_NETWORK_EVENT);
		} else if (cmd.equals("print_io_content")) {
			Main.ValeraToolAssert(arg.length == 2);
			String filename = arg[1];
			IOPostProcessor iop = new IOPostProcessor(filename);
			iop.parse(IOPostProcessor.PRINT_LEVEL_NETWORK_CONTENT);
		} else if (cmd.equals("print_conn_content")) {
			Main.ValeraToolAssert(arg.length == 3);
			String filename = arg[1];
			int connId = Integer.parseInt(arg[2]);
			IOPostProcessor iop = new IOPostProcessor(filename);
			iop.parse(IOPostProcessor.PRINT_LEVEL_NONE);
			byte[] data = iop.getConnResponseData(connId);
			new OfflineHttpParser().parse(data);
		} else if (cmd.equals("print_trace_event")) {
			Main.ValeraToolAssert(arg.length == 2);
			String filename = arg[1];
			new ValeraTrace(filename).parse(ValeraTrace.PRINT_LEVEL_SHORT);
		} else if (cmd.equals("print_trace_content")) {
			Main.ValeraToolAssert(arg.length == 2);
			String filename = arg[1];
			new ValeraTrace(filename).parse(ValeraTrace.PRINT_LEVEL_DETAIL);
		} else if (cmd.equals("compare_events")) {
			Main.ValeraToolAssert(arg.length == 3);
			String filename1 = arg[1];
			String filename2 = arg[2];
			ValeraTrace trace1 = new ValeraTrace(filename1);
			trace1.parse(ValeraTrace.PRINT_LEVEL_NONE);
			ValeraTrace trace2 = new ValeraTrace(filename2);
			trace2.parse(ValeraTrace.PRINT_LEVEL_NONE);
			trace1.compare(trace2);
		} else if (cmd.equals("query_action_graph")) {
			Main.ValeraToolAssert(arg.length == 2);
			String filename = arg[1];
			ValeraTrace vt = new ValeraTrace(filename);
			vt.parse(ValeraTrace.PRINT_LEVEL_NONE);
			vt.queryActionGraph();
		} else if (cmd.equals("action_insns")) {
			Main.ValeraToolAssert(arg.length == 2);
			String filename = arg[1];
			ValeraTrace vt = new ValeraTrace(filename);
			vt.parse(ValeraTrace.PRINT_LEVEL_NONE);
			vt.printActionTrace();
		} else if (cmd.equals("lifecycle")) {
			Main.ValeraToolAssert(arg.length == 2);
			String filename = arg[1];
			ValeraTrace vt = new ValeraTrace(filename);
			vt.parse(ValeraTrace.PRINT_LEVEL_NONE);
			vt.printLifecycle();
		} else if (cmd.equals("generate_schedule")) {
			Main.ValeraToolAssert(arg.length == 2);
			String filename = arg[1];
			ValeraTrace vt = new ValeraTrace(filename);
			vt.parse(ValeraTrace.PRINT_LEVEL_NONE);
			vt.generateSchedule();
		} else if (cmd.equals("print_input_event")) {
			Main.ValeraToolAssert(arg.length == 2);
			String filename = arg[1];
			InputEventProcessor iep = new InputEventProcessor(filename);
			iep.parse(InputEventProcessor.PRINT_LEVEL_EVENT);
		} else if (cmd.equals("print_input_content")) {
			Main.ValeraToolAssert(arg.length == 2);
			String filename = arg[1];
			InputEventProcessor iep = new InputEventProcessor(filename);
			iep.parse(InputEventProcessor.PRINT_LEVEL_CONTENT);
		} else {
			Main.usage();
		}
	}

}
