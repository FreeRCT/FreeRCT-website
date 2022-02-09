package freerct.freerct;

import java.sql.*;
import java.util.*;

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

/** The login page and the password resetting page. */
@Controller
public class Login {
	@GetMapping("/login")
	@ResponseBody
	public String fetch(WebRequest request,
			@RequestParam(value="next", required=false) String next,
			@RequestParam(value="type", required=false) String argument) {
		String body = """
			<div class='login_form_wrapper'>
				<h1>Log In</h1>

				<form class='grid' method='post' enctype='multipart/form-data'>
					<label class='griditem'             style='grid-column:3/span 1; grid-row:1/span 1' for="username">Username:</label>
					<label class='griditem'             style='grid-column:3/span 1; grid-row:2/span 1' for="password">Password:</label>

					<input class='griditem'             style='grid-column:4/span 2; grid-row:1/span 1'
							type="text"     id="username" required name="username" autofocus>
					<input class='griditem'             style='grid-column:4/span 2; grid-row:2/span 1'
							type="password" id="password" required name="password">

					<input class='griditem form_button' style='grid-column:4/span 1; grid-row:3/span 1'
							type="submit" value="Log In"
			"""
			+	"formaction='/login/signin" + (next == null ? "" : ("?next=" + next)) + "'>"
			+ """
					<input class='griditem form_button' style='grid-column:4/span 1; grid-row:4/span 1'
							type="submit" value="I lost my password" formaction="/login/forgotpassword" formnovalidate>

					<div   class='griditem'             style='grid-column:6/span 3; grid-row:1/span 3'></div>
					<div   class='griditem'             style='grid-column:1/span 2; grid-row:1/span 3'></div>
				</form>
		""";

		if (argument != null) {
			boolean isGoodNews = false;
			String message;
			switch (argument.toLowerCase()) {
				case "expired":
					message = "Your session has expired. Please log in again.";
					break;
				case "password_reset":
					isGoodNews = true;
					// message = "An e-mail has been sent to your address. Please follow the link therein to reset your password.";
					message	=	"We are sorry, but the feature to reset your password has not actually been implemented yet. "
							+	"See the <a href='/contact'><i>Contact</i></a> page for information on how to contact the webmaster."
							;
					break;
				case "wrong_username":
					message = "Please enter a valid username.";
					break;
				default:
					message = "Invalid username or password.";
					break;
			}
			body += "<p class='" + (isGoodNews ? "form_ok" : "form_error") + " login_form_caption'>" + message + "</p>";
		}
		body += "</div>";

		return generatePage(request, "Log In", body);
	}

	@PostMapping("/login/forgotpassword")
	public String resetPassword(WebRequest request, @RequestPart(value="username", required=false) String username) {
		try {
			if (username == null) throw new Exception();

			ResultSet userDetails = sql("select id,email from users where username=?", username);
			userDetails.next();
			String email = userDetails.getString("email");

			// TODO actually send a password resetting e-mail...

			return "redirect:/login?type=password_reset";
		} catch (Exception e) {
			return "redirect:/login?type=wrong_username";
		}
	}
}
