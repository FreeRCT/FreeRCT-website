package freerct.freerct;

import java.io.*;
import java.sql.*;
import java.util.*;

import javax.servlet.http.*;
import org.springframework.stereotype.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.*;

import static freerct.freerct.FreeRCTApplication.generatePage;
import static freerct.freerct.FreeRCTApplication.sql;
import static freerct.freerct.FreeRCTApplication.sqlSync;
import static freerct.freerct.FreeRCTApplication.getCalendar;
import static freerct.freerct.FreeRCTApplication.htmlEscape;
import static freerct.freerct.FreeRCTApplication.renderMarkdown;
import static freerct.freerct.FreeRCTApplication.datetimestring;
import static freerct.freerct.FreeRCTApplication.shortDatetimestring;
import static freerct.freerct.FreeRCTApplication.pluralForm;
import static freerct.freerct.FreeRCTApplication.createLinkifiedHeader;
import static freerct.freerct.FreeRCTApplication.generateForumPostForm;

/** The form to change or delete a user's profile image. */
@Controller
public class UserProfileImage {
	@GetMapping("/user/{username}/changeimg")
	@ResponseBody
	public String deleteTopic(WebRequest request, HttpSession session, @PathVariable String username, @RequestParam(value="error", required=false) String error) {
		try {
			if (!username.equals(request.getRemoteUser())) return new ErrorHandler().error(request, session, "forbidden");

			String body = "<h1>User " + username + "</h1>";

			/* ResultSet sql = sql("select forum,name from topics where id=?", topicID);
			sql.next();
			final long forumID = sql.getLong("forum");
			final String topicName = sql.getString("name");

			sql = sql("select name from forums where id=?", forumID);
			sql.next();
			final String forumName = sql.getString("name");

			String body	=	"<h1>Topic: " + htmlEscape(topicName) + ": delete Topic</h1>"
						+	"<p class='forum_description_name'>Forum: <a href='/forum/" + forumID + "'>" + htmlEscape(forumName) + "</a></p>"

						+	"<form><div class='forum_back_button_wrapper'>"
						+		"<input class='form_button' type='submit' value='Back' formaction='/forum/topic/" + topicID + "'>"
						+	"</div></form>"

						+	generateForumPostForm(true, "Delete Topic", null, topicName, "/forum/topic/submit_delete/" + topicID, error, true);
						; */

			return generatePage(request, session, "User | " + username + " | Change Profile Image", body);
		} catch (Exception e) {
			return new ErrorHandler().error(request, session, "internal_server_error");
		}
	}

	@PostMapping("/user/{username}/clearimg")
	public String clearImage(WebRequest request, @PathVariable String username) {
		try {
			if (!username.equals(request.getRemoteUser()) && !SecurityManager.isAdmin(request)) return "redirect:/error?reason=forbidden";

			new File("src/main/resources/static/img/users/" + username + ".png").delete();

			return "redirect:/user/" + username;
		} catch (Exception e) {
			return "redirect:/error?reason=internal_server_error";
		}
	}
}
