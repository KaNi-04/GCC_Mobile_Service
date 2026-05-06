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

            options = getOptionsFromMaster(masterTable, qid);

        } else {

            String optSql = "SELECT aid AS option_id, english_name, tamil_name, " +
                    "orderby, is_mandatory " +
                    "FROM child_survey_answer_master " +
                    "WHERE qid=? AND isactive=1 AND isdelete=0 ORDER BY orderby";

            List<Map<String, Object>> dbOptions = jdbcChildSurveyTemplate.queryForList(optSql, qid);

            for (Map<String, Object> opt : dbOptions) {

                Map<String, Object> optionMap = new LinkedHashMap<>();

                String englishName = (String) opt.get("english_name");
                optionMap.put("option_id", opt.get("option_id"));
                optionMap.put("english_name", opt.get("english_name"));
                optionMap.put("tamil_name", safe(opt.get("tamil_name")));
                optionMap.put("orderby", opt.get("orderby"));
                // optionMap.put("imgfield", opt.get("img_required"));
                // optionMap.put("textfield", opt.get("text_required"));
                // optionMap.put("textname", opt.get("text_name"));
                // optionMap.put("remarksfield", opt.get("remarks_required"));
                optionMap.put("opt_mandatory", opt.get("is_mandatory"));

                // NEW FIELD
                boolean isOthers = englishName != null && englishName.equalsIgnoreCase("Others");

                optionMap.put("is_others", isOthers);

                // tell frontend to show textbox
                optionMap.put("text", isOthers);

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

    // private List<Map<String, Object>> getOptionsFromMaster(String tableName) {

    // String sql = "SELECT id AS option_id, " +
    // "english_name, " +
    // "IFNULL(tamil_name, '') AS tamil_name, " +
    // "orderby, " +
    // "is_mandatory " +
    // "FROM " + tableName + " " +
    // "WHERE isactive=1 AND isdelete=0 " +
    // "ORDER BY orderby";

    // return jdbcChildSurveyTemplate.queryForList(sql);
    // }

    private List<Map<String, Object>> getOptionsFromMaster(String tableName, Integer parentQid) {

        String sql = "SELECT id AS option_id, " +
                "english_name, " +
                "IFNULL(tamil_name, '') AS tamil_name, " +
                "orderby, " +
                "is_mandatory " +
                "FROM " + tableName + " " +
                "WHERE isactive=1 AND isdelete=0 " +
                "ORDER BY orderby";

        List<Map<String, Object>> dbOptions = jdbcChildSurveyTemplate.queryForList(sql);

        List<Map<String, Object>> options = new ArrayList<>();

        for (Map<String, Object> opt : dbOptions) {

            Map<String, Object> optionMap = new LinkedHashMap<>();

            String englishName = (String) opt.get("english_name");

            optionMap.put("option_id", opt.get("option_id"));
            optionMap.put("english_name", englishName);
            optionMap.put("tamil_name", opt.get("tamil_name"));
            optionMap.put("orderby", opt.get("orderby"));
            optionMap.put("is_mandatory", opt.get("is_mandatory"));

            List<Map<String, Object>> childQuestions = new ArrayList<>();

            // 🔥 HANDLE "OTHERS"
            if (englishName != null && englishName.equalsIgnoreCase("Others")) {

                Map<String, Object> child = new LinkedHashMap<>();

                child.put("qid", parentQid); // same parent qid
                child.put("q_english", "Please specify");
                child.put("q_tamil", "");
                child.put("question_type", "text");
                child.put("field_name", "q" + parentQid + "_other");
                child.put("is_mandatory", false);
                child.put("is_dropdown", false);

                childQuestions.add(child);
            }

            optionMap.put("child_questions", childQuestions);

            options.add(optionMap);
        }

        return options;
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

        String cby = params.getOrDefault("cby", "1");
        String timeStr = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("ddMMyyHHmmss"));
        String surveyId = "GCC_CS_" + timeStr;

        // Save surveyor location details first
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
                // 1. Insert without survey_id using KeyHolder
                String logindetailsSql = "INSERT INTO surveyor_location_details " +
                        "(loginId, zone, ward, latitude, longitude, address, is_active, is_delete, cdate) " +
                        "VALUES (?, ?, ?, ?, ?, ?, 1, 0, NOW())";

                org.springframework.jdbc.support.KeyHolder keyHolder = new org.springframework.jdbc.support.GeneratedKeyHolder();

                final String finalLat = lat;
                final String finalLng = lng;

                jdbcChildSurveyTemplate.update(
                        connection -> {
                            java.sql.PreparedStatement ps = connection.prepareStatement(logindetailsSql,
                                    java.sql.Statement.RETURN_GENERATED_KEYS);
                            ps.setString(1, cby);
                            ps.setString(2, zone);
                            ps.setString(3, ward);
                            ps.setString(4, finalLat);
                            ps.setString(5, finalLng);
                            ps.setString(6, address);
                            return ps;
                        },
                        keyHolder);

                // 2. Fetch the generated location id
                Number key = keyHolder.getKey();
                if (key != null) {
                    Integer locationId = key.intValue();

                    // 3. Append to surveyId
                    surveyId = surveyId + locationId;

                    // 4. Update the location record with the final surveyId
                    String updateSql = "UPDATE surveyor_location_details SET survey_id = ? WHERE id = ?";
                    jdbcChildSurveyTemplate.update(updateSql, surveyId, locationId);
                }

            } catch (Exception e) {
                // System.out.println("❌ Error saving surveyor location details: " +
                // e.getMessage());
            }
        }

        String insertSql = "INSERT INTO child_survey_response " +
                "(survey_id, qid, answer, others_answer, cby, parent_answer_id, cdate, isactive, isdelete) " +
                "VALUES (?, ?, ?, ?, ?, ?, NOW(), 1, 0)";

        for (Map.Entry<String, String> entry : params.entrySet()) {

            String fieldName = entry.getKey().trim(); // q1, q2...
            String answer = entry.getValue() == null ? "" : entry.getValue().trim();

            // System.out.println("👉 Processing: " + fieldName);

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
                jdbcChildSurveyTemplate.update(insertSql, surveyId, 3, finalAnswer, "", cby, null);

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
                jdbcChildSurveyTemplate.update(insertSql, surveyId, 30, finalAnswer, "", cby, null);

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

                    String othersKey = fieldName + "_other";
                    String othersValue = params.getOrDefault(othersKey, "");

                    // System.out.println("✔ QID: " + qid);

                    // DOB → AGE
                    if (qid == 9) { // q9 = dob

                        jdbcChildSurveyTemplate.update(insertSql, surveyId, qid, answer, "", cby, null);

                        Integer parentId = jdbcChildSurveyTemplate.queryForObject(
                                "SELECT LAST_INSERT_ID()", Integer.class);

                        String age = calculateAge(answer);

                        // 👉 q10 = age
                        jdbcChildSurveyTemplate.update(insertSql, surveyId, 10, age, "", cby, parentId);

                    } else {
                        jdbcChildSurveyTemplate.update(insertSql, surveyId, qid, answer, othersValue, cby, null);
                    }

                } catch (Exception e) {
                    System.out.println("❌ Invalid field: " + fieldName);
                }
            }
        }

        return "Saved Successfully";
    }

    public Map<String, Object> getLoginDetails(String mobileNo, String password) {

        try {

            String sql = "SELECT uid as loginId,user_name, password, mobile_no, is_active, is_delete " +
                    "FROM login_details " +
                    "WHERE mobile_no = ? AND password = ? AND is_active = 1 AND is_delete = 0 Limit 1";

            List<Map<String, Object>> data = jdbcChildSurveyTemplate.queryForList(sql, mobileNo, password);

            Map<String, Object> response = new HashMap<>();

            if (!data.isEmpty()) {
                // Login success
                response.put("is_login", true);
                response.put("message", "Login Success");
                response.put("data", data);
            } else {
                // Login failed
                response.put("is_login", false);
                response.put("message", "Invalid mobile number or password");
            }

            return response;

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error fetching batch details: " + e.getMessage());
        }
    }

}
