package me.fireballs.share.storage;

import org.bukkit.Bukkit;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class Statements {
    public static final String MATCH_DATA_TABLE_NAME = "match_data";
    public static final String PLAYER_MATCH_DATA_TABLE_NAME = "player_match_data";
    public static final String CREATE_MATCH_DATA_TABLE =
        "CREATE TABLE IF NOT EXISTS match_data (match INTEGER, server TEXT NOT NULL, " +
        "start_time INTEGER NOT NULL, duration INTEGER NOT NULL, winner INTEGER NOT NULL, team_one_score INTEGER NOT NULL, " +
        "team_two_score INTEGER NOT NULL, map TEXT NOT NULL, is_tourney INTEGER NOT NULL, PRIMARY KEY (match))";
    public static final String CREATE_PLAYER_MATCH_DATA_TABLE =
        "CREATE TABLE IF NOT EXISTS player_match_data (player BLOB NOT NULL, match INTEGER NOT NULL, " +
        "team INTEGER NOT NULL, kills INTEGER NOT NULL, deaths INTEGER NOT NULL, assists INTEGER NOT NULL, " +
        "killstreak INTEGER NOT NULL, dmg_dealt REAL NOT NULL, dmg_taken REAL NOT NULL, " +
        "pickups INTEGER NOT NULL, throws INTEGER NOT NULL, passes INTEGER NOT NULL, catches INTEGER NOT NULL, " +
        "strips INTEGER NOT NULL, " +
        "touchdowns INTEGER NOT NULL, touchdown_passes INTEGER NOT NULL, PRIMARY KEY (player, match), " +
        "FOREIGN KEY (match) REFERENCES match_data(match))";
    public static final String CREATE_PLAYER_MATCH_DATA_PLAYER_INDEX =
        "CREATE INDEX IF NOT EXISTS idx_md_player ON player_match_data (player)";
    public static final String CREATE_PLAYER_MATCH_DATA_MATCH_INDEX =
        "CREATE INDEX IF NOT EXISTS idx_md_match ON player_match_data (match)";
    public static final String INSERT_MATCH_DATA_ROW =
        "INSERT INTO match_data (server, start_time, duration, winner, team_one_score, team_two_score, map, " +
        "is_tourney, team_one_name, team_two_name) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    public static final String INSERT_PLAYER_MATCH_DATA_ROW =
        "INSERT INTO player_match_data (player, match, team, kills, deaths, assists, killstreak, dmg_dealt, " +
        "dmg_taken, pickups, throws, passes, catches, strips, touchdowns, touchdown_passes, " +
        "passing_blocks, receive_blocks) VALUES " +
        "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";


    public static String pragmaTableInfo(String tableName) {
        return "PRAGMA table_info('" + tableName + "')";
    }

    public static boolean columnExists(
        final Statement statement,
        final String tableName,
        final String columnName
    ) {
        try (final ResultSet metadata = statement.executeQuery(pragmaTableInfo(tableName))) {
            boolean columnExists = false;
            while (metadata.next()) {
                final String tblColumnName = metadata.getString("name");
                if (tblColumnName.equals(columnName)) {
                    return true;
                }
            }
            return false;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static final String TEAM_ONE_NAME_COLUMN = "team_one_name";
    public static final String CREATE_TEAM_ONE_COLUMN_QUERY =
        "ALTER TABLE match_data ADD COLUMN 'team_one_name' TEXT DEFAULT 'Unknown';";
    public static final String TEAM_TWO_NAME_COLUMN = "team_two_name";
    public static final String CREATE_TEAM_TWO_COLUMN_QUERY =
        "ALTER TABLE match_data ADD COLUMN 'team_two_name' TEXT DEFAULT 'Unknown';";
    public static final String PASSING_BLOCKS_COLUMN = "passing_blocks";
    public static final String CREATE_PASSING_BLOCKS_COLUMN_QUERY =
        "ALTER TABLE player_match_data ADD COLUMN 'passing_blocks' REAL DEFAULT 0.0;";
    public static final String RECEIVE_BLOCKS_COLUMN = "receive_blocks";
    public static final String CREATE_RECEIVE_BLOCKS_COLUMN_QUERY =
        "ALTER TABLE player_match_data ADD COLUMN 'receive_blocks' REAL DEFAULT 0.0;";
}
