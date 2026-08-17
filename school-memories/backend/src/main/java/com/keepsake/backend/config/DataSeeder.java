package com.keepsake.backend.config;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.keepsake.backend.achievement.AchievementService;
import com.keepsake.backend.announcement.Announcement;
import com.keepsake.backend.announcement.AnnouncementRepository;
import com.keepsake.backend.challenge.ChallengeQuestion;
import com.keepsake.backend.challenge.ChallengeQuestionRepository;
import com.keepsake.backend.game.TriviaQuestion;
import com.keepsake.backend.game.TriviaQuestionRepository;
import com.keepsake.backend.memory.Comment;
import com.keepsake.backend.memory.CommentRepository;
import com.keepsake.backend.memory.Memory;
import com.keepsake.backend.memory.MemoryRepository;
import com.keepsake.backend.memory.Reaction;
import com.keepsake.backend.memory.ReactionRepository;
import com.keepsake.backend.school.ClassSet;
import com.keepsake.backend.school.ClassSetRepository;
import com.keepsake.backend.school.School;
import com.keepsake.backend.school.SchoolRepository;
import com.keepsake.backend.social.Follow;
import com.keepsake.backend.social.FollowRepository;
import com.keepsake.backend.user.Role;
import com.keepsake.backend.user.User;
import com.keepsake.backend.user.UserRepository;

/**
 * Seeds demo data so the platform is explorable on first boot: an admin, a
 * demo school with sets, 8 classmates, all 30 questions, and a handful of
 * memories/reactions/comments/follows. Everything is idempotent.
 */
