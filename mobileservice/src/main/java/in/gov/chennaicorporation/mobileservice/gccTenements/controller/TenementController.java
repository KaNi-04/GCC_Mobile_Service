package in.gov.chennaicorporation.mobileservice.gccTenements.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import in.gov.chennaicorporation.mobileservice.gccTenements.service.TenementService;

@RequestMapping("/gccofficialapp/api/tenement")
@RestController("gccofficialapptenement")
public class TenementController {

    @Autowired
    private TenementService tenementService;

    @GetMapping("/getTenementsListByWard")
    public Map<String, Object> getTenementsListByWard(@RequestParam(value = "loginid") String loginid,
            @RequestParam(value = "type", required = false) String type) {
        return tenementService.getTenementsListByWard(loginid, type);
    }

}
