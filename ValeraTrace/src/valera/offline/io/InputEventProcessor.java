package valera.offline.io;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StreamCorruptedException;
import java.util.HashMap;

import valera.offline.camera.Camera;


public class InputEventProcessor {
	
	public final static int PRINT_LEVEL_NONE = 0;
	public final static int PRINT_LEVEL_EVENT = 1;
	public final static int PRINT_LEVEL_CONTENT = 2;
	
	private final static int TYPE_MOTION_EVENT = 1;
	private final static int TYPE_KEY_EVENT    = 2;
	private final static int TYPE_IMM_METHOD = 3;
	private final static int TYPE_SENSOR_EVENT = 4;
	private final static int TYPE_LOCATION_EVENT = 5;
	private final static int TYPE_CAMERA_EVENT = 6;

	private final static int TYPE_WINDOW_INPUT_RECEIVER = 1;

	private static final int IM_DO_GET_TEXT_AFTER_CURSOR = 10;
	private static final int IM_DO_GET_TEXT_BEFORE_CURSOR = 20;
	private static final int IM_DO_GET_SELECTED_TEXT = 25;
	private static final int IM_DO_GET_CURSOR_CAPS_MODE = 30;
	private static final int IM_DO_GET_EXTRACTED_TEXT = 40;
	private static final int IM_DO_COMMIT_TEXT = 50;
	private static final int IM_DO_COMMIT_COMPLETION = 55;
	private static final int IM_DO_COMMIT_CORRECTION = 56;
	private static final int IM_DO_SET_SELECTION = 57;
	private static final int IM_DO_PERFORM_EDITOR_ACTION = 58;
	private static final int IM_DO_PERFORM_CONTEXT_MENU_ACTION = 59;
	private static final int IM_DO_SET_COMPOSING_TEXT = 60;
	private static final int IM_DO_SET_COMPOSING_REGION = 63;
	private static final int IM_DO_FINISH_COMPOSING_TEXT = 65;
	private static final int IM_DO_SEND_KEY_EVENT = 70;
	private static final int IM_DO_DELETE_SURROUNDING_TEXT = 80;
	private static final int IM_DO_BEGIN_BATCH_EDIT = 90;
	private static final int IM_DO_END_BATCH_EDIT = 95;
	private static final int IM_DO_REPORT_FULLSCREEN_MODE = 100;
	private static final int IM_DO_PERFORM_PRIVATE_COMMAND = 120;
	private static final int IM_DO_CLEAR_META_KEY_STATES = 130;
	
	
	public static final int INVALID_POINTER_ID = -1;
    public static final int ACTION_MASK             = 0xff;
	public static final int ACTION_DOWN             = 0;
    public static final int ACTION_UP               = 1;
    public static final int ACTION_MOVE             = 2;
    public static final int ACTION_CANCEL           = 3;
    public static final int ACTION_OUTSIDE          = 4;
    public static final int ACTION_POINTER_DOWN     = 5;
    public static final int ACTION_POINTER_UP       = 6;
    public static final int ACTION_HOVER_MOVE       = 7;
    public static final int ACTION_SCROLL           = 8;
    public static final int ACTION_HOVER_ENTER      = 9;
    public static final int ACTION_HOVER_EXIT       = 10;
    public static final int ACTION_POINTER_INDEX_MASK  = 0xff00;
    public static final int ACTION_POINTER_INDEX_SHIFT = 8;
	
    
    private static final int LOC_LOCATION_CHANGED = 1;
    private static final int LOC_STATUS_CHANGED = 2;
    private static final int LOC_PROVIDER_ENABLED = 3;
    private static final int LOC_PROVIDER_DISABLED = 4;
    
    
	private ValeraLogReader mReader;

	public InputEventProcessor(String inputFile) throws Exception {
		mReader = new ValeraLogReader(new FileInputStream(inputFile));
	}
	
	private static String motionActionToString(int action) {
        switch (action) {
            case ACTION_DOWN:
                return "ACTION_DOWN";
            case ACTION_UP:
                return "ACTION_UP";
            case ACTION_CANCEL:
                return "ACTION_CANCEL";
            case ACTION_OUTSIDE:
                return "ACTION_OUTSIDE";
            case ACTION_MOVE:
                return "ACTION_MOVE";
            case ACTION_HOVER_MOVE:
                return "ACTION_HOVER_MOVE";
            case ACTION_SCROLL:
                return "ACTION_SCROLL";
            case ACTION_HOVER_ENTER:
                return "ACTION_HOVER_ENTER";
            case ACTION_HOVER_EXIT:
                return "ACTION_HOVER_EXIT";
        }
        int index = (action & ACTION_POINTER_INDEX_MASK) >> ACTION_POINTER_INDEX_SHIFT;
        switch (action & ACTION_MASK) {
            case ACTION_POINTER_DOWN:
                return "ACTION_POINTER_DOWN(" + index + ")";
            case ACTION_POINTER_UP:
                return "ACTION_POINTER_UP(" + index + ")";
            default:
                return Integer.toString(action);
        }
    }
	
