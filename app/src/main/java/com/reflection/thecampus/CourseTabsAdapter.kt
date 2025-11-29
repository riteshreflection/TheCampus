package com.reflection.thecampus

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.reflection.thecampus.ui.course.CourseContentFragment

class CourseTabsAdapter(
    activity: FragmentActivity,
    private val course: Course,
    private val mentors: List<Faculty>,
    private val isEnrolled: Boolean
) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = 4

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> CourseOverviewFragment.newInstance(course, mentors, isEnrolled)
            1 -> CourseTestsFragment.newInstance(course.linkedTests, isEnrolled)
            2 -> CourseContentFragment.newInstance(course, isEnrolled)
            3 -> CourseClassesFragment.newInstance(course.linkedClasses)
            else -> CourseOverviewFragment.newInstance(course, mentors, isEnrolled)
        }
    }
}
