import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class Main {

  public static void main(String[] args) {
    Gson gson = new Gson();
    JsonObject array =
        (JsonObject)
            new JsonParser().parse("{\"tags\":\"Avatar: The last Airbender,PetPlugin,Winter\"}");
    System.out.println(array.get("tags").getAsString());
  }
}
