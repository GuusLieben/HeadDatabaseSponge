package nl.guuslieben.headsevolved;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Inject;

import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.plugin.meta.util.NonnullByDefault;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Set;

@Plugin(
        id = "headsevolved",
        name = "nl.guuslieben.headsevolved.HeadsEvolved",
        version = "1.0.10",
        description = "Stores custom heads for Darwin Reforged")
public class HeadsEvolved {

  private static HeadsEvolved singleton;
  private static boolean obtained = false;
  private static boolean initiated = true;

  @Listener
  public void onServerFinishLoad(GameInitializationEvent event) {
    try {
      Class.forName("com.mcsimonflash.sponge.teslalibs.inventory.Layout");
      Sponge.getCommandManager().register(this, hdbMain, "heads", "he", "hdb");
    } catch (ClassNotFoundException e) {
      logger.error("Could not find TeslaLibs! Not registering commands");
      initiated = false;
    }
  }

  @Inject
  public Logger logger;

  @Listener
  public void onServerStart(GameStartedServerEvent event) {
    if (initiated) {
      singleton = this;
      logger.info("Starting head database request task");
      Sponge.getScheduler().createTaskBuilder().async().name("he_obtain").execute(() -> {
        try {
          collectHeadsFromAPI();
        } catch (IOException e) {
          logger.debug("Failed to get head.");
        }
      }).submit(this);
    }
  }

  static String apiLine = "https://minecraft-heads.com/scripts/api.php?tags=true&cat=";

  static String[] uuidBlacklist = {
    "c7299fa8d44b "
  }; // UUID's causing NumberFormatExceptions, currently only one exists in the entire database

  private void collectHeadsFromAPI() throws IOException {
    logger.debug("(async) Obtaining heads...");
    int totalHeads = 0;
    for (HeadObject.Category cat : HeadObject.Category.values()) {
      String connectionLine = apiLine + cat.toString().toLowerCase().replaceAll("_", "-");
      JsonArray array = readJsonFromUrl(connectionLine);
      logger.debug(cat.toString() + " : " + array.size());

      for (Object head : array) {
        if (head instanceof JsonObject) {
          JsonElement nameEl = ((JsonObject) head).get("name");
          JsonElement uuidEl = ((JsonObject) head).get("uuid");
          JsonElement valueEl = ((JsonObject) head).get("value");
          JsonElement tagsEl = ((JsonObject) head).get("tags");

          String name = nameEl.getAsString();
          String uuid = uuidEl.getAsString();
          String value = valueEl.getAsString();
          String tags = tagsEl instanceof JsonNull ? "None" : tagsEl.getAsString();

          boolean doAdd = true;
          for (String blackedUUID : uuidBlacklist) if (blackedUUID.equals(uuid)) doAdd = false;

          if (doAdd) {
            new HeadObject(name, uuid, value, tags, cat);
            totalHeads++;
          }
        }
      }
    }

    logger.debug("\nCollected : " + totalHeads + " heads from MinecraftHeadDB");
    obtained = true;
  }

  private String readAll(Reader rd) throws IOException {
    StringBuilder sb = new StringBuilder();
    int cp;
    while ((cp = rd.read()) != -1) {
      sb.append((char) cp);
    }
    return sb.toString();
  }

  public JsonArray readJsonFromUrl(String url) throws IOException {
    try (InputStream is = new URL(url).openStream()) {
      BufferedReader rd = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
      String jsonText = readAll(rd);
      return (JsonArray) new JsonParser().parse(jsonText);
    }
  }

  private CommandSpec hdbOpen =
      CommandSpec.builder()
          .description(Text.of("Opens Head Evo GUI"))
          .permission("he.open")
          .executor(new openInventory())
          .build();

  private CommandSpec hdbSearch =
          CommandSpec.builder()
                  .description(Text.of("Searches for heads with matching tags or name"))
                  .permission("he.find")
          .arguments(GenericArguments.onlyOne(GenericArguments.string(Text.of("query"))))
          .executor(new searchHeads())
          .build();

  private CommandSpec hdbMain =
      CommandSpec.builder()
          .description(Text.of("Main command"))
          .permission("he.open")
          .child(hdbOpen, "open")
          .child(hdbSearch, "find", "search")
          .build();

  private static class searchHeads implements CommandExecutor {

    @Override
    @NonnullByDefault
    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
      if (!obtained)
        src.sendMessage(Text.of(TextColors.GRAY, "// ", TextColors.RED, "Still obtaining heads from database, please wait.."));
      else if (src instanceof Player) {
        Player player = (Player) src;
        String query = args.<String>getOne("query").get();
        if (query != "") {
          Set<HeadObject> headObjects = HeadObject.getByNameAndTag(query);
          ChestObject.openViewForSet(headObjects, player, "$search");
        }
      }
      return CommandResult.success();
    }
  }

  private static class openInventory implements CommandExecutor {

    @Override
    @NonnullByDefault
    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
      if (!obtained)
        src.sendMessage(Text.of(TextColors.GRAY, "// ", TextColors.RED, "Still obtaining heads from database, please wait.."));
      else if (src instanceof Player) {
        Player player = (Player) src;
        try {
          new ChestObject(player);
        } catch (InstantiationException e) {
          player.sendMessage(
                  Text.of(TextColors.GRAY, "// ", TextColors.RED, "Failed to open Head Database GUI"));
        }
      }
      return CommandResult.success();
    }
  }
}
