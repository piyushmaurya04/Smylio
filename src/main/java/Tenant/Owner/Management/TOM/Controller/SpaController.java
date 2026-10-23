package Tenant.Owner.Management.TOM.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaController {

    @GetMapping({ "/", "/login", "/register", "/profile", "/help", "/owner/{*path}", "/tenant/{*path}" })
    public String forwardToFrontend() {
        return "forward:/index.html";
    }
}
