package com.team15.tripplanning.userservice.service;

import com.team15.tripplanning.userservice.model.SavedDestination;
import com.team15.tripplanning.userservice.model.User;
import com.team15.tripplanning.userservice.observer.EntityObserver;
import com.team15.tripplanning.userservice.observer.MongoEventLogger;
import com.team15.tripplanning.userservice.repository.SavedDestinationRepository;
import com.team15.tripplanning.userservice.repository.UserRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SavedDestinationService {
    private final SavedDestinationRepository savedDestinationRepository;
    private final UserRepository userRepository;
    private final List<EntityObserver> observers = new ArrayList<>();

    public SavedDestinationService(
            SavedDestinationRepository savedDestinationRepository,
            UserRepository userRepository,
            MongoEventLogger mongoEventLogger
    ) {
        this.savedDestinationRepository = savedDestinationRepository;
        this.userRepository = userRepository;
        register(mongoEventLogger);
    }

    public void register(EntityObserver observer) {
        observers.add(observer);
    }

    public void unregister(EntityObserver observer) {
        observers.remove(observer);
    }

    private void notifyObservers(String eventType, Object payload) {
        for (EntityObserver observer : observers) {
            observer.onEvent(eventType, payload);
        }
    }

    @Transactional
    public SavedDestination create(Long userId, SavedDestination savedDestination) {
        User user = resolveUser(userId);
        savedDestination.setUser(user);

        if (Boolean.TRUE.equals(savedDestination.getIsDefault())) {
            clearDefaultFlags(userId);
            savedDestination.setIsDefault(true);
        } else if (savedDestination.getIsDefault() == null) {
            savedDestination.setIsDefault(false);
        }

        return savedDestinationRepository.save(savedDestination);
    }

    public List<SavedDestination> findAll() {
        return savedDestinationRepository.findAll();
    }

    public List<SavedDestination> findByUserId(Long userId) {
        resolveUser(userId);
        return savedDestinationRepository.findByUser_Id(userId);
    }

    public SavedDestination findById(Long id) {
        return savedDestinationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "SavedDestination not found with id: " + id
                ));
    }

    public SavedDestination findByIdForUser(Long userId, Long destinationId) {
        resolveUser(userId);
        return savedDestinationRepository.findByIdAndUser_Id(destinationId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "SavedDestination not found with id: " + destinationId
                ));
    }

    @Transactional
    public SavedDestination update(Long id, SavedDestination savedDestination) {
        SavedDestination existing = findById(id);
        existing.setLabel(savedDestination.getLabel());
        existing.setDestinationName(savedDestination.getDestinationName());
        existing.setCountry(savedDestination.getCountry());
        existing.setLatitude(savedDestination.getLatitude());
        existing.setLongitude(savedDestination.getLongitude());

        if (Boolean.TRUE.equals(savedDestination.getIsDefault()) && existing.getUser() != null) {
            clearDefaultFlags(existing.getUser().getId());
            existing.setIsDefault(true);
        } else if (savedDestination.getIsDefault() != null) {
            existing.setIsDefault(savedDestination.getIsDefault());
        }

        if (savedDestination.getMetadata() != null) {
            existing.setMetadata(savedDestination.getMetadata());
        }

        return savedDestinationRepository.save(existing);
    }

    public void delete(Long id) {
        findById(id);
        savedDestinationRepository.deleteById(id);
    }

    public void deleteForUser(Long userId, Long destinationId) {
        SavedDestination existing = findByIdForUser(userId, destinationId);
        savedDestinationRepository.deleteById(existing.getId());
    }

    @Transactional
    public User setDefaultSavedDestination(Long userId, Long destinationId) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId is required");
        }
        if (destinationId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "destinationId is required");
        }

        resolveUser(userId);
        SavedDestination target = savedDestinationRepository.findById(destinationId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "SavedDestination not found with id: " + destinationId
                ));

        if (target.getUser() == null || !Objects.equals(target.getUser().getId(), userId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Saved destination does not belong to this user"
            );
        }

        clearDefaultFlags(userId);
        target.setIsDefault(true);
        savedDestinationRepository.save(target);

        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", userId);
        payload.put("destinationId", destinationId);
        notifyObservers("DEFAULT_DESTINATION_SET", payload);

        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found with id: " + userId
                ));
    }

    private void clearDefaultFlags(Long userId) {
        List<SavedDestination> userDestinations = savedDestinationRepository.findByUser_Id(userId);
        for (SavedDestination savedDestination : userDestinations) {
            savedDestination.setIsDefault(false);
        }
        savedDestinationRepository.saveAll(userDestinations);
    }

    private User resolveUser(Long userId) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId is required");
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found with id: " + userId
                ));
    }
}
