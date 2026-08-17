package com.keepsake.backend.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.keepsake.backend.common.ApiException;
import com.keepsake.backend.game.GameDtos.BingoCardDto;
import com.keepsake.backend.game.GameDtos.Cell;
import com.keepsake.backend.game.GameDtos.ClaimResult;
import com.keepsake.backend.game.GameDtos.GameScore;
import com.keepsake.backend.game.GameDtos.GuessWhoResult;
import com.keepsake.backend.game.GameDtos.LeaderboardRow;
import com.keepsake.backend.game.GameDtos.Matched;
import com.keepsake.backend.game.GameDtos.Option;
import com.keepsake.backend.game.GameDtos.TriviaResult;
import com.keepsake.backend.game.GameDtos.TriviaRound;
import com.keepsake.backend.memory.CommentRepository;
import com.keepsake.backend.memory.Memory;
import com.keepsake.backend.memory.MemoryRepository;
import com.keepsake.backend.social.FollowRepository;
import com.keepsake.backend.user.Role;
import com.keepsake.backend.user.User;
import com.keepsake.backend.user.UserRepository;

/**
 * The memory games: Guess Who (match a memory to its author), School Trivia
 * (multiple choice, admin-curated) and Classmate Bingo (a 5x5 card of
 * classmate-finding tasks, verified server-side). Scores live on {@link GamePlayer}.
 */
@Service
public class GameService {

    /** Bingo rule -> prompt shown on the card. Order matters only for readability. */
    private static final Map<String, String> BINGO_RULES = Map.ofEntries(
            Map.entry("FOLLOWED_ONE", "Follow a classmate"),
            Map.entry("GOT_LIKED", "Get a like on one of your memories"),
            Map.entry("COMMENTED", "Leave a comment on a classmate's memory"),
            Map.entry("SET_MATE", "Find a classmate from your set"),
            Map.entry("SAME_YEAR", "Find a classmate who graduated the same year as you"),
            Map.entry("PHOTO_UP", "Find a classmate who posted a photo"),
            Map.entry("PROLIFIC", "Find a classmate with 5+ memories"),
            Map.entry("ANSWERED_FIVE", "Answer 5 challenge days"),
            Map.entry("AVATAR_UP", "Find a classmate with a profile picture"),
            Map.entry("BIO_UP", "Find a classmate with a bio"),
            Map.entry("NEW_FRIEND", "Find a classmate you don't follow yet"),
            Map.entry("TEN_DAYS", "Answer 10 challenge days"));

    private final UserRepository userRepository;
    private final MemoryRepository memoryRepository;
    private final CommentRepository commentRepository;
    private final FollowRepository followRepository;
    private final TriviaQuestionRepository triviaQuestionRepository;
    private final GamePlayerRepository gamePlayerRepository;
    private final BingoCardRepository bingoCardRepository;

    public GameService(UserRepository userRepository,
                       MemoryRepository memoryRepository,
                       CommentRepository commentRepository,
                       FollowRepository followRepository,
                       TriviaQuestionRepository triviaQuestionRepository,
                       GamePlayerRepository gamePlayerRepository,
                       BingoCardRepository bingoCardRepository) {
        this.userRepository = userRepository;
        this.memoryRepository = memoryRepository;
        this.commentRepository = commentRepository;
        this.followRepository = followRepository;
        this.triviaQuestionRepository = triviaQuestionRepository;
        this.gamePlayerRepository = gamePlayerRepository;
        this.bingoCardRepository = bingoCardRepository;
    }

    // ----- Guess Who -----

    @Transactional(readOnly = true)
    public GameDtos.GuessWhoRound guessWhoRound(Long userId) {
        User me = user(userId);
        requireSchool(me, "Join a school to play Guess Who");

        List<Memory> pool = memoryRepository.findGuessPool(userId, me.getSchool().getId(), PageRequest.of(0, 100));
        if (pool.isEmpty()) {
            throw ApiException.badRequest("Not enough memories in your school yet — share some first!");
        }
        Memory target = pool.get(ThreadLocalRandom.current().nextInt(pool.size()));

        List<User> classmates = userRepository.findBySchoolIdAndActiveTrueAndIdNot(me.getSchool().getId(), userId);
        classmates.removeIf(u -> u.getId().equals(target.getUser().getId()));
        Collections.shuffle(classmates);
        List<User> options = new ArrayList<>();
        options.add(target.getUser());
        for (int i = 0; i < classmates.size() && options.size() < 4; i++) {
            options.add(classmates.get(i));
        }
        Collections.shuffle(options);

        return new GameDtos.GuessWhoRound(
                target.getId(),
                target.getDayNumber(),
                target.getQuestion() != null ? target.getQuestion().getQuestion() : null,
                target.getAnswer(),
                options.stream().map(u -> new Option(u.getId(), u.getFullName(), u.getNickname(), u.getAvatarUrl())).toList());
    }

