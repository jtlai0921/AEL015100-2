package ch14.gui.b4a.example;

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
			processBA = new BA(this.getApplicationContext(), null, null, "ch14.gui.b4a.example", "ch14.gui.b4a.example.main");
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
		activityBA = new BA(this, layout, processBA, "ch14.gui.b4a.example", "ch14.gui.b4a.example.main");
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
public static anywheresoftware.b4a.sql.SQL _sql1 = null;
public static anywheresoftware.b4a.sql.SQL.CursorWrapper _cursor1 = null;
public anywheresoftware.b4a.objects.EditTextWrapper _edtid = null;
public anywheresoftware.b4a.objects.EditTextWrapper _edtnewprice = null;
public anywheresoftware.b4a.objects.EditTextWrapper _edtprice = null;
public anywheresoftware.b4a.objects.EditTextWrapper _edtsql = null;
public anywheresoftware.b4a.objects.EditTextWrapper _edttitle = null;
public anywheresoftware.b4a.objects.ListViewWrapper _lsvoutput = null;
public static String  _activity_create(boolean _firsttime) throws Exception{
 //BA.debugLineNum = 29;BA.debugLine="Sub Activity_Create(FirstTime As Boolean)";
 //BA.debugLineNum = 30;BA.debugLine="If File.Exists(File.DirInternal, \"MyBooks.sqlite\") = False Then";
if (anywheresoftware.b4a.keywords.Common.File.Exists(anywheresoftware.b4a.keywords.Common.File.getDirInternal(),"MyBooks.sqlite")==anywheresoftware.b4a.keywords.Common.False) { 
 //BA.debugLineNum = 31;BA.debugLine="File.Copy(File.DirAssets, \"MyBooks.sqlite\",File.DirInternal, \"MyBooks.sqlite\")";
anywheresoftware.b4a.keywords.Common.File.Copy(anywheresoftware.b4a.keywords.Common.File.getDirAssets(),"MyBooks.sqlite",anywheresoftware.b4a.keywords.Common.File.getDirInternal(),"MyBooks.sqlite");
 };
 //BA.debugLineNum = 33;BA.debugLine="If SQL1.IsInitialized() = False Then";
if (_sql1.IsInitialized()==anywheresoftware.b4a.keywords.Common.False) { 
 //BA.debugLineNum = 34;BA.debugLine="SQL1.Initialize(File.DirInternal, \"MyBooks.sqlite\", False)";
_sql1.Initialize(anywheresoftware.b4a.keywords.Common.File.getDirInternal(),"MyBooks.sqlite",anywheresoftware.b4a.keywords.Common.False);
 };
 //BA.debugLineNum = 36;BA.debugLine="Activity.LoadLayout(\"Main\")";
mostCurrent._activity.LoadLayout("Main",mostCurrent.activityBA);
 //BA.debugLineNum = 37;BA.debugLine="Activity.Title = \"行動圖書館\"";
mostCurrent._activity.setTitle((Object)("行動圖書館"));
 //BA.debugLineNum = 38;BA.debugLine="edtId.Text = \"M0003\"";
mostCurrent._edtid.setText((Object)("M0003"));
 //BA.debugLineNum = 39;BA.debugLine="edtTitle.Text = \"PHP+MySQL與jQuery Mobile跨行動裝置網站開發\"";
mostCurrent._edttitle.setText((Object)("PHP+MySQL與jQuery Mobile跨行動裝置網站開發"));
 //BA.debugLineNum = 40;BA.debugLine="edtPrice.Text = \"560\"";
mostCurrent._edtprice.setText((Object)("560"));
 //BA.debugLineNum = 41;BA.debugLine="edtNewPrice.Text = \"580\"";
mostCurrent._edtnewprice.setText((Object)("580"));
 //BA.debugLineNum = 42;BA.debugLine="End Sub";
return "";
}
public static String  _activity_pause(boolean _userclosed) throws Exception{
 //BA.debugLineNum = 48;BA.debugLine="Sub Activity_Pause(UserClosed As Boolean)";
 //BA.debugLineNum = 50;BA.debugLine="End Sub";
return "";
}
public static String  _activity_resume() throws Exception{
 //BA.debugLineNum = 44;BA.debugLine="Sub Activity_Resume";
 //BA.debugLineNum = 46;BA.debugLine="End Sub";
return "";
}
public static String  _button1_click() throws Exception{
String _bid = "";
String _title = "";
int _price = 0;
String _strsql = "";
int _dbchange = 0;
 //BA.debugLineNum = 52;BA.debugLine="Sub Button1_Click";
 //BA.debugLineNum = 53;BA.debugLine="Dim bid As String = edtId.Text";
_bid = mostCurrent._edtid.getText();
 //BA.debugLineNum = 54;BA.debugLine="Dim title As String = edtTitle.Text";
_title = mostCurrent._edttitle.getText();
 //BA.debugLineNum = 55;BA.debugLine="Dim price As Int = edtPrice.Text";
_price = (int)(Double.parseDouble(mostCurrent._edtprice.getText()));
 //BA.debugLineNum = 56;BA.debugLine="Dim strSQL As String";
_strsql = "";
 //BA.debugLineNum = 57;BA.debugLine="strSQL = \"INSERT INTO Books(id,title,price)\" & _              \"VALUES('\" & bid & \"','\" & title & \"',\" & price & \")\"";
_strsql = "INSERT INTO Books(id,title,price)"+"VALUES('"+_bid+"','"+_title+"',"+BA.NumberToString(_price)+")";
 //BA.debugLineNum = 59;BA.debugLine="SQL1.ExecNonQuery(strSQL)";
_sql1.ExecNonQuery(_strsql);
 //BA.debugLineNum = 60;BA.debugLine="Dim dbChange As Int";
_dbchange = 0;
 //BA.debugLineNum = 61;BA.debugLine="dbChange = SQL1.ExecQuerySingleResult(\"SELECT changes() FROM Books\")";
_dbchange = (int)(Double.parseDouble(_sql1.ExecQuerySingleResult("SELECT changes() FROM Books")));
 //BA.debugLineNum = 62;BA.debugLine="ToastMessageShow(\"新增圖書記錄: \" & dbChange & \" 筆\", False)";
anywheresoftware.b4a.keywords.Common.ToastMessageShow("新增圖書記錄: "+BA.NumberToString(_dbchange)+" 筆",anywheresoftware.b4a.keywords.Common.False);
 //BA.debugLineNum = 63;BA.debugLine="End Sub";
return "";
}
public static String  _button2_click() throws Exception{
String _bid = "";
String _strsql = "";
 //BA.debugLineNum = 65;BA.debugLine="Sub Button2_Click";
 //BA.debugLineNum = 66;BA.debugLine="Dim bid As String = edtId.Text";
_bid = mostCurrent._edtid.getText();
 //BA.debugLineNum = 67;BA.debugLine="Dim strSQL As String";
_strsql = "";
 //BA.debugLineNum = 68;BA.debugLine="strSQL = \"UPDATE Books SET price ='\" & _          edtNewPrice.Text & \"' WHERE id = '\" & bid & \"'\"";
_strsql = "UPDATE Books SET price ='"+mostCurrent._edtnewprice.getText()+"' WHERE id = '"+_bid+"'";
 //BA.debugLineNum = 70;BA.debugLine="SQL1.ExecNonQuery(strSQL)";
_sql1.ExecNonQuery(_strsql);
 //BA.debugLineNum = 71;BA.debugLine="ToastMessageShow(\"更新一筆圖書記錄...\", False)";
anywheresoftware.b4a.keywords.Common.ToastMessageShow("更新一筆圖書記錄...",anywheresoftware.b4a.keywords.Common.False);
 //BA.debugLineNum = 72;BA.debugLine="End Sub";
return "";
}
public static String  _button3_click() throws Exception{
String _bid = "";
String _strsql = "";
 //BA.debugLineNum = 74;BA.debugLine="Sub Button3_Click";
 //BA.debugLineNum = 75;BA.debugLine="Dim bid As String = edtId.Text";
_bid = mostCurrent._edtid.getText();
 //BA.debugLineNum = 76;BA.debugLine="Dim strSQL As String";
_strsql = "";
 //BA.debugLineNum = 77;BA.debugLine="strSQL = \"DELETE FROM Books WHERE id = '\" & bid & \"'\"";
_strsql = "DELETE FROM Books WHERE id = '"+_bid+"'";
 //BA.debugLineNum = 78;BA.debugLine="SQL1.ExecNonQuery(strSQL)";
_sql1.ExecNonQuery(_strsql);
 //BA.debugLineNum = 79;BA.debugLine="ToastMessageShow(\"刪除一筆圖書記錄...\", False)";
anywheresoftware.b4a.keywords.Common.ToastMessageShow("刪除一筆圖書記錄...",anywheresoftware.b4a.keywords.Common.False);
 //BA.debugLineNum = 80;BA.debugLine="End Sub";
return "";
}
public static String  _button4_click() throws Exception{
 //BA.debugLineNum = 82;BA.debugLine="Sub Button4_Click";
 //BA.debugLineNum = 83;BA.debugLine="queryDB(\"SELECT * FROM Books\")";
_querydb("SELECT * FROM Books");
 //BA.debugLineNum = 84;BA.debugLine="End Sub";
return "";
}
public static String  _button5_click() throws Exception{
 //BA.debugLineNum = 86;BA.debugLine="Sub Button5_Click";
 //BA.debugLineNum = 87;BA.debugLine="queryDB(edtSQL.Text)";
_querydb(mostCurrent._edtsql.getText());
 //BA.debugLineNum = 88;BA.debugLine="End Sub";
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
 //BA.debugLineNum = 21;BA.debugLine="Dim edtId As EditText";
mostCurrent._edtid = new anywheresoftware.b4a.objects.EditTextWrapper();
 //BA.debugLineNum = 22;BA.debugLine="Dim edtNewPrice As EditText";
mostCurrent._edtnewprice = new anywheresoftware.b4a.objects.EditTextWrapper();
 //BA.debugLineNum = 23;BA.debugLine="Dim edtPrice As EditText";
mostCurrent._edtprice = new anywheresoftware.b4a.objects.EditTextWrapper();
 //BA.debugLineNum = 24;BA.debugLine="Dim edtSQL As EditText";
mostCurrent._edtsql = new anywheresoftware.b4a.objects.EditTextWrapper();
 //BA.debugLineNum = 25;BA.debugLine="Dim edtTitle As EditText";
mostCurrent._edttitle = new anywheresoftware.b4a.objects.EditTextWrapper();
 //BA.debugLineNum = 26;BA.debugLine="Dim lsvOutput As ListView";
mostCurrent._lsvoutput = new anywheresoftware.b4a.objects.ListViewWrapper();
 //BA.debugLineNum = 27;BA.debugLine="End Sub";
return "";
}
public static String  _process_globals() throws Exception{
 //BA.debugLineNum = 15;BA.debugLine="Sub Process_Globals";
 //BA.debugLineNum = 16;BA.debugLine="Dim SQL1 As SQL";
_sql1 = new anywheresoftware.b4a.sql.SQL();
 //BA.debugLineNum = 17;BA.debugLine="Dim cursor1 As Cursor";
_cursor1 = new anywheresoftware.b4a.sql.SQL.CursorWrapper();
 //BA.debugLineNum = 18;BA.debugLine="End Sub";
return "";
}
public static String  _querydb(String _strsql) throws Exception{
int _i = 0;
 //BA.debugLineNum = 90;BA.debugLine="Sub queryDB(strSQL As String)";
 //BA.debugLineNum = 91;BA.debugLine="lsvOutput.Clear()";
mostCurrent._lsvoutput.Clear();
 //BA.debugLineNum = 92;BA.debugLine="cursor1 = SQL1.ExecQuery(strSQL)";
_cursor1.setObject((android.database.Cursor)(_sql1.ExecQuery(_strsql)));
 //BA.debugLineNum = 93;BA.debugLine="lsvOutput.SingleLineLayout.Label.TextSize = 10";
mostCurrent._lsvoutput.getSingleLineLayout().Label.setTextSize((float)(10));
 //BA.debugLineNum = 94;BA.debugLine="lsvOutput.SingleLineLayout.ItemHeight = 25dip";
mostCurrent._lsvoutput.getSingleLineLayout().setItemHeight(anywheresoftware.b4a.keywords.Common.DipToCurrent((int)(25)));
 //BA.debugLineNum = 95;BA.debugLine="For i = 0 To cursor1.RowCount - 1";
{
final double step66 = 1;
final double limit66 = (int)(_cursor1.getRowCount()-1);
for (_i = (int)(0); (step66 > 0 && _i <= limit66) || (step66 < 0 && _i >= limit66); _i += step66) {
 //BA.debugLineNum = 96;BA.debugLine="cursor1.Position = i";
_cursor1.setPosition(_i);
 //BA.debugLineNum = 97;BA.debugLine="lsvOutput.AddSingleLine(cursor1.GetString(\"id\") & _                       \" : \" & cursor1.GetString(\"title\")& _                       \" \" & cursor1.GetLong(\"price\"))";
mostCurrent._lsvoutput.AddSingleLine(_cursor1.GetString("id")+" : "+_cursor1.GetString("title")+" "+BA.NumberToString(_cursor1.GetLong("price")));
 }
};
 //BA.debugLineNum = 101;BA.debugLine="End Sub";
return "";
}
}
