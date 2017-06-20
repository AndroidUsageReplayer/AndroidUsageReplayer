package usagereplayer.umich.edu.usagereplayer;

import android.content.Intent;
import android.graphics.Rect;
import android.os.Environment;
import android.os.RemoteException;
import android.os.SystemClock;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.BySelector;
import android.support.test.uiautomator.Direction;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiObject2;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.UiScrollable;
import android.support.test.uiautomator.UiSelector;
import android.support.test.uiautomator.Until;
import android.util.JsonReader;
import android.util.Log;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;

import usagereplayer.umich.edu.usagereplayer.ReplayTrace;


@RunWith(AndroidJUnit4.class)
public class Replayer {
    private static final String TAG = "UIAutomator.Replayer";
    private static final String CONFIG_FILE_NAME = "usages_list";
    private static final long UI_TIMEOUT = 5000;
    private static final long INPUT_DELAY = 60*1000;
    private UiDevice mDevice;


    @Test
    public void init() throws IOException, RemoteException, UiObjectNotFoundException, InterruptedException {
        mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        mDevice.wakeUp();
        ArrayList<String> usages=readConfig();
        for(String usageJsonFile: usages){
            ReplayTrace rt=parseJson(usageJsonFile);
            clearCache(rt.appName);
            lunchAppByIntent(rt.packageName);
            SystemClock.sleep(INPUT_DELAY);
            runActions(rt);
            clearCache(rt.appName);

        }
//
        //read usage files
        //run usages




    }

    private void runActions(ReplayTrace rt) throws UiObjectNotFoundException, InterruptedException {
        for(Action a: rt.actions){
            Log.d(TAG, "<<<"+ a.action);
            if (a.action.equals("click")){
                BySelector selector=generateBySelector(a.findBy, rt.packageName);
                Assert.assertTrue(mDevice.wait(Until.hasObject(selector), UI_TIMEOUT));
                UiObject2 button = mDevice.findObject(selector);
                button.click();
                if(a.measure) {
                    recordInput("click", rt.packageName);
                }

            }else if(a.action.equals("scroll")){
                BySelector selector=generateBySelector(a.findBy, rt.packageName);
                UiObject2 scrollable = mDevice.findObject(selector);
                Rect visBounds= scrollable.getVisibleBounds();
                mDevice.drag(visBounds.centerX(), visBounds.centerY(),visBounds.centerX(), visBounds.top, 10);
                if(a.measure) {
                    recordInput("scroll", rt.packageName);
                }
            }else if(a.action.equals("search")){
                BySelector selector=generateBySelector(a.findBy, rt.packageName);
                Assert.assertTrue(mDevice.wait(Until.hasObject(selector), UI_TIMEOUT));
                UiObject2 editText = mDevice.findObject(selector);
                editText.setText(a.text);
                mDevice.pressEnter();
                if(a.measure) {
                    recordInput("search", rt.packageName);
                }

            }else{
                assert false;
            }
            SystemClock.sleep(INPUT_DELAY);
        }
    }

    private BySelector generateBySelector(HashMap<String, String> selectors, String packageName){
        BySelector selector = By.pkg(packageName);
        for (String key : selectors.keySet()) {
            if(key.equals("text")){
                selector.text(selectors.get(key));
            }else if(key.equals("class")){
                selector.clazz(selectors.get(key));
            }else if(key.equals("desc")){
                selector.desc(selectors.get(key));
            }else if(key.equals("package")){
                selector.pkg(selectors.get(key));
            }else if(key.equals("id")){
                selector.res(selectors.get(key));
            }else{
                assert false;
            }
        }
        return  selector;
    }



    private ArrayList<String> readConfig() throws IOException {
        ArrayList<String> usages = new ArrayList<>();
        BufferedReader br = new BufferedReader(new FileReader(new File(Environment.getExternalStorageDirectory(),CONFIG_FILE_NAME)));
        String line;
        while ((line = br.readLine()) != null) {
            usages.add(line);
        }
        return usages;
    }

