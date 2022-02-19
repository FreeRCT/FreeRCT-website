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

/** The password changing page. */
@Controller
public class ChangePassword {
	private String doCreatePage(WebRequest request, HttpSession session, String username, String argument, boolean useToken) {
		String title = useToken ? "Reset Password" : "Change Password";
		String body = """
			<a class='anchor' id='password_form'></a>
			<div class='login_form_wrapper'>
		"""
		+		"<h1>" + title + "</h1>"
		+ """

				<form class='grid' method='post' enctype='multipart/form-data'>
					<label class='griditem'             style='grid-column:3/span 1; grid-row:1/span 1' for="username"   >Username:</label>
					<label class='griditem'             style='grid-column:3/span 1; grid-row:2/span 1' for="oldpassword"
		"""
		+			">" + (useToken ? "Token:" : "Old Password:") + "</label>"
		+ """
					<label class='griditem'             style='grid-column:3/span 1; grid-row:3/span 1' for="password"   >New Password:</label>
					<label class='griditem'             style='grid-column:3/span 1; grid-row:4/span 1' for="password2"  >Repeat Password:</label>

					<input class='griditem'             style='grid-column:4/span 2; grid-row:1/span 1' type="text" name="username" id="username"
			"""
			+	 (useToken ? "autofocus" : ("readonly value='" + username + "'"))
			+ """
					>
					<input class='griditem'             style='grid-column:4/span 2; grid-row:2/span 1' type="password" id="oldpassword" required name="oldpassword"
			"""
			+		(useToken ? "" : "autofocus")
			+ """
					>
					<input class='griditem'             style='grid-column:4/span 2; grid-row:3/span 1'
							type="password" id="password"    required name="password">
					<input class='griditem'             style='grid-column:4/span 2; grid-row:4/span 1'
							type="password" id="password2"   required name="password2">

					<input class='griditem form_button form_default_action' style='grid-column:4/span 1; grid-row:5/span 1'
			"""
			+				"type='submit' value='" + title
			+				"' formaction='" + (useToken ? "/submit_resetpassword" : ("/user/" + username + "/submit_changepassword")) + "'>"
			+ """

					<div   class='griditem'             style='grid-column:6/span 3; grid-row:1/span 3'></div>
					<div   class='griditem'             style='grid-column:1/span 2; grid-row:1/span 3'></div>
				</form>
		""";

		if (argument != null) {
			String message;
			switch (argument.toLowerCase()) {
				case "mismatch":
					message = "The passwords don't match.";
					break;
				case "wronguser":
					message = "Please enter a valid username.";
					break;
				case "wrongpassword":
					message = "Invalid old password.";
					break;
				case "wrongtoken":
					message = "Invalid token.";
					break;
				default:
					message = "An unknown error has occurred.";
					break;
			}
			body += "<p class='form_error login_form_caption'>" + message + "</p>";
		}
		body += "</div>";

		return generatePage(request, session, useToken ? "Reset Password" : ("User | " + username + " | Change Password"), body);
	}

	@GetMapping("/user/{username}/changepassword")
	@ResponseBody
	public String fetchChange(WebRequest request, HttpSession session,
			@PathVariable String username,
			@RequestParam(value="type", required=false) String argument) {
		return doCreatePage(request, session, username, argument, false);
	}

	@GetMapping("/resetpassword")
	@ResponseBody
	public String fetchReset(WebRequest request, HttpSession session, @RequestParam(value="type", required=false) String argument) {
		return doCreatePage(request, session, null, argument, true);
	}

	@PostMapping("/user/{username}/submit_changepassword")
	public String changePassword(WebRequest request,
			@PathVariable String username,
			@RequestPart("oldpassword") String oldpassword,
			@RequestPart("password") String password,
			@RequestPart("password2") String password2) {
		try {
			// We do not save the values in the Session here. The user should enter his credentials afresh on each try.

			if (!username.equals(request.getRemoteUser()) && !SecurityManager.isAdmin(request)) return "redirect:/error?reason=forbidden";

			if (!password.equals(password2)) {
				return "redirect:/user/" + username + "/changepassword?type=mismatch#password_form";
			}

			ResultSet userDetails = sql("select password from users where username=?", username);
			userDetails.next();
			if (!SecurityManager.passwordEncoder().matches(oldpassword, userDetails.getString("password"))) {
				return "redirect:/user/" + username + "/changepassword?type=wrongpassword#password_form";
			}

			sql("update users set password=? where username=?", SecurityManager.passwordEncoder().encode​(password), username);

			return "redirect:/user/" + username + "?type=password_changed";
		} catch (Exception e) {
			return "redirect:/error?reason=internal_server_error";
		}
	}

	@PostMapping("/submit_resetpassword")
	public String resetPassword(HttpServletRequest request,
			@RequestPart("username") String username,
			@RequestPart("oldpassword") String token,
			@RequestPart("password") String password,
			@RequestPart("password2") String password2) {
		try {
			if (!password.equals(password2)) {
				return "redirect:/resetpassword?type=mismatch#password_form";
			}

			ResultSet userDetails = sql("select state,activation_token,activation_expire from users where username=?", username);
			if (!userDetails.next()) return "redirect:/resetpassword?type=wronguser#password_form";

			switch (userDetails.getInt("state")) {
				case SecurityManager.USER_STATE_NORMAL:
				case SecurityManager.USER_STATE_MODERATOR:
				case SecurityManager.USER_STATE_ADMIN:
					break;
				default:
					return "redirect:/resetpassword?type=wronguser#password_form";
			}

			if (!token.equals(userDetails.getString("activation_token"))) return "redirect:/resetpassword?type=wrongtoken#password_form";

			if (getCalendar(userDetails, "activation_expire").getTimeInMillis() < Calendar.getInstance().getTimeInMillis()) {
				return "redirect:/error?reason=expired";
			}

			sql("update users set password=?, activation_token=null, activation_expire=null where username=?",
					SecurityManager.passwordEncoder().encode​(password), username);

			request.login(username, password);
			return "redirect:/user/" + username + "?type=password_changed";
		} catch (Exception e) {
			return "redirect:/error?reason=internal_server_error";
		}
	}
}
