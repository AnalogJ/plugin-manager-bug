import com.matthiasdenu.*
import hudson.EnvVars
import hudson.FilePath
import hudson.PluginManager
import hudson.model.Job
import javaposse.jobdsl.dsl.DslScriptLoader
import javaposse.jobdsl.dsl.GeneratedItems
import javaposse.jobdsl.dsl.GeneratedJob
import javaposse.jobdsl.dsl.ScriptRequest
import javaposse.jobdsl.plugin.JenkinsJobManagement
import javaposse.jobdsl.plugin.ScriptRequestGenerator
import org.junit.ClassRule
import org.jvnet.hudson.test.JenkinsRule
import org.jvnet.hudson.test.TestPluginManager
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

class Test1Specification extends Specification {
    @Shared
    @ClassRule
    JenkinsRule jenkinsRule = new JenkinsRule()

    @Shared jenkinsJobs = []
    @Shared dslGeneratedItems
    @Shared dslGeneratedJobNames

//    PluginManager getPluginManager() {
//        return TestPluginManager.INSTANCE
//    }
//    @Override
//    PluginManager getPluginManager() {
//        // This breaks `gradle test` with the "java.lang.AssertionError: class
//        // org.jenkinsci.plugins.workflow.job.WorkflowJob is missing its descriptor" error
//        //
//        // You can use one instance of one type, but you can't start a new Spock spec
//        // with a new JenkinsRule using another instance of another type or you will get
//        // this error -- At the end of the day, I want to use the resolveTestPluginsCustom
//        // gradle task in build.gradle to test some of the Jenkins job DSL with a different
//        // set of plugins of different specs... So if there is another way to do this,
//        // I am all ears... I haven't yet tried changing the versions of the
//        // jenkins-test-harness that we're using.
//        return CustomPluginManager.INSTANCE
//    }

    def setupSpec() {
        // Disable the Jenkins security realm (job-dsl modifies it)
        jenkinsRule.jenkins.disableSecurity()

        List scriptRequests = parseJenkinsGroovyScripts()
        dslGeneratedItems = generateDslItems(scriptRequests)
        Set dslGeneratedJobs = getJobsFromGeneratedItems(dslGeneratedItems)

        deleteJobsXmlOutputDirs()
        writeJobsXmlOutputToDir(dslGeneratedJobs, getXmlOutputDir())

        jenkinsJobs = dslGeneratedJobs
                .collect {job -> jenkinsRule.jenkins.getItemByFullName(job.jobName)}
        dslGeneratedJobNames = getNamesFromJobs(dslGeneratedJobs)
        println(dslGeneratedJobNames)
    }

    def 'System - should not exceed max number of view tabs'() {
        expect:
        dslGeneratedItems.views.size() <= 31
    }

    @Unroll
    def 'Job - should have a description - #job.displayName'(job) {
        expect:
        !job.description.isEmpty()

        where:
        job << jenkinsJobs
    }

    @Unroll
    def 'Job - should always activate log rotation - #job.displayName'(job) {
        expect:
        job.getBuildDiscarder() != null

        where:
        job << jenkinsJobs
    }


    void deleteJobsXmlOutputDirs() {
        (new File(getXmlOutputDir())).deleteDir()
    }

    String getWorkspace() {
        return "build/resources/test/workspace/${getEnvironment()}"
    }

    String getXmlOutputDir() {
        return "build/debug-xml/${getEnvironment()}"
    }

    GeneratedItems generateDslItems(List<ScriptRequest> scriptRequests) {
        def jobManagement = new JenkinsJobManagement(System.out, [:], new File(getWorkspace()))
        def dslScriptLoader = new DslScriptLoader(jobManagement)
        return dslScriptLoader.runScripts(scriptRequests)
    }

    String getEnvironmentDisplayName() {
        return getEnvironment().substring(0, 1).toUpperCase() + getEnvironment().substring(1)
    }

    List<String> getJobFiles() {
        return ['jobs/hello_world.groovy']
    }

    static Set<GeneratedJob> getJobsFromGeneratedItems(dslItems,
                                                       excludes = ['QE', 'Foreman']) {
        return dslItems.jobs
                .findAll { !excludes.contains(it.jobName) } as Set<GeneratedJob>
    }

    List parseJenkinsGroovyScripts() {
        EnvVars env = new EnvVars()
        File workspaceFile = new File('.').getAbsoluteFile()
        ScriptRequestGenerator generator = new ScriptRequestGenerator(new FilePath(workspaceFile), env)
        List scriptRequests = generator.getScriptRequests(
                jobFiles.join("\n"),
                false,
                "",
                false,
                "src/main/groovy/").toList()
        return scriptRequests
    }

    static void writeFile(File dir, String name, String xml) {
        List tokens = name.split('/')
        File folderDir = tokens[0..<-1].inject(dir) { File tokenDir, String token ->
            new File(tokenDir, token)
        }
        folderDir.mkdirs()

        File xmlFile = new File(folderDir, "${tokens[-1]}.xml")
        xmlFile.text = xml
    }

    static Set getNamesFromJobs(dslJobs) {
        return dslJobs.collect { it.jobName }
    }

    // Store generated XML files in XML output directory for manual validation.
    void writeJobsXmlOutputToDir(Set<GeneratedJob> jobs, String dir) {
        jobs.each { job ->
            Job jenkinsJob = jenkinsRule.jenkins.getItemByFullName(job.jobName) as Job
            String text = new URL(jenkinsRule.jenkins.rootUrl + jenkinsJob.url + 'config.xml').text
            writeFile(new File(dir, 'jobs'), job.jobName, text)
        }
    }
    String getEnvironment() {
        return "test1"
    }
}
