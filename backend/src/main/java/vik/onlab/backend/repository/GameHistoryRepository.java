package vik.onlab.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vik.onlab.backend.model.GameHistory;

@Repository
public interface GameHistoryRepository extends JpaRepository<GameHistory, Long> {
}