package freerct.freerct;

import java.sql.*;
import java.util.*;

import org.springframework.stereotype.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.*;

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
	public String fetch(WebRequest request, @PathVariable String username) {
		try {
			ResultSet userDetails = FreeRCTApplication.sql("select id,joined,admin from users where username=?", username);
			userDetails.next();

			List<Post> allPosts = new ArrayList<>();
			ResultSet sql = FreeRCTApplication.sql("select id,topic,created from posts where user=? order by id desc", userDetails.getLong("id"));
			while (sql.next()) {
				long topic = sql.getLong("topic");

				ResultSet topicInfo = FreeRCTApplication.sql("select name,forum from topics where id=?", topic);
				topicInfo.next();
				ResultSet forumInfo = FreeRCTApplication.sql("select name from forums where id=?", topicInfo.getLong("forum"));
				forumInfo.next();

				allPosts.add(new Post(sql.getLong("id"), FreeRCTApplication.getCalendar(sql, "created"),
						topic, topicInfo.getString("name"), forumInfo.getString("name")));
			}

			String body = "<h1>User " + username + "</h1>";

			if (userDetails.getLong("admin") > 0) body += "<b><div class='forum_description_name'>Administrator</div></b>";

			body	+=	"<p class='forum_description_name'>Joined: "
					+		FreeRCTApplication.shortDatetimestring(FreeRCTApplication.getCalendar(userDetails, "joined"), request.getLocale())
					+	"</p><p class='forum_description_stats'>Posts: " + allPosts.size() + "</p>"
					;

			for (Post p : allPosts) {
				body	+=	"<div class='forum_list_entry user_post_entry'>"
						+		"<a href='/forum/post/" + p.id + "'>Post</a> on topic <a href='/forum/topic/"
						+		p.topicID + "'>" + p.topicName + "</a> <smallcaps>[" + p.forumName + "]</smallcaps>, "
						+		FreeRCTApplication.datetimestring(p.created, request.getLocale())
						+	"</div>"
						;
			}

			return FreeRCTApplication.generatePage(request, "User | " + username, body);
		} catch (SQLException e) {
			return new ErrorHandler().error(request);
		}
	}
}
