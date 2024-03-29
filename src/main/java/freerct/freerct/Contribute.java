package freerct.freerct;

import javax.servlet.http.*;
import org.springframework.stereotype.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.*;

import static freerct.freerct.FreeRCTApplication.generatePage;
import static freerct.freerct.FreeRCTApplication.sql;
import static freerct.freerct.FreeRCTApplication.getCalendar;
import static freerct.freerct.FreeRCTApplication.htmlEscape;
import static freerct.freerct.FreeRCTApplication.renderMarkdown;
import static freerct.freerct.FreeRCTApplication.datetimestring;
import static freerct.freerct.FreeRCTApplication.shortDatetimestring;
import static freerct.freerct.FreeRCTApplication.createLinkifiedHeader;

/** The Contribute page. */
@Controller
public class Contribute {
	@GetMapping("/contribute")
	@ResponseBody
	public String fetch(WebRequest request, HttpSession session) {
		return generatePage(request, session, "Contribute", """
				<h1>Contribute</h1>
				<p>
					So you want to help develop FreeRCT? That's great!
				</p><p>
					Development happens on our <a target="_blank" href="https://github.com/FreeRCT/FreeRCT">GitHub repo</a>.
					You need to have an account on GitHub and some basic experience with Git to get started.
					It's not that hard though; <a target="_blank" href="https://docs.github.com/en/get-started">see here</a>
					for a quickstart guide on how to create a free GitHub account and use Git.
				</p><p>
					The game is still in an early stage of development and new contributions are always welcome.
					Just start working on a feature or bugfix and propose it for merging via a pull request (PR) on GitHub.
					If you like, you can announce what you'll work on, but this is not required.
					If you are unsure what to work on, look at the
					<a target="_blank" href="https://github.com/FreeRCT/FreeRCT/issues">list of open issues</a>
					for inspiration or ask us for pointers.
				</p>

			"""
			+ createLinkifiedHeader("h2", "/contribute", "graphics", "Graphics Design")
			+ """
				<p>
					Whether you can create 2D graphics for the UI or 3D images for rides and other in-game objects,
					all contributions are welcome. You can enhance or replace existing images, and submit
					graphics for new objects which we'll then include in the game.
				</p>

			"""
			+ createLinkifiedHeader("h2", "/contribute", "sound", "Sound & Music")
			+ """
				<p>
					Okay, FreeRCT doesn't have a sound engine yet, but that's only because we don't
					currently have any sound or music at all.
					If you can design sound effects or compose background music, you're very welcome to
					create and submit some, and we'll integrate them into the game.
				</p>

			"""
			+ createLinkifiedHeader("h2", "/contribute", "translating", "Translating")
			+ """
				<p>
					The source language of FreeRCT is British English (<code>en_GB</code>).
					All strings are defined in <code>graphics/rcd/lang/&lt;LANGUAGE_CODE&gt;.yml</code>
					in the YAML file format.
					Enhancing translations for an existing language as well as adding a new language
					is as easy as opening/creating the respective file for your language
					and editing the strings therein.
				</p><p>
					When you add a new language, it additionally needs to be added to the list of all
					languages in <code>src/language_definitions.h</code>.
					The lists of languages must be kept in alphabetical order.
					If your language uses an unusual plural rule, additionally add a function for that rule.
				</p>

			"""
			+ createLinkifiedHeader("h2", "/contribute", "testing", "Testing")
			+ """
				<p>
					Even if you can't contribute code or art, you're welcome to test FreeRCT and hunt for bugs.
					Just check out the latest development version with Git, compile, and run.
					Report any bugs you encounter on the
					<a target="_blank" href="https://github.com/FreeRCT/FreeRCT/issues">issue tracker</a>
					(you need a GitHub account for this).
				</p><p>
					The issue tracker will often contain reports of bugs with no known way to reproduce.
					Such bugs are usually hard to fix. If you look for a sequence of steps that reliably
					triggers such a bug, fixing it will become much easier for the developers.
				</p><p>
					You can also test
					<a target="_blank" href="https://github.com/FreeRCT/FreeRCT/pulls">open PRs</a>.
					In this case, bugs should be reported in the PR, not on the issue tracker.
					The PR description will tell you what to watch out for specifically.
					To test a PR, you can either <a href="/download#compile">compile it yourself</a>
					or download prebuilt binaries. To get those, select the PR's <em>Checks</em> tab, then
					select the <em>CI</em> group, then scroll down to <em>Artifacts</em> and download the
					desired build. You need a GitHub account to download these artifacts.
				</p><p>
					New screenshots for the website's <a href="/screenshots">screenshots section</a>
					are also always appreciated.
				</p>

			"""
			+ createLinkifiedHeader("h2", "/contribute", "website", "Website Scripting")
			+ """
				<p>
					You're welcome to improve this website in every way you can think of.
				</p><p>
					The site is written in Java 17 using the Spring Boot framework.
					There's a lot of hand-crafted HTML, CSS, and JavaScript as well.
					The site is still heavily under development.
				</p>

			"""
			+ createLinkifiedHeader("h2", "/contribute", "coding", "Coding For FreeRCT")
			+ """
				<p>
					The FreeRCT engine is coded in C++ using GLFW3 with OpenGL ES 3.
					Rides and other units are defined in a custom language called the RCD file format.
				</p><p>
					Please provide Doxygen documentation for your C++ patches.
					If your feature requires new graphics, it is acceptable to create simple placeholder
					pics until someone else creates final images.
				</p><p>
					You can find more technical information in the <code>developer_documentation/</code> folder of the Git checkout.
				</p>
		""");
	}
}
