package in.gov.chennaicorporation.mobileservice.gccHomeLessSurveyNew.Service;

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
public class HomeLessSurveyService {

    @Autowired
    JdbcTemplate jdbcHomeLessSurveyTemplate;

    @Autowired
    public void setDataSource(@Qualifier("mysqlHomeLessSurveyDataSource") DataSource homeLessSurveyDataSource) {
        this.jdbcHomeLessSurveyTemplate = new JdbcTemplate(homeLessSurveyDataSource);
    }

    public List<Map<String, Object>> getSurveyQuestions(Integer cid) {

        String childSql = "SELECT DISTINCT child_qid FROM homeless_question_mapping " +
                "WHERE isactive=1 AND isdelete=0";

        List<Integer> childQids = jdbcHomeLessSurveyTemplate.queryForList(childSql, Integer.class);

        String questionSql = "SELECT * FROM homeless_survey_questions_master " +
                "WHERE cid=? AND isactive=1 AND isdelete=0 ORDER BY orderby";

        List<Map<String, Object>> questions = jdbcHomeLessSurveyTemplate.queryForList(questionSql, cid);

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
                    "FROM homeless_survey_answer_master " +
                    "WHERE qid=? AND isactive=1 AND isdelete=0 ORDER BY orderby";

            List<Map<String, Object>> dbOptions = jdbcHomeLessSurveyTemplate.queryForList(optSql, qid);

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

        String mappingSql = "SELECT child_qid FROM homeless_question_mapping " +
                "WHERE parent_aid=? AND isactive=1 AND isdelete=0";

        List<Integer> childQids = jdbcHomeLessSurveyTemplate.queryForList(mappingSql, Integer.class, parentAid);

        List<Map<String, Object>> childQuestions = new ArrayList<>();

        for (Integer cqid : childQids) {

            String qSql = "SELECT * FROM homeless_survey_questions_master " +
                    "WHERE qid=? AND isactive=1 AND isdelete=0";

            List<Map<String, Object>> childQList = jdbcHomeLessSurveyTemplate.queryForList(qSql, cqid);

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

        List<Map<String, Object>> dbOptions = jdbcHomeLessSurveyTemplate.queryForList(sql);

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
        return jdbcHomeLessSurveyTemplate.queryForList(sql);
    }

    public List<Map<String, Object>> getDistricts(String sid) {
        if (sid != null && !sid.trim().isEmpty()) {
            String sql = "SELECT id AS option_id, english_name, IFNULL(tamil_name, '') AS tamil_name, sid, orderby " +
                         "FROM district_master " +
                         "WHERE sid=? AND isactive=1 AND isdelete=0 " +
                         "ORDER BY orderby";
            return jdbcHomeLessSurveyTemplate.queryForList(sql, sid);
        } else {
            String sql = "SELECT id AS option_id, english_name, IFNULL(tamil_name, '') AS tamil_name, sid, orderby " +
                         "FROM district_master " +
                         "WHERE isactive=1 AND isdelete=0 " +
                         "ORDER BY orderby";
            return jdbcHomeLessSurveyTemplate.queryForList(sql);
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

            List<Map<String, Object>> data = jdbcHomeLessSurveyTemplate.queryForList(sql, mobileNo, password);

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
}
