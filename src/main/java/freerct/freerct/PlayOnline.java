package freerct.freerct;

import java.io.*;
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
import static freerct.freerct.FreeRCTApplication.bash;
import static freerct.freerct.FreeRCTApplication.config;
import static freerct.freerct.FreeRCTApplication.htmlEscape;
import static freerct.freerct.FreeRCTApplication.renderMarkdown;
import static freerct.freerct.FreeRCTApplication.datetimestring;
import static freerct.freerct.FreeRCTApplication.shortDatetimestring;
import static freerct.freerct.FreeRCTApplication.pluralForm;
import static freerct.freerct.FreeRCTApplication.createLinkifiedHeader;
import static freerct.freerct.FreeRCTApplication.generateForumPostForm;

/** The frame that runs FreeRCT as an online WebAssembly game. */
@Controller
public class PlayOnline {
	@PostMapping("/savegames/upload")
	@ResponseBody
	public String uploadSavegame(WebRequest request, HttpSession session, @RequestBody String blob) {
		try {
			final int colonpos = blob.lastIndexOf(':');
			if (colonpos <= 0) return "Missing file name part";
			String name = blob.substring(0, colonpos);
			blob = blob.substring(colonpos + 1);

			final int length = blob.length();
			if (length % 2 != 0 || length < 4) return "Malformed blob";

			File dir = new File(Resources.RESOURCES_DIR, "savegames/storage");
			dir.mkdirs();
			File outFile = new File(dir, new File(name).getName());
			PrintStream out = new PrintStream(outFile);
			for (int i = 0; i < length;) {
				int b1 = blob.charAt(i++) - 'A';
				int b2 = blob.charAt(i++) - 'a';
				if (b1 < 0 || b2 < 0 || b1 > 15 || b2 > 15) {
					out.close();
					outFile.delete();
					return "Invalid byte";
				}
				out.write((b2 << 4) | b1);
			}
			out.close();

			return "";
		} catch (Exception e) {
			return "internal_server_error";
		}
	}

