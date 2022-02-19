package freerct.freerct;

import javax.servlet.http.*;
import org.springframework.boot.web.servlet.error.*;
import org.springframework.stereotype.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.*;

import static freerct.freerct.FreeRCTApplication.generatePage;
import static freerct.freerct.FreeRCTApplication.sql;
import static freerct.freerct.FreeRCTApplication.getCalendar;
import static freerct.freerct.FreeRCTApplication.htmlEscape;
import static freerct.freerct.FreeRCTApplication.renderMarkdown;
import static freerct.freerct.FreeRCTApplication.datetimestring;
import static freerct.freerct.FreeRCTApplication.shortDatetimestring;
import static freerct.freerct.FreeRCTApplication.createLinkifiedHeader;

/** The generic Page Not Found error page. */
@Controller
public class ErrorHandler implements ErrorController {
	@RequestMapping("/error")
	@ResponseBody
	public String error(WebRequest request, HttpSession session, @RequestParam(value="reason", required=false) String reason) {
		String title = "Not Found";
		String body = "The document you requested could not be found. Perhaps you misspelled the address or clicked on a bad link?";

		if (reason != null) {
			switch (reason.toLowerCase()) {
				case "internal_server_error":
					title = "Internal Server Error";
					body = "An internal server error has occurred.";
					break;

				case "forbidden":
					title = "Forbidden";
					body = "You do not have sufficient privileges to do this.";
					break;

				case "expired":
					title = "Expired Token";
					body = "This token has already expired.";
					break;

				default:
					title = "Unknown Error";
					body = "An unknown error has occurred: '" + htmlEscape(reason) + "'";
					break;
			}
		}

		return generatePage(request, session, title, "<h1>" + title + "</h1><p>" + body + "</p>");
	}

	public String getErrorPath() {
		return "/error";
	}
}
