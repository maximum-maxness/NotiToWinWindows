package controller;

import org.json.JSONObject;

import javax.swing.*;
import java.util.Iterator;

public class Notification {

    private boolean isClearable, isRepliable, hasDataLoad;
    private String id, appName, title, text, dataLoadHash, requestReplyId;
    private Icon ico; //TODO Proper Icon integration
    private long timeStamp;


    public Notification() {
        this.isClearable = false;
        this.isRepliable = false;
        this.hasDataLoad = false;
        this.id = "";
        this.appName = "";
        this.title = "";
        this.text = "";
        this.dataLoadHash = "";
        this.requestReplyId = "";
        this.timeStamp = 0L;
    }

    public boolean isClearable() {
        return isClearable;
    }

    public void setClearable(boolean clearable) {
        isClearable = clearable;
    }

    public boolean isRepliable() {
        return isRepliable;
    }

    public void setRepliable(boolean repliable) {
        isRepliable = repliable;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getDataLoadHash() {
        return dataLoadHash;
    }

    public void setDataLoadHash(String dataLoadHash) {
        this.dataLoadHash = dataLoadHash;
    }

    public String getRequestReplyId() {
        return requestReplyId;
    }

    public void setRequestReplyId(String requestReplyId) {
        this.requestReplyId = requestReplyId;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public void setTimeStamp(String timeStamp) {
        this.timeStamp = Long.parseLong(timeStamp);
    }

    public boolean isHasDataLoad() {
        return hasDataLoad;
    }

    public void setHasDataLoad(boolean hasDataLoad) {
        this.hasDataLoad = hasDataLoad;
    }

    public boolean isValidNoti() {
        return this.id != "" && this.appName != "" && this.title != "" && this.timeStamp != 0L;
    }

    public static Notification jsonToNoti(JSONConverter json){
        Notification noti = new Notification();
        JSONObject jsonOb = json.getMainBody();
        Iterator<String> iter = jsonOb.keys();
        while (iter.hasNext()) {
            String key = iter.next();
            switch (key) {
                case "id":
                    noti.setId((String) jsonOb.get(key));
                    break;
                case "isClearable":
                    noti.setClearable((boolean) jsonOb.get(key));
                    break;
                case "appName":
                    noti.setAppName((String) jsonOb.get(key));
                    break;
                case "time":
                    noti.setTimeStamp((String) jsonOb.get(key));
                    break;
                case "title":
                    noti.setTitle((String) jsonOb.get(key));
                    break;
                case "text":
                    noti.setText((String) jsonOb.get(key));
                    break;
                case "isRepliable":
                    noti.setRepliable((boolean) jsonOb.get(key));
                    break;
                case "requestReplyId":
                    noti.setRequestReplyId((String) jsonOb.get(key));
                    break;
                case "hasDataLoad":
                    noti.setHasDataLoad((boolean) jsonOb.get(key));
                    break;
                case "dataLoadHash":
                    noti.setDataLoadHash((String) jsonOb.get(key));
                    break;
                default:
                    System.err.println("Key: \"" + key + "\" isn't a notification key.");
            }
        }
        return noti;
    }
}
