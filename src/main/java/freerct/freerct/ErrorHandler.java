package freerct.freerct;

import org.springframework.boot.web.servlet.error.*;
import org.springframework.stereotype.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.*;

@Controller
public class ErrorHandler implements ErrorController {
	@RequestMapping("/error")
	@ResponseBody
	public String error(WebRequest request) {
		return FreerctApplication.generatePage(request, "Not Found", """
					<h1>Not Found</h1>

					<p>
						The document you requested could not be found. Perhaps you misspelled the address or clicked on a bad link?
					</p>
		""");
	}

	public String getErrorPath() {
		return "/error";
	}
}
