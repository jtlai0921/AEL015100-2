package ch16.gui.b4a.example;

import anywheresoftware.b4a.B4AMenuItem;
import android.app.Activity;
import android.os.Bundle;
import anywheresoftware.b4a.BA;
import anywheresoftware.b4a.BALayout;
import anywheresoftware.b4a.B4AActivity;
import anywheresoftware.b4a.ObjectWrapper;
import anywheresoftware.b4a.objects.ActivityWrapper;
import java.lang.reflect.InvocationTargetException;
import anywheresoftware.b4a.B4AUncaughtException;
import anywheresoftware.b4a.debug.*;
import java.lang.ref.WeakReference;

public class main extends Activity implements B4AActivity{
	public static main mostCurrent;
	static boolean afterFirstLayout;
	static boolean isFirst = true;
    private static boolean processGlobalsRun = false;
	BALayout layout;
	public static BA processBA;
	BA activityBA;
    ActivityWrapper _activity;
    java.util.ArrayList<B4AMenuItem> menuItems;
	public static final boolean fullScreen = false;
	public static final boolean includeTitle = true;
    public static WeakReference<Activity> previousOne;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (isFirst) {
			processBA = new BA(this.getApplicationContext(), null, null, "ch16.gui.b4a.example", "ch16.gui.b4a.example.main");
			processBA.loadHtSubs(this.getClass());
	        float deviceScale = getApplicationContext().getResources().getDisplayMetrics().density;
	        BALayout.setDeviceScale(deviceScale);
		}
		else if (previousOne != null) {
			Activity p = previousOne.get();
			if (p != null && p != this) {
                BA.LogInfo("Killing previous instance (main).");
				p.finish();
			}
		}
		if (!includeTitle) {
        	this.getWindow().requestFeature(android.view.Window.FEATURE_NO_TITLE);
        }
        if (fullScreen) {
        	getWindow().setFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN,   
        			android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
		mostCurrent = this;
        processBA.sharedProcessBA.activityBA = null;
		layout = new BALayout(this);
		setContentView(layout);
		afterFirstLayout = false;
		BA.handler.postDelayed(new WaitForLayout(), 5);

	}
	private static class WaitForLayout implements Runnable {
		public void run() {
			if (afterFirstLayout)
				return;
			if (mostCurrent == null)
				return;
			if (mostCurrent.layout.getWidth() == 0) {
				BA.handler.postDelayed(this, 5);
				return;
			}
			mostCurrent.layout.getLayoutParams().height = mostCurrent.layout.getHeight();
			mostCurrent.layout.getLayoutParams().width = mostCurrent.layout.getWidth();
			afterFirstLayout = true;
			mostCurrent.afterFirstLayout();
		}
	}
	private void afterFirstLayout() {
        if (this != mostCurrent)
			return;
		activityBA = new BA(this, layout, processBA, "ch16.gui.b4a.example", "ch16.gui.b4a.example.main");
        processBA.sharedProcessBA.activityBA = new java.lang.ref.WeakReference<BA>(activityBA);
        anywheresoftware.b4a.objects.ViewWrapper.lastId = 0;
        _activity = new ActivityWrapper(activityBA, "activity");
        anywheresoftware.b4a.Msgbox.isDismissing = false;
        initializeProcessGlobals();		
        initializeGlobals();
        
        BA.LogInfo("** Activity (main) Create, isFirst = " + isFirst + " **");
        processBA.raiseEvent2(null, true, "activity_create", false, isFirst);
		isFirst = false;
		if (this != mostCurrent)
			return;
        processBA.setActivityPaused(false);
        BA.LogInfo("** Activity (main) Resume **");
        processBA.raiseEvent(null, "activity_resume");
        if (android.os.Build.VERSION.SDK_INT >= 11) {
			try {
				android.app.Activity.class.getMethod("invalidateOptionsMenu").invoke(this,(Object[]) null);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}
	public void addMenuItem(B4AMenuItem item) {
		if (menuItems == null)
			menuItems = new java.util.ArrayList<B4AMenuItem>();
		menuItems.add(item);
	}
	@Override
	public boolean onCreateOptionsMenu(android.view.Menu menu) {
		super.onCreateOptionsMenu(menu);
		if (menuItems == null)
			return false;
		for (B4AMenuItem bmi : menuItems) {
			android.view.MenuItem mi = menu.add(bmi.title);
			if (bmi.drawable != null)
				mi.setIcon(bmi.drawable);
            if (android.os.Build.VERSION.SDK_INT >= 11) {
				try {
                    if (bmi.addToBar) {
				        android.view.MenuItem.class.getMethod("setShowAsAction", int.class).invoke(mi, 1);
                    }
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			mi.setOnMenuItemClickListener(new B4AMenuItemsClickListener(bmi.eventName.toLowerCase(BA.cul)));
		}
		return true;
	}
    public void onWindowFocusChanged(boolean hasFocus) {
       super.onWindowFocusChanged(hasFocus);
       processBA.raiseEvent2(null, true, "activity_windowfocuschanged", false, hasFocus);
    }
	private class B4AMenuItemsClickListener implements android.view.MenuItem.OnMenuItemClickListener {
		private final String eventName;
		public B4AMenuItemsClickListener(String eventName) {
			this.eventName = eventName;
		}
		public boolean onMenuItemClick(android.view.MenuItem item) {
			processBA.raiseEvent(item.getTitle(), eventName + "_click");
			return true;
		}
	}
    public static Class<?> getObject() {
		return main.class;
	}
    private Boolean onKeySubExist = null;
    private Boolean onKeyUpSubExist = null;
	@Override
	public boolean onKeyDown(int keyCode, android.view.KeyEvent event) {
		if (onKeySubExist == null)
			onKeySubExist = processBA.subExists("activity_keypress");
		if (onKeySubExist) {
			Boolean res =  (Boolean)processBA.raiseEvent2(_activity, false, "activity_keypress", false, keyCode);
			if (res == null || res == true)
				return true;
            else if (keyCode == anywheresoftware.b4a.keywords.constants.KeyCodes.KEYCODE_BACK) {
				finish();
				return true;
			}
		}
		return super.onKeyDown(keyCode, event);
	}
    @Override
	public boolean onKeyUp(int keyCode, android.view.KeyEvent event) {
		if (onKeyUpSubExist == null)
			onKeyUpSubExist = processBA.subExists("activity_keyup");
		if (onKeyUpSubExist) {
			Boolean res =  (Boolean)processBA.raiseEvent2(_activity, false, "activity_keyup", false, keyCode);
			if (res == null || res == true)
				return true;
		}
		return super.onKeyUp(keyCode, event);
	}
	@Override
	public void onNewIntent(android.content.Intent intent) {
		this.setIntent(intent);
	}
    @Override 
	public void onPause() {
		super.onPause();
        if (_activity == null) //workaround for emulator bug (Issue 2423)
            return;
		anywheresoftware.b4a.Msgbox.dismiss(true);
        BA.LogInfo("** Activity (main) Pause, UserClosed = " + activityBA.activity.isFinishing() + " **");
        processBA.raiseEvent2(_activity, true, "activity_pause", false, activityBA.activity.isFinishing());		
        processBA.setActivityPaused(true);
        mostCurrent = null;
        if (!activityBA.activity.isFinishing())
			previousOne = new WeakReference<Activity>(this);
        anywheresoftware.b4a.Msgbox.isDismissing = false;
	}

	@Override
	public void onDestroy() {
        super.onDestroy();
		previousOne = null;
	}
    @Override 
	public void onResume() {
		super.onResume();
        mostCurrent = this;
        anywheresoftware.b4a.Msgbox.isDismissing = false;
        if (activityBA != null) { //will be null during activity create (which waits for AfterLayout).
        	ResumeMessage rm = new ResumeMessage(mostCurrent);
        	BA.handler.post(rm);
        }
	}
    private static class ResumeMessage implements Runnable {
    	private final WeakReference<Activity> activity;
    	public ResumeMessage(Activity activity) {
    		this.activity = new WeakReference<Activity>(activity);
    	}
		public void run() {
			if (mostCurrent == null || mostCurrent != activity.get())
				return;
			processBA.setActivityPaused(false);
            BA.LogInfo("** Activity (main) Resume **");
		    processBA.raiseEvent(mostCurrent._activity, "activity_resume", (Object[])null);
		}
    }
	@Override
	protected void onActivityResult(int requestCode, int resultCode,
	      android.content.Intent data) {
		processBA.onActivityResult(requestCode, resultCode, data);
	}
	private static void initializeGlobals() {
		processBA.raiseEvent2(null, true, "globals", false, (Object[])null);
	}

public anywheresoftware.b4a.keywords.Common __c = null;
public static anywheresoftware.b4a.objects.MediaPlayerWrapper _mp = null;
public static anywheresoftware.b4a.phone.Phone.PhoneSensors _sensor = null;
public ch16.gui.b4a.example.cameraexclass _camera1 = null;
public anywheresoftware.b4a.objects.PanelWrapper _pnlpreview = null;
public anywheresoftware.b4a.objects.ButtonWrapper _button1 = null;
public anywheresoftware.b4a.objects.CompoundButtonWrapper.CheckBoxWrapper _chkautofocus = null;
public anywheresoftware.b4a.objects.LabelWrapper _lbloutput = null;
public static String  _activity_create(boolean _firsttime) throws Exception{
 //BA.debugLineNum = 28;BA.debugLine="Sub Activity_Create(FirstTime As Boolean)";
 //BA.debugLineNum = 29;BA.debugLine="Activity.LoadLayout(\"Main\")";
mostCurrent._activity.LoadLayout("Main",mostCurrent.activityBA);
 //BA.debugLineNum = 30;BA.debugLine="Activity.Title = \"聰明相機\"";
mostCurrent._activity.setTitle((Object)("聰明相機"));
 //BA.debugLineNum = 31;BA.debugLine="If FirstTime Then";
if (_firsttime) { 
 //BA.debugLineNum = 32;BA.debugLine="MP.Initialize()";
_mp.Initialize();
 //BA.debugLineNum = 33;BA.debugLine="MP.Load(File.DirAssets, \"TakePicture.mp3\")";
_mp.Load(anywheresoftware.b4a.keywords.Common.File.getDirAssets(),"TakePicture.mp3");
 //BA.debugLineNum = 34;BA.debugLine="sensor.Initialize(sensor.TYPE_ACCELEROMETER)";
_sensor.Initialize(_sensor.TYPE_ACCELEROMETER);
 };
 //BA.debugLineNum = 36;BA.debugLine="Button1.Color = Colors.Transparent";
mostCurrent._button1.setColor(anywheresoftware.b4a.keywords.Common.Colors.Transparent);
 //BA.debugLineNum = 37;BA.debugLine="End Sub";
return "";
}
public static String  _activity_pause(boolean _userclosed) throws Exception{
 //BA.debugLineNum = 47;BA.debugLine="Sub Activity_Pause (UserClosed As Boolean)";
 //BA.debugLineNum = 48;BA.debugLine="camera1.Release()";
mostCurrent._camera1._release();
 //BA.debugLineNum = 49;BA.debugLine="sensor.StopListening()";
_sensor.StopListening(processBA);
 //BA.debugLineNum = 50;BA.debugLine="End Sub";
return "";
}
public static String  _activity_resume() throws Exception{
 //BA.debugLineNum = 39;BA.debugLine="Sub Activity_Resume";
 //BA.debugLineNum = 40;BA.debugLine="Button1.Enabled = False";
mostCurrent._button1.setEnabled(anywheresoftware.b4a.keywords.Common.False);
 //BA.debugLineNum = 41;BA.debugLine="camera1.Initialize(pnlPreview, False, Me, \"Camera1\")";
mostCurrent._camera1._initialize(mostCurrent.activityBA,mostCurrent._pnlpreview,anywheresoftware.b4a.keywords.Common.False,main.getObject(),"Camera1");
 //BA.debugLineNum = 42;BA.debugLine="If sensor.StartListening(\"sensor\") = False Then";
if (_sensor.StartListening(processBA,"sensor")==anywheresoftware.b4a.keywords.Common.False) { 
 //BA.debugLineNum = 43;BA.debugLine="lblOutput.Text = \"裝置不支援加速感測器...\"";
mostCurrent._lbloutput.setText((Object)("裝置不支援加速感測器..."));
 };
 //BA.debugLineNum = 45;BA.debugLine="End Sub";
return "";
}
public static String  _button1_click() throws Exception{
 //BA.debugLineNum = 72;BA.debugLine="Sub Button1_Click";
 //BA.debugLineNum = 73;BA.debugLine="Button1.Enabled = False";
mostCurrent._button1.setEnabled(anywheresoftware.b4a.keywords.Common.False);
 //BA.debugLineNum = 74;BA.debugLine="camera1.TakePicture()";
mostCurrent._camera1._takepicture();
 //BA.debugLineNum = 75;BA.debugLine="MP.Play()  ' 播放音效";
_mp.Play();
 //BA.debugLineNum = 76;BA.debugLine="End Sub";
return "";
}
public static String  _camera1_picturetaken(byte[] _data) throws Exception{
 //BA.debugLineNum = 64;BA.debugLine="Sub Camera1_PictureTaken(Data() As Byte)";
 //BA.debugLineNum = 66;BA.debugLine="camera1.SavePictureToFile(Data, File.DirRootExternal, \"Pic1.jpg\")";
mostCurrent._camera1._savepicturetofile(_data,anywheresoftware.b4a.keywords.Common.File.getDirRootExternal(),"Pic1.jpg");
 //BA.debugLineNum = 67;BA.debugLine="camera1.StartPreview()  ' 重新啟動預覽";
mostCurrent._camera1._startpreview();
 //BA.debugLineNum = 68;BA.debugLine="ToastMessageShow(\"照片已儲存: \" & File.Combine(File.DirRootExternal, \"Pic.jpg\"), True)";
anywheresoftware.b4a.keywords.Common.ToastMessageShow("照片已儲存: "+anywheresoftware.b4a.keywords.Common.File.Combine(anywheresoftware.b4a.keywords.Common.File.getDirRootExternal(),"Pic.jpg"),anywheresoftware.b4a.keywords.Common.True);
 //BA.debugLineNum = 69;BA.debugLine="Button1.Enabled = True";
mostCurrent._button1.setEnabled(anywheresoftware.b4a.keywords.Common.True);
 //BA.debugLineNum = 70;BA.debugLine="End Sub";
return "";
}
public static String  _camera1_ready(boolean _success) throws Exception{
 //BA.debugLineNum = 52;BA.debugLine="Sub Camera1_Ready(Success As Boolean)";
 //BA.debugLineNum = 53;BA.debugLine="If Success Then";
if (_success) { 
 //BA.debugLineNum = 55;BA.debugLine="camera1.SetJpegQuality(90) ' 圖檔品質";
mostCurrent._camera1._setjpegquality((int)(90));
 //BA.debugLineNum = 56;BA.debugLine="camera1.CommitParameters()";
mostCurrent._camera1._commitparameters();
 //BA.debugLineNum = 57;BA.debugLine="camera1.StartPreview()     ' 開始預覽";
mostCurrent._camera1._startpreview();
 //BA.debugLineNum = 58;BA.debugLine="Button1.Enabled = True";
mostCurrent._button1.setEnabled(anywheresoftware.b4a.keywords.Common.True);
 }else {
 //BA.debugLineNum = 60;BA.debugLine="ToastMessageShow(\"無法開啟相機...\", True)";
anywheresoftware.b4a.keywords.Common.ToastMessageShow("無法開啟相機...",anywheresoftware.b4a.keywords.Common.True);
 };
 //BA.debugLineNum = 62;BA.debugLine="End Sub";
return "";
}

public static void initializeProcessGlobals() {
    
    if (processGlobalsRun == false) {
	    processGlobalsRun = true;
		try {
		        main._process_globals();
		
        } catch (Exception e) {
			throw new RuntimeException(e);
		}
    }
}

public static boolean isAnyActivityVisible() {
    boolean vis = false;
vis = vis | (main.mostCurrent != null);
return vis;}
public static String  _globals() throws Exception{
 //BA.debugLineNum = 20;BA.debugLine="Sub Globals";
 //BA.debugLineNum = 21;BA.debugLine="Dim camera1 As CameraExClass";
mostCurrent._camera1 = new ch16.gui.b4a.example.cameraexclass();
 //BA.debugLineNum = 22;BA.debugLine="Dim pnlPreview As Panel";
mostCurrent._pnlpreview = new anywheresoftware.b4a.objects.PanelWrapper();
 //BA.debugLineNum = 23;BA.debugLine="Dim Button1 As Button";
mostCurrent._button1 = new anywheresoftware.b4a.objects.ButtonWrapper();
 //BA.debugLineNum = 24;BA.debugLine="Dim chkAutoFocus As CheckBox";
mostCurrent._chkautofocus = new anywheresoftware.b4a.objects.CompoundButtonWrapper.CheckBoxWrapper();
 //BA.debugLineNum = 25;BA.debugLine="Dim lblOutput As Label";
mostCurrent._lbloutput = new anywheresoftware.b4a.objects.LabelWrapper();
 //BA.debugLineNum = 26;BA.debugLine="End Sub";
return "";
}
public static String  _process_globals() throws Exception{
 //BA.debugLineNum = 15;BA.debugLine="Sub Process_Globals";
 //BA.debugLineNum = 16;BA.debugLine="Dim MP As MediaPlayer";
_mp = new anywheresoftware.b4a.objects.MediaPlayerWrapper();
 //BA.debugLineNum = 17;BA.debugLine="Dim sensor As PhoneSensors";
_sensor = new anywheresoftware.b4a.phone.Phone.PhoneSensors();
 //BA.debugLineNum = 18;BA.debugLine="End Sub";
return "";
}
public static String  _sensor_sensorchanged(float[] _values) throws Exception{
 //BA.debugLineNum = 78;BA.debugLine="Sub sensor_SensorChanged(Values() As Float)";
 //BA.debugLineNum = 79;BA.debugLine="If Values(0) > 9.0 OR Values(1) > 9.0 Then";
if (_values[(int)(0)]>9.0 || _values[(int)(1)]>9.0) { 
 //BA.debugLineNum = 80;BA.debugLine="lblOutput.Text = \"相機是正的..\"";
mostCurrent._lbloutput.setText((Object)("相機是正的.."));
 }else {
 //BA.debugLineNum = 82;BA.debugLine="lblOutput.Text = \"相機是歪的..\"";
mostCurrent._lbloutput.setText((Object)("相機是歪的.."));
 };
 //BA.debugLineNum = 84;BA.debugLine="End Sub";
return "";
}
}
