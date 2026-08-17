package com.keepsake.backend.game;

import java.util.List;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.keepsake.backend.game.GameDtos.BingoCardDto;
import com.keepsake.backend.game.GameDtos.ClaimResult;
import com.keepsake.backend.game.GameDtos.GameScore;
import com.keepsake.backend.game.GameDtos.GuessWhoResult;
import com.keepsake.backend.game.GameDtos.LeaderboardRow;
import com.keepsake.backend.game.GameDtos.TriviaResult;
import com.keepsake.backend.game.GameDtos.TriviaRound;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/games")
public class GameController {

    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    // ----- Guess Who -----

    @GetMapping("/guess-who")
    public GameDtos.GuessWhoRound guessWhoRound(Authentication auth) {
        return gameService.guessWhoRound(currentUserId(auth));
    }

    @PostMapping("/guess-who/{memoryId}/guess")
    public GuessWhoResult guess(@PathVariable Long memoryId, Authentication auth,
                                @Valid @RequestBody GuessRequest req) {
        return gameService.guess(currentUserId(auth), memoryId, req.userId());
    }

    // ----- Trivia -----

    @GetMapping("/trivia/next")
    public TriviaRound triviaNext(Authentication auth) {
        return gameService.triviaNext(currentUserId(auth));
    }

    @PostMapping("/trivia/{questionId}/answer")
    public TriviaResult answer(@PathVariable Long questionId, Authentication auth,
                               @Valid @RequestBody AnswerRequest req) {
        return gameService.answerTrivia(currentUserId(auth), questionId, req.optionIndex());
    }

    // ----- Bingo -----

    @GetMapping("/bingo")
    public BingoCardDto bingo(Authentication auth) {
        return gameService.bingoCard(currentUserId(auth));
    }

    @PostMapping("/bingo/claim")
    public ClaimResult claim(Authentication auth, @RequestBody ClaimRequest req) {
        return gameService.claimBingoCell(currentUserId(auth), req.rule());
    }

    @PostMapping("/bingo/regenerate")
    public BingoCardDto regenerate(Authentication auth) {
        return gameService.regenerateBingoCard(currentUserId(auth));
    }

    // ----- scores -----

    @GetMapping("/score")
    public GameScore score(Authentication auth) {
        return gameService.score(currentUserId(auth));
    }

    @GetMapping("/leaderboard")
    public List<LeaderboardRow> leaderboard(@RequestParam(defaultValue = "10") int limit) {
        return gameService.leaderboard(limit);
    }

    private Long currentUserId(Authentication auth) {
        return Long.valueOf(((UserDetails) auth.getPrincipal()).getUsername());
    }

    public record GuessRequest(@NotNull Long userId) {
    }

    public record AnswerRequest(@NotNull @Min(0) @Max(3) Integer optionIndex) {
    }

    public record ClaimRequest(String rule) {
    }
}