    @Transactional
    public GuessWhoResult guess(Long userId, Long memoryId, Long guessedUserId) {
        Memory memory = memoryRepository.findWithUserById(memoryId)
                .orElseThrow(() -> ApiException.notFound("Memory not found"));
        if (memory.isDeleted()) {
            throw ApiException.notFound("Memory not found");
        }
        boolean correct = guessedUserId != null && memory.getUser().getId().equals(guessedUserId);
        GamePlayer player = getOrCreatePlayer(userId);
        if (correct) {
            player.setGuessWhoCorrect(player.getGuessWhoCorrect() + 1);
            gamePlayerRepository.save(player);
        }
        User author = memory.getUser();
        return new GuessWhoResult(correct, author.getId(),
                author.getNickname() != null ? author.getNickname() : author.getFullName(),
                player.getGuessWhoCorrect());
    }

    // ----- Trivia -----

    @Transactional(readOnly = true)
    public TriviaRound triviaNext(Long userId) {
        User me = user(userId);
        requireSchool(me, "Join a school to play trivia");

        List<TriviaQuestion> questions = triviaQuestionRepository.findPlayable(me.getSchool().getId());
        if (questions.isEmpty()) {
            throw ApiException.badRequest("No trivia questions yet — check back soon");
        }
        TriviaQuestion q = questions.get(ThreadLocalRandom.current().nextInt(questions.size()));
        return new TriviaRound(q.getId(), q.getQuestion(), q.getOptions());
    }

    @Transactional
    public TriviaResult answerTrivia(Long userId, Long questionId, int optionIndex) {
        TriviaQuestion q = triviaQuestionRepository.findById(questionId)
                .orElseThrow(() -> ApiException.notFound("Question not found"));
        boolean correct = q.getCorrectIndex() == optionIndex;
        GamePlayer player = getOrCreatePlayer(userId);
        if (correct) {
            player.setTriviaCorrect(player.getTriviaCorrect() + 1);
            gamePlayerRepository.save(player);
        }
        return new TriviaResult(correct, player.getTriviaCorrect());
    }

    // ----- Bingo -----

    @Transactional
    public BingoCardDto bingoCard(Long userId) {
        BingoCard card = bingoCardRepository.findByUserId(userId).orElseGet(() -> createBingoCard(userId));
        return toDto(card);
    }

    @Transactional
    public BingoCardDto regenerateBingoCard(Long userId) {
        bingoCardRepository.findByUserId(userId).ifPresent(bingoCardRepository::delete);
        bingoCardRepository.flush();
        return toDto(createBingoCard(userId));
    }

    @Transactional
    public ClaimResult claimBingoCell(Long userId, String rule) {
        User me = user(userId);
        BingoCard card = bingoCardRepository.findByUserId(userId).orElseGet(() -> createBingoCard(userId));
        if (rule == null || !card.getRules().contains(rule)) {
            throw ApiException.badRequest("That square is not on your card");
        }
        Matched matched = null;
        boolean done = card.getCompleted().contains(rule);
        if (!done) {
            done = verify(me, rule);
            if (done) {
                card.getCompleted().add(rule);
            }
        }

        boolean bingo = false;
        if (done && !card.isBingoClaimed() && hasBingo(card)) {
            card.setBingoClaimed(true);
            bingo = true;
            GamePlayer player = getOrCreatePlayer(userId);
            player.setBingosCompleted(player.getBingosCompleted() + 1);
            gamePlayerRepository.save(player);
        }
        if (done && matched == null && card.getCompleted().contains(rule)) {
            matched = matchedFor(me, rule);
        }
        bingoCardRepository.save(card);
        GamePlayer player = getOrCreatePlayer(userId);
        return new ClaimResult(rule, done, bingo, player.getBingosCompleted(), matched);
    }

