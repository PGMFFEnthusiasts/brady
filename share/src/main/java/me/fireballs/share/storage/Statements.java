package me.fireballs.share.storage;

public class Statements {
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
        "is_tourney) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
    public static final String INSERT_PLAYER_MATCH_DATA_ROW =
        "INSERT INTO player_match_data (player, match, team, kills, deaths, assists, killstreak, dmg_dealt, " +
        "dmg_taken, pickups, throws, passes, catches, strips, touchdowns, touchdown_passes) VALUES " +
        "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
}
