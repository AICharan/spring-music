package org.cloudfoundry.samples.music.web;

import jakarta.validation.Valid;
import org.cloudfoundry.samples.music.domain.Album;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.CrudRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/albums")
public class AlbumController {

    private static final Logger logger = LoggerFactory.getLogger(AlbumController.class);

    private final CrudRepository<Album, String> repository;

    @Autowired
    public AlbumController(CrudRepository<Album, String> repository) {
        this.repository = repository;
    }

    @GetMapping
    public Iterable<Album> albums() {
        return repository.findAll();
    }

    @PutMapping
    public Album add(@RequestBody @Valid Album album) {
        logger.info("Adding album {}", album.getId());
        return repository.save(album);
    }

    @PostMapping
    public Album update(@RequestBody @Valid Album album) {
        logger.info("Updating album {}", album.getId());
        return repository.save(album);
    }

    @GetMapping("/{id}")
    public Album getById(@PathVariable String id) {
        logger.info("Getting album {}", id);
        return repository.findById(id).orElse(null);
    }

    @DeleteMapping("/{id}")
    public void deleteById(@PathVariable String id) {
        logger.info("Deleting album {}", id);
        repository.deleteById(id);
    }
}
