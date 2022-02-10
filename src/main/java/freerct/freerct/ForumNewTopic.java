package freerct.freerct;

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

/** The form to create a new topic in a forum. */
@Controller
public class ForumNewTopic {
	@PostMapping("/render_markdown")
	@ResponseBody
	public String getMarkdownPreview(@RequestBody String content) {
		return renderMarkdown(content);
	}

	@GetMapping("/forum/{forumID}/new")
	@ResponseBody
	public String newTopic(WebRequest request, @PathVariable long forumID, @RequestParam(value="error", required=false) String error) {
		try {
			ResultSet sql = sql("select name,description from forums where id=?", forumID);
			sql.next();
			final String forumName = sql.getString("name");
			final String forumDescription = sql.getString("description");

			String body	=	"<h1>Forum: " + htmlEscape(forumName) + ": New Topic</h1>"
						+	"<p class='forum_description_name'>" + htmlEscape(forumDescription) + "</p>"

						+	"<form><div class='forum_back_button_wrapper'>"
						+		"<input class='form_button' type='submit' value='Back' formaction='/forum/" + forumID + "'>"
						+	"</div></form>"

						+	generateForumPostForm(false, "Subject", "Post", "", "/forum/" + forumID + "/submit_new", error, false);

			return generatePage(request, "Forum | " + forumName + " | New Topic", body);
		} catch (Exception e) {
			return new ErrorHandler().error(request, "internal_server_error");
		}
	}

	@PostMapping("/forum/{forumID}/submit_new")
	public String createTopic(WebRequest request, HttpSession session,
			@PathVariable long forumID,
			@RequestPart("subject") String subject,
			@RequestPart("content") String content) {
		try {
			session.setAttribute("freerct-new-topic-subject", subject);
			session.setAttribute("freerct-new-topic-content", content);

			subject = subject.trim();
			if (subject.isEmpty()) return "redirect:/forum/" + forumID + "/new?error=empty_title#post_form";
			content = content.trim();
			if (content.isEmpty()) return "redirect:/forum/" + forumID + "/new?error=empty_post#post_form";

			ResultSet sql = sql("select id from users where username=?", request.getRemoteUser());
			sql.next();
			long userID = sql.getLong("id");

			synchronized (sqlSync()) {
				sql("insert into topics (forum,name) value(?,?)", forumID, subject);
				sql = sql("select last_insert_id() as new_id");
			}
			sql.next();
			long topicID = sql.getLong("new_id");

			sql("insert into posts (topic,user,body) value(?,?,?)", topicID, userID, content);

			session.removeAttribute("freerct-new-topic-subject");
			session.removeAttribute("freerct-new-topic-content");
			return "redirect:/forum/topic/" + topicID;
		} catch (Exception e) {
			return "redirect:/error?reason=internal_server_error";
		}
	}

	@GetMapping("/forum/post/edit/{postID}")
	@ResponseBody
	public String editPost(WebRequest request, @PathVariable long postID, @RequestParam(value="error", required=false) String error) {
		try {
			ResultSet sql = sql("select topic,body from posts where id=?", postID);
			sql.next();
			final long topicID = sql.getLong("topic");
			final String content = sql.getString("body");

			sql = sql("select forum,name from topics where id=?", topicID);
			sql.next();
			final long forumID = sql.getLong("forum");
			final String topicName = sql.getString("name");

			sql = sql("select name from forums where id=?", forumID);
			sql.next();
			final String forumName = sql.getString("name");

			String body	=	"<h1>Topic: " + htmlEscape(topicName) + ": Edit Post</h1>"
						+	"<p class='forum_description_name'>Forum: <a href='/forum/" + forumID + "'>" + htmlEscape(forumName) + "</a></p>"

						+	"<form><div class='forum_back_button_wrapper'>"
						+		"<input class='form_button' type='submit' value='Back' formaction='/forum/post/" + postID + "'>"
						+	"</div></form>"

						+	generateForumPostForm(true, null, "Edit Post", content, "/forum/post/submit_edit/" + postID, error, false);
						;

			return generatePage(request, "Forum | " + forumName + " | " + topicName + " | Edit Post", body);
		} catch (Exception e) {
			return new ErrorHandler().error(request, "internal_server_error");
		}
	}

