package io.github.kxng0109.chatwithdocs.controller;

import io.github.kxng0109.chatwithdocs.model.SessionCreateRequest;
import io.github.kxng0109.chatwithdocs.model.SessionResponse;
import io.github.kxng0109.chatwithdocs.service.SessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class SessionController {
    private final SessionService sessionService;

    /**
     * Creates a new chat session.
     *
     * @param request Optional name and description for the session
     * @return Created session details with generated sessionId
     */
    @PostMapping
    public ResponseEntity<SessionResponse> createSession(
            @Valid @RequestBody(required = false) SessionCreateRequest request
    ){
        log.info("Creating new session");
        SessionResponse response = sessionService.createSession(request);
        log.info("Session with ID: {} created", response.sessionId());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Retrieves a specific session by ID.
     *
     * @param sessionId The session's UUID
     * @param includeDocuments Whether to include document details in response
     * @return Session details
     */
    @GetMapping("/{sessionId}")
    public ResponseEntity<SessionResponse> getSession(
            @PathVariable String sessionId,
            @RequestParam(defaultValue = "false") boolean includeDocuments
    ){
        log.info("Fetching session with id: {}", sessionId);
        SessionResponse response = sessionService.getSessionInfo(sessionId, includeDocuments);
        return ResponseEntity.ok(response);
    }

    /**
     * Lists all sessions.
     *
     * @param includeDocuments Whether to include document details in response
     * @return List of all sessions (newest first)
     */
    @GetMapping
    public ResponseEntity<List<SessionResponse>> listAllSessions(
            @RequestParam(defaultValue = "false") boolean includeDocuments
    ){
        log.debug("Listing all sessions");
        List<SessionResponse> sessions = sessionService.listAllSessions(includeDocuments);
        return ResponseEntity.ok(sessions);
    }

    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Void> deleteSession(@PathVariable String sessionId){
        log.info("Deleting session with id: {}", sessionId);
        sessionService.deleteSession(sessionId);
        return ResponseEntity.noContent().build();
    }
}
