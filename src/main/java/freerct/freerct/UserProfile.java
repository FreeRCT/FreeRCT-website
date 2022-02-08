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

/** The user profile page. */
@Controller
public class UserProfile {
	/* These constants are stored in the database, DO NOT CHANGE THEM. */
	public static final int USER_STATE_NORMAL      = 0;  ///< State constant for normal users.
	public static final int USER_STATE_ADMIN       = 1;  ///< State constant for administrators.
	public static final int USER_STATE_MODERATOR   = 2;  ///< State constant for moderators.
	public static final int USER_STATE_DEACTIVATED = 3;  ///< State constant for deactivated accounts.

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
	public String fetch(WebRequest request, @PathVariable String username, @RequestParam(value="new_user", required=false) boolean newUser) {
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

			String body = "<h1>User " + htmlEscape(username) + "</h1>";

			if (newUser) {
				body += "<div class='forum_description_name announcement_box'>Welcome! Your account was created successfully.</div>";
			}

			switch (userDetails.getInt("state")) {
				case USER_STATE_ADMIN:
					body += "<b><div class='forum_description_name'>Administrator</div></b>";
					break;
				case USER_STATE_MODERATOR:
					body += "<b><div class='forum_description_name'>Moderator</div></b>";
					break;
				default:
					break;
			}

			body	+=	"<p class='forum_description_name'>Joined: "
					+		shortDatetimestring(getCalendar(userDetails, "joined"), request.getLocale())
					+	"</p><p class='forum_description_stats'>Posts: " + allPosts.size() + "</p>"
					;

			for (Post p : allPosts) {
				body	+=	"<div class='forum_list_entry user_post_entry'>"
						+		"<a href='/forum/post/" + p.id + "'>Post</a> on topic <a href='/forum/topic/"
						+		p.topicID + "'>" + renderMarkdown(p.topicName, true) + "</a> <smallcaps>["
						+		renderMarkdown(p.forumName, true) + "]</smallcaps>, "
						+		datetimestring(p.created, request.getLocale())
						+	"</div>"
						;
			}

			return generatePage(request, "User | " + htmlEscape(username), body);
		} catch (SQLException e) {
			return new ErrorHandler().error(request);
		}
	}
}
