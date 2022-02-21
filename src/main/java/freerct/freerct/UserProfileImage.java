package freerct.freerct;

import java.io.*;
import java.nio.file.Files;
import java.sql.*;
import java.util.*;

import javax.servlet.http.*;
import org.springframework.stereotype.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.*;
import org.springframework.web.multipart.*;

import static freerct.freerct.FreeRCTApplication.generatePage;
import static freerct.freerct.FreeRCTApplication.sql;
import static freerct.freerct.FreeRCTApplication.sqlSync;
import static freerct.freerct.FreeRCTApplication.bash;
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
	public String changeImg(WebRequest request, HttpSession session, @PathVariable String username, @RequestParam(value="error", required=false) String error) {
		try {
			if (!username.equals(request.getRemoteUser())) return new ErrorHandler().error(request, session, "forbidden");

			String body = """
					<a class='anchor' id='image_form'></a>
					<div class='login_form_wrapper'>
						<h1>Change Profile Image</h1>

						<form class='grid' method='post' enctype='multipart/form-data'>
							<label class='griditem' style='grid-column:3/span 1; grid-row:1/span 1' for="image">Upload Image:</label>

							<input class='griditem' style='grid-column:4/span 2; grid-row:1/span 1'
									type="file" accept=".png" required id="image" name="image">

							<input class='griditem form_button form_default_action' style='grid-column:4/span 1; grid-row:2/span 1'
				"""
				+				"type='submit' value='Change Image' formaction='/user/" + username + "/submit_changeimg'>"
				+ """

							<input class='griditem form_button' style='grid-column:4/span 1; grid-row:3/span 1'
				"""
				+				"type='submit' value='Clear Image' formaction='/user/" + username + "/submit_clearimg' formnovalidate>"
				+ """

							<div   class='griditem' style='grid-column:6/span 3; grid-row:1/span 3'></div>
							<div   class='griditem' style='grid-column:1/span 2; grid-row:1/span 3'></div>
						</form>
			""";

			if (error != null) {
				body += "<p class='form_error login_form_caption'>";
				switch (error.toLowerCase()) {
					case "filetype":
						body += "Only PNG files are permitted.";
						break;
					case "damaged":
						body += "This file is not a valid PNG file.";
						break;
					default:
						body += "An unknown error has occurred.";
						break;
				}
				body += "</p>";
			}
			body += "</div>";

			return generatePage(request, session, "User | " + username + " | Change Profile Image", body);
		} catch (Exception e) {
			return new ErrorHandler().error(request, session, "internal_server_error");
		}
	}

	@PostMapping("/user/{username}/submit_clearimg")
	public String clearImage(WebRequest request, @PathVariable String username) {
		try {
			if (!username.equals(request.getRemoteUser()) && !SecurityManager.isAdmin(request)) return "redirect:/error?reason=forbidden";

			File destination = new File(Resources.RESOURCES_DIR, "img/users/" + username + ".png");
			if (destination.exists()) destination.delete();

			return "redirect:/user/" + username;
		} catch (Exception e) {
			return "redirect:/error?reason=internal_server_error";
		}
	}

	@PostMapping("/user/{username}/submit_changeimg")
	public String changeImage(WebRequest request, @PathVariable String username, @RequestPart("image") MultipartFile image) {
		try {
			if (!username.equals(request.getRemoteUser())) return "redirect:/error?reason=forbidden";

			File tempFile = Files.createTempFile(null, ".png").toFile();
			image.transferTo(tempFile);

			if (!bash("mimetype", "-M", "-b", tempFile.getPath()).equals("image/png"))
				return "redirect:/user/" + username + "/changeimg?error=filetype#image_form";

			try {
				javax.imageio.ImageIO.read(tempFile);
			} catch (Exception e) {
				return "redirect:/error?reason=damaged";
			}

			File destination = new File(Resources.RESOURCES_DIR, "img/users");
			destination.mkdir();
			destination = new File(destination, username + ".png");
			if (destination.exists()) destination.delete();
			Files.move(tempFile.toPath(), destination.toPath());

			return "redirect:/user/" + username;
		} catch (Exception e) {
			return "redirect:/error?reason=internal_server_error";
		}
	}
}
