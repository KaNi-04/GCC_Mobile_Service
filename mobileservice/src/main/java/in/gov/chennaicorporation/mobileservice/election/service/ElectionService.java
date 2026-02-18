package in.gov.chennaicorporation.mobileservice.election.service;

import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ElectionService {

    @Autowired
    JdbcTemplate jdbcElectionTemplate;

    private Environment environment;

    private String fileBaseUrl;

    @Autowired
    public void setDataSource(@Qualifier("mysqlElectionDataSource") DataSource electionDataSource) {
        this.jdbcElectionTemplate = new JdbcTemplate(electionDataSource);
    }

    public ElectionService(Environment environment) {
        this.environment = environment;
        this.fileBaseUrl = environment.getProperty("fileBaseUrl");
    }

    @Transactional
    public List<Map<String, Object>> getApplicantsByMobile(String slno) {

        try {

            String sql = "SELECT " +

            /* Person Details */
                    "ppn.id AS person_id, " +
                    "ppn.slno, " +
                    "UPPER(ppn.name) AS name, " +
                    "ppn.age, " +
                    "ppn.sex, " +
                    "ppn.r_mobile_no, " +
                    "ppn.r_phone_no, " +
                    "ppn.r_address1, " +
                    "ppn.r_address2, " +
                    "ppn.r_address3, " +
                    "ppn.r_pincode, " +
                    "ppn.batch_no," +

                    /* Department */
                    "ppn.dept AS dept_id, " +
                    "dm.dept_name, " +

                    /* Designation */
                    "ppn.designation_id AS designation_id, " +
                    "d.new_designation AS designation_name, " +

                    /* Office */
                    "ppn.office_id AS office_id, " +
                    "od.office_name, " +

                    /* Image */
                    "CONCAT('" + fileBaseUrl + "/election/files', ppm.image_path) AS img_full_path, " +

                    /* Office Full Address */
                    "CONCAT( " +
                    "COALESCE(od.address1, ''), ' ', " +
                    "COALESCE(od.address2, ''), ' ', " +
                    "COALESCE(od.address3, ''), ' ', " +
                    "COALESCE(od.pincode, '') " +
                    ") AS office_address, " +

                    /* Constituencies */
                    "ppn.native_ac_no, ac_native.ac_name AS native_constituency, " +
                    "ppn.reside_ac_no, ac_reside.ac_name AS residential_constituency, " +
                    "ppn.elector_ac_no, ac_elector.ac_name AS elector_constituency, " +
                    "od.work_ac_no, ac_work.ac_name AS working_constituency " +

                    "FROM poll_person_new ppn " +

                    "LEFT JOIN poll_person_images ppm ON ppm.ppn_id = ppn.id " +
                    "LEFT JOIN dept_master dm ON ppn.dept = dm.dept_id " +
                    "LEFT JOIN designation d ON ppn.designation_id = d.desig_id " +
                    "LEFT JOIN office_details od ON ppn.office_id = od.id " +
                    "LEFT JOIN ac_list_all ac_native ON ac_native.ac_no = ppn.native_ac_no " +
                    "LEFT JOIN ac_list_all ac_reside ON ac_reside.ac_no = ppn.reside_ac_no " +
                    "LEFT JOIN ac_list_all ac_elector ON ac_elector.ac_no = ppn.elector_ac_no " +
                    "LEFT JOIN ac_list_all ac_work ON ac_work.ac_no = od.work_ac_no " +

                    "WHERE ppn.slno = ? " +
                    "AND ppn.is_active = 1 AND ppn.is_delete = 0";

            List<Map<String, Object>> data = jdbcElectionTemplate.queryForList(sql, slno);

            // if (data.isEmpty()) {
            // throw new RuntimeException("Your details are not registered");
            // }

            /* 🔥 Convert all NULL values to empty string */
            for (Map<String, Object> row : data) {
                for (Map.Entry<String, Object> entry : row.entrySet()) {
                    if (entry.getValue() == null) {
                        entry.setValue("");
                    }
                }
            }

            return data;

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error fetching applicant details: " + e.getMessage());
        }
    }

    public List<Map<String, Object>> getBatchDetails(String mobileNo) {

        try {

            String sql = "SELECT r_mobile_no AS mobile_number, slno " +
                    "FROM poll_person_new " +
                    "WHERE r_mobile_no = ?";

            List<Map<String, Object>> data = jdbcElectionTemplate.queryForList(sql, mobileNo);

            for (Map<String, Object> row : data) {

                Object slnoObj = row.get("slno");

                // Convert null → ""
                String slno = (slnoObj == null) ? "" : slnoObj.toString().trim();
                row.put("slno", slno);

                // hasLogin flag
                boolean hasLogin = !slno.isEmpty();
                row.put("hasLogin", hasLogin);

                // Convert mobile_number null → ""
                Object mobileObj = row.get("mobile_number");
                row.put("mobile_number",
                        mobileObj == null ? "" : mobileObj.toString());
            }

            return data;

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error fetching batch details: " + e.getMessage());
        }
    }

    // @Transactional
    // public List<Map<String, Object>> getApplicantsByMobile(String mobileNo) {

    // try {

    // String sql = "SELECT ppn.*, " +
    // "UPPER(ppn.name) AS uppername, " +
    // "dm.*, d.*, od.*, " +

    // "CONCAT('" + fileBaseUrl + "/election/files', ppm.image_path) AS
    // img_full_path, " +

    // "CONCAT( " +
    // "COALESCE(od.address1, ''), ' ', " +
    // "COALESCE(od.address2, ''), ' ', " +
    // "COALESCE(od.address3, ''), ' ', " +
    // "COALESCE(od.pincode, '') " +
    // ") AS address, " +

    // "ac_native.ac_name AS native_constituency, " +
    // "ac_reside.ac_name AS residential_constituency, " +
    // "ac_elector.ac_name AS elector_constituency, " +
    // "ac_work.ac_name AS working_constituency " +

    // "FROM poll_person_new ppn " +

    // "LEFT JOIN poll_person_images ppm ON ppm.ppn_id = ppn.id " +
    // "LEFT JOIN dept_master dm ON ppn.dept = dm.dept_id " +
    // "LEFT JOIN designation d ON ppn.designation_id = d.desig_id " +
    // "LEFT JOIN office_details od ON ppn.office_id = od.id " +
    // "LEFT JOIN ac_list_all ac_native ON ac_native.ac_no = ppn.native_ac_no " +
    // "LEFT JOIN ac_list_all ac_reside ON ac_reside.ac_no = ppn.reside_ac_no " +
    // "LEFT JOIN ac_list_all ac_elector ON ac_elector.ac_no = ppn.elector_ac_no " +
    // "LEFT JOIN ac_list_all ac_work ON ac_work.ac_no = od.work_ac_no " +

    // "WHERE ppn.r_mobile_no = ?";

    // List<Map<String, Object>> data = jdbcElectionTemplate.queryForList(sql,
    // mobileNo);

    // // Check if no data found
    // if (data.isEmpty()) {
    // throw new RuntimeException("Your details are not registered");
    // }

    // /* Convert all NULL values to empty string */
    // for (Map<String, Object> row : data) {
    // for (Map.Entry<String, Object> entry : row.entrySet()) {
    // if (entry.getValue() == null) {
    // entry.setValue("");
    // }
    // }
    // }

    // return data;

    // } catch (Exception e) {
    // e.printStackTrace();
    // throw new RuntimeException(e.getMessage());
    // }
    // }

}
