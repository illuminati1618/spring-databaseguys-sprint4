package com.open.spring.system;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.open.spring.mvc.announcement.Announcement;
import com.open.spring.mvc.announcement.AnnouncementJPA;
import com.open.spring.mvc.assignments.Assignment;
import com.open.spring.mvc.assignments.AssignmentJpaRepository;
import com.open.spring.mvc.assignments.AssignmentSubmission;
import com.open.spring.mvc.assignments.AssignmentSubmissionJPA;
import com.open.spring.mvc.bank.BankJpaRepository;
import com.open.spring.mvc.bank.BankService;
import com.open.spring.mvc.bathroom.BathroomQueue;
import com.open.spring.mvc.bathroom.BathroomQueueJPARepository;
import com.open.spring.mvc.bathroom.Issue;
import com.open.spring.mvc.bathroom.IssueJPARepository;
import com.open.spring.mvc.bathroom.Teacher;
import com.open.spring.mvc.bathroom.TeacherJpaRepository;
import com.open.spring.mvc.bathroom.TinkleJPARepository;
import com.open.spring.mvc.comment.Comment;
import com.open.spring.mvc.comment.CommentJPA;
import com.open.spring.mvc.hardAssets.HardAssetsRepository;
import com.open.spring.mvc.jokes.Jokes;
import com.open.spring.mvc.jokes.JokesJpaRepository;
import com.open.spring.mvc.media.MediaJpaRepository;
import com.open.spring.mvc.media.Score;
import com.open.spring.mvc.note.Note;
import com.open.spring.mvc.note.NoteJpaRepository;
import com.open.spring.mvc.person.Person;
import com.open.spring.mvc.person.PersonDetailsService;
import com.open.spring.mvc.person.PersonJpaRepository;
import com.open.spring.mvc.person.PersonRole;
import com.open.spring.mvc.person.PersonRoleJpaRepository;

// Adventure sub-APIs have been unified into a single Adventure entity
import com.open.spring.mvc.student.StudentQueue;
import com.open.spring.mvc.student.StudentQueueJPARepository;
import com.open.spring.mvc.synergy.SynergyGrade;
import com.open.spring.mvc.synergy.SynergyGradeJpaRepository;
import com.open.spring.mvc.quiz.QuizScore;
import com.open.spring.mvc.quiz.QuizScoreRepository;
import com.open.spring.mvc.resume.Resume;
import com.open.spring.mvc.resume.ResumeJpaRepository;
import com.open.spring.mvc.stats.Stats; // curators - stats api
import com.open.spring.mvc.stats.StatsRepository;
import com.open.spring.mvc.rpg.adventure.Adventure;
import com.open.spring.mvc.rpg.adventure.AdventureJpaRepository;
import com.open.spring.mvc.rpg.games.Game;
import com.open.spring.mvc.rpg.games.UnifiedGameRepository;


@Component
@Configuration // Scans Application for ModelInit Bean, this detects CommandLineRunner
public class ModelInit {
    @Autowired JokesJpaRepository jokesRepo;
    @Autowired HardAssetsRepository hardAssetsRepository;
    @Autowired NoteJpaRepository noteRepo;
    @Autowired PersonRoleJpaRepository roleJpaRepository;
    @Autowired PersonDetailsService personDetailsService;
    @Autowired PersonJpaRepository personJpaRepository;
    @Autowired AnnouncementJPA announcementJPA;
    @Autowired CommentJPA CommentJPA;
    @Autowired TinkleJPARepository tinkleJPA;
    @Autowired BathroomQueueJPARepository queueJPA;
    @Autowired TeacherJpaRepository teacherJPARepository;
    @Autowired IssueJPARepository issueJPARepository;
    @Autowired
    DataSource dataSource;
    @Autowired
    DatabaseConfig databaseConfig;
    @Autowired
    AdventureJpaRepository adventureJpaRepository;
    @Autowired
    UnifiedGameRepository gameJpaRepository;
    
    @Autowired AssignmentJpaRepository assignmentJpaRepository;
    @Autowired AssignmentSubmissionJPA submissionJPA;
    @Autowired SynergyGradeJpaRepository gradeJpaRepository;
    @Autowired StudentQueueJPARepository studentQueueJPA;
    @Autowired BankJpaRepository bankJpaRepository;
    @Autowired BankService bankService;
    
