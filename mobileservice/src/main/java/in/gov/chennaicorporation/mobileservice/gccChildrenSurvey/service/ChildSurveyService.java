package in.gov.chennaicorporation.mobileservice.gccChildrenSurvey.service;

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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

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

    public List<Map<String, Object>> getParticipateSurveyQuestions() {

        String questionSql = "SELECT * FROM child_survey_participate_master " +
                "WHERE isactive=1 AND isdelete=0 ORDER BY orderby";

        List<Map<String, Object>> questions = jdbcChildSurveyTemplate.queryForList(questionSql);

        List<Map<String, Object>> finalList = new ArrayList<>();

        for (Map<String, Object> q : questions) {
            Map<String, Object> fullQuestion = buildParticipateQuestionWithOptions(q);
            finalList.add(fullQuestion);
        }

        return finalList;
    }

    private Map<String, Object> buildParticipateQuestionWithOptions(Map<String, Object> q) {

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
        questionMap.put("flag", q.get("flag"));
        questionMap.put("master_table_name", masterTable);

        List<Map<String, Object>> options = new ArrayList<>();

        if (masterTable != null && !masterTable.isEmpty()) {
            options = getOptionsFromMaster(masterTable, qid);
        }

        questionMap.put("options", options);

        return questionMap;
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

            boolean isOthers = englishName != null && englishName.equalsIgnoreCase("Others");
            optionMap.put("is_others", isOthers);
            optionMap.put("text", isOthers);

            // 🔥 HANDLE "OTHERS"
            if (isOthers) {

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

        // Detect and save family members details if passed as JSON string
        String familyMembersJson = params.get("family_members");
        if (familyMembersJson == null) {
            familyMembersJson = params.get("familyMembers");
        }
        if (familyMembersJson == null) {
            familyMembersJson = params.get("treeData");
        }

        // We will keep a list of keys that were parsed as family members JSON to skip
        // them in the main q-param loop
        List<String> parsedJsonKeys = new ArrayList<>();

        // Group and parse family members sent as structured parameters like
        // q58[0][name]
        Map<String, Map<Integer, Map<String, Object>>> structuredFamilyMap = new HashMap<>();
        java.util.regex.Pattern structPattern = java.util.regex.Pattern
                .compile("^q(\\d+)\\[(\\d+)\\]\\[([a-zA-Z0-9_]+)\\]$");
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String key = entry.getKey().trim();
            String val = entry.getValue() == null ? "" : entry.getValue().trim();
            java.util.regex.Matcher m = structPattern.matcher(key);
            if (m.matches()) {
                String qidStr = m.group(1);
                String qPrefix = "q" + qidStr;
                int index = Integer.parseInt(m.group(2));
                String prop = m.group(3);

                structuredFamilyMap.computeIfAbsent(qPrefix, k -> new java.util.TreeMap<>())
                        .computeIfAbsent(index, k -> new LinkedHashMap<>())
                        .put(prop, val);

                // Track this key to skip in the normal parameter loop
                parsedJsonKeys.add(key);
            }
        }
        // Save the grouped structured family members
        for (Map.Entry<String, Map<Integer, Map<String, Object>>> entry : structuredFamilyMap.entrySet()) {
            List<Map<String, Object>> familyList = new ArrayList<>(entry.getValue().values());
            if (!familyList.isEmpty()) {
                saveFamilyMembers(familyList, surveyId, cby);
            }
        }
        if (familyMembersJson != null && !familyMembersJson.trim().isEmpty()
                && familyMembersJson.trim().startsWith("[")) {
            try {
                List<Map<String, Object>> familyList = parseFamilyMembersJson(familyMembersJson);
                if (familyList != null && !familyList.isEmpty()) {
                    saveFamilyMembers(familyList, surveyId, cby);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Also check if any qX parameter contains the JSON list of family members
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String fieldName = entry.getKey().trim();
            String answer = entry.getValue() == null ? "" : entry.getValue().trim();
            if (fieldName.startsWith("q") && answer.startsWith("[") && answer.endsWith("]")) {
                try {
                    List<Map<String, Object>> familyList = parseFamilyMembersJson(answer);
                    if (familyList != null && !familyList.isEmpty()) {
                        saveFamilyMembers(familyList, surveyId, cby);
                        parsedJsonKeys.add(fieldName);
                    }
                } catch (Exception e) {
                    // Ignore, not a valid JSON list, process as normal q parameter
                }
            }
        }

        String insertSql = "INSERT INTO child_survey_response " +
                "(survey_id, qid, answer, others_answer, cby, parent_answer_id, cdate, isactive, isdelete) " +
                "VALUES (?, ?, ?, ?, ?, ?, NOW(), 1, 0)";

        for (Map.Entry<String, String> entry : params.entrySet()) {

            String fieldName = entry.getKey().trim(); // q1, q2...
            if (parsedJsonKeys.contains(fieldName)) {
                continue;
            }
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

                    jdbcChildSurveyTemplate.update(insertSql, surveyId, qid, answer, othersValue, cby, null);

                } catch (Exception e) {
                    System.out.println("❌ Invalid field: " + fieldName);
                }
            }
        }

        return "Saved Successfully";
    }

    public String saveParticipateSurveyFromParams(Map<String, String> params) {

        String cby = params.getOrDefault("cby", "1");
        String timeStr = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("ddMMyyHHmmss"));
        String surveyId = "GCC_CSP_" + timeStr;

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
            }
        }

        String insertSql = "INSERT INTO child_survey_participate_response " +
                "(survey_id, qid, answer, others_answer, cby, parent_answer_id, cdate, isactive, isdelete) " +
                "VALUES (?, ?, ?, ?, ?, ?, NOW(), 1, 0)";

        for (Map.Entry<String, String> entry : params.entrySet()) {

            String fieldName = entry.getKey().trim(); // q1, q2...
            String answer = entry.getValue() == null ? "" : entry.getValue().trim();

            if (fieldName.startsWith("q")) {

                try {
                    Integer qid = Integer.parseInt(fieldName.substring(1));

                    String othersKey = fieldName + "_other";
                    String othersValue = params.getOrDefault(othersKey, "");

                    jdbcChildSurveyTemplate.update(insertSql, surveyId, qid, answer, othersValue, cby, null);

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

    private void saveFamilyMembers(List<Map<String, Object>> familyList, String surveyId, String cby) {
        if (familyList == null || familyList.isEmpty()) {
            return;
        }

        String insertFamilySql = "INSERT INTO child_survey_response_family_members " +
                "(qid, answer, others_answer, cby, survey_id, parent_answer_id, cdate, isactive, isdelete) " +
                "VALUES (?, ?, ?, ?, ?, ?, NOW(), 1, 0)";

        for (Map<String, Object> member : familyList) {
            for (Map.Entry<String, Object> field : member.entrySet()) {
                String key = field.getKey().trim();
                Object valObj = field.getValue();
                String answer = valObj == null ? "" : valObj.toString().trim();

                // Skip utility/metadata keys if any
                if ("parent_answer_id".equalsIgnoreCase(key) || "parent_aid".equalsIgnoreCase(key)) {
                    continue;
                }

                // Resolve qid
                Integer qid = null;
                if (key.startsWith("q")) {
                    try {
                        qid = Integer.parseInt(key.substring(1));
                    } catch (Exception e) {
                        // ignore
                    }
                } else {
                    try {
                        qid = Integer.parseInt(key);
                    } catch (Exception e) {
                        // ignore
                    }
                }

                // If not numeric/q-prefixed, resolve statically first
                if (qid == null) {
                    String cleanKey = key.toLowerCase().trim();
                    if ("name".equals(cleanKey)) {
                        qid = 6;
                    } else if ("gender".equals(cleanKey)) {
                        qid = 7;
                    } else if ("age".equals(cleanKey)) {
                        qid = 10;
                    } else if ("relationship".equals(cleanKey)) {
                        qid = 21;
                    } else if ("education".equals(cleanKey)) {
                        qid = 26;
                    } else if ("occupation".equals(cleanKey)) {
                        qid = 22;
                    }
                }

                // Fallback: If still null, try resolving by field_name or matching q_english
                // (case-insensitive) from DB without active/delete constraint
                if (qid == null) {
                    try {
                        String cleanKey = key.toLowerCase().trim();
                        String getQidSql = "SELECT qid FROM child_survey_questions_master " +
                                "WHERE (LOWER(field_name) = ? OR LOWER(q_english) = ? OR LOWER(q_english) LIKE ?) LIMIT 1";
                        List<Integer> qidList = jdbcChildSurveyTemplate.queryForList(getQidSql, Integer.class,
                                cleanKey, cleanKey, cleanKey + "%");
                        if (!qidList.isEmpty()) {
                            qid = qidList.get(0);
                        }
                    } catch (Exception e) {
                        // ignore
                    }
                }

                if (qid != null) {
                    // Extract parent_answer_id (if any is provided in the member object or resolved
                    // from mapping)
                    Integer parentAnswerId = null;
                    Object parentAidVal = member.get("parent_answer_id");
                    if (parentAidVal == null) {
                        parentAidVal = member.get("parent_aid");
                    }
                    if (parentAidVal != null) {
                        try {
                            parentAnswerId = Integer.parseInt(parentAidVal.toString());
                        } catch (Exception e) {
                            // ignore
                        }
                    }

                    // If still null, try resolving statically
                    if (parentAnswerId == null) {
                        if (qid == 10) {
                            parentAnswerId = 1;
                        } else if (qid == 21) {
                            parentAnswerId = 8;
                        } else if (qid == 26) {
                            parentAnswerId = 9;
                        } else if (qid == 22) {
                            parentAnswerId = 5;
                        }
                    }

                    // Fallback: If still null, try resolving from mapping table (without
                    // active/delete constraint)
                    if (parentAnswerId == null) {
                        try {
                            String parentAidSql = "SELECT parent_aid FROM child_question_mapping WHERE child_qid = ? LIMIT 1";
                            List<Integer> list = jdbcChildSurveyTemplate.queryForList(parentAidSql, Integer.class, qid);
                            if (!list.isEmpty()) {
                                parentAnswerId = list.get(0);
                            }
                        } catch (Exception e) {
                            // ignore
                        }
                    }

                    // Extract others_answer if any
                    String othersKey = key + "_other";
                    Object othersValObj = member.get(othersKey);
                    String othersValue = othersValObj == null ? "" : othersValObj.toString().trim();

                    // Insert to child_survey_response_family_members
                    try {
                        jdbcChildSurveyTemplate.update(insertFamilySql, qid, answer, othersValue, cby, surveyId,
                                parentAnswerId);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private List<Map<String, Object>> parseFamilyMembersJson(String jsonStr) {
        if (jsonStr == null || jsonStr.trim().isEmpty()) {
            return new ArrayList<>();
        }
        String str = jsonStr.trim();
        // First try parsing using standard ObjectMapper
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(str, new TypeReference<List<Map<String, Object>>>() {
            });
        } catch (Exception e) {
            // Ignore standard JSON parsing exception, try lenient fallback
        }

        // Lenient parsing fallback
        List<Map<String, Object>> list = new ArrayList<>();
        try {
            if (str.startsWith("[")) {
                str = str.substring(1);
            }
            if (str.endsWith("]")) {
                str = str.substring(0, str.length() - 1);
            }
            str = str.trim();
            if (str.isEmpty()) {
                return list;
            }

            // Match all occurrences of { ... }
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\{([^}]+)\\}");
            java.util.regex.Matcher matcher = pattern.matcher(str);
            while (matcher.find()) {
                String objContent = matcher.group(1);
                Map<String, Object> map = new LinkedHashMap<>();
                String[] pairs = objContent.split(",");
                for (String pair : pairs) {
                    int colonIdx = pair.indexOf(':');
                    if (colonIdx != -1) {
                        String key = pair.substring(0, colonIdx).trim();
                        String value = pair.substring(colonIdx + 1).trim();
                        if ((value.startsWith("\"") && value.endsWith("\""))
                                || (value.startsWith("'") && value.endsWith("'"))) {
                            value = value.substring(1, value.length() - 1);
                        }
                        map.put(key, value);
                    }
                }
                if (!map.isEmpty()) {
                    list.add(map);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

}
