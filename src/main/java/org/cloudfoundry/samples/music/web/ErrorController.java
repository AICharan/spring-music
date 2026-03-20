package org.cloudfoundry.samples.music.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/errors")
public class ErrorController {

    private static final Logger logger = LoggerFactory.getLogger(ErrorController.class);

    // Field kept for fill-heap demo endpoint — not for production use
    private final List<int[]> junk = new ArrayList<>();

    @GetMapping("/kill")
    public void kill() {
        logger.info("Forcing application exit");
        System.exit(1);
    }

    @GetMapping("/fill-heap")
    public void fillHeap() {
        logger.info("Filling heap with junk, to initiate a crash");
        while (true) {
            junk.add(new int[9999999]);
        }
    }

    @GetMapping("/throw")
    public void throwException() {
        logger.info("Forcing an exception to be thrown");
        throw new NullPointerException("Forcing an exception to be thrown");
    }
}
