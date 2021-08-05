package com.matthiasdenu;

import org.jvnet.hudson.test.JenkinsRule;

import java.util.logging.Logger;

public class Custom2JenkinsRule extends JenkinsRule {
    Custom2JenkinsRule(){
        super();
    }

    @Override
    public void before() throws Throwable {
        Logger.getLogger("").severe("==============================");
        Logger.getLogger("").severe("==============================");
        Logger.getLogger("").severe("==============================");
        Logger.getLogger("").severe("================Custom2JenkinsRule before()==============");
        Logger.getLogger("").severe("==============================");
        Logger.getLogger("").severe("==============================");
        super.before();
    }

    @Override
    public void after() throws Exception {
        Logger.getLogger("").severe("==============================");
        Logger.getLogger("").severe("==============================");
        Logger.getLogger("").severe("==============================");
        Logger.getLogger("").severe("================Custom2JenkinsRule after()==============");
        Logger.getLogger("").severe("==============================");
        Logger.getLogger("").severe("==============================");
        super.after();
    }
}
