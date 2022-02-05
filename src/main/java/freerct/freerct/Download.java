package freerct.freerct;

import org.springframework.stereotype.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.*;

@Controller
public class Download {

	@GetMapping("/download")
	@ResponseBody
	public String fetch(WebRequest request) {
		return FreeRCTApplication.generatePage(request, "Get It!", """
			<h1>Get It!</h1>

			"""
			+ FreeRCTApplication.createLinkifiedHeader("h2", "/download", "releases", "Releases")
			+ """
			<p style="text-align:center"> <table>
				<tr>
					<th>Version</th>
					<th>Release Date</th>
					<th>Windows (64 bit)</th>
					<th>Windows (32 bit)</th>
					<th>MacOS</th>
					<th>Debian/Ubuntu</th>
					<th>Source Code</th>
				</tr>
				<tr>
					<td class="table_release">0.1</td>
					<td>To be announced</td>
					<td class="invalid">N/A</td>
					<td class="invalid">N/A</td>
					<td class="invalid">N/A</td>
					<td class="invalid">N/A</td>
					<td class="invalid">N/A</td>
				</tr>
			</table> </p> <p>
				When FreeRCT 0.1 is released, links to the installers and instructions how to use them will be provided here.
				<!-- TODO: Each installer should also provide a checksum so the user can check that the download is valid. -->
			</p>

			"""
			+ FreeRCTApplication.createLinkifiedHeader("h2", "/download", "daily", "Daily Builds")
			+ """
			<p>
				There are no daily builds yet. When FreeRCT daily build integration has been set up,
				links to the installers and instructions how to use them will be provided here.
			</p>

			"""
			+ FreeRCTApplication.createLinkifiedHeader("h2", "/download", "compile", "Compiling From Source")
			+ """
			<ol>
				<li>      Check out the commit you wish to build in Git,
					      or download and extract a ZIP/tarball of the latest development version from
					      <a target="_blank" href="https://github.com/FreeRCT/FreeRCT">GitHub</a>.
				</li><li> You need to have CMake and Make installed in order to do compile FreeRCT.
					      CMake will check that all required libraries are installed.
					      If this fails, you need to install the libraries it complains about,
					      and retry until all dependencies are found.
				</li><li> <tt>cd</tt> into the downloaded <tt>FreeRCT</tt> directory and call:
					      <pre><code>
	<emp># Construct build directory and enter it:</emp>
	mkdir build
	cd build

	<emp># Generate Makefile. Some build options – all of them optional – are: </emp>
	<emp>#   -DCMAKE_INSTALL_PREFIX=/usr/local   # Set install directory, default is '/usr'. </emp>
	<emp>#   -DASAN=ON                           # Link with ASan memory checker. </emp>
	<emp>#   -DUSERDATAPREFIX='~/.freerct'       # FreeRCT expands the '~' to the user home directory at runtime. </emp>
	<emp>#   -DVERSION_STRING="0.0-alpha"        # Set custom version string. </emp>
	<emp># Further details can be found in the README file in the root source directory. </emp>
	cmake ..

	<emp># Compile and install: </emp>
	make
	make install  <emp># Can be skipped. </emp>
					      </code></pre>
				</li>
			</ol>

			"""
			+ FreeRCTApplication.createLinkifiedHeader("h2", "/download", "play", "How To Play")
			+ """
			<p>
				Just start the installed binary or type <tt>freerct</tt>.
				If you compiled FreeRCT yourself and skipped the <tt>make install</tt> step,
				use <tt>./build/bin/freerct</tt> or <tt>make run</tt>.
			</p><p>
				After installation, see the <a href="/manual">online manual</a> for information how to use FreeRCT.
			</p><p>
				Available command-line options are:
			</p><p style="text-align:center"> <table>
				<tr>
					<th>Short Option</th>
					<th>Long Option</th>
					<th>Explanation</th>
				</tr>
				<tr>
					<td class="codefield">-h</td>
					<td class="codefield">--help</td>
					<td>Display all available arguments.</td>
				</tr>
				<tr>
					<td class="codefield">-v</td>
					<td class="codefield">--version</td>
					<td>Display the build version and configuration.</td>
				</tr>
				<tr>
					<td class="codefield">-l FILE</td>
					<td class="codefield">--load FILE</td>
					<td>Directly load the specified savegame file.</td>
				</tr>
				<tr>
					<td class="codefield">-r</td>
					<td class="codefield">--resave</td>
					<td>Immediately save every savegame file again after loading.</td>
				</tr>
				<tr>
					<td class="codefield">-a LANG</td>
					<td class="codefield">--language LANG</td>
					<td>Start FreeRCT in the specified language. See below for supported languages.</td>
				</tr>
			</table></p>
			<p>
				FreeRCT is currently available in the following languages:
			</p><p style="text-align:center"> <table>
				<tr>
					<th>Language Code</th>
					<th>Name</th>
				</tr>
				<tr>
					<td class="codefield">da_DK</td>
					<td>Danish (Denmark)</td>
				</tr>
				<tr>
					<td class="codefield">de_DE</td>
					<td>German (Germany)</td>
				</tr>
				<tr>
					<td class="codefield">en_GB</td>
					<td>English (United Kingdom)</td>
				</tr>
				<tr>
					<td class="codefield">en_US</td>
					<td>English (United States)</td>
				</tr>
				<tr>
					<td class="codefield">es_ES</td>
					<td>Spanish (Spain)</td>
				</tr>
				<tr>
					<td class="codefield">fr_FR</td>
					<td>French (France)</td>
				</tr>
				<tr>
					<td class="codefield">nds_DE</td>
					<td>Low German (Germany)</td>
				</tr>
				<tr>
					<td class="codefield">nl_NL</td>
					<td>Dutch (Netherlands)</td>
				</tr>
				<tr>
					<td class="codefield">sv_SE</td>
					<td>Swedish (Sweden)</td>
				</tr>
			</table></p>
		""");
	}

}
