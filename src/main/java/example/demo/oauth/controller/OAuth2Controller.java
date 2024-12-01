package example.demo.oauth.controller;

import example.demo.oauth.model.CallbackRequest;
import example.demo.oauth.model.UserNameRequest;
import example.demo.oauth.service.GoogleOAuthService;
import example.demo.oauth.service.OAuth2Service;
import example.demo.oauth.service.OAuth2SessionService;
import example.demo.service.UserService;
import example.demo.service.auth.AuthenticationService;
import example.demo.service.auth.CookieService;
import example.demo.signup.model.User;
import example.demo.util.RandomUtil;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1/oauth2")
public class OAuth2Controller {

    private final OAuth2Service oAuth2Service;
    private final GoogleOAuthService googleOAuthService;
    private final OAuth2SessionService oAuth2SessionService;
    private final UserService userService;
    private final AuthenticationService authenticationService;
    private final RandomUtil randomUtil;
    private final CookieService cookieService;


    @GetMapping("/google/callback")
    public ResponseEntity<?> handleOAuthCallback(
            CallbackRequest callbackRequest, HttpSession session) {

        if (callbackRequest.getError() != null) {
            log.error(callbackRequest.getError());
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body("User did not grant permission to access his/her information");
        }

        oAuth2SessionService.checkStateParam(session, callbackRequest.getState());

        var user = googleOAuthService.authenticateUserWithGoogle(callbackRequest.getCode());

        URI uri = URI.create("http://localhost:5173/");
        final String jwt = authenticationService.createAuthenticationToken(user);
        final ResponseCookie authCookie = cookieService.buildAuthCookie(jwt);
        return ResponseEntity
                .status(HttpStatus.FOUND)
                .header(HttpHeaders.SET_COOKIE, authCookie.toString())
                .location(uri)
                .build();

//        if (user.getUsername() == null) {
//            return ResponseEntity
//                    .status(HttpStatus.UNPROCESSABLE_ENTITY)
//                    .body(oAuth2Service.buildErrorResponse(user));
//        } else {
//            URI uri = URI.create("/");
//            return ResponseEntity
//                    .status(HttpStatus.FOUND)
//                    .location(uri)
//                    .build();
//        }
    }

    @PostMapping("/register")
    public String registerUserName(@Valid @RequestBody UserNameRequest request) {
        User user = userService.updateUserNameOfOAuthAccount(request);
        return authenticationService.createAuthenticationToken(user);
    }

    private String generateStateParameter() {
        return randomUtil.getUUID();
    }

    @GetMapping("/google/authenticate")
    public ResponseEntity<String> generateAuthUrl(HttpSession session) {
        final String state = generateStateParameter();
        oAuth2SessionService.storeStateInSession(session, state);

        URI uri = googleOAuthService.buildAuthorizationUrl(state);
        log.info("Google auth url: {}", uri.toString());
        return ResponseEntity.ok(uri.toString());
        //return ResponseEntity.status(HttpStatus.FOUND).location(uri).build();
    }
}
