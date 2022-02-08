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

/** The form to create a new topic in a forum. */
@Controller
public class ForumNewTopic {
	@GetMapping("/forum/{forumID}/new")
	@ResponseBody
	public String fetch(WebRequest request, @PathVariable long forumID) {
		try {
			ResultSet sql = sql("select name,description from forums where id=?", forumID);
			sql.next();
			final String forumName = sql.getString("name");
			final String forumDescription = sql.getString("description");

			String body	=	"<h1>Forum: " + renderMarkdown(forumName) + ": New Topic</h1>"
						+	"<p class='forum_description_name'>" + renderMarkdown(forumDescription) + "</p>"
						+ """
							<form class='grid' method='post' enctype='multipart/form-data'>
								<label class='griditem'             style='grid-column:3/span 1; grid-row:1/span 1' for="subject">Subject:</label>
								<label class='griditem'             style='grid-column:3/span 1; grid-row:2/span 1' for="content">Post:</label>

								<input    class='griditem' style='grid-column:4/span 2; grid-row:1/span 1' type="text"
										id="subject" required name="subject" autofocus>
								<textarea class='griditem' style='grid-column:4/span 2; grid-row:2/span 1; resize:vertical'
										id="content" required name="content"></textarea>

								<input class='griditem form_button' style='grid-column:4/span 1; grid-row:3/span 1'
						"""
						+			"type='submit' value='Submit' formaction='/forum/" + forumID + "/submit_new'>"
						+ """

								<div   class='griditem'             style='grid-column:6/span 3; grid-row:1/span 3'></div>
								<div   class='griditem'             style='grid-column:1/span 2; grid-row:1/span 3'></div>
							</form>
						""";

			return generatePage(request, "Forum | " + forumName + " | New Topic", body);
		} catch (Exception e) {
			return new ErrorHandler().error(request);
		}
	}

	@PostMapping("/forum/{forumID}/submit_new")
	public String createTopic(WebRequest request,
			@PathVariable long forumID,
			@RequestPart("subject") String subject,
			@RequestPart("content") String content) {
		try {
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

			return "redirect:/forum/topic/" + topicID;
		} catch (Exception e) {
			return "redirect:/error";
		}
	}
}
