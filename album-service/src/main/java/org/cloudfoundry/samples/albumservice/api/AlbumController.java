package org.cloudfoundry.samples.albumservice.api;

import jakarta.validation.Valid;
import org.cloudfoundry.samples.albumservice.acl.AlbumPayload;
import org.cloudfoundry.samples.albumservice.acl.AlbumTranslator;
import org.cloudfoundry.samples.albumservice.domain.AlbumEntity;
import org.cloudfoundry.samples.albumservice.event.AlbumCreatedEvent;
import org.cloudfoundry.samples.albumservice.repository.AlbumRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.StreamSupport;

/**
 * Challenge 5 — The Cut.
 *
 * This controller exposes the same /albums API contract as the monolith's AlbumController.
 * The monolith still runs unchanged on port 8080.
 * This service runs on port 8081.
 *
 * Both satisfy the same AlbumPayload contract (verified by AlbumContractTest).
 */
@RestController
@RequestMapping("/albums")
public class AlbumController {

    private static final Logger logger = LoggerFactory.getLogger(AlbumController.class);

    private final AlbumRepository repository;
    private final AlbumTranslator translator;
    private final ApplicationEventPublisher eventPublisher;

    public AlbumController(AlbumRepository repository,
                           AlbumTranslator translator,
                           ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.translator = translator;
        this.eventPublisher = eventPublisher;
    }

    @GetMapping
    public List<AlbumPayload> getAll() {
        return StreamSupport.stream(repository.findAll().spliterator(), false)
                .map(translator::toPayload)
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<AlbumPayload> getById(@PathVariable String id) {
        logger.info("Getting album {}", id);
        return repository.findById(id)
                .map(translator::toPayload)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping
    public AlbumPayload add(@RequestBody @Valid AlbumPayload payload) {
        AlbumEntity entity = translator.toNewEntity(payload);
        AlbumEntity saved = repository.save(entity);
        AlbumPayload result = translator.toPayload(saved);

        // Challenge 9: publish domain event after save
        eventPublisher.publishEvent(new AlbumCreatedEvent(result));
        logger.info("Added album {}", saved.getId());

        return result;
    }

    @PostMapping
    public AlbumPayload update(@RequestBody @Valid AlbumPayload payload) {
        AlbumEntity entity = translator.toEntity(payload);
        AlbumEntity saved = repository.save(entity);
        logger.info("Updated album {}", saved.getId());
        return translator.toPayload(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable String id) {
        logger.info("Deleting album {}", id);
        repository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
