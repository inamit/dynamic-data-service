package com.engine.dynamic.repository;

import com.engine.dynamic.model.DynamicPayload;
import java.util.Optional;

public interface DynamicStorageRepository {
    Long save(String entityType, DynamicPayload payload);
    Optional<DynamicPayload> findById(String entityType, Long id);
    void deleteById(String entityType, Long id);
}
