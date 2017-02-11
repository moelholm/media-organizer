package com.moelholm.tools.mediaorganizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Paths;

@RestController
@RequestMapping("/api/mediaorganizer")
public class MediaOrganizerController {

  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Autowired
  private Environment environment;

  @Autowired
  private MediaOrganizer organizer;

  @GetMapping("/trigger")
  public ResponseEntity<?> runMediaOrganizer(@RequestHeader("Authorization") String apiKey) {

    if (!apiKey.equals(environment.getProperty("web.apiKey"))) {
      logger.warn("Unauthorized request");
      return new ResponseEntity<String>(HttpStatus.UNAUTHORIZED);
    }

    String fromDir = environment.getProperty(MainArgument.FROM_DIR.getArgumentName());

    String toDir = environment.getProperty(MainArgument.TO_DIR.getArgumentName());

    organizer.asyncUndoFlatMess(Paths.get(fromDir), Paths.get(toDir));

    return new ResponseEntity<String>(HttpStatus.OK);

  }

}