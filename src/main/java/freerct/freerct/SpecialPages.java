package freerct.freerct;

import javax.servlet.http.*;
import org.springframework.boot.web.servlet.error.*;
import org.springframework.stereotype.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.*;

/** Miscellaneous special pages. */
@Controller
public class SpecialPages {
	@GetMapping("/.well-known/org.flathub.VerifiedApps.txt")
	@ResponseBody
	public String fetchFlathubVerification(WebRequest request, HttpSession session) {
		return "64df6f93-3702-428b-a35f-53d52f36d03a";
	}
}
