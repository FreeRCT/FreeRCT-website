package freerct.freerct;

import java.sql.*;
import java.util.*;

import javax.servlet.http.*;
import org.springframework.security.authentication.*;
import org.springframework.security.core.*;
import org.springframework.security.core.context.*;
import org.springframework.security.web.authentication.*;
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

/** The registering page. */
@Controller
public class Register {
	private static final String USERNAME_REGEX = "[-._+A-Za-z0-9]+";
	private static final int USERNAME_MAX_LENGTH = 40;  // Arbitrary limit, we could easily handle up to 254 characters.

	@GetMapping("/signup")
	@ResponseBody
	public String fetch(WebRequest request, HttpSession session, @RequestParam(value="type", required=false) String argument) {
		String body = """
			<a class='anchor' id='signup_form'></a>
			<div class='login_form_wrapper'>
				<h1>Register</h1>

				<form class='grid' method='post' enctype='multipart/form-data'>
					<label class='griditem'             style='grid-column:3/span 1; grid-row:1/span 1' for="username" >Username:</label>
					<label class='griditem'             style='grid-column:3/span 1; grid-row:2/span 1' for="email"    >E-Mail:</label>
					<label class='griditem'             style='grid-column:3/span 1; grid-row:3/span 1' for="password" >Password:</label>
					<label class='griditem'             style='grid-column:3/span 1; grid-row:4/span 1' for="password2">Repeat Password:</label>

					<input class='griditem'             style='grid-column:4/span 2; grid-row:1/span 1'
							type="text"     id="username"  required name="username">
					<input class='griditem'             style='grid-column:4/span 2; grid-row:2/span 1'
							type="email"    id="email"     required name="email">
					<input class='griditem'             style='grid-column:4/span 2; grid-row:3/span 1'
							type="password" id="password"  required name="password">
					<input class='griditem'             style='grid-column:4/span 2; grid-row:4/span 1'
							type="password" id="password2" required name="password2">

					<input class='griditem form_button form_default_action' style='grid-column:4/span 1; grid-row:5/span 1'
							type="submit" value="Create Account" formaction="/signup/complete">

					<div   class='griditem'             style='grid-column:6/span 4; grid-row:1/span 3'></div>
					<div   class='griditem'             style='grid-column:1/span 2; grid-row:1/span 3'></div>
				</form>
		""";

		if (argument != null) {
			body += "<p class='form_error login_form_caption'>";
			switch (argument.toLowerCase()) {
				case "passwords":
					body += "The passwords don't match.";
					break;
				case "name_taken":
					body += "This username is already in use.";
					break;
				case "name_invalid":
					body	+=	"Usernames may contain only Latin characters, digits, and the special characters <tt>.-_+</tt> "
							+	"and may not be longer than " + USERNAME_MAX_LENGTH + " characters."
							;
					break;
				default:
					body	+=	"An unknown error has occurred. If you are repeatedly unable to create an account, see the "
							+	"<a href='/contact'><i>Contact</i></a> page for information on how to contact the webmaster."
							;
					break;
			}
			body += "</p>";
		}
		body += "</div>";

		return generatePage(request, session, "Register", body);
	}

	@PostMapping("/signup/complete")
	public String newUser(HttpServletRequest request,
			@RequestPart("username") String username,
			@RequestPart("email") String email,
			@RequestPart("password") String password,
			@RequestPart("password2") String password2) {
		try {
			// We do not save the values in the Session here. The user should enter his credentials afresh on each try.

			if (username.length() > USERNAME_MAX_LENGTH || !username.matches(USERNAME_REGEX)) return "redirect:/signup?type=name_invalid#signup_form";

			ResultSet userDetails = sql("select id from users where username=?", username);
			if (!password.equals(password2)) return "redirect:/signup?type=passwords#signup_form";
			if (userDetails.next()) return "redirect:/signup?type=name_taken#signup_form";

			final String randomToken = SecurityManager.generateRandomToken();
			Calendar tokenExpiry = Calendar.getInstance();
			tokenExpiry.roll(Calendar.DAY_OF_MONTH, 7);  // Keep the token valid for 7 days.

			// TODO run a scheduled job that frequently purges stale tokens from the database

			sql("insert into users (username,email,password,activation_token,activation_expire) value (?,?,?,?,?)",
					username, email, SecurityManager.passwordEncoder().encode​(password), randomToken, new Timestamp(tokenExpiry.getTimeInMillis()));

			sendEMail(email, "Activate Account",
					"Dear " + username + ",\n\n"
					+ "to activate your new FreeRCT account, please click on the following link:\n\n"
					+ "https://freerct.net/signup/" + randomToken
					+ "\n\nThis link remains valid for 7 days.\n\n"
					+ "If you did not request the creation of an account on freerct.net, please ignore this e-mail.\n\n"
					+ "Best regards,\n"
					+ "The FreeRCT Development Team"
				, false);

			return "redirect:/ok?type=account_created";
		} catch (Exception e) {
			return "redirect:/signup?type=error#signup_form";
		}
	}

	@GetMapping("/signup/{token}")
	public String activateAccount(HttpServletRequest request, @PathVariable String token) {
		try {
			ResultSet sql = sql("select id,username,activation_expire,password from users where state=? and activation_token=?",
					SecurityManager.USER_STATE_AWAITING, token);
			sql.next();

			final long userID = sql.getLong("id");

			if (getCalendar(sql, "activation_expire").getTimeInMillis() < Calendar.getInstance().getTimeInMillis()) {
				sql("delete from users where id=?", userID);
				return "redirect:/error?reason=expired";
			}

			final String username = sql.getString("username");
			final String originalPasswordHash = sql.getString("password");

			// Generate a temporary password to log in the user
			final String tempPassword = SecurityManager.generateRandomToken();
			sql("update users set password=?, state=? where id=?",
					SecurityManager.passwordEncoder().encode​(tempPassword), SecurityManager.USER_STATE_NORMAL, userID);

			try {
				request.login(username, tempPassword);
			} catch (Exception e) {
				sql("update users set password=?, state=? where id=?", originalPasswordHash, SecurityManager.USER_STATE_AWAITING, userID);
				return "redirect:/error";
			}

			sql("update users set activation_token=null, activation_expire=null, password=? where id=?", originalPasswordHash, userID);

			return "redirect:/user/" + username + "?type=new_user";
		} catch (Exception e) {
			return "redirect:/error";
		}
	}
}
