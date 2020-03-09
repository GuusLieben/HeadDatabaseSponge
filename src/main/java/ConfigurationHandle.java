import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.spongepowered.api.item.inventory.ItemStack;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ConfigurationHandle {

  private ItemStack[] storedJson;

  public ConfigurationHandle(File file) {
    if (!file.exists()) {
      file.getParentFile().mkdirs();
      try {
        file.createNewFile();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    try {
      FileReader reader = new FileReader(file);
      this.storedJson = new Gson().fromJson(reader, ItemStack[].class);

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public ItemStack[] getCustomHeads() {
    return this.storedJson;
  }
}
