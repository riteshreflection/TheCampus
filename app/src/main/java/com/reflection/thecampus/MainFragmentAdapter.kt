package com.reflection.thecampus

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class MainFragmentAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
    
    override fun getItemCount(): Int = 3
    
    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> MyCoursesFragment()
            1 -> DiscoverFragment()
            2 -> NotificationsFragment()
            else -> MyCoursesFragment()
        }
    }
}
