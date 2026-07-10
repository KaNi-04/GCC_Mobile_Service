package in.gov.chennaicorporation.mobileservice.gccRagPickerSurvey.Service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import in.gov.chennaicorporation.mobileservice.gccactivity.service.DateTimeUtil;

@Service
public class RagPickerSurveyService {

    @Autowired
    JdbcTemplate jdbcRagPickerSurveyTemplate;

    @Autowired
    private Environment environment;

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int STRING_LENGTH = 15;
    private static final Random RANDOM = new SecureRandom();

    public static String generateRandomString() {
        StringBuilder result = new StringBuilder(STRING_LENGTH);
        for (int i = 0; i < STRING_LENGTH; i++) {
            result.append(CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length())));
        }
        return result.toString();
    }

    @Autowired
    public void setDataSource(@Qualifier("mysqlRagPickerSurveyDataSource") DataSource RagPickerSurveyDataSource) {
        this.jdbcRagPickerSurveyTemplate = new JdbcTemplate(RagPickerSurveyDataSource);
    }

    public List<Map<String, Object>> getSurveyQuestions(Integer cid) {

        String childSql = "SELECT DISTINCT child_qid FROM rag_picker_question_mapping " +
                "WHERE isactive=1 AND isdelete=0";

        List<Integer> childQids = jdbcRagPickerSurveyTemplate.queryForList(childSql, Integer.class);

        String questionSql = "SELECT * FROM rag_picker_survey_questions_master " +
                "WHERE cid=? AND isactive=1 AND isdelete=0 ORDER BY orderby";

        List<Map<String, Object>> questions = jdbcRagPickerSurveyTemplate.queryForList(questionSql, cid);

        List<Map<String, Object>> finalList = new ArrayList<>();

        for (Map<String, Object> q : questions) {
            Integer qid = (Integer) q.get("qid");

            if (childQids.contains(qid)) {
                continue;
            }

            Map<String, Object> fullQuestion = buildQuestionWithOptions(q);
            finalList.add(fullQuestion);
        }

        return finalList;
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
        questionMap.put("flag", q.get("flag"));
        questionMap.put("master_table_name", masterTable);

        List<Map<String, Object>> options = new ArrayList<>();

        if (masterTable != null && !masterTable.isEmpty()) {
            if (masterTable.equalsIgnoreCase("state_master") || masterTable.equalsIgnoreCase("district_master")) {
                options = new ArrayList<>();
            } else {
                options = getOptionsFromMaster(masterTable, qid);
            }
        } else {
            String optSql = "SELECT id AS option_id, answer AS english_name, " +
                    "orderby, is_mandatory " +
                    "FROM rag_picker_survey_answer_master " +
                    "WHERE qid=? AND isactive=1 AND isdelete=0 ORDER BY orderby";

            List<Map<String, Object>> dbOptions = jdbcRagPickerSurveyTemplate.queryForList(optSql, qid);

            for (Map<String, Object> opt : dbOptions) {

                Map<String, Object> optionMap = new LinkedHashMap<>();

                String englishName = (String) opt.get("english_name");
                optionMap.put("option_id", opt.get("option_id"));
                optionMap.put("english_name", englishName);
                optionMap.put("tamil_name", "");
                optionMap.put("orderby", opt.get("orderby"));
                optionMap.put("is_mandatory", opt.get("is_mandatory"));

                boolean isOthers = englishName != null && englishName.equalsIgnoreCase("Others");
                optionMap.put("is_others", isOthers);
                optionMap.put("text", isOthers);

                Integer aid = (Integer) opt.get("option_id");

                List<Map<String, Object>> childQuestions = getChildQuestionsRecursive(aid);

                optionMap.put("child_questions", childQuestions);

                options.add(optionMap);
            }
        }

        questionMap.put("options", options);

        return questionMap;
    }

    private List<Map<String, Object>> getChildQuestionsRecursive(Integer parentAid) {

        String mappingSql = "SELECT child_qid FROM rag_picker_question_mapping " +
                "WHERE parent_aid=? AND isactive=1 AND isdelete=0";

        List<Integer> childQids = jdbcRagPickerSurveyTemplate.queryForList(mappingSql, Integer.class, parentAid);

        List<Map<String, Object>> childQuestions = new ArrayList<>();

        for (Integer cqid : childQids) {

            String qSql = "SELECT * FROM rag_picker_survey_questions_master " +
                    "WHERE qid=? AND isactive=1 AND isdelete=0";

            List<Map<String, Object>> childQList = jdbcRagPickerSurveyTemplate.queryForList(qSql, cqid);

            if (!childQList.isEmpty()) {
                Map<String, Object> childQ = childQList.get(0);
                Map<String, Object> fullChild = buildQuestionWithOptions(childQ);
                childQuestions.add(fullChild);
            }
        }

        return childQuestions;
    }

    private List<Map<String, Object>> getOptionsFromMaster(String tableName, Integer parentQid) {

        String sql = "SELECT id AS option_id, " +
                "english_name, " +
                "IFNULL(tamil_name, '') AS tamil_name, " +
                "orderby, " +
                "is_mandatory " +
                "FROM " + tableName + " " +
                "WHERE isactive=1 AND isdelete=0 " +
                "ORDER BY orderby";

        List<Map<String, Object>> dbOptions = jdbcRagPickerSurveyTemplate.queryForList(sql);

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

    public List<Map<String, Object>> getStates() {
        String sql = "SELECT id AS option_id, english_name, IFNULL(tamil_name, '') AS tamil_name, orderby " +
                "FROM state_master " +
                "WHERE isactive=1 AND isdelete=0 " +
                "ORDER BY orderby";
        return jdbcRagPickerSurveyTemplate.queryForList(sql);
    }

    public List<Map<String, Object>> getQuestionsCategory() {
        String sql = "SELECT * FROM question_category_master where isactive=1 and isdelete=0 order by orderby";
        return jdbcRagPickerSurveyTemplate.queryForList(sql);

    }

    public List<Map<String, Object>> getDistricts(String sid) {
        if (sid != null && !sid.trim().isEmpty()) {
            String sql = "SELECT id AS option_id, english_name, IFNULL(tamil_name, '') AS tamil_name, sid, orderby " +
                    "FROM district_master " +
                    "WHERE sid=? AND isactive=1 AND isdelete=0 " +
                    "ORDER BY orderby";
            return jdbcRagPickerSurveyTemplate.queryForList(sql, sid);
        } else {
            String sql = "SELECT id AS option_id, english_name, IFNULL(tamil_name, '') AS tamil_name, sid, orderby " +
                    "FROM district_master " +
                    "WHERE isactive=1 AND isdelete=0 " +
                    "ORDER BY orderby";
            return jdbcRagPickerSurveyTemplate.queryForList(sql);
        }
    }

    private Object safe(Object value) {
        return value == null ? "" : value;
    }

    public Map<String, Object> getLoginDetails(String mobileNo, String password) {

        try {

            String sql = "SELECT uid as loginId,user_name, password, mobile_no, is_active, is_delete " +
                    "FROM login_details " +
                    "WHERE mobile_no = ? AND password = ? AND is_active = 1 AND is_delete = 0 Limit 1";

            List<Map<String, Object>> data = jdbcRagPickerSurveyTemplate.queryForList(sql, mobileNo, password);

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
            throw new RuntimeException("Error fetching login details: " + e.getMessage());
        }
    }

    @Transactional
    public Map<String, Object> saveSurveyResponses(Map<String, String> params,
            MultipartHttpServletRequest fileRequest) {
        Map<String, Object> result = new HashMap<>();
        try {
            // URL-decode all parameter values to handle spaces, etc. (e.g. Adi%20Dravidar)
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (entry.getValue() != null) {
                    String val = entry.getValue();
                    try {
                        if (val.contains("%")) {
                            entry.setValue(java.net.URLDecoder.decode(val, "UTF-8"));
                        }
                    } catch (Exception ex) {
                        // ignore
                    }
                }
            }

            String surveyId = params.get("survey_id");
            String cid = params.get("cid");
            String cby = params.get("cby");
            String zone = params.get("zone");
            String ward = params.get("ward");
            String latitude = params.get("latitude");
            String longitude = params.get("longitude");
            String address = params.get("address");

            // System.out.println(" surveyId: " + surveyId);
            // System.out.println(" cid: " + cid);
            // System.out.println(" cby: " + cby);
            // System.out.println(" zone: " + zone);
            // System.out.println(" ward: " + ward);
            // System.out.println(" latitude: " + latitude);
            // System.out.println(" longitude: " + longitude);
            // System.out.println(" address: " + address);

            // If it is NOT the Profile category, verify that the profile is already created
            if (cid == null || !cid.equals("1")) {
                if (surveyId == null || surveyId.trim().isEmpty() || surveyId.equalsIgnoreCase("null")) {
                    result.put("status", false);
                    result.put("message",
                            "Survey ID is required for this category. Please save the Profile category first.");
                    return result;
                }

                // Check if the Profile category responses or surveyor location record exists
                // for this surveyId
                String checkProfileSql = "SELECT COUNT(*) FROM rag_picker_survey_response WHERE survey_id = ? AND cid = '1' AND isactive = 1 AND isdelete = 0";
                Integer profileCount = jdbcRagPickerSurveyTemplate.queryForObject(checkProfileSql, Integer.class,
                        surveyId);

                String checkLocSql = "SELECT COUNT(*) FROM surveyor_location_details WHERE survey_id = ? AND is_active = 1 AND is_delete = 0";
                Integer locCount = jdbcRagPickerSurveyTemplate.queryForObject(checkLocSql, Integer.class, surveyId);

                if ((profileCount == null || profileCount == 0) && (locCount == null || locCount == 0)) {
                    result.put("status", false);
                    result.put("message",
                            "Profile not created for this survey ID. Please save the Profile category first.");
                    return result;
                }
            }

            // 1. Generate surveyId if not provided
            if (surveyId == null || surveyId.trim().isEmpty() || surveyId.equalsIgnoreCase("null")) {
                String timeStr = java.time.LocalDateTime.now()
                        .format(java.time.format.DateTimeFormatter.ofPattern("ddMMyyHHmmss"));
                surveyId = "GCC_RPS_" + timeStr;
            }

            String resolvedLat = latitude;
            if (resolvedLat == null || resolvedLat.trim().isEmpty() || resolvedLat.equalsIgnoreCase("null")) {
                resolvedLat = params.get("lat");
            }
            if (resolvedLat == null || resolvedLat.trim().isEmpty() || resolvedLat.equalsIgnoreCase("null")) {
                if (fileRequest != null) {
                    resolvedLat = fileRequest.getParameter("lat");
                }
            }
            String resolvedLng = longitude;
            if (resolvedLng == null || resolvedLng.trim().isEmpty() || resolvedLng.equalsIgnoreCase("null")) {
                resolvedLng = params.get("long");
            }
            if (resolvedLng == null || resolvedLng.trim().isEmpty() || resolvedLng.equalsIgnoreCase("null")) {
                if (fileRequest != null) {
                    resolvedLng = fileRequest.getParameter("long");
                }
            }

            // If Profile category (cid = 1) is created, save surveyor location details
            // first (like in child_survey)
            if (cid != null && cid.equals("1") && (zone != null || resolvedLat != null || resolvedLng != null)) {
                try {
                    String logindetailsSql = "INSERT INTO surveyor_location_details " +
                            "(loginId, zone, ward, latitude, longitude, address, is_active, is_delete, cdate) " +
                            "VALUES (?, ?, ?, ?, ?, ?, 1, 0, NOW())";

                    org.springframework.jdbc.support.KeyHolder keyHolder = new org.springframework.jdbc.support.GeneratedKeyHolder();

                    final String finalLat = resolvedLat;
                    final String finalLng = resolvedLng;
                    final String finalZone = zone;
                    final String finalWard = ward;
                    final String finalAddress = address;
                    final String finalCby = cby;

                    jdbcRagPickerSurveyTemplate.update(
                            connection -> {
                                java.sql.PreparedStatement ps = connection.prepareStatement(logindetailsSql,
                                        java.sql.Statement.RETURN_GENERATED_KEYS);
                                ps.setString(1, finalCby);
                                ps.setString(2, finalZone);
                                ps.setString(3, finalWard);
                                ps.setString(4, finalLat);
                                ps.setString(5, finalLng);
                                ps.setString(6, finalAddress);
                                return ps;
                            },
                            keyHolder);

                    Number key = keyHolder.getKey();
                    if (key != null) {
                        Integer locationId = key.intValue();

                        // Append the locationId to surveyId
                        surveyId = surveyId + locationId;

                        // Update the location record with the final surveyId
                        String updateSql = "UPDATE surveyor_location_details SET survey_id = ? WHERE id = ?";
                        jdbcRagPickerSurveyTemplate.update(updateSql, surveyId, locationId);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    System.out.println("Error saving surveyor location details: " + ex.getMessage());
                }
            }
            
            //to save family members details
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
            

            // Deactivate existing active responses for this surveyId and cid
            String deactivateSql = "UPDATE rag_picker_survey_response SET isactive = 0, isdelete = 1 WHERE survey_id = ? AND cid = ? AND isactive = 1";
            jdbcRagPickerSurveyTemplate.update(deactivateSql, surveyId, cid);

            String deactivateParticipateSql = "UPDATE rag_picker_survey_participate_response SET isactive = 0, isdelete = 1 WHERE survey_id = ? AND cid = ? AND isactive = 1";
            jdbcRagPickerSurveyTemplate.update(deactivateParticipateSql, surveyId, cid);

            String insertparticipateSql = "INSERT INTO rag_picker_survey_participate_response (qid, answer, others_answer, survey_id, cid, cby, parent_answer_id, cdate, isactive, isdelete) "
                    +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), 1, 0)";

            String insertSql = "INSERT INTO rag_picker_survey_response (qid, answer, others_answer, survey_id, cid, cby, parent_answer_id, cdate, isactive, isdelete) "
                    +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), 1, 0)";

            String qSql = "SELECT qid, question_type, field_name, q_english FROM rag_picker_survey_questions_master "
                    + "WHERE cid = ? AND (isactive = 1 AND isdelete = 0 OR (cid = '1' AND field_name IN ('q2', 'q78', 'q11', 'q8', 'q9', 'q10', 'q77')))";
            List<Map<String, Object>> questions = jdbcRagPickerSurveyTemplate.queryForList(qSql, cid);

            boolean hasNotWillingResponse = false;
            // First pass: check if qid = 1 is answered with "38" or "Not willing to respond
            // (mark them)"
            for (Map<String, Object> q : questions) {

                Integer qid = (Integer) q.get("qid");
                String fieldName = (String) q.get("field_name");
                if (parsedJsonKeys.contains(fieldName)) {
                    continue; // Already saved in family table
                }
                if (qid != null && qid == 1 && fieldName != null && params.containsKey(fieldName)) {
                    String answer = params.get(fieldName);
                    if (answer != null) {
                        String trimmed = answer.trim();
                        if ("38".equals(trimmed) || "Not willing to respond (mark them)".equalsIgnoreCase(trimmed)) {
                            hasNotWillingResponse = true;
                        }
                    }
                }
            }

            for (Map<String, Object> q : questions) {
                Integer qid = (Integer) q.get("qid");
                String fieldName = (String) q.get("field_name");
                String questionType = (String) q.get("question_type");
                String qEnglish = (String) q.get("q_english");
                
                if (parsedJsonKeys.contains(fieldName)) {
                    continue; // Already saved in family table
                }

                // System.out.println("qid: " + qid);
                // System.out.println("fieldName: " + fieldName);
                // System.out.println("questionType: " + questionType);
                // System.out.println("qEnglish: " + qEnglish);

                if (fieldName == null || fieldName.trim().isEmpty()) {
                    continue;
                }

                // Check if the param exists or has a file upload
                boolean hasParam = params.containsKey(fieldName);
                boolean hasFile = false;
                if (fileRequest != null) {

                    // System.out.println(
                    // "fieldName: " + fieldName + " - " +
                    // fileRequest.getFileMap().containsKey(fieldName));
                    // System.out.println("q_" + qid + " - " +
                    // fileRequest.getFileMap().containsKey("q_" + qid));

                    if (fileRequest.getFileMap().containsKey(fieldName) ||
                            fileRequest.getFileMap().containsKey("q_" + qid) ||
                            fileRequest.getFileMap().containsKey("file_" + qid) ||
                            fileRequest.getFileMap().containsKey("file" + qid) ||
                            fileRequest.getFileMap().containsKey("f_" + qid) ||
                            fileRequest.getFileMap().containsKey("f" + qid) ||
                            fileRequest.getFileMap().containsKey("q" + qid)) {
                        hasFile = true;
                    } else if (questionType != null
                            && (questionType.equalsIgnoreCase("image") || questionType.equalsIgnoreCase("file"))) {
                        boolean matchesOrGeneric = false;
                        if (fileRequest.getFileMap().size() == 1) {
                            String singleKey = fileRequest.getFileMap().keySet().iterator().next();
                            if (singleKey != null && (singleKey.equalsIgnoreCase(fieldName) ||
                                    singleKey.contains("q_" + qid) ||
                                    singleKey.contains("file_" + qid) ||
                                    singleKey.contains("file" + qid) ||
                                    singleKey.contains("f_" + qid) ||
                                    singleKey.contains("f" + qid) ||
                                    singleKey.contains("q" + qid) ||
                                    singleKey.equalsIgnoreCase("files") ||
                                    singleKey.equalsIgnoreCase("file") ||
                                    singleKey.equalsIgnoreCase("image"))) {
                                matchesOrGeneric = true;
                            }
                        }
                        if (fileRequest.getFileMap().containsKey("files") ||
                                fileRequest.getFileMap().containsKey("file") ||
                                fileRequest.getFileMap().containsKey("image") ||
                                matchesOrGeneric) {
                            hasFile = true;
                        }
                    }
                }

                if (hasParam || hasFile) {
                    String answer = params.getOrDefault(fieldName, "");
                    if (answer == null) {
                        answer = "";
                    }

                    // Extract others_answer
                    String othersKey = fieldName + "_other";
                    String othersAnswer = params.getOrDefault(othersKey, "");

                    // Extract parent_answer_id
                    String parentKey = fieldName + "_parent_answer_id";
                    Integer parentAnswerId = 0;
                    if (params.get(parentKey) != null && !params.get(parentKey).isEmpty()) {
                        try {
                            parentAnswerId = Integer.parseInt(params.get(parentKey));
                        } catch (Exception ex) {
                            // ignore
                        }
                    }

                    // Handle image/file uploads
                    if (questionType != null
                            && (questionType.equalsIgnoreCase("image") || questionType.equalsIgnoreCase("file"))) {
                        MultipartFile file = null;
                        if (fileRequest != null) {
                            file = fileRequest.getFile(fieldName);
                            if (file == null || file.isEmpty()) {
                                file = fileRequest.getFile("q_" + qid);
                            }
                            if (file == null || file.isEmpty()) {
                                file = fileRequest.getFile("file_" + qid);
                            }
                            if (file == null || file.isEmpty()) {
                                file = fileRequest.getFile("file" + qid);
                            }
                            if (file == null || file.isEmpty()) {
                                file = fileRequest.getFile("f_" + qid);
                            }
                            if (file == null || file.isEmpty()) {
                                file = fileRequest.getFile("f" + qid);
                            }
                            if (file == null || file.isEmpty()) {
                                file = fileRequest.getFile("q" + qid);
                            }
                            // Fallback generic scan
                            if (file == null || file.isEmpty()) {
                                file = fileRequest.getFile("files");
                            }
                            if (file == null || file.isEmpty()) {
                                file = fileRequest.getFile("file");
                            }
                            if (file == null || file.isEmpty()) {
                                file = fileRequest.getFile("image");
                            }
                            // Fallback scan
                            if (file == null || file.isEmpty()) {
                                for (String name : fileRequest.getFileMap().keySet()) {
                                    if (name.equalsIgnoreCase(fieldName)
                                            || name.contains("q_" + qid)
                                            || name.contains("file_" + qid)
                                            || name.contains("file" + qid)
                                            || name.contains("f_" + qid)
                                            || name.contains("f" + qid)
                                            || name.contains("q" + qid)) {
                                        file = fileRequest.getFile(name);
                                        break;
                                    }
                                }
                            }
                            // Ultimate fallback: if only 1 file exists in the request, use it
                            if ((file == null || file.isEmpty()) && fileRequest.getFileMap().size() == 1) {
                                String singleKey = fileRequest.getFileMap().keySet().iterator().next();
                                if (singleKey != null && (singleKey.equalsIgnoreCase(fieldName) ||
                                        singleKey.contains("q_" + qid) ||
                                        singleKey.contains("file_" + qid) ||
                                        singleKey.contains("file" + qid) ||
                                        singleKey.contains("f_" + qid) ||
                                        singleKey.contains("f" + qid) ||
                                        singleKey.contains("q" + qid) ||
                                        singleKey.equalsIgnoreCase("files") ||
                                        singleKey.equalsIgnoreCase("file") ||
                                        singleKey.equalsIgnoreCase("image"))) {
                                    file = fileRequest.getFileMap().values().iterator().next();
                                }
                            }
                        }

                        if (file != null && !file.isEmpty()) {
                            String uploadedPath = uploadSurveyFile(file, qEnglish, surveyId);
                            if (uploadedPath != null && !uploadedPath.startsWith("Failed")) {
                                answer = uploadedPath;
                                // System.out.println("--- Image Uploaded --- QID: " + qid + " | FieldName: " +
                                // fieldName + " | File: " + file.getOriginalFilename() + " | Path: " +
                                // uploadedPath);
                            }
                        }
                    }

                    boolean isNotWilling = false;
                    if (qid != null && qid == 1) {
                        String trimmed = answer.trim();
                        if ("38".equals(trimmed) || "Not willing to respond (mark them)".equalsIgnoreCase(trimmed)) {
                            isNotWilling = true;
                        }
                    }

                    if (isNotWilling) {
                        jdbcRagPickerSurveyTemplate.update(insertparticipateSql, qid, answer, othersAnswer, surveyId,
                                cid, cby,
                                parentAnswerId);
                        // System.out.println("--- DB SAVE (Participate) --- QID: " + qid + " |
                        // FieldName: " + fieldName + " | Answer: " + answer + " | OthersAnswer: " +
                        // othersAnswer);
                    } else {
                        if (!hasNotWillingResponse) {
                            jdbcRagPickerSurveyTemplate.update(insertSql, qid, answer, othersAnswer, surveyId, cid, cby,
                                    parentAnswerId);
                            // System.out.println("--- DB SAVE (Response) --- QID: " + qid + " | FieldName:
                            // " + fieldName + " | Answer: " + answer + " | OthersAnswer: " + othersAnswer);
                        }
                    }
                }
            }

            // Insert placeholder responses for all other active categories if they do not
            // exist
            if (!hasNotWillingResponse) {
                try {
                    String catSql = "SELECT id FROM question_category_master WHERE isactive = 1 AND isdelete = 0";
                    List<Map<String, Object>> allCats = jdbcRagPickerSurveyTemplate.queryForList(catSql);
                    for (Map<String, Object> catMap : allCats) {
                        Object otherCidObj = catMap.get("id");
                        if (otherCidObj != null) {
                            String otherCid = otherCidObj.toString();
                            if (!otherCid.equals(cid)) {
                                String checkExistSql = "SELECT COUNT(*) FROM rag_picker_survey_response WHERE survey_id = ? AND cid = ? AND isactive = 1 AND isdelete = 0";
                                Integer existCount = jdbcRagPickerSurveyTemplate.queryForObject(checkExistSql,
                                        Integer.class,
                                        surveyId, otherCid);
                                if (existCount == null || existCount == 0) {
                                    String otherQSql = "SELECT qid FROM rag_picker_survey_questions_master WHERE cid = ? AND isactive = 1 AND isdelete = 0";
                                    List<Integer> otherQids = jdbcRagPickerSurveyTemplate.queryForList(otherQSql,
                                            Integer.class, otherCid);
                                    for (Integer otherQid : otherQids) {
                                        jdbcRagPickerSurveyTemplate.update(insertSql, otherQid, null, null, surveyId,
                                                otherCid, cby, 0);
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    System.out.println("Error inserting placeholder survey responses: " + ex.getMessage());
                }
            }

            result.put("status", true);
            result.put("message", "Survey responses saved successfully");
            result.put("survey_id", surveyId);

        } catch (Exception e) {
            e.printStackTrace();
            result.put("status", false);
            result.put("message", "Error saving survey responses: " + e.getMessage());
        }
        return result;
    }

    private String uploadSurveyFile(MultipartFile file, String qEnglish, String surveyId) {
        String uploadDirectory = environment.getProperty("file.upload.directory");
        String serviceFolderName = environment.getProperty("ragpickersurvey_document_foldername");
        // if (serviceFolderName == null || serviceFolderName.trim().isEmpty()) {
        // serviceFolderName = "homelesssurvey_image/";
        // }

        var year = DateTimeUtil.getCurrentYear();
        var month = DateTimeUtil.getCurrentMonth();
        var date = DateTimeUtil.getCurrentDay();

        String finalUploadDir = uploadDirectory + serviceFolderName + year + "/" + month + "/" + date + "/" + surveyId;

        try {
            Path directoryPath = Paths.get(finalUploadDir);
            if (!Files.exists(directoryPath)) {
                Files.createDirectories(directoryPath);
            }

            String cleanName = qEnglish.replaceAll("[^a-zA-Z0-9]", "_").replaceAll("_+", "_");
            String datetimetxt = DateTimeUtil.getCurrentDateTime() + "_" + generateRandomString();
            String fileName = cleanName + "_" + datetimetxt + "_" + file.getOriginalFilename();
            fileName = fileName.replaceAll("\\s+", "");

            String filePath = finalUploadDir + "/" + fileName;
            String relativePath = "/" + serviceFolderName + year + "/" + month + "/" + date + "/" + surveyId + "/"
                    + fileName;

            Path path = Paths.get(filePath);
            byte[] bytes = file.getBytes();

            try {
                BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
                if (image != null) {
                    byte[] compressedBytes = compressImage(image, 0.5f);
                    Files.write(path, compressedBytes);
                } else {
                    Files.write(path, bytes);
                }
            } catch (Exception e) {
                Files.write(path, bytes);
            }

            return relativePath;

        } catch (IOException e) {
            e.printStackTrace();
            return "Failed to save file: " + e.getMessage();
        }
    }

    private byte[] compressImage(BufferedImage image, float quality) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ImageOutputStream imageOutputStream = ImageIO.createImageOutputStream(byteArrayOutputStream);

        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
        writer.setOutput(imageOutputStream);

        ImageWriteParam param = writer.getDefaultWriteParam();
        if (param.canWriteCompressed()) {
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(quality);
        }

        writer.write(null, new javax.imageio.IIOImage(image, null, null), param);

        writer.dispose();
        imageOutputStream.close();
        byteArrayOutputStream.close();

        return byteArrayOutputStream.toByteArray();
    }

    public Map<String, Object> getCategoryStatus(String surveyId) {
        Map<String, Object> result = new HashMap<>();
        try {
            // Check if the Profile category responses or surveyor location record exists
            // for this surveyId
            String checkProfileSql = "SELECT COUNT(*) FROM rag_picker_survey_response WHERE survey_id = ? AND cid = '1' AND isactive = 1 AND isdelete = 0";
            Integer profileCount = jdbcRagPickerSurveyTemplate.queryForObject(checkProfileSql, Integer.class, surveyId);

            String checkLocSql = "SELECT COUNT(*) FROM surveyor_location_details WHERE survey_id = ? AND is_active = 1 AND is_delete = 0";
            Integer locCount = jdbcRagPickerSurveyTemplate.queryForObject(checkLocSql, Integer.class, surveyId);

            if ((profileCount == null || profileCount == 0) && (locCount == null || locCount == 0)) {
                result.put("status", false);
                result.put("message",
                        "Profile not created for this survey ID. Please save the Profile category first.");
                return result;
            }

            String catSql = "SELECT id, english_name, icon, orderby FROM question_category_master WHERE isactive=1 AND isdelete=0 ORDER BY orderby";
            List<Map<String, Object>> categories = jdbcRagPickerSurveyTemplate.queryForList(catSql);

            List<Map<String, Object>> dataList = new ArrayList<>();

            for (Map<String, Object> cat : categories) {
                Object cidObj = cat.get("id");
                String cidStr = cidObj != null ? cidObj.toString() : "";

                String responseSql = "SELECT COUNT(*) FROM (" +
                        "  SELECT id FROM rag_picker_survey_response WHERE survey_id = ? AND cid = ? AND isactive = 1 AND isdelete = 0 AND answer IS NOT NULL AND answer != '' "
                        +
                        "  UNION ALL " +
                        "  SELECT id FROM rag_picker_survey_participate_response WHERE survey_id = ? AND cid = ? AND isactive = 1 AND isdelete = 0 AND answer IS NOT NULL AND answer != '' "
                        +
                        ") t";
                Integer count = jdbcRagPickerSurveyTemplate.queryForObject(responseSql, Integer.class, surveyId, cidStr,
                        surveyId, cidStr);

                Map<String, Object> statusMap = new HashMap<>();
                statusMap.put("cid", cidObj);
                statusMap.put("english_name", cat.get("english_name"));
                statusMap.put("orderby", cat.get("orderby"));
                statusMap.put("icon", cat.get("icon"));
                statusMap.put("status", (count != null && count > 0) ? "Completed" : "Pending");

                dataList.add(statusMap);
            }

            result.put("status", true);
            result.put("data", dataList);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            result.put("status", false);
            result.put("message", "Error fetching category status: " + e.getMessage());
            return result;
        }
    }

    public List<Map<String, Object>> getProfileCreatedList(String cby) {
        List<Map<String, Object>> resultList = new ArrayList<>();
        try {
            String sql;
            List<Object> args = new ArrayList<>();
            if (cby != null && !cby.trim().isEmpty() && !cby.equalsIgnoreCase("null")) {
                sql = "SELECT DISTINCT survey_id FROM rag_picker_survey_response WHERE cid = '1' AND cby = ? AND isactive = 1 AND isdelete = 0";
                args.add(cby);
            } else {
                sql = "SELECT DISTINCT survey_id FROM rag_picker_survey_response WHERE cid = '1' AND isactive = 1 AND isdelete = 0";
            }

            List<String> surveyIds = jdbcRagPickerSurveyTemplate.queryForList(sql, args.toArray(), String.class);

            for (String surveyId : surveyIds) {
                Map<String, Object> profileMap = new LinkedHashMap<>();
                profileMap.put("survey_id", surveyId);

                // Fetch Location Details
                String locSql = "SELECT zone, ward, latitude, longitude, address, loginId as cby, cdate FROM surveyor_location_details WHERE survey_id = ? AND is_active = 1 AND is_delete = 0 LIMIT 1";
                List<Map<String, Object>> locData = jdbcRagPickerSurveyTemplate.queryForList(locSql, surveyId);

                if (!locData.isEmpty()) {
                    Map<String, Object> loc = locData.get(0);
                    profileMap.put("cby", loc.get("cby"));
                    profileMap.put("cdate", loc.get("cdate"));
                    profileMap.put("zone", loc.get("zone"));
                    profileMap.put("ward", loc.get("ward"));
                    profileMap.put("latitude", loc.get("latitude"));
                    profileMap.put("longitude", loc.get("longitude"));
                    profileMap.put("address", loc.get("address"));
                } else {
                    profileMap.put("zone", "");
                    profileMap.put("ward", "");
                    profileMap.put("latitude", "");
                    profileMap.put("longitude", "");
                    profileMap.put("address", "");

                    String fallbackSql = "SELECT cby, cdate FROM rag_picker_survey_response WHERE survey_id = ? AND cid = '1' AND isactive = 1 AND isdelete = 0 LIMIT 1";
                    List<Map<String, Object>> fallbackData = jdbcRagPickerSurveyTemplate.queryForList(fallbackSql,
                            surveyId);
                    if (!fallbackData.isEmpty()) {
                        profileMap.put("cby", fallbackData.get(0).get("cby"));
                        profileMap.put("cdate", fallbackData.get(0).get("cdate"));
                    } else {
                        profileMap.put("cby", "");
                        profileMap.put("cdate", "");
                    }
                }

                // Fetch Answers
                String ansSql = "SELECT q.qid, q.field_name, q.question_type, q.q_english, q.master_table_name, r.answer, r.others_answer "
                        +
                        "FROM rag_picker_survey_response r " +
                        "JOIN rag_picker_survey_questions_master q ON r.qid = q.qid " +
                        "WHERE r.survey_id = ? AND r.cid = '1' AND r.isactive = 1 AND r.isdelete = 0 " +
                        "AND ((q.isactive = 1 AND q.isdelete = 0) OR (q.cid = '1' AND q.field_name IN ('q2', 'q78', 'q11', 'q8', 'q9', 'q10', 'q77')))";

                List<Map<String, Object>> ansData = jdbcRagPickerSurveyTemplate.queryForList(ansSql, surveyId);
                List<Map<String, Object>> answersList = new ArrayList<>();

                String fileBaseUrlVal = environment.getProperty("fileBaseUrl");
                if (fileBaseUrlVal == null) {
                    fileBaseUrlVal = "";
                }

                List<String> allowedFields = java.util.Arrays.asList("q2", "q78", "q11", "q8", "q9", "q10", "q77");

                for (Map<String, Object> ans : ansData) {
                    String fieldName = (String) ans.get("field_name");
                    if (fieldName != null && allowedFields.contains(fieldName)) {
                        Integer qid = (Integer) ans.get("qid");
                        String qEnglish = (String) ans.get("q_english");
                        String masterTable = (String) ans.get("master_table_name");
                        String questionType = (String) ans.get("question_type");
                        String answerVal = (String) ans.get("answer");

                        String resolvedValue = answerVal != null ? answerVal : "";

                        if (answerVal != null && !answerVal.trim().isEmpty()) {
                            // Try to resolve the option ID to its english name
                            try {
                                int optionId = Integer.parseInt(answerVal.trim());
                                if (masterTable != null && !masterTable.trim().isEmpty()) {
                                    String query = "SELECT english_name FROM " + masterTable + " WHERE id = ? LIMIT 1";
                                    List<String> names = jdbcRagPickerSurveyTemplate.queryForList(query, String.class,
                                            optionId);
                                    if (!names.isEmpty()) {
                                        resolvedValue = names.get(0);
                                    }
                                } else {
                                    String query = "SELECT answer FROM rag_picker_survey_answer_master WHERE qid = ? AND id = ? LIMIT 1";
                                    List<String> names = jdbcRagPickerSurveyTemplate.queryForList(query, String.class,
                                            qid, optionId);
                                    if (!names.isEmpty()) {
                                        resolvedValue = names.get(0);
                                    }
                                }
                            } catch (NumberFormatException e) {
                                // Keep original value if not a valid number ID
                            }
                        }

                        if (resolvedValue != null && !resolvedValue.isEmpty() && questionType != null &&
                                (questionType.equalsIgnoreCase("image") || questionType.equalsIgnoreCase("file"))) {
                            resolvedValue = fileBaseUrlVal + "/gccofficialapp/files" + resolvedValue;
                        }

                        Map<String, Object> ansMap = new LinkedHashMap<>();
                        ansMap.put("question", qEnglish != null ? qEnglish : "");
                        ansMap.put("field_name", fieldName);
                        ansMap.put("field_value", resolvedValue != null ? resolvedValue : "");
                        answersList.add(ansMap);

                        String othersVal = (String) ans.get("others_answer");
                        if (othersVal != null && !othersVal.isEmpty()) {
                            Map<String, Object> othersMap = new LinkedHashMap<>();
                            othersMap.put("question", "Please specify");
                            othersMap.put("field_name", fieldName + "_other");
                            othersMap.put("field_value", othersVal);
                            answersList.add(othersMap);
                        }
                    }
                }
                profileMap.put("answers", answersList);
                resultList.add(profileMap);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return resultList;
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
    
    private void saveFamilyMembers(List<Map<String, Object>> familyList, String surveyId, String cby) {
        if (familyList == null || familyList.isEmpty()) {
            return;
        }

        String insertFamilySql = "INSERT INTO rag_picker_survey_family_response " +
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
                        qid = 2;
                    } else if ("relationship".equals(cleanKey)) {
                        qid = 15;
                    } else if ("age".equals(cleanKey)) {
                        qid = 6;
                    } 
                }

                // Fallback: If still null, try resolving by field_name or matching q_english
                // (case-insensitive) from DB without active/delete constraint
                if (qid == null) {
                    try {
                        String cleanKey = key.toLowerCase().trim();
                        String getQidSql = "SELECT qid FROM rag_picker_survey_questions_master " +
                                "WHERE (LOWER(field_name) = ? OR LOWER(q_english) = ? OR LOWER(q_english) LIKE ?) LIMIT 1";
                        List<Integer> qidList = jdbcRagPickerSurveyTemplate.queryForList(getQidSql, Integer.class,
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
                        if (qid == 2) {
                            parentAnswerId = 1;
                        } else if (qid == 15) {
                            parentAnswerId = 7;
                        } else if (qid == 6) {
                            parentAnswerId = 3;
                        } 
                    }

                    // Fallback: If still null, try resolving from mapping table (without
                    // active/delete constraint)
                    if (parentAnswerId == null) {
                        try {
                            String parentAidSql = "SELECT parent_aid FROM rag_picker_question_mapping WHERE child_qid = ? LIMIT 1";
                            List<Integer> list = jdbcRagPickerSurveyTemplate.queryForList(parentAidSql, Integer.class, qid);
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
                        jdbcRagPickerSurveyTemplate.update(insertFamilySql, qid, answer, othersValue, cby, surveyId,
                                parentAnswerId);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
    
    
    
}