	@GetMapping("/forum/post/delete/{postID}")
	@ResponseBody
	public String deletePost(WebRequest request, @PathVariable long postID, @RequestParam(value="error", required=false) String error) {
		try {
			ResultSet sql = sql("select topic,body from posts where id=?", postID);
			sql.next();
			final long topicID = sql.getLong("topic");
			final String content = sql.getString("body");

			sql = sql("select forum,name from topics where id=?", topicID);
			sql.next();
			final long forumID = sql.getLong("forum");
			final String topicName = sql.getString("name");

			sql = sql("select name from forums where id=?", forumID);
			sql.next();
			final String forumName = sql.getString("name");

			String body	=	"<h1>Topic: " + htmlEscape(topicName) + ": Delete Post</h1>"
						+	"<p class='forum_description_name'>Forum: <a href='/forum/" + forumID + "'>" + htmlEscape(forumName) + "</a></p>"

						+	"<form><div class='forum_back_button_wrapper'>"
						+		"<input class='form_button' type='submit' value='Back' formaction='/forum/post/" + postID + "'>"
						+	"</div></form>"

						+	generateForumPostForm(true, null, "Delete Post", content, "/forum/post/submit_delete/" + postID, error, true);
						;

			return generatePage(request, "Forum | " + forumName + " | " + topicName + " | Delete Post", body);
		} catch (Exception e) {
			return new ErrorHandler().error(request, "internal_server_error");
		}
	}

	@GetMapping("/forum/topic/rename/{topicID}")
	@ResponseBody
	public String renameTopic(WebRequest request, @PathVariable long topicID, @RequestParam(value="error", required=false) String error) {
		try {
			if (!SecurityManager.isModerator(request)) return new ErrorHandler().error(request, "forbidden");

			ResultSet sql = sql("select forum,name from topics where id=?", topicID);
			sql.next();
			final long forumID = sql.getLong("forum");
			final String topicName = sql.getString("name");

			sql = sql("select name from forums where id=?", forumID);
			sql.next();
			final String forumName = sql.getString("name");

			String body	=	"<h1>Topic: " + htmlEscape(topicName) + ": Rename Topic</h1>"
						+	"<p class='forum_description_name'>Forum: <a href='/forum/" + forumID + "'>" + htmlEscape(forumName) + "</a></p>"

						+	"<form><div class='forum_back_button_wrapper'>"
						+		"<input class='form_button' type='submit' value='Back' formaction='/forum/topic/" + topicID + "'>"
						+	"</div></form>"

						+	generateForumPostForm(true, "Rename Topic", null, topicName, "/forum/topic/submit_rename/" + topicID, error, false);
						;

			return generatePage(request, "Forum | " + forumName + " | " + topicName + " | Rename Topic", body);
		} catch (Exception e) {
			return new ErrorHandler().error(request, "internal_server_error");
		}
	}

	@PostMapping("/forum/topic/submit_rename/{topicID}")
	public String doRenameTopic(WebRequest request, HttpSession session, @PathVariable long topicID, @RequestPart("subject") String subject) {
		try {
			session.setAttribute("freerct-rename-topic-subject", subject);

			if (!SecurityManager.isModerator(request)) return "redirect:/error?reason=forbidden";

			subject = subject.trim();
			if (subject.isEmpty()) return "redirect:/forum/topic/rename/" + topicID + "?error=empty_post#post_form";

			sql("update topics set name=? where id=?", subject, topicID);

			session.removeAttribute("freerct-rename-topic-subject");
			return "redirect:/forum/topic/" + topicID;
		} catch (Exception e) {
			return "redirect:/error?reason=internal_server_error";
		}
	}

	@GetMapping("/forum/topic/delete/{topicID}")
	@ResponseBody
	public String deleteTopic(WebRequest request, @PathVariable long topicID, @RequestParam(value="error", required=false) String error) {
		try {
			if (!SecurityManager.isModerator(request)) return new ErrorHandler().error(request, "forbidden");

			ResultSet sql = sql("select forum,name from topics where id=?", topicID);
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
						;

			return generatePage(request, "Forum | " + forumName + " | " + topicName + " | Delete Topic", body);
		} catch (Exception e) {
			return new ErrorHandler().error(request, "internal_server_error");
		}
	}

	@PostMapping("/forum/topic/submit_delete/{topicID}")
	public String doDeleteTopic(WebRequest request, @PathVariable long topicID) {
		try {
			if (!SecurityManager.isModerator(request)) return "redirect:/error?reason=forbidden";

			ResultSet sql = sql("select forum from topics where id=?", topicID);
			sql.next();
			final long forumID = sql.getLong("forum");

			sql("delete from topics where id=?", topicID);  // Also takes care of deleting posts and stuff via foreign keys

			return "redirect:/forum/" + forumID;
		} catch (Exception e) {
			return "redirect:/error?reason=internal_server_error";
		}
	}
}
