package me.fireballs.share.storage;

import me.fireballs.share.util.MatchData;
import me.fireballs.share.util.PlayerFootballStats;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import static me.fireballs.share.storage.Statements.CREATE_MATCH_DATA_TABLE;
import static me.fireballs.share.storage.Statements.CREATE_PLAYER_MATCH_DATA_MATCH_INDEX;
import static me.fireballs.share.storage.Statements.CREATE_PLAYER_MATCH_DATA_PLAYER_INDEX;
import static me.fireballs.share.storage.Statements.CREATE_PLAYER_MATCH_DATA_TABLE;
import static me.fireballs.share.storage.Statements.INSERT_MATCH_DATA_ROW;
import static me.fireballs.share.storage.Statements.INSERT_PLAYER_MATCH_DATA_ROW;

public class Database {
    private final Logger logger;
    private Connection connection;

    public Database(final Logger logger) {
        this.logger = logger;
    }

    public void init(final String dbLocation) {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(
                String.format(
                    "jdbc:sqlite:%s",
                    dbLocation
                )
            );
        } catch (SQLException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        try (final Statement statement = connection.createStatement()) {
            statement.addBatch(CREATE_MATCH_DATA_TABLE);
            statement.addBatch(CREATE_PLAYER_MATCH_DATA_TABLE);
            statement.addBatch(CREATE_PLAYER_MATCH_DATA_PLAYER_INDEX);
            statement.addBatch(CREATE_PLAYER_MATCH_DATA_MATCH_INDEX);
            statement.executeBatch();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        logger.warning("Created stat tables");
    }

    public int addMatchData(final MatchData matchData) {
        if (connection == null) return -1;
        try (final PreparedStatement preparedStatement =
                 connection.prepareStatement(INSERT_MATCH_DATA_ROW, Statement.RETURN_GENERATED_KEYS)) {
            preparedStatement.setString(1, matchData.server());
            preparedStatement.setLong(2, matchData.startTime());
            preparedStatement.setInt(3, matchData.duration());
            preparedStatement.setInt(4, matchData.winner());
            preparedStatement.setInt(5, matchData.teamOneScore());
            preparedStatement.setInt(6, matchData.teamTwoScore());
            preparedStatement.setString(7, matchData.map());
            preparedStatement.setBoolean(8, matchData.isTourney());
            if (preparedStatement.executeUpdate() == 1) {
                final ResultSet rs = preparedStatement.getGeneratedKeys();
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return -1;
    }

    public void batchAddPlayerMatchData(
        final int matchId,
        final Map<UUID, PlayerFootballStats> stats
    ) {
        if (connection == null) return;
        try (final PreparedStatement preparedStatement =
                     connection.prepareStatement(INSERT_PLAYER_MATCH_DATA_ROW, Statement.RETURN_GENERATED_KEYS)) {
            for (final Map.Entry<UUID, PlayerFootballStats> entry : stats.entrySet()) {
                final UUID player = entry.getKey();
                final PlayerFootballStats playerStats = entry.getValue();
                preparedStatement.setObject(1, player);
                preparedStatement.setInt(2, matchId);
                preparedStatement.setInt(3, playerStats.team());
                preparedStatement.setInt(4, playerStats.kills());
                preparedStatement.setInt(5, playerStats.deaths());
                preparedStatement.setInt(6, playerStats.assists());
                preparedStatement.setInt(7, playerStats.killstreak());
                preparedStatement.setDouble(8, playerStats.damageDealt());
                preparedStatement.setDouble(9, playerStats.damageTaken());
                preparedStatement.setInt(10, playerStats.pickups());
                preparedStatement.setInt(11, playerStats.throwz());
                preparedStatement.setInt(12, playerStats.passes());
                preparedStatement.setInt(13, playerStats.catches());
                preparedStatement.setInt(14, playerStats.strips());
                preparedStatement.setInt(15, playerStats.touchdowns());
                preparedStatement.setInt(16, playerStats.touchdownPasses());
                preparedStatement.addBatch();
            }
            preparedStatement.executeBatch();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void close() {
        if (this.connection != null) {
            try {
                this.connection.close();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
