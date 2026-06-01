package in.gov.chennaicorporation.mobileservice.gccMobileMenus.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


import in.gov.chennaicorporation.mobileservice.gccMobileMenus.service.MobileMenusService;

@RequestMapping("/gccofficialapp/api/gccmobilemenu")
@RestController("gccofficialappgccmobilemenuConroller")
public class MobileMenusApiController {

	@Autowired
	private MobileMenusService mobileMenusService;
	
	
	@GetMapping({ "/versionCheck" })
    public List<?> versionCheck() {
        return mobileMenusService.versionCheck();
    }
	
	@GetMapping("/getmenus")
    public List<Map<String,Object>> getMenus(
            @RequestParam Integer login_id){

        return mobileMenusService.getMenus(login_id);
    }

    @GetMapping("/getsubmenus")
    public List<Map<String,Object>> getSubMenus(
            @RequestParam Integer login_id,
            @RequestParam Integer menu_id){

        return mobileMenusService.getSubMenus(login_id, menu_id);
    }
    
    
	
}
