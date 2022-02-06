package freerct.freerct;

import java.sql.*;
import java.util.*;

import org.springframework.stereotype.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.*;

@Controller
public class ForumTopic {
	private static class Post {
		public final long id;
		public final String author, editor, body;
		public final Calendar created, edited;
		public Post(long i, String auth, String edit, Calendar timeFirst, Calendar timeLast, String b) {
			id = i;
			author = auth;
			editor = edit;
			created = timeFirst;
			edited = timeLast;
			body = b;
		}
	}

	@GetMapping("/forum/topic/{topicID}")
	@ResponseBody
	public String fetch(WebRequest request, @PathVariable long topicID) {
		try {
			ResultSet sql = FreeRCTApplication.sql("select name,forum from topics where id=?", topicID);
			sql.next();
			final String topicName = sql.getString("name");
			final long forumID = sql.getLong("forum");

			sql = FreeRCTApplication.sql("select name from forums where id=?", forumID);
			sql.next();
			final String forumName = sql.getString("name");

			List<Post> allPosts = new ArrayList<>();
			sql = FreeRCTApplication.sql("select id,user,editor,created,edited,body from posts where topic=? order by id asc", topicID);
			while (sql.next()) {
				ResultSet author = FreeRCTApplication.sql("select username from users where id=?", sql.getLong("user"));
				author.next();
				String authorName = author.getString("username");

				author = FreeRCTApplication.sql("select username from users where id=?", sql.getLong("editor"));
				String editorName = author.next() ? author.getString("username") : null;

				Calendar calendarFirst = Calendar.getInstance();
				calendarFirst.setTime(sql.getTimestamp("created"));

				Calendar calendarLast = null;
				Timestamp edited = sql.getTimestamp("edited");
				if (edited != null) {
					calendarLast = Calendar.getInstance();
					calendarLast.setTime(edited);
				}

				allPosts.add(new Post(sql.getLong("id"), authorName, editorName, calendarFirst, calendarLast, sql.getString("body")));
			}

			String body	=	"<h1>Topic: " + topicName + "</h1>"
						+	"<p class='forum_description_name'>Forum: <a href='/forum/" + forumID + "'>" + forumName + "</a></p>"
						+	"<p class='forum_description_stats'>" + allPosts.size() + " posts</p>"
						;

			for (Post p : allPosts) {
				body	+=	"<div class='forum_list_entry'>"
						+		"<div>"
						+			"<div><a href='/user/" + p.author + "'>" + p.author + "</a></div>"
						+		"</div>"
						+		"<div class='forum_post_wrapper'>"
						+			"<div class='forum_post_meta'>" + FreeRCTApplication.datetimestring(p.created, request.getLocale()) + "</div>"
						+			"<div class='forum_post_body'>" + p.body + "</div>"
						;

				if (p.edited != null) {
					body += "<div class='forum_post_meta'>";
					if (p.author.equals(p.editor)) {
						body += "Edited on ";
					} else {
						body += "Edited by <a href='/user/" + p.editor + "'>" + p.editor + "</a> on ";
					}
					body += FreeRCTApplication.datetimestring(p.created, request.getLocale()) + "</div>";
				}

				body += "</div></div>";
			}

			return FreeRCTApplication.generatePage(request, "Forum | " + forumName + " | " + topicName, body);
		} catch (Exception e) {
			return new ErrorHandler().error(request);
		}
	}
}
