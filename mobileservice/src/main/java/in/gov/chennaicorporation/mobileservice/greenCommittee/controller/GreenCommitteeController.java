package in.gov.chennaicorporation.mobileservice.greenCommittee.controller;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import in.gov.chennaicorporation.mobileservice.greenCommittee.service.GreenCommitteeService;

@RequestMapping("/gccofficialapp/api/greencommittee")
@RestController("gccofficialappagreencommittee")
public class GreenCommitteeController {

	@Autowired
	GreenCommitteeService greenCommitteeService;
	
	@GetMapping(value="/getInspectionBy")
	public List<?> getInspectionBy(
			@RequestParam(value="loginId", required = true) String loginId
			){
		return greenCommitteeService.getInspectionBy(loginId);
	}
	
	@GetMapping(value="/getInspectionDepartmentList")
	public List<?> getInspectionDepartmentList(
			@RequestParam(value="loginId", required = true) String loginId
			){
		return greenCommitteeService.getInspectionDepartmentList();
	}
	
	@GetMapping(value="/getRecommendMasterList")
	public List<?> getRecommendMasterList(
			@RequestParam(value="loginId", required = true) String loginId
			){
		return greenCommitteeService.getRecommendMasterList();
	}
	
	@GetMapping(value="/getComplaintList")
	public List<?> getComplaintList(
			@RequestParam(value="loginId", required = true) String loginId
			){
		return greenCommitteeService.getComplaintList(loginId);
	}
	
	@PostMapping("/saveInspectionData")
 	public List<?> saveInspectionData(
 			 @RequestParam(value = "refid", required = true) String ref_id,
 			 @RequestParam(value = "inspectionby", required = true) String inspection_by,
 			 @RequestParam(value = "inspectiontype", required = true) String visit_type,
 			 @RequestParam(value = "jointWork", required = true) String jointWork,
 			 @RequestParam(value = "remarks", required = true) String remarks,
             @RequestParam(value = "zone", required = true) String zone,
             @RequestParam(value = "ward", required = true) String ward,
             @RequestParam(value = "street_name", required = false) String street_name,
             @RequestParam(value = "street_id", required = false) String street_id,
             @RequestParam(value = "latitude", required = true) String latitude,
             @RequestParam(value = "longitude", required = true) String longitude,
             @RequestParam(value = "loginId", required = true) String inby,
             @RequestParam(value = "file_1", required = true) MultipartFile file_1,
             @RequestParam(value = "file_2", required = true) MultipartFile file_2,
             @RequestParam(value = "file_3", required = true) MultipartFile file_3,
             @RequestParam(value = "treeData", required = true) String treeData,
			 @RequestParam(value = "reinspection_id", required = false) String reinspection_id

     ) throws Exception {
		//Object mapper = new ObjectMapper();
		ObjectMapper mapper = new ObjectMapper(); 
		
	    List<Map<String, Object>> treeList =
	            mapper.readValue(treeData, new TypeReference<List<Map<String, Object>>>() {});
	    List<Map<String, Object>> jointList =
	            mapper.readValue(jointWork, new TypeReference<List<Map<String, Object>>>() {});

	    // Convert jointWork → "3,2,1"
	    StringBuilder visitByBuilder = new StringBuilder();

	    for (Map<String, Object> m : jointList) {
	        if (m.get("work_category_ids") != null) {
	            if (visitByBuilder.length() > 0) {
	                visitByBuilder.append(",");
	            }
	            visitByBuilder.append(m.get("work_category_ids").toString());
	        }
	    }

	    String visitBy = visitByBuilder.toString();
	    
 		return greenCommitteeService.saveInspectionData(
 				ref_id,
 				inspection_by,
 			    visit_type,
 			    visitBy,
 			    remarks,
 			    zone,
 			    ward,
 			    street_name,
 			    street_id,
 			    latitude,
 			    longitude,
 			    inby,
 			    file_1,
 			    file_2,
 			    file_3,
 			    treeList,
				reinspection_id
         );
    }
}
