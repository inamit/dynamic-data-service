package com.engine.dynamic.repository.postgres;

import com.engine.dynamic.model.DynamicPayload;
import com.engine.dynamic.repository.DynamicStorageRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.Optional;

@Repository
public class PostgresJsonRepository implements DynamicStorageRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public PostgresJsonRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    private void ensureTableExists(String entityType) {
        String sql = """
            CREATE TABLE IF NOT EXISTS %s (
                id BIGSERIAL PRIMARY KEY,
                payload JSONB NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """.formatted(sanitizeTableName(entityType));
        jdbcTemplate.execute(sql);
    }

    private String sanitizeTableName(String entityType) {
        // Only allow alphanumeric characters and underscores to prevent SQL injection
        return entityType.replaceAll("[^a-zA-Z0-9_]", "").toLowerCase();
    }

    @Override
    public Long save(String entityType, DynamicPayload payload) {
        ensureTableExists(entityType);
        String tableName = sanitizeTableName(entityType);

        try {
            String payloadJson = objectMapper.writeValueAsString(payload.payload());

            if (payload.id() == null) {
                // Insert
                String sql = "INSERT INTO " + tableName + " (payload) VALUES (?::jsonb)";
                KeyHolder keyHolder = new GeneratedKeyHolder();

                jdbcTemplate.update(connection -> {
                    PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});
                    ps.setString(1, payloadJson);
                    return ps;
                }, keyHolder);

                return keyHolder.getKey().longValue();
            } else {
                // Update
                String sql = "UPDATE " + tableName + " SET payload = ?::jsonb, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
                int rowsAffected = jdbcTemplate.update(sql, payloadJson, payload.id());
                if (rowsAffected == 0) {
                    throw new RuntimeException("Entity not found for update: " + payload.id());
                }
                return payload.id();
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize JSON payload", e);
        }
    }

    @Override
    public Optional<DynamicPayload> findById(String entityType, Long id) {
        ensureTableExists(entityType);
        String tableName = sanitizeTableName(entityType);
        String sql = "SELECT id, payload FROM " + tableName + " WHERE id = ?";

        RowMapper<DynamicPayload> rowMapper = (rs, rowNum) -> {
            try {
                Long rsId = rs.getLong("id");
                String payloadStr = rs.getString("payload");
                JsonNode payloadNode = objectMapper.readTree(payloadStr);
                return new DynamicPayload(rsId, entityType, payloadNode);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to deserialize JSON payload", e);
            }
        };

        return jdbcTemplate.query(sql, rowMapper, id)
                .stream()
                .findFirst();
    }

    @Override
    public void deleteById(String entityType, Long id) {
        ensureTableExists(entityType);
        String tableName = sanitizeTableName(entityType);
        String sql = "DELETE FROM " + tableName + " WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }
}
