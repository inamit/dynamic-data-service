package com.engine.dynamic.repository.postgres;

import com.engine.dynamic.model.DynamicPayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.KeyHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostgresJsonRepositoryTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private ObjectMapper objectMapper = new ObjectMapper();
    private PostgresJsonRepository repository;

    @BeforeEach
    void setUp() {
        repository = new PostgresJsonRepository(jdbcTemplate, objectMapper);
    }

    @Test
    void testSaveInsert() {
        DynamicPayload payload = new DynamicPayload(null, "product", objectMapper.createObjectNode().put("name", "Test"));

        when(jdbcTemplate.update(any(PreparedStatementCreator.class), any(KeyHolder.class))).thenAnswer(invocation -> {
            KeyHolder keyHolder = invocation.getArgument(1);
            keyHolder.getKeyList().add(java.util.Map.of("id", 1L));
            return 1;
        });

        Long id = repository.save("product", payload);

        assertThat(id).isEqualTo(1L);
        verify(jdbcTemplate).execute(anyString()); // verify table creation check
    }

    @Test
    void testSaveUpdate() {
        DynamicPayload payload = new DynamicPayload(1L, "product", objectMapper.createObjectNode().put("name", "Updated"));

        when(jdbcTemplate.update(anyString(), anyString(), eq(1L))).thenReturn(1);

        Long id = repository.save("product", payload);

        assertThat(id).isEqualTo(1L);
    }

    @Test
    void testFindById() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(1L))).thenReturn(List.of(new DynamicPayload(1L, "product", objectMapper.createObjectNode())));

        Optional<DynamicPayload> result = repository.findById("product", 1L);

        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo(1L);
    }

    @Test
    void testDeleteById() {
        repository.deleteById("product", 1L);
        verify(jdbcTemplate).update(anyString(), eq(1L));
    }
}
