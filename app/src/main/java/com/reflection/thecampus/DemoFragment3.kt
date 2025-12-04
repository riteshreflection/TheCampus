package com.reflection.thecampus

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

class DemoFragment3 : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = TextView(context)
        view.text = "Demo Fragment 3\n\nLast test fragment.\n\nIf tabs switch instantly here, the problem is in your original fragments (MyCoursesFragment, DiscoverFragment, NotificationsFragment)."
        view.textSize = 18f
        view.setPadding(32, 32, 32, 32)
        view.setBackgroundColor(0xFFE3F2FD.toInt())
        return view
    }
}
