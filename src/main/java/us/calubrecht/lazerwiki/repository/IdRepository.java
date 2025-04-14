package us.calubrecht.lazerwiki.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@Service
public class IdRepository {
    @Autowired
    JdbcTemplate jdbcTemplate;

    public long getNewId() {
        final String generateIdSQL = "INSERT INTO page_ids VALUES (NULL)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(
                new PreparedStatementCreator() {
                    public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
                        return connection.prepareStatement(generateIdSQL, new String[] {"id"});
                    }
                }, keyHolder);
        return keyHolder.getKey().longValue();
    }
}
