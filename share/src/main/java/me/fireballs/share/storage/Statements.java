package me.fireballs.share.storage;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Statements {
    public static final String CREATE_MATCH_DATA_TABLE =
        "CREATE TABLE IF NOT EXISTS match_data (match SERIAL PRIMARY KEY, server TEXT NOT NULL, " +
        "start_time BIGINT NOT NULL, duration INTEGER NOT NULL, winner INTEGER NOT NULL, team_one_score INTEGER NOT NULL, " +
        "team_two_score INTEGER NOT NULL, map TEXT NOT NULL, is_tourney BOOLEAN NOT NULL)";
    public static final String CREATE_PLAYER_MATCH_DATA_TABLE =
        "CREATE TABLE IF NOT EXISTS player_match_data (player BYTEA NOT NULL, match INTEGER NOT NULL, " +
        "team INTEGER NOT NULL, kills INTEGER NOT NULL, deaths INTEGER NOT NULL, assists INTEGER NOT NULL, " +
        "killstreak INTEGER NOT NULL, dmg_dealt DOUBLE PRECISION NOT NULL, dmg_taken DOUBLE PRECISION NOT NULL, " +
        "pickups INTEGER NOT NULL, throws INTEGER NOT NULL, passes INTEGER NOT NULL, catches INTEGER NOT NULL, " +
        "strips INTEGER NOT NULL, " +
        "touchdowns INTEGER NOT NULL, touchdown_passes INTEGER NOT NULL, PRIMARY KEY (player, match), " +
        "FOREIGN KEY (match) REFERENCES match_data(match))";
    public static final String CREATE_PLAYER_IDENTITY_TABLE =
        "CREATE TABLE IF NOT EXISTS player_identities (uuid BYTEA NOT NULL, name TEXT NOT NULL, PRIMARY KEY (uuid))";
    public static final String CREATE_MATCH_DATA_START_TIME_INDEX =
        "CREATE INDEX IF NOT EXISTS idx_md_start_time ON match_data (start_time)";
    public static final String CREATE_PLAYER_MATCH_DATA_PLAYER_INDEX =
        "CREATE INDEX IF NOT EXISTS idx_md_player ON player_match_data (player)";
    public static final String CREATE_PLAYER_MATCH_DATA_MATCH_INDEX =
        "CREATE INDEX IF NOT EXISTS idx_md_match ON player_match_data (match)";
    public static final String INSERT_MATCH_DATA_ROW =
        "INSERT INTO match_data (server, start_time, duration, winner, team_one_score, team_two_score, map, " +
        "is_tourney, team_one_name, team_two_name, team_one_color, team_two_color) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    public static final String INSERT_PLAYER_MATCH_DATA_ROW =
        "INSERT INTO player_match_data (player, match, team, kills, deaths, assists, killstreak, dmg_dealt, " +
        "dmg_taken, pickups, throws, passes, catches, strips, touchdowns, touchdown_passes, " +
        "passing_blocks, receive_blocks, defensive_interceptions, pass_interceptions, damage_carrier) VALUES " +
        "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    public static final String UPDATE_PLAYER_IDENTITY_QUERY =
        "INSERT INTO player_identities(uuid, name) VALUES (?, ?) ON CONFLICT (uuid) DO UPDATE SET name = excluded.name";

    public static boolean columnExists(
        final DatabaseMetaData databaseMetadata,
        final String tableName,
        final String columnName
    ) {
        try (final ResultSet resultSet = databaseMetadata.getColumns(null, null, tableName, columnName)) {
            return resultSet.next();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static final String TEAM_ONE_NAME_COLUMN = "team_one_name";
    public static final String CREATE_TEAM_ONE_COLUMN_QUERY =
        "ALTER TABLE match_data ADD COLUMN IF NOT EXISTS team_one_name TEXT DEFAULT 'Unknown';";
    public static final String TEAM_TWO_NAME_COLUMN = "team_two_name";
    public static final String CREATE_TEAM_TWO_COLUMN_QUERY =
        "ALTER TABLE match_data ADD COLUMN IF NOT EXISTS team_two_name TEXT DEFAULT 'Unknown';";
    public static final String TEAM_ONE_COLOR_COLUMN = "team_one_color";
    public static final String CREATE_TEAM_ONE_COLOR_COLUMN_QUERY =
        "ALTER TABLE match_data ADD COLUMN IF NOT EXISTS team_one_color INTEGER DEFAULT NULL;";
    public static final String TEAM_TWO_COLOR_COLUMN = "team_two_color";
    public static final String CREATE_TEAM_TWO_COLOR_COLUMN_QUERY =
        "ALTER TABLE match_data ADD COLUMN IF NOT EXISTS team_two_color INTEGER DEFAULT NULL;";
    public static final String PASSING_BLOCKS_COLUMN = "passing_blocks";
    public static final String CREATE_PASSING_BLOCKS_COLUMN_QUERY =
        "ALTER TABLE player_match_data ADD COLUMN IF NOT EXISTS passing_blocks DOUBLE PRECISION DEFAULT 0.0;";
    public static final String RECEIVE_BLOCKS_COLUMN = "receive_blocks";
    public static final String CREATE_RECEIVE_BLOCKS_COLUMN_QUERY =
        "ALTER TABLE player_match_data ADD COLUMN IF NOT EXISTS receive_blocks DOUBLE PRECISION DEFAULT 0.0;";
    public static final String DEFENSIVE_INTERCEPTIONS_COLUMN = "defensive_interceptions";
    public static final String CREATE_DEFENSIVE_INTERCEPTIONS_COLUMN_QUERY =
        "ALTER TABLE player_match_data ADD COLUMN IF NOT EXISTS defensive_interceptions DOUBLE PRECISION DEFAULT 0.0;";
    public static final String PASS_INTERCEPTIONS_COLUMN = "pass_interceptions";
    public static final String CREATE_PASS_INTERCEPTIONS_COLUMN_QUERY =
        "ALTER TABLE player_match_data ADD COLUMN IF NOT EXISTS pass_interceptions DOUBLE PRECISION DEFAULT 0.0;";
    public static final String DAMAGE_CARRIER_COLUMN = "damage_carrier";
    public static final String CREATE_DAMAGE_CARRIER_COLUMN_QUERY =
        "ALTER TABLE player_match_data ADD COLUMN IF NOT EXISTS damage_carrier DOUBLE PRECISION DEFAULT 0.0;";
}
