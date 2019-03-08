import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class HeadObject {
  public enum Category {
    ALPHABET,
    ANIMALS,
    BLOCKS,
    DECORATION,
    FOOD_DRINKS,
    HUMANS,
    HUMANOID,
    MISCELLANEOUS,
    MONSTERS,
    PLANTS
  }

  private static Set<HeadObject> headObjectSet = new HashSet<>();

  private String name;
  private String uuid;
  private String value;
  private Category category;
  private String[] tags;

  public HeadObject(String name, String uuid, String value, String tags, Category category) {
    this.name = name;
    this.uuid = uuid;
    this.value = value;
    this.category = category;
    this.tags = tags.split(",");
    headObjectSet.add(this);
  }

  public static Set<HeadObject> getHeadObjectSet() {
    return headObjectSet;
  }

  public String getName() {
    return name;
  }

  public String getUuid() {
    return uuid;
  }

  public String getValue() {
    return value;
  }

  public String[] getTags() {
    return tags;
  }

  public Category getCategory() {
    return category;
  }

  public static HeadObject getFirstFromCategory(Category category) {
    return (HeadObject) getByCategory(category).toArray()[0];
  }

  public static Set<HeadObject> getByNameAndTag(String query) {
    Set<HeadObject> headObjectsWithNameOrTags =
        headObjectSet.stream()
            .filter(
                object -> {
                  for (String tag : object.getTags()) {
                    if (tag.toLowerCase().contains(query.toLowerCase())) return true;
                  }
                  if (object.getName().toLowerCase().contains(query.toLowerCase())) return true;
                  return false;
                })
            .collect(Collectors.toSet());
    return headObjectsWithNameOrTags;
  }

  public static Set<HeadObject> getByCategory(Category category) {
    Set<HeadObject> headObjectsWithCategory =
        headObjectSet.stream()
            .filter(object -> object.getCategory() == category)
            .collect(Collectors.toSet());
    return headObjectsWithCategory;
  }

  public static Set<HeadObject> getByNameIncludes(String name) {
    Set<HeadObject> headObjectsWithNameIncludes =
        headObjectSet.stream()
            .filter(object -> object.getName().toLowerCase().contains(name.toLowerCase()))
            .collect(Collectors.toSet());
    return headObjectsWithNameIncludes;
  }

  @Override
  public String toString() {
    return name + "\n\tCategory : " + category + "\n\tUUID : " + uuid + "\n\tValue : " + value;
  }
}