	private static String locationActionToString(int action) {
		switch (action) {
			case LOC_LOCATION_CHANGED:
				return "onLocationChanged";
			case LOC_STATUS_CHANGED:
				return "onStatusChanged";
			case LOC_PROVIDER_ENABLED:
				return "onProviderEnabled";
			case LOC_PROVIDER_DISABLED:
				return "onProviderDisabled";
		}
		return "N/A";
	}
	
	private void readMotionEvent(int printLevel) throws Exception {
		long relativeTime = mReader.readLong();
		int msgId = mReader.readInt();
		long downTime = mReader.readLong();
		//downTime += valera.ValeraGlobal.getStartTime();
		long eventTime = mReader.readLong();
		//eventTime += valera.ValeraGlobal.getStartTime();
		int action = mReader.readInt();
		int pointerCount = mReader.readInt();

		PointerProperties pointerProperties[] = new PointerProperties[pointerCount];
		PointerCoords pointerCoords[] = new PointerCoords[pointerCount];

		for (int i = 0; i < pointerCount; i++) {
			pointerProperties[i] = new PointerProperties();
			int id = mReader.readInt();
			pointerProperties[i].id = id;
			int toolType = mReader.readInt();
			pointerProperties[i].toolType = toolType;

			pointerCoords[i] = new PointerCoords();
			float orientation = mReader.readFloat();
			pointerCoords[i].orientation = orientation;
			float pressure = mReader.readFloat();
			pointerCoords[i].pressure = pressure;
			float size = mReader.readFloat();
			pointerCoords[i].size = size;
			float toolMajor = mReader.readFloat();
			pointerCoords[i].toolMajor = toolMajor;
			float toolMinor = mReader.readFloat();
			pointerCoords[i].toolMinor = toolMinor;
			float touchMajor = mReader.readFloat();
			pointerCoords[i].touchMajor = touchMajor;
			float touchMinor = mReader.readFloat();
			pointerCoords[i].touchMinor = touchMinor;
			float x = mReader.readFloat();
			pointerCoords[i].x = x;
			float y = mReader.readFloat();
			pointerCoords[i].y = y;
		}

		int metaState = mReader.readInt();
		int buttonState = mReader.readInt();
		float xPrecision = mReader.readFloat();
		float yPrecision = mReader.readFloat();
		int deviceId = mReader.readInt();
		int edgeFlags = mReader.readInt();
		int source = mReader.readInt();
		int flags = mReader.readInt();
		
		int receiver_type = mReader.readInt();
		int index = mReader.readInt();
		
		// Print Motion Event;
		StringBuilder msg = new StringBuilder();
        msg.append("MotionEvent { action=").append(motionActionToString(action));

        //final int pointerCount = getPointerCount();
        for (int i = 0; i < pointerCount; i++) {
            msg.append(", id[").append(i).append("]=").append(pointerProperties[i].id);
            msg.append(", x[").append(i).append("]=").append(pointerCoords[i].x);
            msg.append(", y[").append(i).append("]=").append(pointerCoords[i].y);
            //msg.append(", toolType[").append(i).append("]=").append(
            //       toolTypeToString(pointerProperties[i].toolType));
        }

        //msg.append(", buttonState=").append(MotionEvent.buttonStateToString(getButtonState()));
        //msg.append(", metaState=").append(KeyEvent.metaStateToString(getMetaState()));
        //msg.append(", flags=0x").append(Integer.toHexString(getFlags()));
        //msg.append(", edgeFlags=0x").append(Integer.toHexString(getEdgeFlags()));
        msg.append(", pointerCount=").append(pointerCount);
        //msg.append(", historySize=").append(getHistorySize());
        msg.append(", eventTime=").append(eventTime);
        msg.append(", downTime=").append(downTime);
        msg.append(", deviceId=").append(deviceId);
        msg.append(", source=0x").append(Integer.toHexString(source));
        msg.append(" }");
        
        switch (printLevel) {
        	case PRINT_LEVEL_EVENT:
        		System.out.println(String.format("MotionEvent time=%d", relativeTime));
        		break;
        	case PRINT_LEVEL_CONTENT:
        		System.out.println(String.format("MotionEvent time=%d content=%s", relativeTime, msg.toString()));
        		break;
        }
	}
	
	public static final int KEY_ACTION_DOWN             = 0;
    public static final int KEY_ACTION_UP               = 1;
    public static final int KEY_ACTION_MULTIPLE         = 2;
	
