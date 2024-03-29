package backend;

import org.json.JSONObject;
import runner.NotiCardHelper;

import javax.swing.*;
import java.io.*;
import java.util.Iterator;
import java.util.Objects;

public class Notification {

    private boolean isClearable, isRepliable, hasDataLoad;
    private String id, appName, title, text, dataLoadHash, requestReplyId, clientID;

    private File icon;
    private Icon ico; // TODO Proper Icon integration
    private long timeStamp, dataLoadSize;

    public Notification() {
        this.isClearable = false;
        this.isRepliable = false;
        this.hasDataLoad = false;
        this.clientID = "";
        this.id = "";
        this.appName = "";
        this.title = "";
        this.text = "";
        this.dataLoadHash = "";
        this.requestReplyId = "";
        this.timeStamp = 0L;
    }

    public static Notification jsonToNoti(JSONConverter json, String clientID) {
        Notification noti = new Notification();
        noti.clientID = clientID;
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
                case "dataLoadSize":
                    noti.setDataLoadSize(Integer.toUnsignedLong((int) jsonOb.get(key)));
                    break;
                default:
                    System.err.println("Key: \"" + key + "\" isn't a notification key.");
            }
        }
        if (json.getDataLoad() != null)
            noti.setIcon(recieveDataLoad(json.getDataLoad(), (noti.getAppName() + "." + noti.getTimeStamp() + "")));
        return noti;
    }

    public String getClientID() {
        return clientID;
    }

    public static File recieveDataLoad(DataLoad dataLoad, String name) { // TODO
        System.out.println("Processing DataLoad!");
        try {
            int bytesRead;
            long size = dataLoad.getSize();
            String tmpDirectoryOp = System.getProperty("java.io.tmpdir");
            File tmpDirectory = new File(tmpDirectoryOp);
            File fstream = File.createTempFile(name, ".bmp", tmpDirectory);
            FileOutputStream output = new FileOutputStream(fstream);

            byte[] buffer = new byte[1024];
            while (size > 0
                    && (bytesRead = dataLoad.getInputStream().read(buffer, 0, (int) Math.min(buffer.length, size)))
                    != -1) {
                output.write(buffer, 0, bytesRead);
                size -= bytesRead;
            }
            output.close();
            dataLoad.getInputStream().close();
            fstream.deleteOnExit();
            return fstream;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public InputStream getIconInputStream() {
        try {
            return (icon == null)
                    ? getClass().getResourceAsStream("/ui/res/ic_launcher_round.png")
                    : new FileInputStream(icon);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    public File getIcon() {
        return icon;
    }

    public void setIcon(File icon) {
        this.icon = icon;
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
        return !Objects.equals(this.id, "")
                && !Objects.equals(this.appName, "")
                && !Objects.equals(this.title, "")
                && this.timeStamp != 0L;
    }

    @Override
    public String toString() {
        StringBuilder returnString = new StringBuilder();
        returnString.append("Notification is Valid? ").append(isValidNoti()).append("\n");
        if (isValidNoti()) {
            returnString.append("Notification ID: ").append(getId()).append("\n");
            returnString.append("Notification TimeStamp: ").append(getTimeStamp()).append("\n");
            returnString.append("Notification AppName: ").append(getAppName()).append("\n");
            returnString.append("Notification Title: ").append(getTitle()).append("\n");
            returnString.append("Notification Text: ").append(getText()).append("\n");
            returnString.append("Notification is Clearable: ").append(isClearable()).append("\n");
            returnString.append("Notification is Repliable: ").append(isRepliable()).append("\n");
            if (isRepliable())
                returnString.append("Notification Reply ID: ").append(getRequestReplyId()).append("\n");
            returnString.append("Notification has DataLoad: ").append(isHasDataLoad()).append("\n");
            if (isHasDataLoad())
                returnString.append("Notification DataLoad Hash: ").append(getDataLoadHash()).append("\n");
        }
        return returnString.toString();
    }

    public long getDataLoadSize() {
        return dataLoadSize;
    }

    public void setDataLoadSize(long dataLoadSize) {
        this.dataLoadSize = dataLoadSize;
    }

    public void display() {
        NotiCardHelper.showNotification(this);
    }
}
