import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.application.Application
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Computable
import com.intellij.psi.PsiElement
import com.jetbrains.cidr.cpp.toolchains.CPPEnvironment
import com.jetbrains.cidr.cpp.toolchains.CPPToolchains
import com.jetbrains.cmake.psi.CMakeCommandName
import com.jetbrains.cmake.psi.CMakeElement
import com.jetbrains.cmake.psi.CMakeLiteral
import org.asciidoctor.Asciidoctor
import org.asciidoctor.jruby.AsciidoctorJRuby.Factory.create
import org.jetbrains.rpc.LOG
import java.io.IOException
import java.util.*


class CMakeDocProvider : AbstractDocumentationProvider() {

    private var moduleList = ArrayList<String>()

    private var propertyList = ArrayList<String>()

    private var variableList = ArrayList<String>()

    private val asciidoctor by lazy {
       // val tmpdir = createTempDir()
        //val prj = AsciiDoc(ProjectManager.getInstance().createProject(tmpdir.absolutePath, tmpdir.absolutePath), tmpdir, null, "clion-cmakedocs")
        val asciidoctor: Asciidoctor = create()
        asciidoctor
    }

    private fun runCMake(vararg args: String): Process {
        val cppEnvironment = CPPEnvironment(CPPToolchains.getInstance().getToolchainByNameOrDefault(null)!!)
        val cmake = cppEnvironment.cMake
        LOG.info("CDP - Executing CMake: ${cmake!!.executablePath} ${args.joinToString(" ")}")
        return ProcessBuilder(cmake.executablePath, *args).start()
    }

    private fun <T> withLinesFromCMake(vararg args: String, block: (Sequence<String>) -> T): T? {
        try {
            return runCMake(*args).inputStream.bufferedReader().useLines { block(it) }
        } catch (e: IOException) {
            e.printStackTrace()
            Notifications.Bus.notify(Notification("ApplicationName", "CMake Docs", "Unable to run CMake to get docs", NotificationType.ERROR))
        }
        return null
    }

    private fun getLinesFromCMake(vararg args: String, block: (Sequence<String>) -> Sequence<String> = { it }): List<String> {
        return withLinesFromCMake(*args) { block(it).toList() } ?: emptyList()
    }

    private fun docForCommand(cmd: String): String? = withLinesFromCMake("--help-command", cmd) { it.joinToString("\n") }

    private fun docForLiteral(literal: String): String? {
        val lit = literal.removeSurrounding("\${", "}") /* remove expansion syntax if present */
        if (moduleList.isEmpty()) {
            moduleList.addAll(getLinesFromCMake("--help-module-list"))
        }
        if (lit in moduleList) {
            return withLinesFromCMake("--help-module", lit) { it.joinToString("\n") }
        }
        if (propertyList.isEmpty()) {
            propertyList.addAll(getLinesFromCMake("--help-property-list"))
        }
        if (lit in propertyList) {
            return withLinesFromCMake("--help-property", lit) { it.joinToString("\n") }
        }
        if (variableList.isEmpty()) {
            variableList.addAll(getLinesFromCMake("--help-variable-list"))
        }
        if (lit in variableList) {
            return withLinesFromCMake("--help-variable", lit) { it.joinToString("\n") }
        }
        return null
    }

    /*fun fuckYOu(): String? {
        val x = ApplicationManager.getApplication().invokeAndWait({
            val tmpdir = createTempDir()
            val prj = AsciiDoc(ProjectManager.getInstance().createProject(tmpdir.absolutePath, tmpdir.absolutePath), tmpdir, null, "clion-cmakedocs$tmpdir")
            val result = prj.render("bold *constrained* & **un**constrained", emptyList())
            return@invokeAndWait Unit.let { result.toString() }
        }, ModalityState.defaultModalityState())


        return x.toString()
    }*/

    override fun generateDoc(element: PsiElement?, originalElement: PsiElement?): String? {
        return if (element == null || element !is CMakeElement) {
            null
        } else {
            when (element) {
                is CMakeCommandName -> docForCommand(element.name)
                is CMakeLiteral -> docForLiteral(element.text)
                else -> null
            }.let {
                if (it == null) null
                else {
                    //asciiDoc.render(it, emptyList())
                    //ApplicationManager.getApplication().isReadAccessAllowed
                    //ApplicationManager.getApplication().isReadAccessAllowed
                    val application: Application = ApplicationManager.getApplication()
                    /*var res = "no WriteAccessAllowed"
                    if (application.isWriteAccessAllowed)
                        res = "WriteAccessAllowed"
                    if (application.isReadAccessAllowed)
                        res += ", ReadAccessAllowed"
                    else
                        res += ", no ReadAccessAllowed"
                    return res*/

                    if (application.isReadAccessAllowed) {
                        return application.runReadAction(Computable<String>() {
                            //val tmpdir = createTempDir()
                            //val prj = AsciiDoc(ProjectManager.getInstance().createProject(tmpdir.absolutePath, tmpdir.absolutePath), tmpdir, null, "clion-cmakedocs$tmpdir")
                            try {
                                // prj.render("bold *constrained* & **un**constrained", emptyList())

                                //val asciidoctor: Asciidoctor = initWithExtensions(extensions, springRestDocsSnippets != null, format)

                                //val asciidoctor: Asciidoctor = create()
                                //val output = asciidoctor.convert("Hello _Baeldung_!", HashMap())
                                //val asciidoctor: Asciidoctor = create()
                                val output = asciidoctor.convert(it, HashMap())
                                output
                                //asciiDoc.render(it, emptyList())

                            } catch (e: IllegalStateException) {
                                // handler
                                return@Computable "IllegalStateException, $it"
                            }
                        })
                    } else {
                        it
                    }

                    /*var testMe = fuckYOu()
                    val tmpdir = createTempDir()
                    val prj = AsciiDoc(ProjectManager.getInstance().createProject(tmpdir.absolutePath, tmpdir.absolutePath), tmpdir, null, "clion-cmakedocs$tmpdir")
                    prj.render("bold *constrained* & **un**constrained", emptyList())
                    //"Drecksau"
                    //it*/
                }
            }
        }
    }
}
