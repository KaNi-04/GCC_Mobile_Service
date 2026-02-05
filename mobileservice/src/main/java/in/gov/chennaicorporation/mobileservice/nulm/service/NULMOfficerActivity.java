package in.gov.chennaicorporation.mobileservice.nulm.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import in.gov.chennaicorporation.mobileservice.gccactivity.service.DateTimeUtil;

@Service
public class NULMOfficerActivity {
	private JdbcTemplate jdbcNULMTemplate;
	private final Environment environment;
	private String fileBaseUrl;

	@Autowired
	public void setDataSource(@Qualifier("mysqlNulmDataSource") DataSource sosDataSource) {
		this.jdbcNULMTemplate = new JdbcTemplate(sosDataSource);
	}

	@Autowired
	public NULMOfficerActivity(Environment environment) {
		this.environment = environment;
		this.fileBaseUrl = environment.getProperty("fileBaseUrl");
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

		return byteArrayOutputStream.toByteArray();
	}

	public List<Map<String, Object>> Template(
			String loginId,
			String request_type,
			String status,
			String zone,
			String ward) {

		String sqlQuery = "SELECT * FROM `rescue` WHERE `isactive`=1 ";

		System.out.println(sqlQuery);
		List<Map<String, Object>> result = jdbcNULMTemplate.queryForList(sqlQuery);
		Map<String, Object> response = new HashMap<>();
		response.put("status", "Success");
		response.put("message", "Request List");
		response.put("ward", result);

		return Collections.singletonList(response);
	}

	public List<Map<String, Object>> getAllRequest(
			String request_type,
			String zone,
			String ward,
			String streetid,
			String fromdate,
			String todate,
			String loginId,
			String status) {

		String sqlQuery = "SELECT * FROM `rescue` WHERE `isactive`=1 ";

		System.out.println(sqlQuery);
		List<Map<String, Object>> result = jdbcNULMTemplate.queryForList(sqlQuery);
		Map<String, Object> response = new HashMap<>();
		response.put("status", "Success");
		response.put("message", "Request List");
		response.put("ward", result);

		return Collections.singletonList(response);
	}

	public List<Map<String, Object>> getStaffListToRegister(
			String reporterId) {

		String sqlQuery = "SELECT * FROM `enrollment_table` WHERE (`isactive`=1 AND `appointed`=1 AND `facial_attendance`=0)"
				+ " AND `incharge_id`='" + reporterId + "'";

		System.out.println(sqlQuery);
		List<Map<String, Object>> result = jdbcNULMTemplate.queryForList(sqlQuery);
		Map<String, Object> response = new HashMap<>();
		response.put("status", "Success");
		response.put("message", "Request List");
		response.put("Data", result);

		return Collections.singletonList(response);
	}

	public List<Map<String, Object>> updateStaffFaceRegister(
			String reporterId,
			String enrollId,
			String status) {
		Map<String, Object> response = new HashMap<>();

		if ((reporterId != null && !reporterId.isBlank() && !reporterId.isEmpty())
				&& (enrollId != null && !enrollId.isEmpty() && !enrollId.isBlank())) {
			String sqlQuery = "UPDATE `enrollment_table` SET `facial_attendance`=1 WHERE `enrollment_id`='" + enrollId
					+ "'"
					+ " AND `incharge_id`='" + reporterId + "'";

			System.out.println(sqlQuery);
			Integer updateResult = jdbcNULMTemplate.update(sqlQuery);

			sqlQuery = "SELECT * FROM `enrollment_table` WHERE (`isactive`=1 AND `appointed`=1 AND `facial_attendance`=1)"
					+ " AND `enrollment_id`='" + enrollId + "'";
			List<Map<String, Object>> result = jdbcNULMTemplate.queryForList(sqlQuery);

			response.put("status", "Success");
			response.put("message", "Facial register successfully!");
			response.put("Data", result);
		} else {
			response.put("status", "Failed");
			response.put("message", "Invalid Data");
		}

		return Collections.singletonList(response);
	}

	public List<Map<String, Object>> getStaffListForAttendance(
			String reporterId) {

		String sqlQuery = "SELECT e.*, "
				+ "       IFNULL(DATE_FORMAT(a.indatetime, '%d-%m-%Y %l:%i %p'), '') AS indatetime, "
				+ "       IFNULL(DATE_FORMAT(a.outdatetime, '%d-%m-%Y %l:%i %p'), '') AS outdatetime, "
				+ "       a.inby, "
				+ "       a.outby, "
				+ "       a.inphoto, "
				+ "       a.outphoto "
				+ "FROM enrollment_table e "
				+ "LEFT JOIN attendance a ON e.enrollment_id = a.enrollment_id"
				+ "    AND a.indatetime = ("
				+ "        SELECT MAX(a2.indatetime) "
				+ "        FROM attendance a2 "
				+ "        WHERE a2.enrollment_id = e.enrollment_id"
				+ "    )"
				+ "WHERE e.isactive = 1 "
				+ "  AND e.appointed = 1 "
				+ "  AND e.facial_attendance = 1 "
				+ "  AND e.incharge_id = '" + reporterId + "'";

		System.out.println(sqlQuery);
		List<Map<String, Object>> result = jdbcNULMTemplate.queryForList(sqlQuery);
		Map<String, Object> response = new HashMap<>();
		response.put("status", "Success");
		response.put("message", "Request List");
		response.put("Data", result);

		return Collections.singletonList(response);
	}

