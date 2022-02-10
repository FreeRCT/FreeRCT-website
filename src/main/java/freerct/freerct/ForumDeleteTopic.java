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

/** The form to delete a forum topic. */
@Controller
public class ForumDeleteTopic {
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
