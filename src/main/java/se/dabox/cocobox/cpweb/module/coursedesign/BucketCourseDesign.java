/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.coursedesign;

import se.dabox.service.common.coursedesign.BucketCourseDesignInfo;
import se.dabox.service.common.coursedesign.CourseDesign;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
public class BucketCourseDesign {
    private CourseDesign design;
    private BucketCourseDesignInfo info;

    public BucketCourseDesign(CourseDesign design, BucketCourseDesignInfo info) {
        this.design = design;
        this.info = info;
    }

    public CourseDesign getDesign() {
        return design;
    }

    public BucketCourseDesignInfo getInfo() {
        return info;
    }

}
