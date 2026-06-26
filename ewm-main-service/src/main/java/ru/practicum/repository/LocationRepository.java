package ru.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.model.ManagedLocation;

public interface LocationRepository extends JpaRepository<ManagedLocation, Long> {
}
