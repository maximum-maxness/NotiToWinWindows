package server.processing;

import backend.Notification;
import javafx.scene.control.TreeItem;

public class NotiToTree {
  public static TreeItem<String> convert(Notification notification) {
    if (notification.isValidNoti()) {
      TreeItem<String> root = new TreeItem<>();
      root.setValue("ID: " + notification.getId());

      TreeItem<String> isValid = new TreeItem<>("Is Valid?");
      TreeItem<String> appName = new TreeItem<>("App Name");
      TreeItem<String> title = new TreeItem<>("Title");
      TreeItem<String> text = new TreeItem<>("Text");
      TreeItem<String> isClearable = new TreeItem<>("Is Clearable?");
      TreeItem<String> isRepliable = new TreeItem<>("Is Repliable?");
      TreeItem<String> hasDataLoad = new TreeItem<>("Has DataLoad?");
      TreeItem<String> timeStamp = new TreeItem<>("Time Stamp");

      root.getChildren()
          .addAll(isValid, appName, title, text, isClearable, isRepliable, hasDataLoad, timeStamp);

      TreeItem<String> valiBool = new TreeItem<>("true");
      isValid.getChildren().add(valiBool);

      TreeItem<String> appNameStr = new TreeItem<>(notification.getAppName());
      appName.getChildren().add(appNameStr);
      TreeItem<String> titleStr = new TreeItem<>(notification.getTitle());
      title.getChildren().add(titleStr);
      TreeItem<String> textStr = new TreeItem<>(notification.getText());
      text.getChildren().add(textStr);

      String s = "false";
      if (notification.isClearable()) {
        s = "true";
      }
      TreeItem<String> clearableBool = new TreeItem<>(s);
      isClearable.getChildren().add(clearableBool);

      TreeItem<String> replyBool;
      if (notification.isRepliable()) {
        replyBool = new TreeItem<>("true");
        TreeItem<String> replyId = new TreeItem<>("Reply ID");
        TreeItem<String> replyIdStr = new TreeItem<>(notification.getRequestReplyId());
        replyId.getChildren().add(replyIdStr);
        isRepliable.getChildren().addAll(replyBool, replyId);
      } else {
        replyBool = new TreeItem<>("false");
        isRepliable.getChildren().add(replyBool);
      }

      TreeItem<String> dataBool;
      if (notification.isHasDataLoad()) {
        dataBool = new TreeItem<>("true");
        TreeItem<String> dataHash = new TreeItem<>("DataLoad Hash");
        TreeItem<String> dataHashStr = new TreeItem<>(notification.getDataLoadHash());
        dataHash.getChildren().add(dataHashStr);
        TreeItem<String> dataSize = new TreeItem<>("DataLoad Size:");
        TreeItem<String> dataSizeStr =
            new TreeItem<>(Long.toString(notification.getDataLoadSize()));
        dataSize.getChildren().add(dataSizeStr);
        hasDataLoad.getChildren().addAll(dataBool, dataHash, dataSize);
      } else {
        dataBool = new TreeItem<>("false");
        hasDataLoad.getChildren().add(dataBool);
      }

      TreeItem<String> timeStampStr = new TreeItem<>(Long.toString(notification.getTimeStamp()));
      timeStamp.getChildren().add(timeStampStr);

      return root;
    } else {
      TreeItem<String> root = new TreeItem<>("Not a valid Notification");
      return root;
    }
  }

  public static void expandTreeView(TreeItem<?> item) {
    if (item != null && !item.isLeaf()) {
      item.setExpanded(true);
      for (TreeItem<?> child : item.getChildren()) {
        expandTreeView(child);
      }
    }
  }
}
