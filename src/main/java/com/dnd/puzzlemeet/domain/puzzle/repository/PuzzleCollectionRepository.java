package com.dnd.puzzlemeet.domain.puzzle.repository;

import com.dnd.puzzlemeet.domain.puzzle.entity.PuzzleCollection;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PuzzleCollectionRepository extends JpaRepository<PuzzleCollection, Long> {}
