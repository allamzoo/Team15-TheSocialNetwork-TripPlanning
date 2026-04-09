package com.team15.tripplanning.userservice.service;

import com.team15.tripplanning.userservice.model.SavedDestination;
import com.team15.tripplanning.userservice.model.User;
import com.team15.tripplanning.userservice.repository.SavedDestinationRepository;
import com.team15.tripplanning.userservice.repository.UserRepository;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SavedDestinationService {
    private final SavedDestinationRepository savedDestinationRepository;
    private final UserRepository userRepository;

    public SavedDestinationService(
            SavedDestinationRepository savedDestinationRepository,
            UserRepository userRepository
    ) {
        this.savedDestinationRepository = savedDestinationRepository;
        this.userRepository = userRepository;
    }

    public SavedDestination create(Long userId, SavedDestination savedDestination) {
        savedDestination.setUser(resolveUser(userId));
        return savedDestinationRepository.save(savedDestination);
    }

    public List<SavedDestination> findAll() {
        return savedDestinationRepository.findAll();
    }

    public SavedDestination findById(Long id) {
        return savedDestinationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "SavedDestination not found with id: " + id
                ));
    }

    public SavedDestination update(Long id, SavedDestination savedDestination) {
        SavedDestination existing = findById(id);
        // Owner relation is set on create via userId path parameter.
        existing.setLabel(savedDestination.getLabel());
        existing.setDestinationName(savedDestination.getDestinationName());
        existing.setCountry(savedDestination.getCountry());
        existing.setLatitude(savedDestination.getLatitude());
        existing.setLongitude(savedDestination.getLongitude());
        existing.setIsDefault(savedDestination.getIsDefault() != null ? savedDestination.getIsDefault() : existing.getIsDefault());
        existing.setMetadata(savedDestination.getMetadata() != null ? savedDestination.getMetadata() : existing.getMetadata());
        return savedDestinationRepository.save(existing);
    }

    public void delete(Long id) {
        findById(id);
        savedDestinationRepository.deleteById(id);
    }

    @Transactional
    public User setDefaultSavedDestination(Long userId, Long destinationId) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId is required");
        }
        if (destinationId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "destinationId is required");
        }

        User user = resolveUser(userId);
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

        List<SavedDestination> userDestinations = savedDestinationRepository.findByUser_Id(userId);
        for (SavedDestination savedDestination : userDestinations) {
            savedDestination.setIsDefault(false);
        }
        savedDestinationRepository.saveAll(userDestinations);

        target.setIsDefault(true);
        savedDestinationRepository.save(target);

        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found with id: " + userId
                ));
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
