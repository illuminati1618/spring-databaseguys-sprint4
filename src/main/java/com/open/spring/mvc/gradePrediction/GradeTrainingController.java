package com.open.spring.mvc.gradePrediction;
import java.io.File;
import java.io.IOException;
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
import smile.regression.RandomForest;
@RestController
@RequestMapping("/api/grade-prediction")
public class GradeTrainingController {
    @Autowired
    private GradeTrainingRepository repository;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    private RandomForest model;
    private HashMap<Object, Object> encoders;
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PredictionRequest {
        private Double attendance;
        private Double workHabits;
        private Double behavior;
        private Double timeliness;
        private Double techSense;
        private Double techTalk;
        private Double techGrowth;
        private Double advocacy;
        private Double communication;
        private Double integrity;
        private Double organization;
    }
    private ObjectMapper objectMapper = new ObjectMapper();
    @GetMapping("/train")
    public ResponseEntity<?> train() {
        try {
            String sql = "CREATE TABLE IF NOT EXISTS grade_training (" +
                         "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                         "student_id INTEGER, " +
                         "attendance REAL, " +
                         "work_habits REAL, " +
                         "behavior REAL, " +
                         "timeliness REAL, " +
                         "tech_sense REAL, " +
                         "tech_talk REAL, " +
                         "tech_growth REAL, " +
                         "advocacy REAL, " +
                         "communication REAL, " +
                         "integrity REAL, " +
                         "organization REAL, " +
                         "final_grade INTEGER)";
            jdbcTemplate.execute(sql);
            String jsonPath = "src/main/java/com/open/spring/mvc/gradePrediction/fake-records-new-new.json";
            File jsonFile = new File(jsonPath);
            JsonNode rootNodeFromFile = objectMapper.readTree(jsonFile);
            
            int n = rootNodeFromFile.size();
            double[] attendance = new double[n];
            double[] workHabits = new double[n];
            double[] behavior = new double[n];
            double[] timeliness = new double[n];
            double[] techSense = new double[n];
            double[] techTalk = new double[n];
            double[] techGrowth = new double[n];
            double[] advocacy = new double[n];
            double[] communication = new double[n];
            double[] integrity = new double[n];
            double[] organization = new double[n];
            double[] finalGrade = new double[n];
            
            List<GradeTraining> records = new ArrayList<>();
            boolean saveToDb = repository.count() == 0;
            for (int i = 0; i < n; i++) {
                JsonNode node = rootNodeFromFile.get(i);
                attendance[i] = node.path("attendance").asDouble();
                workHabits[i] = node.path("work_habits").asDouble();
                behavior[i] = node.path("behavior").asDouble();
                timeliness[i] = node.path("timeliness").asDouble();
                techSense[i] = node.path("tech_sense").asDouble();
                techTalk[i] = node.path("tech_talk").asDouble();
                techGrowth[i] = node.path("tech_growth").asDouble();
                advocacy[i] = node.path("advocacy").asDouble();
                communication[i] = node.path("communication").asDouble();
                integrity[i] = node.path("integrity").asDouble();
                organization[i] = node.path("organization").asDouble();
                finalGrade[i] = node.path("final_grade").asDouble();
                if (saveToDb) {
                    GradeTraining ap = new GradeTraining();
                    ap.setStudentId(node.path("student_id").asLong());
                    ap.setAttendance(attendance[i]);
                    ap.setWorkHabits(workHabits[i]);
                    ap.setBehavior(behavior[i]);
                    ap.setTimeliness(timeliness[i]);
                    ap.setTechSense(techSense[i]);
                    ap.setTechTalk(techTalk[i]);
                    ap.setTechGrowth(techGrowth[i]);
                    ap.setAdvocacy(advocacy[i]);
                    ap.setCommunication(communication[i]);
                    ap.setIntegrity(integrity[i]);
                    ap.setOrganization(organization[i]);
                    // Fix: Cast double to Double for wrapper class
                    ap.setFinalGrade(Double.valueOf(finalGrade[i]));
                    records.add(ap);
                }
            }
            
            if (saveToDb) {
                repository.saveAll(records);
            }
            DataFrame data = DataFrame.of(
                DoubleVector.of("attendance", attendance),
                DoubleVector.of("work_habits", workHabits),
                DoubleVector.of("behavior", behavior),
                DoubleVector.of("timeliness", timeliness),
                DoubleVector.of("tech_sense", techSense),
                DoubleVector.of("tech_talk", techTalk),
                DoubleVector.of("tech_growth", techGrowth),
                DoubleVector.of("advocacy", advocacy),
                DoubleVector.of("communication", communication),
                DoubleVector.of("integrity", integrity),
                DoubleVector.of("organization", organization),
                DoubleVector.of("final_grade", finalGrade)
            );
            data = data.select("final_grade", "attendance", "work_habits", "behavior", "timeliness", 
                             "tech_sense", "tech_talk", "tech_growth", "advocacy", "communication", 
                             "integrity", "organization");
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
            String[] numericFeatures = {"attendance", "work_habits", "behavior", "timeliness", 
                                       "tech_sense", "tech_talk", "tech_growth", "advocacy", "communication", 
                                       "integrity", "organization"};
            
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
            return ResponseEntity.ok(Map.of(
                "predicted_final_grade", prediction,
                "status", "success"
            ));
        } catch (DataAccessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Prediction failed: " + e.getMessage()));
        }
    }
    @GetMapping("/records")
    public ResponseEntity<List<GradeTraining>> getRecords() {
        return ResponseEntity.ok(repository.findAll());
    }
}