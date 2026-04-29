package com.team15.tripplanning.userservice.repository;

import com.team15.tripplanning.userservice.document.AuthEvent;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthEventRepository extends MongoRepository<AuthEvent, String> {
    List<AuthEvent> findByUserIdOrderByTimestampDesc(Long userId);
    Page<AuthEvent> findByUserId(Long userId, Pageable pageable);
}
