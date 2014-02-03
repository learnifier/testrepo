/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.coursedesign;

import net.unixdeveloper.druwa.RequestCycle;
import se.dabox.cocobox.coursebuilder.initdata.CourseBuilderActivationLink;
import se.dabox.cocobox.coursebuilder.initdata.InitData;

/**
 *
 * @author Jerker Klang <jerker.klang@dabox.se>
 */
public class GotoDesignBuilder {

    public static String process(RequestCycle cycle, InitData initData) {

        return CourseBuilderActivationLink.generate(cycle, initData);
    }

    private GotoDesignBuilder() {
    }

}
