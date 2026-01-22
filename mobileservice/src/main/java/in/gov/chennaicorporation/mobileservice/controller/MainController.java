package in.gov.chennaicorporation.mobileservice.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import in.gov.chennaicorporation.mobileservice.service.MainService;
import in.gov.chennaicorporation.mobileservice.service.OfficialAPIService;

@RequestMapping("/gccofficialapp")
@Controller("mainController")
public class MainController {

	@Autowired
    private MainService mainService;
    private final Environment environment;

    @Autowired
    public MainController(Environment environment) {
        this.environment = environment;
    }
	
	
	@GetMapping({"", "/", "/index"})
	public String main(Model model) {
		String assetsBaseUrl = environment.getProperty("assets.base-url");
		model.addAttribute("assetsBaseUrl", assetsBaseUrl);
		return "error";
	}
	
	@GetMapping({"/support"})
	public String support(Model model) {
		String assetsBaseUrl = environment.getProperty("assets.base-url");
		model.addAttribute("assetsBaseUrl", assetsBaseUrl);
		return "modules/gccofficialapp/support";
	}
	
	@GetMapping({"/policy"})
	public String main2(Model model) {
		String assetsBaseUrl = environment.getProperty("assets.base-url");
		model.addAttribute("assetsBaseUrl", assetsBaseUrl);
		return "modules/gccofficialapp/policy";
	}
}
