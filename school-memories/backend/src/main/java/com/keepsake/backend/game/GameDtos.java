package com.keepsake.backend.game;

import java.util.List;

public final class GameDtos {

    private GameDtos() {
    }

    public record GuessWhoRound(Long memoryId, int dayNumber, String question, String answer, List<Option> options) {
    }

    public record Option(Long userId, String name, String nickname, String avatarUrl) {
    }

    public record GuessWhoResult(boolean correct, Long correctUserId, String correctName, int guessWhoCorrect) {
    }

    public record TriviaRound(Long questionId, String question, List<String> options) {
    }

    public record TriviaResult(boolean correct, int triviaCorrect) {
    }

    public record BingoCardDto(List<Cell> cells, int completedCount, boolean bingo) {
    }

    public record Cell(String rule, String prompt, boolean done, Matched matched) {
    }

    /** A classmate that satisfied a bingo cell, if any. */
    public record Matched(Long userId, String name, String nickname, String username, String avatarUrl) {
    }

    public record ClaimResult(String rule, boolean done, boolean bingo, int bingosCompleted, Matched matched) {
    }

    public record GameScore(int guessWhoCorrect, int triviaCorrect, int bingosCompleted, int total) {
    }

    public record LeaderboardRow(Long userId, String name, String avatarUrl, int total) {
    }
}