    @Autowired MediaJpaRepository mediaJpaRepository;
    @Autowired QuizScoreRepository quizScoreRepository;
    @Autowired ResumeJpaRepository resumeJpaRepository;
    @Autowired StatsRepository statsRepository; // curators - stats

    @Bean
    @Transactional
    CommandLineRunner run() {
        return args -> {
            // Ensure unified `adventure` table exists before any seeding
            try {
                if (dataSource != null) {
                    try (Connection conn = dataSource.getConnection(); Statement st = conn.createStatement()) {
                        // ========== DATABASE CLEANUP - START ==========
                        System.out.println("Starting conservative database cleanup...");
                        
                        // 1. Drop User tables (0 rows, 0 code references)
                        try {
                            st.execute("DROP TABLE IF EXISTS user;");
                            st.execute("DROP TABLE IF EXISTS user_seq;");
                            System.out.println("Dropped 'user' tables");
                        } catch (SQLException e) {
                            System.out.println("WARNING: Could not drop user tables");
                        }
                        
                        // 2. Drop responses table (0 rows, 0 code references)
                        try {
                            st.execute("DROP TABLE IF EXISTS responses;");
                            System.out.println("Dropped 'responses' table");
                        } catch (SQLException e) {
                            System.out.println("WARNING: Could not drop responses table");
                        }
                        
                        // 3. Drop student table (0 rows, 0 code references)
                        try {
                            st.execute("DROP TABLE IF EXISTS student;");
                            System.out.println("Dropped 'student' table");
                        } catch (SQLException e) {
                            System.out.println("WARNING: Could not drop student table");
                        }
                        
                        // 4. Drop person_user_mapping tables (0 rows, 0 code references)
                        try {
                            st.execute("DROP TABLE IF EXISTS person_user_mapping;");
                            st.execute("DROP TABLE IF EXISTS person_user_mapping_seq;");
                            System.out.println("Dropped 'person_user_mapping' tables");
                        } catch (SQLException e) {
                            System.out.println("WARNING: Could not drop person_user_mapping tables");
                        }
                        
                        // 5. Drop assignment_queue table (0 rows, migrated to JSON)
                        try {
                            st.execute("DROP TABLE IF EXISTS assignment_queue;");
                            st.execute("DROP TABLE IF EXISTS assignment_queue_seq;");
                            System.out.println("Dropped 'assignment_queue' tables");
                        } catch (SQLException e) {
                            System.out.println("WARNING: Could not drop assignment_queue tables");
                        }
                        
                        // 6. Drop orphaned Hibernate Envers audit tables (0 @Audited annotations)
                        String[] auditTables = {
                            "HTE_announcement", "HTE_assignment", "HTE_assignment_queue",
                            "HTE_assignment_submission", "HTE_bank", "HTE_bathroom_queue",
                            "HTE_blackjack", "HTE_comment", "HTE_game", "HTE_gemini",
                            "HTE_groups", "HTE_hall_pass", "HTE_issue", "HTE_jokes",
                            "HTE_note", "HTE_person", "HTE_person_role", "HTE_person_user_mapping",
                            "HTE_plant", "HTE_progress_bar", "HTE_quiz_scores", "HTE_resume",
                            "HTE_student_queue", "HTE_synergy_grade", "HTE_synergy_grade_request",
                            "HTE_tasks", "HTE_teacher", "HTE_teacher_grading_team_teach",
                            "HTE_tinkle", "HTE_train", "HTE_user", "HTE_user_stocks_table",
                            "HT_groups", "HT_person", "HT_submitter"
                        };
                        
                        int droppedAuditTables = 0;
                        for (String tableName : auditTables) {
                            try {
                                st.execute("DROP TABLE IF EXISTS " + tableName + ";");
                                droppedAuditTables++;
                            } catch (SQLException ignored) {}
                        }
                        System.out.println("Dropped " + droppedAuditTables + " orphaned audit tables");
                        
                        System.out.println("Conservative database cleanup complete!");
                        // ========== DATABASE CLEANUP - END ==========

                        // Build database-specific CREATE TABLE statement for adventure
                        // Note: For MySQL, Hibernate will auto-create tables from JPA entities (ddl-auto=update)
                        //       but we still run CREATE TABLE IF NOT EXISTS as a safety net.
                        //       For SQLite, we must create tables manually since ddl-auto=none.
                        String create;
                        if (databaseConfig.isMySQL()) {
                            // MySQL syntax
                            create = "CREATE TABLE IF NOT EXISTS adventure ("
                                    + "id INT AUTO_INCREMENT PRIMARY KEY,"
                                    + "person_id INT,"
                                    + "person_uid TEXT,"
                                    + "question_id INT,"
                                    + "question_title TEXT,"
                                    + "question_content TEXT,"
                                    + "question_category TEXT,"
                                    + "question_points INT,"
                                    + "choice_id INT,"
                                    + "choice_text TEXT,"
                                    + "choice_is_correct INT,"
                                    + "answer_is_correct INT,"
                                    + "answer_content TEXT,"
                                    + "chat_score INT,"
                                    + "rubric_ruid TEXT,"
                                    + "rubric_criteria TEXT,"
                                    + "balance DOUBLE,"
                                    + "created_at DATETIME DEFAULT CURRENT_TIMESTAMP,"
                                    + "details TEXT"
                                    + ");";
                        } else {
                            // SQLite syntax
                            create = "CREATE TABLE IF NOT EXISTS adventure ("
                                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                                    + "person_id INTEGER,"
                                    + "person_uid TEXT,"
                                    + "question_id INTEGER,"
                                    + "question_title TEXT,"
                                    + "question_content TEXT,"
                                    + "question_category TEXT,"
                                    + "question_points INTEGER,"
                                    + "choice_id INTEGER,"
                                    + "choice_text TEXT,"
                                    + "choice_is_correct INTEGER,"
                                    + "answer_is_correct INTEGER,"
                                    + "answer_content TEXT,"
                                    + "chat_score INTEGER,"
                                    + "rubric_ruid TEXT,"
                                    + "rubric_criteria TEXT,"
                                    + "balance REAL,"
                                    + "created_at DATETIME DEFAULT (strftime('%Y-%m-%d %H:%M:%f','now')),"
                                    + "details TEXT"
                                    + ");";
                        }
                        st.execute(create);
                        System.out.println("Ensured 'adventure' table exists");
                        // Seed default adventure rows if none exist (instantiate in code)
                        try {
                            long advCount = 0L;
                            try { advCount = adventureJpaRepository.count(); } catch (Exception ignore) { advCount = 0L; }
                            if (advCount == 0L) {
                                Adventure[] defaults = Adventure.init();
                                for (Adventure a : defaults) {
                                    try { adventureJpaRepository.save(a); } catch (Exception ignored) {}
                                }
                                System.out.println("Seeded default Adventure rows via Adventure.init()");
                            }
                            // Ensure 'details' column exists and migrate existing columns into JSON 'details'
                            // Note: For MySQL, 'details' column is already included in CREATE TABLE above
                            // For SQLite, we need to add it if it doesn't exist
                            if (!databaseConfig.isMySQL()) {
                                try {
                                    st.execute("ALTER TABLE adventure ADD COLUMN details TEXT;");
                                    System.out.println("Added 'details' column to 'adventure' table");
                                } catch (SQLException ignore) {
                                    // column may already exist; ignore
                                }
                            }
                                try {
                                    // Migrate existing Adventure rows into details JSON if empty
                                    Iterable<Adventure> all = adventureJpaRepository.findAll();
                                    for (Adventure adv : all) {
                                        if (adv.getDetails() == null || adv.getDetails().trim().isEmpty()) {
                                            String choiceText = adv.getChoiceText();
                                            String answerContent = adv.getAnswerContent();
                                            String rubricCriteria = adv.getRubricCriteria();
                                            String rubricRuid = adv.getRubricRuid();
                                            StringBuilder sb = new StringBuilder();
                                            sb.append('{');
                                            sb.append("\"choiceId\":").append(adv.getChoiceId() == null ? "null" : adv.getChoiceId()).append(',');
                                            sb.append("\"choiceText\":").append(choiceText == null ? "null" : ("\"" + choiceText.replace("\\", "\\\\").replace("\"", "\\\"") + "\"" )).append(',');
                                            sb.append("\"choiceIsCorrect\":").append(adv.getChoiceIsCorrect() == null ? "null" : adv.getChoiceIsCorrect()).append(',');
                                            sb.append("\"answerIsCorrect\":").append(adv.getAnswerIsCorrect() == null ? "null" : adv.getAnswerIsCorrect()).append(',');
                                            sb.append("\"answerContent\":").append(answerContent == null ? "null" : ("\"" + answerContent.replace("\\", "\\\\").replace("\"", "\\\"") + "\"" )).append(',');
                                            sb.append("\"chatScore\":").append(adv.getChatScore() == null ? "null" : adv.getChatScore()).append(',');
                                            sb.append("\"rubricRuid\":").append(rubricRuid == null ? "null" : ("\"" + rubricRuid.replace("\\", "\\\\").replace("\"", "\\\"") + "\"" )).append(',');
                                            sb.append("\"rubricCriteria\":").append(rubricCriteria == null ? "null" : ("\"" + rubricCriteria.replace("\\", "\\\\").replace("\"", "\\\"") + "\"" ));
                                            sb.append('}');
                                            adv.setDetails(sb.toString());
                                            try { adventureJpaRepository.save(adv); } catch (Exception ignored) {}
                                        }
                                    }
                                    System.out.println("Migrated Adventure rows into 'details' JSON where missing");
                                } catch (Exception ignore) {
                                }
                            } catch (Throwable t) {
                                // ignore migration failures
                            }
                        } catch (Throwable t) {
                            // don't fail startup for seeding issues
                        }
                    }
                }
            } catch (SQLException e) {
                System.err.println("Failed to ensure 'adventure' table: " + e.getMessage());
            }

                // Ensure unified `games` table exists before any seeding
                // Note: For MySQL, Hibernate will auto-create tables from JPA entities (ddl-auto=update)
                //       but we still run CREATE TABLE IF NOT EXISTS as a safety net.
                //       For SQLite, we must create tables manually since ddl-auto=none.
                try {
                    if (dataSource != null) {
                        try (Connection conn = dataSource.getConnection(); Statement st = conn.createStatement()) {
                            // Build database-specific CREATE TABLE statement for games
                            String createGames;
                            if (databaseConfig.isMySQL()) {
                                // MySQL syntax
                                createGames = "CREATE TABLE IF NOT EXISTS games ("
                                        + "id INT AUTO_INCREMENT PRIMARY KEY,"
                                        + "person_id INT,"
                                        + "person_uid TEXT,"
                                        + "type TEXT,"
                                        + "tx_id TEXT,"
                                        + "bet_amount DOUBLE,"
                                        + "amount DOUBLE,"
                                        + "balance DOUBLE,"
                                        + "result TEXT,"
                                        + "success INT,"
                                        + "details TEXT,"
                                        + "created_at DATETIME DEFAULT CURRENT_TIMESTAMP"
                                        + ");";
                            } else {
                                // SQLite syntax
                                createGames = "CREATE TABLE IF NOT EXISTS games ("
                                        + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                                        + "person_id INTEGER,"
                                        + "person_uid TEXT,"
                                        + "type TEXT,"
                                        + "tx_id TEXT,"
                                        + "bet_amount REAL,"
                                        + "amount REAL,"
                                        + "balance REAL,"
                                        + "result TEXT,"
                                        + "success INTEGER,"
                                        + "details TEXT,"
                                        + "created_at DATETIME DEFAULT (strftime('%Y-%m-%d %H:%M:%f','now'))"
                                        + ");";
                            }
                            st.execute(createGames);
                            System.out.println("Ensured 'games' table exists");
                            try {
                                long gameCount = 0L;
                                try { gameCount = gameJpaRepository.count(); } catch (Exception ignore) { gameCount = 0L; }
                                if (gameCount == 0L) {
                                    Game[] defaults = Game.init();
                                    for (Game g : defaults) {
                                        try { gameJpaRepository.save(g); } catch (Exception ignored) {}
                                    }
                                    System.out.println("Seeded default Game rows via Game.init()");
                                }
                            } catch (Throwable t) {
                            }
                        }
                    }
                } catch (SQLException e) {
                    System.err.println("Failed to ensure 'games' table: " + e.getMessage());
                }

            if (new File("volumes/.skip-modelinit").exists()) {
                System.out.println("Skip flag detected, ModelInit will not run");
                return;
            }

            long personCount = personJpaRepository.count();
            if (personCount > 0) {
                System.out.println("Database already contains " + personCount + " persons. Skipping ModelInit...");
                return;
            }
        
            System.out.println("Loading default sample data...");
            Person[] personArray = Person.init();
            for (Person person : personArray) {
                List<Person> personFound = personDetailsService.list(person.getName(), person.getEmail());
                if (personFound.isEmpty()) { 
                    List<PersonRole> updatedRoles = new ArrayList<>();
                    for (PersonRole role : person.getRoles()) {
                        PersonRole roleFound = roleJpaRepository.findByName(role.getName());
                        if (roleFound == null) {
                            roleJpaRepository.save(role);
                            roleFound = role;
                        }
                        updatedRoles.add(roleFound);
                    }
                    person.setRoles(updatedRoles);
                    
                    // Ensure password is not null or empty
                    if (person.getPassword() == null || person.getPassword().isEmpty()) {
                        person.setPassword("defaultPassword123"); // Set a default password or handle differently
                    }
                    
                    personDetailsService.save(person);
                    
                    String text = "Test " + person.getEmail();
                    Note n = new Note(text, person);
                    noteRepo.save(n);
                }
            }
            
            List<Announcement> announcements = Announcement.init();
            for (Announcement announcement : announcements) {
                Announcement announcementFound = announcementJPA.findByAuthor(announcement.getAuthor());  
                if (announcementFound == null) {
                    announcementJPA.save(new Announcement(announcement.getAuthor(), announcement.getTitle(), announcement.getBody(), announcement.getTags())); // JPA save
                }
            }
            // Adventure sub-APIs have been merged into a single Adventure table/entity.



            
            List<Comment> Comments = Comment.init();
            for (Comment Comment : Comments) {
                List<Comment> CommentFound = CommentJPA.findByAssignment(Comment.getAssignment()); 
                if (CommentFound.isEmpty()) {
                    CommentJPA.save(new Comment(Comment.getAssignment(), Comment.getAuthor(), Comment.getText())); // JPA save
                }
            }


            String[] jokesArray = Jokes.init();
            for (String joke : jokesArray) {
                List<Jokes> jokeFound = jokesRepo.findByJokeIgnoreCase(joke);  // JPA lookup
                if (jokeFound.size() == 0) {
                    jokesRepo.save(new Jokes(null, joke, 0, 0)); // JPA save
                }
            }

            // Tinkle[] tinkleArray = Tinkle.init(personArray);
            // for(Tinkle tinkle: tinkleArray) {
            //     // List<Tinkle> tinkleFound = 
            //     Optional<Tinkle> tinkleFound = tinkleJPA.findByPersonName(tinkle.getPersonName());
            //     if(tinkleFound.isEmpty()) {
            //         tinkleJPA.save(tinkle);
            //     }
            // }

            BathroomQueue[] queueArray = BathroomQueue.init();
            for(BathroomQueue queue: queueArray) {
                Optional<BathroomQueue> queueFound = queueJPA.findByTeacherEmail(queue.getTeacherEmail());
                if(queueFound.isEmpty()) {
                    queueJPA.save(queue);
                }
            }

            StudentQueue[] studentQueueArray = StudentQueue.init();
            for(StudentQueue queue: studentQueueArray) {
                Optional<StudentQueue> queueFound = studentQueueJPA.findByTeacherEmail(queue.getTeacherEmail());
                if(queueFound.isEmpty()) {
                    studentQueueJPA.save(queue);
                }
            }

            // Teacher API is populated with starting announcements
            List<Teacher> teachers = Teacher.init();
            for (Teacher teacher : teachers) {
            List<Teacher> existTeachers = teacherJPARepository.findByFirstnameIgnoreCaseAndLastnameIgnoreCase(teacher.getFirstname(), teacher.getLastname());
                if(existTeachers.isEmpty())
               teacherJPARepository.save(teacher); // JPA save
            }
            
            // Issue database initialization
            Issue[] issueArray = Issue.init();
            for (Issue issue : issueArray) {
                List<Issue> issueFound = issueJPARepository.findByIssueAndBathroomIgnoreCase(issue.getIssue(), issue.getBathroom());
                if (issueFound.isEmpty()) {
                    issueJPARepository.save(issue);
                }
            }
            
            // Assignment database is populated with sample assignments
            Assignment[] assignmentArray = Assignment.init();
            for (Assignment assignment : assignmentArray) {
                Assignment assignmentFound = assignmentJpaRepository.findByName(assignment.getName());
                if (assignmentFound == null) { // if the assignment doesn't exist
                    Assignment newAssignment = new Assignment(assignment.getName(), assignment.getType(), assignment.getDescription(), assignment.getPoints(), assignment.getDueDate());
                    assignmentJpaRepository.save(newAssignment);

                    // create sample submission
                    submissionJPA.save(new AssignmentSubmission(newAssignment, personJpaRepository.findByUid("madam"), "test submission","test comment", false));
                }
            }

            // Now call the non-static init() method
            String[][] gradeArray = SynergyGrade.init();
            for (String[] gradeInfo : gradeArray) {
                Double gradeValue = Double.parseDouble(gradeInfo[0]);
                Assignment assignment = assignmentJpaRepository.findByName(gradeInfo[1]);
                Person student = personJpaRepository.findByUid(gradeInfo[2]);

                SynergyGrade gradeFound = gradeJpaRepository.findByAssignmentAndStudent(assignment, student);
                if (gradeFound == null) { // If the grade doesn't exist
                    SynergyGrade newGrade = new SynergyGrade(gradeValue, assignment, student);
                    gradeJpaRepository.save(newGrade);
                }
            }


            //Media Bias Table

            List<Score> scores = new ArrayList<>();
            scores.add(new Score("Thomas Edison", 0));
            for (Score score : scores) {
                List<Score> existingPlayers = mediaJpaRepository.findByPersonName(score.getPersonName());

                if (existingPlayers.isEmpty()) {
                    mediaJpaRepository.save(score);
                }
            }

            // Quiz Score initialization (guarded in case the table doesn't exist yet)
            try {
                QuizScore[] quizScoreArray = QuizScore.init();
                for (QuizScore quizScore : quizScoreArray) {
                    List<QuizScore> existingScores = quizScoreRepository
                        .findByUsernameIgnoreCaseOrderByScoreDesc(quizScore.getUsername());

                    boolean scoreExists = existingScores.stream()
                        .anyMatch(s -> s.getScore() == quizScore.getScore());

                    if (!scoreExists) {
                        quizScoreRepository.save(quizScore);
                    }
                }
            } catch (Exception ignored) {
                // If the quiz_scores table is missing or unavailable at startup, skip seeding
            }

            // Resume initialization via static init on Resume class (guard missing table)
            try {
                Resume[] resumes = Resume.init();
                for (Resume resume : resumes) {
                    Optional<Resume> existing = resumeJpaRepository.findByUsername(resume.getUsername());
                    if (existing.isEmpty()) {
                        resumeJpaRepository.save(resume);
                    }
                }
            } catch (Exception ignored) {
            }

            try { // initialize Stats data
                Stats[] statsArray = {
                    new Stats(null, "tobytest", "frontend", 1, Boolean.TRUE, 185.0, .92),
                    new Stats(null, "tobytest", "backend", 1, Boolean.FALSE, 0.0, null),
                    new Stats(null, "tobytest", "ai", 2, Boolean.TRUE, 240.5, .95),
                    new Stats(null, "hoptest", "data", 1, Boolean.TRUE, 142.3, .88),
                    new Stats(null, "hoptest", "resume", 3, Boolean.FALSE, 15.2, null),
                    new Stats(null, "curietest", "frontend", 2, Boolean.TRUE, 98.6, 0.90),
                    new Stats(null, "curietest", "backend", 2, Boolean.FALSE, 35.4, null),
                };

                for (Stats stats : statsArray) {
                    Optional<Stats> statsFound = statsRepository.findByUsernameAndModuleAndSubmodule(
                            stats.getUsername(), stats.getModule(), stats.getSubmodule());
                    if (statsFound.isEmpty()) {
                        statsRepository.save(stats);
                    }
                }
            } catch (Exception e) {
                // Handle exception, e.g., log it, but don't stop startup
                System.err.println("Error initializing Stats data: " + e.getMessage());
            }
        };
    }
}
