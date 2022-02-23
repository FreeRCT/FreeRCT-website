package freerct.freerct;

import java.sql.*;
import java.util.*;

import javax.servlet.http.*;
import org.springframework.http.*;
import org.springframework.http.converter.*;
import org.springframework.stereotype.*;
import org.springframework.util.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.*;

import static freerct.freerct.FreeRCTApplication.generatePage;
import static freerct.freerct.FreeRCTApplication.sql;
import static freerct.freerct.FreeRCTApplication.sendEMail;
import static freerct.freerct.FreeRCTApplication.getCalendar;
import static freerct.freerct.FreeRCTApplication.htmlEscape;
import static freerct.freerct.FreeRCTApplication.renderMarkdown;
import static freerct.freerct.FreeRCTApplication.datetimestring;
import static freerct.freerct.FreeRCTApplication.shortDatetimestring;
import static freerct.freerct.FreeRCTApplication.createLinkifiedHeader;

/** The pages to manage e-mail notification subscriptions. */
@Controller
public class EditProfile {
	@GetMapping("/edit_profile")
	@ResponseBody
	public String fetch(WebRequest request, HttpSession session, @RequestParam(value="error", required=false) String argument) {
		try {
			final String username = request.getRemoteUser();
			ResultSet sql = sql("select id,email from users where username=?", username);
			sql.next();
			final long userID = sql.getLong("id");

			String body	=	"<h1>User " + username + ": Edit Profile</h1>"
						+	"<div class='forum_header_grid_toplevel'>"
						+		"<div class='griditem forum_header_grid_side_column_l'>"
						+			"<a class='form_button' href='/user/" + username + "'>Back</a>"
						+		"</div>"
						+		"<div class='griditem forum_header_grid_middle_column'>"
						+		"</div>"
						+		"<div class='griditem forum_header_grid_side_column_r'>"
						+			"<a class='form_button' href='/user/" + username + "/changeimg'>Change Profile Image</a>"
						+			"<a class='form_button' href='/user/" + username + "/changepassword'>Change Password</a>"
						+		"</div>"
						+	"</div>"
						+	"<a class='anchor' id='anchor'></a>"
						;

			if (argument != null) {
				body += "<div class='forum_description_name announcement_box'>";
				switch (argument.toLowerCase()) {
					case "invalid":
						body += "The data you entered is invalid.";
						break;
					default:
						body += "An unknown error has occurred.";
						break;
				}
				body += "</div>";
			}

			body	+=	"<form class='grid edit_profile_form' method='post'>"
					+		"<label class='griditem' style='grid-column:1/span 1; grid-row:1/span 1'  for='email'>E-Mail:</label>"
					+		"<input class='griditem' style='grid-column:2/span 2; grid-row:1/span 1' name='email' type='email' required value='"
					+			sql.getString("email") + "'>"
					+		"<h2 class='griditem' style='grid-column:1/span 3; grid-row:2/span 1'>Notification Settings</h2>"
					;

			int row = 3;
			sql = sql("select id,slug,name,description,default_enable from noticetypes order by id asc");
			while (sql.next()) {
				final String slug = "notice_" + sql.getString("slug");

				ResultSet noticeDetails = sql("select state from notification_settings where notice=? and user=?", sql.getLong("id"), userID);
				final boolean enabled = noticeDetails.next() ? (noticeDetails.getInt("state") > 0) : (sql.getInt("default_enable") > 0);

				body	+=	"<div class='griditem' style='grid-column:1/span 1; grid-row:" + row + "/span 1'>"
						+		"<label for='" + slug + "'>" + sql.getString("name") + "</label>"
						+		"<div class='label_explanation'>" + sql.getString("description") + "</div>"
						+	"</div>"
						+	"<div class='griditem' style='grid-column:2/span 1; grid-row:" + row + "/span 1'><input name='" + slug + "' type='checkbox'"
						+		(enabled ? "checked" : "") + "></div>"
						;

				++row;
			}

			body	+=		"<input class='griditem form_button form_default_action' style='grid-column:2/span 1; grid-row:" + (row++) + "/span 1'"
					+			"type='submit' value='Save Changes' formaction='/submit_edit_profile'>"
					+		"<input class='griditem form_button'                     style='grid-column:2/span 1; grid-row:" + (row++) + "/span 1'"
					+			"type='reset'  value='Reset'>"
					+	"</form>"
					;

			return generatePage(request, session, "User | " + username + " | Edit Profile", body);
		} catch (Exception e) {
			return new ErrorHandler().error(request, session, "internal_server_error");
		}
	}

	@PostMapping(value = "/submit_edit_profile", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
	public String submit(WebRequest request, HttpSession session, @RequestParam Map<String, String> requestBody) {
		try {
			final String username = request.getRemoteUser();
			if (username == null) return "redirect:/error?reason=forbidden";

			ResultSet sql = sql("select id from users where username=?", username);
			sql.next();
			final long userID = sql.getLong("id");

			String email = requestBody.get("email");
			if (email != null) {
				session.setAttribute("freerct-email", email);
				sql("update users set email=? where id=?", email, userID);
			}

			sql("delete from notification_settings where user=?", userID);
			sql = sql("select id,slug from noticetypes");
			while (sql.next()) {
				String value = requestBody.get("notice_" + sql.getString("slug"));
				sql("insert into notification_settings (user,notice,state) value (?,?,?)", userID, sql.getLong("id"),
						(value != null && (value.equalsIgnoreCase("on") || value.equals("1") || value.equalsIgnoreCase("true"))) ? 1 : 0);
			}

			return "redirect:/user/" + username + "?type=profile_updated";
		} catch (Exception e) {
			return "redirect:/error?reason=internal_server_error";
		}
	}
}
