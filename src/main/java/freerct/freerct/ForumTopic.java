package freerct.freerct;

import java.sql.*;
import java.util.*;

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

/** The page that displays a single forum topic. */
@Controller
public class ForumTopic {
	private static class Post {
		public final long id, authorID, editorID;
		public final String author, editor, body;
		public final Calendar created, edited;
		public Post(long i, long a, String auth, long e, String edit, Calendar timeFirst, Calendar timeLast, String b) {
			id = i;
			authorID = a;
			editorID = e;
			author = auth;
			editor = edit;
			created = timeFirst;
			edited = timeLast;
			body = b;
		}
	}

	@GetMapping("/forum/post/{postID}")
	public String permalink(WebRequest request, @PathVariable long postID) {
		try {
			ResultSet sql = sql("select topic from posts where id=?", postID);
			sql.next();
			return "redirect:/forum/topic/" + sql.getLong("topic") + "#post_" + postID;
		} catch (Exception e) {
			return "redirect:/error";
		}
	}

	@GetMapping("/forum/topic/{topicID}")
	@ResponseBody
	public String fetch(WebRequest request, @PathVariable long topicID) {
		try {
			ResultSet sql = sql("select name,forum from topics where id=?", topicID);
			sql.next();
			final String topicName = sql.getString("name");
			final long forumID = sql.getLong("forum");

			sql = sql("select name from forums where id=?", forumID);
			sql.next();
			final String forumName = sql.getString("name");

			List<Post> allPosts = new ArrayList<>();
			sql = sql("select id,user,editor,created,edited,body from posts where topic=? order by id asc", topicID);
			while (sql.next()) {
				ResultSet author = sql("select username from users where id=?", sql.getLong("user"));
				author.next();
				String authorName = author.getString("username");

				author = sql("select username from users where id=?", sql.getLong("editor"));
				String editorName = author.next() ? author.getString("username") : null;

				Calendar calendarFirst = getCalendar(sql, "created");
				Calendar calendarLast = getCalendar(sql, "edited");
				allPosts.add(new Post(sql.getLong("id"), sql.getLong("user"), htmlEscape(authorName), sql.getLong("editor"), htmlEscape(editorName),
						calendarFirst, calendarLast, renderMarkdown(sql.getString("body"))));
			}

			String body	=	"<h1>Topic: " + htmlEscape(topicName) + "</h1>"
						+	"<p class='forum_description_name'>Forum: <a href='/forum/" + forumID + "'>" + htmlEscape(forumName) + "</a></p>"
						+	"<p class='forum_description_stats'>" + pluralForm(allPosts.size(), "post", "posts") + "</p>"
						;

			for (Post p : allPosts) {
				ResultSet postCounter = sql("select count(user) as nr from posts where user=?", p.authorID);
				postCounter.next();
				ResultSet userDetails = sql("select joined,state from users where id=?", p.authorID);
				userDetails.next();

				body	+=	"<div class='forum_list_entry' id='post_" + p.id + "'>"
						+		"<div class='forum_post_usercolumn'>"
						+			"<div><a href='/user/" + p.author + "'>" + p.author + "</a></div>"
						;

				switch (userDetails.getInt("state")) {
					case SecurityManager.USER_STATE_ADMIN:
						body += "<div class='forum_post_userdetails'><b>Administrator</b></div>";
						break;
					case SecurityManager.USER_STATE_MODERATOR:
						body += "<div class='forum_post_userdetails'><b>Moderator</b></div>";
						break;
					default:
						break;
				}

				body	+=			"<div class='forum_post_userdetails'>Posts: <b>" + postCounter.getLong("nr") + "</b></div>"
						+			"<div class='forum_post_userdetails'>Joined: "
						+				shortDatetimestring(getCalendar(userDetails, "joined"), request.getLocale())
						+			"</div>"
						+		"</div>"
						+		"<div class='forum_post_wrapper'>"
						+			"<div class='forum_post_meta'>"
						+				"<div>"
						+					datetimestring(p.created, request.getLocale())
						+				"</div>"
						;

				if (p.edited != null) {
					body += "<div>";
					if (p.authorID == p.editorID) {
						body += "Edited on ";
					} else {
						body += "Edited by <a href='/user/" + p.editor + "'>" + p.editor + "</a> on ";
					}
					body += datetimestring(p.edited, request.getLocale()) + "</div>";
				}

				body += "</div><div class='forum_post_body'>" + p.body + "</div></div></div>";
			}

			if (request.getUserPrincipal() != null) {
				body += generateForumPostForm(false, "New Post", "/forum/topic/" + topicID + "/submit_new");
			}

			return generatePage(request, "Forum | " + forumName + " | " + topicName, body);
		} catch (Exception e) {
			return new ErrorHandler().error(request);
		}
	}

	@PostMapping("/forum/topic/{topicID}/submit_new")
	public String createPost(WebRequest request,
			@PathVariable long topicID,
			@RequestPart("content") String content) {
		try {
			ResultSet sql = sql("select id from users where username=?", request.getRemoteUser());
			sql.next();
			long userID = sql.getLong("id");

			synchronized (sqlSync()) {
				sql("insert into posts (topic,user,body) value(?,?,?)", topicID, userID, content);
				sql = sql("select last_insert_id() as new_id");
			}
			sql.next();
			long postID = sql.getLong("new_id");

			return "redirect:/forum/post/" + postID;
		} catch (Exception e) {
			return "redirect:/error";
		}
	}
}
