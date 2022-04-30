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

/** The generic info page that something is good. */
@Controller
public class Info {
	@RequestMapping("/ok")
	@ResponseBody
	public String error(WebRequest request, HttpSession session, @RequestParam("type") String type) {
		String title, body;

		switch (type.toLowerCase()) {
			case "account_created":
				title = "Account Created";
				body	=	"An e-mail has been sent to your address. Please follow the link therein to activate your account."
						+	"</p><p>"
						+	"<strong>Please note:</strong> Some e-mail providers currently do not accept mail from <em>freerct.net</em>. "
						+	"Therefore also check your spam folder. <em>GMail</em> users may not receive a message at all; in this case, "
						+	"please contact the website administrator <a href='/contact'>as described here</a>."
						;
				break;

			case "password_reset":
				title = "Password Reset";
				body = "An e-mail has been sent to your address. Please follow the link therein to reset your password.";
				break;

			default:
				title = "Unknown Error";
				body = "An unknown error has occurred: '" + htmlEscape(type) + "'";
				break;
		}

		return generatePage(request, session, title, "<h1>" + title + "</h1><p>" + body + "</p>");
	}
}