    private BingoCard createBingoCard(Long userId) {
        List<String> pool = new ArrayList<>(BINGO_RULES.keySet());
        List<String> rules = new ArrayList<>(25);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < BingoCard.SIZE * BingoCard.SIZE; i++) {
            rules.add(pool.get(random.nextInt(pool.size())));
        }
        BingoCard card = new BingoCard();
        card.setUser(user(userId));
        card.setRules(rules);
        return bingoCardRepository.save(card);
    }

    private BingoCardDto toDto(BingoCard card) {
        List<Cell> cells = new ArrayList<>(25);
        for (String rule : card.getRules()) {
            cells.add(new Cell(rule, BINGO_RULES.getOrDefault(rule, rule),
                    card.getCompleted().contains(rule), null));
        }
        return new BingoCardDto(cells, card.getCompleted().size(), card.isBingoClaimed());
    }

    /** Re-verifies a rule against live data. Never trusts the client. */
    private boolean verify(User me, String rule) {
        Long schoolId = me.getSchool() != null ? me.getSchool().getId() : null;
        return switch (rule) {
            case "FOLLOWED_ONE" -> followRepository.countByFollowerId(me.getId()) > 0;
            case "GOT_LIKED" -> memoryRepository.countLikesReceived(me.getId()) > 0;
            case "COMMENTED" -> commentRepository.countByUserId(me.getId()) > 0;
            case "SET_MATE" -> me.getClassSet() != null && schoolId != null
                    && userRepository.findFirstBySchoolIdAndClassSetIdAndIdNot(schoolId, me.getClassSet().getId(), me.getId()).isPresent();
            case "SAME_YEAR" -> me.getGraduationYear() != null && schoolId != null
                    && userRepository.findFirstBySchoolIdAndGraduationYearAndIdNot(schoolId, me.getGraduationYear(), me.getId()).isPresent();
            case "PHOTO_UP" -> schoolId != null
                    && !memoryRepository.findPhotoAuthorOfClassmate(me.getId(), schoolId, PageRequest.of(0, 1)).isEmpty();
            case "PROLIFIC" -> schoolId != null
                    && !memoryRepository.findProlificClassmates(me.getId(), schoolId, 5, PageRequest.of(0, 1)).isEmpty();
            case "ANSWERED_FIVE" -> memoryRepository.countByUserIdAndDeletedFalse(me.getId()) >= 5;
            case "AVATAR_UP" -> schoolId != null
                    && userRepository.findFirstBySchoolIdAndAvatarUrlIsNotNullAndIdNot(schoolId, me.getId()).isPresent();
            case "BIO_UP" -> schoolId != null
                    && userRepository.findFirstBySchoolIdAndBioIsNotNullAndIdNot(schoolId, me.getId()).isPresent();
            case "NEW_FRIEND" -> schoolId != null && newFriend(schoolId, me.getId()).isPresent();
            case "TEN_DAYS" -> memoryRepository.countByUserIdAndDeletedFalse(me.getId()) >= 10;
            default -> false;
        };
    }

    private Matched matchedFor(User me, String rule) {
        Long schoolId = me.getSchool() != null ? me.getSchool().getId() : null;
        Optional<User> match = switch (rule) {
            case "SET_MATE" -> me.getClassSet() != null && schoolId != null
                    ? userRepository.findFirstBySchoolIdAndClassSetIdAndIdNot(schoolId, me.getClassSet().getId(), me.getId())
                    : Optional.empty();
            case "SAME_YEAR" -> me.getGraduationYear() != null && schoolId != null
                    ? userRepository.findFirstBySchoolIdAndGraduationYearAndIdNot(schoolId, me.getGraduationYear(), me.getId())
                    : Optional.empty();
            case "PHOTO_UP" -> schoolId != null
                    ? memoryRepository.findPhotoAuthorOfClassmate(me.getId(), schoolId, PageRequest.of(0, 1)).stream().findFirst()
                    : Optional.empty();
            case "PROLIFIC" -> schoolId != null
                    ? memoryRepository.findProlificClassmates(me.getId(), schoolId, 5, PageRequest.of(0, 1)).stream().findFirst()
                    : Optional.empty();
            case "AVATAR_UP" -> schoolId != null
                    ? userRepository.findFirstBySchoolIdAndAvatarUrlIsNotNullAndIdNot(schoolId, me.getId())
                    : Optional.empty();
            case "BIO_UP" -> schoolId != null
                    ? userRepository.findFirstBySchoolIdAndBioIsNotNullAndIdNot(schoolId, me.getId())
                    : Optional.empty();
            case "NEW_FRIEND" -> schoolId != null ? newFriend(schoolId, me.getId()) : Optional.empty();
            default -> Optional.empty();
        };
        return match.map(u -> new Matched(u.getId(), u.getFullName(), u.getNickname(), u.getUsername(), u.getAvatarUrl()))
                .orElse(null);
    }

    private List<Long> followedIds(Long userId) {
        return followRepository.findByFollowerId(userId).stream()
                .map(f -> f.getFollowing().getId())
                .toList();
    }

    /** A classmate I don't follow yet (an empty follow list must not break the IN clause). */
    private Optional<User> newFriend(Long schoolId, Long userId) {
        List<Long> followed = followedIds(userId);
        if (followed.isEmpty()) {
            return userRepository.findFirstBySchoolIdAndIdNot(schoolId, userId);
        }
        return userRepository.findFirstBySchoolIdAndIdNotAndIdNotIn(schoolId, userId, followed);
    }

    private boolean hasBingo(BingoCard card) {
        List<String> rules = card.getRules();
        int n = BingoCard.SIZE;
        boolean[][] grid = new boolean[n][n];
        for (int i = 0; i < rules.size() && i < n * n; i++) {
            grid[i / n][i % n] = card.getCompleted().contains(rules.get(i));
        }
        for (int r = 0; r < n; r++) {
            boolean row = true;
            for (int c = 0; c < n; c++) {
                row &= grid[r][c];
            }
            if (row) {
                return true;
            }
        }
        for (int c = 0; c < n; c++) {
            boolean col = true;
            for (int r = 0; r < n; r++) {
                col &= grid[r][c];
            }
            if (col) {
                return true;
            }
        }
        boolean diag = true;
        for (int i = 0; i < n; i++) {
            diag &= grid[i][i];
        }
        if (diag) {
            return true;
        }
        diag = true;
        for (int i = 0; i < n; i++) {
            diag &= grid[i][n - 1 - i];
        }
        return diag;
    }

    // ----- scores & leaderboard -----

    @Transactional(readOnly = true)
    public GameScore score(Long userId) {
        GamePlayer p = gamePlayerRepository.findByUserId(userId).orElseGet(() -> new GamePlayer());
        return new GameScore(p.getGuessWhoCorrect(), p.getTriviaCorrect(), p.getBingosCompleted(), p.total());
    }

    @Transactional(readOnly = true)
    public List<LeaderboardRow> leaderboard(int limit) {
        List<LeaderboardRow> rows = new ArrayList<>();
        for (GamePlayer p : gamePlayerRepository.findAll()) {
            User u = p.getUser();
            if (u == null || !u.isActive() || u.getSchool() == null || u.getRole() == Role.ADMIN) {
                continue;
            }
            String name = u.getNickname() != null ? u.getNickname() : u.getFullName();
            rows.add(new LeaderboardRow(u.getId(), name, u.getAvatarUrl(), p.total()));
        }
        rows.sort((a, b) -> Integer.compare(b.total(), a.total()));
        int n = Math.min(limit, 50);
        return rows.size() > n ? rows.subList(0, n) : rows;
    }

    // ----- helpers -----

    private GamePlayer getOrCreatePlayer(Long userId) {
        return gamePlayerRepository.findByUserId(userId).orElseGet(() -> {
            GamePlayer p = new GamePlayer();
            p.setUser(user(userId));
            return gamePlayerRepository.save(p);
        });
    }

    private User user(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> ApiException.unauthorized("Account no longer exists"));
    }

    private static void requireSchool(User user, String message) {
        if (user.getSchool() == null) {
            throw ApiException.badRequest(message);
        }
    }
}
