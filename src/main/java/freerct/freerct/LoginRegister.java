package freerct.freerct;

import java.sql.*;
import java.util.*;

import org.springframework.stereotype.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.*;

@Controller
public class LoginRegister {
	@GetMapping("/login")
	@ResponseBody
	public String fetch(WebRequest request,
			@RequestParam(value="error", required=false) boolean error) {
		String body = """
			<div class="content_flexbox">
				<div class="content_flexbox_content">
					<h1>Log In</h1>

					<form class='grid' method='post' enctype='multipart/form-data'>
						<label class='griditem'             style='grid-column:3/span 1; grid-row:1/span 1' for="username">Username:</label>
						<label class='griditem'             style='grid-column:3/span 1; grid-row:2/span 1' for="password">Password:</label>

						<input class='griditem'             style='grid-column:4/span 2; grid-row:1/span 1'
								type="text"     id="username" required name="username" autofocus>
						<input class='griditem'             style='grid-column:4/span 2; grid-row:2/span 1'
								type="password" id="password" required name="password">

						<input class='griditem form_button' style='grid-column:4/span 1; grid-row:3/span 1'
								type="submit" value="Log In!"            formaction="/login/signin">
						<input class='griditem form_button' style='grid-column:4/span 1; grid-row:4/span 1'
								type="submit" value="I lost my password" formaction="/login/forgotpassword" formnovalidate>

						<div   class='griditem'             style='grid-column:6/span 4; grid-row:1/span 3'></div>
						<div   class='griditem'             style='grid-column:1/span 2; grid-row:1/span 3'></div>
		""";

		if (error) body += "<div class='griditem form_error' style='grid-column:4/span 1; grid-row:5/span 1'>Wrong username or password.</div>";

		body += """
					</form>
				</div>

				<div class="content_flexbox_content">
					<h1>Register</h1>

					<form class='grid' method='post' enctype='multipart/form-data'>
						<label class='griditem'             style='grid-column:3/span 1; grid-row:1/span 1' for="username" >Username:</label>
						<label class='griditem'             style='grid-column:3/span 1; grid-row:2/span 1' for="email" >E-Mail:</label>
						<label class='griditem'             style='grid-column:3/span 1; grid-row:3/span 1' for="password" >Password:</label>
						<label class='griditem'             style='grid-column:3/span 1; grid-row:4/span 1' for="password2">Repeat Password:</label>

						<input class='griditem'             style='grid-column:4/span 2; grid-row:1/span 1'
								type="text"     id="new_username"    required name="new_username">
						<input class='griditem'             style='grid-column:4/span 2; grid-row:2/span 1'
								type="email"    id="email"           required name="email">
						<input class='griditem'             style='grid-column:4/span 2; grid-row:3/span 1'
								type="password" id="new_password  "  required name="new_password">
						<input class='griditem'             style='grid-column:4/span 2; grid-row:4/span 1'
								type="password" id="repeat_password" required name="repeat_password">

						<input class='griditem form_button' style='grid-column:4/span 1; grid-row:5/span 1'
								type="submit" value="Create Account" formaction="/login/signup">

						<div   class='griditem'             style='grid-column:6/span 4; grid-row:1/span 3'></div>
						<div   class='griditem'             style='grid-column:1/span 2; grid-row:1/span 3'></div>
					</form>
				</div>
			</div>
		""";
		return FreeRCTApplication.generatePage(request, "Log In", body);
	}
}
