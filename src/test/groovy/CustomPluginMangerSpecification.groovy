import com.matthiasdenu.CustomPluginManager
import hudson.PluginManager

class CustomPluginMangerSpecification extends BaseSpecification {
    @Override
    String getEnvironment() {
        return "example"
    }

    @Override
    PluginManager getPluginManager() {
        // This breaks `gradle test` with the "java.lang.AssertionError: class
        // org.jenkinsci.plugins.workflow.job.WorkflowJob is missing its descriptor" error
        // 
        // You can use one instance of one type, but you can't start a new Spock spec
        // with a new JenkinsRule using another instance of another type or you will get 
        // this error -- At the end of the day, I want to use the resolveTestPluginsCustom 
        // gradle task in build.gradle to test some of the Jenkins job DSL with a different
        // set of plugins of different specs... So if there is another way to do this, 
        // I am all ears... I haven't yet tried changing the versions of the 
        // jenkins-test-harness that we're using.
        return CustomPluginManager.INSTANCE
    }
}
