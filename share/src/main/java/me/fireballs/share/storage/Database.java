package me.fireballs.share.storage;

import me.fireballs.brady.core.storage.DatabaseConnection;
import me.fireballs.share.util.MatchData;
import me.fireballs.share.util.PlayerFootballStats;
import org.bukkit.entity.Player;

import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import static me.fireballs.share.storage.Statements.CREATE_DAMAGE_CARRIER_COLUMN_QUERY;
import static me.fireballs.share.storage.Statements.CREATE_DEFENSIVE_INTERCEPTIONS_COLUMN_QUERY;
import static me.fireballs.share.storage.Statements.CREATE_MATCH_DATA_START_TIME_INDEX;
import static me.fireballs.share.storage.Statements.CREATE_MATCH_DATA_TABLE;
import static me.fireballs.share.storage.Statements.CREATE_PASSING_BLOCKS_COLUMN_QUERY;
import static me.fireballs.share.storage.Statements.CREATE_PASS_INTERCEPTIONS_COLUMN_QUERY;
import static me.fireballs.share.storage.Statements.CREATE_PLAYER_IDENTITY_TABLE;
import static me.fireballs.share.storage.Statements.CREATE_PLAYER_MATCH_DATA_MATCH_INDEX;
import static me.fireballs.share.storage.Statements.CREATE_PLAYER_MATCH_DATA_PLAYER_INDEX;
import static me.fireballs.share.storage.Statements.CREATE_PLAYER_MATCH_DATA_TABLE;
import static me.fireballs.share.storage.Statements.CREATE_RECEIVE_BLOCKS_COLUMN_QUERY;
import static me.fireballs.share.storage.Statements.CREATE_TEAM_ONE_COLOR_COLUMN_QUERY;
import static me.fireballs.share.storage.Statements.CREATE_TEAM_ONE_COLUMN_QUERY;
import static me.fireballs.share.storage.Statements.CREATE_TEAM_TWO_COLOR_COLUMN_QUERY;
import static me.fireballs.share.storage.Statements.CREATE_TEAM_TWO_COLUMN_QUERY;
import static me.fireballs.share.storage.Statements.DAMAGE_CARRIER_COLUMN;
import static me.fireballs.share.storage.Statements.DEFENSIVE_INTERCEPTIONS_COLUMN;
import static me.fireballs.share.storage.Statements.INSERT_MATCH_DATA_ROW;
import static me.fireballs.share.storage.Statements.INSERT_PLAYER_MATCH_DATA_ROW;
import static me.fireballs.share.storage.Statements.PASSING_BLOCKS_COLUMN;
import static me.fireballs.share.storage.Statements.PASS_INTERCEPTIONS_COLUMN;
import static me.fireballs.share.storage.Statements.RECEIVE_BLOCKS_COLUMN;
import static me.fireballs.share.storage.Statements.TEAM_ONE_COLOR_COLUMN;
import static me.fireballs.share.storage.Statements.TEAM_ONE_NAME_COLUMN;
import static me.fireballs.share.storage.Statements.TEAM_TWO_COLOR_COLUMN;
import static me.fireballs.share.storage.Statements.TEAM_TWO_NAME_COLUMN;
import static me.fireballs.share.storage.Statements.UPDATE_PLAYER_IDENTITY_QUERY;
import static me.fireballs.share.storage.Statements.CREATE_TOURNAMENT_TABLE;
import static me.fireballs.share.storage.Statements.CREATE_TOURNAMENT_TEAM_TABLE;
import static me.fireballs.share.storage.Statements.CREATE_TOURNAMENT_TEAM_PLAYER_TABLE;
import static me.fireballs.share.storage.Statements.CREATE_TOURNAMENT_MATCH_TABLE;
import static me.fireballs.share.storage.Statements.CREATE_TOURNAMENT_DATE_INDEX;
import static me.fireballs.share.storage.Statements.CREATE_TOURNAMENT_MATCH_TOURNAMENT_ID_INDEX;
import static me.fireballs.share.storage.Statements.CREATE_TOURNAMENT_TEAM_PLAYER_TOURNAMENT_ID_INDEX;

public class Database {
    private final Logger logger;
    private Connection connection;

    public Database(final Logger logger) {
        this.logger = logger;
    }