	@Transactional
	public List<Map<String, Object>> markAttendance(
			String reporterId,
			String enrollId,
			String action, // "in" for check-in, "out" for check-out
			String photourl) {

		Map<String, Object> response = new HashMap<>();
		int lastQueryId = 0;

		String datetimetxt = DateTimeUtil.getCurrentDateTimeMysql(); // Current timestamp

		try {
			if ("in".equalsIgnoreCase(action)) {
				// Insert a new attendance record for check-in
				String sqlQuery = "INSERT INTO attendance (indatetime, inby, inphoto, enrollment_id) "
						+ "VALUES (?, ?, ?, ?)";

				KeyHolder keyHolder = new GeneratedKeyHolder();
				int affectedRows = jdbcNULMTemplate.update(new PreparedStatementCreator() {
					@Override
					public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
						PreparedStatement ps = connection.prepareStatement(sqlQuery, new String[] { "id" });
						ps.setString(1, datetimetxt); // Check-in time
						ps.setString(2, reporterId); // Mark By
						ps.setString(3, photourl); // Photo URL for check-in
						ps.setString(4, enrollId); // Employee ID (enrollId)
						return ps;
					}
				}, keyHolder);

				if (affectedRows > 0) {
					Number generatedId = keyHolder.getKey();
					lastQueryId = (generatedId != null) ? generatedId.intValue() : 0;
					response.put("Id", lastQueryId);
					response.put("status", "success");
					response.put("message", "Check-in was recorded successfully!");
					System.out.println("Check-in was recorded successfully! Attendance ID: " + generatedId);
				} else {
					response.put("status", "error");
					response.put("message", "Failed to record check-in.");
				}

			} else if ("out".equalsIgnoreCase(action)) {
				// Update the existing attendance record for check-out
				String sqlQuery = "UPDATE attendance SET outdatetime = ?, outby = ?, outphoto = ? "
						+ "WHERE enrollment_id = ? AND outdatetime IS NULL "
						+ "ORDER BY indatetime DESC LIMIT 1";

				int affectedRows = jdbcNULMTemplate.update(sqlQuery, datetimetxt, reporterId, photourl, enrollId);

				if (affectedRows > 0) {
					response.put("status", "success");
					response.put("message", "Check-out was recorded successfully!");
					System.out.println("Check-out was recorded successfully!");
				} else {
					response.put("status", "error");
					response.put("message", "Failed to record check-out. No pending check-in found.");
				}
			} else if ("od".equalsIgnoreCase(action)) {
				// Mark On Duty (OD) even if no check-in
				String sqlQuery = "INSERT INTO attendance (oddatetime, odby, enrollment_id) "
						+ "VALUES (?, ?, ?)";

				int affectedRows = jdbcNULMTemplate.update(sqlQuery, datetimetxt, reporterId, enrollId);

				if (affectedRows > 0) {
					response.put("status", "success");
					response.put("message", "On Duty was recorded successfully!");
					System.out.println("On Duty was recorded successfully!");
				} else {
					response.put("status", "error");
					response.put("message", "Failed to record On Duty.");
				}

			} else if ("leave".equalsIgnoreCase(action)) {
				// Mark Leave even if no check-in
				String sqlQuery = "INSERT INTO attendance (leavedatetime, leaveby, enrollment_id) "
						+ "VALUES (?, ?, ?)";

				int affectedRows = jdbcNULMTemplate.update(sqlQuery, datetimetxt, reporterId, enrollId);

				if (affectedRows > 0) {
					response.put("status", "success");
					response.put("message", "Leave was recorded successfully!");
					System.out.println("Leave was recorded successfully!");
				} else {
					response.put("status", "error");
					response.put("message", "Failed to record Leave.");
				}

			} else {
				response.put("status", "error");
				response.put("message",
						"Invalid action. Use 'in' for check-in, 'out' for check-out, 'od' for On Duty, or 'leave' for Leave.");
			}
		} catch (DataAccessException e) {
			System.out.println("Data Access Exception:");
			Throwable rootCause = e.getMostSpecificCause();
			if (rootCause instanceof SQLException) {
				SQLException sqlException = (SQLException) rootCause;
				System.out.println("SQL State: " + sqlException.getSQLState());
				System.out.println("Error Code: " + sqlException.getErrorCode());
				System.out.println("Message: " + sqlException.getMessage());
				response.put("status", "error");
				response.put("message", sqlException.getMessage());
				response.put("sqlState", sqlException.getSQLState());
				response.put("errorCode", sqlException.getErrorCode());
			} else {
				System.out.println("Message: " + rootCause.getMessage());
				response.put("status", "error");
				response.put("message", rootCause.getMessage());
			}
		}