	private static String keyActionToString(int action) {
        switch (action) {
            case KEY_ACTION_DOWN:
                return "ACTION_DOWN";
            case KEY_ACTION_UP:
                return "ACTION_UP";
            case KEY_ACTION_MULTIPLE:
                return "ACTION_MULTIPLE";
            default:
                return Integer.toString(action);
        }
    }
	
	private void readKeyEvent(int printLevel) throws Exception {
		long relativeTime = mReader.readLong();
		int msgId = mReader.readInt();
		long downTime = mReader.readLong();
		//downTime += valera.ValeraGlobal.getStartTime();
		long eventTime = mReader.readLong();
		//eventTime += valera.ValeraGlobal.getStartTime();

		int action = mReader.readInt();
		int code = mReader.readInt();
		int repeat = mReader.readInt();
		int metaState = mReader.readInt();
		int deviceId = mReader.readInt();
		int scancode = mReader.readInt();
		int flags = mReader.readInt();
		int source = mReader.readInt();
		
		int receiver_type = mReader.readInt();
		int index = mReader.readInt();

		StringBuilder msg = new StringBuilder();
        msg.append("KeyEvent { action=").append(keyActionToString(action));
        //msg.append(", keyCode=").append(keyCodeToString(mKeyCode));
        //msg.append(", scanCode=").append(mScanCode);
        //if (mCharacters != null) {
        //    msg.append(", characters=\"").append(mCharacters).append("\"");
        //}
        //msg.append(", metaState=").append(metaStateToString(mMetaState));
        //msg.append(", flags=0x").append(Integer.toHexString(mFlags));
        //msg.append(", repeatCount=").append(mRepeatCount);
        msg.append(", eventTime=").append(eventTime);
        msg.append(", downTime=").append(downTime);
        msg.append(", deviceId=").append(deviceId);
        msg.append(", source=0x").append(Integer.toHexString(source));
        msg.append(" }");
        
        switch (printLevel) {
    		case PRINT_LEVEL_EVENT:
    			System.out.println(String.format("KeyEvent time=%d", relativeTime));
    			break;
    		case PRINT_LEVEL_CONTENT:
    			System.out.println(String.format("KeyEvent time=%d content=%s", relativeTime, msg.toString()));
    			break;
        }
	}
	
	private void readLocationEvent(int printLevel) throws Exception {
		int loc_type = mReader.readInt();
		long relativeTime = mReader.readLong();
		String content = "";
		
		switch (loc_type) {
			case LOC_LOCATION_CHANGED: {
				int checkNull = mReader.readInt();
				if (checkNull == 0) {
					content = "null";
				} else {
					float accuracy = mReader.readFloat();
					double altitude = mReader.readDouble();
					float bearing = mReader.readFloat();
					double latitude = mReader.readDouble();
					double longitude = mReader.readDouble();
					String provider = mReader.readString();
					float speed = mReader.readFloat();
					long time = mReader.readLong();
					
					content = String.format("accuracy=%f altitude=%f bearing=%f latitude=%f longitude=%f provider=%s speed=%f time=%d", 
							accuracy, altitude, bearing, latitude, longitude, provider, speed, time);
				}
			}
				break;
			case LOC_STATUS_CHANGED: {
				String provider = mReader.readString();
				int status = mReader.readInt();
				
				content = String.format("provider=%s status=%d", provider, status);
			}
				break;
			case LOC_PROVIDER_ENABLED: {
				String provider = mReader.readString();
				
				content = String.format("provider=%s", provider);
			}
				break;
			case LOC_PROVIDER_DISABLED: {
				String provider = mReader.readString();
				
				content = String.format("provider=%s", provider);
			}
				break;
		}
		
		switch (printLevel) {
			case PRINT_LEVEL_EVENT:
				System.out.println(String.format("LocationEvent time=%d type=%s", 
						relativeTime, locationActionToString(loc_type)));
				break;
			case PRINT_LEVEL_CONTENT:
				System.out.println(String.format("LocationEvent time=%d type=%s content=%s", 
						relativeTime, locationActionToString(loc_type), content));
				break;
		}
	}
	
	private void readCameraEvent(int printLevel) throws Exception {
		long relativeTime = mReader.readLong();
		String content = "";
		
		// TODO: read camera parameters.
		int what = mReader.readInt();
		int arg1 = mReader.readInt();
		int arg2 = mReader.readInt();
		byte[] bytes = mReader.readByteArray();
		
		// TODO: for preview frame / picture, dump to file.
		
		switch (printLevel) {
			case PRINT_LEVEL_EVENT:
				System.out.println(String.format("CameraEvent time=%d what=%s", 
						relativeTime, Camera.cameraMsgToString(what)));
				break;
			case PRINT_LEVEL_CONTENT:
				System.out.println(String.format("CameraEvent time=%d what=%s content=%s", 
						relativeTime, Camera.cameraMsgToString(what), content));
				break;
		}
	}
	
