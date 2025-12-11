package com.open.spring.mvc.academicProgress;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import smile.data.DataFrame;
import smile.data.Tuple;
import smile.data.formula.Formula;
import smile.data.vector.BaseVector;
import smile.data.vector.DoubleVector;
import smile.data.vector.IntVector;
import smile.regression.RandomForest;

@RestController
@RequestMapping("/api/academic-progress")
public class AcademicProgressController {

    @Autowired
    private AcademicProgressRepository repository;

    @Autowired
    private AcademicPredictionRepository predictionRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private RandomForest model;
    private HashMap<Object, Object> encoders;
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PredictionRequest {
        private String assignmentType;
        private String difficultyLevel;
        private String topic;
        private String completionStatus;
        private Integer hoursStudied;
        private Integer attendance;
    }

    private ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping("/train")
    public ResponseEntity<?> train() {
        try {
            String sql = "CREATE TABLE IF NOT EXISTS academic_progress (" +
                         "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                         "student_id INTEGER, " +
                         "assignment_completion_rate REAL, " +
                         "average_assignment_score REAL, " +
                         "collegeboard_quiz_average REAL, " +
                         "office_hours_visits INTEGER, " +
                         "conduct INTEGER, " +
                         "work_habit INTEGER, " +
                         "github_contributions INTEGER, " +
                         "final_grade INTEGER)";
            jdbcTemplate.execute(sql);

            String jsonPath = "src/main/java/com/open/spring/mvc/academicProgress/fake-records-new.json";
            File jsonFile = new File(jsonPath);
            JsonNode rootNodeFromFile = objectMapper.readTree(jsonFile);
            
            int n = rootNodeFromFile.size();
            double[] assignmentCompletionRate = new double[n];
            double[] averageAssignmentScore = new double[n];
            double[] collegeboardQuizAverage = new double[n];
            int[] officeHoursVisits = new int[n];
            int[] conduct = new int[n];
            int[] workHabit = new int[n];
            int[] githubContributions = new int[n];
            int[] finalGrade = new int[n];

            List<AcademicProgress> records = new ArrayList<>();
            boolean saveToDb = repository.count() == 0;

            for (int i = 0; i < n; i++) {
                JsonNode node = rootNodeFromFile.get(i);
                
                assignmentCompletionRate[i] = node.path("assignment_completion_rate").asDouble();
                averageAssignmentScore[i] = node.path("average_assignment_score").asDouble();
                collegeboardQuizAverage[i] = node.path("collegeboard_quiz_average").asDouble();
                officeHoursVisits[i] = node.path("office_hours_visits").asInt();
                conduct[i] = node.path("conduct").asInt();
                workHabit[i] = node.path("work_habit").asInt();
                githubContributions[i] = node.path("github_contributions").asInt();
                finalGrade[i] = node.path("final_grade").asInt();

                if (saveToDb) {
                    AcademicProgress ap = new AcademicProgress();
                    ap.setStudentId(node.path("student_id").asLong());
                    ap.setAssignmentCompletionRate(assignmentCompletionRate[i]);
                    ap.setAverageAssignmentScore(averageAssignmentScore[i]);
                    ap.setCollegeboardQuizAverage(collegeboardQuizAverage[i]);
                    ap.setOfficeHoursVisits(officeHoursVisits[i]);
                    ap.setConduct(conduct[i]);
                    ap.setWorkHabit(workHabit[i]);
                    ap.setGithubContributions(githubContributions[i]);
                    ap.setFinalGrade(finalGrade[i]);
                    records.add(ap);
                }
            }
            
            if (saveToDb) {
                repository.saveAll(records);
            }

            DataFrame data = DataFrame.of(
                DoubleVector.of("assignment_completion_rate", assignmentCompletionRate),
                DoubleVector.of("average_assignment_score", averageAssignmentScore),
                DoubleVector.of("collegeboard_quiz_average", collegeboardQuizAverage),
                IntVector.of("office_hours_visits", officeHoursVisits),
                IntVector.of("conduct", conduct),
                IntVector.of("work_habit", workHabit),
                IntVector.of("github_contributions", githubContributions),
                IntVector.of("final_grade", finalGrade)
            );

            System.out.println("DataFrame BEFORE:");
            System.out.println(data.toString());
            data = data.select("final_grade", "assignment_completion_rate", "average_assignment_score", 
                             "collegeboard_quiz_average", "office_hours_visits", "conduct", 
                             "work_habit", "github_contributions");


            System.out.println("DataFrame AFTER:");
            System.out.println(data.toString());

            this.encoders = new HashMap<>();

            Formula formula = Formula.lhs("final_grade");
            
            this.model = RandomForest.fit(formula, data);

            return ResponseEntity.ok(Map.of(
                "message", "Model trained successfully",
                "rows_processed", data.nrows(),
                "columns", data.names(),
                "model_type", "RandomForest Regression"
            ));

        } catch (IOException | DataAccessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Training failed: " + e.getMessage()));
        }
    }

    @PostMapping("/predict")
    public ResponseEntity<?> predict(@RequestBody Map<String, Object> features) {
        if (model == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Model not trained. Call /train first."));
        }

        try {
            String sql = "CREATE TABLE IF NOT EXISTS academic_prediction (" +
                         "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                         "assignment_completion_rate REAL, " +
                         "average_assignment_score REAL, " +
                         "collegeboard_quiz_average REAL, " +
                         "office_hours_visits INTEGER, " +
                         "conduct INTEGER, " +
                         "work_habit INTEGER, " +
                         "github_contributions INTEGER, " +
                         "predicted_score REAL, " +
                         "created_at INTEGER)";
            jdbcTemplate.execute(sql);

            String[] numericFeatures = {"assignment_completion_rate", "average_assignment_score", 
                                       "collegeboard_quiz_average", "office_hours_visits", 
                                       "conduct", "work_habit", "github_contributions"};
            
            List<BaseVector> vectors = new ArrayList<>();

            for (String col : numericFeatures) {
                Number val = (Number) features.get(col);
                if (val == null) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Missing feature: " + col));
                }
                vectors.add(DoubleVector.of(col, new double[]{ val.doubleValue() }));
            }

            DataFrame singleRow = DataFrame.of(vectors.toArray(BaseVector[]::new));
            Tuple instance = singleRow.stream().findFirst().orElseThrow();

            double prediction = model.predict(instance);

            AcademicPrediction record = new AcademicPrediction(
                ((Number) features.get("assignment_completion_rate")).doubleValue(),
                ((Number) features.get("average_assignment_score")).doubleValue(),
                ((Number) features.get("collegeboard_quiz_average")).doubleValue(),
                ((Number) features.get("office_hours_visits")).intValue(),
                ((Number) features.get("conduct")).intValue(),
                ((Number) features.get("work_habit")).intValue(),
                ((Number) features.get("github_contributions")).intValue(),
                prediction
            );
            predictionRepository.save(record);

            return ResponseEntity.ok(Map.of(
                "predicted_final_grade", prediction,
                "status", "success",
                "prediction_id", record.getId()
            ));

        } catch (DataAccessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Prediction failed: " + e.getMessage()));
        }
    }

    @GetMapping("/records")
    public ResponseEntity<List<AcademicProgress>> getRecords() {
        return ResponseEntity.ok(repository.findAll());
    }
}
