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
	@GetMapping("/forum/{forumID}/new")
	@ResponseBody
	public String newTopic(WebRequest request, HttpSession session, @PathVariable long forumID, @RequestParam(value="error", required=false) String error) {
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

			return generatePage(request, session, "Forum | " + forumName + " | New Topic", body);
		} catch (Exception e) {
			return new ErrorHandler().error(request, session, "internal_server_error");
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
}
