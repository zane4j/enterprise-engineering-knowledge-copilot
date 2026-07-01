package io.github.zane4j.copilot.api.security;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class SecurityProblemAdvice {

    @ExceptionHandler(ActorIdentityException.class)
    ProblemDetail invalidActor(ActorIdentityException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN,
                "The authenticated identity is not authorized for this tenant");
        problem.setProperty("code", "JWT_ACTOR_INVALID");
        return problem;
    }
}