		return Collections.singletonList(response);
	}

	public List<Map<String, Object>> getStaffAttendanceListByDate(
			String reporterId,
			String specificDate // Pass the specific date as an argument
	) {

		String sqlQuery = "SELECT e.*, " +
				"       IFNULL(DATE_FORMAT(a.indatetime, '%d-%m-%Y %l:%i %p'), '') AS indatetime, " +
				"       IFNULL(DATE_FORMAT(a.outdatetime, '%d-%m-%Y %l:%i %p'), '') AS outdatetime, " +
				"       a.inby, " +
				"       a.outby, " +
				"       a.inphoto, " +
				"       a.outphoto, " +
				"       IFNULL(TIMEDIFF(a.outdatetime, a.indatetime), '') AS total_working_hours " +
				"FROM enrollment_table e " +
				"LEFT JOIN attendance a ON e.enrollment_id = a.enrollment_id " +
				"    AND DATE_FORMAT(a.indatetime, '%d-%m-%Y') = '" + specificDate + "' " + // Ensure the date matches
																							// the specific date
				"WHERE e.isactive = 1 " +
				"  AND e.appointed = 1 " +
				"  AND e.facial_attendance = 1 " +
				"  AND e.incharge_id = '" + reporterId + "'";

		System.out.println(sqlQuery);

		// Execute the query with specificDate and reporterId as parameters
		List<Map<String, Object>> result = jdbcNULMTemplate.queryForList(sqlQuery);

		// Prepare the response
		Map<String, Object> response = new HashMap<>();
		response.put("status", "Success");
		response.put("message", "Request List");
		response.put("Data", result);

		return Collections.singletonList(response);
	}

	public List<String> getAttendanceListByStaff(String year, String month, int empId, String attendanceType) {

		String sqlQuery = null;

		if (attendanceType.equals("P")) {

			sqlQuery = "select DATE_FORMAT(indatetime, '%d-%m-%Y') as DATE from attendance where enrollment_id = '"
					+ empId + "' "
					+ "and year(indatetime) = '" + year + "' and monthname(indatetime) = '" + month + "' ";

		} else if (attendanceType.equals("A")) {

			sqlQuery = "select DATE_FORMAT(leavedatetime, '%d-%m-%Y') as DATE from attendance where enrollment_id = '"
					+ empId + "' "
					+ "and year(leavedatetime) = '" + year + "' and monthname(leavedatetime) = '" + month + "' ";

		} else if (attendanceType.equals("O")) {

			sqlQuery = "select DATE_FORMAT (oddatetime, '%d-%m-%Y') as DATE from attendance where enrollment_id = '"
					+ empId + "' "
					+ "and year(oddatetime) = '" + year + "' and monthname(oddatetime) = '" + month + "' ";
		}

		// Execute the query
		List<String> result = jdbcNULMTemplate.queryForList(sqlQuery, String.class);

		if (result.isEmpty())
			return Collections.singletonList("No Data Found");
		else
			return result;
	}

	public List<Map<String, Object>> getStaffListForAttendanceMultipleIncharge(String reporterId) {
		// String sql = "SELECT incharge_id FROM additional_incharge WHERE
		// FIND_IN_SET(?, additional_id)";

		String sql = "SELECT "
				+ "    CASE "
				+ "        WHEN EXISTS ( "
				+ "            SELECT 1  "
				+ "            FROM additional_incharge  "
				+ "            WHERE incharge_id = ?  "
				+ "              AND is_active = 1  "
				+ "              AND is_delete = 0 "
				+ "        ) "
				+ "        THEN ( "
				+ "            SELECT additional_id "
				+ "            FROM additional_incharge "
				+ "            WHERE incharge_id = ? "
				+ "              AND is_active = 1 "
				+ "              AND is_delete = 0 "
				+ "            LIMIT 1 "
				+ "        ) "
				+ "        WHEN EXISTS ( "
				+ "            SELECT 1  "
				+ "            FROM additional_incharge "
				+ "            WHERE FIND_IN_SET( ? , additional_id) > 0 "
				+ "              AND is_active = 1 "
				+ "              AND is_delete = 0 "
				+ "        ) "
				+ "        THEN ? "
				+ "        ELSE NULL "
				+ "    END AS result ";
		String id = jdbcNULMTemplate.queryForObject(sql, String.class, reporterId, reporterId, reporterId, reporterId);
		// String id = jdbcNULMTemplate.queryForObject(sql, String.class, reporterId);
		return getStaffListForAttendance(id);
	}
}
