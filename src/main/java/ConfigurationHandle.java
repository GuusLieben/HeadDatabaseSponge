import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ConfigurationHandle {

  private JsonObject storedJson;

  public ConfigurationHandle(File file) {
    boolean fileExisted = true;

    if (!file.exists()) {
      file.getParentFile().mkdirs();
      try {
        file.createNewFile();
      } catch (IOException e) {
        e.printStackTrace();
      }
      fileExisted = false;
    }

    if (!fileExisted) {
      // Write the default values
    }

    try {
      String triggerJson = new String(Files.readAllBytes(Paths.get(file.getPath())), "UTF-8");
      storedJson = (JsonObject) new JsonParser().parse(triggerJson);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public JsonElement getJsonElement(String key) {
    return storedJson.get(key);
  }
}