    private ReplayTrace parseJson(String  file) throws IOException, UnsupportedEncodingException {
        InputStream is = new FileInputStream(new File(Environment.getExternalStorageDirectory(),"Replayer/"+file));
        JsonReader reader = new JsonReader(new InputStreamReader(is, "UTF-8"));
        ReplayTrace replayTrace = new ReplayTrace();
        reader.beginObject();
        String appName="";
        String packageName="";
        String replayName="";
        while (reader.hasNext()) {
            String key = reader.nextName();
            ;
            if (key.equals("app")) {
                appName = reader.nextString();
            } else if (key.equals("package")) {
                packageName = reader.nextString();
            } else if (key.equals("name")) {
                replayName = reader.nextString();
                replayTrace = new ReplayTrace(appName, packageName, replayName);
            } else if (key.equals("actions")) {
                reader.beginArray();
                while (reader.hasNext()) {
                    Action action = new Action();
                    boolean measure = false;
                    String actionName = "";
                    reader.beginObject();
                    while (reader.hasNext()) {
                        String actionKey = reader.nextName();
                        if (actionKey.equals("action")) {
                            actionName = reader.nextString();
                        } else if (actionKey.equals("findby")) {
                            reader.beginObject();
                            while (reader.hasNext()) {
                                String findbykey = reader.nextName();
                                String findbyvalue = reader.nextString();
                                action.addFindBy(findbykey, findbyvalue);
                            }
                            reader.endObject();
                        } else if (actionKey.equals("measure")) {
                            measure = reader.nextBoolean();
                            action = new Action(actionName, measure);
                        } else if (actionKey.equals("text")) {
                            action.setText(reader.nextString());
                        } else if (actionKey.equals("direction")) {
                            action.setDirection(reader.nextString().equals("UP")? Direction.UP:Direction.DOWN);
                        }
                    }
                    reader.endObject();
                    replayTrace.addAction(action);

                }
                reader.endArray();


            }else {
                reader.skipValue();
            }
        }
        reader.endObject();
        return replayTrace;
    }

    private void lunchAppByIntent(String packageName){
        Intent LaunchIntent = InstrumentationRegistry.getContext().getPackageManager().getLaunchIntentForPackage(packageName);
        InstrumentationRegistry.getContext().startActivity( LaunchIntent );
        recordInput("lunch", packageName);

    }

    private void clearCache(String appName) throws UiObjectNotFoundException {
        mDevice.pressHome();
        mDevice.wait(Until.hasObject(By.desc("Apps")), UI_TIMEOUT);
        UiObject2 appsButton = mDevice.findObject(By.desc("Apps"));
        appsButton.click();

        new UiScrollable(new UiSelector().scrollable(false)).scrollIntoView(new UiSelector().text("Settings"));
        UiObject2 settingsIcon = mDevice.findObject(By.desc("Settings"));
        settingsIcon.click();



        new UiScrollable(new UiSelector().scrollable(false)).scrollIntoView(new UiSelector().text("Apps"));
        try {
            new UiObject(new UiSelector().text("Apps")).click();
        }catch(UiObjectNotFoundException e1){
            new UiScrollable(new UiSelector().scrollable(true)).scrollIntoView(new UiSelector().text("Apps"));
            try {
                new UiObject(new UiSelector().text("Apps")).click();
            }catch (UiObjectNotFoundException e2){

            }
        }

        new UiScrollable(new UiSelector().scrollable(true)).scrollIntoView(new UiSelector().text(appName));
        new UiObject(new UiSelector().text(appName)).click();

        UiObject2 forceStopButton = mDevice.findObject(By.text("Force stop"));
        if(forceStopButton.isEnabled()){
            forceStopButton.click();

            mDevice.wait(Until.hasObject(By.desc("OK")), UI_TIMEOUT);
            UiObject2 okForceStopButton = mDevice.findObject(By.text("OK"));
            okForceStopButton.click();
        }
        mDevice.wait(Until.hasObject(By.desc("Clear cache")), UI_TIMEOUT);
        UiObject2 clearCacheButton = mDevice.findObject(By.text("Clear cache"));
        clearCacheButton.click();

    }

    private void recordInput(String action, String appName){
        try{
            File root = new File(Environment.getExternalStorageDirectory(), "ReplayerResults");

            if (!root.exists()) {
                root.mkdirs();
            }
            File file = new File(root, "input_timings.txt");

            BufferedWriter bW = new BufferedWriter(new FileWriter(file,true));
            bW.write(appName+" "+action+" "+System.currentTimeMillis()+"\n");
            bW.flush();
            bW.close();
        }
        catch(IOException e){
            e.printStackTrace();
        }
    }

}