	@GetMapping("/play/play")
	@ResponseBody
	public String play(WebRequest request, HttpSession session, @RequestParam(value="guest", required=false) boolean noLogin) {
		try {
			if (request.getRemoteUser() == null && !noLogin) {
				return generatePage(request, session, "Play FreeRCT Online", """
					<h1>Play FreeRCT Online</h1>
					<div class='grid new_post_form' style='grid-template-columns: 25% auto 25%'>
						<p class='griditem center'                                 style='grid-column:1/span 3; grid-row:1'
								>Any savegames created while playing online will be stored only for logged-in users.</p>
						<a class='griditem form_button form_default_action center' style='grid-column:2/span 1; grid-row:2'
								href='/login?next=/play/play'>Log In</a>
						<a class='griditem form_button center'                     style='grid-column:2/span 1; grid-row:3'
								href='/play/play?guest=true'>Play as Guest</a>
					</div>
				""");
			}

			String version = bash("bash", "-c", "cd '" + config("freerct") + "' && git describe --tags --always");

			String body	= """
				<h1>Play FreeRCT Online</h1>
				<a class='anchor' id='anchor'></a>
				<div class='forum_header_grid_toplevel'>
					<div class='griditem forum_header_grid_side_column_l'>
					</div>
					<div class='griditem forum_header_grid_middle_column'>
			""";
			if (request.getRemoteUser() == null) {
				body += """
						<p class='forum_description_name' id='note_savegames_not_stored'>
							<strong>Note:</strong>
							You are not logged in. Savegames will <strong>not</strong> be persisted.
						</p>
				""";
			}
			body += """
						<div id='spinner'></div>
						<p class='forum_description_name' id='status'>Preparing...</p>
						<p class='forum_description_stats'
			"""
			+				">Version: <a href='https://github.com/FreeRCT/FreeRCT/commit/" + version + "'>" + version + "</a></p>"
			+ """
					</div>
					<div class='griditem forum_header_grid_side_column_r playonline_buttons'>
						<div class='form_button' onclick="Module.requestFullscreen(false, true)">Fullscreen</div>
					</div>
				</div>

				<div class='playonline_progressbar'><progress hidden value="00" max="100" id="progress"></progress></div>
				<div class='playonline'>
					<canvas id="canvas" oncontextmenu="event.preventDefault()" tabindex=-1>
						The online playing environment is apparently not supported in your browser.
						To use it, please ensure that JavaScript is enabled and your browser supports WebAssembly (WASM).
					</canvas>
				</div>
				<textarea class='playonline_textarea' id="output" readonly rows="3"></textarea>

				<script>
					 /** Jump down to main frame. */
					function GoToAnchor() {
						location.href = "#anchor";
					}
			""";

			if (request.getRemoteUser() == null) {
				body += """
					function GameSavedCallback(name, unused) {
						GoToAnchor();
						var e = document.getElementById('note_savegames_not_stored');
						e.style.backgroundColor = '#e60a12';
						setTimeout(() => {
							e.style.backgroundColor = 'initial';
						}, 8000);
					}
				""";
			} else {
				body += """
					async function GameSavedCallback(name, blob) {
						const response = await fetch("/savegames/upload", {
							method: 'POST',
							body: name + ":" + blob
						});
						const result = await response.text();
						if (result) {
							console.log("WARNING: Saving " + name + " failed: " + result);
						} else {
							console.log("Savegame " + name + " saved on server.");
						}
					}
				""";
			}

			body += """
				</script>

				<!-- Autogenerated by Emscripten -->

				<script type='text/javascript'>
				  var statusElement = document.getElementById('status');
				  var progressElement = document.getElementById('progress');
				  var spinnerElement = document.getElementById('spinner');

				  var Module = {
					preRun: [],
					postRun: [],
					print: (function() {
					  var element = document.getElementById('output');
					  if (element) element.value = ''; // clear browser cache
					  return function(text) {
						if (arguments.length > 1) text = Array.prototype.slice.call(arguments).join(' ');
						// These replacements are necessary if you render to raw HTML
						//text = text.replace(/&/g, "&amp;");
						//text = text.replace(/</g, "&lt;");
						//text = text.replace(/>/g, "&gt;");
						//text = text.replace('\\n', '<br>', 'g');
						console.log(text);
						if (element) {
						  element.value += text + "\\n";
						  element.scrollTop = element.scrollHeight; // focus on bottom
						  element.style.display = "initial";
						}
					  };
					})(),
					canvas: (function() {
					  var canvas = document.getElementById('canvas');

					  // As a default initial behavior, pop up an alert when webgl context is lost. To make your
					  // application robust, you may want to override this behavior before shipping!
					  // See http://www.khronos.org/registry/webgl/specs/latest/1.0/#5.15.2
					  canvas.addEventListener("webglcontextlost", function(e) { alert('WebGL context lost. You will need to reload the page.'); e.preventDefault(); }, false);

					  return canvas;
					})(),
					setStatus: function(text) {
					  if (!Module.setStatus.last) Module.setStatus.last = { time: Date.now(), text: '' };
					  if (text === Module.setStatus.last.text) return;
					  var m = text.match(/([^(]+)\\((\\d+(\\.\\d+)?)\\/(\\d+)\\)/);
					  var now = Date.now();
					  if (m && now - Module.setStatus.last.time < 30) return; // if this is a progress update, skip it if too soon
					  Module.setStatus.last.time = now;
					  Module.setStatus.last.text = text;
					  if (m) {
						text = m[1];
						progressElement.value = parseInt(m[2])*100;
						progressElement.max = parseInt(m[4])*100;
						progressElement.hidden = false;
						spinnerElement.hidden = false;
					  } else {
						progressElement.value = null;
						progressElement.max = null;
						progressElement.hidden = true;
						if (!text) spinnerElement.style.display = 'none';
					  }
					  statusElement.innerHTML = text;
					},
					totalDependencies: 0,
					monitorRunDependencies: function(left) {
					  this.totalDependencies = Math.max(this.totalDependencies, left);
					  Module.setStatus(left ? 'Preparing... (' + (this.totalDependencies-left) + '/' + this.totalDependencies + ')' : 'All downloads complete.');
					}
				  };
				  Module.setStatus('Downloading...');
				  window.onerror = function(event) {
					// TODO: do not warn on ok events like simulating an infinite loop or exitStatus
					Module.setStatus('Exception thrown, see JavaScript console');
					spinnerElement.style.display = 'none';
					Module.setStatus = function(text) {
					  if (text) Module.printErr('[post-exception status] ' + text);
					};
				  };
				</script>
				<script async type="text/javascript" src="/play/freerct.js"></script>

				<script> GoToAnchor() </script>
			""";

			return generatePage(request, session, "Play FreeRCT Online", body);

		} catch (Exception e) {
			return new ErrorHandler().error(request, session, "internal_server_error");
		}
	}
}