@Component
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private static final String DEMO_PASSWORD = "password123";

    private final UserRepository userRepository;
    private final SchoolRepository schoolRepository;
    private final ClassSetRepository classSetRepository;
    private final ChallengeQuestionRepository questionRepository;
    private final MemoryRepository memoryRepository;
    private final ReactionRepository reactionRepository;
    private final CommentRepository commentRepository;
    private final FollowRepository followRepository;    private final AnnouncementRepository announcementRepository;
    private final AchievementService achievementService;
    private final TriviaQuestionRepository triviaQuestionRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository,
                      SchoolRepository schoolRepository,
                      ClassSetRepository classSetRepository,
                      ChallengeQuestionRepository questionRepository,
                      MemoryRepository memoryRepository,
                      ReactionRepository reactionRepository,
                      CommentRepository commentRepository,
                      FollowRepository followRepository,
                      AnnouncementRepository announcementRepository,
                      AchievementService achievementService,
                      TriviaQuestionRepository triviaQuestionRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.schoolRepository = schoolRepository;
        this.classSetRepository = classSetRepository;
        this.questionRepository = questionRepository;
        this.memoryRepository = memoryRepository;
        this.reactionRepository = reactionRepository;
        this.commentRepository = commentRepository;
        this.followRepository = followRepository;
        this.announcementRepository = announcementRepository;
        this.achievementService = achievementService;
        this.triviaQuestionRepository = triviaQuestionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (questionRepository.count() > 0) {
            log.info("Demo data already present — skipping seed.");
            return;
        }
        log.info("Seeding demo data (school, classmates, 30 questions, sample memories)...");

        School school = schoolOrCreate("Greenfield High",
                "Our school is not just a building — it's 40 years of stories.");

        ClassSet set2019 = setOrCreate(school, "Set of 2019", 2019);
        ClassSet set2020 = setOrCreate(school, "Set of 2020", 2020);
        ClassSet set2021 = setOrCreate(school, "Set of 2021", 2021);

        User admin = userOrCreate("admin@greenfield.demo", "Admin", "Admin", null, school, null, Role.ADMIN, true);
        List<User> classmates = List.of(
                userOrCreate("ada@greenfield.demo", "Ada Obi", "Ada", "ada", school, set2019, Role.USER, true),
                userOrCreate("bisi@greenfield.demo", "Bisi Adeyemi", "Bisi", "bisi", school, set2019, Role.USER, true),
                userOrCreate("chidi@greenfield.demo", "Chidi Okonkwo", "Chidi", "chidi", school, set2020, Role.USER, true),
                userOrCreate("dani@greenfield.demo", "Daniella Mensah", "Dani", "dani", school, set2020, Role.USER, true),
                userOrCreate("emeka@greenfield.demo", "Emeka Nwosu", "Emeka", "emeka", school, set2020, Role.USER, true),
                userOrCreate("fatima@greenfield.demo", "Fatima Bello", "Fatima", "fatima", school, set2021, Role.USER, true),
                userOrCreate("george@greenfield.demo", "George Appiah", "George", "george", school, set2021, Role.USER, true),
                userOrCreate("hana@greenfield.demo", "Hana Yusuf", "Hana", "hana", school, set2021, Role.USER, true));

        seedQuestions();
        seedMemories(classmates);
        seedTrivia(school);
        achievementService.seedDefinitions();
        for (User u : classmates) {
            achievementService.checkAndUnlock(u);
        }

        announcementOrCreate("Welcome to Greenfield's memory archive",
                "This is our school's first digital yearbook. Answer a question a day and let's fill it with stories — thirty questions, thirty days, one school.");
        announcementOrCreate("Photo contest coming soon",
                "We're planning a throwback photo contest. Dig out your old school photos — the best ones will live in the archive forever.");

        log.info("Seed complete. Demo login: ada@greenfield.demo / {}  (admin: admin@greenfield.demo / {})",
                DEMO_PASSWORD, DEMO_PASSWORD);
    }

    private School schoolOrCreate(String name, String description) {
        return schoolRepository.findByActiveTrueOrderByNameAsc().stream()
                .filter(s -> s.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElseGet(() -> {
                    School s = new School();
                    s.setName(name);
                    s.setDescription(description);
                    return schoolRepository.save(s);
                });
    }

    private ClassSet setOrCreate(School school, String name, int year) {
        return classSetRepository.findBySchoolIdOrderByGraduationYearAscNameAsc(school.getId()).stream()
                .filter(cs -> cs.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElseGet(() -> {
                    ClassSet cs = new ClassSet();
                    cs.setSchool(school);
                    cs.setName(name);
                    cs.setGraduationYear(year);
                    return classSetRepository.save(cs);
                });
    }

    private User userOrCreate(String email, String fullName, String nickname, String username,
                              School school, ClassSet set, Role role, boolean verified) {
        return userRepository.findByEmailIgnoreCase(email).orElseGet(() -> {
            User u = new User();
            u.setEmail(email);
            u.setPasswordHash(passwordEncoder.encode(DEMO_PASSWORD));
            u.setFullName(fullName);
            u.setNickname(nickname);
            u.setUsername(username);
            u.setSchool(school);
            u.setClassSet(set);
            u.setGraduationYear(set != null ? set.getGraduationYear() : null);
            u.setRole(role);
            u.setVerified(verified);
            u.setActive(true);
            return userRepository.save(u);
        });
    }

    private void seedQuestions() {
        String[][] questions = {
                {"Your Name & Set", "Introduce yourself the way your classmates knew you."},
                {"Your School Nickname", "What did everyone call you — and how did you get that name?"},
                {"Your Favourite Subject", "The one class you actually looked forward to."},
                {"Your Favourite Teacher", "Who made lessons feel less like lessons?"},
                {"Your Favourite School Event", "Sports day, cultural day, that one assembly..."},
                {"Funniest Classroom Experience", "The moment the whole class lost it."},
                {"Three Friends You'll Never Forget", "Name them and one memory each."},
                {"Ever Received Punishment? What Happened?", "Detention, lines, standing outside — tell the story."},
                {"Favourite Spot in School", "The field, the library corner, behind the lab..."},
                {"Your Best School Memory", "The one you'd replay on loop."},
                {"Biggest Trouble You Ever Got Into", "What did you do, and how did you talk your way out of it?"},
                {"Front Seat or Back Seat? Why?", "Team front, team back — choose a side."},
                {"Hardest Subject You Faced", "The one that humbled you."},
                {"Most Embarrassing School Moment", "We've all been there. Tell yours first."},
                {"Proudest Achievement in School", "The award, the match, the moment you stood taller."},
                {"Describe Your School in One Word", "Just one."},
                {"Which Staff Member Will You Never Forget? Why?", "Not always a teacher — sometimes it's the gate man."},
                {"Which School Day Would You Relive?", "A specific day, a specific period, a specific feeling."},
                {"If You Were Principal for One Day, What Would You Change?", "The rule, the schedule, the food..."},
                {"\u201c[SCHOOL NAME] made me...\u201d", "Finish the sentence."},
                {"Your Biggest School Crush \u2014 optional", "You can keep this one to yourself; nobody's judging."},
                {"The Student Everyone Knew", "The legend. The one whose name was always on everyone's lips."},
                {"The Teacher Everyone Feared", "The raised eyebrow. The silence when they walked in."},
                {"Craziest Thing That Happened During Assembly", "The speeches, the singing, the chaos."},
                {"The School Rule You Hated Most", "And yes, we all broke it."},
                {"One Friend You Wish You Could Meet Again", "What would you tell them first?"},
                {"Your Most Unexpected School Moment", "The plot twist you never saw coming."},
                {"One Lesson School Taught You About Life", "Something you still carry today."},
                {"What Would You Do Differently If You Could Go Back?", "One small thing you'd change."},
                {"Write a Letter to Your Younger School Self", "What would you say, knowing everything you know now?"}
        };
        for (int i = 0; i < questions.length; i++) {
            if (!questionRepository.existsByDayNumber(i + 1)) {
                ChallengeQuestion q = new ChallengeQuestion();
                q.setDayNumber(i + 1);
                q.setQuestion(questions[i][0]);
                q.setHint(questions[i][1]);
                questionRepository.save(q);
            }
        }
    }

    private void seedTrivia(School school) {
        if (triviaQuestionRepository.count() > 0) {
            return;
        }
        // Questions whose answers are verifiable against the seeded demo data above.
        trivia( school, "How many questions are in the 30-day challenge?",
                List.of("30", "20", "50", "100"), 0);
        trivia(school, "Which Greenfield set graduated in 2019?",
                List.of("Set of 2019", "Set of 2020", "Set of 2021", "Set of 2022"), 0);
        trivia(school, "Who was Greenfield's debate team captain?",
                List.of("Ada Obi", "Chidi Okonkwo", "George Appiah", "Emeka Nwosu"), 1);
        trivia(school, "Which classmate was nicknamed \"Miss Calculator\"?",
                List.of("Daniella Mensah", "Hana Yusuf", "Fatima Bello", "Bisi Adeyemi"), 2);
        trivia(school, "Where is Greenfield's famous mango tree?",
                List.of("The school field", "Behind the science block", "By the gate", "Next to the library"), 1);
        trivia(school, "Which classmate was called \"Quiet Storm\"?",
                List.of("Bisi Adeyemi", "Chidi Okonkwo", "Emeka Nwosu", "Ada Obi"), 0);
        log.info("Seeded {} trivia questions.", triviaQuestionRepository.count());
    }

    private void trivia(School school, String question, List<String> options, int correctIndex) {
        TriviaQuestion t = new TriviaQuestion();
        t.setSchool(school);
        t.setQuestion(question);
        t.setOptions(options);
        t.setCorrectIndex(correctIndex);
        triviaQuestionRepository.save(t);
    }

    private void seedMemories(List<User> classmates) {
        // day -> (authorIndex, answer)
        Map<Integer, int[]> plan = Map.of(
                1, new int[]{0, 2, 4},
                2, new int[]{1, 3, 5},
                3, new int[]{0, 5},
                4, new int[]{2, 6},
                5, new int[]{1, 7},
                6, new int[]{3, 6},
                7, new int[]{4, 7},
                8, new int[]{0, 2},
                9, new int[]{5, 6},
                10, new int[]{1, 3, 4});
        Map<Integer, String[]> answers = Map.of(
                1, new String[]{
                        "I'm Ada — set of 2019. I sat in the second row by the window and my handwriting was the reason half the class passed notes.",
                        "Chidi here. Set 2020. Captain of the debate team and keeper of every secret the class ever told me.",
                        "Emeka, set 2020. If you remember the kid always eating during break, that was me."},
                2, new String[]{
                        "Everyone called me 'Quiet Storm' — quiet until exams, then suddenly unbeatable.",
                        "I was 'DJ Bisi' because I always had the latest songs on my phone.",
                        "Mine was 'Miss Calculator' — I did mental math faster than the teachers."},
                3, new String[]{
                        "Literature, no contest. We read things that made us feel grown.",
                        "Physics. The experiments where things actually exploded (safely, mostly)."},
                4, new String[]{
                        "Mr. Okafor, chemistry. He explained things like he was telling a story.",
                        "Mrs. Boateng. She made us believe our accents were beautiful.",
                        "Coach Mensah. He ran the field like it was a kingdom and we were his court."},
                5, new String[]{
                        "Inter-house sports day. My house won the relay by a step and we screamed for an hour.",
                        "Cultural day. I danced in front of the whole school and forgot the steps — best day ever.",
                        "The end-of-year talent show. George's magic trick went wrong and became legendary."},
                6, new String[]{
                        "Bisi fell asleep in class and answered a question in her dream. She said 'yes ma' to the wrong thing.",
                        "I once laughed so hard at a joke that milk came out of my nose during break. The whole class saw.",
                        "Someone put a whoopee cushion on the staff chair. Mrs. Boateng sat down. We were in trouble for WEEKS."},
                7, new String[]{
                        "Ada, Bisi and Dani. We planned a school magazine that never got printed, but we laughed writing it.",
                        "Chidi and George. We shared notes, snacks and alibis.",
                        "Emeka, Hana and Fatima. The trio behind every 'study group' that was mostly storytelling."},
                8, new String[]{
                        "I got detention for drawing in class. The drawing was of the teacher. Worth it.",
                        "Wrote lines 100 times: 'I will not talk during assembly.' I talked during assembly the very next day.",
                        "I was sent to stand outside for arguing about football with the geography teacher."},
                9, new String[]{
                        "The mango tree behind the science block. Half the school's best conversations happened there.",
                        "The library's back corner. Quiet, dusty, and mine.",
                        "The school field at 4pm, when the light went gold."},
                10, new String[]{
                        "The day our whole set sang the school anthem at the final assembly and no one got the words right.",
                        "Winning the regional debate. I still remember the silence before the applause.",
                        "The farewell party. We cried, we laughed, and someone's mum brought jollof for 80 people."});

        for (Map.Entry<Integer, int[]> entry : plan.entrySet()) {
            int day = entry.getKey();
            int[] authorIndexes = entry.getValue();
            String[] dayAnswers = answers.get(day);
            ChallengeQuestion question = questionRepository.findByDayNumber(day).orElseThrow();
            for (int i = 0; i < authorIndexes.length; i++) {
                User author = classmates.get(authorIndexes[i]);
                if (memoryRepository.findByUserIdAndDayNumber(author.getId(), day).isEmpty()) {
                    Memory m = new Memory();
                    m.setUser(author);
                    m.setQuestion(question);
                    m.setDayNumber(day);
                    m.setAnswer(dayAnswers[i]);
                    m.setMood(moodFor(day));
                    m.setCreatedAt(LocalDateTime.now().minusDays(30L - day));
                    memoryRepository.save(m);
                }
            }
        }

        // a few reactions + comments between classmates
        var memories = memoryRepository.findAllByUserIdOrderByDay(classmates.get(0).getId());
        if (!memories.isEmpty()) {
            for (int i = 1; i < classmates.size(); i++) {
                User liker = classmates.get(i);
                memories.forEach(m -> {
                    if (reactionRepository.findByMemoryIdAndUserId(m.getId(), liker.getId()).isEmpty()) {
                        Reaction r = new Reaction();
                        r.setMemory(m);
                        r.setUser(liker);
                        r.setType("LIKE");
                        reactionRepository.save(r);
                    }
                });
            }
            Memory first = memories.get(0);
            if (commentRepository.countByMemoryIdAndDeletedFalse(first.getId()) == 0) {
                Comment c = new Comment();
                c.setMemory(first);
                c.setUser(classmates.get(2));
                c.setBody("I was THERE. Crying all over again.");
                commentRepository.save(c);
            }
        }
        var chidiMemories = memoryRepository.findAllByUserIdOrderByDay(classmates.get(2).getId());
        if (!chidiMemories.isEmpty() && commentRepository.countByMemoryIdAndDeletedFalse(chidiMemories.get(0).getId()) == 0) {
            Comment c = new Comment();
            c.setMemory(chidiMemories.get(0));
            c.setUser(classmates.get(0));
            c.setBody("You were the best on that stage and you know it.");
            commentRepository.save(c);
        }

        // a few follows so the suggested list still has candidates
        int n = classmates.size();
        for (int i = 0; i < n; i++) {
            followIfAbsent(classmates.get(i), classmates.get((i + 1) % n));
            followIfAbsent(classmates.get(i), classmates.get((i + 3) % n));
        }
    }

    private void followIfAbsent(User follower, User following) {
        if (!followRepository.existsByFollowerIdAndFollowingId(follower.getId(), following.getId())) {
            Follow f = new Follow();
            f.setFollower(follower);
            f.setFollowing(following);
            followRepository.save(f);
        }
    }

    private void announcementOrCreate(String title, String body) {
        if (announcementRepository.findByActiveTrueOrderByCreatedAtDesc().stream()
                .noneMatch(a -> a.getTitle().equals(title))) {
            Announcement a = new Announcement();
            a.setTitle(title);
            a.setBody(body);
            announcementRepository.save(a);
        }
    }

    private static String moodFor(int day) {
        return switch (day % 5) {
            case 0 -> "joyful";
            case 1 -> "nostalgic";
            case 2 -> "funny";
            case 3 -> "proud";
            default -> "grateful";
        };
    }
}