	private void readIMMEvent(int printLevel) throws Exception {
		int im_type = mReader.readInt();
		String text = mReader.readString();
		int new_pos = mReader.readInt();
		long relative_time = mReader.readLong();
	}
	
	private void readSensorEvent(int printLevel) throws Exception {
		int sensor_type = mReader.readInt();
		long relative_time = mReader.readLong();
		int handle = mReader.readInt();
		int len = mReader.readInt();
		for (int i = 0; i < len; i++)
			mReader.readFloat();
		int accuracy = mReader.readInt();
		long timestamp = mReader.readLong();
		String listener = mReader.readString();
	}
	
	public void parse(int printLevel) {
		int event_type = -1;
		
		while (true) {
			try {
				event_type = mReader.readInt();
			} catch (EOFException e) {
				//System.out.println("EOF reached.");
				break;
			} catch (Exception e) {
				System.err.println(e);
				e.printStackTrace();
				System.exit(-1);
				
			}
			
			try {
				switch (event_type) {
				case TYPE_MOTION_EVENT:
					readMotionEvent(printLevel);
					break;
				case TYPE_KEY_EVENT:
					readKeyEvent(printLevel);
					break;
				case TYPE_IMM_METHOD:
					readIMMEvent(printLevel);
					break;
				case TYPE_SENSOR_EVENT:
					readSensorEvent(printLevel);
					break;
				case TYPE_LOCATION_EVENT:
					readLocationEvent(printLevel);
					break;
				case TYPE_CAMERA_EVENT:
					readCameraEvent(printLevel);
					break;
				default:
					System.err.println("Unsupported replay event: " + event_type);
					break;
				}
			} catch (Exception e) {
				System.err.println(e);
				e.printStackTrace();
				System.exit(-1);
			}
		}
		
		try {
			mReader.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	
	
	public static final class PointerCoords {
        private static final int INITIAL_PACKED_AXIS_VALUES = 8;
        private long mPackedAxisBits;
        private float[] mPackedAxisValues;

        public PointerCoords() {
        }

        public PointerCoords(PointerCoords other) {
            copyFrom(other);
        }

        /** @hide */
        public static PointerCoords[] createArray(int size) {
            PointerCoords[] array = new PointerCoords[size];
            for (int i = 0; i < size; i++) {
                array[i] = new PointerCoords();
            }
            return array;
        }

        public float x;
        
        public float y;
        
        public float pressure;
        
        public float size;
        
        public float touchMajor;
        
        public float touchMinor;
        
        public float toolMajor;
        
        public float toolMinor;
        
        public float orientation;

        public void clear() {
            mPackedAxisBits = 0;

            x = 0;
            y = 0;
            pressure = 0;
            size = 0;
            touchMajor = 0;
            touchMinor = 0;
            toolMajor = 0;
            toolMinor = 0;
            orientation = 0;
        }

        public void copyFrom(PointerCoords other) {
            final long bits = other.mPackedAxisBits;
            mPackedAxisBits = bits;
            if (bits != 0) {
                final float[] otherValues = other.mPackedAxisValues;
                final int count = Long.bitCount(bits);
                float[] values = mPackedAxisValues;
                if (values == null || count > values.length) {
                    values = new float[otherValues.length];
                    mPackedAxisValues = values;
                }
                System.arraycopy(otherValues, 0, values, 0, count);
            }

            x = other.x;
            y = other.y;
            pressure = other.pressure;
            size = other.size;
            touchMajor = other.touchMajor;
            touchMinor = other.touchMinor;
            toolMajor = other.toolMajor;
            toolMinor = other.toolMinor;
            orientation = other.orientation;
        }
    }

    public static final class PointerProperties {
        public PointerProperties() {
            clear();
        }

        public PointerProperties(PointerProperties other) {
            copyFrom(other);
        }

        /** @hide */
        public static PointerProperties[] createArray(int size) {
            PointerProperties[] array = new PointerProperties[size];
            for (int i = 0; i < size; i++) {
                array[i] = new PointerProperties();
            }
            return array;
        }

        public int id;

        public int toolType;

        public void clear() {
            id = -1;
            toolType = 0;
        }

        public void copyFrom(PointerProperties other) {
            id = other.id;
            toolType = other.toolType;
        }

        @Override
        public boolean equals(Object other) {
            if (other instanceof PointerProperties) {
                return equals((PointerProperties)other);
            }
            return false;
        }

        private boolean equals(PointerProperties other) {
            return other != null && id == other.id && toolType == other.toolType;
        }

        @Override
        public int hashCode() {
            return id | (toolType << 8);
        }
    }
}
