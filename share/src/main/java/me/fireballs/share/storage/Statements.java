package me.fireballs.share.storage;

public class Statements {
    public static final String CREATE_MATCH_DATA_TABLE =
        "CREATE TABLE IF NOT EXISTS match_data (match int NOT NULL AUTO_INCREMENT, server varchar(20) NOT NULL, " +
        "start_time TIMESTAMP NOT NULL, duration int NOT NULL, winner int NOT NULL, team_one_score int NOT NULL, " +
        "team_two_score int NOT NULL, map varchar(200) NOT NULL, is_tourney boolean NOT NULL, PRIMARY KEY (match))";
    public static final String CREATE_PLAYER_MATCH_DATA_TABLE =
        "CREATE TABLE IF NOT EXISTS player_match_data (player uuid NOT NULL, match int NOT NULL, " +
        "team int NOT NULL, kills int NOT NULL, deaths int NOT NULL, assists int NOT NULL, " +
        "killstreak int NOT NULL, dmg_dealt double precision NOT NULL, dmg_taken double precision NOT NULL, " +
        "pickups int NOT NULL, throws int NOT NULL, passes int NOT NULL, catches int NOT NULL, strips int NOT NULL, " +
        "touchdowns int NOT NULL, touchdown_passes int NOT NULL, PRIMARY KEY (player, match), " +
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
