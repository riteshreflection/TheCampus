package com.reflection.thecampus

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class MainFragmentAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
    
    override fun getItemCount(): Int = 5
    
    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> MyCoursesFragment()
            1 -> DashboardFragment()
            2 -> DiscoverFragment()
            3 -> ChatFragment()
            4 -> NotificationsFragment()
            else -> MyCoursesFragment()
        }
    }
}
