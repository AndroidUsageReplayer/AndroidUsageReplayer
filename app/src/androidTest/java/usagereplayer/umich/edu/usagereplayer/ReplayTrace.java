package usagereplayer.umich.edu.usagereplayer;

import android.support.test.uiautomator.Direction;

import java.util.ArrayList;
import java.util.HashMap;


public class ReplayTrace {
    String appName;
    String packageName;
    String name;
    ArrayList<Action> actions;
    public ReplayTrace(){
    }
    public ReplayTrace(String appName, String packageName, String replayName){
        this.appName = appName;
        this.packageName = packageName;
        this.name=replayName;
        this.actions = new ArrayList<Action>();
    }
    public void addAction(Action action){
        actions.add(action);
    }
}

class Action{
    String action;
    HashMap<String, String> findBy;
    boolean measure;
    String text;
    Direction direction;
    public Action(String action, boolean measure){
        this.action=action;
        this.measure=measure;
        findBy= new HashMap<String, String>();
        this.direction=Direction.DOWN;
    }
    public Action(){
    }
    public void setText(String text){
        this.text=text;
    }
    public void setDirection(Direction d){
        this.direction=d;
    }
    public void addFindBy(String key, String value){
        findBy.put(key, value);
    }
}
