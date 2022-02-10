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

/** The user profile page. */
@Controller
public class UserProfile {
	private static class Post {
		public final long id, topicID;
		public final String topicName, forumName;
		public final Calendar created;
		public Post(long i, Calendar c, long t, String tn, String fn) {
			id = i;
			created = c;
			topicID = t;
			topicName = tn;
			forumName = fn;
		}
	}

	@GetMapping("/user/{username}")
	@ResponseBody
	public String fetch(WebRequest request, HttpSession session, @PathVariable String username, @RequestParam(value="type", required=false) String argument) {
		try {
			ResultSet userDetails = sql("select id,joined,state from users where username=?", username);
			userDetails.next();

			List<Post> allPosts = new ArrayList<>();
			ResultSet sql = sql("select id,topic,created from posts where user=? order by id desc", userDetails.getLong("id"));
			while (sql.next()) {
				long topic = sql.getLong("topic");

				ResultSet topicInfo = sql("select name,forum from topics where id=?", topic);
				topicInfo.next();
				ResultSet forumInfo = sql("select name from forums where id=?", topicInfo.getLong("forum"));
				forumInfo.next();

				allPosts.add(new Post(sql.getLong("id"), getCalendar(sql, "created"),
						topic, topicInfo.getString("name"), forumInfo.getString("name")));
			}

			final boolean isSelf = username.equals(request.getRemoteUser());

			String body = "<h1>User " + htmlEscape(username) + "</h1>";

			if (argument != null) {
				body += "<div class='forum_description_name announcement_box'>";
				switch (argument.toLowerCase()) {
					case "new_user":
						body += "Welcome! Your account was created successfully.";
						break;
					case "password_changed":
						body += isSelf ? "Your password was changed successfully." : "The user's password was changed successfully.";
						break;
					default:
						body += "An unknown error has occurred.";
						break;
				}
				body += "</div>";
			}

			switch (userDetails.getInt("state")) {
				case SecurityManager.USER_STATE_ADMIN:
					body += "<b><div class='forum_description_name'>Administrator</div></b>";
					break;
				case SecurityManager.USER_STATE_MODERATOR:
					body += "<b><div class='forum_description_name'>Moderator</div></b>";
					break;
				default:
					break;
			}

			body	+=	"<p class='forum_description_name'>Joined: "
					+		shortDatetimestring(getCalendar(userDetails, "joined"), request.getLocale())
					+	"</p><p class='forum_description_stats'>Posts: " + allPosts.size() + "</p>"
					;

			if (isSelf || SecurityManager.isAdmin(request)) {
				body	+=	"<form><div class='forum_new_topic_button_wrapper'>"
						+		"<input class='form_button' type='submit' value='Change Password' formaction='/user/" + username + "/changepassword'>"
						;

				if (isSelf) {
					body	+=	"<input class='form_button' type='submit' value='Messages'        formaction='/inbox'>";
				}

				body += "</div></form>";
			}

			for (Post p : allPosts) {
				body	+=	"<div class='forum_list_entry user_post_entry'>"
						+		"<a href='/forum/post/" + p.id + "'>Post</a> on topic <a href='/forum/topic/"
						+		p.topicID + "'>" + htmlEscape(p.topicName) + "</a> <smallcaps>["
						+		htmlEscape(p.forumName) + "]</smallcaps>, "
						+		datetimestring(p.created, request.getLocale())
						+	"</div>"
						;
			}

			return generatePage(request, session, "User | " + htmlEscape(username), body);
		} catch (SQLException e) {
			return new ErrorHandler().error(request, session, "internal_server_error");
		}
	}
}
