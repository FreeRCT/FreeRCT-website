package freerct.freerct;

import java.sql.*;
import java.util.*;

import javax.servlet.http.*;
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
import static freerct.freerct.FreeRCTApplication.sendEMail;

/** The login page and the password resetting page. */
@Controller
public class Login {
	@GetMapping("/login")
	@ResponseBody
	public String fetch(WebRequest request, HttpSession session,
			@RequestParam(value="next", required=false) String next,
			@RequestParam(value="type", required=false) String argument) {
		String body = """
			<a class='anchor' id='login_form'></a>
			<div class='login_form_wrapper'>
				<h1>Log In</h1>

				<form class='grid' method='post' enctype='multipart/form-data'>
					<label class='griditem'             style='grid-column:3/span 1; grid-row:1/span 1' for="username">Username:</label>
					<label class='griditem'             style='grid-column:3/span 1; grid-row:2/span 1' for="password">Password:</label>

					<input class='griditem'             style='grid-column:4/span 2; grid-row:1/span 1'
							type="text"     id="username" required name="username" autofocus>
					<input class='griditem'             style='grid-column:4/span 2; grid-row:2/span 1'
							type="password" id="password" required name="password">

					<input class='griditem form_button form_default_action' style='grid-column:4/span 1; grid-row:3/span 1'
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
			String message;
			switch (argument.toLowerCase()) {
				case "expired":
					message = "Your session has expired. Please log in again.";
					break;
				case "wrong_username":
					message = "Please enter a valid username.";
					break;
				default:
					message = "Invalid username or password.";
					break;
			}
			body += "<p class='form_error login_form_caption'>" + message + "</p>";
		}
		body += "</div>";

		return generatePage(request, session, "Log In", body);
	}

	@PostMapping("/login/forgotpassword")
	public String resetPassword(WebRequest request, @RequestPart(value="username", required=false) String username) {
		try {
			if (username == null) return "redirect:/login?type=wrong_username#login_form";

			ResultSet userDetails = sql("select email,state from users where username=?", username);
			userDetails.next();

			switch (userDetails.getInt("state")) {
				case SecurityManager.USER_STATE_NORMAL:
				case SecurityManager.USER_STATE_MODERATOR:
				case SecurityManager.USER_STATE_ADMIN:
					break;
				default:
					return "redirect:/login?type=wrong_username#login_form";
			}

			String email = userDetails.getString("email");

			final String randomToken = SecurityManager.generateRandomToken();
			Calendar tokenExpiry = Calendar.getInstance();
			tokenExpiry.roll(Calendar.DAY_OF_MONTH, 7);  // Keep the token valid for 7 days.

			sql("update users set activation_token=?, activation_expire=? where username=?",
					randomToken, new Timestamp(tokenExpiry.getTimeInMillis()), username);

			sendEMail(email, "Reset Password",
					"Dear " + username + ",\n\n"
					+ "to reset your password, please visit https://freerct.net/resetpassword and use the following token to set a new password:\n\n"
					+ randomToken
					+ "\n\nYour token remains valid for 7 days and can be used only once.\n\n"
					+ "Best regards,\n"
					+ "The FreeRCT Development Team"
				, false);

			return "redirect:/ok?type=password_reset";
		} catch (Exception e) {
			return "redirect:/login?type=wrong_username#login_form";
		}
	}
}
