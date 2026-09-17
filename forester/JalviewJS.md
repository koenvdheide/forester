# Building the JalviewJS library

Use the same Forester source checkout for the desktop jar and browser library.
The Java2Script profile is `forester/.j2s`; its output goes to
`forester/build/jalviewjs`.

1. Install the Java2Script compiler from the target Jalview checkout's
   `jalviewjs/resources/SwingJSPlugin/swingjs/net.sf.j2s.core-j11.jar` into
   Eclipse. The current Jalview build uses Eclipse 2024-03 and Java 11 source
   compatibility.
2. Import the existing `forester` Eclipse project and run a clean build.
   Verify the Java2Script build reports zero errors.
3. From `forester`, run `ant -f java/build.xml jalviewjs-site`.
4. Copy `forester/build/forester-site.zip` to Jalview's
   `jalviewjs/resources/libjs/forester-site.zip`, then run
   `./gradlew jalviewjs:buildSite` in Jalview.

The archive contains the transpiled Forester classes and `phyloxml.xsd`.
Jalview supplies its Archaeopteryx configuration and its own wrappers and
alignment bindings. The desktop jar remains built by `java/build.xml`'s `jar`
target in a copy of the source tree, because that target cleans tracked class
files.

The browser profile does not bundle iText, OpenChart or Commons Codec classes.
Jalview hides Archaeopteryx's inference menu and browser PDF export. Optional
ImageIO writer discovery is disabled in JavaScript. Adding another viewer
feature that uses an external library requires checking its browser support.