    public void init(
        final String username,
        final String password,
        final String dbLocation
    ) {
        connection = DatabaseConnection.postgres(username, password, dbLocation);

        try (final Statement statement = connection.createStatement()) {
            statement.addBatch(CREATE_MATCH_DATA_TABLE);
            statement.addBatch(CREATE_PLAYER_MATCH_DATA_TABLE);
            statement.addBatch(CREATE_PLAYER_IDENTITY_TABLE);
            statement.addBatch(CREATE_MATCH_DATA_START_TIME_INDEX);
            statement.addBatch(CREATE_PLAYER_MATCH_DATA_PLAYER_INDEX);
            statement.addBatch(CREATE_PLAYER_MATCH_DATA_MATCH_INDEX);
            statement.addBatch(CREATE_TOURNAMENT_TABLE);
            statement.addBatch(CREATE_TOURNAMENT_TEAM_TABLE);
            statement.addBatch(CREATE_TOURNAMENT_TEAM_PLAYER_TABLE);
            statement.addBatch(CREATE_TOURNAMENT_MATCH_TABLE);
            statement.addBatch(CREATE_TOURNAMENT_DATE_INDEX);
            statement.addBatch(CREATE_TOURNAMENT_MATCH_TOURNAMENT_ID_INDEX);
            statement.addBatch(CREATE_TOURNAMENT_TEAM_PLAYER_TOURNAMENT_ID_INDEX);
            statement.executeBatch();
            statement.clearBatch();
            final DatabaseMetaData databaseMetadata = connection.getMetaData();
            createColumnIfNotExists(databaseMetadata, statement, "match_data", TEAM_ONE_NAME_COLUMN, CREATE_TEAM_ONE_COLUMN_QUERY);
            createColumnIfNotExists(databaseMetadata, statement,"match_data",  TEAM_TWO_NAME_COLUMN, CREATE_TEAM_TWO_COLUMN_QUERY);
            createColumnIfNotExists(databaseMetadata, statement,"match_data",  TEAM_ONE_COLOR_COLUMN, CREATE_TEAM_ONE_COLOR_COLUMN_QUERY);
            createColumnIfNotExists(databaseMetadata, statement,"match_data",  TEAM_TWO_COLOR_COLUMN, CREATE_TEAM_TWO_COLOR_COLUMN_QUERY);
            createColumnIfNotExists(databaseMetadata, statement, "player_match_data", PASSING_BLOCKS_COLUMN, CREATE_PASSING_BLOCKS_COLUMN_QUERY);
            createColumnIfNotExists(databaseMetadata, statement, "player_match_data", RECEIVE_BLOCKS_COLUMN, CREATE_RECEIVE_BLOCKS_COLUMN_QUERY);
            createColumnIfNotExists(databaseMetadata, statement, "player_match_data", DEFENSIVE_INTERCEPTIONS_COLUMN, CREATE_DEFENSIVE_INTERCEPTIONS_COLUMN_QUERY);
            createColumnIfNotExists(databaseMetadata, statement, "player_match_data", PASS_INTERCEPTIONS_COLUMN, CREATE_PASS_INTERCEPTIONS_COLUMN_QUERY);
            createColumnIfNotExists(databaseMetadata, statement, "player_match_data", DAMAGE_CARRIER_COLUMN, CREATE_DAMAGE_CARRIER_COLUMN_QUERY);
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
            preparedStatement.setString(9, matchData.teamOneName());
            preparedStatement.setString(10, matchData.teamTwoName());
            preparedStatement.setInt(11, matchData.teamOneColor());
            preparedStatement.setInt(12, matchData.teamTwoColor());
            if (preparedStatement.executeUpdate() == 1) {
                try (final ResultSet rs = preparedStatement.getGeneratedKeys()) {
                    rs.next();
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return -1;
    }

    public int preallocateMatchId() {
        if (connection == null) return -1;
        try (final PreparedStatement preparedStatement =
                 connection.prepareStatement(Statements.PREALLOCATE_MATCH_ID, Statement.RETURN_GENERATED_KEYS)) {
            // Use server name as a placeholder identifier
            preparedStatement.setString(1, "pending");
            if (preparedStatement.executeUpdate() == 1) {
                try (final ResultSet rs = preparedStatement.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return -1;
    }

    public boolean updateMatchData(int matchId, final MatchData matchData) {
        if (connection == null) return false;
        try (final PreparedStatement preparedStatement =
                 connection.prepareStatement(Statements.UPDATE_MATCH_DATA_ROW)) {
            preparedStatement.setString(1, matchData.server());
            preparedStatement.setLong(2, matchData.startTime());
            preparedStatement.setInt(3, matchData.duration());
            preparedStatement.setInt(4, matchData.winner());
            preparedStatement.setInt(5, matchData.teamOneScore());
            preparedStatement.setInt(6, matchData.teamTwoScore());
            preparedStatement.setString(7, matchData.map());
            preparedStatement.setBoolean(8, matchData.isTourney());
            preparedStatement.setString(9, matchData.teamOneName());
            preparedStatement.setString(10, matchData.teamTwoName());
            preparedStatement.setInt(11, matchData.teamOneColor());
            preparedStatement.setInt(12, matchData.teamTwoColor());
            preparedStatement.setInt(13, matchId);
            return preparedStatement.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean deletePlaceholderMatch(int matchId) {
        if (connection == null) return false;
        try (final PreparedStatement preparedStatement =
                 connection.prepareStatement(Statements.DELETE_PLACEHOLDER_MATCH)) {
            preparedStatement.setInt(1, matchId);
            return preparedStatement.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
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
                preparedStatement.setObject(1, uuidToBytes(player));
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
                preparedStatement.setInt(17, playerStats.totalPassingBlocks());
                preparedStatement.setInt(18, playerStats.totalReceivingBlocks());
                preparedStatement.setInt(19, playerStats.defensiveInterceptions());
                preparedStatement.setInt(20, playerStats.passingInterceptions());
                preparedStatement.setDouble(21, playerStats.damageCarrier());
                preparedStatement.addBatch();
            }
            preparedStatement.executeBatch();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void updatePlayerIdentity(Player player) {
        try (final PreparedStatement preparedStatement = connection.prepareStatement(UPDATE_PLAYER_IDENTITY_QUERY)) {
            preparedStatement.setObject(1, uuidToBytes(player.getUniqueId()));
            preparedStatement.setString(2, player.getName());
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static byte[] uuidToBytes(UUID uuid) {
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return bb.array();
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

    private void createColumnIfNotExists(
        final DatabaseMetaData databaseMetadata,
        final Statement statement,
        final String tableName,
        final String columnName,
        final String alterQuery
    ) {
        if (!Statements.columnExists(databaseMetadata, tableName, columnName)) {
            try {
                statement.execute(alterQuery);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
