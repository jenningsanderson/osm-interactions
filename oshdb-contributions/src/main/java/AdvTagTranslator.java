import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import org.heigit.bigspatialdata.oshdb.util.OSHDBRole;
import org.heigit.bigspatialdata.oshdb.util.OSHDBTag;
import org.heigit.bigspatialdata.oshdb.util.OSHDBTagKey;
import org.heigit.bigspatialdata.oshdb.util.TableNames;
import org.heigit.bigspatialdata.oshdb.util.exceptions.OSHDBKeytablesNotFoundException;
import org.heigit.bigspatialdata.oshdb.util.tagtranslator.OSMRole;
import org.heigit.bigspatialdata.oshdb.util.tagtranslator.OSMTag;
import org.heigit.bigspatialdata.oshdb.util.tagtranslator.OSMTagKey;
import org.heigit.bigspatialdata.oshdb.util.tagtranslator.TagTranslator;

public class AdvTagTranslator implements AutoCloseable {
  private static final Comparator<OSHDBTagKey> keyOrder =
      (a, b) -> Integer.compare(a.toInt(), b.toInt());

  private final Connection conn;
  private final TagTranslator tagTranslator;

  private final PreparedStatement keyCaseInsensitive;

  public AdvTagTranslator(Connection conn) throws SQLException, OSHDBKeytablesNotFoundException {
    this.conn = conn;
    this.tagTranslator = new TagTranslator(conn);

    this.keyCaseInsensitive = conn.prepareStatement(
        "SELECT ID FROM " + TableNames.E_KEY.toString() + " WHERE KEY.TXT LIKE ? ;");
  }

  @Override
  public void close() throws Exception {
    conn.close();
  }

  public Map<String, String> getTagsAsKeyValueMap(Iterable<OSHDBTag> tags) {
    Map<String, String> map = new HashMap<>();
    for (OSHDBTag tag : tags) {
      OSMTag translated = getOSMTagOf(tag);
      map.put(translated.getKey(), translated.getValue());

    }
    return map;
  }

  public OSHDBTagKey getOSHDBTagKeyOf(String key) {
    return tagTranslator.getOSHDBTagKeyOf(key);
  }

  public Set<OSHDBTagKey> getOSHDBTagKeyOf(String key, boolean caseSensitive) throws SQLException {
    if (caseSensitive) {
      return Collections.singleton(getOSHDBTagKeyOf(key));
    }

    SortedSet<OSHDBTagKey> set = new TreeSet<>(keyOrder);

    keyCaseInsensitive.setString(1, key.toString());
    try (ResultSet keys = keyCaseInsensitive.executeQuery()) {
      while (keys.next()) {
        set.add(new OSHDBTagKey(keys.getInt("ID")));
      }
    }
    return set;
  }

  public OSHDBTagKey getOSHDBTagKeyOf(OSMTagKey key) {
    return tagTranslator.getOSHDBTagKeyOf(key);
  }

  public OSMTagKey getOSMTagKeyOf(int key) {
    return tagTranslator.getOSMTagKeyOf(key);
  }

  public OSMTagKey getOSMTagKeyOf(OSHDBTagKey key) {
    return tagTranslator.getOSMTagKeyOf(key);
  }

  public OSHDBTag getOSHDBTagOf(String key, String value) {
    return tagTranslator.getOSHDBTagOf(key, value);
  }

  public OSHDBTag getOSHDBTagOf(OSMTag tag) {
    return tagTranslator.getOSHDBTagOf(tag);
  }

  public OSMTag getOSMTagOf(int key, int value) {
    return tagTranslator.getOSMTagOf(key, value);
  }

  public OSMTag getOSMTagOf(OSHDBTag tag) {
    return tagTranslator.getOSMTagOf(tag);
  }

  public OSHDBRole getOSHDBRoleOf(String role) {
    return tagTranslator.getOSHDBRoleOf(role);
  }

  public OSHDBRole getOSHDBRoleOf(OSMRole role) {
    return tagTranslator.getOSHDBRoleOf(role);
  }

  public OSMRole getOSMRoleOf(int role) {
    return tagTranslator.getOSMRoleOf(role);
  }

  public OSMRole getOSMRoleOf(OSHDBRole role) {
    return tagTranslator.getOSMRoleOf(role);
  }

  public Connection getConnection() {
    return tagTranslator.getConnection();
  }



}
