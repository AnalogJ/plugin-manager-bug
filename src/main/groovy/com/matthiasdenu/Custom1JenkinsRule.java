package com.matthiasdenu;

import org.jvnet.hudson.test.JenkinsRule;

import java.util.logging.Logger;

public class Custom1JenkinsRule extends JenkinsRule {
    Custom1JenkinsRule(){
        super();
    }

    @Override
    public void before() throws Throwable {
        Logger.getLogger("").severe("==============================");
        Logger.getLogger("").severe("==============================");
        Logger.getLogger("").severe("==============================");
        Logger.getLogger("").severe("================Custom1JenkinsRule before()==============");
        Logger.getLogger("").severe("==============================");
        Logger.getLogger("").severe("==============================");
        super.before();
    }

    @Override
    public void after() throws Exception {
        Logger.getLogger("").severe("==============================");
        Logger.getLogger("").severe("==============================");
        Logger.getLogger("").severe("==============================");
        Logger.getLogger("").severe("================Custom1JenkinsRule after()==============");
        Logger.getLogger("").severe("==============================");
        Logger.getLogger("").severe("==============================");
        super.after();
    }

}
