package in.gov.chennaicorporation.mobileservice.gccChildrenSurvey.service;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class ChildSurveyService {

    @Autowired
    JdbcTemplate jdbcChildSurveyTemplate;

    @Autowired
    public void setDataSource(@Qualifier("mysqlChildSurveyDataSource") DataSource childSurveyDataSource) {
        this.jdbcChildSurveyTemplate = new JdbcTemplate(childSurveyDataSource);
    }

    public List<Map<String, Object>> getSurveyQuestions() {

        // Step 1: Get all child_qids
        String childSql = "SELECT DISTINCT child_qid FROM child_question_mapping " +
                "WHERE isactive=1 AND isdelete=0";

        List<Integer> childQids = jdbcChildSurveyTemplate.queryForList(childSql, Integer.class);

        // Step 2: Get only parent questions
        String questionSql = "SELECT * FROM child_survey_questions_master " +
                "WHERE isactive=1 AND isdelete=0 ORDER BY orderby";

        List<Map<String, Object>> questions = jdbcChildSurveyTemplate.queryForList(questionSql);

        List<Map<String, Object>> finalList = new ArrayList<>();

        for (Map<String, Object> q : questions) {

            Integer qid = (Integer) q.get("qid");

            // Skip if this is a child question
            if (childQids.contains(qid)) {
                continue;
            }

            Map<String, Object> fullQuestion = buildQuestionWithOptions(q);
            finalList.add(fullQuestion);
        }

        return finalList;
    }

    private Object safe(Object value) {
        return value == null ? "" : value;
    }

    private Map<String, Object> buildQuestionWithOptions(Map<String, Object> q) {

        Integer qid = (Integer) q.get("qid");
        String masterTable = (String) safe(q.get("master_table_name"));

        // Maintain order
        Map<String, Object> questionMap = new LinkedHashMap<>();

        questionMap.put("qid", q.get("qid"));
        questionMap.put("q_english", q.get("q_english"));
        questionMap.put("q_tamil", safe(q.get("q_tamil")));
        questionMap.put("field_name", q.get("field_name"));
        questionMap.put("cdate", q.get("cdate"));
        questionMap.put("question_type", q.get("question_type"));
        questionMap.put("isactive", q.get("isactive"));
        questionMap.put("isdelete", q.get("isdelete"));
        questionMap.put("orderby", q.get("orderby"));
        questionMap.put("is_mandatory", q.get("is_mandatory"));
        questionMap.put("is_dropdown", q.get("is_dropdown"));
        questionMap.put("master_table_name", masterTable);
        // String fieldName = (String) q.get("field_name");

        // // default
        // boolean isDropdown = false;

        // // only for these 2 fields
        // if ("duration".equalsIgnoreCase(fieldName) ||
        // "dropout_year".equalsIgnoreCase(fieldName)) {

        // isDropdown = true;
        // }

        // questionMap.put("is_dropdown", isDropdown);

        List<Map<String, Object>> options = new ArrayList<>();

        if (masterTable != null && !masterTable.isEmpty()) {

            options = getOptionsFromMaster(masterTable);

        } else {

            String optSql = "SELECT aid AS option_id, english_name, tamil_name, " +
                    "orderby, is_mandatory " +
                    "FROM child_survey_answer_master " +
                    "WHERE qid=? AND isactive=1 AND isdelete=0 ORDER BY orderby";

            List<Map<String, Object>> dbOptions = jdbcChildSurveyTemplate.queryForList(optSql, qid);

            for (Map<String, Object> opt : dbOptions) {

                Map<String, Object> optionMap = new LinkedHashMap<>();

                optionMap.put("option_id", opt.get("option_id"));
                optionMap.put("english_name", opt.get("english_name"));
                optionMap.put("tamil_name", safe(opt.get("tamil_name")));
                optionMap.put("orderby", opt.get("orderby"));
                // optionMap.put("imgfield", opt.get("img_required"));
                // optionMap.put("textfield", opt.get("text_required"));
                // optionMap.put("textname", opt.get("text_name"));
                // optionMap.put("remarksfield", opt.get("remarks_required"));
                optionMap.put("opt_mandatory", opt.get("is_mandatory"));

                Integer aid = (Integer) opt.get("option_id");

                // child questions
                List<Map<String, Object>> childQuestions = getChildQuestionsRecursive(aid);

                optionMap.put("child_questions", childQuestions);

                options.add(optionMap);
            }
        }

        questionMap.put("options", options);

        return questionMap;
    }

    private List<Map<String, Object>> getChildQuestionsRecursive(Integer parentAid) {

        String mappingSql = "SELECT child_qid FROM child_question_mapping " +
                "WHERE parent_aid=? AND isactive=1 AND isdelete=0";

        List<Integer> childQids = jdbcChildSurveyTemplate.queryForList(mappingSql, Integer.class, parentAid);

        List<Map<String, Object>> childQuestions = new ArrayList<>();

        for (Integer cqid : childQids) {

            String qSql = "SELECT * FROM child_survey_questions_master " +
                    "WHERE qid=? AND isactive=1 AND isdelete=0";

            Map<String, Object> childQ = jdbcChildSurveyTemplate.queryForMap(qSql, cqid);

            // IMPORTANT: reuse same method (recursive)
            Map<String, Object> fullChild = buildQuestionWithOptions(childQ);

            childQuestions.add(fullChild);
        }

        return childQuestions;
    }

    private List<Map<String, Object>> getOptionsFromMaster(String tableName) {

        String sql = "SELECT id AS option_id, " +
                "english_name, " +
                "IFNULL(tamil_name, '') AS tamil_name, " +
                "orderby, " +
                "is_mandatory " +
                "FROM " + tableName + " " +
                "WHERE isactive=1 AND isdelete=0 " +
                "ORDER BY orderby";

        return jdbcChildSurveyTemplate.queryForList(sql);
    }

    private String calculateAge(String dobStr) {
        try {
            LocalDate dob = LocalDate.parse(dobStr); // format: yyyy-MM-dd
            LocalDate today = LocalDate.now();
            Period age = Period.between(dob, today);
            return String.valueOf(age.getYears());
        } catch (Exception e) {
            return "";
        }
    }

    public String saveSurveyFromParams(Map<String, String> params) {

        String insertSql = "INSERT INTO child_survey_response " +
                "(qid, answer, cby, parent_answer_id, cdate, isactive, isdelete) " +
                "VALUES (?, ?, ?, ?, NOW(), 1, 0)";

        String cby = params.getOrDefault("cby", "1");

        for (Map.Entry<String, String> entry : params.entrySet()) {

            String fieldName = entry.getKey().trim(); // q1, q2...
            String answer = entry.getValue() == null ? "" : entry.getValue().trim();

            System.out.println("👉 Processing: " + fieldName);

            // =====================================================
            // 🔥 HANDLE DURATION (q3 assumed)
            // =====================================================
            if ("duration_value".equalsIgnoreCase(fieldName)) {

                String value = answer;
                String typeId = params.get("duration_type");

                String typeName = "";

                if (typeId != null) {
                    List<String> list = jdbcChildSurveyTemplate.queryForList(
                            "SELECT english_name FROM duration_master WHERE id=?",
                            String.class, typeId);

                    if (!list.isEmpty()) {
                        typeName = list.get(0);
                    }
                }

                String finalAnswer = value + " " + typeName;

                // 👉 q3 = duration
                jdbcChildSurveyTemplate.update(insertSql, 3, finalAnswer, cby, null);

                continue;
            }

            if ("duration_type".equalsIgnoreCase(fieldName)) {
                continue;
            }

            // =====================================================
            // 🔥 HANDLE DROPOUT YEAR (q30 assumed)
            // =====================================================
            if ("dropout_year_value".equalsIgnoreCase(fieldName)) {

                String value = answer;
                String typeId = params.get("dropout_year_type");

                String typeName = "";

                if (typeId != null) {
                    List<String> list = jdbcChildSurveyTemplate.queryForList(
                            "SELECT english_name FROM duration_master WHERE id=?",
                            String.class, typeId);

                    if (!list.isEmpty()) {
                        typeName = list.get(0);
                    }
                }

                String finalAnswer = value + " " + typeName;

                // 👉 q30 = dropout_year
                jdbcChildSurveyTemplate.update(insertSql, 30, finalAnswer, cby, null);

                continue;
            }

            if ("dropout_year_type".equalsIgnoreCase(fieldName)) {
                continue;
            }

            // =====================================================
            // 🔥 NORMAL Q1, Q2, Q3...
            // =====================================================
            if (fieldName.startsWith("q")) {

                try {
                    Integer qid = Integer.parseInt(fieldName.substring(1));

                    System.out.println("✔ QID: " + qid);

                    // DOB → AGE
                    if (qid == 9) { // q9 = dob

                        jdbcChildSurveyTemplate.update(insertSql, qid, answer, cby, null);

                        Integer parentId = jdbcChildSurveyTemplate.queryForObject(
                                "SELECT LAST_INSERT_ID()", Integer.class);

                        String age = calculateAge(answer);

                        // 👉 q10 = age
                        jdbcChildSurveyTemplate.update(insertSql, 10, age, cby, parentId);

                    } else {
                        jdbcChildSurveyTemplate.update(insertSql, qid, answer, cby, null);
                    }

                } catch (Exception e) {
                    System.out.println("❌ Invalid field: " + fieldName);
                }
            }
        }

        // Save surveyor location details single time after loop
        String zone = params.get("zone");
        String ward = params.get("ward");
        String lat = params.get("latitude");
        if (lat == null)
            lat = params.get("lat");
        String lng = params.get("longitude");
        if (lng == null)
            lng = params.get("long");
        String address = params.get("address");

        if (zone != null || lat != null || lng != null) {
            try {
                String logindetailsSql = "INSERT INTO surveyor_location_details " +
                        "(loginId, zone, ward, latitude, longitude, address, is_active, is_delete, cdate) " +
                        "VALUES (?, ?, ?, ?, ?, ?, 1, 0, NOW())";
                jdbcChildSurveyTemplate.update(logindetailsSql, cby, zone, ward, lat, lng, address);
            } catch (Exception e) {
                System.out.println("❌ Error saving surveyor location details: " + e.getMessage());
            }
        }

        return "Saved Successfully";
    }

}
