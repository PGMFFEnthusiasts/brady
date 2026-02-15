package me.fireballs.brady.core.storage

import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

object DatabaseConnection {
    @JvmStatic
    fun postgres(
        username: String,
        password: String,
        dbLocation: String
    ): Connection {
        return try {
            Class.forName("org.postgresql.Driver")
            DriverManager.getConnection("jdbc:postgresql://$dbLocation", username, password)
        } catch (e: SQLException) {
            throw RuntimeException(e)
        } catch (e: ClassNotFoundException) {
            throw RuntimeException(e)
        }
    }
}
