package nl.guuslieben.headsevolved;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Set;

@Plugin(
        id = "headsevolved",
        name = "nl.guuslieben.headsevolved.HeadsEvolved",
        version = "@version@",
        description = "Stores custom heads for Darwin Reforged.")
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
      LOG.error("Could not find TeslaLibs! Not registering commands");
      initiated = false;
    }
  }

  public static Logger LOG = LoggerFactory.getLogger(HeadsEvolved.class);

  @Listener
  public void onServerStart(GameStartedServerEvent event) {
    if (initiated) {
      singleton = this;
      LOG.info("Starting head database request task");
      Sponge.getScheduler().createTaskBuilder().async().name("he_obtain").execute(() -> {
        try {
          collectHeadsFromAPI();
        } catch (IOException e) {
          LOG.error("Failed to get head.", e);
        }
      }).submit(this);
    }
  }

  static String apiLine = "https://minecraft-heads.com/scripts/api.php?tags=true&cat=";

  static String[] uuidBlacklist = {
          "c7299fa8d44b "
  }; // UUID's causing NumberFormatExceptions, currently only one exists in the entire database

  private static void collectHeadsFromAPI() throws IOException {
    obtained = false;
    HeadObject.resetSet();
    LOG.debug("(async) Obtaining heads...");
    int totalHeads = 0;
    for (HeadObject.Category cat : HeadObject.Category.values()) {
      String connectionLine = apiLine + cat.toString().toLowerCase().replaceAll("_", "-");
      JsonArray array = readJsonFromUrl(connectionLine);
      LOG.debug(cat.toString() + " : " + array.size());

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

    LOG.debug("\nCollected : " + totalHeads + " heads from MinecraftHeadDB");
    obtained = true;
  }

  private static String readAll(Reader rd) throws IOException {
    StringBuilder sb = new StringBuilder();
    int cp;
    while ((cp = rd.read()) != -1) {
      sb.append((char) cp);
    }
    return sb.toString();
  }

  public static JsonArray readJsonFromUrl(String url) throws IOException {
      URLConnection urlcon = new URL(url).openConnection();
      urlcon.setRequestProperty("User-Agent", "HeadsEvolved_v@version@");
      try (
              InputStream is = urlcon.getInputStream();
              BufferedReader rd = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
        String jsonText = readAll(rd);
        return (JsonArray) new JsonParser().parse(jsonText);
      }
  }

  private CommandSpec hdbOpen =
          CommandSpec.builder()
                  .description(Text.of("Opens Head Evo GUI"))
                  .permission("he.open")
                  .executor(new OpenInventory())
                  .build();

  private CommandSpec hdbSearch =
          CommandSpec.builder()
                  .description(Text.of("Searches for heads with matching tags or name"))
                  .permission("he.find")
                  .arguments(GenericArguments.onlyOne(GenericArguments.string(Text.of("query"))))
                  .executor(new SearchHeads())
                  .build();

  private CommandSpec hdbReload =
          CommandSpec.builder()
                  .description(Text.of("Reloads the Heads Evolved plugin"))
                  .permission("he.reload")
                  .executor(new ReloadHE())
                  .build();

  private CommandSpec hdbMain =
          CommandSpec.builder()
                  .description(Text.of("Main command"))
                  .permission("he.open")
                  .child(hdbOpen, "open")
                  .child(hdbSearch, "find", "search")
                  .child(hdbReload, "reload")
                  .build();

  private static class SearchHeads implements CommandExecutor {

    @Override
    @NonnullByDefault
    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
      if (!obtained)
        src.sendMessage(Text.of(TextColors.GRAY, "// ", TextColors.RED, "Still obtaining heads from database, please wait.."));
      else if (src instanceof Player) {
        Player player = (Player) src;
        String query = args.<String>getOne("query").get();
        if (!"".equals(query)) {
          Set<HeadObject> headObjects = HeadObject.getByNameAndTag(query);
          ChestObject.openViewForSet(headObjects, player, "$search");
        }
      }
      return CommandResult.success();
    }
  }

  private static class OpenInventory implements CommandExecutor {

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

  private static class ReloadHE implements CommandExecutor {

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
      if (!obtained)
        src.sendMessage(Text.of(TextColors.GRAY, "// ", TextColors.RED, "Still obtaining heads from database, please wait.."));
      else {
        Sponge.getScheduler().createTaskBuilder().async().name("he_obtain").execute(() -> {
          try {
            collectHeadsFromAPI();
          } catch (IOException e) {
            LOG.error("Failed to get head.", e);
          }
          src.sendMessage(Text.of(TextColors.GRAY, "// ", TextColors.GOLD, "Finished obtaining heads from API, found " + HeadObject.getHeadObjectSet().size() + " heads"));
        }).submit(HeadsEvolved.singleton);
        src.sendMessage(Text.of(TextColors.GRAY, "// ", TextColors.AQUA, "Started obtaining heads, this may take a minute"));
      }
      return CommandResult.success();
    }
  }
}
